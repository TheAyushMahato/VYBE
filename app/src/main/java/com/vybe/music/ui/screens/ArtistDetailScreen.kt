package com.vybe.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vybe.music.model.ArtistAlbum
import com.vybe.music.model.ArtistDetail
import com.vybe.music.model.ArtistSummary
import com.vybe.music.model.Track
import com.vybe.music.ui.components.VybeTrackArtwork
import com.vybe.music.ui.components.bouncyClickable

@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArtistDetailScreen(
    artistName: String,
    artistDetail: ArtistDetail?,
    isLoading: Boolean,
    currentTrack: Track?,
    isPlaying: Boolean,
    onBackClick: () -> Unit,
    onPlayAllClick: (List<Track>, Boolean) -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (browseId: String?, albumTitle: String, artistName: String, artworkUrl: String, year: String) -> Unit = { _, _, _, _, _ -> },
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var isBioExpanded by remember { mutableStateOf(false) }
    var showAllSongs by remember { mutableStateOf(false) }

    val topSongs = artistDetail?.topSongs ?: emptyList()
    val isCurrentArtistPlaying = isPlaying && currentTrack?.artist?.contains(artistName, ignoreCase = true) == true

    // Sticky top bar trigger
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
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + 28.dp
            )
        ) {
            // ── 1. Hero Header (440.dp) ──────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(440.dp)
                ) {
                    val bannerUrl = artistDetail?.backdropUrl?.ifBlank { artistDetail.imageUrl } ?: ""

                    // Background: High-res Photo
                    if (bannerUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(bannerUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = artistDetail?.name ?: artistName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Bottom scrim gradient fading seamlessly into AMOLED #000000
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

                    // Floating Top Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Top-left: 44.dp circular dark translucent Back button
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

                        // Top-right: 44.dp circular dark translucent menu button
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
                        // Artist Name: 34.sp Bold, lineHeight 38.sp
                        Text(
                            text = artistDetail?.name ?: artistName,
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 38.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Listeners Pill: Compact translucent pill
                        val listenersText = artistDetail?.monthlyListeners?.ifBlank { "Verified Artist" } ?: "Verified Artist"
                        val formattedPillText = if (listenersText.contains("listeners", ignoreCase = true)) {
                            listenersText
                        } else {
                            "$listenersText listeners"
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = formattedPillText,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left side: Shuffle & Artist Radio buttons (46.dp circular)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Shuffle Button
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF242424))
                                        .bouncyClickable {
                                            if (topSongs.isNotEmpty()) onPlayAllClick(topSongs, true)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Artist Radio Button
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF242424))
                                        .bouncyClickable {
                                            if (topSongs.isNotEmpty()) onPlayAllClick(topSongs, false)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Radio,
                                        contentDescription = "Artist Radio",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Right side: One large 58.dp circular soft off-white Play / Pause button
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE0E0E0))
                                    .bouncyClickable {
                                        if (topSongs.isNotEmpty()) {
                                            if (isCurrentArtistPlaying) {
                                                onTrackClick(currentTrack ?: topSongs.first(), topSongs)
                                            } else {
                                                onPlayAllClick(topSongs, false)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCurrentArtistPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Play Artist",
                                    tint = Color(0xFF121212),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── 2. Genre Chips ───────────────────────────────────────────────
            val genres = artistDetail?.genres ?: emptyList()
            if (genres.isNotEmpty()) {
                item {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (genre in genres) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF141414))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = genre,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // ── 3. Expandable About Section ──────────────────────────────────
            val bio = artistDetail?.bio.orEmpty()
            if (bio.isNotBlank()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .animateContentSize()
                    ) {
                        Text(
                            text = "About",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = bio,
                            color = Color(0xFFD0D0D0),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            maxLines = if (isBioExpanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBioExpanded) "Show less" else "... Read more",
                            color = Color(0xFF5CCBDE),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { isBioExpanded = !isBioExpanded }
                        )
                    }
                }
            }

            // ── 4. Songs Section (Expandable 5 -> All) ───────────────────────
            if (topSongs.isNotEmpty()) {
                item {
                    Text(
                        text = "Songs",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 10.dp)
                    )
                }

                val songsToDisplay = if (showAllSongs) topSongs else topSongs.take(5)

                itemsIndexed(songsToDisplay, key = { index, track -> "art_song_${track.id}_$index" }) { index, track ->
                    val isFirst = index == 0
                    val isLast = index == songsToDisplay.lastIndex
                    val isSingle = songsToDisplay.size == 1

                    val cardShape = when {
                        isSingle -> RoundedCornerShape(24.dp)
                        isFirst -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
                        isLast -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                        else -> RoundedCornerShape(6.dp)
                    }

                    val isCurrent = currentTrack?.id == track.id

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 1.5.dp)
                            .clip(cardShape)
                            .background(Color(0xFF141414))
                            .bouncyClickable { onTrackClick(track, topSongs) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VybeTrackArtwork(
                                track = track,
                                modifier = Modifier.size(54.dp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
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
                                    text = track.album.ifBlank { artistName },
                                    color = Color(0xFFA8A8A8),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // 3-dots button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF262626))
                                    .clickable { /* options */ },
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

                // Show all songs / Show less centered pill button
                if (topSongs.size > 5) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF1A1A1A))
                                    .clickable { showAllSongs = !showAllSongs }
                                    .padding(horizontal = 18.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = if (showAllSongs) "Show less ^" else "Show all songs ⌄",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // ── 5. Albums Section ────────────────────────────────────────────
            val albums = artistDetail?.albums ?: emptyList()
            if (albums.isNotEmpty()) {
                item {
                    Text(
                        text = "Albums",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 12.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(albums, key = { "alb_${it.id}" }) { album ->
                            Column(
                                modifier = Modifier
                                    .width(156.dp)
                                    .bouncyClickable {
                                        onAlbumClick(album.browseId, album.title, album.artist, album.artworkUrl, album.year)
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(156.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF1E1E22))
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(album.artworkUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = album.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Dark translucent Year pill at bottom-left
                                    if (album.year.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(8.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black.copy(alpha = 0.65f))
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = album.year,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = album.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Album",
                                    color = Color(0xFFA0A0A0),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // ── 6. Singles & EPs Section ─────────────────────────────────────
            val singles = artistDetail?.singlesAndEps ?: emptyList()
            if (singles.isNotEmpty()) {
                item {
                    Text(
                        text = "Singles & EPs",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 12.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(singles, key = { "single_${it.id}" }) { single ->
                            Column(
                                modifier = Modifier
                                    .width(156.dp)
                                    .bouncyClickable {
                                        onAlbumClick(single.browseId, single.title, single.artist, single.artworkUrl, single.year)
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(156.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF1E1E22))
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(single.artworkUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = single.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    if (single.year.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(8.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black.copy(alpha = 0.65f))
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = single.year,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = single.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Single",
                                    color = Color(0xFFA0A0A0),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // ── 7. Fans Also Like Section ────────────────────────────────────
            val related = artistDetail?.relatedArtists ?: emptyList()
            if (related.isNotEmpty()) {
                item {
                    Text(
                        text = "Fans Also Like",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 12.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(related, key = { "rel_${it.name}" }) { rel ->
                            Column(
                                modifier = Modifier
                                    .width(104.dp)
                                    .bouncyClickable { onArtistClick(rel.name) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(rel.imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = rel.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(104.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E1E22))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = rel.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
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

                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(artistDetail?.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = artistDetail?.name ?: artistName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222222))
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = artistDetail?.name ?: artistName,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // 40.dp circular off-white play button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E0E0))
                            .bouncyClickable {
                                if (topSongs.isNotEmpty()) {
                                    if (isCurrentArtistPlaying) {
                                        onTrackClick(currentTrack ?: topSongs.first(), topSongs)
                                    } else {
                                        onPlayAllClick(topSongs, false)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCurrentArtistPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play Artist",
                            tint = Color(0xFF121212),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
