package com.vybe.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimationRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import com.vybe.music.ui.components.VybeDropdownMenu
import com.vybe.music.ui.components.VybeDropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vybe.music.data.PlaybackStats
import com.vybe.music.model.Track
import com.vybe.music.ui.components.VybeTrackArtwork
import com.vybe.music.ui.components.VybeCrystalHeader
import com.vybe.music.ui.components.VybeGlassTopHeader
import com.vybe.music.ui.components.VybeTransparentGlassHeader
import com.vybe.music.ui.components.bouncyClickable
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class StatsFilter(val label: String) {
    RECENT("Recent"),
    MOST_PLAYED("Most Played"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    TOP_ARTISTS("Top Artists"),
    TOP_ALBUMS("Top Albums")
}

data class ArtistStat(
    val artistName: String,
    val artworkUrl: String,
    val scrobbleCount: Int,
    val tracks: List<Track>
)

data class AlbumStat(
    val albumName: String,
    val artistName: String,
    val artworkUrl: String,
    val scrobbleCount: Int,
    val tracks: List<Track>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    stats: PlaybackStats = PlaybackStats(),
    currentTrack: Track?,
    isPlaying: Boolean,
    userName: String = "Ayush Mahato",
    userPhotoUrl: String? = null,
    isRefreshingStats: Boolean = false,
    onRefreshStats: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onTrackClick: (Track, List<Track>) -> Unit = { _, _ -> },
    onOpenArtistDetail: (String) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var selectedFilter by remember { mutableStateOf(StatsFilter.RECENT) }
    var showFilterDropdown by remember { mutableStateOf(false) }
    var showBreakdownSheet by remember { mutableStateOf(false) }

    // Helper to get effective last played timestamp
    fun getEffectiveTimestamp(track: Track): Long {
        val normKey = PlaybackStats.normalizedKey(track)
        val legacyKey = "${track.title.trim().lowercase()}_${track.artist.trim().lowercase()}"
        return track.lastPlayedTimestamp.takeIf { it > 0L }
            ?: stats.trackLastPlayed[track.id]
            ?: stats.trackLastPlayed[normKey]
            ?: stats.trackLastPlayed[legacyKey]
            ?: 0L
    }

    // Filter tracks based on selected filter
    val displayTracks = remember(stats.recentTracks, stats.trackPlayCounts, stats.trackLastPlayed, selectedFilter) {
        when (selectedFilter) {
            StatsFilter.RECENT -> stats.recentTracks
            StatsFilter.MOST_PLAYED -> stats.recentTracks.sortedByDescending { stats.getPlayCount(it) }
            StatsFilter.LAST_7_DAYS -> {
                val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000L
                stats.recentTracks.filter { track ->
                    val ts = getEffectiveTimestamp(track)
                    ts == 0L || ts >= cutoff
                }.sortedByDescending { stats.getPlayCount(it) }
            }
            StatsFilter.LAST_30_DAYS -> {
                val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L
                stats.recentTracks.filter { track ->
                    val ts = getEffectiveTimestamp(track)
                    ts == 0L || ts >= cutoff
                }.sortedByDescending { stats.getPlayCount(it) }
            }
            StatsFilter.TOP_ARTISTS, StatsFilter.TOP_ALBUMS -> emptyList()
        }
    }

    // Active dataset for Top Summary Card (100% synchronized with visible list/filter)
    val activeTrackSet = remember(stats.recentTracks, stats.trackLastPlayed, selectedFilter) {
        when (selectedFilter) {
            StatsFilter.RECENT, StatsFilter.MOST_PLAYED, StatsFilter.TOP_ARTISTS, StatsFilter.TOP_ALBUMS -> stats.recentTracks
            StatsFilter.LAST_7_DAYS, StatsFilter.LAST_30_DAYS -> displayTracks
        }
    }

    val activeScrobbles = remember(activeTrackSet, stats.trackPlayCounts) {
        activeTrackSet.sumOf { stats.getPlayCount(it) }
    }
    val activeUniqueTracks = remember(activeTrackSet) {
        activeTrackSet.distinctBy { PlaybackStats.normalizedKey(it) }.size
    }
    val activeUniqueArtists = remember(activeTrackSet) {
        activeTrackSet.mapNotNull { PlaybackStats.cleanArtist(it.artist) }.distinct().size
    }
    val activeUniqueAlbums = remember(activeTrackSet) {
        activeTrackSet.mapNotNull { PlaybackStats.cleanAlbum(it.album, it.title) }.distinct().size
    }

    val activePlayTimeMs = remember(activeTrackSet, stats.totalPlayTimeMs, selectedFilter) {
        if (selectedFilter == StatsFilter.RECENT || selectedFilter == StatsFilter.MOST_PLAYED || selectedFilter == StatsFilter.TOP_ARTISTS || selectedFilter == StatsFilter.TOP_ALBUMS) {
            stats.totalPlayTimeMs
        } else {
            val totalPlays = stats.recentTracks.sumOf { stats.getPlayCount(it) }.coerceAtLeast(1)
            val filteredPlays = activeTrackSet.sumOf { stats.getPlayCount(it) }
            if (stats.totalPlayTimeMs > 0L) {
                (stats.totalPlayTimeMs * filteredPlays / totalPlays)
            } else {
                activeTrackSet.sumOf { it.durationMs * stats.getPlayCount(it) }
            }
        }
    }

    // Format total listening duration as 🎧 Xd Xh Xm
    val totalSecs = (activePlayTimeMs / 1000L).coerceAtLeast(0L)
    val totalMinutes = totalSecs / 60L
    val days = totalMinutes / (24L * 60L)
    val remainingHours = (totalMinutes % (24L * 60L)) / 60L
    val remainingMinutes = totalMinutes % 60L
    val listeningDurationText = when {
        days > 0L -> "${days}d ${remainingHours}h ${remainingMinutes}m"
        remainingHours > 0L -> "${remainingHours}h ${remainingMinutes}m"
        remainingMinutes > 0L -> "${remainingMinutes}m"
        else -> "${totalSecs}s"
    }

    // Animated counters for bento cards
    val animatedScrobbles by animateIntAsState(
        targetValue = activeScrobbles,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "scrobbles_anim"
    )
    val animatedTracks by animateIntAsState(
        targetValue = activeUniqueTracks,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "tracks_anim"
    )
    val animatedArtists by animateIntAsState(
        targetValue = activeUniqueArtists,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "artists_anim"
    )
    val animatedAlbums by animateIntAsState(
        targetValue = activeUniqueAlbums,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "albums_anim"
    )

    // Grouped recent tracks for Recent view with date headers
    val groupedRecentTracks = remember(stats.recentTracks, stats.trackLastPlayed) {
        stats.recentTracks.groupBy { track ->
            val ts = getEffectiveTimestamp(track)
            formatScrobbleDateHeader(ts)
        }
    }

    // Top Artists aggregated by scrobbles (filtering out blanks / placeholders)
    val topArtists = remember(stats.recentTracks, stats.trackPlayCounts) {
        stats.recentTracks
            .filter { PlaybackStats.cleanArtist(it.artist) != null }
            .groupBy { PlaybackStats.cleanArtist(it.artist) ?: it.artist.trim() }
            .map { (artistName, tracks) ->
                val totalScrobbles = tracks.sumOf { stats.getPlayCount(it) }
                val artwork = tracks.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl.orEmpty()
                ArtistStat(
                    artistName = artistName,
                    artworkUrl = artwork,
                    scrobbleCount = totalScrobbles,
                    tracks = tracks
                )
            }
            .sortedByDescending { it.scrobbleCount }
    }

    // Top Albums aggregated by scrobbles (filtering out blanks / placeholders)
    val topAlbums = remember(stats.recentTracks, stats.trackPlayCounts) {
        stats.recentTracks
            .filter { PlaybackStats.cleanAlbum(it.album, it.title) != null }
            .groupBy { track ->
                val albumClean = PlaybackStats.cleanAlbum(track.album, track.title) ?: track.title.trim()
                val artistClean = PlaybackStats.cleanArtist(track.artist) ?: track.artist.trim()
                "$albumClean|$artistClean"
            }
            .map { (_, tracks) ->
                val first = tracks.first()
                val albumName = PlaybackStats.cleanAlbum(first.album, first.title) ?: first.title.trim()
                val artistName = PlaybackStats.cleanArtist(first.artist) ?: first.artist.trim()
                val artwork = tracks.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl.orEmpty()
                val totalScrobbles = tracks.sumOf { stats.getPlayCount(it) }
                AlbumStat(
                    albumName = albumName,
                    artistName = artistName,
                    artworkUrl = artwork,
                    scrobbleCount = totalScrobbles,
                    tracks = tracks
                )
            }
            .sortedByDescending { it.scrobbleCount }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        // Pull-to-refresh setup
        val pullStateStats = rememberPullToRefreshState()
        val hapticStats = LocalHapticFeedback.current
        var hapticFiredStats by remember { mutableStateOf(false) }
        LaunchedEffect(pullStateStats.distanceFraction) {
            if (pullStateStats.distanceFraction >= 1f && !hapticFiredStats) {
                hapticStats.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                hapticFiredStats = true
            }
            if (pullStateStats.distanceFraction < 0.5f) hapticFiredStats = false
        }

        PullToRefreshBox(
            isRefreshing = isRefreshingStats,
            onRefresh = onRefreshStats,
            state = pullStateStats,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullStateStats,
                    isRefreshing = isRefreshingStats,
                    containerColor = Color(0xFF1E1E1E),
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = statusBarTop + 64.dp)
                )
            }
        ) {
        // ── Scrollable Body (Passes smoothly underneath Crystal Header) ──
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = statusBarTop + 82.dp,
                bottom = contentPadding.calculateBottomPadding() + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // ── 1. Top User & Listening Time Row (Arrangement.SpaceBetween across full width) ──
            item(key = "user_listening_time_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Username pill (#161616, CircleShape, zero border)
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF161616))
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = userName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        // Right: Listening time pill (#161616, CircleShape, zero border)
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF161616))
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.90f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = listeningDurationText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // ── 2. Unified Statistics Summary Card (Outer #141414 container, RoundedCornerShape(32.dp), padding = 14.dp) ──
                item(key = "hero_summary_card") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0xFF141414))
                            .padding(14.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Top Inner Scrobbles Card: #3A3A3A, RoundedCornerShape(24.dp), height = 136.dp
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(136.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color(0xFF3A3A3A))
                                    .padding(horizontal = 20.dp)
                            ) {
                                // Centered count (48.sp, bold) and label (15.sp, Color(0xFFD0D0D0))
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$animatedScrobbles",
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Scrobbles",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color(0xFFD0D0D0)
                                    )
                                }

                                // Right-aligned 52.dp circular off-white button (#DCDCDC) with dark #121212 right arrow
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .align(Alignment.CenterEnd)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDCDCDC))
                                        .bouncyClickable(pressedScale = 0.90f) {
                                            showBreakdownSheet = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "View Breakdown",
                                        tint = Color(0xFF121212),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Bottom Row (10.dp gap between tiles): Tracks, Artists, Albums
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatCounterCard(
                                    count = animatedTracks,
                                    label = "Tracks",
                                    modifier = Modifier.weight(1f)
                                )
                                StatCounterCard(
                                    count = animatedArtists,
                                    label = "Artists",
                                    modifier = Modifier.weight(1f)
                                )
                                StatCounterCard(
                                    count = animatedAlbums,
                                    label = "Albums",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // ── 3. List Header Row + Filter Dropdown ──
                item(key = "list_header_row") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .padding(top = 18.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "List",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Box {
                            // Filter dropdown pill (CircleShape, #18181C fill, clock icon + <Filter> ▾)
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF181818))
                                    .bouncyClickable(pressedScale = 0.92f) {
                                        showFilterDropdown = true
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${selectedFilter.label} ▾",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }

                            VybeDropdownMenu(
                                expanded = showFilterDropdown,
                                onDismissRequest = { showFilterDropdown = false }
                            ) {
                                StatsFilter.values().forEach { filter ->
                                    val isSelected = selectedFilter == filter
                                    VybeDropdownMenuItem(
                                        text = filter.label,
                                        isActive = isSelected,
                                        trailingIcon = {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedFilter = filter
                                            showFilterDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 4. Grouped Material 3 Expressive Track Cards ──
                when (selectedFilter) {
                    StatsFilter.RECENT -> {
                        if (stats.recentTracks.isEmpty()) {
                            item(key = "empty_recent") {
                                EmptyStatsState(message = "No listening history yet")
                            }
                        } else {
                            itemsIndexed(
                                items = stats.recentTracks,
                                key = { index, track -> "recent_${track.id}_$index" }
                            ) { index, track ->
                                val isCurrent = currentTrack?.id == track.id && isPlaying
                                val cardShape = getStatsTrackCardShape(index, stats.recentTracks.size)
                                StatTrackRow(
                                    track = track,
                                    isCurrent = isCurrent,
                                    shape = cardShape,
                                    onClick = { onTrackClick(track, stats.recentTracks) },
                                    onOpenArtistDetail = onOpenArtistDetail
                                )
                            }
                        }
                    }

                    StatsFilter.MOST_PLAYED, StatsFilter.LAST_7_DAYS, StatsFilter.LAST_30_DAYS -> {
                        if (displayTracks.isEmpty()) {
                            item(key = "empty_tracks_${selectedFilter.name}") {
                                EmptyStatsState(
                                    message = if (selectedFilter == StatsFilter.LAST_7_DAYS) {
                                        "No tracks played in the last 7 days"
                                    } else if (selectedFilter == StatsFilter.LAST_30_DAYS) {
                                        "No tracks played in the last 30 days"
                                    } else {
                                        "No played tracks recorded yet"
                                    }
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = displayTracks,
                                key = { index, track -> "${selectedFilter.name}_${track.id}_$index" }
                            ) { index, track ->
                                val isCurrent = currentTrack?.id == track.id && isPlaying
                                val cardShape = getStatsTrackCardShape(index, displayTracks.size)
                                val playCount = stats.getPlayCount(track)
                                StatTrackRow(
                                    track = track,
                                    isCurrent = isCurrent,
                                    shape = cardShape,
                                    playCount = playCount,
                                    onClick = { onTrackClick(track, displayTracks) },
                                    onOpenArtistDetail = onOpenArtistDetail
                                )
                            }
                        }
                    }

                    StatsFilter.TOP_ARTISTS -> {
                        if (topArtists.isEmpty()) {
                            item(key = "empty_artists") {
                                EmptyStatsState(message = "No artist listening history yet")
                            }
                        } else {
                            itemsIndexed(
                                items = topArtists,
                                key = { _, artistStat -> "artist_${artistStat.artistName}" }
                            ) { _, artistStat ->
                                StatArtistRow(
                                    artistStat = artistStat,
                                    onClick = { onOpenArtistDetail(artistStat.artistName) }
                                )
                            }
                        }
                    }

                    StatsFilter.TOP_ALBUMS -> {
                        if (topAlbums.isEmpty()) {
                            item(key = "empty_albums") {
                                EmptyStatsState(message = "No album listening history yet")
                            }
                        } else {
                            itemsIndexed(
                                items = topAlbums,
                                key = { _, albumStat -> "album_${albumStat.albumName}_${albumStat.artistName}" }
                            ) { _, albumStat ->
                                StatAlbumRow(
                                    albumStat = albumStat,
                                    onClick = {
                                        if (albumStat.tracks.isNotEmpty()) {
                                            onTrackClick(albumStat.tracks.first(), albumStat.tracks)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } // end PullToRefreshBox

            // ── Single Source-of-Truth Crystal-Clear Zero-Blur Header ──
            VybeCrystalHeader(
                title = "Statistics",
                onSearchClick = onSearchClick,
                onProfileClick = onProfileClick,
                userPhotoUrl = userPhotoUrl,
                modifier = Modifier.align(Alignment.TopCenter)
            )

        // ── 5. Detailed Breakdown / Genres Sheet Overlay ──
        AnimatedVisibility(
            visible = showBreakdownSheet,
            enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { it / 2 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(250)) { it / 2 }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { showBreakdownSheet = false }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.75f)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(Color(0xFF0E0E0E))
                        .clickable(enabled = false) {}
                        .padding(24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Sheet Header Row: Back button, Title, Overall Pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1A1A1A))
                                    .bouncyClickable(pressedScale = 0.88f) {
                                        showBreakdownSheet = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = "Detailed Breakdown",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF1A1A1A))
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "Overall",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Top Genres & Play Activity",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.85f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        val genres = listOf(
                            Pair("Pop / Dance", 0.42f),
                            Pair("R&B / Soul", 0.28f),
                            Pair("Hip-Hop / Rap", 0.18f),
                            Pair("Indie / Alternative", 0.12f)
                        )

                        genres.forEach { (genre, ratio) ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = genre,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${(ratio * 100).toInt()}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.70f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1A1A1A))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(ratio)
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(Color(0xFFE5E5E5))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format timestamp into clean date header (Today, Yesterday, d MMM yyyy).
 */
private fun formatScrobbleDateHeader(timestamp: Long): String {
    if (timestamp <= 0L) return "Today"
    val now = Calendar.getInstance()
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }

    val nowYear = now.get(Calendar.YEAR)
    val nowDay = now.get(Calendar.DAY_OF_YEAR)
    val dateYear = date.get(Calendar.YEAR)
    val dateDay = date.get(Calendar.DAY_OF_YEAR)

    return when {
        nowYear == dateYear && nowDay == dateDay -> "Today"
        nowYear == dateYear && nowDay - dateDay == 1 -> "Yesterday"
        else -> {
            val sdf = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)
            sdf.format(Date(timestamp))
        }
    }
}

/**
 * Standalone Stat Counter Card (Tracks, Artists, Albums).
 */
@Composable
private fun StatCounterCard(
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0A0A0A))
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$count",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFD0D0D0)
            )
        }
    }
}

fun getStatsTrackCardShape(index: Int, total: Int): Shape = when {
    total <= 1 -> RoundedCornerShape(24.dp)
    index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
    index == total - 1 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    else -> RoundedCornerShape(6.dp)
}

/**
 * Grouped Material 3 Expressive Track Card on #141414.
 */
@Composable
private fun StatTrackRow(
    track: Track,
    isCurrent: Boolean,
    shape: Shape,
    playCount: Int? = null,
    onClick: () -> Unit,
    onOpenArtistDetail: (String) -> Unit = {}
) {
    var showTrackMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(shape)
            .background(Color(0xFF141414))
            .bouncyClickable(pressedScale = 0.98f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 54.dp × 54.dp rounded artwork (RoundedCornerShape(12.dp))
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                VybeTrackArtwork(
                    track = track,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        MiniEqualizer(
                            color = Color.White,
                            modifier = Modifier.size(width = 16.dp, height = 16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Middle: Bold white title & muted artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                val subtitle = com.vybe.music.data.TrackMetadataIndex.formatSubtitle(track.artist, track.album)
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFA8A8A8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Play count pill next to 3-dots button (when filter is Most Played / Last 7 Days / Last 30 Days)
            if (playCount != null && playCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF222222))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (playCount == 1) "1 play" else "$playCount plays",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD0D0D0)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Clean 36.dp circular #262626 3-dots button on the right
            Box {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF262626))
                        .bouncyClickable(pressedScale = 0.88f) {
                            showTrackMenu = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                VybeDropdownMenu(
                    expanded = showTrackMenu,
                    onDismissRequest = { showTrackMenu = false }
                ) {
                    VybeDropdownMenuItem(
                        text = "Play now",
                        leadingIcon = Icons.Rounded.PlayArrow,
                        onClick = {
                            onClick()
                            showTrackMenu = false
                        }
                    )
                    if (track.artist.isNotBlank()) {
                        VybeDropdownMenuItem(
                            text = "View artist",
                            leadingIcon = Icons.Rounded.Person,
                            onClick = {
                                onOpenArtistDetail(track.artist)
                                showTrackMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top Artist Row with 48.dp Circular Avatar and Play Count.
 */
@Composable
private fun StatArtistRow(
    artistStat: ArtistStat,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(pressedScale = 0.98f, onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular artist avatar (48.dp, CircleShape)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF222222)),
            contentAlignment = Alignment.Center
        ) {
            if (artistStat.artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(artistStat.artworkUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = artistStat.artistName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.60f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Middle: Bold Artist Name & track count
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artistStat.artistName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${artistStat.tracks.size} ${if (artistStat.tracks.size == 1) "track" else "tracks"} played",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF9A9AA0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Play count badge
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xFF1E1E1E))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${artistStat.scrobbleCount}x",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE5E5E5)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Dark circular #1C1C1C arrow button
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF1C1C1C)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View Artist",
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Top Album Row with 48.dp Album Cover and Play Count.
 */
@Composable
private fun StatAlbumRow(
    albumStat: AlbumStat,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(pressedScale = 0.98f, onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album cover (48.dp, RoundedCornerShape(8.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF222222))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(albumStat.artworkUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = albumStat.albumName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Middle: Bold Album Name & muted Artist Name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = albumStat.albumName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = albumStat.artistName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF9A9AA0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Play count badge
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xFF1E1E1E))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${albumStat.scrobbleCount}x",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE5E5E5)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Dark circular #1C1C1C button
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF1C1C1C)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Play Album",
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Empty stats placeholder state.
 */
@Composable
private fun EmptyStatsState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF121212))
            .padding(vertical = 40.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(38.dp)
            )
            Text(
                text = message,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.50f)
            )
            Text(
                text = "Play songs to track your personal music stats",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.35f)
            )
        }
    }
}

/**
 * Live animated 4-bar white equalizer visualizer.
 */
@Composable
private fun MiniEqualizer(
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "eq_transition")
    val h1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = LinearEasing),
            repeatMode = AnimationRepeatMode.Reverse
        ),
        label = "h1"
    )
    val h2 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360, easing = LinearEasing),
            repeatMode = AnimationRepeatMode.Reverse
        ),
        label = "h2"
    )
    val h3 by transition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, easing = LinearEasing),
            repeatMode = AnimationRepeatMode.Reverse
        ),
        label = "h3"
    )
    val h4 by transition.animateFloat(
        initialValue = 0.90f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 390, easing = LinearEasing),
            repeatMode = AnimationRepeatMode.Reverse
        ),
        label = "h4"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(h1, h2, h3, h4).forEach { heightFraction ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(heightFraction)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
