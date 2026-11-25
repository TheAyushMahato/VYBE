package com.vybe.music.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vybe.music.model.NavTab
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

data class BottomNavItem(
    val tab: NavTab,
    val title: String
)

/**
 * Sleek custom vector icon for Home:
 * - Selected: Solid filled rounded-roof house silhouette (Color.White) with soft rounded bottom corners and NO door cutout.
 * - Unselected: Clean outlined rounded-roof house (strokeWidth = 2.1.dp, StrokeCap.Round, StrokeJoin.Round, Color.White) with small rounded vertical doorway arch.
 */
@Composable
fun NavHomeIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier.size(26.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        if (isSelected) {
            val path = Path().apply {
                moveTo(12f / 24f * w, 3.2f / 24f * h)
                lineTo(3.6f / 24f * w, 10.4f / 24f * h)
                lineTo(5.6f / 24f * w, 10.4f / 24f * h)
                lineTo(5.6f / 24f * w, 19.4f / 24f * h)
                quadraticTo(
                    5.6f / 24f * w, 21.0f / 24f * h,
                    7.2f / 24f * w, 21.0f / 24f * h
                )
                lineTo(16.8f / 24f * w, 21.0f / 24f * h)
                quadraticTo(
                    18.4f / 24f * w, 21.0f / 24f * h,
                    18.4f / 24f * w, 19.4f / 24f * h
                )
                lineTo(18.4f / 24f * w, 10.4f / 24f * h)
                lineTo(20.4f / 24f * w, 10.4f / 24f * h)
                close()
            }
            drawPath(path = path, color = Color.White)
        } else {
            val strokePx = 2.1.dp.toPx()
            val path = Path().apply {
                moveTo(9.8f / 24f * w, 20.5f / 24f * h)
                lineTo(6.0f / 24f * w, 20.5f / 24f * h)
                lineTo(6.0f / 24f * w, 10.6f / 24f * h)
                lineTo(3.8f / 24f * w, 10.6f / 24f * h)
                lineTo(12.0f / 24f * w, 3.8f / 24f * h)
                lineTo(20.2f / 24f * w, 10.6f / 24f * h)
                lineTo(18.0f / 24f * w, 10.6f / 24f * h)
                lineTo(18.0f / 24f * w, 20.5f / 24f * h)
                lineTo(14.2f / 24f * w, 20.5f / 24f * h)
                lineTo(14.2f / 24f * w, 15.0f / 24f * h)
                quadraticTo(
                    12.0f / 24f * w, 13.0f / 24f * h,
                    9.8f / 24f * w, 15.0f / 24f * h
                )
                lineTo(9.8f / 24f * w, 20.5f / 24f * h)
            }
            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

/**
 * Sleek custom vector icon for Stats:
 * 3 sleek, thin vertical rounded-cap bars (strokeWidth = 2.4.dp, StrokeCap.Round, Color.White)
 * aligned at the bottom baseline — left short (~42%), center tall (100%), right medium (~68%).
 */
@Composable
fun NavStatsIcon(
    isSelected: Boolean = false,
    modifier: Modifier = Modifier.size(26.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokePx = 2.4.dp.toPx()

        val baseY = 20.0f / 24f * h
        val tallTopY = 4.0f / 24f * h
        val barSpan = baseY - tallTopY

        val leftX = 6.0f / 24f * w
        val centerX = 12.0f / 24f * w
        val rightX = 18.0f / 24f * w

        val leftTopY = baseY - (barSpan * 0.42f)
        val centerTopY = tallTopY
        val rightTopY = baseY - (barSpan * 0.68f)

        // Left Bar (short ~42%)
        drawLine(
            color = Color.White,
            start = Offset(leftX, baseY),
            end = Offset(leftX, leftTopY),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )

        // Center Bar (tall 100%)
        drawLine(
            color = Color.White,
            start = Offset(centerX, baseY),
            end = Offset(centerX, centerTopY),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )

        // Right Bar (medium ~68%)
        drawLine(
            color = Color.White,
            start = Offset(rightX, baseY),
            end = Offset(rightX, rightTopY),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Sleek custom vector icon for Playlists:
 * 3 thin rounded horizontal lines (strokeWidth = 2.1.dp, StrokeCap.Round, Color.White) stacked on left
 * paired on bottom-right with a sleek outlined musical note (vertical stem + flag curve + hollow circular notehead).
 */
@Composable
fun NavPlaylistsIcon(
    isSelected: Boolean = false,
    modifier: Modifier = Modifier.size(26.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokePx = 2.1.dp.toPx()

        // 3 horizontal lines on left
        drawLine(
            color = Color.White,
            start = Offset(3.5f / 24f * w, 6.2f / 24f * h),
            end = Offset(13.5f / 24f * w, 6.2f / 24f * h),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(3.5f / 24f * w, 11.2f / 24f * h),
            end = Offset(13.5f / 24f * w, 11.2f / 24f * h),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(3.5f / 24f * w, 16.2f / 24f * h),
            end = Offset(9.5f / 24f * w, 16.2f / 24f * h),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )

        // Outlined musical note on bottom-right
        val stemX = 18.5f / 24f * w
        val stemTopY = 6.2f / 24f * h
        val stemBottomY = 16.5f / 24f * h

        drawLine(
            color = Color.White,
            start = Offset(stemX, stemTopY),
            end = Offset(stemX, stemBottomY),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )

        val flagPath = Path().apply {
            moveTo(stemX, stemTopY)
            quadraticTo(
                21.2f / 24f * w, 6.5f / 24f * h,
                21.2f / 24f * w, 9.8f / 24f * h
            )
        }
        drawPath(
            path = flagPath,
            color = Color.White,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        val noteheadCenterX = 16.0f / 24f * w
        val noteheadCenterY = 16.5f / 24f * h
        val noteheadRadius = 2.4.dp.toPx()

        drawCircle(
            color = Color.White,
            radius = noteheadRadius,
            center = Offset(noteheadCenterX, noteheadCenterY),
            style = Stroke(width = strokePx)
        )
    }
}

@Composable
fun BottomNavBar(
    selectedTab: NavTab,
    isCollapsed: Boolean,
    onTabSelected: (NavTab) -> Unit,
    onExpandClick: () -> Unit,
    hazeState: HazeState? = null,
    accentColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val items = remember {
        listOf(
            BottomNavItem(NavTab.HOME, "Home"),
            BottomNavItem(NavTab.STATS, "Stats"),
            BottomNavItem(NavTab.PLAYLISTS, "Playlists")
        )
    }

    val capsuleWidth by animateDpAsState(
        targetValue = if (isCollapsed) 74.dp else 312.dp,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "nav_capsule_width"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // ── Stable Floating Frosted-Glass Outer Capsule (312.dp expanded / 74.dp collapsed, 74.dp height, CircleShape, padding 7.dp) ──
        Box(
            modifier = Modifier
                .width(capsuleWidth)
                .height(74.dp)
                .glassDockShadow(shape = CircleShape, elevation = 16.dp, alpha = 0.40f)
                .clip(CircleShape)
                .then(
                    if (hazeState != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier
                            .hazeChild(
                                state = hazeState,
                                shape = CircleShape,
                                style = HazeStyle(
                                    tint = Color(0xFF141414).copy(alpha = 0.62f),
                                    blurRadius = 22.dp,
                                    noiseFactor = 0f
                                )
                            )
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    } else {
                        Modifier
                            .background(
                                Color(0xFF161616).copy(alpha = 0.85f),
                                CircleShape
                            )
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.09f), Color.Transparent)
                                ),
                                CircleShape
                            )
                    }
                )
                .padding(7.dp),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = isCollapsed,
                animationSpec = tween(durationMillis = 180),
                label = "nav_morph_content"
            ) { collapsed ->
                if (collapsed) {
                    // ── State B: Scroll-Collapsed Compact Circular Home Button ──
                    var isCollapsedPressed by remember { mutableStateOf(false) }
                    val collapsedScale by animateFloatAsState(
                        targetValue = if (isCollapsedPressed) 0.94f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "nav_collapsed_scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .graphicsLayer {
                                scaleX = collapsedScale
                                scaleY = collapsedScale
                            }
                            .clip(CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isCollapsedPressed = true
                                        tryAwaitRelease()
                                        isCollapsedPressed = false
                                    },
                                    onTap = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onExpandClick()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        NavHomeIcon(isSelected = true, modifier = Modifier.size(26.dp))
                    }
                } else {
                    // ── State A: Expanded Floating 3-Tab Nav Capsule with Sliding Frosted Pill ──
                    val slotBounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
                    val selectedIndex = items.indexOfFirst { it.tab == selectedTab }.coerceAtLeast(0)
                    val targetBounds = slotBounds[selectedIndex]

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Layer 1: Single borderless sliding frosted pill behind tabs
                        if (targetBounds != null) {
                            val pillOffsetX by animateDpAsState(
                                targetValue = with(density) { targetBounds.first.toDp() },
                                animationSpec = spring(
                                    dampingRatio = 0.82f,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "nav_pill_offset_x"
                            )
                            val pillWidth by animateDpAsState(
                                targetValue = with(density) { targetBounds.second.toDp() },
                                animationSpec = spring(
                                    dampingRatio = 0.82f,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "nav_pill_width"
                            )

                            if (pillWidth > 0.dp) {
                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(pillOffsetX.roundToPx(), 0) }
                                        .width(pillWidth)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.16f))
                                )
                            }
                        }

                        // Layer 2: 3 Smoothly weighted tab slots
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items.forEachIndexed { index, item ->
                                val isSelected = selectedTab == item.tab
                                var isTabPressed by remember { mutableStateOf(false) }
                                val tabScale by animateFloatAsState(
                                    targetValue = if (isTabPressed) 0.96f else 1.0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "nav_tab_scale_${item.tab}"
                                )
                                val targetWeight = when {
                                    !isSelected -> 1.0f
                                    item.tab == NavTab.PLAYLISTS -> 2.05f
                                    else -> 1.65f
                                }
                                val tabWeight by animateFloatAsState(
                                    targetValue = targetWeight,
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "nav_tab_weight_${item.tab}"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(tabWeight)
                                        .fillMaxHeight()
                                        .onGloballyPositioned { coords ->
                                            val x = coords.positionInParent().x
                                            val w = coords.size.width.toFloat()
                                            val prev = slotBounds[index]
                                            if (prev == null || kotlin.math.abs(prev.first - x) > 0.5f || kotlin.math.abs(prev.second - w) > 0.5f) {
                                                slotBounds[index] = x to w
                                            }
                                        }
                                        .graphicsLayer {
                                            scaleX = tabScale
                                            scaleY = tabScale
                                        }
                                        .clip(CircleShape)
                                        .pointerInput(item.tab) {
                                            detectTapGestures(
                                                onPress = {
                                                    isTabPressed = true
                                                    tryAwaitRelease()
                                                    isTabPressed = false
                                                },
                                                onTap = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onTabSelected(item.tab)
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        when (item.tab) {
                                            NavTab.HOME -> NavHomeIcon(isSelected = isSelected, modifier = Modifier.size(26.dp))
                                            NavTab.STATS -> NavStatsIcon(isSelected = isSelected, modifier = Modifier.size(26.dp))
                                            NavTab.PLAYLISTS -> NavPlaylistsIcon(isSelected = isSelected, modifier = Modifier.size(26.dp))
                                            else -> Unit
                                        }

                                        AnimatedVisibility(
                                            visible = isSelected,
                                            enter = fadeIn(tween(180, delayMillis = 50)) + expandHorizontally(
                                                expandFrom = Alignment.Start,
                                                animationSpec = tween(200)
                                            ),
                                            exit = fadeOut(tween(80)) + shrinkHorizontally(
                                                shrinkTowards = Alignment.Start,
                                                animationSpec = tween(140)
                                            )
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = item.title,
                                                    color = Color.White,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    letterSpacing = 0.sp,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Visible
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
        }
    }
}
