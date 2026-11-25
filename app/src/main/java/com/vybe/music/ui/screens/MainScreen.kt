package com.vybe.music.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vybe.music.model.NavTab
import com.vybe.music.ui.components.BottomNavBar
import com.vybe.music.ui.components.DraggableSleepTimerBubble
import com.vybe.music.ui.components.SleepTimerDialog
import com.vybe.music.ui.components.MiniPlayer
import com.vybe.music.ui.components.MiniPlayerMode
import com.vybe.music.ui.components.VybeFloatingBanner
import com.vybe.music.ui.components.bouncyClickable
import com.vybe.music.ui.components.glassDockShadow
import com.vybe.music.ui.components.liquidGlass
import com.vybe.music.ui.theme.AmoledBlack
import com.vybe.music.ui.theme.PureWhite
import com.vybe.music.viewmodel.PlayerViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsState()
    val favoriteTrackIds by viewModel.favoriteTrackIds.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val trendingTracks by viewModel.trendingTracks.collectAsState()
    val top5DailyTracks by viewModel.top5DailyTracks.collectAsState()
    val quickPicks by viewModel.quickPicks.collectAsState()
    val freshFinds by viewModel.freshFinds.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val darkVibrantColor by viewModel.darkVibrantColor.collectAsState()
    val vibrantColor by viewModel.vibrantColor.collectAsState()
    val mutedColor by viewModel.mutedColor.collectAsState()
    val isVideoMode by viewModel.isVideoMode.collectAsState()
    val isVideoLoading by viewModel.isVideoLoading.collectAsState()
    val videoPlayer by viewModel.videoPlayerState.collectAsState()
    val isVideoSynced by viewModel.isVideoSynced.collectAsState()
    val currentVideoStream by viewModel.currentVideoStream.collectAsState()
    val selectedVideoQuality by viewModel.selectedVideoQuality.collectAsState()
    val alternateVideos by viewModel.alternateVideos.collectAsState()
    val isSearchingAlternates by viewModel.isSearchingAlternates.collectAsState()
    val mediaController by viewModel.mediaControllerState.collectAsState()
    val selectedArtist by viewModel.selectedArtist.collectAsState()
    val userSelectedArtists by viewModel.selectedArtists.collectAsState()
    val artistTracks by viewModel.artistSpotlightTracks.collectAsState()
    val isArtistLoading by viewModel.isArtistLoading.collectAsState()
    val isHomeLoading by viewModel.isHomeLoading.collectAsState()
    val currentArtistDetail by viewModel.currentArtistDetail.collectAsState()
    val isArtistDetailLoading by viewModel.isArtistDetailLoading.collectAsState()
    val currentAlbumDetail by viewModel.currentAlbumDetail.collectAsState()
    val isAlbumLoading by viewModel.isAlbumLoading.collectAsState()
    val playbackStats by viewModel.playbackStats.collectAsState()
    val customPlaylists by viewModel.customPlaylists.collectAsState()
    val likedTracks by viewModel.likedTracks.collectAsState()
    val selectedGenres by viewModel.selectedGenres.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    val userPhotoUrl by viewModel.userPhotoUrl.collectAsState()
    val currentAudioFormat by viewModel.currentAudioFormat.collectAsState()
    val streamingQuality by viewModel.streamingQuality.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val playbackPositionMs by viewModel.playbackPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val miniPlayerProgress = if (durationMs > 0L) (playbackPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val downloadedTrackIds by viewModel.downloadedTrackIds.collectAsState()
    val downloadingTrackIds by viewModel.downloadingTrackIds.collectAsState()
    val queuedTrackIds by viewModel.queuedTrackIds.collectAsState()
    val activePlaylistDownloads by viewModel.activePlaylistDownloads.collectAsState()
    val localTracks by viewModel.localTracks.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val isYouTubeResolving by viewModel.isYouTubeResolving.collectAsState()
    val isCurrentDownloaded = currentTrack?.let { downloadedTracks.any { dt -> dt.id == it.id } } ?: false
    val isCurrentDownloading = currentTrack?.let { downloadingTrackIds.contains(it.id) } ?: false

    val animatedDominantColor by animateColorAsState(
        targetValue = dynamicColor,
        animationSpec = tween(durationMillis = 600),
        label = "animated_dominant_color"
    )
    val animatedDarkVibrantColor by animateColorAsState(
        targetValue = darkVibrantColor,
        animationSpec = tween(durationMillis = 700),
        label = "animated_dark_vibrant_color"
    )
    val animatedVibrantColor by animateColorAsState(
        targetValue = vibrantColor,
        animationSpec = tween(durationMillis = 700),
        label = "animated_vibrant_color"
    )
    val animatedMutedColor by animateColorAsState(
        targetValue = mutedColor,
        animationSpec = tween(durationMillis = 700),
        label = "animated_muted_color"
    )

    val isCurrentFavorite = currentTrack?.let { track ->
        val normKey = com.vybe.music.utils.TrackUtils.normalizeTrackKey(track.title, track.artist)
        favoriteTrackIds.contains(track.id) ||
            likedTracks.any { it.id == track.id || (normKey.isNotBlank() && com.vybe.music.utils.TrackUtils.normalizeTrackKey(it.title, it.artist) == normKey) }
    } ?: false

    val coroutineScope = rememberCoroutineScope()
    val hazeState = remember { HazeState() }
    val density = LocalDensity.current

    // ── Instant Scroll-Up Pop-Up & Responsive Scroll-Down (~10.dp / 24px threshold) ──
    var isNavCollapsed by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab) {
        // Tab switch immediately forces expanded
        isNavCollapsed = false
    }

    val nestedScrollConnection = remember(density) {
        val downThresholdPx = with(density) { 10.dp.toPx() } // ~24px
        var accumulatedDownScroll = 0f

        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y

                if (dy > 4f) {
                    // Upward scroll: immediately pop back to expanded state on the very first gesture
                    if (isNavCollapsed) {
                        isNavCollapsed = false
                    }
                    accumulatedDownScroll = 0f
                } else if (dy < -2f) {
                    // Downward scroll: accumulate past small, snappy ~10.dp threshold
                    accumulatedDownScroll += -dy
                    if (accumulatedDownScroll >= downThresholdPx) {
                        if (!isNavCollapsed) {
                            isNavCollapsed = true
                        }
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 0f) {
                    if (isNavCollapsed) {
                        isNavCollapsed = false
                    }
                }
                return Offset.Zero
            }
        }
    }

    var isLibraryDetailOpen by remember { mutableStateOf(false) }
    var selectedArtistDetail by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedTab) {
        if (selectedTab != NavTab.PLAYLISTS) {
            isLibraryDetailOpen = false
        }
        selectedArtistDetail = null
    }

    // Dock state determination
    // Top-level root tabs showing Bottom Navigation Bar: HOME, STATS, and main PLAYLISTS overview
    val isDetailRoute = selectedArtistDetail != null ||
                        currentAlbumDetail != null ||
                        selectedTab == NavTab.SEARCH ||
                        selectedTab == NavTab.SETTINGS ||
                        (selectedTab == NavTab.PLAYLISTS && isLibraryDetailOpen)
    val dockState = when {
        isDetailRoute -> MiniPlayerMode.DOCKED
        isNavCollapsed -> MiniPlayerMode.COMPACT
        else -> MiniPlayerMode.EXPANDED
    }

    // Scroll Clearance (contentPadding.bottom)
    val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomContentPadding = when {
        isDetailRoute -> if (currentTrack != null) navBarBottomInset + 104.dp else navBarBottomInset + 24.dp
        currentTrack != null -> navBarBottomInset + 190.dp
        else -> navBarBottomInset + 104.dp
    }

    // ── Ultra-Smooth 60fps GPU-accelerated transition (0f = MiniPlayer, 1f = Full Expanded Player) ──
    val sheetProgress by animateFloatAsState(
        targetValue = if (isNowPlayingExpanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = 280,
            easing = FastOutSlowInEasing
        ),
        label = "np_sheet_progress"
    )
    val miniPlayerAlpha by animateFloatAsState(
        targetValue = if (isNowPlayingExpanded) 0f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "mini_player_fade"
    )

    val collapseNowPlayingSheet: () -> Unit = {
        viewModel.collapseNowPlaying()
    }

    val openArtistDetail: (String, Boolean) -> Unit = { artistName, playImmediately ->
        selectedArtistDetail = artistName
        viewModel.loadArtistDetail(artistName)
        if (playImmediately) {
            viewModel.playArtist(artistName)
        }
    }

    // BackHandler: collapse player if open, dismiss album detail, dismiss artist detail if open, else navigate to Home
    BackHandler(enabled = isNowPlayingExpanded || currentAlbumDetail != null || selectedArtistDetail != null || selectedTab != NavTab.HOME) {
        when {
            isNowPlayingExpanded -> collapseNowPlayingSheet()
            currentAlbumDetail != null -> viewModel.closeAlbum()
            selectedArtistDetail != null -> selectedArtistDetail = null
            else -> viewModel.selectTab(NavTab.HOME)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        val screenWidthDp = maxWidth
        val screenWidthPx = with(density) { screenWidthDp.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val isCompactPhone = screenWidthDp < 600.dp

        val p = sheetProgress
        val backgroundScale = 1f - (p * 0.05f)
        val backgroundCornerRadius = (p * 20f).dp

        // ── Layer 1: Background Screen Scaffold (Tab Content) ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = backgroundScale
                    scaleY = backgroundScale
                    shape = RoundedCornerShape(backgroundCornerRadius)
                    clip = p > 0.01f
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
                    .haze(state = hazeState)
            ) {
                if (currentAlbumDetail != null) {
                    AlbumDetailScreen(
                        albumDetail = currentAlbumDetail,
                        isLoading = isAlbumLoading,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        onBackClick = { viewModel.closeAlbum() },
                        onPlayAllClick = { tracks, shuffle -> viewModel.playTrackList(tracks, shuffle) },
                        onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                        onArtistClick = { nextArtist ->
                            viewModel.closeAlbum()
                            openArtistDetail(nextArtist, false)
                        },
                        contentPadding = PaddingValues(bottom = bottomContentPadding),
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (selectedArtistDetail != null) {
                    ArtistDetailScreen(
                        artistName = selectedArtistDetail ?: "",
                        artistDetail = currentArtistDetail,
                        isLoading = isArtistDetailLoading,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        onBackClick = { selectedArtistDetail = null },
                        onPlayAllClick = { tracks, shuffle -> viewModel.playTrackList(tracks, shuffle) },
                        onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                        onArtistClick = { nextArtist ->
                            selectedArtistDetail = nextArtist
                            viewModel.loadArtistDetail(nextArtist)
                        },
                        onAlbumClick = { browseId, title, artist, art, yr ->
                            viewModel.openAlbum(browseId, title, artist, art, yr)
                        },
                        contentPadding = PaddingValues(bottom = bottomContentPadding),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    when (selectedTab) {
                        NavTab.HOME -> {
                            val context = LocalContext.current
                            HomeScreen(
                                currentTrack = currentTrack,
                                isPlaying = isPlaying,
                                trendingTracks = trendingTracks,
                                top5DailyTracks = top5DailyTracks,
                                quickPicks = quickPicks,
                                freshFinds = freshFinds,
                                selectedArtist = selectedArtist,
                                userSelectedArtists = userSelectedArtists,
                                userGenres = selectedGenres.toList(),
                                favoriteTrackIds = favoriteTrackIds,
                                likedTracks = likedTracks,
                                artistTracks = artistTracks,
                                isArtistLoading = isArtistLoading,
                                isHomeLoading = isHomeLoading,
                                onArtistSelect = { viewModel.selectArtist(it) },
                                onSearchArtist = { viewModel.searchArtist(it) },
                                onSearchClick = { viewModel.selectTab(NavTab.SEARCH) },
                                onProfileClick = { viewModel.openSettings() },
                                onRefreshClick = { viewModel.refreshRecommendations() },
                                onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                                onPlayTracks = { tracks, shuffle -> viewModel.playTrackList(tracks, shuffle) },
                                onOpenArtistDetail = openArtistDetail,
                                onAlbumClick = { browseId, title, artist, art, yr ->
                                    viewModel.openAlbum(browseId, title, artist, art, yr)
                                },
                                onMoodSelect = { mood -> viewModel.filterHomeByMood(mood) },
                                onGenreClick = { genre ->
                                    Toast.makeText(context, "Starting $genre mix", Toast.LENGTH_SHORT).show()
                                    viewModel.startGenreMix(genre)
                                },
                                onLikedSongsQuickAccess = {
                                    if (likedTracks.isNotEmpty()) {
                                        viewModel.playTrackList(likedTracks, shuffle = true)
                                    } else {
                                        Toast.makeText(context, "Like a few songs first to play your Liked mix", Toast.LENGTH_SHORT).show()
                                        viewModel.showBanner("Like a few songs first to play your Liked mix")
                                    }
                                },
                                onDiscoverMixQuickAccess = {
                                    Toast.makeText(context, "Starting Discover mix", Toast.LENGTH_SHORT).show()
                                    viewModel.startDiscoverMix()
                                },
                                onYourMixQuickAccess = {
                                    Toast.makeText(context, "Starting Your Mix", Toast.LENGTH_SHORT).show()
                                    viewModel.startPersonalizedMix()
                                },
                                userPhotoUrl = userPhotoUrl,
                                hazeState = hazeState,
                                contentPadding = PaddingValues(bottom = bottomContentPadding)
                            )
                        }
                        NavTab.STATS -> {
                            StatsScreen(
                                stats = playbackStats,
                                currentTrack = currentTrack,
                                isPlaying = isPlaying,
                                userName = displayName,
                                userPhotoUrl = userPhotoUrl,
                                onSearchClick = { viewModel.selectTab(NavTab.SEARCH) },
                                onProfileClick = { viewModel.openSettings() },
                                onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                                onOpenArtistDetail = { name -> openArtistDetail(name, false) },
                                contentPadding = PaddingValues(bottom = bottomContentPadding)
                            )
                        }
                        NavTab.PLAYLISTS -> {
                            LibraryScreen(
                                favoriteCount = likedTracks.size,
                                favoriteTrackIds = favoriteTrackIds,
                                likedTracks = likedTracks,
                                customPlaylists = customPlaylists,
                                downloadedTracks = downloadedTracks,
                                localTracks = localTracks,
                                onScanLocalAudio = { viewModel.scanLocalAudio() },
                                isPlaying = isPlaying,
                                currentTrack = currentTrack,
                                onCreatePlaylist = { viewModel.createPlaylist(it) },
                                onDeleteDownloadedTrack = { viewModel.deleteDownloadedTrack(it) },
                                onDeletePlaylist = { viewModel.deletePlaylist(it) },
                                onUpdatePlaylistTracks = { id, tracks -> viewModel.updatePlaylistTracks(id, tracks) },
                                onUpdateLikedTracks = { viewModel.updateLikedTracks(it) },
                                onDownloadAllTracks = { viewModel.downloadAllTracks(it) },
                                downloadedTrackIds = downloadedTrackIds,
                                downloadingTrackIds = downloadingTrackIds,
                                queuedTrackIds = queuedTrackIds,
                                activePlaylistDownloads = activePlaylistDownloads,
                                onCancelPlaylistDownloads = { id, tracks -> viewModel.cancelPlaylistDownloads(id, tracks) },
                                onDeleteDownloadedPlaylist = { viewModel.deleteDownloadedPlaylist(it) },
                                onSearchClick = { viewModel.selectTab(NavTab.SEARCH) },
                                onProfileClick = { viewModel.openSettings() },
                                onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                                onPlayTracks = { tracks, shuffle -> viewModel.playTrackList(tracks, shuffle) },
                                onDetailStateChanged = { isLibraryDetailOpen = it },
                                userPhotoUrl = userPhotoUrl,
                                hazeState = hazeState,
                                contentPadding = PaddingValues(bottom = bottomContentPadding)
                            )
                        }
                        NavTab.SEARCH -> {
                            SearchScreen(
                                searchQuery = searchQuery,
                                searchResults = searchResults,
                                isSearching = isSearching,
                                currentTrack = currentTrack,
                                onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                                onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                                onOpenArtistDetail = openArtistDetail,
                                onAlbumClick = { browseId, title, artist, art, yr ->
                                    viewModel.openAlbum(browseId, title, artist, art, yr)
                                },
                                onBackClick = { viewModel.selectTab(NavTab.HOME) },
                                recentSearches = recentSearches,
                                onAddRecentSearch = { viewModel.addRecentSearch(it) },
                                onRemoveRecentSearch = { viewModel.removeRecentSearch(it) },
                                onClearRecentSearches = { viewModel.clearRecentSearches() },
                                contentPadding = PaddingValues(bottom = bottomContentPadding),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        NavTab.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBackClick = { viewModel.selectTab(NavTab.HOME) },
                                contentPadding = PaddingValues(bottom = bottomContentPadding),
                                displayName = displayName,
                                likedCount = favoriteTrackIds.size,
                                playlistsCount = customPlaylists.size,
                                historyCount = playbackStats.recentTracks.size,
                                streamingQuality = streamingQuality,
                                onQualitySelected = { viewModel.setStreamingQuality(it) },
                                onUpdateDisplayName = { viewModel.updateDisplayName(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // ── Layer 2: Unified Bottom Dock (Mini-Player + Navigation Bar) ──
            val hasTrack = currentTrack != null

            // ── Unified 60fps Morph Transition (Spring Physics) ──
            val dockTransition = updateTransition(targetState = dockState, label = "dock_morph")
            val springSpecDp = spring<Dp>(
                dampingRatio = 0.8f,
                stiffness = 380f
            )
            val springSpecFloat = spring<Float>(
                dampingRatio = 0.8f,
                stiffness = 380f
            )

            val dockWidth = minOf(screenWidthDp - 24.dp, 440.dp)
            val expandedNavWidth = minOf(screenWidthDp - 76.dp, 360.dp)

            val miniTranslationY by dockTransition.animateDp(
                transitionSpec = { springSpecDp },
                label = "mini_ty"
            ) { state ->
                when (state) {
                    MiniPlayerMode.EXPANDED -> (-84).dp // -(navHeight 74.dp + 10.dp gap)
                    MiniPlayerMode.COMPACT -> 0.dp
                    MiniPlayerMode.DOCKED -> 0.dp
                }
            }

            val miniWidth by dockTransition.animateDp(
                transitionSpec = { springSpecDp },
                label = "mini_width"
            ) { state ->
                when (state) {
                    MiniPlayerMode.EXPANDED -> dockWidth
                    MiniPlayerMode.COMPACT -> (dockWidth - 82.dp).coerceAtLeast(180.dp)
                    MiniPlayerMode.DOCKED -> dockWidth
                }
            }

            val miniHeight by dockTransition.animateDp(
                transitionSpec = { springSpecDp },
                label = "mini_height"
            ) { state ->
                when (state) {
                    MiniPlayerMode.COMPACT -> 66.dp
                    MiniPlayerMode.DOCKED -> 76.dp
                    MiniPlayerMode.EXPANDED -> 76.dp
                }
            }

            val miniOffsetX by dockTransition.animateDp(
                transitionSpec = { springSpecDp },
                label = "mini_offset_x"
            ) { state ->
                when (state) {
                    MiniPlayerMode.EXPANDED -> 0.dp
                    MiniPlayerMode.COMPACT -> 82.dp
                    MiniPlayerMode.DOCKED -> 0.dp
                }
            }

            val navWidth by dockTransition.animateDp(
                transitionSpec = { springSpecDp },
                label = "nav_width"
            ) { state ->
                when (state) {
                    MiniPlayerMode.EXPANDED -> expandedNavWidth
                    MiniPlayerMode.COMPACT -> 74.dp
                    MiniPlayerMode.DOCKED -> 0.dp
                }
            }

            val navOffsetStartX by dockTransition.animateDp(
                transitionSpec = { springSpecDp },
                label = "nav_offset_x"
            ) { state ->
                when (state) {
                    MiniPlayerMode.EXPANDED -> (dockWidth - expandedNavWidth).coerceAtLeast(0.dp) / 2
                    MiniPlayerMode.COMPACT -> 0.dp
                    MiniPlayerMode.DOCKED -> 0.dp
                }
            }

            val navHeight by dockTransition.animateDp(
                transitionSpec = { springSpecDp },
                label = "nav_height"
            ) { state ->
                when (state) {
                    MiniPlayerMode.EXPANDED -> 74.dp
                    MiniPlayerMode.COMPACT -> 74.dp
                    MiniPlayerMode.DOCKED -> 0.dp
                }
            }

            val navAlpha by dockTransition.animateFloat(
                transitionSpec = { springSpecFloat },
                label = "nav_alpha"
            ) { state ->
                when (state) {
                    MiniPlayerMode.DOCKED -> 0f
                    else -> 1f
                }
            }

            // Underlying Bottom Navigation Bar handoff during expansion
            val navHandoffAlpha = (1f - p * 2.5f).coerceIn(0f, 1f)
            val navHandoffTy = with(density) { (28.dp * (p * 2.5f).coerceIn(0f, 1f)).toPx() }

            // Outer Floating / Bottom-Docked Container
            val isDocked = dockState == MiniPlayerMode.DOCKED
            val dockContainerModifier = if (isDocked) {
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .width(dockWidth)
                    .padding(bottom = navBarBottomInset + 10.dp)
            }

            Box(
                modifier = dockContainerModifier
            ) {
                // ── Bottom Navigation Bar Layer: anchored on LEFT side (Alignment.BottomStart) in COMPACT ──
                if (!isDocked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset { IntOffset(x = with(density) { navOffsetStartX.toPx().roundToInt() }, y = 0) }
                            .width(navWidth)
                            .height(navHeight)
                            .graphicsLayer {
                                alpha = navAlpha * navHandoffAlpha
                                translationY = navHandoffTy
                            }
                            .zIndex(40f)
                    ) {
                        BottomNavBar(
                            selectedTab = selectedTab,
                            isCollapsed = dockState == MiniPlayerMode.COMPACT,
                            onTabSelected = {
                                isLibraryDetailOpen = false
                                viewModel.selectTab(it)
                            },
                            onExpandClick = { isNavCollapsed = false },
                            hazeState = hazeState,
                            accentColor = animatedVibrantColor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // ── Mini-Player Layer ──
                if (hasTrack) {
                    val miniBoxModifier = if (isDocked) {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .graphicsLayer {
                                alpha = miniPlayerAlpha
                            }
                            .zIndex(30f)
                    } else {
                        Modifier
                            .align(Alignment.BottomStart)
                            .offset {
                                IntOffset(
                                    x = with(density) { miniOffsetX.toPx().roundToInt() },
                                    y = with(density) { miniTranslationY.toPx().roundToInt() }
                                )
                            }
                            .width(miniWidth)
                            .height(miniHeight)
                            .graphicsLayer {
                                alpha = miniPlayerAlpha
                            }
                            .zIndex(30f)
                    }

                    Box(
                        modifier = miniBoxModifier
                    ) {
                        MiniPlayer(
                            track = currentTrack,
                            isPlaying = isPlaying,
                            progressFraction = miniPlayerProgress,
                            isBuffering = isBuffering,
                            mode = dockState,
                            hazeState = hazeState,
                            accentColor = animatedVibrantColor,
                            onExpandClick = { viewModel.expandNowPlaying() },
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onNextClick = { viewModel.nextTrack() },
                            onPreviousClick = { viewModel.previousTrack() },
                            onDismiss = {
                                viewModel.dismissPlayer()
                            },
                            modifier = if (isDocked) Modifier.fillMaxWidth() else Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // ── Layer 3: Dark Scrim Overlay when expanding (alpha = 0.65f * progress) ──
        if (p > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(45f)
                    .background(Color.Black.copy(alpha = 0.65f * p))
            )
        }

        // ── Layer 4: Ultra-Smooth GPU-Accelerated Expanding Sheet ──
        if (p > 0.001f && currentTrack != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(50f)
                    .graphicsLayer {
                        translationY = screenHeightPx * (1f - p)
                        alpha = (p * 1.5f).coerceIn(0f, 1f)
                    }
                    .background(Color(0xFF060608))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                if (dragAmount > 16f) {
                                    collapseNowPlayingSheet()
                                }
                            }
                        )
                    }
            ) {
                NowPlayingScreen(
                    track = currentTrack,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    playbackPositionState = viewModel.playbackPositionMs,
                    durationState = viewModel.durationMs,
                    isShuffleEnabled = isShuffleEnabled,
                    repeatMode = repeatMode,
                    isFavorite = isCurrentFavorite,
                    dynamicColor = animatedDominantColor,
                    darkVibrantColor = animatedDarkVibrantColor,
                    vibrantColor = animatedVibrantColor,
                    mutedColor = animatedMutedColor,
                    isVideoMode = isVideoMode,
                    isVideoLoading = isVideoLoading,
                    isVideoSynced = isVideoSynced,
                    isDownloaded = isCurrentDownloaded,
                    isDownloading = isCurrentDownloading,
                    isYouTubeResolving = isYouTubeResolving,
                    onDownloadClick = { currentTrack?.let { viewModel.toggleDownloadTrack(it) } },
                    onYouTubeClick = { viewModel.toggleVideoMode() },
                    player = mediaController,
                    videoPlayer = videoPlayer,
                    selectedVideoQuality = selectedVideoQuality,
                    availableVideoQualities = currentVideoStream?.availableQualities ?: emptyList(),
                    alternateVideos = alternateVideos,
                    isSearchingAlternates = isSearchingAlternates,
                    onQualitySelect = { viewModel.setVideoQuality(it) },
                    onSelectAlternateVideo = { viewModel.selectAlternateVideo(it) },
                    onSearchAlternateVideos = { viewModel.searchAlternateVideos(it) },
                    onCloseVideo = { viewModel.toggleVideoMode(false) },
                    onSeekRelative = { viewModel.seekRelative(it) },
                    audioFormatInfo = currentAudioFormat,
                    streamingQuality = streamingQuality,
                    onQualitySelected = { viewModel.setStreamingQuality(it) },
                    onCollapseClick = collapseNowPlayingSheet,
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onNextClick = { viewModel.nextTrack() },
                    onPreviousClick = { viewModel.previousTrack() },
                    onSeek = { viewModel.seekTo(it) },
                    onShuffleToggle = { viewModel.toggleShuffle() },
                    onRepeatToggle = { viewModel.toggleRepeat() },
                    onFavoriteToggle = {
                        val track = currentTrack ?: viewModel.currentTrack.value
                        track?.let { viewModel.toggleLike(it) }
                    },
                    onVideoModeToggle = { viewModel.toggleVideoMode(it) },
                    viewModel = viewModel
                )
            }

            // ── Layer 5: Top Floating Notification Banner ──
            val bannerData by viewModel.bannerMessage.collectAsState()
            VybeFloatingBanner(
                bannerData = bannerData,
                onDismiss = { viewModel.dismissBanner() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp)
                    .zIndex(100f)
            )

            // ── Layer 6: Draggable Floating Sleep Timer Bubble ──
            val sleepTimerSeconds by viewModel.sleepTimerRemainingSeconds.collectAsState()
            var showGlobalSleepTimerDialog by remember { mutableStateOf(false) }

            if (sleepTimerSeconds != null && sleepTimerSeconds!! > 0) {
                DraggableSleepTimerBubble(
                    remainingSeconds = sleepTimerSeconds!!,
                    onClick = { showGlobalSleepTimerDialog = true },
                    modifier = Modifier.zIndex(90f)
                )
            }

            if (showGlobalSleepTimerDialog) {
                SleepTimerDialog(
                    remainingSeconds = sleepTimerSeconds,
                    onSetTimer = { minutes -> viewModel.setSleepTimer(minutes) },
                    onDismiss = { showGlobalSleepTimerDialog = false }
                )
            }
        }
    }
}
