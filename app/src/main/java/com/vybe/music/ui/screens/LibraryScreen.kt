package com.vybe.music.ui.screens

import android.content.ContentUris
import android.content.Context
import com.vybe.music.data.UserSessionManager
import com.vybe.music.data.repository.LocalAudioRepository
import com.vybe.music.ui.components.MiniEqualizerBars
import androidx.compose.material.icons.rounded.AudioFile
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Share
import com.vybe.music.ui.components.VybeDropdownMenu
import com.vybe.music.ui.components.VybeDropdownMenuItem
import com.vybe.music.ui.components.VybeSortDropdownItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.vybe.music.data.ArtworkRepairManager
import com.vybe.music.ui.components.VybeTrackArtwork
import com.vybe.music.ui.components.VybeCrystalHeader
import com.vybe.music.ui.components.VybeGlassTopHeader
import com.vybe.music.ui.components.VybeTransparentGlassHeader
import com.vybe.music.data.PlaylistDownloadProgress
import com.vybe.music.model.CustomPlaylist
import com.vybe.music.model.Track
import com.vybe.music.ui.components.bouncyClickable
import com.vybe.music.ui.theme.AmoledBlack
import com.vybe.music.ui.theme.PureWhite
import kotlinx.coroutines.delay
import java.io.File

/**
 * Flagship Grouped Material 3 Expressive Segmented Card Shapes
 */
fun getGroupedCardShape(index: Int, total: Int): Shape = when {
    total <= 1 -> RoundedCornerShape(26.dp)
    index == 0 -> RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
    index == total - 1 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 26.dp, bottomEnd = 26.dp)
    else -> RoundedCornerShape(6.dp)
}

fun getTrackCardShape(index: Int, total: Int): Shape = when {
    total <= 1 -> RoundedCornerShape(24.dp)
    index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
    index == total - 1 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    else -> RoundedCornerShape(6.dp)
}

enum class PlaylistSortOption(val displayName: String) {
    CUSTOM("Custom order"),
    DATE_ADDED("Date added"),
    NAME("Name"),
    ARTIST("Artist"),
    PLAY_TIME("Play time")
}

fun queryDeviceAudio(context: Context): List<Track> {
    val trackList = mutableListOf<Track>()
    try {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Unknown Track"
                val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                val album = cursor.getString(albumCol) ?: "Unknown Album"
                val duration = cursor.getLong(durationCol)
                val albumId = cursor.getLong(albumIdCol)
                val artUri = "content://media/external/audio/albumart/$albumId"
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                trackList.add(
                    Track(
                        id = "local_$id",
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        artworkUrl = artUri,
                        mediaUri = contentUri.toString()
                    )
                )
            }
        }
    } catch (_: Exception) {}
    return trackList
}

fun generateCsv(title: String, tracks: List<Track>): String {
    return buildString {
        appendLine("Title,Artist,Album,DurationMs")
        tracks.forEach { track ->
            val safeTitle = "\"${track.title.replace("\"", "\"\"")}\""
            val safeArtist = "\"${track.artist.replace("\"", "\"\"")}\""
            val safeAlbum = "\"${track.album.replace("\"", "\"\"")}\""
            appendLine("$safeTitle,$safeArtist,$safeAlbum,${track.durationMs}")
        }
    }
}

fun generateM3u(title: String, tracks: List<Track>): String {
    return buildString {
        appendLine("#EXTM3U")
        appendLine("#PLAYLIST:$title")
        tracks.forEach { track ->
            val durationSec = (track.durationMs / 1000).coerceAtLeast(0)
            appendLine("#EXTINF:$durationSec,${track.artist} - ${track.title}")
            appendLine(track.mediaUri.ifBlank { "https://music.youtube.com/watch?v=${track.id}" })
        }
    }
}

fun exportPlaylist(
    context: Context,
    title: String,
    tracks: List<Track>,
    isM3u: Boolean,
    shareImmediately: Boolean
) {
    try {
        val extension = if (isM3u) "m3u" else "csv"
        val content = if (isM3u) generateM3u(title, tracks) else generateCsv(title, tracks)
        val cleanTitle = title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "${cleanTitle}.$extension"

        if (shareImmediately) {
            val cacheFile = File(context.cacheDir, fileName)
            cacheFile.writeText(content)
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (isM3u) "audio/x-mpegurl" else "text/csv"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "$title Playlist")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Playlist"))
        } else {
            var savedFile: File? = null
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir != null && (downloadsDir.exists() || downloadsDir.mkdirs())) {
                    val file = File(downloadsDir, fileName)
                    file.writeText(content)
                    savedFile = file
                }
            } catch (_: Exception) {}

            if (savedFile == null) {
                val extFiles = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val file = File(extFiles ?: context.filesDir, fileName)
                file.writeText(content)
                savedFile = file
            }
            Toast.makeText(context, "Saved $fileName", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun LibraryScreen(
    favoriteCount: Int,
    favoriteTrackIds: Set<String> = emptySet(),
    likedTracks: List<Track> = emptyList(),
    customPlaylists: List<CustomPlaylist> = emptyList(),
    downloadedTracks: List<Track> = emptyList(),
    downloadedTrackIds: Set<String> = emptySet(),
    downloadingTrackIds: Set<String> = emptySet(),
    queuedTrackIds: Set<String> = emptySet(),
    activePlaylistDownloads: Map<String, PlaylistDownloadProgress> = emptyMap(),
    localTracks: List<Track> = emptyList(),
    onScanLocalAudio: () -> Unit = {},
    isPlaying: Boolean = false,
    currentTrack: Track? = null,
    onCreatePlaylist: (String) -> Unit = {},
    onDeleteDownloadedTrack: (String) -> Unit = {},
    onDeletePlaylist: (String) -> Unit = {},
    onUpdatePlaylistTracks: (String, List<Track>) -> Unit = { _, _ -> },
    onUpdateLikedTracks: (List<Track>) -> Unit = {},
    onDownloadAllTracks: (List<Track>) -> Unit = {},
    onCancelPlaylistDownloads: (String, List<Track>) -> Unit = { _, _ -> },
    onDeleteDownloadedPlaylist: (List<Track>) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onTrackClick: (Track, List<Track>) -> Unit = { _, _ -> },
    onPlayTracks: (List<Track>, Boolean) -> Unit = { _, _ -> },
    onDetailStateChanged: (Boolean) -> Unit = {},
    userPhotoUrl: String? = null,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onScanLocalAudio()
            Toast.makeText(context, "Scanning local audio...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permission needed to access device audio", Toast.LENGTH_SHORT).show()
        }
    }

    val prefs = remember(UserSessionManager.currentUserId) {
        context.getSharedPreferences(
            UserSessionManager.getUserPrefsName("vybe_playlist_covers"),
            Context.MODE_PRIVATE
        )
    }
    var coverVersion by remember { mutableIntStateOf(0) }
    fun getCover(id: String): String? {
        val dummy = coverVersion
        return prefs.getString("cover_$id", null)
    }
    fun setCover(id: String, uri: String) {
        prefs.edit().putString("cover_$id", uri).apply()
        coverVersion++
    }

    var isSortReversed by remember { mutableStateOf(false) }
    var playlistSearchQuery by remember { mutableStateOf("") }

    val rawPlaylists = remember(customPlaylists, isSortReversed) {
        if (isSortReversed) customPlaylists.reversed() else customPlaylists
    }
    val displayPlaylists = remember(rawPlaylists, playlistSearchQuery) {
        if (playlistSearchQuery.isBlank()) rawPlaylists
        else rawPlaylists.filter { it.title.contains(playlistSearchQuery, ignoreCase = true) }
    }

    // Local device audio tracks
    val deviceTracks = localTracks

    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf<CustomPlaylist?>(null) }
    var renamePlaylistText by remember { mutableStateOf("") }
    var showGeneratorDialog by remember { mutableStateOf(false) }

    // Cover picker state for featured and custom playlists
    var targetCoverPlaylistId by remember { mutableStateOf<String?>(null) }
    var showCoverDialogForId by remember { mutableStateOf<Pair<String, String>?>(null) } // id to title
    var showExportDialogForPlaylist by remember { mutableStateOf<Pair<String, List<Track>>?>(null) }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && targetCoverPlaylistId != null) {
            val pid = targetCoverPlaylistId!!
            var savedUriString = uri.toString()
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            try {
                val coversDir = File(context.filesDir, "playlist_covers").apply { mkdirs() }
                val coverFile = File(coversDir, "cover_${pid}_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    coverFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (coverFile.exists() && coverFile.length() > 0) {
                    savedUriString = Uri.fromFile(coverFile).toString()
                }
            } catch (_: Exception) {}
            setCover(pid, savedUriString)
            Toast.makeText(context, "Cover updated", Toast.LENGTH_SHORT).show()
        }
    }

    // Pinned playlists set (Liked Songs and Downloads pinned by default)
    var pinnedPlaylistIds by rememberSaveable { mutableStateOf(setOf("liked_songs", "downloads")) }

    // Screen navigation state within Library
    var activePlaylistDetail by remember { mutableStateOf<CustomPlaylist?>(null) }
    var isViewingLikedSongs by remember { mutableStateOf(false) }
    var isViewingDownloads by remember { mutableStateOf(false) }
    var isViewingLocalSongs by remember { mutableStateOf(false) }

    LaunchedEffect(isViewingLikedSongs, isViewingDownloads, isViewingLocalSongs, activePlaylistDetail) {
        onDetailStateChanged(isViewingLikedSongs || isViewingDownloads || isViewingLocalSongs || activePlaylistDetail != null)
    }

    // Back handler: pops playlist detail view if open
    BackHandler(enabled = isViewingLikedSongs || isViewingDownloads || isViewingLocalSongs || activePlaylistDetail != null) {
        isViewingLikedSongs = false
        isViewingDownloads = false
        isViewingLocalSongs = false
        activePlaylistDetail = null
        onDetailStateChanged(false)
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isViewingLocalSongs) {
            PlaylistDetailScreen(
                title = "Local songs",
                playlistId = "local_songs",
                tracks = localTracks,
                subtitleDate = "On this device • ${localTracks.size} songs",
                isPlaying = isPlaying,
                currentTrack = currentTrack,
                isPinned = false,
                downloadedTrackIds = downloadedTrackIds,
                downloadingTrackIds = downloadingTrackIds,
                queuedTrackIds = queuedTrackIds,
                activePlaylistDownloads = activePlaylistDownloads,
                downloadedTracks = downloadedTracks,
                onBack = {
                    isViewingLocalSongs = false
                    onDetailStateChanged(false)
                },
                onTrackClick = onTrackClick,
                onPlayTracks = onPlayTracks,
                contentPadding = contentPadding
            )
        } else if (isViewingLikedSongs) {
            val isPinned = pinnedPlaylistIds.contains("liked_songs")
            val customCover = getCover("liked_songs")
            PlaylistDetailScreen(
                title = "Liked Songs",
                playlistId = "liked_songs",
                tracks = likedTracks,
                subtitleDate = "Updated today",
                isPlaying = isPlaying,
                currentTrack = currentTrack,
                isLikedSongs = true,
                isPinned = isPinned,
                customCoverUrl = customCover,
                onTogglePin = {
                    pinnedPlaylistIds = if (isPinned) pinnedPlaylistIds - "liked_songs" else pinnedPlaylistIds + "liked_songs"
                },
                onSaveOrder = { onUpdateLikedTracks(it) },
                onDownloadAll = { onDownloadAllTracks(it) },
                downloadedTrackIds = downloadedTrackIds,
                downloadingTrackIds = downloadingTrackIds,
                queuedTrackIds = queuedTrackIds,
                activePlaylistDownloads = activePlaylistDownloads,
                downloadedTracks = downloadedTracks,
                onCancelDownload = { onCancelPlaylistDownloads("liked_songs", likedTracks) },
                onDeleteDownloadedPlaylist = { onDeleteDownloadedPlaylist(likedTracks) },
                onChangeCoverClick = {
                    showCoverDialogForId = "liked_songs" to "Liked Songs"
                },
                onBack = {
                    isViewingLikedSongs = false
                    onDetailStateChanged(false)
                },
                onTrackClick = onTrackClick,
                onPlayTracks = onPlayTracks,
                contentPadding = contentPadding
            )
        } else if (isViewingDownloads) {
            val isPinned = pinnedPlaylistIds.contains("downloads")
            val customCover = getCover("downloads")
            PlaylistDetailScreen(
                title = "Downloads",
                playlistId = "downloads",
                tracks = downloadedTracks,
                subtitleDate = "Offline",
                isPlaying = isPlaying,
                currentTrack = currentTrack,
                isDownloads = true,
                isPinned = isPinned,
                customCoverUrl = customCover,
                onTogglePin = {
                    pinnedPlaylistIds = if (isPinned) pinnedPlaylistIds - "downloads" else pinnedPlaylistIds + "downloads"
                },
                onDeleteTrack = onDeleteDownloadedTrack,
                downloadedTrackIds = downloadedTrackIds,
                downloadingTrackIds = downloadingTrackIds,
                queuedTrackIds = queuedTrackIds,
                activePlaylistDownloads = activePlaylistDownloads,
                downloadedTracks = downloadedTracks,
                onChangeCoverClick = {
                    showCoverDialogForId = "downloads" to "Downloads"
                },
                onBack = {
                    isViewingDownloads = false
                    onDetailStateChanged(false)
                },
                onTrackClick = onTrackClick,
                onPlayTracks = onPlayTracks,
                contentPadding = contentPadding
            )
        } else if (activePlaylistDetail != null) {
            val pl = activePlaylistDetail!!
            val isPinned = pinnedPlaylistIds.contains(pl.id)
            val customCover = getCover(pl.id)
            PlaylistDetailScreen(
                title = pl.title,
                playlistId = pl.id,
                tracks = pl.tracks,
                subtitleDate = pl.createdAt,
                isPlaying = isPlaying,
                currentTrack = currentTrack,
                isPinned = isPinned,
                customCoverUrl = customCover,
                onTogglePin = {
                    pinnedPlaylistIds = if (isPinned) pinnedPlaylistIds - pl.id else pinnedPlaylistIds + pl.id
                },
                onSaveOrder = { onUpdatePlaylistTracks(pl.id, it) },
                onDownloadAll = { onDownloadAllTracks(it) },
                downloadedTrackIds = downloadedTrackIds,
                downloadingTrackIds = downloadingTrackIds,
                queuedTrackIds = queuedTrackIds,
                activePlaylistDownloads = activePlaylistDownloads,
                downloadedTracks = downloadedTracks,
                onCancelDownload = { onCancelPlaylistDownloads(pl.id, pl.tracks) },
                onDeleteDownloadedPlaylist = { onDeleteDownloadedPlaylist(pl.tracks) },
                onChangeCoverClick = {
                    showCoverDialogForId = pl.id to pl.title
                },
                onDeletePlaylist = {
                    onDeletePlaylist(pl.id)
                    activePlaylistDetail = null
                    onDetailStateChanged(false)
                },
                onBack = {
                    activePlaylistDetail = null
                    onDetailStateChanged(false)
                },
                onTrackClick = onTrackClick,
                onPlayTracks = onPlayTracks,
                contentPadding = contentPadding
            )
        } else {
            // ── Main Library Tab Screen ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AmoledBlack)
            ) {
                val totalPlaylistsCount = customPlaylists.size + 2 // Downloads, Liked Songs
                val totalTracksCount = likedTracks.size + downloadedTracks.size + customPlaylists.sumOf { it.tracks.size }
                val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = statusBarTop + 64.dp,
                    bottom = contentPadding.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // ── 1. Top Search & Generator Row ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 54dp Search Bar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF141414))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.60f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                BasicTextField(
                                    value = playlistSearchQuery,
                                    onValueChange = { playlistSearchQuery = it },
                                    textStyle = TextStyle(color = PureWhite, fontSize = 14.sp),
                                    cursorBrush = SolidColor(PureWhite),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        if (playlistSearchQuery.isEmpty()) {
                                            Text(
                                                text = "Search playlists & local audio...",
                                                color = Color(0xFF757575),
                                                fontSize = 14.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (playlistSearchQuery.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .bouncyClickable(pressedScale = 0.85f) {
                                                playlistSearchQuery = ""
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = PureWhite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 54dp Generator Button (elevated charcoal #2B2B2B with double-sparkles)
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF2B2B2B))
                                .bouncyClickable(pressedScale = 0.90f) {
                                    showGeneratorDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "AI Playlist Generator",
                                tint = PureWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // ── 2. Library Subtitle Row with [+] and [Sort] ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$totalPlaylistsCount Playlists • $totalTracksCount Tracks",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF9E9E9E)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Circular + button (38.dp, #1A1A1C) to create playlist
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1A1A1C))
                                    .bouncyClickable(pressedScale = 0.88f) {
                                        newPlaylistName = ""
                                        showCreateDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Create playlist",
                                    tint = PureWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Pill Sort button with sliders icon (#1A1A1C)
                            Box(
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF1A1A1C))
                                    .bouncyClickable(pressedScale = 0.90f) {
                                        isSortReversed = !isSortReversed
                                    }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Sort",
                                        tint = PureWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Sort",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PureWhite
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 3. Segmented Container: Row 1 (Local songs on left, Downloads on right) ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Left tile: Local songs
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                                .background(Color(0xFF2B2B2B))
                                .bouncyClickable(pressedScale = 0.96f) {
                                    if (LocalAudioRepository.hasAudioPermission(context)) {
                                        if (localTracks.isEmpty()) onScanLocalAudio()
                                        isViewingLocalSongs = true
                                    } else {
                                        audioPermissionLauncher.launch(LocalAudioRepository.getRequiredPermission())
                                    }
                                }
                                .padding(14.dp)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF383838)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AudioFile,
                                        contentDescription = "Local songs",
                                        tint = PureWhite,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Local songs",
                                    color = PureWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (localTracks.isEmpty()) "Tap to scan" else "${localTracks.size} songs",
                                    color = Color(0xFFA0A0A0),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Right tile: Downloads
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                                .background(Color(0xFF2B2B2B))
                                .bouncyClickable(pressedScale = 0.96f) {
                                    isViewingDownloads = true
                                }
                                .padding(14.dp)
                        ) {
                            Column {
                                val isAnyDownloading = downloadingTrackIds.isNotEmpty() || queuedTrackIds.isNotEmpty() || activePlaylistDownloads.isNotEmpty()
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF383838)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isAnyDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.4.dp,
                                            color = PureWhite
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.DownloadDone,
                                            contentDescription = "Downloads",
                                            tint = PureWhite,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Downloads",
                                    color = PureWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isAnyDownloading) "${downloadedTracks.size} tracks • Downloading..." else if (downloadedTracks.isEmpty()) "0 tracks" else "${downloadedTracks.size} tracks",
                                    color = Color(0xFFA0A0A0),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // ── 4. Segmented Container: Liked Songs ──
                item {
                    val isPinned = pinnedPlaylistIds.contains("liked_songs")
                    val customCover = getCover("liked_songs")
                    val firstLikedArt = customCover ?: likedTracks.firstOrNull()?.artworkUrl.orEmpty()
                    val isLikedActive = isPlaying && likedTracks.any { it.id == currentTrack?.id }
                    val cardShape = if (displayPlaylists.isEmpty()) {
                        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
                    } else {
                        RoundedCornerShape(8.dp)
                    }
                    var showLikedOptions by remember { mutableStateOf(false) }

                    val likedProgress = activePlaylistDownloads["liked_songs"]
                    val isLikedDownloading = likedProgress?.isDownloading == true || (likedTracks.isNotEmpty() && likedTracks.any { it.id in downloadingTrackIds || it.id in queuedTrackIds })
                    val downloadedLikedCount = likedProgress?.completed ?: likedTracks.count { tr -> downloadedTrackIds.contains(tr.id) || downloadedTracks.any { it.id == tr.id } }
                    val totalLikedCount = likedProgress?.total ?: likedTracks.size

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 1.5.dp)
                            .clip(cardShape)
                            .background(Color(0xFF141414))
                            .bouncyClickable(pressedScale = 0.97f) {
                                isViewingLikedSongs = true
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 58dp cover artwork
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF222226)),
                                contentAlignment = Alignment.Center
                            ) {
                                val firstLikedTrack = likedTracks.firstOrNull()
                                if (!customCover.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(customCover)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Liked Songs Artwork",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (firstLikedTrack != null) {
                                    VybeTrackArtwork(
                                        track = firstLikedTrack,
                                        modifier = Modifier.fillMaxSize(),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(Color(0xFF5A44BA), Color(0xFF8B5CF6), Color(0xFFC084FC))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = PureWhite,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Liked Songs",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isLikedActive) Color(0xFF5CCBDE) else PureWhite,
                                        letterSpacing = (-0.3).sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isLikedActive) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        MiniEqualizerBars(modifier = Modifier.size(14.dp))
                                    }
                                    if (isPinned) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = "Pinned",
                                            tint = PureWhite,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                val trackLabel = if (likedTracks.size == 1) "1 track" else "${likedTracks.size} tracks"
                                val likedSubtitle = if (isLikedDownloading) {
                                    "$trackLabel • Downloading $downloadedLikedCount/$totalLikedCount"
                                } else {
                                    "$trackLabel • Updated today"
                                }
                                Text(
                                    text = likedSubtitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (isLikedDownloading) Color(0xFF5CCBDE) else Color(0xFFA8A8A8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                // 42dp circular Play button
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF242424))
                                        .bouncyClickable(pressedScale = 0.88f) {
                                            if (likedTracks.isNotEmpty()) {
                                                onPlayTracks(likedTracks, false)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Liked Songs",
                                        tint = PureWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .bouncyClickable(pressedScale = 0.85f) {
                                                showLikedOptions = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = Color(0xFFC4C4C4),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    VybeDropdownMenu(
                                        expanded = showLikedOptions,
                                        onDismissRequest = { showLikedOptions = false }
                                    ) {
                                        VybeDropdownMenuItem(
                                            text = if (isPinned) "Unpin from Library" else "Pin to Library",
                                            leadingIcon = Icons.Default.PushPin,
                                            onClick = {
                                                pinnedPlaylistIds = if (isPinned) pinnedPlaylistIds - "liked_songs" else pinnedPlaylistIds + "liked_songs"
                                                showLikedOptions = false
                                            }
                                        )
                                        val allLikedDownloaded = likedTracks.isNotEmpty() && downloadedLikedCount == likedTracks.size

                                        when {
                                            isLikedDownloading -> {
                                                VybeDropdownMenuItem(
                                                    text = "Cancel downloads ($downloadedLikedCount/$totalLikedCount)",
                                                    leadingIcon = Icons.Rounded.Close,
                                                    onClick = {
                                                        onCancelPlaylistDownloads("liked_songs", likedTracks)
                                                        showLikedOptions = false
                                                    }
                                                )
                                            }
                                            allLikedDownloaded -> {
                                                VybeDropdownMenuItem(
                                                    text = "Delete downloaded playlist",
                                                    leadingIcon = Icons.Outlined.Delete,
                                                    isDestructive = true,
                                                    onClick = {
                                                        onDeleteDownloadedPlaylist(likedTracks)
                                                        showLikedOptions = false
                                                    }
                                                )
                                            }
                                            else -> {
                                                VybeDropdownMenuItem(
                                                    text = "Download all songs",
                                                    leadingIcon = Icons.Rounded.Download,
                                                    onClick = {
                                                        val missing = likedTracks.count { it.id !in downloadedTrackIds && downloadedTracks.none { dt -> dt.id == it.id } }
                                                        if (missing == 0 && likedTracks.isNotEmpty()) {
                                                            Toast.makeText(context, "All songs already downloaded", Toast.LENGTH_SHORT).show()
                                                        } else if (missing > 0) {
                                                            Toast.makeText(context, "Downloading $missing songs...", Toast.LENGTH_SHORT).show()
                                                        }
                                                        onDownloadAllTracks(likedTracks)
                                                        showLikedOptions = false
                                                    }
                                                )
                                            }
                                        }
                                        VybeDropdownMenuItem(
                                            text = "Change cover",
                                            leadingIcon = Icons.Rounded.Image,
                                            onClick = {
                                                showLikedOptions = false
                                                showCoverDialogForId = "liked_songs" to "Liked Songs"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 5. User-Created Custom Playlists ──
                itemsIndexed(displayPlaylists, key = { _, playlist -> playlist.id }) { plIndex, playlist ->
                    val customCover = getCover(playlist.id)
                    val plArtwork = customCover ?: playlist.tracks.firstOrNull()?.artworkUrl.orEmpty()
                    val isPinned = pinnedPlaylistIds.contains(playlist.id)
                    val isPlActive = isPlaying && playlist.tracks.any { it.id == currentTrack?.id }
                    val isLast = plIndex == displayPlaylists.lastIndex
                    val cardShape = if (isLast) {
                        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
                    } else {
                        RoundedCornerShape(8.dp)
                    }
                    var showCustomOptions by remember { mutableStateOf(false) }

                    val plProgress = activePlaylistDownloads[playlist.id]
                    val isCustomDownloading = plProgress?.isDownloading == true || (playlist.tracks.isNotEmpty() && playlist.tracks.any { it.id in downloadingTrackIds || it.id in queuedTrackIds })
                    val downloadedCustomCount = plProgress?.completed ?: playlist.tracks.count { tr -> downloadedTrackIds.contains(tr.id) || downloadedTracks.any { it.id == tr.id } }
                    val totalCustomCount = plProgress?.total ?: playlist.tracks.size

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 1.5.dp)
                            .clip(cardShape)
                            .background(Color(0xFF141414))
                            .bouncyClickable(pressedScale = 0.97f) {
                                activePlaylistDetail = playlist
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 58dp cover artwork
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF222226)),
                                contentAlignment = Alignment.Center
                            ) {
                                val firstTrack = playlist.tracks.firstOrNull()
                                if (!customCover.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(customCover)
                                            .crossfade(200)
                                            .build(),
                                        contentDescription = playlist.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (firstTrack != null) {
                                    VybeTrackArtwork(
                                        track = firstTrack,
                                        modifier = Modifier.fillMaxSize(),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = Color(0xFF757575),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = playlist.title,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isPlActive) Color(0xFF5CCBDE) else PureWhite,
                                        letterSpacing = (-0.3).sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isPlActive) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        MiniEqualizerBars(modifier = Modifier.size(14.dp))
                                    }
                                    if (isPinned) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = "Pinned",
                                            tint = PureWhite,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                val count = playlist.tracks.size
                                val countLabel = if (count == 1) "1 track" else "$count tracks"
                                val customSubtitle = if (isCustomDownloading) {
                                    "$countLabel • Downloading $downloadedCustomCount/$totalCustomCount"
                                } else {
                                    "$countLabel • ${playlist.createdAt}"
                                }
                                Text(
                                    text = customSubtitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (isCustomDownloading) Color(0xFF5CCBDE) else Color(0xFFA8A8A8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                // 42dp Play button
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF242424))
                                        .bouncyClickable(pressedScale = 0.88f) {
                                            if (playlist.tracks.isNotEmpty()) {
                                                onPlayTracks(playlist.tracks, false)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play ${playlist.title}",
                                        tint = PureWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .bouncyClickable(pressedScale = 0.85f) {
                                                showCustomOptions = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = Color(0xFFC4C4C4),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    VybeDropdownMenu(
                                        expanded = showCustomOptions,
                                        onDismissRequest = { showCustomOptions = false }
                                    ) {
                                        VybeDropdownMenuItem(
                                            text = if (isPinned) "Unpin from Library" else "Pin to Library",
                                            leadingIcon = Icons.Default.PushPin,
                                            onClick = {
                                                pinnedPlaylistIds = if (isPinned) pinnedPlaylistIds - playlist.id else pinnedPlaylistIds + playlist.id
                                                showCustomOptions = false
                                            }
                                        )
                                        val allCustomDownloaded = playlist.tracks.isNotEmpty() && downloadedCustomCount == playlist.tracks.size

                                        when {
                                            isCustomDownloading -> {
                                                VybeDropdownMenuItem(
                                                    text = "Cancel downloads ($downloadedCustomCount/$totalCustomCount)",
                                                    leadingIcon = Icons.Rounded.Close,
                                                    onClick = {
                                                        onCancelPlaylistDownloads(playlist.id, playlist.tracks)
                                                        showCustomOptions = false
                                                    }
                                                )
                                            }
                                            allCustomDownloaded -> {
                                                VybeDropdownMenuItem(
                                                    text = "Delete downloaded playlist",
                                                    leadingIcon = Icons.Outlined.Delete,
                                                    isDestructive = true,
                                                    onClick = {
                                                        onDeleteDownloadedPlaylist(playlist.tracks)
                                                        showCustomOptions = false
                                                    }
                                                )
                                            }
                                            else -> {
                                                VybeDropdownMenuItem(
                                                    text = "Download all songs",
                                                    leadingIcon = Icons.Rounded.Download,
                                                    onClick = {
                                                        val missing = playlist.tracks.count { it.id !in downloadedTrackIds && downloadedTracks.none { dt -> dt.id == it.id } }
                                                        if (missing == 0 && playlist.tracks.isNotEmpty()) {
                                                            Toast.makeText(context, "All songs already downloaded", Toast.LENGTH_SHORT).show()
                                                        } else if (missing > 0) {
                                                            Toast.makeText(context, "Downloading $missing songs...", Toast.LENGTH_SHORT).show()
                                                        }
                                                        onDownloadAllTracks(playlist.tracks)
                                                        showCustomOptions = false
                                                    }
                                                )
                                            }
                                        }
                                        VybeDropdownMenuItem(
                                            text = "Change cover",
                                            leadingIcon = Icons.Rounded.Image,
                                            onClick = {
                                                showCustomOptions = false
                                                showCoverDialogForId = playlist.id to playlist.title
                                            }
                                        )
                                        VybeDropdownMenuItem(
                                            text = "Rename",
                                            leadingIcon = Icons.Default.Edit,
                                            onClick = {
                                                showCustomOptions = false
                                                renamePlaylistText = playlist.title
                                                showRenameDialog = playlist
                                            }
                                        )
                                        VybeDropdownMenuItem(
                                            text = "Export",
                                            leadingIcon = Icons.Rounded.Share,
                                            onClick = {
                                                showCustomOptions = false
                                                showExportDialogForPlaylist = playlist.title to playlist.tracks
                                            }
                                        )
                                        VybeDropdownMenuItem(
                                            text = "Delete",
                                            leadingIcon = Icons.Default.DeleteOutline,
                                            isDestructive = true,
                                            onClick = {
                                                onDeletePlaylist(playlist.id)
                                                showCustomOptions = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(140.dp))
                }
            }

            // ── Single Source-of-Truth Crystal-Clear Zero-Blur Header ──
            VybeCrystalHeader(
                title = "Playlists",
                onSearchClick = onSearchClick,
                onProfileClick = onProfileClick,
                userPhotoUrl = userPhotoUrl,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

        // ── Create Playlist Dialog ────────────────────────────────────────────
        if (showCreateDialog) {
            Dialog(onDismissRequest = { showCreateDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF161618))
                        .padding(24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "New playlist",
                            color = PureWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        BasicTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            textStyle = TextStyle(color = PureWhite, fontSize = 16.sp),
                            cursorBrush = SolidColor(PureWhite),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF222225))
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (newPlaylistName.isEmpty()) {
                                        Text(
                                            text = "Playlist name",
                                            color = Color(0xFF757575),
                                            fontSize = 15.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cancel",
                                color = PureWhite.copy(alpha = 0.8f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showCreateDialog = false }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Create",
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (newPlaylistName.isNotBlank()) {
                                            onCreatePlaylist(newPlaylistName.trim())
                                            newPlaylistName = ""
                                            showCreateDialog = false
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Rename Playlist Dialog ────────────────────────────────────────────
        if (showRenameDialog != null) {
            val pl = showRenameDialog!!
            Dialog(onDismissRequest = { showRenameDialog = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF161618))
                        .padding(24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Rename playlist",
                            color = PureWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        BasicTextField(
                            value = renamePlaylistText,
                            onValueChange = { renamePlaylistText = it },
                            textStyle = TextStyle(color = PureWhite, fontSize = 16.sp),
                            cursorBrush = SolidColor(PureWhite),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF222225))
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (renamePlaylistText.isEmpty()) {
                                        Text(
                                            text = "Playlist name",
                                            color = Color(0xFF757575),
                                            fontSize = 15.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cancel",
                                color = PureWhite.copy(alpha = 0.8f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showRenameDialog = null }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save",
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (renamePlaylistText.isNotBlank()) {
                                            onUpdatePlaylistTracks(pl.id, pl.tracks)
                                            showRenameDialog = null
                                            Toast.makeText(context, "Renamed to '$renamePlaylistText'", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Smart Playlist Generator Dialog ───────────────────────────────────
        if (showGeneratorDialog) {
            val allCatalog = remember(likedTracks, downloadedTracks, deviceTracks) {
                (likedTracks + downloadedTracks + deviceTracks).distinctBy { it.id }
            }
            SmartPlaylistGeneratorDialog(
                allAvailableTracks = allCatalog,
                onDismiss = { showGeneratorDialog = false },
                onGenerate = { name, tracks ->
                    onCreatePlaylist(name)
                    Toast.makeText(context, "Created '$name' with ${tracks.size} tracks", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // ── Modal Cover Dialog ────────────────────────────────────────────────
        if (showCoverDialogForId != null) {
            val (pid, ptitle) = showCoverDialogForId!!
            val currentCover = getCover(pid)
            PlaylistCoverDialog(
                playlistTitle = ptitle,
                currentCoverUri = currentCover,
                onDismiss = { showCoverDialogForId = null },
                onChooseImage = {
                    targetCoverPlaylistId = pid
                    coverPickerLauncher.launch("image/*")
                }
            )
        }

        // ── Modal Export Dialog ───────────────────────────────────────────────
        if (showExportDialogForPlaylist != null) {
            val (title, tracks) = showExportDialogForPlaylist!!
            PlaylistExportDialog(
                playlistTitle = title,
                tracks = tracks,
                onDismiss = { showExportDialogForPlaylist = null },
                onExport = { isM3u, shareImmediately ->
                    exportPlaylist(context, title, tracks, isM3u, shareImmediately)
                }
            )
        }
    }
}

/**
 * ── 2. Modal Cover Dialog ──────────────────────────────────────────────────
 */
@Composable
fun PlaylistCoverDialog(
    playlistTitle: String,
    currentCoverUri: String?,
    onDismiss: () -> Unit,
    onChooseImage: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF161618))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (playlistTitle.isNotBlank()) "$playlistTitle cover" else "Playlist cover",
                    color = PureWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF222226)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!currentCoverUri.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(currentCoverUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Cover preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Automatic cover uses the first song with available artwork metadata.",
                    color = Color(0xFFA0A0A0),
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF242426))
                            .bouncyClickable(pressedScale = 0.94f) { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Done",
                            color = PureWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(50))
                            .background(PureWhite)
                            .bouncyClickable(pressedScale = 0.94f) {
                            onChooseImage()
                            onDismiss()
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Choose image",
                            color = Color(0xFF111111),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * ── 3. Modal Export Dialog (CSV & M3U) ──────────────────────────────────────
 */
@Composable
fun PlaylistExportDialog(
    playlistTitle: String,
    tracks: List<Track>,
    onDismiss: () -> Unit,
    onExport: (isM3u: Boolean, shareImmediately: Boolean) -> Unit
) {
    var selectedFormat by remember { mutableStateOf("CSV") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF161618))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Export Playlist",
                    color = PureWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(18.dp))

                // CSV Option Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selectedFormat == "CSV") Color(0xFF26262A) else Color(0xFF1E1E22))
                        .bouncyClickable(pressedScale = 0.96f) { selectedFormat = "CSV" }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CSV",
                                color = PureWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Spreadsheet-compatible",
                                color = Color(0xFF9E9E9E),
                                fontSize = 12.sp
                            )
                        }
                        RadioButton(
                            selected = selectedFormat == "CSV",
                            onClick = { selectedFormat = "CSV" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = PureWhite,
                                unselectedColor = Color(0xFF757575)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // M3U Option Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selectedFormat == "M3U") Color(0xFF26262A) else Color(0xFF1E1E22))
                        .bouncyClickable(pressedScale = 0.96f) { selectedFormat = "M3U" }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "M3U",
                                color = PureWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Media-player playlist file",
                                color = Color(0xFF9E9E9E),
                                fontSize = 12.sp
                            )
                        }
                        RadioButton(
                            selected = selectedFormat == "M3U",
                            onClick = { selectedFormat = "M3U" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = PureWhite,
                                unselectedColor = Color(0xFF757575)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF222226))
                            .bouncyClickable(pressedScale = 0.94f) { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color(0xFFB0B0B0),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF333338))
                            .bouncyClickable(pressedScale = 0.94f) {
                                onExport(selectedFormat == "M3U", false)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Save",
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(50))
                            .background(PureWhite)
                            .bouncyClickable(pressedScale = 0.94f) {
                                onExport(selectedFormat == "M3U", true)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Share",
                            color = Color(0xFF111111),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * ── 4. Smart Playlist Generator Dialog ─────────────────────────────────────
 */
@Composable
fun SmartPlaylistGeneratorDialog(
    allAvailableTracks: List<Track>,
    onDismiss: () -> Unit,
    onGenerate: (name: String, tracks: List<Track>) -> Unit
) {
    val presets = listOf(
        "Late Night Chill" to "Moody, calm atmospheric tracks",
        "High Energy Workout" to "Uptempo fast beats & gym fuel",
        "Focus & Deep Work" to "Instrumental & smooth concentration",
        "Top Chart Hits" to "Today's most played viral anthems"
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF161618))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Smart Generator",
                        color = PureWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Generate a curated playlist instantly powered by your library & listening taste.",
                    color = Color(0xFF9E9E9E),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                presets.forEach { (name, desc) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF222226))
                            .bouncyClickable(pressedScale = 0.96f) {
                                val pool = if (allAvailableTracks.isNotEmpty()) allAvailableTracks.shuffled() else emptyList()
                                val selected = pool.take(10)
                                onGenerate(name, selected)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Column {
                            Text(
                                text = name,
                                color = PureWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = desc,
                                color = Color(0xFFA0A0A0),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF2A2A2E))
                        .bouncyClickable(pressedScale = 0.94f) { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Close",
                        color = PureWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun formatPlaylistDuration(durationMs: Long): String {
    if (durationMs <= 0L) return ""
    val totalSeconds = durationMs / 1000
    val totalMinutes = totalSeconds / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
    } else {
        "${minutes.coerceAtLeast(1)} min"
    }
}

private fun formatDynamicRelativeDate(raw: String): String {
    if (raw.isBlank()) return "Updated today"
    if (raw.equals("offline", ignoreCase = true)) return "Offline"
    if (raw.startsWith("Updated", ignoreCase = true)) return raw

    raw.toLongOrNull()?.let { millis ->
        val now = System.currentTimeMillis()
        val diffMs = (now - millis).coerceAtLeast(0L)
        val diffDays = diffMs / (24 * 60 * 60 * 1000L)
        return when {
            diffDays == 0L -> "Updated today"
            diffDays == 1L -> "Updated yesterday"
            diffDays < 7L -> "Updated $diffDays days ago"
            else -> "Updated " + java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(millis))
        }
    }

    val formats = listOf("yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss", "MMM d, yyyy", "dd MMM yyyy")
    for (pattern in formats) {
        try {
            val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
            val date = sdf.parse(raw)
            if (date != null) {
                val diffDays = (System.currentTimeMillis() - date.time).coerceAtLeast(0L) / (24 * 60 * 60 * 1000L)
                return when {
                    diffDays == 0L -> "Updated today"
                    diffDays == 1L -> "Updated yesterday"
                    diffDays < 7L -> "Updated $diffDays days ago"
                    else -> "Updated " + java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(date)
                }
            }
        } catch (_: Exception) {}
    }

    return if (raw.startsWith("Created", ignoreCase = true)) {
        raw.replaceFirst("Created", "Updated", ignoreCase = true)
    } else {
        "Updated $raw"
    }
}

/**
 * ── 5. Upgraded Playlist Detail Screen ──────────────────────────────────────
 * Matches Reference Screenshot (Attachment 1) Exactly:
 * Full-bleed Artwork Hero with Fading Scrim, White Pill Play/Playing Button,
 * Circular Shuffle and Radio Buttons, Custom Order Sort Pill, Lock Reorder Handle,
 * Grouped Material 3 Expressive Track Cards, and Complete 3-Dots Options Menu.
 */
@Composable
fun PlaylistDetailScreen(
    title: String,
    playlistId: String? = null,
    tracks: List<Track>,
    subtitleDate: String,
    onBack: () -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onPlayTracks: (List<Track>, Boolean) -> Unit,
    contentPadding: PaddingValues,
    isPlaying: Boolean = false,
    currentTrack: Track? = null,
    isDownloads: Boolean = false,
    isLikedSongs: Boolean = false,
    customCoverUrl: String? = null,
    onDeleteTrack: (String) -> Unit = {},
    onDeletePlaylist: () -> Unit = {},
    onSaveOrder: ((List<Track>) -> Unit)? = null,
    onDownloadAll: ((List<Track>) -> Unit)? = null,
    downloadedTrackIds: Set<String> = emptySet(),
    downloadingTrackIds: Set<String> = emptySet(),
    queuedTrackIds: Set<String> = emptySet(),
    activePlaylistDownloads: Map<String, PlaylistDownloadProgress> = emptyMap(),
    downloadedTracks: List<Track> = emptyList(),
    onCancelDownload: (() -> Unit)? = null,
    onDeleteDownloadedPlaylist: (() -> Unit)? = null,
    onChangeCoverClick: (() -> Unit)? = null,
    isPinned: Boolean = false,
    onTogglePin: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    var localTracks by remember(tracks) { mutableStateOf(tracks) }
    var isReorderMode by remember { mutableStateOf(false) }
    var isShuffleActive by remember { mutableStateOf(false) }

    var isSearchOpen by remember { mutableStateOf(false) }
    var inPlaylistSearchQuery by remember { mutableStateOf("") }
    var currentSort by remember { mutableStateOf(PlaylistSortOption.CUSTOM) }
    var isSortAscending by remember { mutableStateOf(true) }
    var showSortDropdown by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }

    val currentProgress = playlistId?.let { activePlaylistDownloads[it] }
    val isThisPlaylistDownloading = currentProgress?.isDownloading == true ||
        (localTracks.isNotEmpty() && localTracks.any { it.id in downloadingTrackIds || it.id in queuedTrackIds })
    val downloadedCount = currentProgress?.completed ?: localTracks.count { it.id in downloadedTrackIds || downloadedTracks.any { dt -> dt.id == it.id } }
    val totalCount = currentProgress?.total ?: localTracks.size
    val allDownloaded = localTracks.isNotEmpty() && downloadedCount == localTracks.size

    val isThisPlaylistPlaying = isPlaying && currentTrack != null && localTracks.any { it.id == currentTrack.id }

    val configuration = LocalConfiguration.current
    val heroHeight = 440.dp

    val isScrolledPastHero by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 280
        }
    }

    LaunchedEffect(isSearchOpen) {
        if (isSearchOpen) {
            delay(120L)
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    BackHandler(enabled = isSearchOpen || isReorderMode) {
        if (isSearchOpen) {
            isSearchOpen = false
            inPlaylistSearchQuery = ""
            focusManager.clearFocus()
        } else if (isReorderMode) {
            isReorderMode = false
        }
    }

    val displayedTracks = remember(localTracks, currentSort, isSortAscending, inPlaylistSearchQuery) {
        val base = when (currentSort) {
            PlaylistSortOption.CUSTOM -> localTracks
            PlaylistSortOption.DATE_ADDED -> localTracks
            PlaylistSortOption.NAME -> localTracks.sortedBy { it.title.lowercase() }
            PlaylistSortOption.ARTIST -> localTracks.sortedBy { it.artist.lowercase() }
            PlaylistSortOption.PLAY_TIME -> localTracks.sortedByDescending { it.durationMs }
        }
        val ordered = if (isSortAscending) base else base.reversed()
        if (inPlaylistSearchQuery.isBlank()) {
            ordered
        } else {
            ordered.filter {
                it.title.contains(inPlaylistSearchQuery, ignoreCase = true) ||
                    it.artist.contains(inPlaylistSearchQuery, ignoreCase = true) ||
                    it.album.contains(inPlaylistSearchQuery, ignoreCase = true)
            }
        }
    }

    val firstTrack = localTracks.firstOrNull()
    val firstTrackRepaired = firstTrack?.let { ArtworkRepairManager.getRepairedUrl(it) }
    val heroPoster = customCoverUrl ?: firstTrackRepaired ?: firstTrack?.artworkUrl?.takeIf { it.isNotBlank() }.orEmpty()
    val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val layoutDirection = LocalLayoutDirection.current
    val listBottomPadding = maxOf(contentPadding.calculateBottomPadding(), navBarBottomInset + 115.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(layoutDirection),
                top = 0.dp,
                end = contentPadding.calculateEndPadding(layoutDirection),
                bottom = listBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // ── Tall Immersive Hero Header (Attachment 1) ─────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight)
                ) {
                    if (heroPoster.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(heroPoster)
                                .crossfade(true)
                                .build(),
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Smooth vertical scrim gradient fading seamlessly into pure #000000
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.00f to Color.Black.copy(alpha = 0.25f),
                                        0.45f to Color.Black.copy(alpha = 0.20f),
                                        0.78f to Color.Black.copy(alpha = 0.78f),
                                        1.00f to Color(0xFF000000)
                                    )
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.00f to Color(0xFF1E1E22),
                                        0.45f to Color(0xFF151518),
                                        0.78f to Color(0xFF0A0A0C),
                                        1.00f to Color(0xFF000000)
                                    )
                                )
                        )
                    }

                    // Content Overlay aligned to bottom
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        // Title: 36.sp Bold White
                        Text(
                            text = title,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            letterSpacing = (-0.5).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val songWord = if (localTracks.size == 1) "1 song" else "${localTracks.size} songs"
                        val totalDurationMs = remember(localTracks) { localTracks.sumOf { it.durationMs.coerceAtLeast(0L) } }
                        val formattedTotalTime = remember(totalDurationMs) { formatPlaylistDuration(totalDurationMs) }
                        val relativeUpdatedText = remember(subtitleDate) { formatDynamicRelativeDate(subtitleDate) }
                        val subtitleText = remember(songWord, formattedTotalTime, relativeUpdatedText) {
                            if (formattedTotalTime.isNotBlank()) {
                                "$songWord • $formattedTotalTime • $relativeUpdatedText"
                            } else {
                                "$songWord • $relativeUpdatedText"
                            }
                        }
                        Text(
                            text = subtitleText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.80f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Action Buttons Row (Shuffle, Big Off-White Play Pill, Radio) ──
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Circular Shuffle Button (50.dp, #282828 with 0.85 alpha)
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(if (isShuffleActive) Color.White else Color(0xFF282828).copy(alpha = 0.85f))
                                    .bouncyClickable(pressedScale = 0.88f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isShuffleActive = !isShuffleActive
                                        if (displayedTracks.isNotEmpty()) {
                                            onPlayTracks(displayedTracks, isShuffleActive)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (isShuffleActive) Color(0xFF111111) else PureWhite,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Primary Play / Playing Off-White Pill (height = 54.dp, minWidth = 148.dp, Color(0xFFDCDCDC))
                            Box(
                                modifier = Modifier
                                    .height(54.dp)
                                    .defaultMinSize(minWidth = 148.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDCDCDC))
                                    .bouncyClickable(pressedScale = 0.94f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (displayedTracks.isNotEmpty()) {
                                            onPlayTracks(displayedTracks, false)
                                        }
                                    }
                                    .padding(horizontal = 26.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isThisPlaylistPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (isThisPlaylistPlaying) "Pause" else "Play",
                                        tint = Color(0xFF111111),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isThisPlaylistPlaying) "Playing" else "Play",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111111)
                                    )
                                }
                            }

                            // Circular Radio Button (50.dp, #282828 with 0.85 alpha)
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF282828).copy(alpha = 0.85f))
                                    .bouncyClickable(pressedScale = 0.88f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (displayedTracks.isNotEmpty()) {
                                            onPlayTracks(displayedTracks.shuffled(), false)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Sensors,
                                    contentDescription = "Radio",
                                    tint = PureWhite,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Live Download Progress Pill ──
                        AnimatedVisibility(
                            visible = isThisPlaylistDownloading,
                            enter = expandVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy)) + fadeIn(),
                            exit = shrinkVertically(spring()) + fadeOut()
                        ) {
                            val progressRatio = if (totalCount > 0) (downloadedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f
                            val animatedProgress by animateFloatAsState(
                                targetValue = progressRatio,
                                animationSpec = spring(stiffness = Spring.StiffnessLow),
                                label = "playlist_download_progress"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1A1A1A))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Downloading $downloadedCount of $totalCount",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.10f))
                                                .bouncyClickable(pressedScale = 0.88f) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onCancelDownload?.invoke()
                                                }
                                                .padding(horizontal = 10.dp, vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = "Cancel",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "Cancel",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    LinearProgressIndicator(
                                        progress = { animatedProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.5.dp),
                                        color = Color.White,
                                        trackColor = Color.White.copy(alpha = 0.14f)
                                    )
                                }
                            }
                        }

                        // ── Sub-Action Row: "Custom order ↑" and "4 tracks 🔒" ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "Custom order ↑" Pill Button
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFF1E1E1E))
                                        .bouncyClickable(pressedScale = 0.92f) {
                                            showSortDropdown = true
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${currentSort.displayName} ${if (isSortAscending) "↑" else "↓"}",
                                        color = PureWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                VybeDropdownMenu(
                                    expanded = showSortDropdown,
                                    onDismissRequest = { showSortDropdown = false }
                                ) {
                                    PlaylistSortOption.values().forEach { option ->
                                        val isSelected = currentSort == option
                                        VybeSortDropdownItem(
                                            text = option.displayName,
                                            isSelected = isSelected,
                                            isAscending = isSortAscending,
                                            onClick = {
                                                if (isSelected) {
                                                    isSortAscending = !isSortAscending
                                                } else {
                                                    currentSort = option
                                                    isSortAscending = true
                                                }
                                                showSortDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Right: Track count + Lock
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .bouncyClickable(pressedScale = 0.88f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isReorderMode = !isReorderMode
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${displayedTracks.size} tracks",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF9E9E9E)
                                )
                                Icon(
                                    imageVector = if (isReorderMode) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                                    contentDescription = "Toggle Reorder",
                                    tint = if (isReorderMode) PureWhite else Color(0xFF9E9E9E),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Inline Search Pill (Animated Visibility) ──────────────────────
            item {
                AnimatedVisibility(
                    visible = isSearchOpen,
                    enter = expandVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy)) + fadeIn(),
                    exit = shrinkVertically(spring()) + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF141414))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            BasicTextField(
                                value = inPlaylistSearchQuery,
                                onValueChange = { inPlaylistSearchQuery = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                cursorBrush = SolidColor(Color.White),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                decorationBox = { innerTextField ->
                                    if (inPlaylistSearchQuery.isEmpty()) {
                                        Text(
                                            text = "Search in $title...",
                                            color = Color.White.copy(alpha = 0.40f),
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            if (inPlaylistSearchQuery.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .bouncyClickable(pressedScale = 0.85f) {
                                            inPlaylistSearchQuery = ""
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .bouncyClickable(pressedScale = 0.88f) {
                                        isSearchOpen = false
                                        inPlaylistSearchQuery = ""
                                        focusManager.clearFocus()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Close",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }
            }

            // ── Grouped Material 3 Expressive Tracklist Cards (Attachment 1) ─
            itemsIndexed(displayedTracks, key = { _, track -> track.id }) { index, track ->
                val trackCardShape = getTrackCardShape(index, displayedTracks.size)
                var showTrackMenu by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(trackCardShape)
                        .background(Color(0xFF141414))
                        .bouncyClickable(pressedScale = 0.98f) {
                            if (!isReorderMode) {
                                onTrackClick(track, displayedTracks)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 54.dp × 54.dp rounded artwork (RoundedCornerShape(12.dp))
                        VybeTrackArtwork(
                            track = track,
                            modifier = Modifier.size(54.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        // Title & Artist/Album Subtitle
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = PureWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            val subtitle = if (track.album.isNotBlank()) "${track.artist} • ${track.album}" else track.artist
                            val isTrackDownloading = track.id in downloadingTrackIds
                            val isTrackQueued = track.id in queuedTrackIds
                            val isTrackDownloaded = track.id in downloadedTrackIds || downloadedTracks.any { it.id == track.id }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isTrackDownloading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.6.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else if (isTrackQueued) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = "Queued",
                                        tint = Color(0xFF7A7A7A),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else if (isTrackDownloaded) {
                                    Icon(
                                        imageVector = Icons.Rounded.DownloadDone,
                                        contentDescription = "Downloaded",
                                        tint = Color(0xFFB5B5B5),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                }
                                Text(
                                    text = subtitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFFA8A8A8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Right side action (Reorder buttons or 3-Dots Menu)
                        if (isReorderMode) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DragHandle,
                                    contentDescription = "Drag",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(2.dp))

                                // Move Up
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF262626))
                                        .bouncyClickable(pressedScale = 0.85f) {
                                            if (index > 0) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                val mutable = localTracks.toMutableList()
                                                val moved = mutable.removeAt(index)
                                                mutable.add(index - 1, moved)
                                                localTracks = mutable
                                                onSaveOrder?.invoke(mutable)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Move Up",
                                        tint = if (index > 0) PureWhite else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Move Down
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF262626))
                                        .bouncyClickable(pressedScale = 0.85f) {
                                            if (index < localTracks.lastIndex) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                val mutable = localTracks.toMutableList()
                                                val moved = mutable.removeAt(index)
                                                mutable.add(index + 1, moved)
                                                localTracks = mutable
                                                onSaveOrder?.invoke(mutable)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Move Down",
                                        tint = if (index < localTracks.lastIndex) PureWhite else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else {
                            // Subtle 36.dp circular #262626 3-dots button on the right
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF262626))
                                        .bouncyClickable(pressedScale = 0.88f) {
                                            showTrackMenu = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                VybeDropdownMenu(
                                    expanded = showTrackMenu,
                                    onDismissRequest = { showTrackMenu = false }
                                ) {
                                    VybeDropdownMenuItem(
                                        text = "Play next",
                                        leadingIcon = Icons.Default.PlayArrow,
                                        onClick = {
                                            onTrackClick(track, listOf(track) + displayedTracks)
                                            showTrackMenu = false
                                        }
                                    )
                                    VybeDropdownMenuItem(
                                        text = "Share song",
                                        leadingIcon = Icons.Rounded.Share,
                                        onClick = {
                                            val shareIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, "Listen to ${track.title} by ${track.artist} on VYBE")
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                                            showTrackMenu = false
                                        }
                                    )
                                    if (isDownloads) {
                                        VybeDropdownMenuItem(
                                            text = "Delete download",
                                            leadingIcon = Icons.Default.DeleteOutline,
                                            isDestructive = true,
                                            onClick = {
                                                onDeleteTrack(track.id)
                                                showTrackMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // ── Top Navigation Bar: Floating Buttons over Artwork ─────────────────
        AnimatedVisibility(
            visible = !isScrolledPastHero,
            enter = fadeIn(tween(140)),
            exit = fadeOut(tween(140)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Back Button (44.dp)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.42f))
                        .bouncyClickable(pressedScale = 0.88f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isSearchOpen) {
                                isSearchOpen = false
                                inPlaylistSearchQuery = ""
                                focusManager.clearFocus()
                            } else {
                                onBack()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PureWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Top-right: Single unified horizontal dark translucent capsule pill
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.42f))
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Search Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .bouncyClickable(pressedScale = 0.88f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isSearchOpen = !isSearchOpen
                                    if (!isSearchOpen) {
                                        inPlaylistSearchQuery = ""
                                        focusManager.clearFocus()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = PureWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 3-Dots Options Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .bouncyClickable(pressedScale = 0.88f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showOptionsMenu = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = PureWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Sleek dark rounded menu (#181818, 20.dp radius)
                    VybeDropdownMenu(
                        expanded = showOptionsMenu && !isScrolledPastHero,
                        onDismissRequest = { showOptionsMenu = false }
                    ) {
                        VybeDropdownMenuItem(
                            text = "Rearrange songs",
                            leadingIcon = Icons.Default.Tune,
                            onClick = {
                                isReorderMode = true
                                showOptionsMenu = false
                            }
                        )
                        VybeDropdownMenuItem(
                            text = if (isPinned) "Unpin playlist" else "Pin playlist",
                            leadingIcon = Icons.Default.PushPin,
                            onClick = {
                                onTogglePin?.invoke()
                                showOptionsMenu = false
                            }
                        )
                        when {
                            isThisPlaylistDownloading -> {
                                VybeDropdownMenuItem(
                                    text = "Cancel playlist downloads ($downloadedCount/$totalCount)",
                                    leadingIcon = Icons.Rounded.Close,
                                    onClick = {
                                        onCancelDownload?.invoke()
                                        showOptionsMenu = false
                                    }
                                )
                            }
                            allDownloaded -> {
                                VybeDropdownMenuItem(
                                    text = "Delete downloaded playlist",
                                    leadingIcon = Icons.Outlined.Delete,
                                    isDestructive = true,
                                    onClick = {
                                        onDeleteDownloadedPlaylist?.invoke()
                                        showOptionsMenu = false
                                    }
                                )
                            }
                            else -> {
                                VybeDropdownMenuItem(
                                    text = "Download all songs",
                                    leadingIcon = Icons.Rounded.Download,
                                    onClick = {
                                        val missingCount = localTracks.count { it.id !in downloadedTrackIds && downloadedTracks.none { dt -> dt.id == it.id } }
                                        if (missingCount == 0 && localTracks.isNotEmpty()) {
                                            Toast.makeText(context, "All songs already downloaded", Toast.LENGTH_SHORT).show()
                                        } else if (missingCount > 0) {
                                            Toast.makeText(context, "Downloading $missingCount songs...", Toast.LENGTH_SHORT).show()
                                        }
                                        onDownloadAll?.invoke(localTracks)
                                        showOptionsMenu = false
                                    }
                                )
                            }
                        }
                        VybeDropdownMenuItem(
                            text = "Change cover image",
                            leadingIcon = Icons.Rounded.Image,
                            onClick = {
                                showOptionsMenu = false
                                onChangeCoverClick?.invoke()
                            }
                        )
                        VybeDropdownMenuItem(
                            text = "Export / Share",
                            leadingIcon = Icons.Rounded.Share,
                            onClick = {
                                showOptionsMenu = false
                                showExportSheet = true
                            }
                        )
                        if (!isLikedSongs && !isDownloads) {
                            VybeDropdownMenuItem(
                                text = "Delete playlist",
                                leadingIcon = Icons.Default.DeleteOutline,
                                isDestructive = true,
                                onClick = {
                                    onDeletePlaylist()
                                    showOptionsMenu = false
                                    onBack()
                                }
                            )
                        }
                    }
                }
            }
        }

        // ── Sticky Collapsed Top Bar on Scroll ────────────────────────────────
        AnimatedVisibility(
            visible = isScrolledPastHero,
            enter = fadeIn(tween(180)) + slideInVertically(tween(180)),
            exit = fadeOut(tween(140)) + slideOutVertically(tween(140)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A0A))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF222222))
                        .bouncyClickable(pressedScale = 0.88f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isSearchOpen) {
                                isSearchOpen = false
                                inPlaylistSearchQuery = ""
                                focusManager.clearFocus()
                            } else {
                                onBack()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                if (heroPoster.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(heroPoster)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1E22))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222222))
                            .bouncyClickable(pressedScale = 0.88f) {
                                isSearchOpen = !isSearchOpen
                                if (!isSearchOpen) {
                                    inPlaylistSearchQuery = ""
                                    focusManager.clearFocus()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF222222))
                                .bouncyClickable(pressedScale = 0.88f) {
                                    showOptionsMenu = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = PureWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        VybeDropdownMenu(
                            expanded = showOptionsMenu && isScrolledPastHero,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            VybeDropdownMenuItem(
                                text = "Rearrange songs",
                                leadingIcon = Icons.Default.Tune,
                                onClick = {
                                    isReorderMode = true
                                    showOptionsMenu = false
                                }
                            )
                            VybeDropdownMenuItem(
                                text = if (isPinned) "Unpin playlist" else "Pin playlist",
                                leadingIcon = Icons.Default.PushPin,
                                onClick = {
                                    onTogglePin?.invoke()
                                    showOptionsMenu = false
                                }
                            )
                            when {
                                isThisPlaylistDownloading -> {
                                    VybeDropdownMenuItem(
                                        text = "Cancel playlist downloads ($downloadedCount/$totalCount)",
                                        leadingIcon = Icons.Rounded.Close,
                                        onClick = {
                                            onCancelDownload?.invoke()
                                            showOptionsMenu = false
                                        }
                                    )
                                }
                                allDownloaded -> {
                                    VybeDropdownMenuItem(
                                        text = "Delete downloaded playlist",
                                        leadingIcon = Icons.Outlined.Delete,
                                        isDestructive = true,
                                        onClick = {
                                            onDeleteDownloadedPlaylist?.invoke()
                                            showOptionsMenu = false
                                        }
                                    )
                                }
                                else -> {
                                    VybeDropdownMenuItem(
                                        text = "Download all songs",
                                        leadingIcon = Icons.Rounded.Download,
                                        onClick = {
                                            val missingCount = localTracks.count { it.id !in downloadedTrackIds && downloadedTracks.none { dt -> dt.id == it.id } }
                                            if (missingCount == 0 && localTracks.isNotEmpty()) {
                                                Toast.makeText(context, "All songs already downloaded", Toast.LENGTH_SHORT).show()
                                            } else if (missingCount > 0) {
                                                Toast.makeText(context, "Downloading $missingCount songs...", Toast.LENGTH_SHORT).show()
                                            }
                                            onDownloadAll?.invoke(localTracks)
                                            showOptionsMenu = false
                                        }
                                    )
                                }
                            }
                            VybeDropdownMenuItem(
                                text = "Change cover image",
                                leadingIcon = Icons.Rounded.Image,
                                onClick = {
                                    showOptionsMenu = false
                                    onChangeCoverClick?.invoke()
                                }
                            )
                            VybeDropdownMenuItem(
                                text = "Export / Share",
                                leadingIcon = Icons.Rounded.Share,
                                onClick = {
                                    showOptionsMenu = false
                                    showExportSheet = true
                                }
                            )
                            if (!isLikedSongs && !isDownloads) {
                                VybeDropdownMenuItem(
                                    text = "Delete playlist",
                                    leadingIcon = Icons.Default.DeleteOutline,
                                    isDestructive = true,
                                    onClick = {
                                        onDeletePlaylist()
                                        showOptionsMenu = false
                                        onBack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Export Dialog inside PlaylistDetailScreen
        if (showExportSheet) {
            PlaylistExportDialog(
                playlistTitle = title,
                tracks = localTracks,
                onDismiss = { showExportSheet = false },
                onExport = { isM3u, shareImmediately ->
                    exportPlaylist(context, title, localTracks, isM3u, shareImmediately)
                }
            )
        }
    }
}
