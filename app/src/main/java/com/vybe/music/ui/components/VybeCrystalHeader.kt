package com.vybe.music.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Single-Source-of-Truth Crystal-Clear Zero-Blur Transparent Top Header.
 *
 * - 100% ZERO BLUR: NO Modifier.blur, NO hazeEffect, NO RenderEffect.
 *   Scrolling album art and text behind it remain crystal clear, and foreground text/icons razor sharp.
 * - Shape: RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp) extending edge-to-edge behind status bar.
 * - Subtle Dark Glass Fill: Color.Black.copy(alpha = 0.28f).
 * - Sleek Bottom-Curved Glass Stroke (Header ONLY):
 *   BorderStroke(0.9.dp, Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.18f))))
 * - Foreground: 28.sp bold Color.White title, borderless 40.dp circular Search button (#1F1F1F @ 85%),
 *   and borderless 40.dp circular Profile avatar (or 40.dp Back button for Settings).
 */
@Composable
fun VybeCrystalHeader(
    title: String,
    onSearchClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    userPhotoUrl: String? = null,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val headerShape = remember { RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp) }
    val glassRimStroke = remember {
        BorderStroke(
            0.9.dp,
            Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.05f),
                    Color.White.copy(alpha = 0.18f)
                )
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(10f)
            .clip(headerShape)
            .background(Color.Black.copy(alpha = 0.28f), headerShape)
            .border(glassRimStroke, headerShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = if (showBackButton) Arrangement.spacedBy(14.dp) else Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                // Borderless circular 40.dp Back button (#1F1F1F @ 85%)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F1F1F).copy(alpha = 0.85f))
                        .bouncyClickable(pressedScale = 0.88f, onClick = onBackClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Razor-Sharp Bold Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        fontSize = 28.sp,
                        color = Color.White
                    )
                )
            } else {
                // Razor-Sharp Bold Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        fontSize = 28.sp,
                        color = Color.White
                    )
                )

                // Right side: Search + Profile
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Borderless circular 40.dp Search button (#1F1F1F @ 85%)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1F1F1F).copy(alpha = 0.85f))
                            .bouncyClickable(pressedScale = 0.88f, onClick = onSearchClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Borderless circular 40.dp Profile avatar (#1F1F1F @ 85%)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1F1F1F).copy(alpha = 0.85f))
                            .bouncyClickable(pressedScale = 0.88f, onClick = onProfileClick),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!userPhotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(userPhotoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = "Profile",
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
