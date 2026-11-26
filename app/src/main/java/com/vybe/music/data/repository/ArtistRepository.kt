package com.vybe.music.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.vybe.music.model.ArtistAlbum
import com.vybe.music.model.ArtistDetail
import com.vybe.music.model.ArtistSummary
import com.vybe.music.model.CuratedArtist
import com.vybe.music.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object ArtistRepository {
    private const val TAG = "ArtistRepository"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val gson = Gson()
    private val artistCache = mutableMapOf<String, ArtistDetail>()

    /**
     * Strict validator for artist portrait URLs.
     * Rejects empty/blank URLs and any known Deezer / CDN silhouette placeholder patterns.
     */
    fun isValidArtistImageUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase().trim()
        if (lower.contains("/artist//")) return false
        if (lower.contains("d41d8cd98f00b204e9800998ecf8427e")) return false
        if (lower.contains("default_artist")) return false
        if (lower.contains("placeholder") || lower.contains("avatar_default") || lower.contains("silhouette")) return false
        // Known Deezer empty silhouette placeholder MD5 hashes
        val knownSilhouetteHashes = setOf(
            "1e7e8346174a7a8cfc6a8b13d2f9d14d",
            "b86a87796d19485f7a014a60ea511c94",
            "552bc47385f09199d7b5791fc51db778",
            "2dfb9442e9759dc3eec4b29bb1f6c442",
            "25cb90a2a46c19f564d6db53df904351",
            "b4e31846b9dbebe9a53820cb008b8b8f",
            "8f52232a510522c0b435ff3b683ef090",
            "508dd64267e108e4046178c77aa7ff8a",
            "5856ebfc4db90ad562a1b5d13a52e05a",
            "d1417076d3f27b5886ca4e1a0b3e64f8"
        )
        for (hash in knownSilhouetteHashes) {
            if (lower.contains(hash)) return false
        }
        return true
    }

    // Curated catalog of verified top artists with verified CDN photos & genre tags
    val verifiedCatalog: List<ArtistSummary> = listOf(
        ArtistSummary(
            name = "Arijit Singh",
            imageUrl = "https://c.saavncdn.com/artists/Arijit_Singh_500x500.jpg",
            genre = "Bollywood / Soul",
            topTrackTitle = "Kesariya"
        ),
        ArtistSummary(
            name = "The Weeknd",
            imageUrl = "https://c.saavncdn.com/artists/The_Weeknd_002_20241003071400_500x500.jpg",
            genre = "R&B / Synthwave",
            topTrackTitle = "One Of The Girls"
        ),
        ArtistSummary(
            name = "Diljit Dosanjh",
            imageUrl = "https://c.saavncdn.com/artists/Diljit_Dosanjh_500x500.jpg",
            genre = "Punjabi / Pop",
            topTrackTitle = "Born to Shine"
        ),
        ArtistSummary(
            name = "Karan Aujla",
            imageUrl = "https://c.saavncdn.com/artists/Karan_Aujla_005_20260925061936_500x500.jpg",
            genre = "Punjabi / Hip-Hop",
            topTrackTitle = "Tauba Tauba"
        ),
        ArtistSummary(
            name = "Dua Lipa",
            imageUrl = "https://c.saavncdn.com/artists/Dua_Lipa_004_20231120090922_500x500.jpg",
            genre = "Nu-Disco / Pop",
            topTrackTitle = "Levitating"
        ),
        ArtistSummary(
            name = "AP Dhillon",
            imageUrl = "https://c.saavncdn.com/artists/AP_Dhillon_004_20251023102150_500x500.jpg",
            genre = "Indo-Western Hip-Hop",
            topTrackTitle = "Brown Munde"
        ),
        ArtistSummary(
            name = "Sabrina Carpenter",
            imageUrl = "https://c.saavncdn.com/artists/Sabrina_Carpenter_002_20240902091400_500x500.jpg",
            genre = "Dance-Pop",
            topTrackTitle = "Espresso"
        ),
        ArtistSummary(
            name = "Billie Eilish",
            imageUrl = "https://c.saavncdn.com/artists/Billie_Eilish_004_20240520063200_500x500.jpg",
            genre = "Alt-Pop / Indie",
            topTrackTitle = "Birds of a Feather"
        ),
        ArtistSummary(
            name = "Pritam",
            imageUrl = "https://c.saavncdn.com/artists/Pritam_Chakraborty-20170711073326_500x500.jpg",
            genre = "Bollywood Composer",
            topTrackTitle = "Chaleya"
        ),
        ArtistSummary(
            name = "Alan Walker",
            imageUrl = "https://c.saavncdn.com/artists/Alan_Walker_003_20231116095443_500x500.jpg",
            genre = "Electronic / EDM",
            topTrackTitle = "Faded"
        ),
        ArtistSummary(
            name = "Taylor Swift",
            imageUrl = "https://c.saavncdn.com/artists/Taylor_Swift_500x500.jpg",
            genre = "Pop / Singer-Songwriter",
            topTrackTitle = "Cruel Summer"
        ),
        ArtistSummary(
            name = "Shreya Ghoshal",
            imageUrl = "https://c.saavncdn.com/artists/Shreya_Ghoshal_500x500.jpg",
            genre = "Bollywood Classical",
            topTrackTitle = "Sunn Raha Hai"
        ),
        ArtistSummary(
            name = "Atif Aslam",
            imageUrl = "https://c.saavncdn.com/artists/Atif_Aslam_500x500.jpg",
            genre = "Sufi / Romantic",
            topTrackTitle = "Tajdar-e-Haram"
        ),
        ArtistSummary(
            name = "Badshah",
            imageUrl = "https://c.saavncdn.com/artists/Badshah_500x500.jpg",
            genre = "Desi Hip-Hop",
            topTrackTitle = "Genda Phool"
        ),
        ArtistSummary(
            name = "Ed Sheeran",
            imageUrl = "https://c.saavncdn.com/artists/Ed_Sheeran_500x500.jpg",
            genre = "Pop / Acoustic",
            topTrackTitle = "Shape of You"
        ),
        ArtistSummary(
            name = "Arctic Monkeys",
            imageUrl = "https://cdn-images.dzcdn.net/images/artist/6c03e4c7c36800897fd468633286db24/500x500-000000-80-0-0.jpg",
            genre = "Indie Rock / Alt-Rock",
            topTrackTitle = "Do I Wanna Know?"
        ),
        ArtistSummary(
            name = "Lana Del Rey",
            imageUrl = "https://cdn-images.dzcdn.net/images/artist/8994d3be1a59a72f887f1f8afd2d4c6c/500x500-000000-80-0-0.jpg",
            genre = "Dream Pop / Baroque Pop",
            topTrackTitle = "Summertime Sadness"
        ),
        ArtistSummary(
            name = "Britney Spears",
            imageUrl = "https://cdn-images.dzcdn.net/images/artist/caea45732bb52679494602c60430435a/500x500-000000-80-0-0.jpg",
            genre = "Dance-Pop / Pop",
            topTrackTitle = "Toxic"
        ),
        ArtistSummary(
            name = "Frank Ocean",
            imageUrl = "https://cdn-images.dzcdn.net/images/artist/d57b0431d7d794302e1782dfd7a3800d/500x500-000000-80-0-0.jpg",
            genre = "R&B / Soul",
            topTrackTitle = "Lost"
        ),
        ArtistSummary(
            name = "Drake",
            imageUrl = "https://cdn-images.dzcdn.net/images/artist/70223888f501f4b843142e071abda364/500x500-000000-80-0-0.jpg",
            genre = "Hip-Hop / Rap",
            topTrackTitle = "God's Plan"
        ),
        ArtistSummary(
            name = "Tame Impala",
            imageUrl = "https://cdn-images.dzcdn.net/images/artist/879015e713cc6ad6ffaeec154c027505/500x500-000000-80-0-0.jpg",
            genre = "Psychedelic Pop / Rock",
            topTrackTitle = "The Less I Know The Better"
        ),
        ArtistSummary(
            name = "SZA",
            imageUrl = "https://cdn-images.dzcdn.net/images/artist/c1291c474f833ba1d02cc3bc3e0940eb/500x500-000000-80-0-0.jpg",
            genre = "R&B / Neo Soul",
            topTrackTitle = "Kill Bill"
        ),
        ArtistSummary(
            name = "Coldplay",
            imageUrl = "https://cdn-images.dzcdn.net/images/artist/3087954bca22f306324912e5ac8375c3/500x500-000000-80-0-0.jpg",
            genre = "Alt-Rock / Pop",
            topTrackTitle = "Yellow"
        ),
        ArtistSummary(
            name = "Bruno Mars",
            imageUrl = "https://cdn-images.dzcdn.net/images/artist/90f0b5b11df4f87ee878f38569b5995b/500x500-000000-80-0-0.jpg",
            genre = "Pop / Funk / R&B",
            topTrackTitle = "That's What I Like"
        ),
        ArtistSummary(
            name = "Kendrick Lamar",
            imageUrl = "https://cdn-images.dzcdn.net/images/artist/be0a7c550567f4af0ed202d7235b74d6/500x500-000000-80-0-0.jpg",
            genre = "Hip-Hop / West Coast",
            topTrackTitle = "Not Like Us"
        ),
        ArtistSummary(
            name = "Anuv Jain",
            imageUrl = "https://c.saavncdn.com/artists/Anuv_Jain_002_20230713063539_500x500.jpg",
            genre = "Indie / Acoustic",
            topTrackTitle = "Husn"
        ),
        ArtistSummary(
            name = "KK",
            imageUrl = "https://c.saavncdn.com/artists/K_K__500x500.jpg",
            genre = "Bollywood Rock / Soul",
            topTrackTitle = "Zara Sa"
        ),
        ArtistSummary(
            name = "Sonu Nigam",
            imageUrl = "https://c.saavncdn.com/artists/Sonu_Nigam_500x500.jpg",
            genre = "Bollywood / Retro",
            topTrackTitle = "Kal Ho Naa Ho"
        ),
        ArtistSummary(
            name = "A.R. Rahman",
            imageUrl = "https://c.saavncdn.com/artists/A_R__Rahman_500x500.jpg",
            genre = "World / Composer",
            topTrackTitle = "Kun Faya Kun"
        ),
        ArtistSummary(
            name = "Seedhe Maut",
            imageUrl = "https://c.saavncdn.com/artists/Seedhe_Maut_002_20240322102100_500x500.jpg",
            genre = "Desi Hip-Hop",
            topTrackTitle = "Nanchaku"
        ),
        ArtistSummary(
            name = "Shubh",
            imageUrl = "https://c.saavncdn.com/artists/Shubh_002_20231117094056_500x500.jpg",
            genre = "Punjabi Hip-Hop",
            topTrackTitle = "No Love"
        ),
        ArtistSummary(
            name = "Jubin Nautiyal",
            imageUrl = "https://c.saavncdn.com/artists/Jubin_Nautiyal_500x500.jpg",
            genre = "Bollywood / Romance",
            topTrackTitle = "Raataan Lambiyan"
        ),
        ArtistSummary(
            name = "Vishal Mishra",
            imageUrl = "https://c.saavncdn.com/artists/Vishal_Mishra_500x500.jpg",
            genre = "Bollywood / Indie",
            topTrackTitle = "Pehle Bhi Main"
        ),
        ArtistSummary(
            name = "B Praak",
            imageUrl = "https://c.saavncdn.com/artists/B_Praak_500x500.jpg",
            genre = "Punjabi / Romantic",
            topTrackTitle = "Mann Bharryaa"
        )
    )

    suspend fun searchArtists(query: String): List<CuratedArtist> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()
        val queryLower = cleanQuery.lowercase()

        // 1. Check local catalog first
        val localMatches = mutableListOf<CuratedArtist>()
        for (artist in verifiedCatalog) {
            val nameLower = artist.name.lowercase()
            if (nameLower == queryLower || nameLower.startsWith(queryLower) || nameLower.contains(queryLower)) {
                localMatches.add(CuratedArtist(name = artist.name, imageUrl = artist.imageUrl))
            }
        }

        // 2. Query YTM Artists & Deezer Artists in parallel
        val ytmDeferred = async { searchYtmArtists(cleanQuery) }
        val deezerDeferred = async { searchDeezerArtists(cleanQuery) }

        val ytmResults = try { ytmDeferred.await() } catch (_: Exception) { emptyList() }
        val deezerResults = try { deezerDeferred.await() } catch (_: Exception) { emptyList() }

        val combined = mutableListOf<CuratedArtist>()
        val seen = mutableSetOf<String>()

        fun addCandidate(c: CuratedArtist) {
            val norm = c.name.lowercase().trim()
            if (norm.isBlank() || seen.contains(norm)) return

            // Filter out spam/lyric/fan channels
            val spamKeywords = listOf("lyrics", "7clouds", "karaoke", "mix", "vevo", "records", "entertainment", "series", "topic")
            val isSpam = spamKeywords.any { kw -> norm.contains(kw) && !queryLower.contains(kw) }
            if (isSpam) return

            seen.add(norm)
            combined.add(c)
        }

        // Add local matches first
        localMatches.forEach { addCandidate(it) }

        // Add Deezer verified artists with HD portraits
        for (d in deezerResults) {
            addCandidate(d)
        }

        // Add YTM official artists
        for (y in ytmResults) {
            val existing = combined.find { it.name.equals(y.name, ignoreCase = true) }
            if (existing == null) {
                addCandidate(y)
            } else if (existing.imageUrl.isBlank() && y.imageUrl.isNotBlank()) {
                val idx = combined.indexOf(existing)
                combined[idx] = existing.copy(imageUrl = y.imageUrl)
            }
        }

        // 3. Rank results: exact match first, then startsWith, then fuzzy/contains
        val ranked = combined.sortedByDescending { artist ->
            val nameLower = artist.name.lowercase().trim()
            var score = 0
            if (nameLower == queryLower) {
                score += 1000
            } else if (nameLower.startsWith(queryLower)) {
                score += 500
            } else if (nameLower.contains(queryLower)) {
                score += 200
            }

            // Word-level matching (e.g. "artic monkey" -> "arctic monkeys", "britney" -> "britney spears")
            val queryWords = queryLower.split(Regex("\\s+")).filter { it.isNotBlank() }
            val nameWords = nameLower.split(Regex("\\s+")).filter { it.isNotBlank() }
            for (qw in queryWords) {
                if (nameWords.any { nw -> nw.startsWith(qw) || nw.contains(qw) || qw.contains(nw) }) {
                    score += 100
                }
            }

            if (artist.imageUrl.isNotBlank()) {
                score += 50
            }
            score
        }

        ranked.take(20)
    }

    private fun searchDeezerArtists(query: String): List<CuratedArtist> {
        return try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://api.deezer.com/search/artist?q=$encoded&limit=15"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            val json = httpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return emptyList()

            val root = gson.fromJson(json, JsonObject::class.java)
            val data = root.getAsJsonArray("data") ?: return emptyList()
            val list = mutableListOf<CuratedArtist>()
            for (item in data) {
                if (!item.isJsonObject) continue
                val obj = item.asJsonObject
                val name = obj.get("name")?.asString?.trim() ?: continue
                val rawPic = obj.get("picture_big")?.asString
                    ?: obj.get("picture_medium")?.asString
                    ?: obj.get("picture")?.asString
                    ?: ""
                val pic = rawPic.takeIf { isValidArtistImageUrl(it) } ?: ""
                list.add(CuratedArtist(name = name, imageUrl = pic))
            }
            list
        } catch (e: Exception) {
            Log.w(TAG, "searchDeezerArtists failed: ${e.message}")
            emptyList()
        }
    }

    private fun searchYtmArtists(query: String): List<CuratedArtist> {
        return try {
            val searchApiUrl = "https://music.youtube.com/youtubei/v1/search"
            val payload = JsonObject().apply {
                val contextObj = JsonObject().apply {
                    val clientObj = JsonObject().apply {
                        addProperty("clientName", "WEB_REMIX")
                        addProperty("clientVersion", "1.20240101.01.00")
                        addProperty("hl", "en")
                        addProperty("gl", "US")
                    }
                    add("client", clientObj)
                }
                add("context", contextObj)
                addProperty("query", query.trim())
                addProperty("params", "EgWKAQIgAWoKEAkQChADEAUQBA%3D%3D") // Strict ARTISTS filter
            }

            val reqBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(searchApiUrl)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .post(reqBody)
                .build()

            val json = httpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return emptyList()

            val root = gson.fromJson(json, JsonObject::class.java)
            val renderers = mutableListOf<JsonObject>()
            findRenderersByKey(root, "musicResponsiveListItemRenderer", renderers)

            val list = mutableListOf<CuratedArtist>()
            for (r in renderers) {
                // Must have browseId starting with "UC"
                var browseId: String? = null
                try {
                    val nav = r.getAsJsonObject("navigationEndpoint")
                    browseId = nav?.getAsJsonObject("browseEndpoint")?.get("browseId")?.asString
                } catch (_: Exception) {}

                if (browseId == null || !browseId.startsWith("UC")) {
                    continue
                }

                var title = ""
                try {
                    val flexCols = r.getAsJsonArray("flexColumns")
                    if (flexCols != null && flexCols.size() > 0) {
                        val col0 = flexCols.get(0).asJsonObject
                            .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                        val runs = col0?.getAsJsonObject("text")?.getAsJsonArray("runs")
                        if (runs != null && runs.size() > 0) {
                            title = runs.get(0).asJsonObject.get("text")?.asString ?: ""
                        }
                    }
                } catch (_: Exception) {}

                var thumb = ""
                try {
                    val thumbObj = r.getAsJsonObject("thumbnail")
                        ?.getAsJsonObject("musicThumbnailRenderer")
                        ?.getAsJsonObject("thumbnail")
                    val thumbs = thumbObj?.getAsJsonArray("thumbnails")
                    if (thumbs != null && thumbs.size() > 0) {
                        thumb = thumbs.get(thumbs.size() - 1).asJsonObject.get("url")?.asString ?: ""
                    }
                } catch (_: Exception) {}

                if (title.isNotBlank()) {
                    val cleanThumb = if (thumb.isNotBlank()) {
                        thumb.replace(Regex("=w\\d+-h\\d+.*"), "=w544-h544-l90-rj")
                    } else ""
                    val validThumb = cleanThumb.takeIf { isValidArtistImageUrl(it) } ?: ""
                    list.add(CuratedArtist(name = title.trim(), imageUrl = validThumb))
                }
            }
            list
        } catch (e: Exception) {
            Log.w(TAG, "searchYtmArtists failed: ${e.message}")
            emptyList()
        }
    }

    fun extractPrimaryArtist(artist: String): String {
        return artist.split(Regex("(?i)\\s+(?:&|feat\\.?|ft\\.?|x|,)\\s+|[,&/•;]"))
            .firstOrNull()?.trim()
            ?.takeIf { it.isNotBlank() } ?: artist.trim()
    }

    fun findVerifiedSummary(artistName: String): ArtistSummary? {
        val clean = artistName.trim().lowercase()
        return verifiedCatalog.firstOrNull {
            val catName = it.name.lowercase()
            catName == clean || catName.contains(clean) || clean.contains(catName)
        }
    }

    fun findArtistMatch(query: String): ArtistSummary? = findVerifiedSummary(query)

    suspend fun getArtistDetail(
        artistName: String,
        musicRepository: MusicRepository
    ): ArtistDetail = withContext(Dispatchers.IO) {
        val primaryArtist = extractPrimaryArtist(artistName)
        val cacheKey = primaryArtist.trim().lowercase()
        artistCache[cacheKey]?.let { return@withContext it }

        val summaryMatch = findVerifiedSummary(primaryArtist)
        val resolvedName = summaryMatch?.name ?: primaryArtist

        // 1. Fetch real artist data from YouTube Music InnerTube
        val ytDeferred = async { fetchArtistFromInnerTube(resolvedName) }

        // 2. Fetch official catalog tracks concurrently
        val songsDeferred = async {
            musicRepository.getArtistTracks(resolvedName).getOrDefault(emptyList())
        }

        // 3. Fallback iTunes albums and Saavn profile
        val itunesDeferred = async { fetchAlbumsFromITunes(resolvedName) }
        val saavnDeferred = async { fetchJioSaavnArtistProfile(resolvedName) }

        val ytResult = ytDeferred.await()
        val itunesSongs = songsDeferred.await()
        val (itunesAlbums, itunesSingles) = itunesDeferred.await()
        val (saavnImg, saavnBio, saavnListeners) = saavnDeferred.await()

        // Combine songs: prefer InnerTube top songs + fill up to 25 from iTunes
        val combinedSongs = mutableListOf<Track>()
        if (ytResult != null && ytResult.topSongs.isNotEmpty()) {
            combinedSongs.addAll(ytResult.topSongs)
        }
        for (song in itunesSongs) {
            if (combinedSongs.none { it.title.equals(song.title, ignoreCase = true) }) {
                combinedSongs.add(song)
            }
            if (combinedSongs.size >= 30) break
        }
        if (combinedSongs.isEmpty()) {
            combinedSongs.addAll(itunesSongs)
        }

        // Image / Banner resolution: pick highest resolution validated image available, fallback to top track album art
        val topTrackArtwork = combinedSongs.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl.orEmpty()
        val finalBanner = ytResult?.bannerUrl?.takeIf { isValidArtistImageUrl(it) }
            ?: ytResult?.avatarUrl?.takeIf { isValidArtistImageUrl(it) }
            ?: saavnImg.takeIf { isValidArtistImageUrl(it) }
            ?: summaryMatch?.imageUrl?.takeIf { isValidArtistImageUrl(it) }
            ?: topTrackArtwork

        val finalAvatar = ytResult?.avatarUrl?.takeIf { isValidArtistImageUrl(it) }
            ?: saavnImg.takeIf { isValidArtistImageUrl(it) }
            ?: summaryMatch?.imageUrl?.takeIf { isValidArtistImageUrl(it) }
            ?: finalBanner.takeIf { isValidArtistImageUrl(it) }
            ?: topTrackArtwork

        // Monthly listeners: format cleanly, e.g. "1.02M"
        val rawListeners = ytResult?.monthlyListeners?.takeIf { it.isNotBlank() }
            ?: saavnListeners.takeIf { it.isNotBlank() }
            ?: "1.4M"
        val cleanListeners = formatListenerPill(rawListeners)

        // Bio
        val finalBio = ytResult?.bio?.takeIf { it.isNotBlank() }
            ?: saavnBio.takeIf { it.isNotBlank() }
            ?: "${resolvedName} is an acclaimed recording artist known for chart-topping studio releases, emotive vocal delivery, and global impact."

        // Genres
        val genres = mutableListOf<String>()
        if (ytResult != null && ytResult.genres.isNotEmpty()) {
            genres.addAll(ytResult.genres)
        } else if (summaryMatch != null) {
            genres.addAll(summaryMatch.genre.split("/").map { it.trim() })
        } else {
            genres.addAll(listOf("Pop", "Top Hits", "Contemporary"))
        }

        // Albums & Singles
        val albums = if (ytResult != null && ytResult.albums.isNotEmpty()) ytResult.albums else itunesAlbums
        val singles = if (ytResult != null && ytResult.singles.isNotEmpty()) ytResult.singles else itunesSingles

        // Fans Also Like
        val related = if (ytResult != null && ytResult.fansAlsoLike.isNotEmpty()) {
            ytResult.fansAlsoLike
        } else {
            verifiedCatalog
                .filter { !it.name.equals(resolvedName, ignoreCase = true) }
                .shuffled()
                .take(6)
        }

        val topVideoId = ytResult?.topVideoId?.takeIf { it.isNotBlank() }
            ?: combinedSongs.firstOrNull()?.let {
                if (it.mediaUri.startsWith("vybe://yt/")) it.mediaUri.removePrefix("vybe://yt/") else null
            } ?: ""

        val detail = ArtistDetail(
            name = resolvedName,
            imageUrl = finalAvatar,
            backdropUrl = finalBanner,
            bio = finalBio,
            genres = genres.distinct(),
            monthlyListeners = cleanListeners,
            topSongs = combinedSongs,
            albums = albums,
            singlesAndEps = singles,
            relatedArtists = related,
            topVideoId = topVideoId,
            browseId = ytResult?.browseId
        )

        artistCache[cacheKey] = detail
        detail
    }

    private fun formatListenerPill(raw: String): String {
        val clean = raw
            .replace("monthly listeners", "", ignoreCase = true)
            .replace("monthly audience", "", ignoreCase = true)
            .replace("subscribers", "", ignoreCase = true)
            .replace("listeners", "", ignoreCase = true)
            .replace("Artist •", "", ignoreCase = true)
            .replace("•", "")
            .trim()

        if (clean.endsWith("M", ignoreCase = true) || clean.endsWith("K", ignoreCase = true)) {
            return clean.uppercase()
        }

        val digits = clean.replace(Regex("[^0-9.]"), "")
        val num = digits.toDoubleOrNull()
        return if (num != null) {
            if (num >= 1_000_000) "%.2fM".format(num / 1_000_000.0)
            else if (num >= 1_000) "%.1fK".format(num / 1_000.0)
            else clean.ifBlank { "1.2M" }
        } else {
            clean.ifBlank { "1.2M" }
        }
    }

    private data class InnerTubeArtistData(
        val browseId: String,
        val name: String,
        val bannerUrl: String,
        val avatarUrl: String,
        val monthlyListeners: String,
        val bio: String,
        val genres: List<String>,
        val topSongs: List<Track>,
        val albums: List<ArtistAlbum>,
        val singles: List<ArtistAlbum>,
        val fansAlsoLike: List<ArtistSummary>,
        val topVideoId: String
    )

    private fun fetchArtistFromInnerTube(artistName: String): InnerTubeArtistData? {
        try {
            // Step 1: Search YTM Artists with Artists filter
            val searchApiUrl = "https://music.youtube.com/youtubei/v1/search"
            val searchPayload = JsonObject().apply {
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
                addProperty("query", artistName)
                addProperty("params", "EgWKAQIgAWoKEAkQBRAKEAMQBA%3D%3D")
            }

            val searchReq = Request.Builder()
                .url(searchApiUrl)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Origin", "https://music.youtube.com")
                .post(searchPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val searchResp = httpClient.newCall(searchReq).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return null

            val searchRoot = gson.fromJson(searchResp, JsonObject::class.java)
            val searchItems = mutableListOf<JsonObject>()
            findRenderersByKey(searchRoot, "musicResponsiveListItemRenderer", searchItems)

            var resolvedBrowseId = ""
            var resolvedListeners = ""

            for (r in searchItems) {
                val nav = r.getAsJsonObject("navigationEndpoint")?.getAsJsonObject("browseEndpoint")
                val bid = nav?.get("browseId")?.asString
                if (!bid.isNullOrBlank() && bid.startsWith("UC")) {
                    resolvedBrowseId = bid
                    val subRuns = r.getAsJsonArray("flexColumns")?.get(1)?.asJsonObject
                        ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.getAsJsonObject("text")
                        ?.getAsJsonArray("runs")
                    if (subRuns != null) {
                        for (i in 0 until subRuns.size()) {
                            val txt = subRuns.get(i).asJsonObject.get("text")?.asString ?: ""
                            if (txt.contains("monthly") || txt.contains("subscribers") || txt.contains("audience")) {
                                resolvedListeners = txt
                                break
                            }
                        }
                    }
                    break
                }
            }

            if (resolvedBrowseId.isBlank()) return null

            // Step 2: Browse the channel
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
                addProperty("browseId", resolvedBrowseId)
            }

            val browseReq = Request.Builder()
                .url(browseUrl)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Origin", "https://music.youtube.com")
                .post(browsePayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val browseResp = httpClient.newCall(browseReq).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return null

            val browseRoot = gson.fromJson(browseResp, JsonObject::class.java)

            // Header metadata
            val header = browseRoot.getAsJsonObject("header")
            val h = header?.getAsJsonObject("musicImmersiveHeaderRenderer")
                ?: header?.getAsJsonObject("musicVisualHeaderRenderer")

            var bannerUrl = ""
            var avatarUrl = ""
            val headerThumbs = h?.getAsJsonObject("thumbnail")
                ?.getAsJsonObject("musicThumbnailRenderer")
                ?.getAsJsonObject("thumbnail")
                ?.getAsJsonArray("thumbnails")
            if (headerThumbs != null && headerThumbs.size() > 0) {
                bannerUrl = headerThumbs.get(headerThumbs.size() - 1).asJsonObject.get("url")?.asString ?: ""
                avatarUrl = headerThumbs.get(0).asJsonObject.get("url")?.asString ?: bannerUrl
            }

            val headerSubscribers = h?.getAsJsonObject("subscriptionButton")
                ?.getAsJsonObject("subscribeButtonRenderer")
                ?.getAsJsonObject("subscriberCountText")
                ?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: ""

            if (resolvedListeners.isBlank()) {
                resolvedListeners = headerSubscribers
            }

            var bioText = ""
            val descRuns = h?.getAsJsonObject("description")?.getAsJsonArray("runs")
            if (descRuns != null && descRuns.size() > 0) {
                val sb = StringBuilder()
                for (i in 0 until descRuns.size()) {
                    sb.append(descRuns.get(i).asJsonObject.get("text")?.asString ?: "")
                }
                bioText = sb.toString().substringBefore("From Wikipedia").trim()
            }

            // Top Songs
            val topSongs = mutableListOf<Track>()
            val songRenderers = mutableListOf<JsonObject>()
            findRenderersByKey(browseRoot, "musicResponsiveListItemRenderer", songRenderers)

            for (r in songRenderers) {
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
                val songTitle = try {
                    cols.get(0).asJsonObject
                        .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.getAsJsonObject("text")
                        ?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: ""
                } catch (e: Exception) { "" }
                if (songTitle.isBlank()) continue

                val songArtist = try {
                    cols.get(1).asJsonObject
                        .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.getAsJsonObject("text")
                        ?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: artistName
                } catch (e: Exception) { artistName }

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

                var art = ""
                val artThumbs = r.getAsJsonObject("thumbnail")
                    ?.getAsJsonObject("musicThumbnailRenderer")
                    ?.getAsJsonObject("thumbnail")
                    ?.getAsJsonArray("thumbnails")
                if (artThumbs != null && artThumbs.size() > 0) {
                    art = artThumbs.get(artThumbs.size() - 1).asJsonObject.get("url")?.asString ?: ""
                }

                topSongs.add(
                    Track(
                        id = vid,
                        title = songTitle,
                        artist = songArtist,
                        album = "Single",
                        durationMs = durationMs,
                        artworkUrl = art.replace(Regex("=w\\d+-h\\d+.*"), "=w500-h500-l90-rj"),
                        mediaUri = "vybe://yt/$vid"
                    )
                )
            }

            // Albums, Singles, Fans Also Like, and Top Video
            val albums = mutableListOf<ArtistAlbum>()
            val singles = mutableListOf<ArtistAlbum>()
            val fansAlsoLike = mutableListOf<ArtistSummary>()
            var topVideoId = ""

            val carouselShelves = mutableListOf<JsonObject>()
            findRenderersByKey(browseRoot, "musicCarouselShelfRenderer", carouselShelves)

            for (shelf in carouselShelves) {
                val headerTitle = shelf.getAsJsonObject("header")
                    ?.getAsJsonObject("musicCarouselShelfBasicHeaderRenderer")
                    ?.getAsJsonObject("title")
                    ?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: ""

                val contents = shelf.getAsJsonArray("contents") ?: continue
                for (item in contents) {
                    val twoRow = item.asJsonObject.getAsJsonObject("musicTwoRowItemRenderer") ?: continue
                    val title = twoRow.getAsJsonObject("title")
                        ?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: ""
                    val subtitleRuns = twoRow.getAsJsonObject("subtitle")?.getAsJsonArray("runs")
                    val subText = if (subtitleRuns != null) {
                        val sb = StringBuilder()
                        for (i in 0 until subtitleRuns.size()) {
                            sb.append(subtitleRuns.get(i).asJsonObject.get("text")?.asString ?: "")
                        }
                        sb.toString()
                    } else ""

                    val bid = twoRow.getAsJsonObject("navigationEndpoint")
                        ?.getAsJsonObject("browseEndpoint")?.get("browseId")?.asString ?: ""

                    var artUrl = ""
                    val thumbs = twoRow.getAsJsonObject("thumbnailRenderer")
                        ?.getAsJsonObject("musicThumbnailRenderer")
                        ?.getAsJsonObject("thumbnail")
                        ?.getAsJsonArray("thumbnails")
                    if (thumbs != null && thumbs.size() > 0) {
                        artUrl = thumbs.get(thumbs.size() - 1).asJsonObject.get("url")?.asString ?: ""
                        artUrl = artUrl.replace(Regex("=w\\d+-h\\d+.*"), "=w600-h600-l90-rj")
                    }

                    val year = subText.filter { it.isDigit() }.take(4).ifBlank { "2024" }

                    if (headerTitle.contains("Album", ignoreCase = true)) {
                        albums.add(
                            ArtistAlbum(
                                id = bid.ifBlank { title },
                                title = title,
                                artist = artistName,
                                year = year,
                                artworkUrl = artUrl,
                                isSingleOrEp = false,
                                browseId = bid
                            )
                        )
                    } else if (headerTitle.contains("Single", ignoreCase = true) || headerTitle.contains("EP", ignoreCase = true)) {
                        singles.add(
                            ArtistAlbum(
                                id = bid.ifBlank { title },
                                title = title,
                                artist = artistName,
                                year = year,
                                artworkUrl = artUrl,
                                isSingleOrEp = true,
                                browseId = bid
                            )
                        )
                    } else if (headerTitle.contains("Fans", ignoreCase = true) || headerTitle.contains("Similar", ignoreCase = true)) {
                        fansAlsoLike.add(
                            ArtistSummary(
                                name = title,
                                imageUrl = artUrl,
                                genre = subText.ifBlank { "Artist" },
                                browseId = bid
                            )
                        )
                    } else if (headerTitle.contains("Video", ignoreCase = true) && topVideoId.isBlank()) {
                        val watchVid = twoRow.getAsJsonObject("navigationEndpoint")
                            ?.getAsJsonObject("watchEndpoint")?.get("videoId")?.asString ?: ""
                        if (watchVid.isNotBlank()) topVideoId = watchVid
                    }
                }
            }

            Log.i(TAG, "Loaded InnerTube artist: ${artistName} [${topSongs.size} songs, ${albums.size} albums, ${singles.size} singles, ${fansAlsoLike.size} fans-like]")

            return InnerTubeArtistData(
                browseId = resolvedBrowseId,
                name = artistName,
                bannerUrl = bannerUrl.replace(Regex("=w\\d+-h\\d+.*"), "=w1920-h800-l90-rj"),
                avatarUrl = avatarUrl.replace(Regex("=w\\d+-h\\d+.*"), "=w600-h600-l90-rj"),
                monthlyListeners = resolvedListeners,
                bio = bioText,
                genres = listOf("Pop", "Contemporary", "Top Hits"),
                topSongs = topSongs,
                albums = albums,
                singles = singles,
                fansAlsoLike = fansAlsoLike,
                topVideoId = topVideoId
            )
        } catch (e: Exception) {
            Log.w(TAG, "fetchArtistFromInnerTube failed for $artistName: ${e.message}")
            return null
        }
    }

    private fun findRenderersByKey(obj: com.google.gson.JsonElement, key: String, out: MutableList<JsonObject>) {
        when {
            obj.isJsonObject -> {
                val jObj = obj.asJsonObject
                if (jObj.has(key) && jObj.get(key).isJsonObject) {
                    out.add(jObj.getAsJsonObject(key))
                }
                for (entry in jObj.entrySet()) {
                    findRenderersByKey(entry.value, key, out)
                }
            }
            obj.isJsonArray -> {
                for (item in obj.asJsonArray) {
                    findRenderersByKey(item, key, out)
                }
            }
        }
    }

    private fun fetchAlbumsFromITunes(artistName: String): Pair<List<ArtistAlbum>, List<ArtistAlbum>> {
        val albums = mutableListOf<ArtistAlbum>()
        val singles = mutableListOf<ArtistAlbum>()
        try {
            val encoded = URLEncoder.encode(artistName, "UTF-8")
            val url = "https://itunes.apple.com/search?term=$encoded&entity=album&limit=25&country=IN"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val jsonString = httpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return Pair(emptyList(), emptyList())

            val root = gson.fromJson(jsonString, JsonObject::class.java)
            val results = root.getAsJsonArray("results") ?: return Pair(emptyList(), emptyList())

            for (elem in results) {
                if (!elem.isJsonObject) continue
                val obj = elem.asJsonObject
                val collName = obj.get("collectionName")?.asString ?: continue
                val collId = obj.get("collectionId")?.asString ?: collName.hashCode().toString()
                val artist = obj.get("artistName")?.asString ?: artistName
                val rawDate = obj.get("releaseDate")?.asString ?: ""
                val year = if (rawDate.length >= 4) rawDate.substring(0, 4) else "2024"
                val rawArt = obj.get("artworkUrl100")?.asString ?: ""
                val art = rawArt
                    .replace("100x100bb.jpg", "600x600bb.jpg")
                    .replace("100x100bb.png", "600x600bb.png")
                val trackCount = obj.get("trackCount")?.asInt ?: 1

                val isSingleOrEp = collName.contains("- Single", ignoreCase = true) ||
                        collName.contains("- EP", ignoreCase = true) ||
                        collName.contains("(Single)", ignoreCase = true)

                val cleanTitle = collName
                    .replace(" - Single", "")
                    .replace(" - EP", "")
                    .replace(" (Single)", "")
                    .trim()

                val item = ArtistAlbum(
                    id = collId,
                    title = cleanTitle,
                    artist = artist,
                    year = year,
                    artworkUrl = art,
                    isSingleOrEp = isSingleOrEp,
                    trackCount = trackCount,
                    browseId = null
                )

                if (isSingleOrEp) {
                    if (singles.none { it.title.equals(cleanTitle, ignoreCase = true) }) {
                        singles.add(item)
                    }
                } else {
                    if (albums.none { it.title.equals(cleanTitle, ignoreCase = true) }) {
                        albums.add(item)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchAlbumsFromITunes failed for $artistName: ${e.message}")
        }
        return Pair(albums, singles)
    }

    private data class ProfileInfo(val image: String, val bio: String, val followers: String)

    private fun fetchJioSaavnArtistProfile(artistName: String): ProfileInfo {
        try {
            val encoded = URLEncoder.encode(artistName, "UTF-8")
            val searchUrl = "https://www.jiosaavn.com/api.php?__call=search.getArtistResults&_format=json&_marker=0&ctx=android&q=$encoded&n=1"
            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val jsonString = httpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return ProfileInfo("", "", "")

            val root = gson.fromJson(jsonString, JsonObject::class.java)
            val results = root.getAsJsonArray("results")
            if (results == null || results.size() == 0) return ProfileInfo("", "", "")

            val first = results.get(0).asJsonObject
            val artistId = first.get("id")?.asString ?: first.get("artistid")?.asString
            val rawImg = first.get("image")?.asString ?: ""
            val highResImg = rawImg
                .replace("50x50", "500x500")
                .replace("150x150", "500x500")

            var bioText = ""
            var followers = ""

            if (!artistId.isNullOrBlank()) {
                val detailUrl = "https://www.jiosaavn.com/api.php?__call=artist.getArtistPageDetails&_format=json&_marker=0&ctx=android&artistId=$artistId&n_song=1&n_album=1"
                val detailReq = Request.Builder()
                    .url(detailUrl)
                    .header("User-Agent", "Mozilla/5.0")
                    .build()

                val detailJson = httpClient.newCall(detailReq).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string() else null
                }
                if (!detailJson.isNullOrBlank()) {
                    val detailRoot = gson.fromJson(detailJson, JsonObject::class.java)
                    if (detailRoot.has("bio") && detailRoot.get("bio").isJsonArray) {
                        val bioArray = detailRoot.getAsJsonArray("bio")
                        val sb = StringBuilder()
                        for (b in bioArray) {
                            if (b.isJsonObject) {
                                val t = b.asJsonObject.get("text")?.asString ?: ""
                                if (t.isNotBlank()) {
                                    if (sb.isNotEmpty()) sb.append("\n\n")
                                    sb.append(t.trim())
                                }
                            }
                        }
                        bioText = sb.toString()
                    }
                    if (bioText.isBlank()) {
                        bioText = detailRoot.get("subtitle")?.asString ?: ""
                    }

                    val fans = detailRoot.get("fan_count")?.asString
                        ?: detailRoot.get("follower_count")?.asString
                    if (!fans.isNullOrBlank()) {
                        val fanNum = fans.toLongOrNull()
                        followers = if (fanNum != null && fanNum > 1_000_000) {
                            "%.1fM".format(fanNum / 1_000_000.0)
                        } else if (fanNum != null && fanNum > 1_000) {
                            "%.1fK".format(fanNum / 1_000.0)
                        } else {
                            fans
                        }
                    }
                }
            }

            return ProfileInfo(highResImg, bioText, followers)
        } catch (e: Exception) {
            Log.w(TAG, "fetchJioSaavnArtistProfile error for $artistName: ${e.message}")
            return ProfileInfo("", "", "")
        }
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
}
