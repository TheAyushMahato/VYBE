package com.vybe.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vybe.music.model.ArtistSummary
import com.vybe.music.data.repository.ArtistRepository

/**
 * Reusable Artist Spotlight Card matching a sleek, borderless, high-contrast Apple/LastWave aesthetic.
 *
 * Features:
 * - RoundedCornerShape(28.dp) card, base background #111111, zero outer border.
 * - Sharp / unblurred artist backdrop (0.dp blur) in the top 65% at alpha = 0.42f,
 *   covered by a smooth vertical gradient fading into solid #111111 at 0.62f to 1.0f.
 * - Clean borderless CircleShape ARTIST SPOTLIGHT badge with monochrome white sparkle icon.
 * - 82.dp borderless circular artist photo + 27.sp bold artist name + MusicNote track subtitle.
 * - Chunky 52.dp borderless action pills (#E2E2E2 ▶ Artist and #1D1D1D View artist).
 */
@Composable
fun ArtistSpotlightCard(
    artistName: String,
    imageUrl: String,
    topTrackTitle: String = "Popular Hits",
    onPlayArtistClick: () -> Unit,
    onViewArtistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val validImageUrl = remember(imageUrl, artistName) {
        if (ArtistRepository.isValidArtistImageUrl(imageUrl)) {
            imageUrl
        } else {
            val verified = ArtistRepository.findVerifiedSummary(artistName)?.imageUrl
            if (ArtistRepository.isValidArtistImageUrl(verified)) verified!!
            else "https://c.saavncdn.com/artists/The_Weeknd_002_20241003071400_500x500.jpg"
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(256.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF111111))
    ) {
        // ── Background: Sharp dimmed artist backdrop (top 65%, NO blur) ─────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .graphicsLayer { alpha = 0.42f }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(validImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Smooth vertical gradient fading into solid #111111 at 0.62f to 1.0f ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.35f to Color(0xFF111111).copy(alpha = 0.45f),
                        0.62f to Color(0xFF111111),
                        1.0f to Color(0xFF111111)
                    )
                )
        )

        // ── Card Foreground Content ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top-Left: Clean Borderless ARTIST SPOTLIGHT Pill ──
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "ARTIST SPOTLIGHT",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.6.sp
                )
            }

            // ── Middle Row: Larger Borderless Avatar & Bold Typography ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Avatar: 82.dp, CircleShape, zero border/ring
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E1E))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(validImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = artistName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Artist Name + Top Track Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artistName,
                        color = Color.White,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = topTrackTitle,
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ── Bottom Row: Chunky Borderless Action Pills (52.dp height) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left: Fill #E2E2E2, dark #111111 play triangle icon + Artist text (16.sp, FontWeight.SemiBold)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2E2E2))
                        .bouncyClickable(pressedScale = 0.94f) {
                            onPlayArtistClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play Artist",
                            tint = Color(0xFF111111),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Artist",
                            color = Color(0xFF111111),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Right: Fill #1D1D1D (no border), crisp #FFFFFF View artist text (16.sp, FontWeight.SemiBold)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1D1D1D))
                        .bouncyClickable(pressedScale = 0.94f) {
                            onViewArtistClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "View artist",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ArtistSpotlightCard(
    summary: ArtistSummary,
    onPlayArtistClick: () -> Unit,
    onViewArtistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ArtistSpotlightCard(
        artistName = summary.name,
        imageUrl = summary.imageUrl,
        topTrackTitle = summary.topTrackTitle.ifBlank { summary.genre },
        onPlayArtistClick = onPlayArtistClick,
        onViewArtistClick = onViewArtistClick,
        modifier = modifier
    )
}
