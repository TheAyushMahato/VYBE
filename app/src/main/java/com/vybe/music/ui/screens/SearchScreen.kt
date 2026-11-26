package com.vybe.music.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vybe.music.model.Track
import com.vybe.music.ui.components.SearchScreenSkeleton
import com.vybe.music.ui.components.VybeTrackArtwork
import com.vybe.music.ui.components.bouncyClickable

@Composable
fun SearchScreen(
    searchQuery: String,
    searchResults: List<Track>,
    isSearching: Boolean,
    currentTrack: Track?,
    onQueryChanged: (String) -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit = { _, _ -> },
    onOpenArtistDetail: (String, Boolean) -> Unit = { _, _ -> },
    onAlbumClick: (browseId: String?, albumTitle: String, artistName: String, artworkUrl: String, year: String) -> Unit = { _, _, _, _, _ -> },
    onBackClick: () -> Unit = {},
    recentSearches: List<String> = emptyList(),
    onAddRecentSearch: (String) -> Unit = {},
    onRemoveRecentSearch: (String) -> Unit = {},
    onClearRecentSearches: () -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val addRecent: (String) -> Unit = { term ->
        val trimmed = term.trim()
        if (trimmed.isNotBlank()) {
            onAddRecentSearch(trimmed)
        }
    }

    val context = LocalContext.current

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = matches?.firstOrNull()
            if (!text.isNullOrBlank()) {
                onQueryChanged(text)
                addRecent(text)
            }
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(searchQuery) {
        listState.scrollToItem(0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding()
    ) {
        // ── 1. Top Search Bar Row (Symmetrical: Back 46dp, Input Pill 46dp, Mic 46dp) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Back Button (46.dp, CircleShape, #1A1A1A surface)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1A))
                    .bouncyClickable(pressedScale = 0.92f, onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Pill-Shaped Search Input (46.dp height, CircleShape, #1A1A1A surface, zero harsh border)
            BasicTextField(
                value = searchQuery,
                onValueChange = onQueryChanged,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(Color.White),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF1A1A1A))
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search songs, artists, albums...",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 15.sp
                                )
                            }
                            innerTextField()
                        }
                        if (searchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable { onQueryChanged("") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF8E8E93),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            )

            // Separate Circular Voice / Mic Button (46.dp, CircleShape, exact same #1A1A1A surface as Back)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1A))
                    .bouncyClickable(pressedScale = 0.90f) {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Search songs, artists, albums...")
                        }
                        try {
                            voiceLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Voice recognition not available", Toast.LENGTH_SHORT).show()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice search",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ── 2. Content Area (Empty Query State or Active Search Results) ──
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                top = 6.dp,
                bottom = contentPadding.calculateBottomPadding()
            )
        ) {
            if (searchQuery.isBlank()) {
                // ── Empty Query State: Recent Searches ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent searches",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (recentSearches.isNotEmpty()) {
                            Text(
                                text = "Clear all",
                                color = Color(0xFF9A9AA0),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onClearRecentSearches() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (recentSearches.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No recent searches",
                                color = Color(0xFF8E8E93),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(recentSearches, key = { it }) { query ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onQueryChanged(query)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = Color(0xFF9A9AA0),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = query,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        onRemoveRecentSearch(query)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color(0xFF8E8E93),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // ── Active Search Results State ──

                if (isSearching) {
                    item {
                        SearchScreenSkeleton()
                    }
                } else if (!isSearching && searchResults.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tracks found for \"$searchQuery\"",
                                color = Color(0xFF9A9AA0),
                                fontSize = 15.sp
                            )
                        }
                    }
                } else if (searchResults.isNotEmpty()) {
                    // Top Song Hero Card (first result)
                    val topSong = searchResults.first()
                    item(key = "top_song_${topSong.id}") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFF121212))
                                .clickable {
                                    addRecent(topSong.title)
                                    onTrackClick(topSong, searchResults)
                                }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VybeTrackArtwork(
                                    track = topSong,
                                    modifier = Modifier.size(68.dp),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF242424))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "TOP SONG",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE5E5E5)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = topSong.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = topSong.artist,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color(0xFF9A9AA0),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3A3A3A))
                                        .bouncyClickable(
                                            pressedScale = 0.90f,
                                            onClick = {
                                                addRecent(topSong.title)
                                                onTrackClick(topSong, searchResults)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Remaining Results List
                    val remainingTracks = searchResults.drop(1)
                    itemsIndexed(remainingTracks, key = { index, track -> "search_${track.id}_$index" }) { index, track ->
                        val cardShape = getSearchTrackCardShape(index, remainingTracks.size)
                        SearchResultItem(
                            track = track,
                            isCurrent = currentTrack?.id == track.id,
                            shape = cardShape,
                            onClick = {
                                addRecent(track.title)
                                onTrackClick(track, searchResults)
                            }
                        )
                        if (index < remainingTracks.size - 1) {
                            Spacer(modifier = Modifier.height(3.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun getSearchTrackCardShape(index: Int, total: Int): Shape = when {
    total <= 1 -> RoundedCornerShape(24.dp)
    index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
    index == total - 1 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    else -> RoundedCornerShape(6.dp)
}

@Composable
private fun SearchResultItem(
    track: Track,
    isCurrent: Boolean,
    shape: Shape,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(Color(0xFF141414))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VybeTrackArtwork(
            track = track,
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCurrent) Color(track.defaultDominantColorHex) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = com.vybe.music.data.TrackMetadataIndex.formatSubtitle(track.artist, track.album),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF9E9E9E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF242424))
                .clickable { /* context menu */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = Color(0xFF9A9AA0),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
