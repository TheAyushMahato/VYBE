package com.vybe.music.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.vybe.music.model.AlbumDetail
import com.vybe.music.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object AlbumRepository {
    private const val TAG = "AlbumRepository"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val gson = Gson()
    private val albumCache = ConcurrentHashMap<String, AlbumDetail>()

    suspend fun getAlbumDetails(
        browseId: String?,
        albumTitle: String,
        artistName: String,
        fallbackArtwork: String = "",
        fallbackYear: String = ""
    ): AlbumDetail? = withContext(Dispatchers.IO) {
        val cacheKey = "${albumTitle.trim().lowercase()}|${artistName.trim().lowercase()}"
        albumCache[cacheKey]?.let { return@withContext it }

        var resolvedBrowseId = browseId?.takeIf { it.isNotBlank() }

        // 1. If browseId is missing, search YouTube Music with Albums filter
        if (resolvedBrowseId == null) {
            resolvedBrowseId = searchAlbumBrowseId(albumTitle, artistName)
        }

        // 2. Fetch tracks using InnerTube browse endpoint
        var detail: AlbumDetail? = null
        if (!resolvedBrowseId.isNullOrBlank()) {
            detail = fetchAlbumByBrowseId(resolvedBrowseId, albumTitle, artistName, fallbackArtwork, fallbackYear)
        }

        // 3. Robust fallback: iTunes Album Tracklist Lookup
        if (detail == null || detail.tracks.isEmpty()) {
            detail = fetchAlbumFromITunes(albumTitle, artistName, fallbackArtwork, fallbackYear)
        }

        if (detail != null && detail.tracks.isNotEmpty()) {
            albumCache[cacheKey] = detail
            if (!resolvedBrowseId.isNullOrBlank()) {
                albumCache[resolvedBrowseId] = detail
            }
        }

        detail
    }

    private fun searchAlbumBrowseId(albumTitle: String, artistName: String): String? {
        try {
            val query = "$albumTitle $artistName".trim()
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
                // Albums filter params
                addProperty("params", "EgWKAQIYAWoKEAkQBRAKEAMQBA%3D%3D")
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

            val root = gson.fromJson(responseBody, JsonObject::class.java)
            val renderers = mutableListOf<JsonObject>()
            findMusicResponsiveListItemRenderers(root, renderers)

            for (r in renderers) {
                val nav = r.getAsJsonObject("navigationEndpoint")?.getAsJsonObject("browseEndpoint")
                val bid = nav?.get("browseId")?.asString
                if (!bid.isNullOrBlank() && (bid.startsWith("MPREb_") || bid.startsWith("OLAK5uy_"))) {
                    Log.i(TAG, "Resolved album browseId for '$albumTitle': $bid")
                    return bid
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "searchAlbumBrowseId error: ${e.message}")
        }
        return null
    }

    private fun fetchAlbumByBrowseId(
        browseId: String,
        fallbackTitle: String,
        fallbackArtist: String,
        fallbackArtwork: String,
        fallbackYear: String
    ): AlbumDetail? {
        try {
            val browseUrl = "https://music.youtube.com/youtubei/v1/browse"
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
                addProperty("browseId", browseId)
            }

            val reqBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(browseUrl)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Origin", "https://music.youtube.com")
                .post(reqBody)
                .build()

            val responseBody = httpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return null

            val root = gson.fromJson(responseBody, JsonObject::class.java)

            // Extract metadata from microformat
            val mf = root.getAsJsonObject("microformat")?.getAsJsonObject("microformatDataRenderer")
            val albumTitle = mf?.get("title")?.asString?.ifBlank { null } ?: fallbackTitle
            val desc = mf?.get("description")?.asString ?: ""
            val parsedArtist = if (desc.contains("•")) {
                desc.substringAfter("•").trim()
            } else fallbackArtist

            var artworkUrl = fallbackArtwork
            val thumbs = mf?.getAsJsonObject("thumbnail")?.getAsJsonArray("thumbnails")
            if (thumbs != null && thumbs.size() > 0) {
                val rawUrl = thumbs.get(thumbs.size() - 1).asJsonObject.get("url")?.asString ?: ""
                if (rawUrl.isNotBlank()) {
                    artworkUrl = rawUrl.replace(Regex("=w\\d+-h\\d+.*"), "=w1200-h1200-l90-rj")
                }
            }

            val renderers = mutableListOf<JsonObject>()
            findMusicResponsiveListItemRenderers(root, renderers)

            val tracks = mutableListOf<Track>()
            var trackIndex = 1

            for (r in renderers) {
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

                val cols = r.getAsJsonArray("flexColumns") ?: continue
                val songTitle = try {
                    cols.get(0).asJsonObject
                        .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.getAsJsonObject("text")
                        ?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: ""
                } catch (e: Exception) { "" }

                if (songTitle.isBlank()) continue

                val songArtist = try {
                    if (cols.size() > 1) {
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
                        sb.toString().ifBlank { parsedArtist }
                    } else parsedArtist
                } catch (e: Exception) { parsedArtist }

                var durationMs = 210000L
                val fixedCols = r.getAsJsonArray("fixedColumns")
                if (fixedCols != null && fixedCols.size() > 0) {
                    val durText = try {
                        fixedCols.get(0).asJsonObject
                            .getAsJsonObject("musicResponsiveListItemFixedColumnRenderer")
                            ?.getAsJsonObject("text")
                            ?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: ""
                    } catch (e: Exception) { "" }
                    val sec = parseDurationSeconds(durText)
                    if (sec > 0) durationMs = sec * 1000L
                }

                val trackId = if (vid.isNotBlank()) vid else "${browseId}_$trackIndex"

                tracks.add(
                    Track(
                        id = trackId,
                        title = songTitle,
                        artist = songArtist,
                        album = albumTitle,
                        durationMs = durationMs,
                        artworkUrl = artworkUrl,
                        mediaUri = if (vid.isNotBlank()) "vybe://yt/$vid" else ""
                    )
                )
                trackIndex++
            }

            if (tracks.isNotEmpty()) {
                Log.i(TAG, "Successfully fetched ${tracks.size} tracks for album '$albumTitle' via YTM browse")
                return AlbumDetail(
                    id = browseId,
                    title = albumTitle,
                    artist = parsedArtist,
                    year = fallbackYear.ifBlank { "2024" },
                    artworkUrl = artworkUrl,
                    trackCount = tracks.size,
                    tracks = tracks,
                    browseId = browseId
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchAlbumByBrowseId error: ${e.message}")
        }
        return null
    }

    private fun fetchAlbumFromITunes(
        albumTitle: String,
        artistName: String,
        fallbackArtwork: String,
        fallbackYear: String
    ): AlbumDetail? {
        try {
            val encoded = URLEncoder.encode("$albumTitle $artistName".trim(), "UTF-8")
            val searchUrl = "https://itunes.apple.com/search?term=$encoded&entity=album&limit=1&country=IN"
            val searchReq = Request.Builder().url(searchUrl).header("User-Agent", "Mozilla/5.0").build()

            val jsonString = httpClient.newCall(searchReq).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return null

            val root = gson.fromJson(jsonString, JsonObject::class.java)
            val results = root.getAsJsonArray("results") ?: return null
            if (results.size() == 0) return null

            val albumObj = results.get(0).asJsonObject
            val collId = albumObj.get("collectionId")?.asString ?: return null
            val resolvedTitle = albumObj.get("collectionName")?.asString ?: albumTitle
            val resolvedArtist = albumObj.get("artistName")?.asString ?: artistName
            val rawYear = albumObj.get("releaseDate")?.asString ?: ""
            val year = if (rawYear.length >= 4) rawYear.substring(0, 4) else fallbackYear
            val rawArt = albumObj.get("artworkUrl100")?.asString ?: fallbackArtwork
            val art = rawArt.replace("100x100bb.jpg", "1200x1200bb.jpg").replace("100x100bb.png", "1200x1200bb.png")

            // Lookup songs in collection
            val lookupUrl = "https://itunes.apple.com/lookup?id=$collId&entity=song"
            val lookupReq = Request.Builder().url(lookupUrl).header("User-Agent", "Mozilla/5.0").build()

            val lookupJson = httpClient.newCall(lookupReq).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return null

            val lookupRoot = gson.fromJson(lookupJson, JsonObject::class.java)
            val songResults = lookupRoot.getAsJsonArray("results") ?: return null

            val tracks = mutableListOf<Track>()
            for (elem in songResults) {
                if (!elem.isJsonObject) continue
                val s = elem.asJsonObject
                if (s.get("wrapperType")?.asString != "track") continue

                val trackName = s.get("trackName")?.asString ?: continue
                val trackArtist = s.get("artistName")?.asString ?: resolvedArtist
                val trackId = s.get("trackId")?.asString ?: "${collId}_${tracks.size + 1}"
                val durationMs = s.get("trackTimeMillis")?.asLong ?: 210000L

                tracks.add(
                    Track(
                        id = trackId,
                        title = trackName,
                        artist = trackArtist,
                        album = resolvedTitle,
                        durationMs = durationMs,
                        artworkUrl = art,
                        mediaUri = ""
                    )
                )
            }

            if (tracks.isNotEmpty()) {
                Log.i(TAG, "Successfully fetched ${tracks.size} tracks for album '$resolvedTitle' via iTunes lookup")
                return AlbumDetail(
                    id = collId,
                    title = resolvedTitle,
                    artist = resolvedArtist,
                    year = year,
                    artworkUrl = art,
                    trackCount = tracks.size,
                    tracks = tracks,
                    browseId = null
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchAlbumFromITunes error: ${e.message}")
        }
        return null
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
}
