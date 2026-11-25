package com.vybe.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vybe.music.model.Track
import com.vybe.music.ui.components.VybeTrackArtwork
import com.vybe.music.ui.theme.PureWhite
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.tanh

enum class MiniPlayerMode {
    EXPANDED, // Floating State A
    COMPACT,  // Scroll-Collapsed State B (56.dp inline)
    DOCKED    // Detail-Route Bottom-Docked State C
}

/**
 * 4-Bar EQ Visualizer (Expanded & Docked only):
 * 14.dp × 14.dp, 4 vertical capsule bars (width = 2.dp, gap = 2.dp, CircleShape, #FFFFFF at 90% alpha).
 * Playing loops with varied frequencies, paused static scale.
 */
@Composable
fun MiniEqualizerBars(
    isPlaying: Boolean = true,
    modifier: Modifier = Modifier
) {
    val barColor = Color.White.copy(alpha = 0.90f)

    if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "mini_eq_bars")

        val scale1 by transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(750, delayMillis = 0, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar1"
        )
        val scale2 by transition.animateFloat(
            initialValue = 0.85f,
            targetValue = 0.20f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = 150, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar2"
        )
        val scale3 by transition.animateFloat(
            initialValue = 0.30f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(850, delayMillis = 300, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar3"
        )
        val scale4 by transition.animateFloat(
            initialValue = 0.70f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(650, delayMillis = 100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar4"
        )

        Row(
            modifier = modifier.size(14.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            EqBar(scale1, barColor)
            EqBar(scale2, barColor)
            EqBar(scale3, barColor)
            EqBar(scale4, barColor)
        }
    } else {
        Row(
            modifier = modifier.size(14.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            EqBar(0.25f, barColor)
            EqBar(0.40f, barColor)
            EqBar(0.20f, barColor)
            EqBar(0.35f, barColor)
        }
    }
}

@Composable
private fun EqBar(scaleY: Float, color: Color) {
    Box(
        modifier = Modifier
            .width(2.dp)
            .fillMaxHeight()
            .graphicsLayer {
                this.scaleY = scaleY
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun MiniPlayer(
    track: Track?,
    isPlaying: Boolean,
    progressFraction: Float = 0f,
    progressFractionProvider: (() -> Float)? = null,
    isBuffering: Boolean = false,
    mode: MiniPlayerMode = MiniPlayerMode.EXPANDED,
    hazeState: HazeState? = null,
    accentColor: Color = Color.Unspecified,
    onExpandClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit = {},
    onDismiss: () -> Unit = {},
    onPositioned: ((LayoutCoordinates) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (track == null) return

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // ── Gesture Translation & Physics ──
    val dragOffsetX = remember { Animatable(0f) }
    val dragOffsetY = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(1f) }
    val containerScale = remember { Animatable(1f) }

    LaunchedEffect(track?.id) {
        dragOffsetX.snapTo(0f)
        dragOffsetY.snapTo(0f)
        contentAlpha.snapTo(1f)
        containerScale.snapTo(1f)
    }

    var isSwipeActive by remember { mutableStateOf(false) }
    var hasTriggeredThresholdHaptic by remember { mutableStateOf(false) }

    // Dimensions based on Mode
    val isCompact = mode == MiniPlayerMode.COMPACT
    val isDocked = mode == MiniPlayerMode.DOCKED

    val containerShape: Shape = if (isDocked) {
        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
    } else {
        CircleShape
    }

    val artworkSize = when {
        isDocked -> 52.dp
        isCompact -> 44.dp
        else -> 56.dp
    }
    val artworkShape: Shape = when {
        isDocked -> RoundedCornerShape(14.dp)
        else -> RoundedCornerShape(18.dp)
    }

    val playPauseSize = when {
        isDocked -> 46.dp
        isCompact -> 40.dp
        else -> 48.dp
    }
    val playPauseIconSize = when {
        isDocked -> 22.dp
        isCompact -> 20.dp
        else -> 24.dp
    }

    val nextSize = when {
        isDocked -> 46.dp
        isCompact -> 40.dp
        else -> 48.dp
    }
    val nextIconSize = when {
        isDocked -> 22.dp
        isCompact -> 20.dp
        else -> 24.dp
    }

    val internalPadStart = if (isDocked) 14.dp else 10.dp
    val internalPadEnd = if (isDocked) 14.dp else 12.dp
    val internalPadTop = 0.dp
    val internalPadBottom = 0.dp

    val touchSlopPx = with(density) { 12.dp.toPx() }
    val skipThresholdPx = with(density) { 56.dp.toPx() }
    val dismissThresholdPx = with(density) { 48.dp.toPx() }
    val expandThresholdPx = with(density) { -36.dp.toPx() }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                onPositioned?.invoke(coords)
            }
            .graphicsLayer {
                translationX = dragOffsetX.value
                translationY = dragOffsetY.value
                scaleX = containerScale.value
                scaleY = containerScale.value
            }
            .then(
                if (!isDocked) {
                    Modifier.glassDockShadow(
                        shape = CircleShape,
                        elevation = 16.dp,
                        alpha = 0.40f
                    )
                } else {
                    Modifier
                }
            )
            .clip(containerShape)
            .then(
                if (hazeState != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier
                        .hazeChild(
                            state = hazeState,
                            shape = containerShape,
                            style = HazeStyle(
                                tint = Color(0xFF181818).copy(alpha = 0.48f),
                                blurRadius = 20.dp,
                                noiseFactor = 0f
                            )
                        )
                        .background(Color.White.copy(alpha = 0.08f), containerShape)
                } else {
                    Modifier
                        .background(
                            Color(0xFF1A1A1A).copy(alpha = 0.78f),
                            containerShape
                        )
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.09f), Color.Transparent)
                            ),
                            containerShape
                        )
                }
            )
            .pointerInput(mode) {
                val isCompact = mode == MiniPlayerMode.COMPACT
                val isDocked = mode == MiniPlayerMode.DOCKED
                val padEndPx = with(density) { (if (isDocked) 14.dp else 12.dp).toPx() }
                val spacingPx = with(density) { 8.dp.toPx() }
                val playSizePx = with(density) { (when { isDocked -> 46.dp; isCompact -> 40.dp; else -> 48.dp }).toPx() }
                val nextSizePx = with(density) { (when { isDocked -> 46.dp; isCompact -> 40.dp; else -> 48.dp }).toPx() }

                val velocityTracker = VelocityTracker()
                var isHorizontal: Boolean? = null
                var initialX = 0f
                var initialY = 0f
                var totalDx = 0f
                var totalDy = 0f

                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent().changes.firstOrNull() ?: continue
                        if (down.pressed) {
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val rowH = if (isDocked) with(density) { 72.dp.toPx() } else h
                            val centerY = rowH / 2f

                            // Check if touch is inside Play/Pause or Next button circles
                            val nextCenterX = w - padEndPx - (nextSizePx / 2f)
                            val nextCenterY = centerY
                            val nextRadius = (nextSizePx / 2f) + with(density) { 4.dp.toPx() }
                            val distNextSq = (down.position.x - nextCenterX) * (down.position.x - nextCenterX) +
                                    (down.position.y - nextCenterY) * (down.position.y - nextCenterY)
                            val isOverNext = distNextSq <= nextRadius * nextRadius

                            val playCenterX = w - padEndPx - nextSizePx - spacingPx - (playSizePx / 2f)
                            val playCenterY = centerY
                            val playRadius = (playSizePx / 2f) + with(density) { 4.dp.toPx() }
                            val distPlaySq = (down.position.x - playCenterX) * (down.position.x - playCenterX) +
                                    (down.position.y - playCenterY) * (down.position.y - playCenterY)
                            val isOverPlay = distPlaySq <= playRadius * playRadius

                            // If touch originated on Play/Pause or Next buttons, or was consumed by them, let buttons handle it independently
                            if (down.isConsumed || isOverPlay || isOverNext) {
                                continue
                            }

                            velocityTracker.resetTracking()
                            velocityTracker.addPosition(down.uptimeMillis, down.position)
                            initialX = down.position.x
                            initialY = down.position.y
                            totalDx = 0f
                            totalDy = 0f
                            isHorizontal = null
                            hasTriggeredThresholdHaptic = false

                            val pointerId = down.id

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.find { it.id == pointerId } ?: break
                                if (!change.pressed) {
                                    // Pointer released
                                    change.consume()
                                    val velocity = velocityTracker.calculateVelocity()
                                    val velX = velocity.x / 1000f // px/ms
                                    val velY = velocity.y / 1000f

                                    val isNext = totalDx >= skipThresholdPx || (velX >= 0.55f && totalDx >= with(density) { 20.dp.toPx() })
                                    val isPrev = totalDx <= -skipThresholdPx || (velX <= -0.55f && totalDx <= -with(density) { 20.dp.toPx() })
                                    val isDismiss = !isDocked && (totalDy >= dismissThresholdPx || (velY >= 0.45f && totalDy >= with(density) { 20.dp.toPx() }))

                                    if (isHorizontal == true && isNext) {
                                        // ── Horizontal Commit Next ──
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        scope.launch {
                                            dragOffsetX.animateTo(
                                                with(density) { 110.dp.toPx() },
                                                tween(140, easing = FastOutLinearInEasing)
                                            )
                                            contentAlpha.animateTo(0f, tween(140))
                                            onNextClick()
                                            dragOffsetX.snapTo(with(density) { -48.dp.toPx() })
                                            launch { contentAlpha.animateTo(1f, tween(280)) }
                                            dragOffsetX.animateTo(
                                                0f,
                                                tween(280, easing = CubicBezierEasing(0.22f, 1.12f, 0.36f, 1.0f))
                                            )
                                        }
                                    } else if (isHorizontal == true && isPrev) {
                                        // ── Horizontal Commit Prev ──
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        scope.launch {
                                            dragOffsetX.animateTo(
                                                with(density) { -110.dp.toPx() },
                                                tween(140, easing = FastOutLinearInEasing)
                                            )
                                            contentAlpha.animateTo(0f, tween(140))
                                            onPreviousClick()
                                            dragOffsetX.snapTo(with(density) { 48.dp.toPx() })
                                            launch { contentAlpha.animateTo(1f, tween(280)) }
                                            dragOffsetX.animateTo(
                                                0f,
                                                tween(280, easing = CubicBezierEasing(0.22f, 1.12f, 0.36f, 1.0f))
                                            )
                                        }
                                    } else if (isHorizontal == false && isDismiss) {
                                        // ── Vertical Commit Dismiss ──
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        scope.launch {
                                            launch {
                                                dragOffsetY.animateTo(
                                                    with(density) { 95.dp.toPx() },
                                                    tween(220, easing = CubicBezierEasing(0.32f, 0.0f, 0.67f, 0.0f))
                                                )
                                            }
                                            launch { containerScale.animateTo(0.90f, tween(220)) }
                                            contentAlpha.animateTo(0f, tween(220))
                                            onDismiss()
                                        }
                                    } else {
                                        // ── Tap Anywhere on Mini-Player Body (Album Art, Title, Artist, or Empty Pill Area) or Swipe Up ──
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        scope.launch {
                                            launch {
                                                dragOffsetX.animateTo(
                                                    0f,
                                                    tween(180, easing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f))
                                                )
                                            }
                                            launch {
                                                dragOffsetY.animateTo(
                                                    0f,
                                                    tween(180, easing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f))
                                                )
                                            }
                                            launch { containerScale.animateTo(1f, tween(180)) }
                                            launch { contentAlpha.animateTo(1f, tween(180)) }
                                        }
                                        onExpandClick()
                                    }

                                    isSwipeActive = false
                                    break
                                }

                                velocityTracker.addPosition(change.uptimeMillis, change.position)

                                totalDx = change.position.x - initialX
                                totalDy = change.position.y - initialY

                                // Strict 12.dp deadzone axis lock
                                if (isHorizontal == null) {
                                    val absX = abs(totalDx)
                                    val absY = abs(totalDy)
                                    if (absX >= touchSlopPx || absY >= touchSlopPx) {
                                        if (absY >= absX) {
                                            // Lock to Vertical (swipe down dismiss / swipe up expand)
                                            isHorizontal = false
                                            isSwipeActive = true
                                        } else if (absX > absY * 1.5f) {
                                            // Lock to Horizontal ONLY if clearly horizontal (> 1.5x vertical)
                                            isHorizontal = true
                                            isSwipeActive = true
                                        }
                                    }
                                }

                                if (isSwipeActive) {
                                    change.consume()
                                    if (isHorizontal == true) {
                                        // Rubber-band horizontal skip tracking
                                        val dampedDx = if (abs(totalDx) <= skipThresholdPx) {
                                            totalDx
                                        } else {
                                            val extra = abs(totalDx) - skipThresholdPx
                                            val damped = with(density) { 16.dp.toPx() } * tanh(extra / with(density) { 32.dp.toPx() })
                                            val sign = if (totalDx > 0) 1f else -1f
                                            sign * (skipThresholdPx + damped)
                                        }

                                        scope.launch {
                                            dragOffsetX.snapTo(dampedDx)
                                            val alpha = 1.0f - (0.35f * (abs(totalDx) / skipThresholdPx).coerceIn(0f, 1f))
                                            contentAlpha.snapTo(alpha)
                                        }

                                        if (abs(totalDx) >= skipThresholdPx && !hasTriggeredThresholdHaptic) {
                                            hasTriggeredThresholdHaptic = true
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } else if (abs(totalDx) < skipThresholdPx) {
                                            hasTriggeredThresholdHaptic = false
                                        }
                                    } else if (isHorizontal == false) {
                                        // Vertical tracking
                                        if (totalDy > 0f) {
                                            // Dragging down towards dismiss
                                            val dampedDy = if (totalDy <= dismissThresholdPx) {
                                                totalDy
                                            } else {
                                                val extra = totalDy - dismissThresholdPx
                                                val damped = with(density) { 47.dp.toPx() } * tanh(extra / with(density) { 60.dp.toPx() })
                                                dismissThresholdPx + damped
                                            }.coerceAtMost(with(density) { 95.dp.toPx() })

                                            scope.launch {
                                                dragOffsetY.snapTo(dampedDy)
                                                val p = (totalDy / dismissThresholdPx).coerceIn(0f, 1f)
                                                containerScale.snapTo(1.0f - (0.06f * p))
                                                contentAlpha.snapTo(1.0f - (0.65f * p))
                                            }
                                        } else {
                                            // Dragging up towards expand
                                            val dampedUp = (totalDy * 0.55f).coerceAtLeast(with(density) { -36.dp.toPx() })
                                            scope.launch {
                                                dragOffsetY.snapTo(dampedUp)
                                                containerScale.snapTo(1.015f)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        if (isDocked) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .graphicsLayer {
                            alpha = contentAlpha.value
                        }
                        .padding(start = 14.dp, end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniPlayerContent(
                        track = track,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        artworkSize = artworkSize,
                        artworkShape = artworkShape,
                        playPauseSize = playPauseSize,
                        playPauseIconSize = playPauseIconSize,
                        nextSize = nextSize,
                        nextIconSize = nextIconSize,
                        isCompact = isCompact,
                        haptic = haptic,
                        density = density,
                        onPlayPauseClick = onPlayPauseClick,
                        onNextClick = onNextClick
                    )
                }
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = contentAlpha.value
                    }
                    .padding(
                        start = internalPadStart,
                        end = internalPadEnd,
                        top = internalPadTop,
                        bottom = internalPadBottom
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniPlayerContent(
                    track = track,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    artworkSize = artworkSize,
                    artworkShape = artworkShape,
                    playPauseSize = playPauseSize,
                    playPauseIconSize = playPauseIconSize,
                    nextSize = nextSize,
                    nextIconSize = nextIconSize,
                    isCompact = isCompact,
                    haptic = haptic,
                    density = density,
                    onPlayPauseClick = onPlayPauseClick,
                    onNextClick = onNextClick
                )
            }
        }

        // ── Progress Bar ──
        // Thin 2.dp pure Color.White playback progress indicator (clipped on top edge in DOCKED mode, bottom edge in floating mode)
        val progress = (progressFractionProvider?.invoke() ?: progressFraction).coerceIn(0f, 1f)

        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = if (isPlaying) tween(300, easing = LinearEasing) else tween(0),
            label = "mini_player_progress"
        )

        Box(
            modifier = Modifier
                .align(if (isDocked) Alignment.TopStart else Alignment.BottomStart)
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = animatedProgress
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun RowScope.MiniPlayerContent(
    track: Track,
    isPlaying: Boolean,
    isBuffering: Boolean,
    artworkSize: Dp,
    artworkShape: Shape,
    playPauseSize: Dp,
    playPauseIconSize: Dp,
    nextSize: Dp,
    nextIconSize: Dp,
    isCompact: Boolean,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    density: androidx.compose.ui.unit.Density,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit
) {
    // ── Squircle Album Artwork ──
    VybeTrackArtwork(
        track = track,
        modifier = Modifier.size(artworkSize),
        shape = artworkShape,
        contentDescription = "${track.title} artwork"
    )

    Spacer(modifier = Modifier.width(if (isCompact) 10.dp else 14.dp))

    // ── Track Title & Artist (Larger, Cleaner Typography) ──
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = track.title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 20.sp,
            letterSpacing = (-0.4).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = track.artist,
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    Spacer(modifier = Modifier.width(8.dp))

    // ── Action Buttons ──
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Play / Pause Button: Translucent frosted button (Color.White @ 16%), crisp pure Color.White icon
        var isPlayPressed by remember { mutableStateOf(false) }
        val playScale by animateFloatAsState(
            targetValue = if (isPlayPressed) 0.94f else 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "mini_play_scale"
        )

        Box(
            modifier = Modifier
                .size(playPauseSize)
                .graphicsLayer {
                    scaleX = playScale
                    scaleY = playScale
                }
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPlayPressed = true
                            tryAwaitRelease()
                            isPlayPressed = false
                        },
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPlayPauseClick()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(playPauseIconSize * 0.85f),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                val iconModifier = if (!isPlaying) {
                    Modifier
                        .size(playPauseIconSize)
                        .graphicsLayer { translationX = with(density) { 1.5.dp.toPx() } }
                } else {
                    Modifier.size(playPauseIconSize)
                }
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = iconModifier
                )
            }
        }

        // Next Button: Subtle hover/press sheen with crisp pure Color.White icon
        var isNextPressed by remember { mutableStateOf(false) }
        val nextScale by animateFloatAsState(
            targetValue = if (isNextPressed) 0.94f else 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "mini_next_scale"
        )

        Box(
            modifier = Modifier
                .size(nextSize)
                .graphicsLayer {
                    scaleX = nextScale
                    scaleY = nextScale
                }
                .clip(CircleShape)
                .background(if (isNextPressed) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isNextPressed = true
                            tryAwaitRelease()
                            isNextPressed = false
                        },
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNextClick()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next Track",
                tint = Color.White,
                modifier = Modifier.size(nextIconSize)
            )
        }
    }
}
