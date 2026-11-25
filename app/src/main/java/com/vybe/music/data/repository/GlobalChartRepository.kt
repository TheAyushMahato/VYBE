package com.vybe.music.data.repository

import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.vybe.music.data.LocalMusicStorage
import com.vybe.music.model.Track
import com.vybe.music.utils.TrackUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ── Deezer DTOs ─────────────────────────────────────────────────────────────
data class DeezerChartDto(
    val data: List<DeezerTrackItemDto>? = null,
    val total: Int = 0
)

data class DeezerTrackItemDto(
    val id: Long? = null,
    val title: String? = null,
    val title_short: String? = null,
    val duration: Long? = null,
    val artist: DeezerArtistItemDto? = null,
    val album: DeezerAlbumItemDto? = null
)

data class DeezerArtistItemDto(
    val id: Long? = null,
    val name: String? = null
)

data class DeezerAlbumItemDto(
    val id: Long? = null,
    val title: String? = null,
    val cover_xl: String? = null,
    val cover_big: String? = null,
    val cover_medium: String? = null
)

// ── Apple Music RSS DTOs ────────────────────────────────────────────────────
data class AppleMusicRssDto(
    val feed: AppleMusicFeedItemDto? = null
)

data class AppleMusicFeedItemDto(
    val results: List<AppleMusicResultItemDto>? = null
)

data class AppleMusicResultItemDto(
    val id: String? = null,
    val name: String? = null,
    val artistName: String? = null,
    val artworkUrl100: String? = null
)

object GlobalChartRepository {
    private const val TAG = "GlobalChartRepository"

    val BLOCKED_CHART_KEYWORDS = listOf(
        "karaoke",
        "instrumental",
        "sped up",
        "slowed",
        "tribute",
        "cover",
        "remix",
        "compilation",
        "blockbusters",
        "greatest hits"
    )

    // Curated high-quality fallback chart (strictly official albums/soundtracks; ZERO - EP or - Single entries)
    val DEFAULT_TOP5_CHART = listOf(
        Track(
            id = "1693891139",
            title = "One Of The Girls",
            artist = "The Weeknd, JENNIE & Lily Rose Depp",
            album = "The Idol Episode 4",
            durationMs = 244685L,
            artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/db/28/e7/db28e7c3-f53a-76c8-eaa4-d92a22e4f160/23UMGIM72115.rgb.jpg/1200x1200bb.jpg",
            mediaUri = "https://aac.saavncdn.com/262/cfee281799a04f2be345a57bdeef26de_320.mp4",
            defaultDominantColorHex = 0xFFE50914
        ),
        Track(
            id = "1495799403",
            title = "Levitating",
            artist = "Dua Lipa",
            album = "Future Nostalgia",
            durationMs = 203807L,
            artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/6c/11/d6/6c11d681-aa3a-d59e-4c2e-f77e181026ab/190295092665.jpg/1200x1200bb.jpg",
            mediaUri = "https://aac.saavncdn.com/117/190295092665_320.mp4",
            defaultDominantColorHex = 0xFF9C27B0
        ),
        Track(
            id = "rjkrTnma",
            title = "Kesariya",
            artist = "Pritam, Arijit Singh",
            album = "Brahmastra",
            durationMs = 268000L,
            artworkUrl = "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg",
            mediaUri = "https://aac.saavncdn.com/871/c2febd353f3a076a406fa37510f31f9f_320.mp4",
            defaultDominantColorHex = 0xFFFF5722
        ),
        Track(
            id = "aAOXwvz-",
            title = "Born to Shine",
            artist = "Diljit Dosanjh",
            album = "G.O.A.T.",
            durationMs = 214000L,
            artworkUrl = "https://c.saavncdn.com/597/G-O-A-T-Punjabi-2020-20240708055140-500x500.jpg",
            mediaUri = "https://aac.saavncdn.com/597/f1efd650819d3f427bd10e8b9addcd40_320.mp4",
            defaultDominantColorHex = 0xFF00BCD4
        ),
        Track(
            id = "DF6eazs2",
            title = "Winning Speech",
            artist = "Karan Aujla",
            album = "Winning Speech",
            durationMs = 227000L,
            artworkUrl = "https://c.saavncdn.com/089/Winning-Speech-Punjabi-2024-20260626013220-500x500.jpg",
            mediaUri = "https://aac.saavncdn.com/089/64beffa430e4c948223ec6bfcc3a13f0_320.mp4",
            defaultDominantColorHex = 0xFFE91E63
        )
    )

    // Strict 5-second timeout client so fallbacks trigger instantly if unreachable
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()

    private fun logD(tag: String, msg: String) {
        try { Log.d(tag, msg) } catch (e: Throwable) { println("[$tag] $msg") }
    }
    private fun logI(tag: String, msg: String) {
        try { Log.i(tag, msg) } catch (e: Throwable) { println("[$tag] $msg") }
    }
    private fun logW(tag: String, msg: String) {
        try { Log.w(tag, msg) } catch (e: Throwable) { System.err.println("[$tag] $msg") }
    }
    private fun logE(tag: String, msg: String) {
        try { Log.e(tag, msg) } catch (e: Throwable) { System.err.println("[$tag] $msg") }
    }

    /**
     * Parses Deezer Chart JSON into a strictly filtered, deduplicated list of top 5 ranked tracks
     * with verified 1000x1000 HD artwork.
     */
    fun parseDeezerChartJson(jsonString: String): List<Track> {
        val dto = try {
            gson.fromJson(jsonString, DeezerChartDto::class.java)
        } catch (e: Exception) {
            logE(TAG, "Error parsing Deezer chart json: ${e.message}")
            null
        }

        val rawTracks = dto?.data.orEmpty()
        val results = mutableListOf<Track>()
        val seenSignatures = mutableSetOf<String>()

        for (item in rawTracks) {
            val title = (item.title_short?.takeIf { it.isNotBlank() } ?: item.title ?: "").trim()
            val artist = (item.artist?.name ?: "").trim()
            val album = (item.album?.title ?: "").trim()

            if (title.isBlank() || artist.isBlank()) continue

            // 1. Strict Keyword Filter (karaoke, remix, slowed, compilation, etc.)
            val lowerTitle = title.lowercase(Locale.ROOT)
            val lowerAlbum = album.lowercase(Locale.ROOT)
            val isBlocked = BLOCKED_CHART_KEYWORDS.any { lowerTitle.contains(it) || lowerAlbum.contains(it) }
            if (isBlocked) {
                logD(TAG, "Deezer filter excluded: '$title' by '$artist'")
                continue
            }

            // 2. Normalized Title + Artist Deduplication (Key = "$normalizedTitle|$primaryArtist")
            val signature = TrackUtils.normalizeTrackKey(title, artist)
            if (!seenSignatures.add(signature)) {
                logD(TAG, "Deezer duplicate excluded: '$title' by '$artist'")
                continue
            }

            // 3. HD 1000x1000 Artwork URL
            var artwork = item.album?.cover_xl ?: item.album?.cover_big ?: item.album?.cover_medium ?: ""
            if (artwork.contains("500x500")) {
                artwork = artwork.replace("500x500", "1000x1000")
            } else if (artwork.contains("250x250")) {
                artwork = artwork.replace("250x250", "1000x1000")
            }

            val durationMs = if ((item.duration ?: 0L) > 0) item.duration!! * 1000L else 200000L

            results.add(
                Track(
                    id = "deezer_${item.id ?: System.currentTimeMillis()}",
                    title = title,
                    artist = artist,
                    album = album,
                    durationMs = durationMs,
                    artworkUrl = artwork,
                    mediaUri = "" // Stream resolved dynamically on play
                )
            )

            if (results.size >= 5) break
        }

        return results
    }

    /**
     * Parses Apple Music Global Top Songs RSS JSON into a filtered list of top ranked tracks
     * with upgraded 1000x1000bb artwork.
     */
    fun parseAppleMusicRssJson(jsonString: String): List<Track> {
        val dto = try {
            gson.fromJson(jsonString, AppleMusicRssDto::class.java)
        } catch (e: Exception) {
            logE(TAG, "Error parsing Apple Music RSS json: ${e.message}")
            null
        }

        val rawResults = dto?.feed?.results.orEmpty()
        val results = mutableListOf<Track>()
        val seenSignatures = mutableSetOf<String>()

        for (item in rawResults) {
            val title = (item.name ?: "").trim()
            val artist = (item.artistName ?: "").trim()

            if (title.isBlank() || artist.isBlank()) continue

            val lowerTitle = title.lowercase(Locale.ROOT)
            val isBlocked = BLOCKED_CHART_KEYWORDS.any { lowerTitle.contains(it) }
            if (isBlocked) continue

            val signature = TrackUtils.normalizeTrackKey(title, artist)
            if (!seenSignatures.add(signature)) continue

            val rawArtwork = item.artworkUrl100 ?: ""
            val hdArtwork = rawArtwork.replace("100x100bb", "1000x1000bb")

            results.add(
                Track(
                    id = "apple_${item.id ?: System.currentTimeMillis()}",
                    title = title,
                    artist = artist,
                    album = title,
                    durationMs = 210000L,
                    artworkUrl = hdArtwork,
                    mediaUri = ""
                )
            )

            if (results.size >= 5) break
        }

        return results
    }

    /**
     * Fallback parser for iTunes Top Songs RSS JSON.
     */
    fun parseITunesRssJson(jsonString: String, maxLimit: Int = 25): List<Track> {
        val root = try {
            gson.fromJson(jsonString, JsonObject::class.java)
        } catch (e: Exception) {
            null
        }
        val feed = root?.getAsJsonObject("feed")
        val entries = feed?.getAsJsonArray("entry") ?: return emptyList()
        val results = mutableListOf<Track>()
        val seenSignatures = mutableSetOf<String>()

        for (entryElem in entries) {
            if (!entryElem.isJsonObject) continue
            val entry = entryElem.asJsonObject
            val title = entry.getAsJsonObject("im:name")?.get("label")?.asString?.trim() ?: continue
            val artist = entry.getAsJsonObject("im:artist")?.get("label")?.asString?.trim() ?: continue

            val lowerTitle = title.lowercase(Locale.ROOT)
            if (BLOCKED_CHART_KEYWORDS.any { lowerTitle.contains(it) }) continue

            val signature = TrackUtils.normalizeTrackKey(title, artist)
            if (!seenSignatures.add(signature)) continue

            var artwork = ""
            val images = entry.getAsJsonArray("im:image")
            if (images != null && images.size() > 0) {
                val lastImg = images.get(images.size() - 1).asJsonObject
                val rawUrl = lastImg.get("label")?.asString ?: ""
                artwork = rawUrl.replace(Regex("\\d+x\\d+bb"), "1000x1000bb")
            }

            results.add(
                Track(
                    id = "itunes_${System.currentTimeMillis()}_${results.size}",
                    title = title,
                    artist = artist,
                    album = title,
                    durationMs = 210000L,
                    artworkUrl = artwork,
                    mediaUri = ""
                )
            )

            if (results.size >= maxLimit) break
        }

        return results
    }

    private fun executeHttp(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:130.0) Gecko/130.0 Firefox/130.0")
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    logW(TAG, "HTTP ${response.code} for $url")
                    null
                }
            }
        } catch (e: Exception) {
            logW(TAG, "HTTP call failed for $url: ${e.message}")
            null
        }
    }

    /**
     * Dedicated fetchGlobalTop5Daily running on Dispatchers.IO with strict order & 5s timeouts.
     */
    suspend fun fetchGlobalTop5Daily(
        storage: LocalMusicStorage? = null,
        forceRefresh: Boolean = false
    ): List<Track> = withContext(Dispatchers.IO) {
        val todayDateKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }

        // On startup or fetch, clear older/stale Top 5 cache keys
        storage?.clearStaleGlobalTop5Cache(todayDateKey)

        // Check local date-keyed cache if not force refreshing
        if (!forceRefresh && storage != null) {
            val cached = storage.getCachedGlobalTop5(todayDateKey)
            if (!cached.isNullOrEmpty() && cached.size >= 5) {
                logD(TAG, "Loaded Top 5 Daily from date-keyed cache for $todayDateKey: ${cached.map { it.title }}")
                return@withContext cached
            }
        }

        // ── 1. Primary: Deezer Worldwide Chart API ───────────────────────────
        logD(TAG, "Fetching live Deezer Global Chart...")
        val deezerChartJson = executeHttp("https://api.deezer.com/chart/0/tracks?limit=20")
        var tracks = deezerChartJson?.let { parseDeezerChartJson(it) }.orEmpty()

        // Fallback to Deezer Worldwide Chart Playlist (3155776842) if chart/0 is empty in current region
        if (tracks.size < 5) {
            logD(TAG, "Deezer chart/0 empty, checking Deezer Top Worldwide Playlist...")
            val deezerPlaylistJson = executeHttp("https://api.deezer.com/playlist/3155776842/tracks?limit=20")
            val playlistTracks = deezerPlaylistJson?.let { parseDeezerChartJson(it) }.orEmpty()
            if (playlistTracks.size >= 5) {
                tracks = playlistTracks
            }
        }

        // ── 2. Secondary Fallback: Apple Music Global/US Top Songs RSS ────────
        if (tracks.size < 5) {
            logW(TAG, "Deezer returned fewer than 5 tracks, trying Apple Music Top Songs RSS...")
            val appleJson = executeHttp("https://rss.marketingtools.apple.com/api/v2/us/music/most-played/20/songs.json")
            val appleTracks = appleJson?.let { parseAppleMusicRssJson(it) }.orEmpty()
            if (appleTracks.size >= 5) {
                tracks = appleTracks
            }
        }

        // ── 3. Tertiary Fallback: iTunes India & US RSS ─────────────────────────
        if (tracks.size < 5) {
            logW(TAG, "Checking live iTunes India Top Songs RSS...")
            val itunesInJson = executeHttp("https://itunes.apple.com/in/rss/topsongs/limit=20/json")
            val inTracks = itunesInJson?.let { parseITunesRssJson(it, 10) }.orEmpty()
            if (inTracks.size >= 5) {
                tracks = inTracks
            } else {
                val itunesJson = executeHttp("https://itunes.apple.com/us/rss/topsongs/limit=20/json")
                val itunesTracks = itunesJson?.let { parseITunesRssJson(it, 10) }.orEmpty()
                if (itunesTracks.size >= 5) {
                    tracks = itunesTracks
                }
            }
        }

        // ── Cache & Return ───────────────────────────────────────────────────
        // Merge with DEFAULT_TOP5_CHART if fewer than 5 tracks found, then strictly deduplicate
        val pool = if (tracks.size < 5) tracks + DEFAULT_TOP5_CHART else tracks
        val dedupedTracks = TrackUtils.deduplicateTracks(pool)
        val top5 = dedupedTracks.take(5)
        storage?.saveCachedGlobalTop5(todayDateKey, top5)
        logI(TAG, "Successfully resolved and cached live Global Top 5 for $todayDateKey: ${top5.map { "${it.title} by ${it.artist} (${it.album})" }}")
        return@withContext top5
    }

    /**
     * Fetches today's live real-time trending songs (India + Global) with 6-8h cache refresh.
     */
    suspend fun fetchLiveTrendingCharts(
        storage: LocalMusicStorage? = null,
        forceRefresh: Boolean = false
    ): List<Track> = withContext(Dispatchers.IO) {
        if (!forceRefresh && storage != null) {
            val cached = storage.getCachedTrendingTracks()
            if (!cached.isNullOrEmpty() && cached.size >= 10) {
                logD(TAG, "Loaded live trending tracks from cache (${cached.size} tracks)")
                return@withContext cached
            }
        }

        logD(TAG, "Fetching live real-time trending charts (India + Global)...")
        val candidates = mutableListOf<Track>()

        // 1. Fetch live iTunes India Top Songs
        try {
            val inJson = executeHttp("https://itunes.apple.com/in/rss/topsongs/limit=50/json")
            if (!inJson.isNullOrBlank()) {
                val inTracks = parseITunesRssJson(inJson, maxLimit = 35)
                candidates.addAll(inTracks)
            }
        } catch (e: Exception) {
            logW(TAG, "Error fetching India Top Songs: ${e.message}")
        }

        // 2. Fetch live iTunes Global/US Top Songs
        try {
            val usJson = executeHttp("https://itunes.apple.com/us/rss/topsongs/limit=30/json")
            if (!usJson.isNullOrBlank()) {
                val usTracks = parseITunesRssJson(usJson, maxLimit = 20)
                candidates.addAll(usTracks)
            }
        } catch (e: Exception) {
            logW(TAG, "Error fetching Global Top Songs: ${e.message}")
        }

        val deduped = TrackUtils.deduplicateTracks(candidates)
        val finalTrending = if (deduped.isNotEmpty()) deduped else DEFAULT_TOP5_CHART

        storage?.saveCachedTrendingTracks(finalTrending)
        logI(TAG, "Cached and returned ${finalTrending.size} live trending tracks")
        return@withContext finalTrending
    }
}
