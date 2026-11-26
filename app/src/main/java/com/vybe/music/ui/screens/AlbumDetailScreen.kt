package com.vybe.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vybe.music.model.AlbumDetail
import com.vybe.music.model.Track
import com.vybe.music.ui.components.AlbumDetailSkeleton
import com.vybe.music.ui.components.bouncyClickable

@Composable
fun AlbumDetailScreen(
    albumDetail: AlbumDetail?,
    isLoading: Boolean,
    currentTrack: Track?,
    isPlaying: Boolean,
    onBackClick: () -> Unit,
    onPlayAllClick: (List<Track>, Boolean) -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val showStickyHeader by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 380
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        if (albumDetail == null && isLoading) {
            AlbumDetailSkeleton(
                onBackClick = onBackClick,
                contentPadding = contentPadding
            )
            return
        }

        val album = albumDetail ?: return
        val tracks = album.tracks
        val isCurrentAlbumPlaying = isPlaying && tracks.any { it.id == currentTrack?.id }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + 115.dp
            )
        ) {
            // ── 1. Tall Edge-to-Edge Full-Bleed Hero Header (440.dp) ────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(440.dp)
                ) {
                    // Full-bleed album artwork
                    if (album.artworkUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(album.artworkUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Multi-stop vertical black gradient seamlessly merging into AMOLED black
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Transparent,
                                        Color(0xFF000000).copy(alpha = 0.65f),
                                        Color(0xFF000000)
                                    )
                                )
                            )
                    )

                    // Floating Glass Top Bar inside Hero
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 44.dp circular dark translucent Back button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .bouncyClickable(onClick = onBackClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 44.dp circular dark translucent Options button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Options",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Hero Bottom Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // 34sp bold Album Title
                        Text(
                            text = album.title,
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 38.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Clickable Artist Name Pill
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .bouncyClickable { onArtistClick(album.artist) }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = album.artist,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtitle: Artist • Year • Track count • Duration
                        val totalDurationMs = tracks.sumOf { it.durationMs }
                        val formattedDuration = if (totalDurationMs > 0) {
                            val totalMinutes = totalDurationMs / 60000
                            if (totalMinutes > 60) "${totalMinutes / 60} hr ${totalMinutes % 60} min" else "$totalMinutes min"
                        } else null

                        val songCountText = if (tracks.isNotEmpty()) {
                            "${tracks.size} songs"
                        } else if (album.trackCount > 0) {
                            "${album.trackCount} songs"
                        } else null

                        val subtitleParts = listOfNotNull(
                            album.artist.takeIf { it.isNotBlank() },
                            album.year.takeIf { it.isNotBlank() },
                            songCountText,
                            formattedDuration
                        )
                        val subtitleText = subtitleParts.joinToString(" • ")

                        if (subtitleText.isNotBlank()) {
                            Text(
                                text = subtitleText,
                                color = Color(0xFFA0A0A0),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Row: 50dp Shuffle | 50dp Radio | 54dp Play capsule pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                // 50dp circular Shuffle button
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF242424))
                                        .bouncyClickable {
                                            if (tracks.isNotEmpty()) onPlayAllClick(tracks, true)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // 50dp circular Radio button
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF242424))
                                        .bouncyClickable {
                                            if (tracks.isNotEmpty()) onPlayAllClick(tracks, false)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Radio,
                                        contentDescription = "Album Radio",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // 54dp Soft Off-White Play Capsule Pill Button
                            Box(
                                modifier = Modifier
                                    .height(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDCDCDC))
                                    .bouncyClickable {
                                        if (tracks.isNotEmpty()) {
                                            if (isCurrentAlbumPlaying) {
                                                onTrackClick(currentTrack ?: tracks.first(), tracks)
                                            } else {
                                                onPlayAllClick(tracks, false)
                                            }
                                        }
                                    }
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentAlbumPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isCurrentAlbumPlaying) "Pause Album" else "Play Album",
                                        tint = Color(0xFF121212),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = if (isCurrentAlbumPlaying) "Pause" else "Play",
                                        color = Color(0xFF121212),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 2. Tracklist (Material 3 Expressive Grouped Cards) ───────────
            if (tracks.isNotEmpty()) {
                item {
                    Text(
                        text = "Tracks",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 10.dp)
                    )
                }

                itemsIndexed(tracks, key = { index, item -> "alb_tr_${item.id}_$index" }) { index, track ->
                    val isFirst = index == 0
                    val isLast = index == tracks.lastIndex
                    val isSingle = tracks.size == 1

                    val cardShape = when {
                        isSingle -> RoundedCornerShape(24.dp)
                        isFirst -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
                        isLast -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                        else -> RoundedCornerShape(6.dp)
                    }

                    val isCurrent = currentTrack?.id == track.id
                    val formatDuration = formatTrackDuration(track.durationMs)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .clip(cardShape)
                            .background(Color(0xFF141414))
                            .bouncyClickable {
                                onTrackClick(track, tracks)
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Track Number / Artwork
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E1E1E)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (track.artworkUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(track.artworkUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = "${index + 1}",
                                        color = if (isCurrent) Color(0xFF5CCBDE) else Color(0xFF888888),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Title & Subtitle
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = track.title,
                                    color = if (isCurrent) Color(0xFF5CCBDE) else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${track.artist} • $formatDuration",
                                    color = Color(0xFFA8A8A8),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // 3-dots overflow button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF262626)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Options",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            } else if (!isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tracks available in this release",
                            color = Color(0xFF888888),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // ── Sticky Collapsed Header on Scroll ────────────────────────────────
        AnimatedVisibility(
            visible = showStickyHeader,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A0A).copy(alpha = 0.94f))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1E1E))
                            .bouncyClickable(onClick = onBackClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    if (album.artworkUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(album.artworkUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF222222))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Text(
                        text = album.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCDCDC))
                            .bouncyClickable {
                                if (tracks.isNotEmpty()) {
                                    if (isCurrentAlbumPlaying) {
                                        onTrackClick(currentTrack ?: tracks.first(), tracks)
                                    } else {
                                        onPlayAllClick(tracks, false)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCurrentAlbumPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = Color(0xFF121212),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTrackDuration(durationMs: Long): String {
    if (durationMs <= 0) return "3:00"
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
