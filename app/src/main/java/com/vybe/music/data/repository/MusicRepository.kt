package com.vybe.music.data.repository

import android.os.Build
import android.text.Html
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.vybe.music.data.api.ITunesSongDto
import com.vybe.music.data.api.MusicApiService
import com.vybe.music.data.audio.YouTubeAudioExtractor
import com.vybe.music.model.Track
import com.vybe.music.utils.TrackUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

interface MusicRepository {
    suspend fun searchTracks(query: String): Result<List<Track>>
    suspend fun getTrendingTracks(): Result<List<Track>>
    suspend fun getArtistTracks(artistName: String): Result<List<Track>>
}

class MusicRepositoryImpl(
    private val apiService: MusicApiService = MusicApiService.instance
) : MusicRepository {

    companion object {
        private const val TAG = "MusicRepository"

        private val httpClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        private val gson = Gson()

        private val PENALIZED_SEARCH_TERMS = listOf(
            "(live", "live at", "live from", "live in", "in concert", "home session",
            "punk goes pop", "karaoke", "cover", "dubstep", "sped up", "slowed", "tribute"
        )

        private val ALLOWED_SEARCH_INTENTS = listOf(
            "live", "concert", "acoustic", "remix", "cover", "session"
        )

        private val SEARCH_STOPWORDS = setOf(
            "the", "a", "an", "and", "or", "by", "in", "of", "to", "on", "at", "for", "with", "from",
            "feat", "featuring", "ft", "lyrics", "audio", "video", "official", "song", "track"
        )

        private val REJECTED_VIDEO_KEYWORDS = listOf(
            "reaction", "reacting", "interview", "tutorial", "how to play", "guitar lesson",
            "piano lesson", "behind the scenes", "full album", "podcast", "review", "reviewing",
            "vlog", "tiktok compilation", "live reaction"
        )

        // Strict blacklist to filter out karaoke, instrumentals, covers, tributes, ringtones, etc.
        private val BLOCKED_KEYWORDS = listOf(
            "karaoke",
            "tribute",
            "ringtone",
            "workout",
            "8d",
            "slowed",
            "reverb",
            "lofi",
            "lo-fi",
            "karaoke version",
            "made famous",
            "8-bit",
            "16-bit",
            "emulation"
        )

        fun upgradeArtwork(url: String?): String {
            if (url.isNullOrBlank()) return ""
            return url
                .replace("100x100bb.jpg", "1200x1200bb.jpg")
                .replace("100x100bb.png", "1200x1200bb.png")
                .replace("100x100bb", "1200x1200bb")
                .replace(Regex("=w\\d+-h\\d+.*"), "=w1200-h1200-l90-rj")
                .replace("150x150", "500x500")
                .replace("50x50", "500x500")
        }

        private fun parseDurationSeconds(durationText: String): Long {
            if (durationText.isBlank()) return 210_000L
            val parts = durationText.trim().split(":").mapNotNull { it.trim().toLongOrNull() }
            val seconds = when (parts.size) {
                1 -> parts[0]
                2 -> parts[0] * 60L + parts[1]
                3 -> parts[0] * 3600L + parts[1] * 60L + parts[2]
                else -> 210L
            }
            return (seconds * 1000L).coerceAtLeast(30_000L)
        }
    }

    // Curated high-energy global & Indian official hit tracks
    private val curatedOfficialSongs = listOf(
        "One Of The Girls The Weeknd",
        "Levitating Dua Lipa",
        "Espresso Sabrina Carpenter",
        "Starboy The Weeknd",
        "Born to Shine Diljit Dosanjh",
        "Kesariya Arijit Singh",
        "Winning Speech Karan Aujla",
        "Blinding Lights The Weeknd",
        "Birds of a Feather Billie Eilish",
        "Lover Diljit Dosanjh",
        "Tum Hi Ho Arijit Singh",
        "Tauba Tauba Karan Aujla",
        "Dynamite BTS",
        "Chaleya Arijit Singh",
        "Brown Munde AP Dhillon"
    )

    // ── Check if a result is a genuine official track ───────────────────────
    private fun isOfficialTrack(dto: ITunesSongDto, requiredArtist: String? = null): Boolean {
        val title = (dto.trackName ?: "").lowercase()
        val album = (dto.collectionName ?: "").lowercase()
        val artist = (dto.artistName ?: "").lowercase()

        // 1. Strict Keyword Blacklist
        for (blocked in BLOCKED_KEYWORDS) {
            if (title.contains(blocked) || album.contains(blocked) || artist.contains(blocked)) {
                Log.d(TAG, "Filtering out non-official track [matched '$blocked']: '${dto.trackName}' by '$artist'")
                return false
            }
        }

        // 2. Artist Verification if specified
        if (!requiredArtist.isNullOrBlank()) {
            val req = requiredArtist.trim().lowercase()
            val matches = artist.contains(req) || req.contains(artist)
            if (!matches) {
                return false
            }
        }

        return !dto.trackName.isNullOrBlank()
    }

    // ── Global & Trending Official Hits ─────────────────────────────────────
    override suspend fun getTrendingTracks(): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching global trending official tracks via iTunes...")
            val jobs = curatedOfficialSongs.map { query ->
                async {
                    runCatching {
                        val response = withTimeoutOrNull(4000L) {
                            apiService.searchITunes(term = query, limit = 5, country = "IN")
                        }
                        val results = response?.results.orEmpty()
                        val candidates = results.filter { isOfficialTrack(it) }.mapNotNull { it.toTrack() }
                        // Pick the highest scoring candidate (prefers full Album / Soundtrack over EP / Single)
                        TrackUtils.deduplicateTracks(candidates).firstOrNull()
                    }.getOrNull()
                }
            }

            val fetched = jobs.awaitAll().filterNotNull()
            val tracks = TrackUtils.deduplicateTracks(fetched)

            if (tracks.isNotEmpty()) {
                Log.d(TAG, "Successfully loaded ${tracks.size} global official tracks")
                Result.success(tracks)
            } else {
                Log.w(TAG, "iTunes empty or failed to load official tracks")
                Result.failure(Exception("Failed to load trending tracks"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network failure in getTrendingTracks: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ── Artist Spotlight: Top Official Vocal Tracks for that Artist ─────────
    override suspend fun getArtistTracks(artistName: String): Result<List<Track>> = withContext(Dispatchers.IO) {
        if (artistName.isBlank()) return@withContext Result.success(emptyList())
        try {
            Log.d(TAG, "Fetching official top tracks for artist: $artistName")
            val response = withTimeoutOrNull(5000L) {
                apiService.searchITunes(term = artistName.trim(), limit = 25, country = "IN")
            }
            var results = response?.results.orEmpty()
            if (results.isEmpty()) {
                val globalResp = withTimeoutOrNull(5000L) {
                    apiService.searchITunesGlobal(term = artistName.trim(), limit = 25)
                }
                results = globalResp?.results.orEmpty()
            }

            val tracks = TrackUtils.deduplicateTracks(
                results
                    .filter { isOfficialTrack(it, requiredArtist = artistName) }
                    .mapNotNull { it.toTrack() }
            )

            Log.d(TAG, "Loaded ${tracks.size} official tracks for artist: $artistName")
            Result.success(tracks)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching artist tracks for '$artistName': ${e.message}", e)
            Result.failure(e)
        }
    }

    data class ParsedVideoMetadata(
        val title: String,
        val artist: String,
        val album: String
    )

    data class YouTubeWebVideoResult(
        val track: Track,
        val rawTitle: String,
        val viewCount: Long
    )

    private fun parseVideoTitleAndArtist(rawTitle: String, uploader: String): ParsedVideoMetadata {
        val cleanTitle = rawTitle
            .replace(Regex("""(?i)\s*[\(\[](official\s*(music\s*)?video|official\s*audio|lyrics?|lyric\s*video|audio|visualizer|unreleased|remastered|hq|hd|4k)[\)\]]"""), "")
            .trim()

        val separators = listOf(" - ", " – ", " — ", " : ", " | ")
        for (sep in separators) {
            if (cleanTitle.contains(sep)) {
                val parts = cleanTitle.split(sep, limit = 2)
                val left = parts[0].trim()
                val right = parts[1].trim()
                if (left.isNotBlank() && right.isNotBlank()) {
                    return ParsedVideoMetadata(
                        title = right,
                        artist = left,
                        album = if (uploader.isNotBlank()) "YouTube • $uploader" else "YouTube"
                    )
                }
            }
        }
        return ParsedVideoMetadata(
            title = cleanTitle,
            artist = uploader.ifBlank { "YouTube" },
            album = "YouTube"
        )
    }

    private fun parseViewCount(viewStr: String): Long {
        if (viewStr.isBlank()) return 0L
        val clean = viewStr.lowercase().replace("views", "").replace("view", "").trim()
        return try {
            when {
                clean.endsWith("b") -> (clean.dropLast(1).trim().toDouble() * 1_000_000_000).toLong()
                clean.endsWith("m") -> (clean.dropLast(1).trim().toDouble() * 1_000_000).toLong()
                clean.endsWith("k") -> (clean.dropLast(1).trim().toDouble() * 1_000).toLong()
                else -> clean.replace(",", "").replace(".", "").toLongOrNull() ?: 0L
            }
        } catch (_: Exception) { 0L }
    }

    private val FAN_TRANSLATION_SPAM = listOf(
        "traduzione", "italiana", "sub español", "subtitulada", "traducción", "türkçe",
        "çeviri", "paroles", "lyrics", "karaoke", "8d", "slowed", "reverb", "sped up"
    )

    private fun isStrongQueryMatch(track: Track, query: String): Boolean {
        val queryLower = query.lowercase().trim()
        val queryTokens = queryLower
            .split(Regex("""[^\p{L}\p{Nd}]+"""))
            .filter { it.length > 1 && it !in SEARCH_STOPWORDS }

        if (queryTokens.isEmpty()) return true

        val cleanTitle = track.title.lowercase().split(Regex("""[^\p{L}\p{Nd}]+""")).filter { it !in SEARCH_STOPWORDS }.joinToString(" ")
        val cleanArtist = track.artist.lowercase().split(Regex("""[^\p{L}\p{Nd}]+""")).filter { it !in SEARCH_STOPWORDS }.joinToString(" ")
        val combined = "$cleanTitle $cleanArtist"

        val matchedTokens = queryTokens.count { token -> combined.contains(token) }

        val required = when (queryTokens.size) {
            1 -> 1
            2 -> 2
            3 -> 2
            else -> queryTokens.size - 1
        }
        return matchedTokens >= required
    }

    // ── Search: Native YouTube Music InnerTube (+ YouTube Fallback) ──────
    override suspend fun searchTracks(query: String): Result<List<Track>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext Result.success(emptyList())
        val cleanQuery = query.trim()
        Log.d(TAG, "Searching YouTube Music InnerTube for: '$cleanQuery'")

        try {
            val candidateList = mutableListOf<Track>()
            val trackRawTitles = mutableMapOf<String, String>()
            val trackViewCounts = mutableMapOf<String, Long>()

            val queryWordsCount = cleanQuery.split(Regex("\\s+")).filter { it.isNotBlank() }.size

            // 1. Fetch YTM Songs and YouTube Web Videos (and YTM Videos for 2+ words) in parallel
            val ytmSongsDeferred = async { searchYtmSongs(cleanQuery) }
            val ytWebDeferred = async { searchYouTubeWebVideos(cleanQuery) }
            val ytmVideosDeferred = if (queryWordsCount >= 2) async { searchYtmVideos(cleanQuery) } else null

            val ytmTracks = ytmSongsDeferred.await()
            val ytWebResults = ytWebDeferred.await()
            val ytmVideoTracks = ytmVideosDeferred?.await() ?: emptyList()

            for (t in ytmTracks) {
                if (candidateList.none { it.id == t.id }) {
                    candidateList.add(t)
                }
            }

            for (yt in ytWebResults) {
                if (candidateList.none { it.id == yt.track.id }) {
                    candidateList.add(yt.track)
                    trackRawTitles[yt.track.id] = yt.rawTitle
                    trackViewCounts[yt.track.id] = yt.viewCount
                }
            }

            for (t in ytmVideoTracks) {
                if (candidateList.none { it.id == t.id }) {
                    candidateList.add(t)
                }
            }

            if (candidateList.isEmpty()) {
                Log.d(TAG, "Zero search results found for: '$cleanQuery'")
                return@withContext Result.success(emptyList())
            }

            // 2. Strict Studio Original Ranking & Unreleased Track Promotion
            val queryLower = cleanQuery.lowercase()
            val queryTokens = queryLower
                .split(Regex("""[^\p{L}\p{Nd}]+"""))
                .filter { it.length > 1 && it !in SEARCH_STOPWORDS }

            val hasAllowedIntent = ALLOWED_SEARCH_INTENTS.any { queryLower.contains(it) }
            val hasOfficialStrongMatch = ytmTracks.any { isStrongQueryMatch(it, cleanQuery) }

            val ranked = candidateList.sortedByDescending { track ->
                val titleLower = track.title.lowercase()
                val artistLower = track.artist.lowercase()
                val albumLower = track.album.lowercase()
                val rawTitleLower = (trackRawTitles[track.id] ?: track.title).lowercase()
                val combinedLower = "$titleLower $artistLower $rawTitleLower $albumLower"

                var score = 100

                // Exact title match boost
                if (titleLower == queryLower || rawTitleLower == queryLower) {
                    score += 80
                } else if (titleLower.startsWith(queryLower) || rawTitleLower.startsWith(queryLower)) {
                    score += 50
                } else if (titleLower.contains(queryLower) || rawTitleLower.contains(queryLower)) {
                    score += 30
                }

                // Token matching across title, artist, and raw video title
                var tokenMatches = 0
                for (t in queryTokens) {
                    if (titleLower.contains(t) || rawTitleLower.contains(t)) {
                        score += 25
                        tokenMatches++
                    } else if (artistLower.contains(t)) {
                        score += 20
                        tokenMatches++
                    } else if (combinedLower.contains(t)) {
                        score += 10
                        tokenMatches++
                    }
                }

                val matchedAllTokens = queryTokens.isNotEmpty() && tokenMatches >= queryTokens.size
                val isOfficialStudio = !track.album.startsWith("YouTube") && !track.album.contains("Uploaded by")

                if (hasOfficialStrongMatch) {
                    // Official studio release exists: Keep studio tracks ranked at #1
                    if (isOfficialStudio) {
                        score += 120
                    } else {
                        // Heavily penalize fan-upload / lyrics / translation videos and low-view uploads
                        val hasSpamTag = FAN_TRANSLATION_SPAM.any {
                            titleLower.contains(it) || rawTitleLower.contains(it)
                        }
                        if (hasSpamTag) {
                            score -= 300
                        }
                        val v = trackViewCounts[track.id] ?: 0L
                        if (v < 50_000L) {
                            score -= 200
                        }
                    }
                } else {
                    // NO official YTM song matches all query tokens (unreleased/rare track e.g. "American Wedding"):
                    // Promote top matching YouTube Video straight into TOP SONG (#1)!
                    if (matchedAllTokens) {
                        score += 180
                    }
                    if (isOfficialStudio && !matchedAllTokens) {
                        score -= 50
                    }
                }

                // High view count boost for video candidates (indicates primary viral/authoritative upload)
                val views = trackViewCounts[track.id] ?: 0L
                if (views > 10_000_000L) {
                    score += 40
                } else if (views > 1_000_000L) {
                    score += 25
                } else if (views > 100_000L) {
                    score += 10
                }

                // Studio original boost: official artist / topic / clean album
                if (artistLower.endsWith("- topic") || artistLower.contains("topic")) {
                    score += 25
                }
                if (albumLower.isNotBlank() && !albumLower.contains("live") && !albumLower.contains("session")) {
                    score += 15
                }

                // Heavily penalize non-studio terms (live, cover, karaoke) unless user searched for it
                if (!hasAllowedIntent) {
                    for (pen in PENALIZED_SEARCH_TERMS) {
                        if (titleLower.contains(pen) || albumLower.contains(pen) || rawTitleLower.contains(pen)) {
                            score -= 80
                        }
                    }
                }

                score
            }

            val deduped = TrackUtils.deduplicateTracks(ranked)
            // Pre-cache videoIds so YouTubeAudioExtractor immediately knows the stream source
            for (track in deduped) {
                if (track.id.length == 11) {
                    YouTubeAudioExtractor.cacheTrackVideoId(track.id, track.id)
                }
            }

            Log.d(TAG, "Returning ${deduped.size} ranked tracks for query '$cleanQuery' (Top: '${deduped.firstOrNull()?.title}' by '${deduped.firstOrNull()?.artist}', isStrongOfficial=$hasOfficialStrongMatch)")
            Result.success(deduped)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching tracks for query '$cleanQuery': ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun searchYtm(query: String, params: String): List<Track> {
        return try {
            val searchApiUrl = "https://music.youtube.com/youtubei/v1/search"
            val payload = JsonObject().apply {
                val contextObj = JsonObject().apply {
                    val clientObj = JsonObject().apply {
                        addProperty("clientName", "WEB_REMIX")
                        addProperty("clientVersion", "1.20231201.01.00")
                        addProperty("hl", "en")
                        addProperty("gl", "US")
                    }
                    add("client", clientObj)
                }
                add("context", contextObj)
                addProperty("query", query)
                addProperty("params", params)
            }

            val reqBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(searchApiUrl)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Origin", "https://music.youtube.com")
                .post(reqBody)
                .build()

            val responseBody = httpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return emptyList()

            val root = gson.fromJson(responseBody, JsonObject::class.java)
            val items = mutableListOf<JsonObject>()
            findMusicResponsiveListItemRenderers(root, items)

            val tracks = mutableListOf<Track>()
            for (r in items) {
                var vid = try {
                    r.getAsJsonObject("playlistItemData")?.get("videoId")?.asString ?: ""
                } catch (_: Exception) { "" }

                if (vid.isBlank()) {
                    vid = try {
                        r.getAsJsonObject("overlay")
                            ?.getAsJsonObject("musicItemThumbnailOverlayRenderer")
                            ?.getAsJsonObject("content")
                            ?.getAsJsonObject("musicPlayButtonRenderer")
                            ?.getAsJsonObject("playNavigationEndpoint")
                            ?.getAsJsonObject("watchEndpoint")
                            ?.get("videoId")?.asString ?: ""
                    } catch (_: Exception) { "" }
                }

                val cols = r.getAsJsonArray("flexColumns") ?: continue
                if (cols.size() == 0) continue

                if (vid.isBlank()) {
                    vid = try {
                        cols.get(0).asJsonObject
                            .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                            ?.getAsJsonObject("text")
                            ?.getAsJsonArray("runs")?.get(0)?.asJsonObject
                            ?.getAsJsonObject("navigationEndpoint")
                            ?.getAsJsonObject("watchEndpoint")
                            ?.get("videoId")?.asString ?: ""
                    } catch (_: Exception) { "" }
                }

                if (vid.isBlank() || vid.length != 11) continue

                val title = try {
                    cols.get(0).asJsonObject
                        .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.getAsJsonObject("text")
                        ?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: ""
                } catch (_: Exception) { "" }

                if (title.isBlank()) continue

                var artist = ""
                var album = ""
                var durationText = ""

                if (cols.size() > 1) {
                    try {
                        val runs = cols.get(1).asJsonObject
                            .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                            ?.getAsJsonObject("text")
                            ?.getAsJsonArray("runs")

                        if (runs != null && runs.size() > 0) {
                            val parts = mutableListOf<String>()
                            for (i in 0 until runs.size()) {
                                val t = runs.get(i).asJsonObject.get("text")?.asString ?: ""
                                if (t.isNotBlank() && t != " • " && t != "•") {
                                    parts.add(t)
                                }
                            }
                            if (parts.isNotEmpty()) {
                                artist = parts[0]
                            }
                            if (parts.size >= 3) {
                                album = parts[1]
                                durationText = parts[2]
                            } else if (parts.size == 2) {
                                if (parts[1].contains(":")) {
                                    durationText = parts[1]
                                } else {
                                    album = parts[1]
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                var artworkUrl = try {
                    r.getAsJsonObject("thumbnail")
                        ?.getAsJsonObject("musicThumbnailRenderer")
                        ?.getAsJsonObject("thumbnail")
                        ?.getAsJsonArray("thumbnails")
                        ?.lastOrNull()?.asJsonObject
                        ?.get("url")?.asString ?: ""
                } catch (_: Exception) { "" }

                if (artworkUrl.isNotBlank()) {
                    artworkUrl = artworkUrl
                        .replace(Regex("=w\\d+-h\\d+.*"), "=w544-h544-l90-rj")
                        .replace("=s60", "=s544")
                        .replace("=s120", "=s544")
                }

                val durationMs = parseDurationSeconds(durationText)

                tracks.add(
                    Track(
                        id = vid,
                        title = title,
                        artist = artist.ifBlank { "Unknown Artist" },
                        album = album,
                        durationMs = durationMs,
                        artworkUrl = artworkUrl
                    )
                )
            }
            tracks
        } catch (e: Exception) {
            Log.w(TAG, "YTM search failed: ${e.message}")
            emptyList()
        }
    }

    private fun searchYtmSongs(query: String): List<Track> =
        searchYtm(query, "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")

    private fun searchYtmVideos(query: String): List<Track> =
        searchYtm(query, "EgWKAQIQAWoKEAkQBRAKEAMQBA%3D%3D")

    private fun searchYouTubeWebVideos(query: String): List<YouTubeWebVideoResult> {
        return try {
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
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .post(reqBody)
                .build()

            val responseBody = httpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return emptyList()

            val root = gson.fromJson(responseBody, JsonObject::class.java)
            val renderers = mutableListOf<JsonObject>()
            findVideoRenderers(root, renderers)

            val results = mutableListOf<YouTubeWebVideoResult>()
            for (r in renderers) {
                val vid = r.get("videoId")?.asString ?: continue
                if (vid.length != 11) continue

                val rawTitle = try {
                    r.getAsJsonObject("title")?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: ""
                } catch (_: Exception) { "" }

                if (rawTitle.isBlank()) continue

                val rawTitleLower = rawTitle.lowercase()
                // Filter out non-song videos: reactions, interviews, tutorials, reviews, etc.
                if (REJECTED_VIDEO_KEYWORDS.any { rawTitleLower.contains(it) }) {
                    continue
                }

                val uploader = try {
                    r.getAsJsonObject("ownerText")?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: ""
                } catch (_: Exception) { "YouTube" }

                val durationText = try {
                    r.getAsJsonObject("lengthText")?.get("simpleText")?.asString ?: ""
                } catch (_: Exception) { "" }

                val durationMs = parseDurationSeconds(durationText)
                // Filter out Shorts (< 45s) and full-length podcasts / hour-long sets (> 12 mins)
                if (durationMs > 0 && (durationMs < 45_000L || durationMs > 720_000L)) {
                    continue
                }

                val viewCountText = try {
                    r.getAsJsonObject("viewCountText")?.get("simpleText")?.asString
                        ?: r.getAsJsonObject("shortViewCountText")?.get("simpleText")?.asString
                        ?: ""
                } catch (_: Exception) { "" }
                val viewCount = parseViewCount(viewCountText)

                val artworkUrl = try {
                    r.getAsJsonObject("thumbnail")?.getAsJsonArray("thumbnails")?.lastOrNull()?.asJsonObject?.get("url")?.asString
                        ?: "https://i.ytimg.com/vi/$vid/hqdefault.jpg"
                } catch (_: Exception) { "https://i.ytimg.com/vi/$vid/hqdefault.jpg" }

                val parsed = parseVideoTitleAndArtist(rawTitle, uploader)

                val track = Track(
                    id = vid,
                    title = parsed.title,
                    artist = parsed.artist,
                    album = parsed.album,
                    durationMs = durationMs,
                    artworkUrl = artworkUrl
                )
                results.add(YouTubeWebVideoResult(track, rawTitle, viewCount))
            }
            results
        } catch (e: Exception) {
            Log.w(TAG, "YouTube Web videos search failed: ${e.message}")
            emptyList()
        }
    }

    private fun findMusicResponsiveListItemRenderers(obj: JsonElement, out: MutableList<JsonObject>) {
        when {
            obj.isJsonObject -> {
                val jObj = obj.asJsonObject
                if (jObj.has("musicResponsiveListItemRenderer") && jObj.get("musicResponsiveListItemRenderer").isJsonObject) {
                    out.add(jObj.getAsJsonObject("musicResponsiveListItemRenderer"))
                }
                for (entry in jObj.entrySet()) {
                    findMusicResponsiveListItemRenderers(entry.value, out)
                }
            }
            obj.isJsonArray -> {
                for (item in obj.asJsonArray) {
                    findMusicResponsiveListItemRenderers(item, out)
                }
            }
        }
    }

    private fun findVideoRenderers(obj: JsonElement, out: MutableList<JsonObject>) {
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
}
