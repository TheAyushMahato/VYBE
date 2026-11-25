package com.vybe.music.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.vybe.music.data.audio.YouTubeAudioExtractor
import com.vybe.music.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Reactive Global Artwork Repair Manager.
 *
 * - Maintains Compose-observable `repairedUrls` (SnapshotStateMap) keyed by track.id and "$title|$artist"
 *   so any fetched/repaired artwork immediately updates all visible UI rows in real-time.
 * - Concurrency limited to 4 simultaneous downloads on Dispatchers.IO.
 * - Instant local cross-lookup (Statistics/History/Trending/Playlists).
 * - High-res square artwork via iTunes API (upgraded to 600x600) with InnerTube & YouTube fallbacks.
 * - Permanent storage persistence across Liked Songs, custom playlists, and Downloads.
 * - Automatic corrupt local cover cleanup (>2KB & valid BitmapFactory bounds).
 */
object ArtworkRepairManager {
    private const val TAG = "ArtworkRepairManager"

    val repairedUrls: SnapshotStateMap<String, String> = mutableStateMapOf()

    private val repairScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlightRepairs = ConcurrentHashMap.newKeySet<String>()
    private val repairSemaphore = Semaphore(4)

    private val _artworkRepairedFlow = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 64)
    val artworkRepairedFlow = _artworkRepairedFlow.asSharedFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun normalizeKey(title: String, artist: String): String {
        val cleanT = title.replace(Regex("(?i)\\(.*?\\)|\\[.*?\\]"), "").trim().lowercase()
        val cleanA = artist.split(Regex("[,&/•]|\\bfeat\\.?\\b|\\bft\\.?\\b", RegexOption.IGNORE_CASE))
            .firstOrNull()?.trim()?.lowercase() ?: ""
        return "$cleanT|$cleanA"
    }

    /**
     * Verifies local cover file exists, is > 2048 bytes, and contains valid decode bounds.
     * Deletes corrupt/truncated files immediately so they fall back to network repair.
     */
    fun getValidLocalCover(context: Context, trackId: String): File? {
        if (trackId.isBlank()) return null
        val coversDir = File(context.filesDir, "downloads/covers")
        val coverFile = File(coversDir, "$trackId.jpg")

        if (!coverFile.exists() || coverFile.length() <= 2048) {
            if (coverFile.exists() && coverFile.length() <= 2048) {
                Log.w(TAG, "Deleting empty/undersized cover file for track $trackId (${coverFile.length()} bytes)")
                coverFile.delete()
            }
            return null
        }

        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(coverFile.absolutePath, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                coverFile
            } else {
                Log.w(TAG, "Corrupt image bounds for track $trackId cover, deleting file")
                coverFile.delete()
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed decoding local cover for track $trackId: ${e.message}")
            coverFile.delete()
            null
        }
    }

    fun getRepairedUrl(track: Track): String? {
        if (track.id.isNotBlank()) {
            repairedUrls[track.id]?.let { return it }
        }
        val key = normalizeKey(track.title, track.artist)
        return repairedUrls[key]
    }

    fun repairIfNeeded(context: Context, track: Track, onRepaired: ((String) -> Unit)? = null) {
        if (track.title.isBlank()) return
        val dedupKey = if (track.id.isNotBlank()) track.id else normalizeKey(track.title, track.artist)
        if (!inFlightRepairs.add(dedupKey)) return // Deduplicate

        repairScope.launch {
            try {
                repairSemaphore.withPermit {
                    val repairedUrl = resolveArtwork(context, track)
                    if (!repairedUrl.isNullOrBlank()) {
                        Log.i(TAG, "Repaired artwork for '${track.title}': $repairedUrl")

                        // 1. Update in-memory Compose SnapshotStateMap for instant live UI update
                        withContext(Dispatchers.Main) {
                            if (track.id.isNotBlank()) {
                                repairedUrls[track.id] = repairedUrl
                            }
                            repairedUrls[normalizeKey(track.title, track.artist)] = repairedUrl
                            onRepaired?.invoke(repairedUrl)
                        }

                        // 2. Persist permanently back into storage
                        val storage = LocalMusicStorage(context)
                        storage.updateTrackArtworkEverywhere(track.id, repairedUrl)

                        // 3. If downloaded, cache cover file locally
                        val downloadsDir = UserSessionManager.getUserDownloadsDir(context, UserSessionManager.currentUserId)
                        val m4aFile = File(downloadsDir, "${track.id}.m4a")
                        if (m4aFile.exists() && !repairedUrl.startsWith("file://")) {
                            try {
                                val coversDir = File(context.filesDir, "downloads/covers").apply { mkdirs() }
                                val artFile = File(coversDir, "${track.id}.jpg")
                                val req = Request.Builder().url(repairedUrl).build()
                                httpClient.newCall(req).execute().use { resp ->
                                    if (resp.isSuccessful && resp.body != null) {
                                        resp.body!!.byteStream().use { input ->
                                            FileOutputStream(artFile).use { out -> input.copyTo(out) }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed writing local cover for ${track.id}: ${e.message}")
                            }
                        }

                        // 4. Emit flow for ViewModels
                        _artworkRepairedFlow.tryEmit(track.id to repairedUrl)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Repair error for '${track.title}': ${e.message}")
            } finally {
                inFlightRepairs.remove(dedupKey)
            }
        }
    }

    private suspend fun resolveArtwork(context: Context, track: Track): String? = withContext(Dispatchers.IO) {
        // 1. Instant Local Cross-Lookup:
        // Check if Statistics / History, Trending, or another Playlist already has a working artworkUrl
        val storage = LocalMusicStorage(context)
        val normalizedTarget = normalizeKey(track.title, track.artist)

        val statsTracks = storage.getPlaybackStats().recentTracks
        val trendingTracks = storage.getCachedTrendingTracks().orEmpty()
        val allLocalLists = listOf(statsTracks, trendingTracks, storage.getLikedTracks())

        for (list in allLocalLists) {
            val match = list.firstOrNull {
                (it.id == track.id || normalizeKey(it.title, it.artist) == normalizedTarget) &&
                    it.artworkUrl.isNotBlank() &&
                    !it.artworkUrl.startsWith("file://")
            }
            if (match != null) {
                return@withContext upgradeArtworkUrl(match.artworkUrl)
            }
        }

        val cleanTitle = track.title.replace(Regex("(?i)\\(.*?\\)|\\[.*?\\]"), "").trim()
        val firstArtist = track.artist
            .split(Regex("[,&/•]|\\bfeat\\.?\\b|\\bft\\.?\\b", RegexOption.IGNORE_CASE))
            .firstOrNull()?.trim() ?: ""

        // 2. Fast Reliable High-Res Square Cover Fetch: iTunes Search API
        try {
            val itunesArt = fetchItunesSquareArt(cleanTitle, firstArtist)
            if (!itunesArt.isNullOrBlank()) {
                return@withContext itunesArt
            }
        } catch (e: Exception) {
            Log.w(TAG, "iTunes search failed for '$cleanTitle $firstArtist': ${e.message}")
        }

        // 3. Fallback: YouTube Music InnerTube Search
        try {
            val ytmArt = fetchYtmSquareArt("$cleanTitle $firstArtist".trim())
            if (!ytmArt.isNullOrBlank()) {
                return@withContext ytmArt
            }
        } catch (e: Exception) {
            Log.w(TAG, "YTM search failed for '$cleanTitle $firstArtist': ${e.message}")
        }

        // 4. Fallback: Video ID thumbnail
        val cachedVid = YouTubeAudioExtractor.getCachedVideoId(track.id)
            ?: if (track.id.length == 11 && track.id.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) track.id else null
        if (!cachedVid.isNullOrBlank()) {
            return@withContext "https://i.ytimg.com/vi/$cachedVid/hqdefault.jpg"
        }

        null
    }

    private fun upgradeArtworkUrl(url: String): String {
        var u = url.trim()
        if (u.startsWith("http://")) {
            u = "https://" + u.removePrefix("http://")
        }
        if (u.contains("100x100bb.jpg")) {
            u = u.replace("100x100bb.jpg", "600x600bb.jpg")
        }
        if (u.contains("=w") && !u.contains("=w544")) {
            u = u.replace(Regex("=w\\d+-h\\d+.*"), "=w544-h544-l90-rj")
        }
        return u
    }

    private fun fetchItunesSquareArt(title: String, artist: String): String? {
        val query = "$title $artist".trim()
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://itunes.apple.com/search?term=$encoded&media=music&entity=song&limit=1"
        val request = Request.Builder().url(url).build()

        val json = httpClient.newCall(request).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        } ?: return null

        val root = JsonParser.parseString(json).asJsonObject
        val results = root.getAsJsonArray("results") ?: return null
        if (results.size() == 0) return null

        val item = results.get(0).asJsonObject
        val art100 = item.get("artworkUrl100")?.asString ?: return null
        return art100.replace("100x100bb.jpg", "600x600bb.jpg")
    }

    private fun fetchYtmSquareArt(query: String): String? {
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

        val responseBody = httpClient.newCall(request).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        } ?: return null

        val root = JsonParser.parseString(responseBody).asJsonObject
        val items = mutableListOf<JsonObject>()
        findRenderers(root, "musicResponsiveListItemRenderer", items)

        for (r in items) {
            val rawUrl = try {
                r.getAsJsonObject("thumbnail")
                    ?.getAsJsonObject("musicThumbnailRenderer")
                    ?.getAsJsonObject("thumbnail")
                    ?.getAsJsonArray("thumbnails")
                    ?.lastOrNull()?.asJsonObject
                    ?.get("url")?.asString ?: ""
            } catch (_: Exception) { "" }

            if (rawUrl.isNotBlank()) {
                return rawUrl
                    .replace(Regex("=w\\d+-h\\d+.*"), "=w544-h544-l90-rj")
                    .replace("=s60", "=s544")
                    .replace("=s120", "=s544")
            }
        }
        return null
    }

    private fun findRenderers(element: com.google.gson.JsonElement, key: String, results: MutableList<JsonObject>) {
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has(key) && obj.get(key).isJsonObject) {
                results.add(obj.getAsJsonObject(key))
            }
            for (entry in obj.entrySet()) {
                findRenderers(entry.value, key, results)
            }
        } else if (element.isJsonArray) {
            for (item in element.asJsonArray) {
                findRenderers(item, key, results)
            }
        }
    }
}
