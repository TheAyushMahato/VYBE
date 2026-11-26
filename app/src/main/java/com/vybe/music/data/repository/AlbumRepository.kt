package com.vybe.music.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.vybe.music.model.AlbumDetail
import com.vybe.music.model.EditorialAlbum
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
    private val userAlbumsCache = ConcurrentHashMap<String, List<EditorialAlbum>>()

    suspend fun getAlbumsForUser(
        selectedArtists: Set<String>,
        preferredLanguages: Set<String>,
        topArtists: List<String> = emptyList()
    ): List<EditorialAlbum> = withContext(Dispatchers.IO) {
        val cacheKey = "${preferredLanguages.sorted().joinToString(",")}|${selectedArtists.sorted().joinToString(",")}"
        userAlbumsCache[cacheKey]?.let { return@withContext it }

        val hasEnglish = preferredLanguages.any { it.equals("English", ignoreCase = true) } || preferredLanguages.isEmpty()
        val hasHindi = preferredLanguages.any { it.equals("Hindi", ignoreCase = true) }
        val hasPunjabi = preferredLanguages.any { it.equals("Punjabi", ignoreCase = true) }
        val isEnglishOnly = hasEnglish && !hasHindi && !hasPunjabi

        val curatedAlbums = mutableListOf<EditorialAlbum>()

        // ── English Studio Master Albums ──
        curatedAlbums.add(
            EditorialAlbum(
                title = "AM",
                subtitle = "Arctic Monkeys • 2013",
                artist = "Arctic Monkeys",
                year = "2013",
                artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/69/9c/b5/699cb5d6-115c-ff73-9d26-e57ea4350d72/887828031795.png/600x600bb.jpg",
                language = "English"
            )
        )
        curatedAlbums.add(
            EditorialAlbum(
                title = "Favourite Worst Nightmare",
                subtitle = "Arctic Monkeys • 2007",
                artist = "Arctic Monkeys",
                year = "2007",
                artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/c4/86/67/c4866750-a8eb-84aa-dafa-fe7f62b083c2/887828018888.png/600x600bb.jpg",
                language = "English"
            )
        )
        curatedAlbums.add(
            EditorialAlbum(
                title = "After Hours",
                subtitle = "The Weeknd • 2020",
                artist = "The Weeknd",
                year = "2020",
                artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/36/53/b7/3653b708-bb65-0a56-82f7-e62a875fa637/20UMGIM10183.rgb.jpg/600x600bb.jpg",
                language = "English"
            )
        )
        curatedAlbums.add(
            EditorialAlbum(
                title = "Starboy",
                subtitle = "The Weeknd • 2016",
                artist = "The Weeknd",
                year = "2016",
                artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/4b/f5/ec/4bf5ecf8-a00d-584a-fa63-c7e63b65fa36/16UMGIM61536.rgb.jpg/600x600bb.jpg",
                language = "English"
            )
        )
        curatedAlbums.add(
            EditorialAlbum(
                title = "In the Zone",
                subtitle = "Britney Spears • 2003",
                artist = "Britney Spears",
                year = "2003",
                artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/b9/e2/26/b9e22646-6819-bf9f-545b-9d41d9e79d7f/mzi.bspctbba.jpg/600x600bb.jpg",
                language = "English"
            )
        )
        curatedAlbums.add(
            EditorialAlbum(
                title = "Blackout",
                subtitle = "Britney Spears • 2007",
                artist = "Britney Spears",
                year = "2007",
                artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music114/v4/71/eb/65/71eb65ad-5b12-9c98-1e4a-939e0839f9b5/886444062828.jpg/600x600bb.jpg",
                language = "English"
            )
        )
        curatedAlbums.add(
            EditorialAlbum(
                title = "SOS",
                subtitle = "SZA • 2022",
                artist = "SZA",
                year = "2022",
                artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/03/db/ff/03dbff27-b52e-c75c-d7cf-d17e704812da/196589624519.jpg/600x600bb.jpg",
                language = "English"
            )
        )
        curatedAlbums.add(
            EditorialAlbum(
                title = "HIT ME HARD AND SOFT",
                subtitle = "Billie Eilish • 2024",
                artist = "Billie Eilish",
                year = "2024",
                artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/4a/0f/5e/4a0f5e13-68bc-a89c-46fe-8418ff8676fe/24UMGIM39003.rgb.jpg/600x600bb.jpg",
                language = "English"
            )
        )
        curatedAlbums.add(
            EditorialAlbum(
                title = "1989 (Taylor's Version)",
                subtitle = "Taylor Swift • 2023",
                artist = "Taylor Swift",
                year = "2023",
                artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/28/bf/c6/28bfc663-80c1-be54-7127-d3ebf61e8609/23UMGIM99228.rgb.jpg/600x600bb.jpg",
                language = "English"
            )
        )
        curatedAlbums.add(
            EditorialAlbum(
                title = "DAMN.",
                subtitle = "Kendrick Lamar • 2017",
                artist = "Kendrick Lamar",
                year = "2017",
                artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music128/v4/2e/0f/f8/2e0ff837-77fb-3129-cb6a-74a68e8ccba4/17UMGIM26788.rgb.jpg/600x600bb.jpg",
                language = "English"
            )
        )
        curatedAlbums.add(
            EditorialAlbum(
                title = "Future Nostalgia",
                subtitle = "Dua Lipa • 2020",
                artist = "Dua Lipa",
                year = "2020",
                artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/7c/4f/f5/7c4ff558-7ae7-bfcf-21eb-093557e0fb8f/190295286101.jpg/600x600bb.jpg",
                language = "English"
            )
        )
        curatedAlbums.add(
            EditorialAlbum(
                title = "UTOPIA",
                subtitle = "Travis Scott • 2023",
                artist = "Travis Scott",
                year = "2023",
                artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/55/e2/24/55e224e7-4402-9993-9c84-180e927c8172/196588414432.jpg/600x600bb.jpg",
                language = "English"
            )
        )

        // ── Hindi Studio Master Soundtracks / Albums ──
        curatedAlbums.add(
            EditorialAlbum(
                title = "Brahmastra",
                subtitle = "Pritam • 2022",
                artist = "Pritam",
                year = "2022",
                artworkUrl = "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg",
                language = "Hindi"
            )
        )
        curatedAlbums.add(
            EditorialAlbum(
                title = "Rockstar",
                subtitle = "A.R. Rahman • 2011",
                artist = "A.R. Rahman",
                year = "2011",
                artworkUrl = "https://c.saavncdn.com/209/Rockstar-Hindi-2011-500x500.jpg",
                language = "Hindi"
            )
        )
        curatedAlbums.add(
            EditorialAlbum(
                title = "Aashiqui 2",
                subtitle = "Mithoon • 2013",
                artist = "Mithoon",
                year = "2013",
                artworkUrl = "https://c.saavncdn.com/430/Aashiqui-2-Hindi-2013-500x500.jpg",
                language = "Hindi"
            )
        )
        curatedAlbums.add(
            EditorialAlbum(
                title = "Kabir Singh",
                subtitle = "Sachet-Parampara • 2019",
                artist = "Sachet-Parampara",
                year = "2019",
                artworkUrl = "https://c.saavncdn.com/649/Kabir-Singh-Hindi-2019-20240316041004-500x500.jpg",
                language = "Hindi"
            )
        )

        // ── Punjabi Studio Master Albums ──
        curatedAlbums.add(
            EditorialAlbum(
                title = "G.O.A.T.",
                subtitle = "Diljit Dosanjh • 2020",
                artist = "Diljit Dosanjh",
                year = "2020",
                browseId = "MPREb_grVglC0todv",
                artworkUrl = "https://c.saavncdn.com/597/G-O-A-T-Punjabi-2020-20240708055140-500x500.jpg",
                language = "Punjabi"
            )
        )
        curatedAlbums.add(
            EditorialAlbum(
                title = "MoonChild Era",
                subtitle = "Diljit Dosanjh • 2021",
                artist = "Diljit Dosanjh",
                year = "2021",
                browseId = "MPREb_hv8LEmvUqnE",
                artworkUrl = "https://c.saavncdn.com/209/MoonChild-Era-Punjabi-2021-20240715073449-500x500.jpg",
                language = "Punjabi"
            )
        )
        curatedAlbums.add(
            EditorialAlbum(
                title = "Making Memories",
                subtitle = "Karan Aujla • 2023",
                artist = "Karan Aujla",
                year = "2023",
                artworkUrl = "https://c.saavncdn.com/178/Making-Memories-Punjabi-2023-20230818044738-500x500.jpg",
                language = "Punjabi"
            )
        )
        curatedAlbums.add(
            EditorialAlbum(
                title = "Moosetape",
                subtitle = "Sidhu Moose Wala • 2021",
                artist = "Sidhu Moose Wala",
                year = "2021",
                artworkUrl = "https://c.saavncdn.com/492/Moosetape-Punjabi-2021-20210514101419-500x500.jpg",
                language = "Punjabi"
            )
        )

        // Dynamically fetch albums for user's selected artists via iTunes Search API
        val dynamicAlbums = mutableListOf<EditorialAlbum>()
        val combinedArtists = (selectedArtists + topArtists).filter { it.isNotBlank() }.distinct().take(6)
        for (artist in combinedArtists) {
            val fetched = searchAlbumsForArtist(artist)
            dynamicAlbums.addAll(fetched)
        }

        // Combine curated and dynamic, prioritizing user's selected artists
        val allAvailable = (dynamicAlbums + curatedAlbums).distinctBy { "${it.title.lowercase()}|${it.artist.lowercase()}" }

        // Filter and mix according to language rules
        val result = if (isEnglishOnly) {
            // Strictly 100% English albums
            allAvailable.filter { it.language.equals("English", ignoreCase = true) }
        } else if (!hasEnglish && (hasHindi || hasPunjabi)) {
            // Only Indian languages selected
            allAvailable.filter { !it.language.equals("English", ignoreCase = true) }
        } else {
            // Multi-language (e.g. English + Hindi / Punjabi): Interleave round-robin
            val englishList = allAvailable.filter { it.language.equals("English", ignoreCase = true) }.toMutableList()
            val regionalList = allAvailable.filter { !it.language.equals("English", ignoreCase = true) }.toMutableList()
            val interleaved = mutableListOf<EditorialAlbum>()
            while (englishList.isNotEmpty() || regionalList.isNotEmpty()) {
                if (englishList.isNotEmpty()) interleaved.add(englishList.removeAt(0))
                if (regionalList.isNotEmpty()) interleaved.add(regionalList.removeAt(0))
            }
            interleaved
        }

        // Prioritize albums matching selected artists
        val sortedResult = result.sortedByDescending { alb ->
            if (selectedArtists.any { it.equals(alb.artist, ignoreCase = true) || alb.artist.contains(it, ignoreCase = true) }) 2 else 0
        }

        val finalAlbums = sortedResult.take(16).ifEmpty {
            if (isEnglishOnly) curatedAlbums.filter { it.language == "English" } else curatedAlbums
        }

        userAlbumsCache[cacheKey] = finalAlbums
        finalAlbums
    }

    private fun searchAlbumsForArtist(artist: String): List<EditorialAlbum> {
        try {
            val encoded = URLEncoder.encode(artist.trim(), "UTF-8")
            val country = if (isIndianArtistName(artist)) "IN" else "US"
            val searchUrl = "https://itunes.apple.com/search?term=$encoded&entity=album&limit=4&country=$country"
            val req = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val jsonString = httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return emptyList()

            val root = gson.fromJson(jsonString, JsonObject::class.java)
            val results = root.getAsJsonArray("results") ?: return emptyList()

            val albums = mutableListOf<EditorialAlbum>()
            for (elem in results) {
                if (!elem.isJsonObject) continue
                val obj = elem.asJsonObject
                val title = obj.get("collectionName")?.asString ?: continue
                if (title.contains("- Single", ignoreCase = true) || title.contains("- EP", ignoreCase = true)) continue
                val trackCount = obj.get("trackCount")?.asInt ?: 0
                if (trackCount < 3) continue

                val artistName = obj.get("artistName")?.asString ?: artist
                val rawYear = obj.get("releaseDate")?.asString ?: ""
                val year = if (rawYear.length >= 4) rawYear.substring(0, 4) else ""
                val rawArt = obj.get("artworkUrl100")?.asString ?: ""
                val art = rawArt.replace("100x100bb.jpg", "600x600bb.jpg")
                    .replace("100x100bb.png", "600x600bb.png")
                val lang = if (isIndianArtistName(artistName)) "Hindi" else "English"

                albums.add(
                    EditorialAlbum(
                        title = title,
                        subtitle = "$artistName • $year",
                        artist = artistName,
                        year = year,
                        artworkUrl = art,
                        browseId = null,
                        language = lang
                    )
                )
            }
            return albums
        } catch (e: Exception) {
            Log.w(TAG, "searchAlbumsForArtist error for '$artist': ${e.message}")
            return emptyList()
        }
    }

    private fun isIndianArtistName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.contains("arijit") || lower.contains("diljit") || lower.contains("pritam") ||
                lower.contains("rahman") || lower.contains("karan aujla") || lower.contains("sidhu") ||
                lower.contains("moose") || lower.contains("ap dhillon") || lower.contains("shreya") ||
                lower.contains("sonu") || lower.contains("badshah") || lower.contains("mithoon") ||
                lower.contains("vishal") || lower.contains("shekhar") || lower.contains("sachin") ||
                lower.contains("jigar") || lower.contains("tanishk") || lower.contains("shubh") ||
                lower.contains("guru randhawa") || lower.contains("jubin") || lower.contains("neha kakkar")
    }

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
