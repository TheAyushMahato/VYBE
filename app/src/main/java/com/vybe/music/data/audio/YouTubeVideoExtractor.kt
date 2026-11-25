package com.vybe.music.data.audio

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.vybe.music.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class VideoQualityStream(
    val qualityLabel: String, // e.g. "1080p", "720p", "480p", "360p", "240p"
    val url: String,
    val mimeType: String,
    val width: Int = 0,
    val height: Int = 0,
    val bitrate: Int = 0
)

data class VideoCandidate(
    val videoId: String,
    val title: String,
    val channel: String,
    val durationText: String = "",
    val thumbnailUrl: String = ""
)

data class VideoStreamResult(
    val videoId: String,
    val title: String,
    val channel: String,
    val streamUrl: String,
    val audioUrl: String? = null,
    val durationMs: Long,
    val availableQualities: List<VideoQualityStream> = emptyList(),
    val fallbackUrls: List<String> = emptyList(),
    val userAgent: String = YouTubeVideoExtractor.VIDEO_USER_AGENT
)

/**
 * Direct ad-free YouTube video stream extractor & Smart YouTube Video Link Resolver.
 * Strictly NO WebView and NO ads.
 * Tier 1: Official music video (filtering audio-only, lyrics, visualizers, topic channels, etc.).
 * Tier 2: Official video / visualizer.
 * Tier 3: Verified live performance/session.
 * Tier 4: Direct fallback to best matched song video or audio track videoId.
 */
object YouTubeVideoExtractor {
    private const val TAG = "YouTubeVideoExtractor"
    const val VIDEO_USER_AGENT = "Mozilla/5.0 (Linux; Android 12; Quest 3)"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val gson = Gson()
    private val videoCache = mutableMapOf<String, VideoStreamResult>()
    // Cache resolved videoId per track so toggling Video mode on the same track is instant
    private val resolvedVideoIdCache = mutableMapOf<String, String>()
    @Volatile
    private var cachedVisitorData: String? = null

    private val REJECTED_KEYWORDS = listOf(
        "live", "concert", "stage", "performance", "award", "unplugged",
        "reaction", "status", "shorts", "#shorts", "cover", "karaoke",
        "sped up", "speed up", "slowed", "reverb", "nightcore", "8d", "tiktok", "chipmunk",
        "pitched", "remix", "guitar lesson", "tutorial", "review", "teaser", "trailer"
    )

    private val VERIFIED_MUSIC_LABELS = listOf(
        "vevo", "t-series", "tseries", "sony music", "zee music", "desi melodies",
        "yrf", "yash raj", "speed records", "tips official", "tips music", "saregama",
        "universal music", "warner music", "atlantic records", "columbia records",
        "def jam", "interscope", "republic records", "spinnin'", "monstercat", "ultra music"
    )

    private val STRICT_AUDIO_KEYWORDS = listOf(
        "official audio", "audio only", "(audio)", "[audio]", " audio", "audio "
    )

    fun extractPrimaryArtist(artist: String): String {
        return artist.split(Regex("(?i)\\s+(?:&|feat\\.?|ft\\.?|x|,)\\s+|[,&/•;]"))
            .firstOrNull()?.trim()
            ?.takeIf { it.isNotBlank() } ?: artist.trim()
    }

    private fun cleanSongTitle(title: String): String {
        var clean = title
        val patternsToRemove = listOf(
            Regex("(?i)\\(official\\s+audio\\)"),
            Regex("(?i)\\[official\\s+audio\\]"),
            Regex("(?i)\\(audio\\)"),
            Regex("(?i)\\[audio\\]"),
            Regex("(?i)\\(official\\s+video\\)"),
            Regex("(?i)\\[official\\s+video\\]"),
            Regex("(?i)\\(official\\s+music\\s+video\\)"),
            Regex("(?i)\\[official\\s+music\\s+video\\]"),
            Regex("(?i)\\(lyrics?\\)"),
            Regex("(?i)\\[lyrics?\\]"),
            Regex("(?i)\\(visualizer\\)"),
            Regex("(?i)\\[visualizer\\]")
        )
        for (pattern in patternsToRemove) {
            clean = clean.replace(pattern, "").trim()
        }
        return clean.ifBlank { title.trim() }
    }

    /**
     * Resolves the exact YouTube videoId for a track using strict Official Music Video resolution:
     * Tier 1: "official music video" via YouTube InnerTube search + YTM Videos — strict score >= 35
     * Tier 2: "music video" fallback — relaxed score >= 30
     * Tier 3: Lyrics/visualizer fallback ONLY for tracks with no official MV (unreleased songs)
     * Returns null if no verified candidate exists (no random or blind track.id fallback).
     * Results are cached in memory per trackId so re-toggling is instant.
     */
    suspend fun resolveSmartYouTubeVideoId(track: Track): String? = withContext(Dispatchers.IO) {
        // Check cache first — keyed by trackId so it's instant on re-toggle
        val cacheKey = "${track.id}|${track.title.trim().lowercase()}_${track.artist.trim().lowercase()}"
        resolvedVideoIdCache[cacheKey]?.let { cached ->
            Log.d(TAG, "Returning cached resolved videoId for '${track.title}': $cached")
            return@withContext cached
        }

        val primaryArtist = extractPrimaryArtist(track.artist)
        val cleanTitle = cleanSongTitle(track.title)
        val isExplicitLiveSong = track.title.contains("live", ignoreCase = true) ||
                track.title.contains("concert", ignoreCase = true)
        // Tracks from YouTube • uploader albums are user-uploaded — allow lyrics/visualizer tier immediately
        val isExplicitYouTubeUpload = track.album.startsWith("YouTube") || track.album.contains("Uploaded by")

        try {
            if (!isExplicitYouTubeUpload) {
                // Tier 1: Search specifically with "$cleanSongTitle $primaryArtist official music video"
                val q1 = "$cleanTitle $primaryArtist official music video"
                Log.d(TAG, "Tier 1 Search: '$q1'")
                val c1 = searchVideoCandidates(q1, trackTitle = cleanTitle, trackArtist = primaryArtist, searchTier = 1, allowLive = isExplicitLiveSong, allowLyricsVisualizer = false, trackDurationMs = track.durationMs)
                if (c1 != null && c1.score >= 35) {
                    Log.i(TAG, "Tier 1 matched: ${c1.videoId} - ${c1.title} (${c1.channel}) [score=${c1.score}]")
                    resolvedVideoIdCache[cacheKey] = c1.videoId
                    return@withContext c1.videoId
                }

                // Tier 2: Search with "$cleanSongTitle $primaryArtist music video"
                val q2 = "$cleanTitle $primaryArtist music video"
                Log.d(TAG, "Tier 2 Search: '$q2'")
                val c2 = searchVideoCandidates(q2, trackTitle = cleanTitle, trackArtist = primaryArtist, searchTier = 2, allowLive = isExplicitLiveSong, allowLyricsVisualizer = false, trackDurationMs = track.durationMs)
                if (c2 != null && c2.score >= 30) {
                    Log.i(TAG, "Tier 2 matched: ${c2.videoId} - ${c2.title} (${c2.channel}) [score=${c2.score}]")
                    resolvedVideoIdCache[cacheKey] = c2.videoId
                    return@withContext c2.videoId
                }
            }

            // Tier 3: Lyrics/visualizer fallback for unreleased tracks or when no official MV exists
            val q3 = "$cleanTitle $primaryArtist"
            Log.d(TAG, "Tier 3 (lyrics/visualizer fallback) Search: '$q3'")
            val c3 = searchVideoCandidates(q3, trackTitle = cleanTitle, trackArtist = primaryArtist, searchTier = 3, allowLive = isExplicitLiveSong, allowLyricsVisualizer = true, trackDurationMs = track.durationMs)
            if (c3 != null && c3.score >= 20) {
                Log.i(TAG, "Tier 3 (lyrics/visualizer) matched: ${c3.videoId} - ${c3.title} (${c3.channel}) [score=${c3.score}]")
                resolvedVideoIdCache[cacheKey] = c3.videoId
                return@withContext c3.videoId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed resolving smart YouTube video for '${track.title}': ${e.message}", e)
        }

        null
    }

    /**
     * Launches the resolved YouTube video URL directly in the YouTube app,
     * falling back seamlessly to the device's web browser if the YouTube app is not installed.
     */
    fun launchYouTubeVideo(context: Context, videoId: String) {
        val webUri = Uri.parse("https://www.youtube.com/watch?v=$videoId")
        val appIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
            setPackage("com.google.android.youtube")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(appIntent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    suspend fun extractVideoStream(track: Track): VideoStreamResult? = withContext(Dispatchers.IO) {
        val cacheKey = "${track.title.trim().lowercase()}_${track.artist.trim().lowercase()}"
        videoCache[cacheKey]?.let { cached ->
            Log.d(TAG, "Returning cached video stream for: ${track.title} -> ${cached.streamUrl}")
            return@withContext cached
        }

        try {
            val resolvedVideoId = resolveSmartYouTubeVideoId(track)
            if (resolvedVideoId != null) {
                val streamResult = extractStreamForVideoId(
                    videoId = resolvedVideoId,
                    title = track.title,
                    channel = track.artist
                )
                if (streamResult != null) {
                    videoCache[cacheKey] = streamResult
                    Log.i(TAG, "Successfully extracted direct video stream: ${streamResult.streamUrl.take(70)}...")
                    return@withContext streamResult
                }
            }

            Log.w(TAG, "No suitable verified video stream found for: ${track.title} by ${track.artist}")
        } catch (e: Exception) {
            Log.e(TAG, "Video extraction failed for '${track.title}': ${e.message}", e)
        }

        null
    }

    private data class Candidate(val videoId: String, val title: String, val channel: String, val score: Int)

    private fun searchVideoCandidates(
        query: String,
        trackTitle: String,
        trackArtist: String,
        searchTier: Int,
        allowLive: Boolean = false,
        allowLyricsVisualizer: Boolean = false,
        trackDurationMs: Long = 0L
    ): Candidate? {
        val renderers = mutableListOf<JsonObject>()

        // 1. Primary: InnerTube Search API (clean structured JSON, fast & reliable)
        try {
            val searchApiUrl = "https://www.youtube.com/youtubei/v1/search"
            val payload = JsonObject().apply {
                val contextObj = JsonObject().apply {
                    val clientObj = JsonObject().apply {
                        addProperty("clientName", "WEB")
                        addProperty("clientVersion", "2.20231201.00.00")
                        addProperty("hl", "en")
                        addProperty("gl", "US")
                    }
                    add("client", clientObj)
                }
                add("context", contextObj)
                addProperty("query", query)
            }
            val reqBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(searchApiUrl)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .post(reqBody)
                .build()

            val responseBody = httpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }

            if (!responseBody.isNullOrBlank()) {
                val root = gson.fromJson(responseBody, JsonObject::class.java)
                findVideoRenderers(root, renderers)
            }
        } catch (e: Exception) {
            Log.w(TAG, "InnerTube search API failed, falling back to HTML: ${e.message}")
        }

        // 2. Fallback: HTML scraping if InnerTube yielded zero results
        if (renderers.isEmpty()) {
            try {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val searchUrl = "https://www.youtube.com/results?search_query=$encodedQuery"
                val request = Request.Builder()
                    .url(searchUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .build()

                val html = httpClient.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string() else null
                }

                if (!html.isNullOrBlank()) {
                    val jsonPattern = Pattern.compile("ytInitialData\\s*=\\s*(\\{.+?\\});")
                    val matcher = jsonPattern.matcher(html)
                    val jsonString = if (matcher.find()) {
                        matcher.group(1)
                    } else {
                        val altPattern = Pattern.compile("var ytInitialData = (\\{.*?\\});</script>")
                        val altMatcher = altPattern.matcher(html)
                        if (altMatcher.find()) altMatcher.group(1) else null
                    }

                    if (!jsonString.isNullOrBlank()) {
                        val root = gson.fromJson(jsonString, JsonObject::class.java)
                        findVideoRenderers(root, renderers)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "HTML search fallback failed: ${e.message}")
            }
        }

        if (renderers.isEmpty()) return null

        val candidates = mutableListOf<Candidate>()
        val lowerArtist = trackArtist.lowercase().trim()
        val lowerTrackTitle = trackTitle.lowercase().trim()

        // Extract key words from song title for relevance scoring
        val stopwords = setOf("the", "and", "feat", "ft", "with", "from", "remix", "version", "song", "for", "you")
        val titleWords = lowerTrackTitle
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.length > 2 && it !in stopwords }

        for (r in renderers) {
            val vid = r.get("videoId")?.asString ?: continue
            val title = try {
                r.getAsJsonObject("title")
                    ?.getAsJsonArray("runs")
                    ?.get(0)?.asJsonObject
                    ?.get("text")?.asString ?: ""
            } catch (e: Exception) { "" }

            val channel = try {
                r.getAsJsonObject("ownerText")
                    ?.getAsJsonArray("runs")
                    ?.get(0)?.asJsonObject
                    ?.get("text")?.asString ?: ""
            } catch (e: Exception) { "" }

            val lowerTitle = title.lowercase()
            val lowerChannel = channel.lowercase()

            // Extract video duration for scoring
            val durationText = try {
                r.getAsJsonObject("lengthText")?.get("simpleText")?.asString ?: ""
            } catch (e: Exception) { "" }
            val videoDurationMs = parseDurationToMs(durationText)
            // Reject Shorts (< 45s) and hour-long mixes/podcasts (> 12 min)
            if (videoDurationMs > 0 && (videoDurationMs < 45_000L || videoDurationMs > 720_000L)) continue

            // Strictly reject non-official keywords (live, concert, stage, award, reaction, shorts, cover, etc.) unless track itself is explicitly live
            if (!allowLive) {
                if (REJECTED_KEYWORDS.any { lowerTitle.contains(it) || lowerChannel.contains(it) }) continue
            }

            // Topic channel & strict audio filtering for video tiers
            val isTopicChannel = lowerChannel.endsWith("- topic") || (lowerChannel.contains("topic") && lowerChannel.length <= 16)
            if (isTopicChannel) continue

            if (STRICT_AUDIO_KEYWORDS.any { lowerTitle.contains(it) }) continue
            // For Tier-3 (allowLyricsVisualizer), allow lyrics/visualizer videos as fallback
            if (!allowLyricsVisualizer) {
                if (lowerTitle.contains("lyric") || lowerTitle.contains("lyrics") || lowerTitle.contains("visualizer")) continue
            }

            // Word-matching relevance: ensure title matches the requested song
            val matchedWords = titleWords.filter { lowerTitle.contains(it) }
            if (titleWords.isNotEmpty()) {
                if (matchedWords.isEmpty()) continue
                if (titleWords.size >= 2 && matchedWords.size < (titleWords.size + 1) / 2) {
                    continue
                }
            }

            val artistParts = lowerArtist.split(Regex("(?i)\\s+(?:&|feat\\.?|ft\\.?|x|,|and)\\s+|[,&/•;]"))
                .map { it.trim() }
                .filter { it.length > 1 }
            val channelOrTitleHasArtist = artistParts.any { lowerChannel.contains(it) || lowerTitle.contains(it) }
            val isOfficialArtistChannel = (lowerArtist.isNotBlank() && (lowerChannel.contains(lowerArtist) || lowerArtist.contains(lowerChannel)))
            val isVerifiedMusicLabel = VERIFIED_MUSIC_LABELS.any { lowerChannel.contains(it) || lowerTitle.contains(it) }

            // Candidate channel or title MUST match artist, or channel MUST be a verified label
            // For Tier-3 (lyrics/visualizer fallback), relax artist requirement slightly — title match is sufficient
            if (!channelOrTitleHasArtist && !isOfficialArtistChannel && !isVerifiedMusicLabel && !allowLyricsVisualizer) {
                continue
            }
            // For Tier-3 with relaxed artist check: at least one title word must still match
            if (allowLyricsVisualizer && matchedWords.isEmpty()) continue

            var score = 0
            val titleMatchRatio = if (titleWords.isNotEmpty()) matchedWords.size.toFloat() / titleWords.size.toFloat() else 1f
            score += (titleMatchRatio * 45).toInt()

            if (lowerTitle.contains(lowerTrackTitle)) {
                score += 35 // Full phrase match!
            }

            val badges = r.getAsJsonArray("ownerBadges")
            val isVerified = badges != null && badges.size() > 0

            if (isOfficialArtistChannel) score += 30
            if (isVerifiedMusicLabel) score += 35
            if (isVerified) score += 15

            if (lowerTitle.contains("official music video")) score += 45
            else if (lowerTitle.contains("official video")) score += 40
            else if (lowerTitle.contains("full video")) score += 30
            else if (lowerTitle.contains("music video") || lowerTitle.contains(" m/v") || lowerTitle.contains(" mv") || lowerTitle.contains("[mv]") || lowerTitle.contains("(mv)")) score += 25
            else if (lowerTitle.contains("video")) score += 15

            // Tier-3 lyrics/visualizer bonus for unreleased tracks
            if (allowLyricsVisualizer) {
                if (lowerTitle.contains("lyrics") || lowerTitle.contains("lyric video")) score += 10
                if (lowerTitle.contains("visualizer")) score += 8
            }

            // Duration proximity bonus: +10 if within ±50s of track duration
            if (trackDurationMs > 0 && videoDurationMs > 0) {
                val diffSec = kotlin.math.abs(videoDurationMs - trackDurationMs) / 1000
                if (diffSec <= 50) score += 10
            }

            if (score > 0) {
                candidates.add(Candidate(vid, title, channel, score))
            }
        }

        return candidates.maxByOrNull { it.score }
    }

    /** Parse "M:SS" or "H:MM:SS" duration string to milliseconds */
    private fun parseDurationToMs(durationText: String): Long {
        if (durationText.isBlank()) return 0L
        return try {
            val parts = durationText.trim().split(":")
            when (parts.size) {
                2 -> (parts[0].toLong() * 60 + parts[1].toLong()) * 1000L
                3 -> (parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()) * 1000L
                else -> 0L
            }
        } catch (_: Exception) { 0L }
    }

    private fun findVideoRenderers(obj: com.google.gson.JsonElement, out: MutableList<JsonObject>) {
        when {
            obj.isJsonObject -> {
                val jObj = obj.asJsonObject
                if (jObj.has("videoRenderer") && jObj.get("videoRenderer").isJsonObject) {
                    out.add(jObj.getAsJsonObject("videoRenderer"))
                }
                for (entry in jObj.entrySet()) {
                    findVideoRenderers(entry.value, out)
                }
            }
            obj.isJsonArray -> {
                for (item in obj.asJsonArray) {
                    findVideoRenderers(item, out)
                }
            }
        }
    }

    fun extractVideoId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.length == 11 && trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return trimmed
        }
        val patterns = listOf(
            Pattern.compile("(?:v=|/v/|youtu\\.be/|/embed/|/shorts/|\\?v=|^v/)([a-zA-Z0-9_-]{11})"),
            Pattern.compile("[?&]v=([a-zA-Z0-9_-]{11})")
        )
        for (p in patterns) {
            val m = p.matcher(trimmed)
            if (m.find()) {
                return m.group(1)
            }
        }
        return null
    }

    suspend fun searchAlternateVideos(query: String): List<VideoCandidate> = withContext(Dispatchers.IO) {
        try {
            val searchApiUrl = "https://www.youtube.com/youtubei/v1/search"
            val payload = JsonObject().apply {
                val contextObj = JsonObject().apply {
                    val clientObj = JsonObject().apply {
                        addProperty("clientName", "WEB")
                        addProperty("clientVersion", "2.20231201.00.00")
                        addProperty("hl", "en")
                        addProperty("gl", "US")
                    }
                    add("client", clientObj)
                }
                add("context", contextObj)
                addProperty("query", query)
            }
            val reqBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(searchApiUrl)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .post(reqBody)
                .build()

            val responseBody = httpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return@withContext emptyList()

            val root = gson.fromJson(responseBody, JsonObject::class.java)
            val renderers = mutableListOf<JsonObject>()
            findVideoRenderers(root, renderers)

            val candidates = mutableListOf<VideoCandidate>()
            for (r in renderers) {
                val vid = r.get("videoId")?.asString ?: continue
                val title = try {
                    r.getAsJsonObject("title")
                        ?.getAsJsonArray("runs")
                        ?.get(0)?.asJsonObject
                        ?.get("text")?.asString ?: ""
                } catch (e: Exception) { "" }

                val channel = try {
                    r.getAsJsonObject("ownerText")
                        ?.getAsJsonArray("runs")
                        ?.get(0)?.asJsonObject
                        ?.get("text")?.asString ?: ""
                } catch (e: Exception) { "" }

                val duration = try {
                    r.getAsJsonObject("lengthText")
                        ?.get("simpleText")?.asString ?: ""
                } catch (e: Exception) { "" }

                val thumb = try {
                    r.getAsJsonObject("thumbnail")
                        ?.getAsJsonArray("thumbnails")
                        ?.lastOrNull()?.asJsonObject
                        ?.get("url")?.asString
                } catch (e: Exception) { null } ?: "https://i.ytimg.com/vi/$vid/hqdefault.jpg"

                if (title.isNotBlank()) {
                    candidates.add(VideoCandidate(vid, title, channel, duration, thumb))
                }
                if (candidates.size >= 12) break
            }
            return@withContext candidates
        } catch (e: Exception) {
            Log.e(TAG, "searchAlternateVideos failed: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Resolves visitorData token needed for ANDROID_VR client player requests.
     */
    private fun getVisitorData(): String? {
        cachedVisitorData?.let { return it }

        // 1. Try fast visitor_id JSON endpoint
        try {
            val endpoint = "https://www.youtube.com/youtubei/v1/visitor_id"
            val payload = JsonObject().apply {
                val contextObj = JsonObject().apply {
                    val clientObj = JsonObject().apply {
                        addProperty("clientName", "WEB")
                        addProperty("clientVersion", "2.20231201.00.00")
                    }
                    add("client", clientObj)
                }
                add("context", contextObj)
            }
            val reqBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .post(reqBody)
                .build()

            val resp = httpClient.newCall(request).execute().use { r ->
                if (r.isSuccessful) r.body?.string() else null
            }
            if (!resp.isNullOrBlank()) {
                val j = gson.fromJson(resp, JsonObject::class.java)
                val token = j.getAsJsonObject("responseContext")?.get("visitorData")?.asString
                if (!token.isNullOrBlank()) {
                    cachedVisitorData = token
                    return token
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "visitor_id endpoint lookup failed: ${e.message}")
        }

        return null
    }

    suspend fun extractStreamForVideoId(
        videoId: String,
        title: String? = null,
        channel: String? = null
    ): VideoStreamResult? = withContext(Dispatchers.IO) {
        // Step 1: GET watch page to extract video-bound visitorData token
        var visitorData: String? = null
        try {
            val watchUrl = "https://www.youtube.com/watch?v=$videoId"
            val watchRequest = Request.Builder()
                .url(watchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val watchHtml = httpClient.newCall(watchRequest).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: ""

            val visitorPattern = Pattern.compile("\"(?:VISITOR_DATA|visitorData)\":\"([^\"]+)\"")
            val vMatcher = visitorPattern.matcher(watchHtml)
            visitorData = if (vMatcher.find()) vMatcher.group(1) else null
        } catch (e: Exception) {
            Log.w(TAG, "Watch page visitor token extraction failed: ${e.message}")
        }

        if (visitorData.isNullOrBlank()) {
            visitorData = getVisitorData()
        }

        // Multi-client fallback chain: ANDROID_VR -> IOS -> TVHTML5_SIMPLY_EMBEDDED_PLAYER
        data class InnerTubeClient(
            val name: String,
            val clientName: String,
            val clientVersion: String,
            val clientHeader: String,
            val userAgent: String,
            val deviceMake: String? = null,
            val deviceModel: String? = null,
            val osName: String? = null,
            val osVersion: String? = null
        )

        val clients = listOf(
            InnerTubeClient(
                name = "ANDROID_VR",
                clientName = "ANDROID_VR",
                clientVersion = "1.65.10",
                clientHeader = "28",
                userAgent = VIDEO_USER_AGENT,
                deviceMake = "Oculus",
                deviceModel = "Quest 3",
                osName = "Android",
                osVersion = "12"
            ),
            InnerTubeClient(
                name = "IOS",
                clientName = "IOS",
                clientVersion = "19.29.1",
                clientHeader = "5",
                userAgent = "com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X; en_US)",
                deviceMake = "Apple",
                deviceModel = "iPhone16,2",
                osName = "iOS",
                osVersion = "17.5.1.21F90"
            ),
            InnerTubeClient(
                name = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
                clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
                clientVersion = "2.0",
                clientHeader = "85",
                userAgent = "Mozilla/5.0 (PlayStation; PlayStation 4/11.50) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/11.50 Safari/605.1.15"
            )
        )

        val playerUrl = "https://www.youtube.com/youtubei/v1/player"

        for (clientConfig in clients) {
            val payload = JsonObject().apply {
                val contextObj = JsonObject().apply {
                    val clientObj = JsonObject().apply {
                        addProperty("clientName", clientConfig.clientName)
                        addProperty("clientVersion", clientConfig.clientVersion)
                        clientConfig.deviceMake?.let { addProperty("deviceMake", it) }
                        clientConfig.deviceModel?.let { addProperty("deviceModel", it) }
                        clientConfig.osName?.let { addProperty("osName", it) }
                        clientConfig.osVersion?.let { addProperty("osVersion", it) }
                    }
                    add("client", clientObj)
                }
                add("context", contextObj)
                addProperty("videoId", videoId)
                if (clientConfig.clientName == "TVHTML5_SIMPLY_EMBEDDED_PLAYER") {
                    addProperty("racyCheckOk", true)
                    addProperty("contentCheckOk", true)
                }
            }

            val requestBuilder = Request.Builder()
                .url(playerUrl)
                .header("Content-Type", "application/json")
                .header("User-Agent", clientConfig.userAgent)
                .header("X-Youtube-Client-Name", clientConfig.clientHeader)
                .header("X-Youtube-Client-Version", clientConfig.clientVersion)

            if (!visitorData.isNullOrBlank()) {
                requestBuilder.header("X-Goog-Visitor-Id", visitorData)
            }

            val reqBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            requestBuilder.post(reqBody)

            val playerResponse = try {
                httpClient.newCall(requestBuilder.build()).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string() else null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Player API call failed for client ${clientConfig.name}: ${e.message}")
                null
            } ?: continue

            val pRoot = try {
                gson.fromJson(playerResponse, JsonObject::class.java)
            } catch (e: Exception) {
                continue
            }

            val playabilityStatus = pRoot.getAsJsonObject("playabilityStatus")?.get("status")?.asString ?: "UNKNOWN"
            Log.i(TAG, "Video $videoId client ${clientConfig.name} playabilityStatus: $playabilityStatus")

            if (playabilityStatus != "OK") {
                Log.w(TAG, "Client ${clientConfig.name} returned status=$playabilityStatus for $videoId, trying next fallback...")
                continue
            }

            val streamingData = pRoot.getAsJsonObject("streamingData") ?: continue
            val formats = streamingData.getAsJsonArray("formats")
            val adaptiveFormats = streamingData.getAsJsonArray("adaptiveFormats")

            val videoQualities = mutableListOf<VideoQualityStream>()

            fun processFormat(fObj: JsonObject) {
                val mimeType = fObj.get("mimeType")?.asString ?: ""
                if (!mimeType.startsWith("video/")) return

                // Strictly exclude AV1 (av01) - causes high CPU decode overhead & blank frames on many Android devices
                if (mimeType.contains("av01", ignoreCase = true)) return

                val url = fObj.get("url")?.asString ?: ""
                if (url.isBlank() || url.contains("/sabr/")) return

                val itag = fObj.get("itag")?.asInt ?: 0
                val qualityLabelRaw = fObj.get("qualityLabel")?.asString ?: ""
                val height = fObj.get("height")?.asInt ?: 0
                val width = fObj.get("width")?.asInt ?: 0
                val bitrate = fObj.get("bitrate")?.asInt ?: 0
                val fps = fObj.get("fps")?.asInt ?: 30

                // Exclude 4K, 1440p, and 1080p60+ (allow 1080p30 for HD quality)
                if (height > 1080) return
                if (qualityLabelRaw.contains("1440", ignoreCase = true) ||
                    qualityLabelRaw.contains("2160", ignoreCase = true) ||
                    qualityLabelRaw.contains("4k", ignoreCase = true)) return
                // Exclude 1080p60 (high fps) but keep 1080p30
                if (height >= 1080 && fps > 50) return

                val label = when {
                    qualityLabelRaw.contains("1080") || height >= 1080 -> "1080p"
                    qualityLabelRaw.contains("720") || height in 720..1079 -> "720p"
                    qualityLabelRaw.contains("480") || height in 480..719 -> "480p"
                    qualityLabelRaw.contains("360") || height in 360..479 -> "360p"
                    qualityLabelRaw.contains("240") || height in 240..359 -> "240p"
                    qualityLabelRaw.contains("144") || (height in 1..239) -> "144p"
                    else -> qualityLabelRaw.ifBlank { if (height > 0) "${height}p" else "SD" }
                }

                val isH264 = mimeType.contains("avc1", ignoreCase = true) || mimeType.contains("mp4", ignoreCase = true)

                val existing = videoQualities.indexOfFirst { it.qualityLabel == label }
                if (existing >= 0) {
                    val current = videoQualities[existing]
                    // Strictly prefer hardware-decoded H.264 (avc1/mp4) over VP9/WebM
                    if (!current.mimeType.contains("avc") && isH264) {
                        videoQualities[existing] = VideoQualityStream(label, url, mimeType, width, height, bitrate)
                    }
                } else {
                    videoQualities.add(VideoQualityStream(label, url, mimeType, width, height, bitrate))
                }
            }

            // 1. Process adaptiveFormats (video-only streams, direct byte access)
            adaptiveFormats?.forEach { if (it.isJsonObject) processFormat(it.asJsonObject) }
            // 2. Also process muxed formats (itag 18 is 360p H.264 muxed, highly reliable fallback)
            formats?.forEach { if (it.isJsonObject) processFormat(it.asJsonObject) }

            // 3. Process HLS Manifest URL if available
            val hlsManifestUrl = streamingData.get("hlsManifestUrl")?.asString
            if (!hlsManifestUrl.isNullOrBlank() && hlsManifestUrl.startsWith("http")) {
                videoQualities.add(
                    VideoQualityStream(
                        qualityLabel = "HLS Auto",
                        url = hlsManifestUrl,
                        mimeType = "application/x-mpegURL",
                        width = 1280,
                        height = 720,
                        bitrate = 0
                    )
                )
            }

            var matchingAudioUrl: String? = null
            if (adaptiveFormats != null) {
                for (f in adaptiveFormats) {
                    if (!f.isJsonObject) continue
                    val fObj = f.asJsonObject
                    val mimeType = fObj.get("mimeType")?.asString ?: ""
                    if (!mimeType.startsWith("audio/")) continue
                    val aUrl = fObj.get("url")?.asString ?: ""
                    if (aUrl.isBlank() || aUrl.contains("/sabr/")) continue

                    val itag = fObj.get("itag")?.asInt ?: 0
                    if (itag == 140) { // 128kbps AAC audio from exact same videoId
                        matchingAudioUrl = aUrl
                        break
                    } else if (matchingAudioUrl == null) {
                        matchingAudioUrl = aUrl
                    }
                }
            }

            if (videoQualities.isNotEmpty()) {
                val qualityRank = mapOf(
                    "1080p" to 1,
                    "720p" to 2,
                    "480p" to 3,
                    "360p" to 4,
                    "240p" to 5,
                    "144p" to 6,
                    "HLS Auto" to 7
                )
                // Sort with strong preference for H.264 (avc1/mp4) within 480p-720p
                videoQualities.sortWith(
                    compareBy<VideoQualityStream> {
                        if (it.mimeType.contains("avc1", ignoreCase = true) || it.mimeType.contains("mp4", ignoreCase = true)) 0 else 1
                    }.thenBy {
                        qualityRank[it.qualityLabel] ?: 99
                    }
                )

                val autoStream = videoQualities.firstOrNull { it.qualityLabel == "1080p" && (it.mimeType.contains("avc1") || it.mimeType.contains("mp4")) }
                    ?: videoQualities.firstOrNull { it.qualityLabel == "720p" && (it.mimeType.contains("avc1") || it.mimeType.contains("mp4")) }
                    ?: videoQualities.firstOrNull { it.qualityLabel == "480p" && (it.mimeType.contains("avc1") || it.mimeType.contains("mp4")) }
                    ?: videoQualities.firstOrNull { it.qualityLabel == "360p" && (it.mimeType.contains("avc1") || it.mimeType.contains("mp4")) }
                    ?: videoQualities.firstOrNull { it.mimeType.contains("avc1") || it.mimeType.contains("mp4") }
                    ?: videoQualities.firstOrNull()

                val targetUrl = autoStream?.url
                val fallbackUrls = videoQualities.map { it.url }.filter { it != targetUrl }

                val isMuxedOrHls = autoStream?.qualityLabel == "HLS Auto" ||
                    (formats?.any { it.isJsonObject && it.asJsonObject.get("url")?.asString == targetUrl } == true)
                val finalAudioUrl = if (isMuxedOrHls) null else matchingAudioUrl

                val lengthSeconds = pRoot.getAsJsonObject("videoDetails")
                    ?.get("lengthSeconds")?.asString?.toLongOrNull() ?: 0L
                val durationMs = lengthSeconds * 1000L

                val videoDetails = pRoot.getAsJsonObject("videoDetails")
                val resolvedTitle = if (!title.isNullOrBlank()) title else videoDetails?.get("title")?.asString ?: "Music Video"
                val resolvedChannel = if (!channel.isNullOrBlank()) channel else videoDetails?.get("author")?.asString ?: ""

                if (!targetUrl.isNullOrBlank()) {
                    val result = VideoStreamResult(
                        videoId = videoId,
                        title = resolvedTitle,
                        channel = resolvedChannel,
                        streamUrl = targetUrl,
                        audioUrl = finalAudioUrl,
                        durationMs = durationMs,
                        availableQualities = videoQualities,
                        fallbackUrls = fallbackUrls,
                        userAgent = clientConfig.userAgent
                    )
                    Log.i(TAG, "Direct video stream ready via ${clientConfig.name} for $videoId: quality=${autoStream?.qualityLabel}, audioIncluded=${finalAudioUrl == null}, url=${targetUrl.take(70)}...")
                    return@withContext result
                }
            }
        }

        Log.e(TAG, "All video client fallbacks failed for videoId=$videoId")
        null
    }
}
