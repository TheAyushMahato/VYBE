package com.vybe.music.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Lightweight 60fps AMOLED dark skeleton shimmer modifier.
 * Base Color(0xFF141414) with subtle Color(0xFF222222) shimmer sweep, zero grey borders.
 */
fun Modifier.vybeShimmer(
    shape: Shape? = null
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "vybe_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vybe_shimmer_trans"
    )

    val shimmerColors = listOf(
        Color(0xFF111111),
        Color(0xFF1E1E1E),
        Color(0xFF111111)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim, translateAnim),
        end = Offset(translateAnim + 400f, translateAnim + 400f)
    )

    val baseModifier = if (shape != null) {
        this.clip(shape).background(brush)
    } else {
        this.background(brush)
    }
    baseModifier
}

@Composable
fun VybeSkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .vybeShimmer(shape)
    )
}

/**
 * Single track row skeleton:
 * 52.dp square thumbnail + title & subtitle shimmer bars
 */
@Composable
fun TrackRowSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        VybeSkeletonBox(
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(12.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VybeSkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(14.dp),
                shape = RoundedCornerShape(4.dp)
            )
            VybeSkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .height(11.dp),
                shape = RoundedCornerShape(4.dp)
            )
        }
    }
}

/**
 * Layout-matched skeleton for HomeScreen:
 * - Rounded 245.dp hero card box (RoundedCornerShape(24.dp), matching Vybe Flow)
 * - Section header shimmer bar + horizontal track card boxes (RoundedCornerShape(16.dp))
 * - Circular artist placeholders for Artists for you
 * - Mix card boxes (RoundedCornerShape(16.dp))
 */
@Composable
fun HomeScreenSkeleton(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(
            top = statusBarTop + 64.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // 1. Full-width rounded hero card box matching Vybe Flow
        item {
            VybeSkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(245.dp)
                    .padding(horizontal = 12.dp),
                shape = RoundedCornerShape(24.dp)
            )
        }

        // 2. Section 1: Quick picks
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VybeSkeletonBox(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .width(140.dp)
                        .height(20.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(4) {
                        Column(
                            modifier = Modifier.width(156.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VybeSkeletonBox(
                                modifier = Modifier
                                    .size(156.dp),
                                shape = RoundedCornerShape(16.dp)
                            )
                            VybeSkeletonBox(
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(13.dp),
                                shape = RoundedCornerShape(4.dp)
                            )
                            VybeSkeletonBox(
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(10.dp),
                                shape = RoundedCornerShape(4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Section 2: Artists for you (circular placeholders)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VybeSkeletonBox(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .width(130.dp)
                        .height(20.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(5) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VybeSkeletonBox(
                                modifier = Modifier.size(82.dp),
                                shape = CircleShape
                            )
                            VybeSkeletonBox(
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(11.dp),
                                shape = RoundedCornerShape(4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Section 3: Mixes to explore
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VybeSkeletonBox(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .width(150.dp)
                        .height(20.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(3) {
                        Column(
                            modifier = Modifier.width(156.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VybeSkeletonBox(
                                modifier = Modifier.size(156.dp),
                                shape = RoundedCornerShape(16.dp)
                            )
                            VybeSkeletonBox(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(13.dp),
                                shape = RoundedCornerShape(4.dp)
                            )
                            VybeSkeletonBox(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(10.dp),
                                shape = RoundedCornerShape(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Layout-matched skeleton for SearchScreen results
 */
@Composable
fun SearchScreenSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(7) {
            TrackRowSkeleton()
        }
    }
}

/**
 * Layout-matched skeleton for ArtistDetailScreen:
 * 440.dp hero banner with circular artist avatar + listeners pill + track rows
 */
@Composable
fun ArtistDetailSkeleton(
    onBackClick: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(
            bottom = contentPadding.calculateBottomPadding() + 28.dp
        )
    ) {
        // Hero Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .background(Color(0xFF0D0D0D))
            ) {
                // Back button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(top = statusBarTop + 8.dp, start = 12.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Centered Avatar & Title
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    VybeSkeletonBox(
                        modifier = Modifier.size(130.dp),
                        shape = CircleShape
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    VybeSkeletonBox(
                        modifier = Modifier
                            .width(200.dp)
                            .height(26.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    VybeSkeletonBox(
                        modifier = Modifier
                            .width(96.dp)
                            .height(22.dp),
                        shape = CircleShape
                    )
                }
            }
        }

        // Section header
        item {
            VybeSkeletonBox(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .width(130.dp)
                    .height(20.dp),
                shape = RoundedCornerShape(6.dp)
            )
        }

        // Track rows
        items(6) {
            TrackRowSkeleton()
        }
    }
}

/**
 * Layout-matched skeleton for AlbumDetailScreen:
 * 440.dp hero banner with centered square album cover + track rows
 */
@Composable
fun AlbumDetailSkeleton(
    onBackClick: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(
            bottom = contentPadding.calculateBottomPadding() + 28.dp
        )
    ) {
        // Hero Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .background(Color(0xFF0D0D0D))
            ) {
                // Back button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(top = statusBarTop + 8.dp, start = 12.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Centered Album Artwork & Title
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    VybeSkeletonBox(
                        modifier = Modifier.size(200.dp),
                        shape = RoundedCornerShape(20.dp)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    VybeSkeletonBox(
                        modifier = Modifier
                            .width(210.dp)
                            .height(24.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    VybeSkeletonBox(
                        modifier = Modifier
                            .width(120.dp)
                            .height(14.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }
        }

        // Section header
        item {
            VybeSkeletonBox(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .width(110.dp)
                    .height(20.dp),
                shape = RoundedCornerShape(6.dp)
            )
        }

        // Track rows
        items(6) {
            TrackRowSkeleton()
        }
    }
}

/**
 * Layout-matched skeleton for StatsScreen
 */
@Composable
fun StatsScreenSkeleton(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(
            top = statusBarTop + 82.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Listening pills
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VybeSkeletonBox(
                    modifier = Modifier
                        .width(110.dp)
                        .height(32.dp),
                    shape = CircleShape
                )
                VybeSkeletonBox(
                    modifier = Modifier
                        .width(120.dp)
                        .height(32.dp),
                    shape = CircleShape
                )
            }
        }

        // Hero Summary Card
        item {
            VybeSkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 14.dp),
                shape = RoundedCornerShape(32.dp)
            )
        }

        // Filter pills row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(4) {
                    VybeSkeletonBox(
                        modifier = Modifier
                            .width(82.dp)
                            .height(34.dp),
                        shape = CircleShape
                    )
                }
            }
        }

        // Track rows
        items(5) {
            TrackRowSkeleton()
        }
    }
}

/**
 * Layout-matched skeleton for LibraryScreen
 */
@Composable
fun LibraryScreenSkeleton(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(
            top = statusBarTop + 82.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top summary header card
        item {
            VybeSkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Rows
        items(6) {
            TrackRowSkeleton()
        }
    }
}
