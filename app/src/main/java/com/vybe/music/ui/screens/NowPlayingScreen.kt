package com.vybe.music.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.blur
import com.vybe.music.ui.components.PlayerOverflowMenuSheet
import com.vybe.music.ui.components.AddToPlaylistSheet
import com.vybe.music.ui.components.SleepTimerDialog
import com.vybe.music.ui.components.AudioSpecsDialog
import com.vybe.music.viewmodel.PlayerViewModel
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.ui.AspectRatioFrameLayout
import com.vybe.music.data.audio.VideoCandidate
import com.vybe.music.data.audio.VideoQualityStream
import com.vybe.music.data.audio.YouTubeVideoExtractor
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.res.painterResource
import com.vybe.music.R
import com.vybe.music.model.AudioQuality
import com.vybe.music.model.AudioTrackFormatInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.composed
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vybe.music.model.RepeatMode
import com.vybe.music.model.Track
import com.vybe.music.ui.components.bouncyClickable
import com.vybe.music.ui.components.liquidGlass
import com.vybe.music.ui.theme.HeartActive
import com.vybe.music.ui.theme.VybeGreen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Android 13+ (API 33+) AGSL Liquid Glass button styling:
 * Uses unified Modifier.liquidGlass() with subtle lens refraction,
 * crisp specular rim highlights, and high-contrast translucent dark tint.
 */
private fun Modifier.nowPlayingGlassButton(
    hazeState: HazeState? = null,
    shape: Shape = CircleShape,
    blurRadius: Dp = 18.dp,
    borderWidth: Dp = 0.75.dp,
    blackAlpha: Float = 0.15f,
    whiteAlpha: Float = 0.03f
): Modifier = this.liquidGlass(
    hazeState = hazeState,
    shape = shape,
    blurRadius = blurRadius,
    borderWidth = borderWidth,
    blackAlpha = blackAlpha,
    whiteAlpha = whiteAlpha
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    track: Track?,
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    playbackPositionState: StateFlow<Long>? = null,
    durationState: StateFlow<Long>? = null,
    playbackPositionMs: Long = 0L,
    durationMs: Long = 0L,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    isFavorite: Boolean,
    dynamicColor: Color,
    darkVibrantColor: Color = Color(0xFF121216),
    vibrantColor: Color = dynamicColor,
    mutedColor: Color = darkVibrantColor,
    isVideoMode: Boolean = false,
    isVideoLoading: Boolean = false,
    isVideoSynced: Boolean = true,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    isYouTubeResolving: Boolean = false,
    onDownloadClick: () -> Unit = {},
    onYouTubeClick: () -> Unit = {},
    player: Player? = null,
    videoPlayer: Player? = null,
    selectedVideoQuality: String = "HD Auto",
    availableVideoQualities: List<VideoQualityStream> = emptyList(),
    alternateVideos: List<VideoCandidate> = emptyList(),
    isSearchingAlternates: Boolean = false,
    onQualitySelect: (String) -> Unit = {},
    onSelectAlternateVideo: (String) -> Unit = {},
    onSearchAlternateVideos: (String) -> Unit = {},
    onCloseVideo: () -> Unit = {},
    onSeekRelative: (Long) -> Unit = {},
    onCollapseClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onVideoModeToggle: (Boolean) -> Unit = {},
    onLyricsClick: () -> Unit = {},
    audioFormatInfo: AudioTrackFormatInfo = AudioTrackFormatInfo(),
    streamingQuality: AudioQuality = AudioQuality.MAX,
    onQualitySelected: (AudioQuality) -> Unit = {},
    viewModel: PlayerViewModel? = null,
    modifier: Modifier = Modifier
) {
    if (track == null) return

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val npHazeState = remember { HazeState() }
    var showQualitySheet by remember { mutableStateOf(false) }
    val qualitySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showOverflowSheet by remember { mutableStateOf(false) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showAudioSpecsDialog by remember { mutableStateOf(false) }

    val playerBlur by animateDpAsState(
        targetValue = if (showOverflowSheet) 20.dp else 0.dp,
        animationSpec = tween(durationMillis = 250),
        label = "overflowBlur"
    )

    val activeVideoPlayer = videoPlayer ?: player
    var showInlineControls by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showFullscreenControls by remember { mutableStateOf(true) }
    var showVideoQualitySheet by remember { mutableStateOf(false) }
    val videoQualitySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showChooseVideoSheet by remember { mutableStateOf(false) }
    val chooseVideoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customVideoUrlInput by remember { mutableStateOf("") }

    val activity = context as? Activity
    DisposableEffect(isFullscreen) {
        if (isFullscreen) {
            try {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                activity?.window?.let { window ->
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                activity?.window?.let { window ->
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        onDispose {
            try {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                activity?.window?.let { window ->
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    LaunchedEffect(showInlineControls, isPlaying) {
        if (showInlineControls && isPlaying) {
            kotlinx.coroutines.delay(3500)
            showInlineControls = false
        }
    }

    LaunchedEffect(showFullscreenControls, isPlaying) {
        if (showFullscreenControls && isPlaying) {
            kotlinx.coroutines.delay(3500)
            showFullscreenControls = false
        }
    }

    val currentPosMs = playbackPositionState?.collectAsState()?.value ?: playbackPositionMs
    val totalDurMs = durationState?.collectAsState()?.value ?: durationMs

    // ── Animated colour palette (700ms crossfade on track change) ──────────
    val animatedDominant by animateColorAsState(
        targetValue = dynamicColor,
        animationSpec = tween(durationMillis = 700),
        label = "np_dominant"
    )
    val animatedDarkVibrant by animateColorAsState(
        targetValue = darkVibrantColor,
        animationSpec = tween(durationMillis = 700),
        label = "np_dark_vibrant"
    )
    val animatedVibrant by animateColorAsState(
        targetValue = vibrantColor,
        animationSpec = tween(durationMillis = 700),
        label = "np_vibrant"
    )
    val animatedMuted by animateColorAsState(
        targetValue = mutedColor,
        animationSpec = tween(durationMillis = 700),
        label = "np_muted"
    )

    // ── Slow ambient drift (14 seconds) for blurred backdrop ───────────────
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_drift")
    val driftPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "drift_phase"
    )
    // Subtle translation drift for blurred backdrop layer (±18dp over 14s)
    val backdropOffsetX by infiniteTransition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "backdrop_tx"
    )
    val backdropOffsetY by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "backdrop_ty"
    )
    val backdropScale by infiniteTransition.animateFloat(
        initialValue = 1.60f,
        targetValue = 1.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "backdrop_scale"
    )

    // ── Seek state ──────────────────────────────────────────────────────────
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragSliderFraction by remember { mutableFloatStateOf(0f) }
    var pendingSeekMs by remember { mutableStateOf<Long?>(null) }
    var pendingSeekTime by remember { mutableStateOf(0L) }

    LaunchedEffect(currentPosMs) {
        val pending = pendingSeekMs
        if (pending != null) {
            val diff = kotlin.math.abs(currentPosMs - pending)
            val elapsed = System.currentTimeMillis() - pendingSeekTime
            if (diff <= 1500L || elapsed > 1500L) {
                pendingSeekMs = null
            }
        }
    }

    val currentDisplayMs = when {
        isDraggingSlider -> (dragSliderFraction * totalDurMs).toLong()
        pendingSeekMs != null -> pendingSeekMs!!
        else -> currentPosMs
    }
    val currentFraction = if (totalDurMs > 0) (currentDisplayMs.toFloat() / totalDurMs.toFloat()).coerceIn(0f, 1f) else 0f
    val remainingMs = (totalDurMs - currentDisplayMs).coerceAtLeast(0L)

    // ── High-res artwork URL ────────────────────────────────────────────────
    val highResArtworkUrl = remember(track.artworkUrl) {
        com.vybe.music.utils.TrackUtils.upgradeArtworkUrl(track.artworkUrl)
    }

    // ── Root container: opaque #060608 base layer ─────────────────────────────
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060608))
            .then(if (playerBlur > 0.dp) Modifier.blur(playerBlur) else Modifier)
    ) {
        // ── Backdrop container with Haze capture (Layers 1-4) ─────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = npHazeState)
        ) {
            // ── Layer 1: AMOLED black base ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF000000))
            )

            // ── Layer 2: Live blurred artwork backdrop (slowly drifting/breathing) ─
            // 100% full-screen coverage with boosted saturation & brightness for vibrant ambient illumination
            val ambientColorFilter = remember {
                val cm = ColorMatrix().apply {
                    setToSaturation(1.42f)
                }
                val values = cm.values
                values[4] = 16f
                values[9] = 16f
                values[14] = 16f
                ColorFilter.colorMatrix(ColorMatrix(values))
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = backdropScale
                        scaleY = backdropScale
                        translationX = backdropOffsetX.dp.toPx()
                        translationY = backdropOffsetY.dp.toPx()
                        // Android S+ (API 31): hardware-accelerated RenderEffect blur (blurRadius = 60.dp)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            renderEffect = android.graphics.RenderEffect
                                .createBlurEffect(60f, 60f, android.graphics.Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        }
                        alpha = 0.98f
                    }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(highResArtworkUrl)
                        .crossfade(false)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = ambientColorFilter,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // ── Layer 3: Subtle dark shadow overlay ────────────────────────────────
            // Placed directly above the blurred artwork background and behind title/controls
            // so bright album covers have slightly lower brightness without losing color glow.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.46f to Color.Transparent,
                            0.62f to Color.Black.copy(alpha = 0.22f),
                            0.80f to Color.Black.copy(alpha = 0.32f),
                            1.0f to Color.Black.copy(alpha = 0.42f)
                        )
                    )
            )

            // ── Layer 4: Sharp hero poster / Native video stream (proportional 62% height, edge-to-edge from y = 0) ──
            // Soft BlendMode.DstIn alpha fade (0.00f..0.42f = 1.0f, 0.70f = 0.45f, 0.88f = 0.10f, 1.00f = 0.0f)
            // so the poster/video melts 100% invisibly into the background with zero gap above song title on tall screens.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.62f)
                    .align(Alignment.TopCenter)
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        if (!isVideoMode) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.00f to Color.Black,
                                    0.42f to Color.Black,
                                    0.70f to Color.Black.copy(alpha = 0.45f),
                                    0.88f to Color.Black.copy(alpha = 0.10f),
                                    1.00f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                    }
            ) {
                val artworkAlpha by animateFloatAsState(
                    targetValue = if (isVideoMode) 0f else 1f,
                    animationSpec = tween(durationMillis = 280),
                    label = "artworkAlpha"
                )
                if (artworkAlpha > 0.01f) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(highResArtworkUrl)
                            .size(coil.size.Size.ORIGINAL)
                            .precision(coil.size.Precision.EXACT)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${track.title} artwork",
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize(),
                        alpha = artworkAlpha
                    )
                }
            }
        }

        // ── 16:9 Rounded Inline Video Box Centered In Artwork Slot ──
        if (isVideoMode && activeVideoPlayer != null && !isFullscreen) {
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.60f)
                    .padding(top = statusBarTop + 48.dp)
                    .align(Alignment.TopCenter),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(20.dp))
                        .clipToBounds()
                        .background(Color(0xFF0F0F12))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            (LayoutInflater.from(ctx).inflate(R.layout.view_video_player, null, false) as PlayerView).apply {
                                this.player = activeVideoPlayer
                                useController = false
                                controllerAutoShow = false
                                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            }
                        },
                        update = { view ->
                            if (view.player != activeVideoPlayer) {
                                view.player = activeVideoPlayer
                            }
                        },
                        onRelease = { view ->
                            view.player = null
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Click overlay to toggle inline video controls
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        showInlineControls = !showInlineControls
                                    }
                                )
                            }
                    )

                    // Subtle CircularProgressIndicator inside 16:9 box while resolving/loading
                    if (isVideoLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(34.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        }
                    }

                // Inline Video Overlay Controls (Auto-Hiding)
                AnimatedVisibility(
                    visible = showInlineControls,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(180)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Black.copy(alpha = 0.70f),
                                    0.45f to Color.Transparent,
                                    1.0f to Color.Black.copy(alpha = 0.65f)
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        // Top-left: • SYNCED
                        Row(
                            modifier = Modifier.align(Alignment.TopStart),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                    .border(0.75.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(Color(0xFF4CAF50), CircleShape)
                                )
                                Text(
                                    text = "SYNCED",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }

                        // Top-right: HD + Fullscreen (⛶) + Close (✕)
                        Row(
                            modifier = Modifier.align(Alignment.TopEnd),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // HD Quality Pill
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                    .border(0.75.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                    .bouncyClickable(pressedScale = 0.92f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showVideoQualitySheet = true
                                    }
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (selectedVideoQuality.contains("Auto")) "HD" else selectedVideoQuality,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Fullscreen (⛶) Button
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                    .border(0.75.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                    .bouncyClickable(pressedScale = 0.88f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isFullscreen = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Close (✕) Button
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                    .border(0.75.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                    .bouncyClickable(pressedScale = 0.88f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onCloseVideo()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close video",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        // ── Layer 5: Top navigation header ────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 6.dp, start = 20.dp, end = 20.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Collapse (Down chevron)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .nowPlayingGlassButton(hazeState = npHazeState, shape = CircleShape)
                    .bouncyClickable(pressedScale = 0.85f) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCollapseClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Centre header
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "NOW PLAYING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = Color.White.copy(alpha = 0.70f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (track.album.isNotBlank()) track.album else "Liked Songs",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.basicMarquee()
                )
            }

            // Options (3-dot)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .nowPlayingGlassButton(hazeState = npHazeState, shape = CircleShape)
                    .bouncyClickable(pressedScale = 0.85f) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showOverflowSheet = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // ── Layer 8: Controls column (bottom-aligned, tighter layout) ─────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Track info + Like/Lyrics ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 14.dp)
                ) {
                    Text(
                        text = track.title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = track.artist.uppercase(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like button: 50.dp circle with 22.dp icon
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .nowPlayingGlassButton(hazeState = npHazeState, shape = CircleShape)
                            .bouncyClickable(pressedScale = 0.82f) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onFavoriteToggle()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Unlike" else "Like",
                            tint = if (isFavorite) HeartActive else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Lyrics button: 50.dp circle with 22.dp icon
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .nowPlayingGlassButton(hazeState = npHazeState, shape = CircleShape)
                            .bouncyClickable(pressedScale = 0.82f) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLyricsClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = "Lyrics",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Seekbar ───────────────────────────────────────────────────────
            VybeThickSeekBar(
                fraction = currentFraction,
                isDragging = isDraggingSlider,
                onSeek = { frac ->
                    val targetMs = (frac * totalDurMs).toLong()
                    pendingSeekMs = targetMs
                    pendingSeekTime = System.currentTimeMillis()
                    onSeek(targetMs)
                },
                onDragging = { dragging, frac ->
                    isDraggingSlider = dragging
                    if (dragging) dragSliderFraction = frac
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // ── Timestamps ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = Track.formatDuration(currentDisplayMs),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = "-${Track.formatDuration(remainingMs)}",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── Audio quality label: clean, borderless centered text ───
            Box(
                modifier = Modifier
                    .bouncyClickable(pressedScale = 0.95f) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showQualitySheet = true
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = audioFormatInfo.label,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.50f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Main playback controls: 64.dp prev/next, 64.dp center play/pause pill (weight 1f) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous: 64.dp circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .nowPlayingGlassButton(hazeState = npHazeState, shape = CircleShape)
                        .bouncyClickable(pressedScale = 0.84f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPreviousClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play / Pause: 64.dp tall pill (weight 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .nowPlayingGlassButton(hazeState = npHazeState, shape = CircleShape)
                        .bouncyClickable(pressedScale = 0.92f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPlayPauseClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Loading",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPlaying) "Pause" else "Play",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                // Next: 64.dp circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .nowPlayingGlassButton(hazeState = npHazeState, shape = CircleShape)
                        .bouncyClickable(pressedScale = 0.84f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNextClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Bottom utility dock: sleek 46.dp tall pills filling width evenly with 12.dp spacing ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle: 46.dp tall pill (monochrome dark smoked glass)
                val isShuffleActive = isShuffleEnabled
                val shuffleIconScale = remember { Animatable(1f) }
                var isShuffleInitial by remember { mutableStateOf(true) }
                LaunchedEffect(isShuffleActive) {
                    if (isShuffleInitial) {
                        isShuffleInitial = false
                        return@LaunchedEffect
                    }
                    shuffleIconScale.snapTo(1f)
                    shuffleIconScale.animateTo(
                        targetValue = 1.15f,
                        animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing)
                    )
                    shuffleIconScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .nowPlayingGlassButton(hazeState = npHazeState, shape = CircleShape)
                        .bouncyClickable(pressedScale = 0.88f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onShuffleToggle()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    scaleX = shuffleIconScale.value
                                    scaleY = shuffleIconScale.value
                                }
                        )
                        AnimatedVisibility(
                            visible = isShuffleActive,
                            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }

                // Split Center Pill: 46.dp tall pill (Left: Download, Right: YouTube, separated by 1.dp divider)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .nowPlayingGlassButton(hazeState = npHazeState, shape = CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left half: Download icon / progress / check
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .bouncyClickable(pressedScale = 0.88f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDownloadClick()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(19.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else if (isDownloaded) {
                                Icon(
                                    imageVector = Icons.Rounded.DownloadDone,
                                    contentDescription = "Downloaded",
                                    tint = Color.White,
                                    modifier = Modifier.size(21.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = "Download",
                                    tint = Color.White,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }

                        // Subtle vertical divider: 1.dp width, Color.White.copy(alpha = 0.2f)
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(Color.White.copy(alpha = 0.2f))
                        )

                        // Right half: YouTube icon
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .bouncyClickable(pressedScale = 0.88f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onYouTubeClick()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isYouTubeResolving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(19.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_youtube),
                                    contentDescription = "YouTube",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Repeat: 46.dp tall pill (monochrome dark smoked glass)
                val isRepeatActive = repeatMode != RepeatMode.OFF
                val repeatIconScale = remember { Animatable(1f) }
                var isRepeatInitial by remember { mutableStateOf(true) }
                LaunchedEffect(repeatMode) {
                    if (isRepeatInitial) {
                        isRepeatInitial = false
                        return@LaunchedEffect
                    }
                    repeatIconScale.snapTo(1f)
                    repeatIconScale.animateTo(
                        targetValue = 1.15f,
                        animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing)
                    )
                    repeatIconScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .nowPlayingGlassButton(hazeState = npHazeState, shape = CircleShape)
                        .bouncyClickable(pressedScale = 0.88f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRepeatToggle()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    scaleX = repeatIconScale.value
                                    scaleY = repeatIconScale.value
                                }
                        )
                        AnimatedVisibility(
                            visible = isRepeatActive,
                            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Immersive Landscape Fullscreen Video Overlay ────────────────────
        if (isFullscreen && isVideoMode && activeVideoPlayer != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .zIndex(999f)
            ) {
                // Video TextureView (fit mode so nothing is cropped)
                AndroidView(
                    factory = { ctx ->
                        (LayoutInflater.from(ctx).inflate(R.layout.view_video_player, null, false) as PlayerView).apply {
                            this.player = activeVideoPlayer
                            useController = false
                            controllerAutoShow = false
                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    update = { view ->
                        if (view.player != activeVideoPlayer) {
                            view.player = activeVideoPlayer
                        }
                    },
                    onRelease = { view ->
                        view.player = null
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Tap detector for fullscreen controls
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    showFullscreenControls = !showFullscreenControls
                                }
                            )
                        }
                )

                // Fullscreen HUD overlay
                AnimatedVisibility(
                    visible = showFullscreenControls,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Black.copy(alpha = 0.80f),
                                    0.25f to Color.Transparent,
                                    0.75f to Color.Transparent,
                                    1.0f to Color.Black.copy(alpha = 0.85f)
                                )
                            )
                    ) {
                        // ── Top Bar ──
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Back (<) + Song Title & Artist
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                        .border(0.75.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                        .bouncyClickable(pressedScale = 0.88f) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isFullscreen = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Exit fullscreen",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = track.title,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = track.artist,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Right: • SYNCED, 🔍 Choose video, HD Auto, ✕ Close
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // SYNCED Pill
                                Row(
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                        .border(0.75.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF4CAF50), CircleShape)
                                    )
                                    Text(
                                        text = "SYNCED",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }

                                // 🔍 Choose video
                                Row(
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                        .border(0.75.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                        .bouncyClickable(pressedScale = 0.92f) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSearchAlternateVideos("")
                                            showChooseVideoSheet = true
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Choose video",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Choose video",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // HD Quality Pill
                                Box(
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                        .border(0.75.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                        .bouncyClickable(pressedScale = 0.92f) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            showVideoQualitySheet = true
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (selectedVideoQuality.contains("Auto")) "HD Auto" else selectedVideoQuality,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Close (✕) Button
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                        .border(0.75.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                        .bouncyClickable(pressedScale = 0.88f) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isFullscreen = false
                                            onCloseVideo()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Close video",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // ── Center Controls (-10s, Play/Pause, +10s) ──
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(36.dp)
                        ) {
                            // -10s Button
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                    .border(0.75.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                    .bouncyClickable(pressedScale = 0.88f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSeekRelative(-10000L)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Replay10,
                                    contentDescription = "Rewind 10 seconds",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Play/Pause Button
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .background(Color.White.copy(alpha = 0.25f), CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                    .bouncyClickable(pressedScale = 0.90f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onPlayPauseClick()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            // +10s Button
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                    .border(0.75.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                    .bouncyClickable(pressedScale = 0.88f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSeekRelative(10000L)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Forward10,
                                    contentDescription = "Forward 10 seconds",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // ── Bottom Bar (Synced Seekbar + Exit Fullscreen ⛶) ──
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 28.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Track.formatDuration(currentDisplayMs),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Sleek synced slider
                            androidx.compose.material3.Slider(
                                value = currentFraction,
                                onValueChange = { frac ->
                                    isDraggingSlider = true
                                    dragSliderFraction = frac
                                },
                                onValueChangeFinished = {
                                    val targetMs = (dragSliderFraction * totalDurMs).toLong()
                                    pendingSeekMs = targetMs
                                    pendingSeekTime = System.currentTimeMillis()
                                    isDraggingSlider = false
                                    onSeek(targetMs)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 14.dp),
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                )
                            )

                            Text(
                                text = Track.formatDuration(totalDurMs),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Exit Fullscreen (⛶) Button
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                    .border(0.75.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                    .bouncyClickable(pressedScale = 0.88f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isFullscreen = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FullscreenExit,
                                    contentDescription = "Exit fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQualitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showQualitySheet = false },
            sheetState = qualitySheetState,
            containerColor = Color(0xFF141417),
            scrimColor = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 12.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = "Streaming Audio Quality",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Active stream: ${audioFormatInfo.label}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF8E8E93)
                )

                Spacer(modifier = Modifier.height(18.dp))

                AudioQuality.values().forEach { quality ->
                    val isSelected = quality == streamingQuality
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) Color(0xFF222228) else Color(0xFF1A1A1E))
                            .border(
                                width = if (isSelected) 1.2.dp else 0.dp,
                                color = if (isSelected) Color.White.copy(alpha = 0.35f) else Color.Transparent,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .bouncyClickable(pressedScale = 0.97f) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onQualitySelected(quality)
                                showQualitySheet = false
                            }
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = quality.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = quality.subtitle,
                                    fontSize = 12.5.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color(0xFF8E8E93)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Video Quality Bottom Sheet ──────────────────────────────────────────
    if (showVideoQualitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showVideoQualitySheet = false },
            sheetState = videoQualitySheetState,
            containerColor = Color(0xFF141418),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Video Quality",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Ad-free direct stream quality",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 2.dp, bottom = 18.dp)
                )

                val qualityOptions = remember(availableVideoQualities) {
                    val list = mutableListOf("Auto (Recommended)")
                    val standardQualities = listOf("1080p", "720p", "480p", "360p", "240p")
                    standardQualities.forEach { q ->
                        if (availableVideoQualities.any { it.qualityLabel.contains(q, ignoreCase = true) }) {
                            list.add(q)
                        }
                    }
                    if (list.size == 1) {
                        list.addAll(listOf("1080p", "720p", "480p", "360p", "240p"))
                    }
                    list
                }

                qualityOptions.forEach { qOption ->
                    val isSelected = (qOption.startsWith("Auto") && selectedVideoQuality.contains("Auto")) ||
                            (!qOption.startsWith("Auto") && selectedVideoQuality.contains(qOption))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .bouncyClickable(pressedScale = 0.96f) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onQualitySelect(if (qOption.startsWith("Auto")) "HD Auto" else qOption)
                                showVideoQualitySheet = false
                            }
                            .background(if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = qOption,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (qOption == "1080p") {
                                Text(
                                    text = "Full HD",
                                    fontSize = 11.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (qOption == "720p") {
                                Text(
                                    text = "HD",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2196F3),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Choose Video Bottom Sheet ───────────────────────────────────────────
    if (showChooseVideoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChooseVideoSheet = false },
            sheetState = chooseVideoSheetState,
            containerColor = Color(0xFF141418),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Find the Right Video",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Alternate music videos & live performances",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                )

                // Paste custom URL or 11-char Video ID input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .border(0.75.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (customVideoUrlInput.isEmpty()) {
                            Text(
                                text = "Paste YouTube link or video ID...",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 13.sp
                            )
                        }
                        BasicTextField(
                            value = customVideoUrlInput,
                            onValueChange = { customVideoUrlInput = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                val extractedId = YouTubeVideoExtractor.extractVideoId(customVideoUrlInput)
                                if (extractedId != null) {
                                    onSelectAlternateVideo(extractedId)
                                    showChooseVideoSheet = false
                                } else {
                                    android.widget.Toast.makeText(context, "Invalid YouTube link or ID", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            })
                        )
                    }
                    if (customVideoUrlInput.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .bouncyClickable(pressedScale = 0.90f) {
                                    val extractedId = YouTubeVideoExtractor.extractVideoId(customVideoUrlInput)
                                    if (extractedId != null) {
                                        onSelectAlternateVideo(extractedId)
                                        showChooseVideoSheet = false
                                    } else {
                                        android.widget.Toast.makeText(context, "Invalid YouTube link or ID", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Load",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Matching YouTube Results",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                if (isSearchingAlternates) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    }
                } else if (alternateVideos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No alternate videos found",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(alternateVideos) { candidate ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .bouncyClickable(pressedScale = 0.96f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectAlternateVideo(candidate.videoId)
                                        showChooseVideoSheet = false
                                    }
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Video Thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(width = 96.dp, height = 54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.4f))
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(candidate.thumbnailUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = candidate.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (candidate.durationText.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(3.dp)
                                                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = candidate.durationText,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = candidate.title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = candidate.channel,
                                        color = Color.White.copy(alpha = 0.55f),
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Flagship 3-Dots Overflow Menu Sheet & Sub-Sheets ──
    if (viewModel != null) {
        val sleepTimerSeconds by viewModel.sleepTimerRemainingSeconds.collectAsState()

        PlayerOverflowMenuSheet(
            show = showOverflowSheet,
            onDismiss = { showOverflowSheet = false },
            track = track,
            viewModel = viewModel,
            audioFormatInfo = audioFormatInfo,
            streamingQuality = streamingQuality,
            onCollapsePlayer = onCollapseClick,
            onOpenVideo = { onYouTubeClick() },
            onOpenSleepTimer = { showSleepTimerDialog = true },
            onOpenAddToPlaylist = { showAddToPlaylistSheet = true },
            onOpenAudioSpecs = { showAudioSpecsDialog = true }
        )

        AddToPlaylistSheet(
            show = showAddToPlaylistSheet,
            onDismiss = { showAddToPlaylistSheet = false },
            track = track,
            viewModel = viewModel
        )

        if (showSleepTimerDialog) {
            SleepTimerDialog(
                remainingSeconds = sleepTimerSeconds,
                onSetTimer = { min -> viewModel.setSleepTimer(min) },
                onDismiss = { showSleepTimerDialog = false }
            )
        }

        AudioSpecsDialog(
            show = showAudioSpecsDialog,
            onDismiss = { showAudioSpecsDialog = false },
            track = track,
            audioFormatInfo = audioFormatInfo,
            streamingQuality = streamingQuality
        )
    }
}

/**
 * Thick expressive seekbar — 12dp track with rounded pill ends,
 * inactive track Color.White.copy(alpha = 0.16f), active track #FFFFFF,
 * 4.5dp wide × 34dp tall vertical pill thumb (CircleShape, #FFFFFF) with clean 4dp visual gap,
 * and a 4dp white dot inset near the right end of the inactive track.
 */
@Composable
fun VybeThickSeekBar(
    fraction: Float,
    isDragging: Boolean,
    thumbAccentColor: Color = Color.White,
    onSeek: (Float) -> Unit,
    onDragging: (Boolean, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var widthPx by remember { mutableFloatStateOf(1f) }
    var localDragFraction by remember { mutableFloatStateOf(fraction) }

    // Pill thumb: 4.5.dp wide x 34.dp tall, expands slightly on drag
    val pillWidth by animateFloatAsState(
        targetValue = if (isDragging) 6.5f else 4.5f,
        animationSpec = tween(durationMillis = 120),
        label = "pill_w"
    )
    val pillHeight by animateFloatAsState(
        targetValue = if (isDragging) 38f else 34f,
        animationSpec = tween(durationMillis = 120),
        label = "pill_h"
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.20f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "halo_alpha"
    )

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newFrac = (offset.x / widthPx).coerceIn(0f, 1f)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    localDragFraction = newFrac
                    onSeek(newFrac)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val newFrac = (offset.x / widthPx).coerceIn(0f, 1f)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        localDragFraction = newFrac
                        onDragging(true, newFrac)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val newFrac = (change.position.x / widthPx).coerceIn(0f, 1f)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        localDragFraction = newFrac
                        onDragging(true, newFrac)
                    },
                    onDragEnd = {
                        onDragging(false, localDragFraction)
                        onSeek(localDragFraction)
                    },
                    onDragCancel = {
                        onDragging(false, localDragFraction)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            widthPx = size.width
            val totalWidth = size.width
            val centerY = size.height / 2f
            val activeFraction = if (isDragging) localDragFraction else fraction
            val safeFraction = if (activeFraction.isNaN()) 0f else activeFraction.coerceIn(0f, 1f)

            // Track geometry: 12.dp height, rounded pill ends
            val trackH = 12.dp.toPx()
            val trackCorner = CornerRadius(trackH / 2f, trackH / 2f)

            // Pill thumb geometry: 4.5.dp wide x 34.dp tall
            val pillW = pillWidth.dp.toPx()
            val pillH = pillHeight.dp.toPx()
            val pillCorner = CornerRadius(pillW / 2f, pillW / 2f)

            // Gap on each side of thumb: clean 4.dp visual gap
            val gapPx = 4.dp.toPx()

            // Thumb centre X (clamped so pill never clips outside track)
            val thumbCenterX = (safeFraction * totalWidth).coerceIn(0f, totalWidth)

            // 1. Touch halo when dragging
            if (haloAlpha > 0.01f) {
                drawCircle(
                    color = Color.White.copy(alpha = haloAlpha),
                    radius = 26.dp.toPx(),
                    center = Offset(thumbCenterX, centerY)
                )
            }

            // 2. Inactive (remaining) track — right of thumb + gap: Color.White.copy(alpha = 0.16f)
            val inactiveStart = (thumbCenterX + pillW / 2f + gapPx).coerceIn(0f, totalWidth)
            if (inactiveStart < totalWidth) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.16f),
                    topLeft = Offset(inactiveStart, centerY - trackH / 2f),
                    size = Size(totalWidth - inactiveStart, trackH),
                    cornerRadius = trackCorner
                )

                // 4.dp white dot (Color.White.copy(alpha = 0.85f)) inset near right end of inactive track
                val dotCenterOffset = Offset(totalWidth - 10.dp.toPx(), centerY)
                if (inactiveStart < (totalWidth - 14.dp.toPx())) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = 2.dp.toPx(),
                        center = dotCenterOffset
                    )
                }
            }

            // 3. Active (played) track — left of thumb - gap: #FFFFFF
            val activeEnd = (thumbCenterX - pillW / 2f - gapPx).coerceIn(0f, totalWidth)
            if (activeEnd > 0f) {
                drawRoundRect(
                    color = Color(0xFFFFFFFF),
                    topLeft = Offset(0f, centerY - trackH / 2f),
                    size = Size(activeEnd, trackH),
                    cornerRadius = trackCorner
                )
            }

            // 4. Vertical pill thumb — crisp #FFFFFF CircleShape
            drawRoundRect(
                color = Color(0xFFFFFFFF),
                topLeft = Offset(thumbCenterX - pillW / 2f, centerY - pillH / 2f),
                size = Size(pillW, pillH),
                cornerRadius = pillCorner
            )
        }
    }
}
