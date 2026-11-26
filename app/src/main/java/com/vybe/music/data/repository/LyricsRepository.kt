package com.vybe.music.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.vybe.music.data.audio.YouTubeAudioExtractor
import com.vybe.music.data.audio.YouTubeVideoExtractor
import com.vybe.music.model.LyricLine
import com.vybe.music.model.LyricsResult
import com.vybe.music.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object LyricsRepository {
    private const val TAG = "LyricsRepository"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val gson = Gson()
    private val memoryCache = ConcurrentHashMap<String, LyricsResult>()

    /**
     * Smart Query Sanitizer:
     * Strips noisy video/audio tags, remaster tags, feature tags, and Topic suffixes
     * so search hits the true canonical song title and primary artist.
     */
    fun sanitizeQuery(rawTitle: String, rawArtist: String): Pair<String, String> {
        var cleanTitle = rawTitle.trim()

        // 1. Remove text after pipe or hyphen separators containing noisy keywords
        cleanTitle = cleanTitle.replace(Regex("""\s*\|\s*.*$"""), "")
        cleanTitle = cleanTitle.replace(Regex("""(?i)\s*-\s*(?:Official|Lyric|Video|Audio|Topic|Full Song|HD|4K|Visualizer).*$"""), "")

        // 2. Remove parenthesized / bracketed noise
        val noisePattern = Regex(
            """(?i)\s*[\(\[](?:Official\s*(?:Music\s*)?(?:Video|Audio|HD|4K|Visualizer|Lyric\s*Video)|From\s*["'].*?["']|From\s+[^\)\]]+|Lyrics?|Audio|Remastered.*?|Slowed.*?|Reverb.*?|Live.*?|Full\s*Song|4K|HD|feat\.?.*?|ft\.?.*?|Explicit|Clean|Studio\s*Version)[\)\]]"""
        )
        cleanTitle = cleanTitle.replace(noisePattern, "")

        // 3. Fallback cleanup: remove double spaces
        cleanTitle = cleanTitle.replace(Regex("""\s{2,}"""), " ").trim()
        if (cleanTitle.isBlank()) cleanTitle = rawTitle.trim()

        // 4. Clean artist: remove - Topic and split featured artists
        var cleanArtist = rawArtist.trim()
        cleanArtist = cleanArtist.replace(Regex("""(?i)\s*-\s*Topic$"""), "")
        cleanArtist = cleanArtist.split(Regex("""(?i)\s+(?:&|feat\.?|ft\.?|x|,)\s+|[,&/•;]"""))
            .firstOrNull()?.trim()
            ?.takeIf { it.isNotBlank() } ?: cleanArtist

        return Pair(cleanTitle, cleanArtist)
    }

    /**
     * Parses standard LRC timestamped text [mm:ss.xx] into sorted List<LyricLine>.
     */
    fun parseLrc(lrcText: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val timeRegex = Pattern.compile("""\[(\d{1,2}):(\d{1,2})(?:\.(\d{1,3}))?\]""")

        for (rawLine in lrcText.lines()) {
            val line = rawLine.trim()
            if (line.isBlank()) continue

            // Skip ID tags e.g. [ar: Arijit Singh], [ti: Kesariya]
            if (line.matches(Regex("""^\[[a-zA-Z]+:[^\]]*\]$"""))) continue

            val matcher = timeRegex.matcher(line)
            val timestamps = mutableListOf<Long>()
            var lastEnd = 0

            while (matcher.find()) {
                val mins = matcher.group(1)?.toLongOrNull() ?: 0L
                val secs = matcher.group(2)?.toLongOrNull() ?: 0L
                val msStr = matcher.group(3) ?: "0"
                val ms = when (msStr.length) {
                    1 -> (msStr.toLongOrNull() ?: 0L) * 100L
                    2 -> (msStr.toLongOrNull() ?: 0L) * 10L
                    else -> msStr.take(3).toLongOrNull() ?: 0L
                }
                val totalMs = mins * 60_000L + secs * 1_000L + ms
                timestamps.add(totalMs)
                lastEnd = matcher.end()
            }

            val lyricText = line.substring(lastEnd).trim()
            for (ts in timestamps) {
                lines.add(LyricLine(startTimeMs = ts, text = lyricText))
            }
        }

        return lines.sortedBy { it.startTimeMs }
    }

    /**
     * Parses plain text lyrics into spaced LyricLines.
     */
    fun parsePlainText(plainText: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        var index = 0
        for (raw in plainText.lines()) {
            val trimmed = raw.trim()
            if (trimmed.isNotBlank()) {
                lines.add(LyricLine(startTimeMs = index * 4000L, text = trimmed))
                index++
            }
        }
        return lines
    }

    /**
     * Fetches lyrics using a 3-tier pipeline with disk & memory cache.
     */
    suspend fun getLyrics(
        context: Context,
        track: Track,
        forceRefresh: Boolean = false
    ): LyricsResult? = withContext(Dispatchers.IO) {
        val trackId = track.id

        // 1. Memory cache check
        if (!forceRefresh) {
            memoryCache[trackId]?.let { return@withContext it }

            // Disk cache check
            loadFromDisk(context, trackId)?.let {
                memoryCache[trackId] = it
                return@withContext it
            }
        }

        val (cleanTitle, cleanArtist) = sanitizeQuery(track.title, track.artist)
        Log.d(TAG, "Fetching lyrics for track '${track.title}' -> Sanitized: '$cleanTitle' by '$cleanArtist'")

        var plainCandidate: LyricsResult? = null

        // ── Tier 1: LRCLIB (Synced LRC) ──────────────────────────────────────────
        try {
            val durationSec = if (track.durationMs > 0L) track.durationMs / 1000L else 0L

            // Step 1a: Exact lookup
            val exactUrl = "https://lrclib.net/api/get?track_name=${urlEncode(cleanTitle)}&artist_name=${urlEncode(cleanArtist)}" +
                if (durationSec > 0) "&duration=$durationSec" else ""

            val exactResult = fetchLrclibResult(exactUrl, trackId)
            if (exactResult != null) {
                if (exactResult.isSynced && exactResult.lines.isNotEmpty()) {
                    saveToCache(context, exactResult)
                    return@withContext exactResult
                } else if (exactResult.lines.isNotEmpty()) {
                    plainCandidate = exactResult
                }
            }

            // Step 1b: Search fallback on LRCLIB
            val searchQueries = listOf(
                "https://lrclib.net/api/search?q=${urlEncode("$cleanTitle $cleanArtist")}",
                "https://lrclib.net/api/search?track_name=${urlEncode(cleanTitle)}"
            )

            for (sUrl in searchQueries) {
                val candidate = searchLrclib(sUrl, trackId, cleanTitle, cleanArtist, track.durationMs)
                if (candidate != null) {
                    if (candidate.isSynced && candidate.lines.isNotEmpty()) {
                        saveToCache(context, candidate)
                        return@withContext candidate
                    } else if (plainCandidate == null && candidate.lines.isNotEmpty()) {
                        plainCandidate = candidate
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Tier 1 LRCLIB error: ${e.message}")
        }

        // ── Tier 2: YouTube Music Official Lyrics (Next -> Browse) ──────────────
        try {
            val ytmResult = fetchYouTubeMusicLyrics(track, cleanTitle, cleanArtist)
            if (ytmResult != null && ytmResult.lines.isNotEmpty()) {
                saveToCache(context, ytmResult)
                return@withContext ytmResult
            }
        } catch (e: Exception) {
            Log.w(TAG, "Tier 2 YTM lyrics error: ${e.message}")
        }

        // ── Tier 3: Plain Lyrics Fallback (LRCLIB plain or Lyrics.ovh) ───────────
        if (plainCandidate != null) {
            saveToCache(context, plainCandidate)
            return@withContext plainCandidate
        }

        try {
            val ovhUrl = "https://api.lyrics.ovh/v1/${urlEncode(cleanArtist)}/${urlEncode(cleanTitle)}"
            val ovhResult = fetchLyricsOvh(ovhUrl, trackId)
            if (ovhResult != null && ovhResult.lines.isNotEmpty()) {
                saveToCache(context, ovhResult)
                return@withContext ovhResult
            }
        } catch (e: Exception) {
            Log.w(TAG, "Tier 3 Lyrics.ovh error: ${e.message}")
        }

        null
    }

    private fun fetchLrclibResult(url: String, trackId: String): LyricsResult? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "VYBE-Android/1.0")
            .build()

        return httpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            val root = gson.fromJson(body, JsonObject::class.java) ?: return null

            val syncedText = root.get("syncedLyrics")?.asString?.trim()
            if (!syncedText.isNullOrBlank()) {
                val lines = parseLrc(syncedText)
                if (lines.isNotEmpty()) {
                    return LyricsResult(trackId = trackId, lines = lines, isSynced = true, source = "LRCLIB")
                }
            }

            val plainText = root.get("plainLyrics")?.asString?.trim()
            if (!plainText.isNullOrBlank()) {
                val lines = parsePlainText(plainText)
                if (lines.isNotEmpty()) {
                    return LyricsResult(trackId = trackId, lines = lines, isSynced = false, source = "LRCLIB")
                }
            }

            null
        }
    }

    private fun searchLrclib(
        url: String,
        trackId: String,
        cleanTitle: String,
        cleanArtist: String,
        targetDurationMs: Long
    ): LyricsResult? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "VYBE-Android/1.0")
            .build()

        return httpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            val array = gson.fromJson(body, JsonArray::class.java) ?: return null
            if (array.size() == 0) return null

            var bestScore = -1
            var bestItem: JsonObject? = null

            val targetDurSec = if (targetDurationMs > 0) targetDurationMs / 1000L else 0L

            for (element in array) {
                if (!element.isJsonObject) continue
                val obj = element.asJsonObject
                val itemTitle = obj.get("trackName")?.asString.orEmpty().lowercase()
                val itemArtist = obj.get("artistName")?.asString.orEmpty().lowercase()
                val itemDur = obj.get("duration")?.asLong ?: 0L
                val hasSynced = !obj.get("syncedLyrics")?.asString.isNullOrBlank()

                var score = 0
                val titleLower = cleanTitle.lowercase()
                val artistLower = cleanArtist.lowercase()

                if (itemTitle == titleLower) score += 50
                else if (itemTitle.contains(titleLower) || titleLower.contains(itemTitle)) score += 25

                if (itemArtist == artistLower) score += 30
                else if (itemArtist.contains(artistLower) || artistLower.contains(itemArtist)) score += 15

                if (targetDurSec > 0 && itemDur > 0) {
                    val diff = kotlin.math.abs(itemDur - targetDurSec)
                    if (diff <= 12) score += 25
                    else if (diff <= 25) score += 10
                }

                if (hasSynced) score += 40

                if (score > bestScore) {
                    bestScore = score
                    bestItem = obj
                }
            }

            bestItem?.let { obj ->
                val syncedText = obj.get("syncedLyrics")?.asString?.trim()
                if (!syncedText.isNullOrBlank()) {
                    val lines = parseLrc(syncedText)
                    if (lines.isNotEmpty()) {
                        return LyricsResult(trackId = trackId, lines = lines, isSynced = true, source = "LRCLIB")
                    }
                }

                val plainText = obj.get("plainLyrics")?.asString?.trim()
                if (!plainText.isNullOrBlank()) {
                    val lines = parsePlainText(plainText)
                    if (lines.isNotEmpty()) {
                        return LyricsResult(trackId = trackId, lines = lines, isSynced = false, source = "LRCLIB")
                    }
                }
            }

            null
        }
    }

    private suspend fun fetchYouTubeMusicLyrics(
        track: Track,
        cleanTitle: String,
        cleanArtist: String
    ): LyricsResult? {
        // Resolve videoId: check mediaUri, cached videoId, or track.id
        var videoId = if (track.mediaUri.startsWith("vybe://yt/")) {
            track.mediaUri.removePrefix("vybe://yt/").trim()
        } else {
            YouTubeAudioExtractor.getCachedVideoId(track.id)
        }

        if (videoId.isNullOrBlank() && track.id.matches(Regex("""^[a-zA-Z0-9_-]{11}$"""))) {
            videoId = track.id
        }

        // If videoId is still missing, query YouTube search
        if (videoId.isNullOrBlank()) {
            videoId = YouTubeVideoExtractor.resolveSmartYouTubeVideoId(track)
        }

        if (videoId.isNullOrBlank()) return null

        // 1. Call next endpoint to get Lyrics browseId
        val nextUrl = "https://music.youtube.com/youtubei/v1/next"
        val nextPayload = JsonObject().apply {
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
            addProperty("videoId", videoId)
        }

        val reqBody = nextPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val nextRequest = Request.Builder()
            .url(nextUrl)
            .header("Content-Type", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .post(reqBody)
            .build()

        val nextJson = httpClient.newCall(nextRequest).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        } ?: return null

        val nextRoot = gson.fromJson(nextJson, JsonObject::class.java) ?: return null
        var lyricsBrowseId: String? = null

        val tabs = nextRoot.getAsJsonObject("contents")
            ?.getAsJsonObject("singleColumnMusicWatchNextResultsRenderer")
            ?.getAsJsonObject("tabbedRenderer")
            ?.getAsJsonObject("watchNextTabbedResultsRenderer")
            ?.getAsJsonArray("tabs")

        if (tabs != null) {
            for (tabElem in tabs) {
                val tabObj = tabElem.asJsonObject.getAsJsonObject("tabRenderer") ?: continue
                val bId = tabObj.getAsJsonObject("endpoint")
                    ?.getAsJsonObject("browseEndpoint")
                    ?.get("browseId")?.asString
                if (bId != null && bId.startsWith("MPLYt")) {
                    lyricsBrowseId = bId
                    break
                }
            }
        }

        if (lyricsBrowseId.isNullOrBlank()) return null

        // 2. Call browse endpoint with MPLYt browseId
        val browseUrl = "https://music.youtube.com/youtubei/v1/browse"
        val browsePayload = JsonObject().apply {
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
            addProperty("browseId", lyricsBrowseId)
        }

        val browseReq = Request.Builder()
            .url(browseUrl)
            .header("Content-Type", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .post(browsePayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val browseJson = httpClient.newCall(browseReq).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        } ?: return null

        val browseRoot = gson.fromJson(browseJson, JsonObject::class.java) ?: return null
        val contents = browseRoot.getAsJsonObject("contents")
            ?.getAsJsonObject("sectionListRenderer")
            ?.getAsJsonArray("contents") ?: return null

        for (c in contents) {
            val shelf = c.asJsonObject.getAsJsonObject("musicDescriptionShelfRenderer") ?: continue
            val runs = shelf.getAsJsonObject("description")?.getAsJsonArray("runs") ?: continue
            val sb = StringBuilder()
            for (r in runs) {
                sb.append(r.asJsonObject.get("text")?.asString.orEmpty())
            }
            val text = sb.toString().trim()
            if (text.isNotBlank()) {
                val lines = parsePlainText(text)
                if (lines.isNotEmpty()) {
                    return LyricsResult(trackId = track.id, lines = lines, isSynced = false, source = "YouTube Music")
                }
            }
        }

        return null
    }

    private fun fetchLyricsOvh(url: String, trackId: String): LyricsResult? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "VYBE-Android/1.0")
            .build()

        return httpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            val root = gson.fromJson(body, JsonObject::class.java) ?: return null
            val rawLyrics = root.get("lyrics")?.asString?.trim() ?: return null
            if (rawLyrics.isNotBlank()) {
                val lines = parsePlainText(rawLyrics)
                if (lines.isNotEmpty()) {
                    return LyricsResult(trackId = trackId, lines = lines, isSynced = false, source = "LyricsOVH")
                }
            }
            null
        }
    }

    // ── Cache Persistence ────────────────────────────────────────────────────────

    private fun getCacheDir(context: Context): File {
        val dir = File(context.filesDir, "lyrics_cache")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun loadFromDisk(context: Context, trackId: String): LyricsResult? {
        return try {
            val file = File(getCacheDir(context), "${cleanFileKey(trackId)}.json")
            if (file.exists()) {
                val json = file.readText()
                gson.fromJson(json, LyricsResult::class.java)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun saveToCache(context: Context, result: LyricsResult) {
        memoryCache[result.trackId] = result
        try {
            val file = File(getCacheDir(context), "${cleanFileKey(result.trackId)}.json")
            file.writeText(gson.toJson(result))
        } catch (e: Exception) {
            Log.w(TAG, "Error saving lyrics to disk: ${e.message}")
        }
    }

    private fun cleanFileKey(trackId: String): String {
        return trackId.replace(Regex("""[^a-zA-Z0-9_-]"""), "_")
    }

    private fun urlEncode(value: String): String {
        return try {
            URLEncoder.encode(value.trim(), "UTF-8")
        } catch (_: Exception) {
            value.trim().replace(" ", "%20")
        }
    }
}
