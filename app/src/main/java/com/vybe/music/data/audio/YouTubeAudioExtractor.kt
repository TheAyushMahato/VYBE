package com.vybe.music.data.audio

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.vybe.music.data.api.MusicApiService
import com.vybe.music.model.AudioQuality
import com.vybe.music.model.AudioTrackFormatInfo
import com.vybe.music.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

data class StreamCandidate(
    val url: String,
    val itag: Int,
    val mimeType: String,
    val bitrate: Int,
    val sampleRate: Int,
    val source: String,
    val label: String
)

/**
 * Extracts and resolves the highest possible audio quality stream for tracks.
 * - Extracts YouTube InnerTube unthrottled adaptive audio streams.
 * - Prioritizes standard, universally playable streams: Opus (itag 251, 48kHz) -> AAC (itag 140, 44.1kHz).
 * - Avoids blindly picking Premium-only or restricted itags (774/141).
 * - Automatically falls back to high-fidelity 320kbps official studio streams or local URIs if playback fails.
 */
object YouTubeAudioExtractor {
    private const val TAG = "VYBE_PLAYER"
    private const val DES_KEY = "38346591"
    const val YOUTUBE_STREAM_USER_AGENT = "Mozilla/5.0 (Linux; Android 12; Quest 3)"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val gson = Gson()
    data class CachedStream(val url: String, val expiresAtMillis: Long)
    private val streamUrlCache = ConcurrentHashMap<String, CachedStream>()
    private const val STREAM_CACHE_TTL_MS = 2 * 60 * 60 * 1000L // 2 hours
    private val audioStreamCache = ConcurrentHashMap<String, String>()
    private val trackVideoIdCache = ConcurrentHashMap<String, String>()
    private val candidateDetailsCache = ConcurrentHashMap<String, Candidate>()
    private val formatInfoCache = ConcurrentHashMap<String, AudioTrackFormatInfo>()
    private val trackCandidatesMap = ConcurrentHashMap<String, List<StreamCandidate>>()
    private val currentCandidateIndexMap = ConcurrentHashMap<String, Int>()

    val REJECTED_UNOFFICIAL_KEYWORDS = listOf(
        "sped up", "speed up", "slowed", "reverb", "nightcore", "8d", "tiktok", "chipmunk",
        "pitched", "remix", "cover", "karaoke", "instrumental", "lyrics video", "live", "loop", "1 hour"
    )

    val VERIFIED_RECORD_LABELS = listOf(
        "t-series", "tseries", "zee music", "sony music", "indie music label", "yrf", "yash raj",
        "speed records", "tips official", "tips music", "saregama", "universal music",
        "warner music", "vevo", "atlantic records", "columbia records", "def jam",
        "interscope", "republic records", "spinnin'", "monstercat", "ultra music"
    )

    fun isUnofficialUpload(title: String, artist: String): Boolean {
        val lowTitle = title.lowercase()
        val lowArtist = artist.lowercase()
        for (kw in REJECTED_UNOFFICIAL_KEYWORDS) {
            if (lowTitle.contains(kw) || lowArtist.contains(kw)) return true
        }
        return false
    }

    fun splitArtists(artist: String): List<String> {
        return artist.split(Regex("(?i)\\s+(?:&|feat\\.?|ft\\.?|x|,|and)\\s+|[,&/•;]"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun normalizeTokens(text: String): List<String> {
        val clean = text.lowercase()
            .replace(Regex("""\([^)]*\)"""), " ")
            .replace(Regex("""\[[^]]*]"""), " ")
            .replace("'", "")
            .replace("’", "")
            .replace("-", " ")
            .replace(Regex("""[^\p{L}\p{Nd}\s]"""), " ")
        val stopWords = setOf("the", "a", "an", "feat", "ft", "featuring", "official", "audio", "video", "music", "song", "lyrics", "full", "hd", "from", "and", "&")
        return clean.split(Regex("""\s+""")).filter { it.length > 1 && it !in stopWords }
    }

    private fun validateCandidate(
        candidateTitle: String,
        candidateChannel: String,
        requestedTitle: String,
        requestedArtist: String,
        isRelaxed: Boolean = false
    ): Boolean {
        if (isUnofficialUpload(candidateTitle, candidateChannel)) {
            val requestedLower = requestedTitle.lowercase()
            val hasExplicit = REJECTED_UNOFFICIAL_KEYWORDS.any { kw ->
                requestedLower.contains(kw) && candidateTitle.lowercase().contains(kw)
            }
            if (!hasExplicit) {
                return false
            }
        }
        val titleTokens = normalizeTokens(requestedTitle)
        val artistsList = splitArtists(requestedArtist)
        val lowTitle = candidateTitle.lowercase()
        val lowChannel = candidateChannel.lowercase()

        // 1. Title verification
        if (titleTokens.isNotEmpty()) {
            val matchingTitleTokens = titleTokens.count { lowTitle.contains(it) }
            if (matchingTitleTokens == 0) {
                return false
            }
            if (!isRelaxed && titleTokens.size >= 2 && matchingTitleTokens < (titleTokens.size + 1) / 2) {
                return false
            }
        }

        if (isRelaxed) {
            return true
        }

        // 2. Artist verification: accept match if ANY split artist matches channel or title
        val allArtistTokens = artistsList.flatMap { normalizeTokens(it) }.distinct()
        if (allArtistTokens.isNotEmpty()) {
            val isTopicChannel = lowChannel.contains("topic")
            val isMajorLabel = VERIFIED_RECORD_LABELS.any { lowChannel.contains(it) }
            val matchingArtistTokens = allArtistTokens.count { lowChannel.contains(it) || lowTitle.contains(it) }

            if (isTopicChannel) {
                val isAnyArtistTopic = artistsList.any { artist ->
                    val aTokens = normalizeTokens(artist)
                    aTokens.isNotEmpty() && aTokens.all { lowChannel.contains(it) }
                }
                if (!isAnyArtistTopic && matchingArtistTokens == 0) {
                    Log.d(TAG, "Rejecting wrong topic channel: '$candidateChannel' for artists '$requestedArtist'")
                    return false
                }
            } else if (!isMajorLabel) {
                if (matchingArtistTokens == 0) {
                    Log.d(TAG, "Rejecting candidate lacking any requested artist '$requestedArtist': '$candidateTitle' by '$candidateChannel'")
                    return false
                }
            } else {
                val titleTokensMatch = titleTokens.count { lowTitle.contains(it) }
                if (matchingArtistTokens == 0 && titleTokensMatch < titleTokens.size) {
                    return false
                }
            }
        }

        return true
    }

    private fun computeCandidateScore(
        candidateTitle: String,
        candidateChannel: String,
        durationSeconds: Int,
        requestedTitle: String,
        requestedArtist: String,
        targetDurationSeconds: Int,
        isYtm: Boolean = false
    ): Int {
        var score = if (isYtm) 40 else 0
        val lowTitle = candidateTitle.lowercase()
        val lowChannel = candidateChannel.lowercase()
        val titleTokens = normalizeTokens(requestedTitle)
        val artistTokens = normalizeTokens(requestedArtist)

        val isTopicChannel = lowChannel.contains("topic")
        val isMajorLabel = VERIFIED_RECORD_LABELS.any { lowChannel.contains(it) }

        val channelMatchesArtist = artistTokens.any { lowChannel.contains(it) }
        val titleMatchesArtist = artistTokens.any { lowTitle.contains(it) }

        if (isTopicChannel && channelMatchesArtist) {
            score += 90
        } else if (isMajorLabel && (channelMatchesArtist || titleMatchesArtist)) {
            score += 75
        } else if (channelMatchesArtist) {
            score += 50
        } else if (titleMatchesArtist) {
            score += 35
        }

        if (lowChannel.contains("vevo")) score += 25

        val matchingTitleTokens = titleTokens.count { lowTitle.contains(it) }
        if (titleTokens.isNotEmpty() && matchingTitleTokens == titleTokens.size) {
            score += 50
        } else if (matchingTitleTokens > 0) {
            score += 25 * matchingTitleTokens / maxOf(1, titleTokens.size)
        }

        if (lowTitle.contains("official audio") || lowTitle.contains("original audio")) {
            score += 25
        } else if (lowTitle.contains("official video") || lowTitle.contains("music video")) {
            score += 15
        } else if (lowTitle.contains("audio")) {
            score += 10
        }

        if (targetDurationSeconds > 30 && durationSeconds > 0) {
            val diff = Math.abs(durationSeconds - targetDurationSeconds)
            if (diff <= 3) score += 50
            else if (diff <= 8) score += 35
            else if (diff <= 15) score += 20
            else if (diff > 45) score -= 40
        }

        return score
    }

    fun isPreviewUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val u = url.lowercase()
        return u.contains("audio-ssl.itunes.apple.com") ||
                u.contains("apple.com/itunes-assets") ||
                u.contains("preview") ||
                u.contains("cdns-preview") || // Deezer preview
                u.contains(".plus.aac.p.m4a") ||
                u.contains("/preview/") ||
                u.contains("_preview") ||
                u.startsWith("vybe://unresolved")
    }

    fun getCachedVideoId(trackId: String): String? = trackVideoIdCache[trackId]

    fun cacheTrackVideoId(trackId: String, videoId: String) {
        if (trackId.isNotBlank() && videoId.isNotBlank()) {
            trackVideoIdCache[trackId] = videoId
        }
    }

    fun getCachedAudioStream(trackId: String): String? {
        val entry = streamUrlCache[trackId]
        if (entry != null && System.currentTimeMillis() < entry.expiresAtMillis) {
            return entry.url
        }
        return audioStreamCache[trackId]
    }

    fun clearCache() {
        streamUrlCache.clear()
        trackVideoIdCache.clear()
        audioStreamCache.clear()
        formatInfoCache.clear()
        trackCandidatesMap.clear()
        currentCandidateIndexMap.clear()
        Log.i(TAG, "Cleared YouTubeAudioExtractor cache")
    }

    fun getCachedTrackFormat(trackId: String): AudioTrackFormatInfo? = formatInfoCache[trackId]

    fun getNextFallbackStream(trackId: String, failedHttpCode: Int = -1): StreamCandidate? {
        val candidates = trackCandidatesMap[trackId] ?: return null
        val currentIdx = currentCandidateIndexMap[trackId] ?: 0
        val currentCandidate = candidates.getOrNull(currentIdx)

        // If a YouTube stream failed with HTTP 403 (e.g. IP-binding or token challenge),
        // all YouTube streams for this video ID will fail with 403. Skip directly to non-YouTube streams.
        val skipYouTube = failedHttpCode == 403 && currentCandidate?.source == "YOUTUBE"

        var nextIdx = currentIdx + 1
        if (skipYouTube) {
            while (nextIdx < candidates.size && candidates[nextIdx].source == "YOUTUBE") {
                nextIdx++
            }
        }

        if (nextIdx < candidates.size) {
            currentCandidateIndexMap[trackId] = nextIdx
            val next = candidates[nextIdx]
            formatInfoCache[trackId] = AudioTrackFormatInfo(
                codec = next.label.substringBefore(" •"),
                bitrateKbps = next.bitrate,
                sampleRateHz = next.sampleRate,
                channelCount = 2,
                label = next.label
            )
            Log.i(TAG, "Selected fallback candidate #${nextIdx + 1}/${candidates.size} for track $trackId [source=${next.source}, itag=${next.itag}, mime=${next.mimeType}, bitrate=${next.bitrate}kbps]: ${next.url}")
            return next
        }
        return null
    }

    suspend fun preloadStream(track: Track, quality: AudioQuality = AudioQuality.MAX): String? = withContext(Dispatchers.IO) {
        extractAudioStream(track, quality)
    }

    suspend fun extractAudioStream(track: Track, quality: AudioQuality = AudioQuality.MAX): String? = withContext(Dispatchers.IO) {
        val cacheKey = "${track.title.trim().lowercase()}_${track.artist.trim().lowercase()}_${quality.name}"
        val now = System.currentTimeMillis()
        val cached = streamUrlCache[cacheKey] ?: streamUrlCache[track.id]
        if (cached != null && now < cached.expiresAtMillis) {
            Log.d(TAG, "Returning cached 2-hr TTL audio stream for: ${track.title} [${quality.name}]")
            return@withContext cached.url
        }

        val candidatesList = mutableListOf<StreamCandidate>()

        // 1. Direct official studio 320kbps audio extraction with DES stream decryption (High-Fi 320kbps Studio Master)
        // Skip for explicit YouTube video uploads to avoid wrong studio match or slow fallback
        val isExplicitYouTubeVideo = track.album.startsWith("YouTube") || track.album.contains("Uploaded by")
        if (!isExplicitYouTubeVideo) {
            try {
                val directStream = resolveOfficialStudioStream(track.title, track.artist)
                if (!directStream.isNullOrBlank()) {
                    candidatesList.add(
                        StreamCandidate(
                            url = directStream,
                            itag = 141,
                            mimeType = "audio/mp4",
                            bitrate = 320,
                            sampleRate = 44100,
                            source = "STUDIO_320",
                            label = "AAC • 320 kbps • 44.1 kHz"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Official studio stream extraction failed for '${track.title}': ${e.message}")
            }
        }

        // 2. YouTube InnerTube Adaptive Audio Streams (Opus 48kHz / AAC 44.1kHz)
        try {
            val ytStreams = extractAllYouTubeAdaptiveStreams(track, quality)
            for (yt in ytStreams) {
                val eff = if (yt.averageBitrate > 0) yt.averageBitrate else yt.bitrate
                val kbps = (eff / 1000).coerceAtLeast(32)
                val codec = if (yt.isOpus) "Opus" else "AAC"
                val rateStr = if (yt.sampleRate % 1000 == 0) "${yt.sampleRate / 1000} kHz" else "%.1f kHz".format(yt.sampleRate / 1000.0)
                candidatesList.add(
                    StreamCandidate(
                        url = yt.url,
                        itag = yt.itag,
                        mimeType = yt.mimeType,
                        bitrate = kbps,
                        sampleRate = yt.sampleRate,
                        source = "YOUTUBE",
                        label = "$codec • $kbps kbps • $rateStr"
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "YouTube adaptive audio extraction error for '${track.title}': ${e.message}")
        }

        // 3. Fall back to existing track mediaUri ONLY if it is a genuine full-length stream, NEVER a 30s preview
        if (track.mediaUri.isNotBlank() && !isPreviewUrl(track.mediaUri)) {
            val is320 = track.mediaUri.contains("_320")
            candidatesList.add(
                StreamCandidate(
                    url = track.mediaUri,
                    itag = 140,
                    mimeType = "audio/mp4",
                    bitrate = if (is320) 320 else 256,
                    sampleRate = 44100,
                    source = "ORIGINAL",
                    label = if (is320) "AAC • 320 kbps • 44.1 kHz" else "AAC • 256 kbps • 44.1 kHz"
                )
            )
        }

        if (candidatesList.isEmpty()) {
            Log.e(TAG, "No audio stream candidates found for '${track.title}'")
            return@withContext null
        }

        // Strictly filter candidates so the main audio player ONLY loads valid audio streams:
        // mimeType.startsWith("audio/") or muxed progressive streams with audio — NEVER a video-only adaptive stream
        val validAudioCandidates = candidatesList.filter { candidate ->
            val mime = candidate.mimeType.lowercase()
            mime.startsWith("audio/") || (mime.startsWith("video/") && (mime.contains("mp4a") || mime.contains("opus")))
        }

        if (validAudioCandidates.isEmpty()) {
            Log.e(TAG, "No valid audio-bearing stream candidates found for '${track.title}'")
            return@withContext null
        }

        trackCandidatesMap[track.id] = validAudioCandidates
        currentCandidateIndexMap[track.id] = 0

        val chosen = validAudioCandidates.first()
        val ttl = System.currentTimeMillis() + STREAM_CACHE_TTL_MS
        streamUrlCache[cacheKey] = CachedStream(chosen.url, ttl)
        streamUrlCache[track.id] = CachedStream(chosen.url, ttl)
        audioStreamCache[cacheKey] = chosen.url
        audioStreamCache[track.id] = chosen.url
        formatInfoCache[track.id] = AudioTrackFormatInfo(
            codec = chosen.label.substringBefore(" •"),
            bitrateKbps = chosen.bitrate,
            sampleRateHz = chosen.sampleRate,
            channelCount = 2,
            label = chosen.label
        )

        Log.i(TAG, "Resolved stream for '${track.title}' [source=${chosen.source}, itag=${chosen.itag}, mime=${chosen.mimeType}, bitrate=${chosen.bitrate}kbps]: ${chosen.url}")
        return@withContext chosen.url
    }

    private data class Candidate(val videoId: String, val title: String, val channel: String, val durationSeconds: Int, val score: Int)

    private data class AdaptiveAudioStream(
        val itag: Int,
        val mimeType: String,
        val bitrate: Int,
        val averageBitrate: Int,
        val sampleRate: Int,
        val url: String,
        val isOpus: Boolean,
        val isAac: Boolean
    )

    private fun extractAllYouTubeAdaptiveStreams(track: Track, quality: AudioQuality): List<AdaptiveAudioStream> {
        val directId = if (track.id.length == 11 && track.id.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) track.id else null
        val cachedVid = trackVideoIdCache[track.id]
            ?: directId
            ?: trackVideoIdCache["${track.title.trim().lowercase()}|${track.artist.trim().lowercase()}"]

        val videoId = if (directId != null) {
            trackVideoIdCache[track.id] = directId
            directId
        } else if (!cachedVid.isNullOrBlank()) {
            val cachedCandidate = candidateDetailsCache[cachedVid]
            val isCacheValid = if (cachedCandidate != null) {
                validateCandidate(cachedCandidate.title, cachedCandidate.channel, track.title, track.artist)
            } else {
                true
            }
            if (isCacheValid) cachedVid else null
        } else {
            null
        } ?: run {
            val targetDurationSeconds = (track.durationMs / 1000).toInt()
            trackVideoIdCache.remove(track.id)
            trackVideoIdCache.remove("${track.title.trim().lowercase()}|${track.artist.trim().lowercase()}")

            // Pass 1: Strict search
            val ytmCandidate = searchInnerTubeSongs(track.title, track.artist, targetDurationSeconds)
            var candidate = ytmCandidate ?: (
                searchCandidates("${track.title} ${track.artist} audio", track.title, track.artist, targetDurationSeconds, isRelaxed = false)
                    ?: searchCandidates("${track.title} ${track.artist} official audio", track.title, track.artist, targetDurationSeconds, isRelaxed = false)
                    ?: searchCandidates("${track.title} ${track.artist}", track.title, track.artist, targetDurationSeconds, isRelaxed = false)
            )

            // Pass 2: Relaxed fallback (never return null if real candidate exists)
            if (candidate == null) {
                Log.d(TAG, "Pass 1 strict search yielded no candidate for '${track.title}'. Trying Pass 2 relaxed search...")
                candidate = searchCandidates("${track.title} ${track.artist}", track.title, track.artist, targetDurationSeconds, isRelaxed = true)
                    ?: searchCandidates(track.title, track.title, track.artist, targetDurationSeconds, isRelaxed = true)
            }

            if (candidate == null) {
                Log.w(TAG, "No YouTube candidate found for: ${track.title} ${track.artist}")
                return emptyList()
            } else {
                trackVideoIdCache[track.id] = candidate.videoId
                trackVideoIdCache["${track.title.trim().lowercase()}|${track.artist.trim().lowercase()}"] = candidate.videoId
                candidateDetailsCache[candidate.videoId] = candidate
                candidate.videoId
            }
        }

        // Step 1: GET watch page for visitorData
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
        val visitorData = if (vMatcher.find()) vMatcher.group(1) else null

        // Step 2: POST player API using ANDROID_VR client (Oculus Quest 3)
        val playerUrl = "https://www.youtube.com/youtubei/v1/player"
        val payload = JsonObject().apply {
            val contextObj = JsonObject().apply {
                val clientObj = JsonObject().apply {
                    addProperty("clientName", "ANDROID_VR")
                    addProperty("clientVersion", "1.65.10")
                    addProperty("deviceMake", "Oculus")
                    addProperty("deviceModel", "Quest 3")
                    addProperty("osName", "Android")
                    addProperty("osVersion", "12")
                }
                add("client", clientObj)
            }
            add("context", contextObj)
            addProperty("videoId", videoId)
        }

        val requestBuilder = Request.Builder()
            .url(playerUrl)
            .header("Content-Type", "application/json")
            .header("User-Agent", YOUTUBE_STREAM_USER_AGENT)
            .header("X-Youtube-Client-Name", "28")
            .header("X-Youtube-Client-Version", "1.65.10")

        if (!visitorData.isNullOrBlank()) {
            requestBuilder.header("X-Goog-Visitor-Id", visitorData)
        }

        val reqBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        requestBuilder.post(reqBody)

        val playerResponse = httpClient.newCall(requestBuilder.build()).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        } ?: return emptyList()

        val pRoot = try {
            gson.fromJson(playerResponse, JsonObject::class.java)
        } catch (e: Exception) {
            return emptyList()
        }

        val streamingData = pRoot.getAsJsonObject("streamingData") ?: return emptyList()
        val adaptiveFormats = streamingData.getAsJsonArray("adaptiveFormats") ?: return emptyList()

        val audioCandidates = mutableListOf<AdaptiveAudioStream>()

        for (elem in adaptiveFormats) {
            if (!elem.isJsonObject) continue
            val f = elem.asJsonObject
            val mimeType = f.get("mimeType")?.asString ?: ""
            // Filter strictly for audio-only adaptive streams (never muxed video+audio)
            if (!mimeType.startsWith("audio/")) continue

            val itag = f.get("itag")?.asInt ?: -1
            val bitrate = f.get("bitrate")?.asInt ?: 0
            val averageBitrate = f.get("averageBitrate")?.asInt ?: bitrate
            val sampleRate = f.get("audioSampleRate")?.asString?.toIntOrNull() ?: 44100
            val url = f.get("url")?.asString ?: continue
            if (url.isBlank()) continue

            val isOpus = mimeType.contains("opus", ignoreCase = true) || itag == 251 || itag == 774 || itag == 249 || itag == 250
            val isAac = mimeType.contains("mp4a", ignoreCase = true) || itag == 140 || itag == 141 || itag == 139

            audioCandidates.add(
                AdaptiveAudioStream(
                    itag = itag,
                    mimeType = mimeType,
                    bitrate = bitrate,
                    averageBitrate = averageBitrate,
                    sampleRate = sampleRate,
                    url = url,
                    isOpus = isOpus,
                    isAac = isAac
                )
            )
        }

        // If no audio-only adaptive stream found, fall back to progressive formats that contain audio tracks
        if (audioCandidates.isEmpty()) {
            val formats = streamingData.getAsJsonArray("formats")
            if (formats != null) {
                for (elem in formats) {
                    if (!elem.isJsonObject) continue
                    val f = elem.asJsonObject
                    val mimeType = f.get("mimeType")?.asString ?: ""
                    val url = f.get("url")?.asString ?: continue
                    if (url.isBlank()) continue

                    val hasAudio = f.has("audioSampleRate") || f.has("audioQuality") || mimeType.contains("mp4a", ignoreCase = true)
                    if (hasAudio) {
                        val itag = f.get("itag")?.asInt ?: -1
                        val bitrate = f.get("bitrate")?.asInt ?: 128000
                        val sampleRate = f.get("audioSampleRate")?.asString?.toIntOrNull() ?: 44100
                        audioCandidates.add(
                            AdaptiveAudioStream(
                                itag = itag,
                                mimeType = mimeType,
                                bitrate = bitrate,
                                averageBitrate = bitrate,
                                sampleRate = sampleRate,
                                url = url,
                                isOpus = false,
                                isAac = true
                            )
                        )
                    }
                }
            }
        }

        if (audioCandidates.isEmpty()) return emptyList()

        // Sort audio streams prioritizing stable, universally hardware-decoded high-bitrate audio (141/140 M4A/AAC > 251 Opus)
        return when (quality) {
            AudioQuality.MAX, AudioQuality.HIGH -> {
                audioCandidates.sortedWith(
                    compareByDescending<AdaptiveAudioStream> { stream ->
                        val effBitrate = if (stream.averageBitrate > 0) stream.averageBitrate else stream.bitrate
                        when (stream.itag) {
                            141 -> 120_000_000 + effBitrate // Top: M4A/AAC 256kbps (universal hardware-decoded)
                            140 -> 100_000_000 + effBitrate // High: M4A/AAC 128kbps (universal hardware-decoded)
                            251 -> 80_000_000 + effBitrate  // High: Opus 160kbps (clean webm)
                            250 -> 40_000_000 + effBitrate
                            249 -> 20_000_000 + effBitrate
                            139 -> 10_000_000 + effBitrate
                            774 -> 5_000_000 + effBitrate
                            else -> effBitrate
                        }
                    }
                )
            }
            AudioQuality.DATA_SAVER -> {
                audioCandidates.sortedWith(
                    compareBy<AdaptiveAudioStream> { stream ->
                        if (stream.averageBitrate > 0) stream.averageBitrate else stream.bitrate
                    }
                )
            }
        }
    }

    private fun searchInnerTubeSongs(trackTitle: String, trackArtist: String, targetDurationSeconds: Int = 0): Candidate? {
        val query = "$trackTitle $trackArtist".trim()
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
            addProperty("params", "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
        }

        val reqBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(searchApiUrl)
            .header("Content-Type", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Origin", "https://music.youtube.com")
            .post(reqBody)
            .build()

        val responseBody = try {
            httpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "InnerTube songs search failed: ${e.message}")
            null
        } ?: return null

        val root = try {
            gson.fromJson(responseBody, JsonObject::class.java)
        } catch (e: Exception) { null } ?: return null

        val items = mutableListOf<JsonObject>()
        findMusicResponsiveListItemRenderers(root, items)

        val candidates = mutableListOf<Candidate>()
        val lowReqTitle = trackTitle.lowercase().trim()
        val lowReqArtist = trackArtist.lowercase().trim()

        for (r in items) {
            var vid = try {
                r.getAsJsonObject("playlistItemData")?.get("videoId")?.asString ?: ""
            } catch (e: Exception) { "" }
            if (vid.isBlank()) {
                vid = try {
                    r.getAsJsonObject("overlay")
                        ?.getAsJsonObject("musicItemThumbnailOverlayRenderer")
                        ?.getAsJsonObject("content")
                        ?.getAsJsonObject("musicPlayButtonRenderer")
                        ?.getAsJsonObject("playNavigationEndpoint")
                        ?.getAsJsonObject("watchEndpoint")
                        ?.get("videoId")?.asString ?: ""
                } catch (e: Exception) { "" }
            }
            if (vid.isBlank() || vid.length != 11) continue

            val cols = r.getAsJsonArray("flexColumns") ?: continue
            val title = try {
                cols.get(0).asJsonObject
                    .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.getAsJsonObject("text")
                    ?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: ""
            } catch (e: Exception) { "" }

            val channel = try {
                val runs = cols.get(1).asJsonObject
                    .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.getAsJsonObject("text")
                    ?.getAsJsonArray("runs")
                val sb = StringBuilder()
                if (runs != null) {
                    for (i in 0 until runs.size()) {
                        sb.append(runs.get(i).asJsonObject.get("text")?.asString ?: "")
                    }
                }
                sb.toString()
            } catch (e: Exception) { "" }

            if (!validateCandidate(title, channel, trackTitle, trackArtist)) {
                Log.d(TAG, "Rejected YTM candidate failing title/artist validation: '$title' by '$channel'")
                continue
            }

            var durationText = ""
            val fixedCols = r.getAsJsonArray("fixedColumns")
            if (fixedCols != null && fixedCols.size() > 0) {
                durationText = try {
                    fixedCols.get(0).asJsonObject
                        .getAsJsonObject("musicResponsiveListItemFixedColumnRenderer")
                        ?.getAsJsonObject("text")
                        ?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: ""
                } catch (e: Exception) { "" }
            }
            val durationSeconds = parseDurationSeconds(durationText)

            val score = computeCandidateScore(title, channel, durationSeconds, trackTitle, trackArtist, targetDurationSeconds, isYtm = true)
            candidates.add(Candidate(vid, title, channel, durationSeconds, score))
        }

        if (targetDurationSeconds > 30) {
            val closeMatch = candidates.filter { it.durationSeconds > 0 && Math.abs(it.durationSeconds - targetDurationSeconds) <= 15 }
            if (closeMatch.isNotEmpty()) {
                val best = closeMatch.maxByOrNull { it.score }
                if (best != null) {
                    Log.i(TAG, "YTM exact duration match: ${best.videoId} '${best.title}' (${best.durationSeconds}s vs target ${targetDurationSeconds}s)")
                    return best
                }
            }
        }

        val best = candidates.maxByOrNull { it.score }
        if (best != null) {
            Log.i(TAG, "YTM best scored candidate: ${best.videoId} '${best.title}' [score=${best.score}]")
        }
        return best
    }

    private fun searchCandidates(
        query: String,
        requestedTitle: String = "",
        requestedArtist: String = "",
        targetDurationSeconds: Int = 0,
        isRelaxed: Boolean = false
    ): Candidate? {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val searchUrl = "https://www.youtube.com/results?search_query=$encodedQuery"

        val request = Request.Builder()
            .url(searchUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()

        val html = try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()
            }
        } catch (e: Exception) {
            return null
        } ?: return null

        val jsonPattern = Pattern.compile("ytInitialData\\s*=\\s*(\\{.+?\\});")
        val matcher = jsonPattern.matcher(html)
        val jsonString = if (matcher.find()) {
            matcher.group(1)
        } else {
            val altPattern = Pattern.compile("var ytInitialData = (\\{.*?\\});</script>")
            val altMatcher = altPattern.matcher(html)
            if (altMatcher.find()) altMatcher.group(1) else null
        } ?: return null

        val root = try {
            gson.fromJson(jsonString, JsonObject::class.java)
        } catch (e: Exception) {
            return null
        }

        val renderers = mutableListOf<JsonObject>()
        findVideoRenderers(root, renderers)

        val candidates = mutableListOf<Candidate>()

        for (r in renderers) {
            val vid = r.get("videoId")?.asString ?: continue
            val title = try {
                r.getAsJsonObject("title")?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: ""
            } catch (e: Exception) { "" }

            val channel = try {
                r.getAsJsonObject("ownerText")?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: ""
            } catch (e: Exception) { "" }

            if (!validateCandidate(title, channel, requestedTitle, requestedArtist, isRelaxed)) {
                Log.d(TAG, "Rejected web candidate failing title/artist validation: '$title' by '$channel'")
                continue
            }

            // Parse video duration from lengthText (e.g. "3:45", "0:29", "12:30")
            val durationText = try {
                r.getAsJsonObject("lengthText")?.get("simpleText")?.asString
                    ?: r.getAsJsonObject("lengthText")?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString
                    ?: ""
            } catch (e: Exception) { "" }

            val durationSeconds = parseDurationSeconds(durationText)

            // Strictly filter out YouTube Shorts / teasers / preview clips under 60 seconds
            if (durationSeconds in 1..60) {
                Log.d(TAG, "Filtering out short/preview candidate (${durationText}): '$title'")
                continue
            }

            // Check if navigationEndpoint points to a Short
            val navUrl = try {
                r.getAsJsonObject("navigationEndpoint")?.getAsJsonObject("commandMetadata")?.getAsJsonObject("webCommandMetadata")?.get("url")?.asString ?: ""
            } catch (e: Exception) { "" }
            if (navUrl.contains("/shorts/")) continue

            val score = computeCandidateScore(title, channel, durationSeconds, requestedTitle, requestedArtist, targetDurationSeconds, isYtm = false)
            candidates.add(Candidate(vid, title, channel, durationSeconds, score))
        }

        if (targetDurationSeconds > 30) {
            val closeMatch = candidates.filter { it.durationSeconds > 0 && Math.abs(it.durationSeconds - targetDurationSeconds) <= 15 }
            if (closeMatch.isNotEmpty()) {
                return closeMatch.maxByOrNull { it.score }
            }
        }

        return candidates.maxByOrNull { it.score }
    }

    private fun parseDurationSeconds(text: String): Int {
        if (text.isBlank()) return 0
        val parts = text.trim().split(":")
        return try {
            when (parts.size) {
                3 -> parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
                2 -> parts[0].toInt() * 60 + parts[1].toInt()
                1 -> parts[0].toInt()
                else -> 0
            }
        } catch (e: Exception) { 0 }
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

    private fun findMusicResponsiveListItemRenderers(obj: com.google.gson.JsonElement, out: MutableList<JsonObject>) {
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

    private suspend fun resolveOfficialStudioStream(title: String, artist: String): String? {
        val searchQuery = "$title $artist".trim()
        val response = withTimeoutOrNull(3500L) {
            MusicApiService.instance.searchDirectSaavn(query = searchQuery, limit = 6)
        } ?: return null

        val results = response.results.orEmpty()
        if (results.isEmpty()) return null

        val cleanTitle = title.lowercase().trim()
        val cleanArtist = artist.lowercase()
            .split(",", "&", "feat.", "ft.", "x", "/")
            .firstOrNull()?.trim() ?: artist.lowercase()

        val bestMatch = results.firstOrNull { item ->
            val itemTitle = (item.song ?: "").lowercase()
            val itemArtist = (item.primaryArtists ?: "").lowercase()

            val titleMatches = itemTitle.contains(cleanTitle) || cleanTitle.contains(itemTitle)
            val artistMatches = itemArtist.contains(cleanArtist) || cleanArtist.contains(itemArtist)
            val isOfficial = !itemArtist.contains("tribute") &&
                    !itemTitle.contains("karaoke") &&
                    !itemTitle.contains("cover") &&
                    !itemArtist.contains("cover")

            titleMatches && artistMatches && isOfficial
        } ?: results.firstOrNull { item ->
            val itemTitle = (item.song ?: "").lowercase()
            val itemArtist = (item.primaryArtists ?: "").lowercase()
            val titleMatches = itemTitle.contains(cleanTitle) || cleanTitle.contains(itemTitle)
            val isOfficial = !itemTitle.contains("karaoke") &&
                    !itemTitle.contains("cover") &&
                    !itemArtist.contains("tribute")
            titleMatches && isOfficial
        } ?: return null

        val encrypted = bestMatch.encryptedMediaUrl ?: return null
        return decryptDesUrl(encrypted)
    }

    fun decryptDesUrl(encryptedBase64: String): String? {
        return try {
            val keyBytes = DES_KEY.toByteArray(Charsets.UTF_8)
            val keySpec = SecretKeySpec(keyBytes, "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)

            val decodedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            val rawUrl = String(decryptedBytes, Charsets.UTF_8).trim()

            // Upgrade to 320kbps MP4 full-length studio track
            rawUrl.replace("_96.mp4", "_320.mp4")
                .replace("_160.mp4", "_320.mp4")
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting DES audio URL: ${e.message}", e)
            null
        }
    }
}
