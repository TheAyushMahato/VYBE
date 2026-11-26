package com.vybe.music.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vybe.music.model.AudioQuality
import com.vybe.music.model.AudioTrackFormatInfo
import com.vybe.music.model.CustomPlaylist
import com.vybe.music.model.Track
import com.vybe.music.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Flagship Grouped Shape Helper for Action List
 */
private fun getMenuCardShape(index: Int, total: Int): Shape = when {
    total <= 1 -> RoundedCornerShape(24.dp)
    index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
    index == total - 1 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    else -> RoundedCornerShape(6.dp)
}

/**
 * Flagship Expanded Player 3-Dots Overflow Menu Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerOverflowMenuSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    track: Track,
    viewModel: PlayerViewModel,
    audioFormatInfo: AudioTrackFormatInfo = AudioTrackFormatInfo(),
    streamingQuality: AudioQuality = AudioQuality.MAX,
    onCollapsePlayer: () -> Unit = {},
    onOpenVideo: () -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
    onOpenAddToPlaylist: () -> Unit = {},
    onOpenAudioSpecs: () -> Unit = {}
) {
    if (!show) return

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val trackGenre = remember(track.id, track.artist) {
        val lower = "${track.title} ${track.artist} ${track.album}".lowercase()
        when {
            lower.contains("rock") || lower.contains("metal") -> "Rock"
            lower.contains("pop") -> "Pop"
            lower.contains("rap") || lower.contains("hip hop") -> "Hip-Hop"
            lower.contains("jazz") || lower.contains("blues") -> "Jazz"
            lower.contains("classical") || lower.contains("piano") -> "Classical"
            lower.contains("dance") || lower.contains("edm") || lower.contains("remix") -> "Electronic"
            lower.contains("indie") || lower.contains("acoustic") -> "Indie / Acoustic"
            lower.contains("lofi") || lower.contains("chill") -> "Lo-Fi / Chill"
            else -> "Pop / Indie"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0A0A0A),
        scrimColor = Color.Black.copy(alpha = 0.70f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            contentPadding = PaddingValues(top = 4.dp)
        ) {
            // ── 1. Top Hero Pill ("Start Mix with this Song") ──
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFD8D8D8))
                        .bouncyClickable(pressedScale = 0.97f) {
                            viewModel.startMixWithTrack(track)
                            onDismiss()
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 42.dp dark circle with crisp white Shuffle/Mix icon
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1A1A1A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = "Mix",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = "Start Mix with this Song",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF121212)
                        )
                    }
                }
            }

            // ── 2. 2-Column Quick Action Row ──
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Play next
                    QuickActionCard(
                        icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                        label = "Play next",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.playNext(track)
                            onDismiss()
                        }
                    )

                    // Add to playlist
                    QuickActionCard(
                        icon = Icons.Default.BookmarkAdd,
                        label = "Add to playlist",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onOpenAddToPlaylist()
                        }
                    )
                }
            }

            // ── 3. Grouped Material 3 Expressive Action List (6 Items) ──
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val totalItems = 6

                    // 1. Genre: {trackGenre}
                    ActionListRow(
                        index = 0,
                        total = totalItems,
                        icon = Icons.Default.LocalOffer,
                        label = "Genre: $trackGenre",
                        trailingChevron = true,
                        onClick = {
                            viewModel.navigateToSearch()
                            viewModel.onSearchQueryChanged(trackGenre)
                            onCollapsePlayer()
                            onDismiss()
                        }
                    )

                    // 2. Sleep timer
                    ActionListRow(
                        index = 1,
                        total = totalItems,
                        icon = Icons.Default.Bedtime,
                        label = "Sleep timer",
                        onClick = {
                            onOpenSleepTimer()
                            onDismiss()
                        }
                    )

                    // 3. Go to Artist ({artistName})
                    ActionListRow(
                        index = 2,
                        total = totalItems,
                        icon = Icons.Default.Person,
                        label = "Go to Artist (${track.artist})",
                        onClick = {
                            viewModel.selectArtist(track.artist)
                            onCollapsePlayer()
                            onDismiss()
                        }
                    )

                    // 4. Go to Album ({albumName})
                    ActionListRow(
                        index = 3,
                        total = totalItems,
                        icon = Icons.Default.Album,
                        label = "Go to Album (${if (track.album.isNotBlank()) track.album else track.title})",
                        onClick = {
                            val albumName = if (track.album.isNotBlank()) track.album else track.title
                            viewModel.navigateToSearch()
                            viewModel.onSearchQueryChanged(albumName)
                            onCollapsePlayer()
                            onDismiss()
                        }
                    )

                    // 5. Download (Hi-Res)
                    ActionListRow(
                        index = 4,
                        total = totalItems,
                        icon = Icons.Default.Download,
                        label = "Download (Hi-Res)",
                        onClick = {
                            viewModel.toggleDownloadTrack(track)
                            onDismiss()
                        }
                    )

                    // 6. Add to queue
                    ActionListRow(
                        index = 5,
                        total = totalItems,
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        label = "Add to queue",
                        onClick = {
                            viewModel.addToQueue(track)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

/**
 * 3-Column Quick Action Card
 */
@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141414))
            .bouncyClickable(pressedScale = 0.94f, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Grouped Material 3 Expressive Action Row
 */
@Composable
private fun ActionListRow(
    index: Int,
    total: Int,
    icon: ImageVector,
    label: String,
    trailingChevron: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(getMenuCardShape(index, total))
            .background(Color(0xFF141414))
            .bouncyClickable(pressedScale = 0.97f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 38.dp rounded square with 19.dp white icon
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF222222)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (trailingChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * ── Working Sub-Sheet: Multi-Select Add to Playlist Sheet ──
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    track: Track,
    viewModel: PlayerViewModel
) {
    if (!show) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val customPlaylists by viewModel.customPlaylists.collectAsState()
    val likedTracks by viewModel.likedTracks.collectAsState()

    var selectedPlaylistIds by remember { mutableStateOf(setOf<String>()) }
    var showCreateDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0A0A0A),
        scrimColor = Color.Black.copy(alpha = 0.70f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── Header: Icon + Title + Subtitle + Circular "+" ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Add to playlist",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (selectedPlaylistIds.isEmpty()) "Choose one or more playlists" else "${selectedPlaylistIds.size} selected",
                        fontSize = 13.sp,
                        color = Color(0xFF9E9E9E)
                    )
                }

                // 42.dp circular "+" button to create new playlist
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E1E))
                        .bouncyClickable(pressedScale = 0.90f) {
                            showCreateDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Playlist",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Current Song Preview Card ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF141414))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(track.artworkUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF222222))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.artist,
                            fontSize = 12.sp,
                            color = Color(0xFF9E9E9E),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Selectable Playlists List ──
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Liked Songs row
                item {
                    val isSelected = selectedPlaylistIds.contains("liked_songs")
                    PlaylistSelectRow(
                        title = "Liked Songs",
                        subtitle = "${likedTracks.size} songs",
                        icon = Icons.Default.Favorite,
                        isSelected = isSelected,
                        onToggle = {
                            selectedPlaylistIds = if (isSelected) {
                                selectedPlaylistIds - "liked_songs"
                            } else {
                                selectedPlaylistIds + "liked_songs"
                            }
                        }
                    )
                }

                // Custom User Playlists
                items(customPlaylists) { playlist ->
                    val isSelected = selectedPlaylistIds.contains(playlist.id)
                    PlaylistSelectRow(
                        title = playlist.title,
                        subtitle = "${playlist.tracks.size} songs",
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        isSelected = isSelected,
                        artworkUrl = playlist.tracks.firstOrNull()?.artworkUrl,
                        onToggle = {
                            selectedPlaylistIds = if (isSelected) {
                                selectedPlaylistIds - playlist.id
                            } else {
                                selectedPlaylistIds + playlist.id
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Bottom Bar: Cancel + Add to {n} Pill ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF222222))
                        .bouncyClickable(pressedScale = 0.95f, onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                val canAdd = selectedPlaylistIds.isNotEmpty()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(if (canAdd) Color.White else Color.White.copy(alpha = 0.35f))
                        .bouncyClickable(pressedScale = 0.95f) {
                            if (canAdd) {
                                viewModel.addTrackToPlaylists(track, selectedPlaylistIds)
                                onDismiss()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedPlaylistIds.size <= 1) "Add to playlist" else "Add to ${selectedPlaylistIds.size}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canAdd) Color.Black else Color.Black.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    // ── Create Playlist Dialog ──
    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { newName ->
                viewModel.createPlaylistWithTrack(newName, track)
                showCreateDialog = false
                onDismiss()
            }
        )
    }
}

@Composable
private fun PlaylistSelectRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    artworkUrl: String? = null,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFF424242).copy(alpha = 0.45f) else Color(0xFF141414))
            .bouncyClickable(pressedScale = 0.98f, onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 48.dp cover art
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1F1F1F)),
                contentAlignment = Alignment.Center
            ) {
                if (!artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artworkUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF9E9E9E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color.White,
                    uncheckedColor = Color(0xFF757575),
                    checkmarkColor = Color.Black
                )
            )
        }
    }
}

/**
 * Create Playlist Dark Dialog
 */
@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141414))
                .padding(22.dp)
        ) {
            Column {
                Text(
                    text = "Create playlist",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = { Text("Playlist name", color = Color(0xFF757575)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF9E9E9E))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (nameInput.isNotBlank()) Color.White else Color.White.copy(alpha = 0.35f))
                            .bouncyClickable(pressedScale = 0.95f) {
                                if (nameInput.isNotBlank()) {
                                    onCreate(nameInput.trim())
                                }
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Create & Add",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

/**
 * ── Working Sub-Sheet: Sleep Timer Dialog ──
 */
@Composable
fun SleepTimerDialog(
    remainingSeconds: Int?,
    onSetTimer: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var showCustomInput by remember { mutableStateOf(false) }
    var customMinutesText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF141414))
                .padding(22.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sleep timer",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Top-right "+" custom minutes button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222222))
                            .bouncyClickable(pressedScale = 0.90f) {
                                showCustomInput = !showCustomInput
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Custom Minutes",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (remainingSeconds != null && remainingSeconds > 0) {
                    val m = remainingSeconds / 60
                    val s = remainingSeconds % 60
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Active: %02d:%02d remaining".format(m, s),
                        fontSize = 13.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (showCustomInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customMinutesText,
                            onValueChange = { customMinutesText = it.filter { ch -> ch.isDigit() } },
                            placeholder = { Text("Minutes (e.g. 45)", color = Color(0xFF757575)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                val min = customMinutesText.toIntOrNull() ?: 0
                                if (min > 0) {
                                    onSetTimer(min)
                                    onDismiss()
                                }
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color(0xFF1E1E1E),
                                unfocusedContainerColor = Color(0xFF1E1E1E)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .bouncyClickable(pressedScale = 0.95f) {
                                    val min = customMinutesText.toIntOrNull() ?: 0
                                    if (min > 0) {
                                        onSetTimer(min)
                                        onDismiss()
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                        ) {
                            Text("Set", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Standard Options
                val options = listOf(
                    "Off" to 0,
                    "15 minutes" to 15,
                    "30 minutes" to 30,
                    "60 minutes" to 60
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    options.forEach { (label, minutes) ->
                        val isCurrent = if (minutes == 0) remainingSeconds == null else {
                            remainingSeconds != null && (remainingSeconds / 60) in (minutes - 1)..minutes
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isCurrent) Color(0xFF262626) else Color(0xFF1A1A1A))
                                .bouncyClickable(pressedScale = 0.97f) {
                                    onSetTimer(minutes)
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                                if (isCurrent) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(Color(0xFF222222))
                        .bouncyClickable(pressedScale = 0.95f, onClick = onDismiss)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

/**
 * ── Draggable Floating Circular Countdown Bubble ──
 * 58.dp dark circle with thin white border, timer icon + MM:SS countdown.
 */
@Composable
fun DraggableSleepTimerBubble(
    remainingSeconds: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(20f) }
    var offsetY by remember { mutableFloatStateOf(320f) }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(58.dp)
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(0xFF161616).copy(alpha = 0.95f))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX = (offsetX + dragAmount.x).coerceAtLeast(0f)
                    offsetY = (offsetY + dragAmount.y).coerceAtLeast(0f)
                }
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bedtime,
                contentDescription = "Sleep Timer",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeFormatted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}


