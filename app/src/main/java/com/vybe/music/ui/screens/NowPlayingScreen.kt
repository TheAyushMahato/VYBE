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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import com.vybe.music.ui.components.VybeSkeletonBox
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import com.vybe.music.data.audio.VideoCandidate
import com.vybe.music.data.audio.VideoQualityStream
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
import androidx.media3.common.Player
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
import com.vybe.music.ui.components.vybeSpringPress
import com.vybe.music.ui.components.vybeSpringClickable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
    val clipboardManager = LocalClipboardManager.current
    val npHazeState = remember { HazeState() }
    val coroutineScope = rememberCoroutineScope()

    // ── Tactile Micro-Animations State ─────────────────────────────────────
    val prevNudgeX = remember { Animatable(0f) }
    val nextNudgeX = remember { Animatable(0f) }
    val heartBounceScale = remember { Animatable(1f) }
    var prevFavState by remember { mutableStateOf(isFavorite) }
    LaunchedEffect(isFavorite) {
        if (isFavorite && !prevFavState) {
            heartBounceScale.snapTo(1f)
            heartBounceScale.animateTo(
                targetValue = 1.30f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            heartBounceScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        prevFavState = isFavorite
    }
    val animatedHeartColor by animateColorAsState(
        targetValue = if (isFavorite) HeartActive else Color.White,
        animationSpec = tween(durationMillis = 220),
        label = "heartColor"
    )

    var showOverflowSheet by remember { mutableStateOf(false) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    val playerBlur by animateDpAsState(
        targetValue = if (showOverflowSheet) 20.dp else 0.dp,
        animationSpec = tween(durationMillis = 250),
        label = "overflowBlur"
    )

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

            // ── Layer 4: Sharp hero poster (proportional 62% height, edge-to-edge from y = 0) ──
            // Soft BlendMode.DstIn alpha fade (0.00f..0.42f = 1.0f, 0.70f = 0.45f, 0.88f = 0.10f, 1.00f = 0.0f)
            // so the poster melts 100% invisibly into the background with zero gap above song title on tall screens.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.62f)
                    .align(Alignment.TopCenter)
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
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
            ) {
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
                    modifier = Modifier.fillMaxSize()
                )
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
                    .vybeSpringClickable(pressedScale = 0.88f) {
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
                    .vybeSpringClickable(pressedScale = 0.88f) {
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

                // Like button: 50.dp circle with 22.dp icon and celebratory bounce
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .nowPlayingGlassButton(hazeState = npHazeState, shape = CircleShape)
                        .vybeSpringClickable(pressedScale = 0.85f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onFavoriteToggle()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Unlike" else "Like",
                        tint = animatedHeartColor,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer {
                                scaleX = heartBounceScale.value
                                scaleY = heartBounceScale.value
                            }
                    )
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

            // ── Audio quality label: frozen, completely non-clickable status readout ───
            Box(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Main playback controls: 64.dp prev/next, 64.dp center play/pause pill (weight 1f) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous: 64.dp circle with -5.dp tactile nudge kick
                val prevInteractionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .nowPlayingGlassButton(hazeState = npHazeState, shape = CircleShape)
                        .vybeSpringClickable(pressedScale = 0.88f, interactionSource = prevInteractionSource) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            coroutineScope.launch {
                                prevNudgeX.snapTo(-5f)
                                prevNudgeX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                            onPreviousClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer {
                                translationX = prevNudgeX.value.dp.toPx()
                            }
                    )
                }

                // Play / Pause: 64.dp tall pill (weight 1f) with scale + fade crossfade and elevation response
                val playPauseInteractionSource = remember { MutableInteractionSource() }
                val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
                val pillAlpha by animateFloatAsState(
                    targetValue = if (isPlayPausePressed) 0.85f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                    label = "pill_alpha"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .graphicsLayer { alpha = pillAlpha }
                        .nowPlayingGlassButton(hazeState = npHazeState, shape = CircleShape)
                        .vybeSpringClickable(pressedScale = 0.93f, interactionSource = playPauseInteractionSource) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPlayPauseClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isBuffering) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
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
                        }
                    } else {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = {
                                (scaleIn(initialScale = 0.82f) + fadeIn(tween(180)))
                                    .togetherWith(scaleOut(targetScale = 0.82f) + fadeOut(tween(140)))
                            },
                            label = "play_pause_content"
                        ) { playing ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (playing) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (playing) "Pause" else "Play",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                // Next: 64.dp circle with +5.dp tactile nudge kick
                val nextInteractionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .nowPlayingGlassButton(hazeState = npHazeState, shape = CircleShape)
                        .vybeSpringClickable(pressedScale = 0.88f, interactionSource = nextInteractionSource) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            coroutineScope.launch {
                                nextNudgeX.snapTo(5f)
                                nextNudgeX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                            onNextClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier
                            .size(30.dp)
                            .graphicsLayer {
                                translationX = nextNudgeX.value.dp.toPx()
                            }
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
                // Shuffle: 46.dp tall pill
                val isShuffleActive = isShuffleEnabled
                val shuffleIconOffset by animateDpAsState(
                    targetValue = if (isShuffleActive) (-2).dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "shuffleIconOffset"
                )
                val shuffleDotScale by animateFloatAsState(
                    targetValue = if (isShuffleActive) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "shuffleDotScale"
                )
                val shuffleDotAlpha by animateFloatAsState(
                    targetValue = if (isShuffleActive) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "shuffleDotAlpha"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .nowPlayingGlassButton(hazeState = npHazeState, shape = CircleShape)
                        .vybeSpringClickable(pressedScale = 0.90f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onShuffleToggle()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Keep icons always Color.White with animated bottom dot when active
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .offset(y = shuffleIconOffset)
                    )
                    if (shuffleDotAlpha > 0.005f) {
                        Box(
                            modifier = Modifier
                                .offset(y = shuffleIconOffset + 14.5.dp)
                                .size(4.5.dp)
                                .graphicsLayer {
                                    scaleX = shuffleDotScale
                                    scaleY = shuffleDotScale
                                    alpha = shuffleDotAlpha
                                }
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }

                // Download pill button: 46.dp tall pill with checkmark pop-in animation
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .nowPlayingGlassButton(hazeState = npHazeState, shape = CircleShape)
                        .vybeSpringClickable(pressedScale = 0.90f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDownloadClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isDownloaded,
                        transitionSpec = {
                            (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn())
                                .togetherWith(scaleOut() + fadeOut())
                        },
                        label = "download_status"
                    ) { downloaded ->
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(19.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else if (downloaded) {
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
                }

                // Repeat: 46.dp tall pill
                val isRepeatActive = repeatMode != RepeatMode.OFF
                val repeatIconOffset by animateDpAsState(
                    targetValue = if (isRepeatActive) (-2).dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "repeatIconOffset"
                )
                val repeatDotScale by animateFloatAsState(
                    targetValue = if (isRepeatActive) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "repeatDotScale"
                )
                val repeatDotAlpha by animateFloatAsState(
                    targetValue = if (isRepeatActive) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "repeatDotAlpha"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .nowPlayingGlassButton(hazeState = npHazeState, shape = CircleShape)
                        .vybeSpringClickable(pressedScale = 0.90f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRepeatToggle()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Keep icons always Color.White with animated bottom dot when active
                    Icon(
                        imageVector = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .offset(y = repeatIconOffset)
                    )
                    if (repeatDotAlpha > 0.005f) {
                        Box(
                            modifier = Modifier
                                .offset(y = repeatIconOffset + 14.5.dp)
                                .size(4.5.dp)
                                .graphicsLayer {
                                    scaleX = repeatDotScale
                                    scaleY = repeatDotScale
                                    alpha = repeatDotAlpha
                                }
                                .clip(CircleShape)
                                .background(Color.White)
                        )
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
            onOpenSleepTimer = { showSleepTimerDialog = true },
            onOpenAddToPlaylist = { showAddToPlaylistSheet = true }
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
