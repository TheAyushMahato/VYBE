package com.vybe.music.ui.screens

import android.content.Context
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import com.vybe.music.data.repository.ArtistRepository
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.vybe.music.model.ArtistSummary
import com.vybe.music.model.Track
import com.vybe.music.ui.components.ArtistSpotlightCard
import com.vybe.music.ui.components.MiniEqualizerBars
import com.vybe.music.ui.components.VybeCrystalHeader
import com.vybe.music.ui.components.VybeGlassTopHeader
import com.vybe.music.ui.components.VybeTransparentGlassHeader
import com.vybe.music.ui.components.bouncyClickable
import com.vybe.music.ui.theme.AmoledBlack
import com.vybe.music.ui.theme.PureWhite
import com.vybe.music.ui.theme.SurfaceCard
import com.vybe.music.ui.theme.VybeDisplayFontFamily
import com.vybe.music.utils.TrackUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SpotlightArtist(
    val name: String,
    val imageUrl: String,
    val genre: String
)

val spotlightArtists = listOf(
    SpotlightArtist(
        name = "Arijit Singh",
        imageUrl = "https://c.saavncdn.com/artists/Arijit_Singh_500x500.jpg",
        genre = "Bollywood / Soul"
    ),
    SpotlightArtist(
        name = "Diljit Dosanjh",
        imageUrl = "https://c.saavncdn.com/artists/Diljit_Dosanjh_500x500.jpg",
        genre = "Punjabi / Pop"
    ),
    SpotlightArtist(
        name = "Karan Aujla",
        imageUrl = "https://c.saavncdn.com/artists/Karan_Aujla_005_20260925061936_500x500.jpg",
        genre = "Punjabi / Hip-Hop"
    ),
    SpotlightArtist(
        name = "Pritam",
        imageUrl = "https://c.saavncdn.com/artists/Pritam_Chakraborty-20170711073326_500x500.jpg",
        genre = "Bollywood Composer"
    ),
    SpotlightArtist(
        name = "Alan Walker",
        imageUrl = "https://c.saavncdn.com/artists/Alan_Walker_003_20231116095443_500x500.jpg",
        genre = "Electronic / EDM"
    ),
    SpotlightArtist(
        name = "AP Dhillon",
        imageUrl = "https://c.saavncdn.com/artists/AP_Dhillon_004_20251023102150_500x500.jpg",
        genre = "Indo-Western Hip-Hop"
    ),
    SpotlightArtist(
        name = "The Weeknd",
        imageUrl = "https://c.saavncdn.com/artists/The_Weeknd_002_20241003071400_500x500.jpg",
        genre = "R&B / Synthwave"
    )
)

data class EditorialAlbum(
    val title: String,
    val subtitle: String,
    val artist: String = "",
    val year: String = "",
    val artworkUrl: String,
    val browseId: String? = null,
    val sampleTrack: Track
)

data class EditorialMix(
    val title: String,
    val subtitle: String,
    val artworkUrl: String,
    val tracks: List<Track>
)

@Composable
fun HomeScreen(
    currentTrack: Track?,
    isPlaying: Boolean,
    trendingTracks: List<Track>,
    top5DailyTracks: List<Track> = emptyList(),
    quickPicks: List<Track> = emptyList(),
    freshFinds: List<Track> = emptyList(),
    selectedArtist: String = "Arijit Singh",
    userSelectedArtists: Set<String> = emptySet(),
    userGenres: List<String> = emptyList(),
    favoriteTrackIds: Set<String> = emptySet(),
    likedTracks: List<Track> = emptyList(),
    artistTracks: List<Track> = emptyList(),
    isArtistLoading: Boolean = false,
    isHomeLoading: Boolean = false,
    onArtistSelect: (String) -> Unit = {},
    onSearchArtist: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    onTrackClick: (Track, List<Track>) -> Unit = { _, _ -> },
    onPlayTracks: (List<Track>, Boolean) -> Unit = { _, _ -> },
    onOpenArtistDetail: (String, Boolean) -> Unit = { _, _ -> },
    onAlbumClick: (browseId: String?, albumTitle: String, artistName: String, artworkUrl: String, year: String) -> Unit = { _, _, _, _, _ -> },
    onMoodSelect: (String) -> Unit = {},
    onGenreClick: (String) -> Unit = {},
    onLikedSongsQuickAccess: () -> Unit = {},
    onDiscoverMixQuickAccess: () -> Unit = {},
    onYourMixQuickAccess: () -> Unit = {},
    userPhotoUrl: String? = null,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val liveDate = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                LocalDate.now().format(
                    DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
                ).uppercase(Locale.getDefault())
            } catch (e: Exception) {
                "TUESDAY, NOVEMBER 24"
            }
        } else {
            "TUESDAY, NOVEMBER 24"
        }
    }

    // Unified master track pool: combines top5DailyTracks and trendingTracks, strictly deduplicated (preferring clean albums & library likes)
    val allTracks = remember(trendingTracks, top5DailyTracks, favoriteTrackIds) {
        val base = if (trendingTracks.isNotEmpty()) trendingTracks else com.vybe.music.data.repository.GlobalChartRepository.DEFAULT_TOP5_CHART
        val combined = top5DailyTracks + base
        TrackUtils.deduplicateTracks(combined, favoriteTrackIds)
    }

    // Dynamic spotlight candidate artists: verified catalog blended with live trending artists
    val spotlightCandidates = remember(trendingTracks) {
        val catalog = ArtistRepository.verifiedCatalog
        val dynamicNames = trendingTracks.map { it.artist }.distinct().take(6)
        val dynamicSummaries = dynamicNames.mapNotNull { artistName ->
            val track = trendingTracks.firstOrNull { it.artist == artistName }
            if (track != null) {
                ArtistSummary(
                    name = artistName,
                    imageUrl = track.artworkUrl,
                    genre = "Trending Artist",
                    topTrackTitle = track.title
                )
            } else null
        }
        (catalog + dynamicSummaries).distinctBy { it.name.lowercase().trim() }
    }

    // Auto-rotation state: random/fresh artist on initial mount / app reopen, then rotates every 12 seconds
    var currentSpotlightIndex by rememberSaveable {
        val initialSize = ArtistRepository.verifiedCatalog.size
        mutableIntStateOf(if (initialSize > 0) (0 until initialSize).random() else 0)
    }

    LaunchedEffect(spotlightCandidates.size) {
        if (spotlightCandidates.isEmpty()) return@LaunchedEffect
        while (isActive) {
            delay(12000L)
            currentSpotlightIndex = (currentSpotlightIndex + 1) % spotlightCandidates.size
        }
    }

    // 24-Hour rotating famous song poster for Infinite Radio
    val context = LocalContext.current
    val dailyHeroPosterUrl = remember(allTracks, top5DailyTracks, trendingTracks) {
        val prefs = context.getSharedPreferences("vybe_infinite_radio_hero", Context.MODE_PRIVATE)
        val todayStr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                LocalDate.now().toString()
            } catch (e: Exception) {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            }
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }

        val cachedDate = prefs.getString("poster_date", null)
        val cachedUrl = prefs.getString("poster_url", null)

        if (!cachedUrl.isNullOrEmpty() && cachedDate == todayStr) {
            if (cachedUrl.contains("500x500")) cachedUrl.replace("500x500", "1000x1000") else cachedUrl
        } else {
            // Pick one high-resolution poster from top famous/trending tracks feed
            val candidateTrack = top5DailyTracks.firstOrNull { it.artworkUrl.isNotBlank() }
                ?: trendingTracks.firstOrNull { it.artworkUrl.isNotBlank() }
                ?: allTracks.firstOrNull { it.artworkUrl.isNotBlank() }
            var chosenUrl = candidateTrack?.artworkUrl?.ifEmpty { null }
                ?: "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-1000x1000.jpg"
            if (chosenUrl.contains("500x500")) {
                chosenUrl = chosenUrl.replace("500x500", "1000x1000")
            }

            prefs.edit()
                .putString("poster_date", todayStr)
                .putString("poster_url", chosenUrl)
                .apply()
            chosenUrl
        }
    }

    // Top 5 tracks (mapped to best album versions from allTracks)
    val heroTracks = remember(top5DailyTracks, allTracks, favoriteTrackIds) {
        val base = if (top5DailyTracks.isNotEmpty()) {
            top5DailyTracks.map { chartTrack ->
                val key = TrackUtils.normalizeTrackKey(chartTrack.title, chartTrack.artist)
                allTracks.firstOrNull { TrackUtils.normalizeTrackKey(it.title, it.artist) == key } ?: chartTrack
            }
        } else {
            allTracks
        }
        TrackUtils.deduplicateTracks(base, favoriteTrackIds).take(5)
    }

    // 1. Quick picks tracks (deduplicated and excludes any tracks already in the Hero Carousel)
    val quickPicksTracks = remember(quickPicks, allTracks, heroTracks) {
        val heroKeys = heroTracks.map { TrackUtils.normalizeTrackKey(it.title, it.artist) }.toSet()
        val pool = if (quickPicks.isNotEmpty()) quickPicks else allTracks
        val nonHero = pool.filter { TrackUtils.normalizeTrackKey(it.title, it.artist) !in heroKeys }
        if (nonHero.isNotEmpty()) nonHero.take(8) else pool.take(8)
    }

    // 2. Fresh finds tracks (unique from both Hero and Quick Picks)
    val freshFindsTracks = remember(freshFinds, allTracks, heroTracks, quickPicksTracks) {
        val usedKeys = (heroTracks + quickPicksTracks).map { TrackUtils.normalizeTrackKey(it.title, it.artist) }.toSet()
        val pool = if (freshFinds.isNotEmpty()) freshFinds else allTracks
        val remaining = pool.filter { TrackUtils.normalizeTrackKey(it.title, it.artist) !in usedKeys }
        if (remaining.isNotEmpty()) remaining.take(8) else pool.drop(2).take(8).ifEmpty { pool }
    }

    // 3. Jump back in tracks
    val jumpBackInTracks = remember(allTracks) {
        allTracks.reversed().take(8)
    }

    // 4. Mixes to explore items (dynamically generated from user's selected/top artists)
    val mixes = remember(allTracks, userSelectedArtists) {
        val userArtistsList = userSelectedArtists.toList()
        val generated = mutableListOf<EditorialMix>()

        for (artistName in userArtistsList.take(3)) {
            val matchingTracks = allTracks.filter { it.artist.contains(artistName, ignoreCase = true) }
            val summary = ArtistRepository.findVerifiedSummary(artistName)
            val artUrl = summary?.imageUrl?.ifBlank { null }
                ?: matchingTracks.firstOrNull()?.artworkUrl
                ?: "https://c.saavncdn.com/artists/The_Weeknd_002_20241003071400_500x500.jpg"

            generated.add(
                EditorialMix(
                    title = "$artistName Mix",
                    subtitle = "Top tracks, melodic vibes & hits",
                    artworkUrl = artUrl,
                    tracks = matchingTracks.ifEmpty { allTracks.shuffled().take(5) }
                )
            )
        }

        if (generated.size < 4) {
            generated.add(
                EditorialMix(
                    title = "Late Night Chill",
                    subtitle = "Emotional acoustic & atmospheric vibes",
                    artworkUrl = "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg",
                    tracks = allTracks.shuffled(java.util.Random(11)).take(5)
                )
            )
        }
        if (generated.size < 4) {
            generated.add(
                EditorialMix(
                    title = "Discover Weekly",
                    subtitle = "Fresh tracks picked for your taste",
                    artworkUrl = "https://c.saavncdn.com/artists/Alan_Walker_003_20231116095443_500x500.jpg",
                    tracks = allTracks.shuffled(java.util.Random(23)).take(5)
                )
            )
        }
        generated.take(4)
    }

    // 5. Dynamic Artists for you (prioritizing user's chosen artists)
    val homeArtistsForYou = remember(userSelectedArtists) {
        val userArtistsList = userSelectedArtists.toList()
        val list = mutableListOf<SpotlightArtist>()
        for (artistName in userArtistsList) {
            val summary = ArtistRepository.findVerifiedSummary(artistName)
            val pic = summary?.imageUrl?.ifBlank { null } ?: "https://c.saavncdn.com/artists/The_Weeknd_002_20241003071400_500x500.jpg"
            list.add(SpotlightArtist(name = artistName, imageUrl = pic, genre = summary?.genre ?: "Contemporary Pop"))
        }
        for (sa in spotlightArtists) {
            if (list.none { it.name.equals(sa.name, ignoreCase = true) }) {
                list.add(sa)
            }
        }
        list.take(8)
    }

    // 6. Trending today tracks
    val trendingTodayTracks = remember(allTracks) {
        allTracks
    }

    // 7. Favorites to revisit tracks (strictly pulls user's actual liked tracks, hides if empty)
    val favoriteTracks = remember(likedTracks) {
        if (likedTracks.isNotEmpty()) likedTracks.shuffled(java.util.Random(42)).take(8) else emptyList()
    }

    // 8. Albums for you items
    val albums = remember(allTracks) {
        listOf(
            EditorialAlbum(
                title = "Brahmastra",
                subtitle = "Pritam • 2022",
                artist = "Pritam",
                year = "2022",
                artworkUrl = "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg",
                sampleTrack = allTracks.firstOrNull { it.album.contains("Brahmastra", ignoreCase = true) } ?: allTracks.first()
            ),
            EditorialAlbum(
                title = "G.O.A.T.",
                subtitle = "Diljit Dosanjh • 2020",
                artist = "Diljit Dosanjh",
                year = "2020",
                browseId = "MPREb_grVglC0todv",
                artworkUrl = "https://c.saavncdn.com/597/G-O-A-T-Punjabi-2020-20240708055140-500x500.jpg",
                sampleTrack = allTracks.firstOrNull { it.album.contains("G.O.A.T", ignoreCase = true) } ?: allTracks.getOrElse(1) { allTracks.first() }
            ),
            EditorialAlbum(
                title = "Bad Newz",
                subtitle = "Karan Aujla • 2024",
                artist = "Karan Aujla",
                year = "2024",
                artworkUrl = "https://c.saavncdn.com/992/Bad-Newz-Hindi-2024-20250730113701-500x500.jpg",
                sampleTrack = allTracks.firstOrNull { it.album.contains("Bad Newz", ignoreCase = true) } ?: allTracks.getOrElse(2) { allTracks.first() }
            ),
            EditorialAlbum(
                title = "Winning Speech",
                subtitle = "Karan Aujla • 2024",
                artist = "Karan Aujla",
                year = "2024",
                artworkUrl = "https://c.saavncdn.com/089/Winning-Speech-Punjabi-2024-20260626013220-500x500.jpg",
                sampleTrack = allTracks.firstOrNull { it.album.contains("Winning", ignoreCase = true) } ?: allTracks.getOrElse(3) { allTracks.first() }
            ),
            EditorialAlbum(
                title = "MoonChild Era",
                subtitle = "Diljit Dosanjh • 2021",
                artist = "Diljit Dosanjh",
                year = "2021",
                browseId = "MPREb_hv8LEmvUqnE",
                artworkUrl = "https://c.saavncdn.com/209/MoonChild-Era-Punjabi-2021-20240715073449-500x500.jpg",
                sampleTrack = allTracks.firstOrNull { it.album.contains("MoonChild", ignoreCase = true) } ?: allTracks.getOrElse(4) { allTracks.first() }
            ),
            EditorialAlbum(
                title = "Aashiqui 2",
                subtitle = "Mithoon • 2013",
                artist = "Mithoon",
                year = "2013",
                artworkUrl = "https://c.saavncdn.com/430/Aashiqui-2-Hindi-2013-500x500.jpg",
                sampleTrack = allTracks.firstOrNull { it.album.contains("Aashiqui", ignoreCase = true) } ?: allTracks.first()
            )
        )
    }

    // 9. New releases tracks
    val newReleasesTracks = remember(allTracks) {
        allTracks.takeLast(8).ifEmpty { allTracks }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        // ── Scrolling Content (Passes smoothly underneath Crystal Header) ────
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = statusBarTop + 64.dp, // Below pinned header with status bar
                bottom = contentPadding.calculateBottomPadding()
            )
        ) {

            if (isHomeLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White.copy(alpha = 0.7f),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // ── Dynamic Greeting & Date Row ─────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = VybeDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp,
                            letterSpacing = 0.3.sp,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = liveDate,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF8E8E8E),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.4.sp
                        )
                    )
                }
            }

            // ── Hero Card: Infinite Radio ──────────────────────────────────
            item {
                InfiniteRadioHeroCard(
                    artworkUrl = dailyHeroPosterUrl,
                    onPlayClick = {
                        if (allTracks.isNotEmpty()) {
                            onPlayTracks(allTracks, true)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 20.dp)
                )
            }

            // ── Section 1: Quick picks ───────────────────────────────────────
            item {
                SectionHeader(
                    title = "Quick picks",
                    subtitle = "Because you listen to ${selectedArtist.ifEmpty { "Arijit Singh" }}",
                    showShuffleIcon = true,
                    showPlayAll = true,
                    onShuffleClick = { onPlayTracks(quickPicksTracks, true) },
                    onPlayAllClick = { onPlayTracks(quickPicksTracks, false) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(bottom = 26.dp)
                ) {
                    items(quickPicksTracks, key = { "qp_${it.id}" }) { track ->
                        val isCurrent = currentTrack?.id == track.id
                        EditorialCard(
                            artworkUrl = track.artworkUrl,
                            title = track.title,
                            subtitle = track.artist,
                            showPlayOverlay = true,
                            isPlaying = isPlaying && isCurrent,
                            onClick = { onTrackClick(track, quickPicksTracks) },
                            onPlayOverlayClick = { onTrackClick(track, quickPicksTracks) }
                        )
                    }
                }
            }

            // ── Section 2: Fresh finds ───────────────────────────────────────
            item {
                SectionHeader(
                    title = "Fresh finds",
                    subtitle = "New tracks tailored to your rotation",
                    showShuffleIcon = true,
                    showPlayAll = true,
                    onShuffleClick = { onPlayTracks(freshFindsTracks, true) },
                    onPlayAllClick = { onPlayTracks(freshFindsTracks, false) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(bottom = 26.dp)
                ) {
                    items(freshFindsTracks, key = { "ff_${it.id}" }) { track ->
                        val isCurrent = currentTrack?.id == track.id
                        EditorialCard(
                            artworkUrl = track.artworkUrl,
                            title = track.title,
                            subtitle = track.artist,
                            badgeText = "NEW",
                            showPlayOverlay = true,
                            isPlaying = isPlaying && isCurrent,
                            onClick = { onTrackClick(track, freshFindsTracks) },
                            onPlayOverlayClick = { onTrackClick(track, freshFindsTracks) }
                        )
                    }
                }
            }

            // ── Section 3: Jump back in ──────────────────────────────────────
            item {
                SectionHeader(
                    title = "Jump back in",
                    subtitle = "From your listening history",
                    showPlayAll = true,
                    onPlayAllClick = { onPlayTracks(jumpBackInTracks, false) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(bottom = 26.dp)
                ) {
                    items(jumpBackInTracks, key = { "jbi_${it.id}" }) { track ->
                        val isCurrent = currentTrack?.id == track.id
                        EditorialCard(
                            artworkUrl = track.artworkUrl,
                            title = track.title,
                            subtitle = track.artist,
                            showPlayOverlay = true,
                            isPlaying = isPlaying && isCurrent,
                            onClick = { onTrackClick(track, jumpBackInTracks) },
                            onPlayOverlayClick = { onTrackClick(track, jumpBackInTracks) }
                        )
                    }
                }
            }

            // ── Section 4: Mixes to explore ──────────────────────────────────
            item {
                SectionHeader(
                    title = "Mixes to explore",
                    subtitle = "Familiar favorites, fresh combinations...",
                    showShufflePill = true,
                    onShuffleClick = {
                        val allMixTracks = mixes.flatMap { it.tracks }
                        onPlayTracks(allMixTracks, true)
                    }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(bottom = 26.dp)
                ) {
                    items(mixes, key = { "mix_${it.title}" }) { mix ->
                        val firstTrack = mix.tracks.firstOrNull() ?: allTracks.first()
                        val isCurrent = currentTrack?.id == firstTrack.id
                        EditorialCard(
                            artworkUrl = mix.artworkUrl,
                            title = mix.title,
                            subtitle = mix.subtitle,
                            badgeText = "((o)) MIX",
                            showPlayOverlay = true,
                            isPlaying = isPlaying && isCurrent,
                            onClick = { onPlayTracks(mix.tracks, false) },
                            onPlayOverlayClick = { onPlayTracks(mix.tracks, false) }
                        )
                    }
                }
            }

            // ── Artist Spotlight Card (Auto-Rotating) ────────────────────────
            item {
                val activeSpotlight = spotlightCandidates.getOrNull(
                    if (spotlightCandidates.isNotEmpty()) currentSpotlightIndex % spotlightCandidates.size else 0
                )
                if (activeSpotlight != null) {
                    Crossfade(
                        targetState = activeSpotlight,
                        animationSpec = tween(durationMillis = 600),
                        label = "spotlight_crossfade"
                    ) { artistSummary ->
                        ArtistSpotlightCard(
                            summary = artistSummary,
                            onPlayArtistClick = { onOpenArtistDetail(artistSummary.name, true) },
                            onViewArtistClick = { onOpenArtistDetail(artistSummary.name, false) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 4.dp, bottom = 24.dp)
                        )
                    }
                }
            }

            // ── Section 5: Artists for you ───────────────────────────────────
            item {
                SectionHeader(
                    title = "Artists for you",
                    subtitle = "Verified artists • Tap card for profile, tap play for radio queue"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(bottom = 26.dp)
                ) {
                    items(homeArtistsForYou, key = { "art_${it.name}" }) { artist ->
                        val matchingTracks = allTracks.filter {
                            it.artist.contains(artist.name.split(" ").first(), ignoreCase = true)
                        }.ifEmpty { allTracks }

                        ArtistCircleCard(
                            artist = artist,
                            onArtistClick = {
                                onOpenArtistDetail(artist.name, false)
                            },
                            onPlayRadioClick = {
                                onOpenArtistDetail(artist.name, true)
                            }
                        )
                    }
                }
            }

            // ── Section 6: Trending today ────────────────────────────────────
            item {
                SectionHeader(
                    title = "Trending today",
                    subtitle = "Most played tracks worldwide right now",
                    showShuffleIcon = true,
                    showPlayAll = true,
                    onShuffleClick = { onPlayTracks(trendingTodayTracks, true) },
                    onPlayAllClick = { onPlayTracks(trendingTodayTracks, false) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(bottom = 26.dp)
                ) {
                    items(trendingTodayTracks, key = { "tt_${it.id}" }) { track ->
                        val isCurrent = currentTrack?.id == track.id
                        EditorialCard(
                            artworkUrl = track.artworkUrl,
                            title = track.title,
                            subtitle = track.artist,
                            showPlayOverlay = true,
                            isPlaying = isPlaying && isCurrent,
                            onClick = { onTrackClick(track, trendingTodayTracks) },
                            onPlayOverlayClick = { onTrackClick(track, trendingTodayTracks) }
                        )
                    }
                }
            }

            // ── Section 7: Favorites to revisit ──────────────────────────────
            if (favoriteTracks.isNotEmpty()) {
                item {
                    val favCountText = if (likedTracks.isNotEmpty()) "${likedTracks.size}" else "${favoriteTrackIds.size}"
                    SectionHeader(
                        title = "Favorites to revisit",
                        subtitle = "From your $favCountText liked tracks",
                        showPlayAll = true,
                        onPlayAllClick = { onPlayTracks(favoriteTracks, false) }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(bottom = 26.dp)
                    ) {
                        items(favoriteTracks, key = { "fav_${it.id}" }) { track ->
                            val isCurrent = currentTrack?.id == track.id
                            EditorialCard(
                                artworkUrl = track.artworkUrl,
                                title = track.title,
                                subtitle = track.artist,
                                showPlayOverlay = true,
                               isPlaying = isPlaying && isCurrent,
                                onClick = { onTrackClick(track, favoriteTracks) },
                                onPlayOverlayClick = { onTrackClick(track, favoriteTracks) }
                            )
                        }
                    }
                }
            }

            // ── Section 8: Albums for you ────────────────────────────────────
            item {
                SectionHeader(
                    title = "Albums for you",
                    subtitle = "Original Master Studio Artwork",
                    showMorePill = true,
                    onMoreClick = { onSearchClick() }
                )
                val albumSampleTracks = remember(albums) { albums.map { it.sampleTrack } }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(bottom = 26.dp)
                ) {
                    items(albums, key = { "alb_${it.title}" }) { album ->
                        EditorialCard(
                            artworkUrl = album.artworkUrl,
                            title = album.title,
                            subtitle = album.subtitle,
                            showPlayOverlay = false, // NO play overlay on artwork
                            onClick = {
                                onAlbumClick(album.browseId, album.title, album.artist, album.artworkUrl, album.year)
                            }
                        )
                    }
                }
            }

            // ── Section 9: New releases ──────────────────────────────────────
            item {
                SectionHeader(
                    title = "New releases",
                    subtitle = "Fresh drops and new albums",
                    showPlayAll = true,
                    onPlayAllClick = { onPlayTracks(newReleasesTracks, false) }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(bottom = 26.dp)
                ) {
                    items(newReleasesTracks, key = { "nr_${it.id}" }) { track ->
                        val isCurrent = currentTrack?.id == track.id
                        EditorialCard(
                            artworkUrl = track.artworkUrl,
                            title = track.title,
                            subtitle = track.artist,
                            badgeText = "NEW",
                            showPlayOverlay = true,
                            isPlaying = isPlaying && isCurrent,
                            onClick = { onTrackClick(track, newReleasesTracks) },
                            onPlayOverlayClick = { onTrackClick(track, newReleasesTracks) }
                        )
                    }
                }
            }

            // ── Bottom Footer ────────────────────────────────────────────────
            item {
                val formattedTime = remember {
                    try {
                        val cal = Calendar.getInstance()
                        val hour = cal.get(Calendar.HOUR)
                        val displayHour = if (hour == 0) 12 else hour
                        val minute = String.format("%02d", cal.get(Calendar.MINUTE))
                        val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
                        "$displayHour:$minute $amPm"
                    } catch (e: Exception) {
                        "2:00 PM"
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "♪  Updated $formattedTime • Made for you from your taste",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF757575),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.2.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(160.dp))
            }
        }

        // ── Single Source-of-Truth Crystal-Clear Zero-Blur Header ──
        VybeCrystalHeader(
            title = "Home",
            onSearchClick = onSearchClick,
            onProfileClick = onProfileClick,
            userPhotoUrl = userPhotoUrl,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

// ── 'INFINITE RADIO' Hero Editorial Card ───────────────────────────────────────

@Composable
private fun InfiniteRadioHeroCard(
    artworkUrl: String,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val heroCardShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
    val desaturatedMatrix = remember {
        ColorMatrix().apply { setToSaturation(0.25f) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(245.dp)
            .clip(heroCardShape)
            .background(Color.Black)
    ) {
        // 1. Atmospheric desaturated low-intensity backdrop
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artworkUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Infinite Radio Poster",
            contentScale = ContentScale.Crop,
            alpha = 0.28f,
            colorFilter = ColorFilter.colorMatrix(desaturatedMatrix),
            modifier = Modifier.fillMaxSize()
        )

        // 2. Vertical melt scrim: fades 100% seamlessly into pure #000000 AMOLED black
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.25f),
                        0.45f to Color.Black.copy(alpha = 0.55f),
                        0.78f to Color.Black.copy(alpha = 0.92f),
                        1.0f to Color.Black
                    )
                )
        )

        // 3. Soft left-to-right readability vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
                    )
                )
        )

        // 4. Sleek Editorial Hierarchy
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Top Badge: Sleek borderless smoky translucent pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.11f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFBDBDBD),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "MADE FOR YOU",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    color = Color(0xFFC8C8C8)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title (12.dp below badge): Wide geometric display styling
            Text(
                text = "Infinite Radio",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = VybeDisplayFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.2.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle (8.dp below title)
            Text(
                text = "An endless station shaped by your listening",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFA8A8A8),
                letterSpacing = 0.1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Compact Muted Silver Play Pill
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFBDBDBD))
                    .bouncyClickable(pressedScale = 0.92f, onClick = onPlayClick)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color(0xFF0F0F0F),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Play",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F0F0F)
                    )
                }
            }
        }
    }
}

// ── Reusable Component 1: SectionHeader ──────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    onShuffleClick: (() -> Unit)? = null,
    onPlayAllClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,
    showShuffleIcon: Boolean = false,
    showPlayAll: Boolean = false,
    showShufflePill: Boolean = false,
    showMorePill: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.4).sp,
                    color = PureWhite
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF9E9E9E),
                    fontSize = 13.sp,
                    letterSpacing = 0.1.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right side controls (spaced 8.dp apart, borderless dark #1A1A1C buttons)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Circular Shuffle button (40.dp, #1A1A1C)
            if (showShuffleIcon) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1C))
                        .bouncyClickable(pressedScale = 0.88f) { onShuffleClick?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Pill Play all button
            if (showPlayAll) {
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF1A1A1C))
                        .bouncyClickable(pressedScale = 0.90f) { onPlayAllClick?.invoke() }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play all",
                            tint = PureWhite,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Play all",
                            color = PureWhite,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Pill Shuffle button
            if (showShufflePill) {
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF1A1A1C))
                        .bouncyClickable(pressedScale = 0.90f) { onShuffleClick?.invoke() }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = PureWhite,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Shuffle",
                            color = PureWhite,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Pill More button
            if (showMorePill) {
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF1A1A1C))
                        .bouncyClickable(pressedScale = 0.90f) { onMoreClick?.invoke() }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "More",
                            color = PureWhite,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "More",
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Reusable Component 2: EditorialCard ──────────────────────────────────────

@Composable
private fun EditorialCard(
    artworkUrl: String,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    showPlayOverlay: Boolean = true,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onPlayOverlayClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(164.dp)
            .bouncyClickable(pressedScale = 0.96f, onClick = onClick)
    ) {
        // 164.dp rounded square artwork
        Box(
            modifier = Modifier
                .size(164.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF18181C))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artworkUrl)
                    .crossfade(200)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Optional top-left translucent dark pill badge ("NEW" or "((o)) MIX")
            if (!badgeText.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = PureWhite,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                }
            }

            // Optional bottom-right circular dark translucent button (40.dp, #000000 at 60% alpha)
            if (showPlayOverlay) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.BottomEnd)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.60f))
                        .bouncyClickable(pressedScale = 0.85f) {
                            (onPlayOverlayClick ?: onClick).invoke()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        MiniEqualizerBars(
                            modifier = Modifier
                                .size(width = 16.dp, height = 14.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Below artwork: bold white title (15.sp, 1 line ellipsis) + muted grey artist/subtitle (13.sp, 1 line ellipsis)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = PureWhite
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                color = Color(0xFF9E9E9E)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Reusable Component 3: ArtistCircleCard ───────────────────────────────────

@Composable
private fun ArtistCircleCard(
    artist: SpotlightArtist,
    onArtistClick: () -> Unit,
    onPlayRadioClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(136.dp)
            .bouncyClickable(pressedScale = 0.96f, onClick = onArtistClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(136.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artist.imageUrl)
                    .crossfade(200)
                    .build(),
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )

            // Small dark translucent flame badge at bottom-left
            Box(
                modifier = Modifier
                    .padding(start = 4.dp, bottom = 4.dp)
                    .align(Alignment.BottomStart)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔥",
                    fontSize = 15.sp
                )
            }

            // Solid white circular Play button (38.dp, black play triangle) at bottom-right
            Box(
                modifier = Modifier
                    .padding(end = 4.dp, bottom = 4.dp)
                    .align(Alignment.BottomEnd)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PureWhite)
                    .bouncyClickable(pressedScale = 0.84f, onClick = onPlayRadioClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play Artist Radio",
                    tint = AmoledBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Centered bold white artist name below
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp,
                color = PureWhite
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
