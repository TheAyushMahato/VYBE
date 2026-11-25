package com.vybe.music.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.ui.graphics.vector.ImageVector
import com.vybe.music.data.audio.YouTubeAudioExtractor
import com.vybe.music.model.AudioQuality
import com.vybe.music.model.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class PlaylistDownloadProgress(
    val playlistId: String,
    val completed: Int,
    val total: Int,
    val isDownloading: Boolean = true
) {
    val completedCount: Int get() = completed
    val totalCount: Int get() = total
}

class OfflineDownloadManager(
    private val context: Context,
    private val storage: LocalMusicStorage,
    private var onBannerMessage: ((String, ImageVector?) -> Unit)? = null
) {
    companion object {
        @Volatile
        private var instance: OfflineDownloadManager? = null

        fun getInstance(
            context: Context,
            storage: LocalMusicStorage? = null,
            onBannerMessage: ((String, ImageVector?) -> Unit)? = null
        ): OfflineDownloadManager {
            return instance ?: synchronized(this) {
                instance ?: OfflineDownloadManager(
                    context.applicationContext,
                    storage ?: LocalMusicStorage(context.applicationContext),
                    onBannerMessage
                ).also { instance = it }
            }
        }
    }

    fun setBannerCallback(callback: ((String, ImageVector?) -> Unit)?) {
        this.onBannerMessage = callback
    }

    // Application-scoped scope: never gets cancelled when screens navigate away
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Semaphore(2) limits concurrency strictly to 2 simultaneous downloads
    private val downloadSemaphore = Semaphore(2)
    private val playlistJobs = ConcurrentHashMap<String, Job>()
    private val playlistQueuedTrackIds = ConcurrentHashMap<String, Set<String>>()

    var currentUserId: String = UserSessionManager.currentUserId
        private set

    val downloadsDir: File
        get() = UserSessionManager.getUserDownloadsDir(context, currentUserId)

    private val initialDownloadedTracks = loadValidDownloadedTracks()

    private val _downloadedTracks = MutableStateFlow<List<Track>>(initialDownloadedTracks)
    val downloadedTracks: StateFlow<List<Track>> = _downloadedTracks.asStateFlow()

    private val _downloadedTrackIds = MutableStateFlow<Set<String>>(initialDownloadedTracks.map { it.id }.toSet())
    val downloadedTrackIds: StateFlow<Set<String>> = _downloadedTrackIds.asStateFlow()

    private val _downloadingTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingTrackIds: StateFlow<Set<String>> = _downloadingTrackIds.asStateFlow()

    private val _queuedTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val queuedTrackIds: StateFlow<Set<String>> = _queuedTrackIds.asStateFlow()

    private val _activePlaylistDownloads = MutableStateFlow<Map<String, PlaylistDownloadProgress>>(emptyMap())
    val activePlaylistDownloads: StateFlow<Map<String, PlaylistDownloadProgress>> = _activePlaylistDownloads.asStateFlow()
    val playlistProgress: StateFlow<Map<String, PlaylistDownloadProgress>> = _activePlaylistDownloads.asStateFlow()

    private fun updateDownloadedState(tracks: List<Track>) {
        _downloadedTracks.value = tracks
        _downloadedTrackIds.value = tracks.map { it.id }.toSet()
    }

    private fun recomputeQueuedTrackIds(extraSingleTrackIds: Set<String> = emptySet()) {
        val allPlaylistQueued = playlistQueuedTrackIds.values.flatten().toSet()
        val activelyDownloading = _downloadingTrackIds.value
        _queuedTrackIds.value = (allPlaylistQueued + extraSingleTrackIds) - activelyDownloading
    }

    fun switchUser(newUserId: String) {
        currentUserId = newUserId
        playlistJobs.values.forEach { it.cancel() }
        playlistJobs.clear()
        playlistQueuedTrackIds.clear()
        _downloadingTrackIds.value = emptySet()
        _queuedTrackIds.value = emptySet()
        _activePlaylistDownloads.value = emptyMap()
        updateDownloadedState(loadValidDownloadedTracks())
    }

    private fun loadValidDownloadedTracks(): List<Track> {
        val tracks = storage.getDownloadedTracks()
        val valid = tracks.filter { track ->
            val file = File(downloadsDir, "${track.id}.m4a")
            file.exists() && file.length() > 50_000
        }
        if (valid.size != tracks.size) {
            storage.saveDownloadedTracks(valid)
        }
        return valid
    }

    fun isTrackDownloaded(trackId: String): Boolean {
        val f = File(downloadsDir, "${trackId}.m4a")
        return f.exists() && f.length() > 50_000
    }

    fun isTrackDownloading(trackId: String): Boolean {
        return _downloadingTrackIds.value.contains(trackId)
    }

    fun isPlaylistDownloading(playlistId: String, tracks: List<Track>): Boolean {
        if (_activePlaylistDownloads.value.containsKey(playlistId)) return true
        return tracks.isNotEmpty() && tracks.any { isTrackDownloading(it.id) || _queuedTrackIds.value.contains(it.id) }
    }

    fun toggleDownload(track: Track) {
        if (isTrackDownloaded(track.id)) {
            deleteDownloadedTrack(track.id)
        } else {
            downloadTrack(track)
        }
    }

    fun getLocalFileUri(trackId: String): String? {
        val f = File(downloadsDir, "${trackId}.m4a")
        return if (f.exists() && f.length() > 50_000) Uri.fromFile(f).toString() else null
    }

    fun downloadTrack(track: Track) {
        if (isTrackDownloaded(track.id)) {
            notifyBanner("Already downloaded", Icons.Rounded.DownloadDone)
            return
        }
        if (isTrackDownloading(track.id) || _queuedTrackIds.value.contains(track.id)) {
            notifyBanner("Downloading…", Icons.Rounded.Downloading)
            return
        }

        _queuedTrackIds.value = _queuedTrackIds.value + track.id
        notifyBanner("Downloading…", Icons.Rounded.Downloading)
        scope.launch {
            downloadSemaphore.withPermit {
                downloadSingleTrackInternal(track, maxRetries = 2)
            }
        }
    }

    /**
     * Bulletproof bulk download queue:
     * - Immediately updates activePlaylistDownloads and queuedTrackIds (< 50ms reactive UI feedback).
     * - Staggers resolutions by 300ms to avoid InnerTube / YouTube 429 rate-limiting.
     * - Limits concurrency to 2 simultaneous jobs via Semaphore(2).
     * - Wraps each track in try/catch with up to 2 retries; one failure NEVER cancels the queue.
     * - Atomic file write via .tmp -> .m4a.
     * - Automatically caches square cover art to filesDir/downloads/covers/${track.id}.jpg.
     * - Removes playlistId from activePlaylistDownloads when completed or cancelled.
     */
    fun downloadPlaylist(playlistId: String, tracks: List<Track>) {
        if (tracks.isEmpty()) return

        playlistJobs[playlistId]?.cancel()

        val missingTracks = tracks.filter { !isTrackDownloaded(it.id) }
        val totalCount = tracks.size
        val alreadyDownloaded = totalCount - missingTracks.size

        if (missingTracks.isEmpty()) {
            notifyBanner("All songs already downloaded", Icons.Rounded.DownloadDone)
            _activePlaylistDownloads.value = _activePlaylistDownloads.value - playlistId
            playlistQueuedTrackIds.remove(playlistId)
            recomputeQueuedTrackIds()
            return
        }

        val missingIds = missingTracks.map { it.id }.toSet()
        playlistQueuedTrackIds[playlistId] = missingIds
        _queuedTrackIds.value = (_queuedTrackIds.value + missingIds) - _downloadingTrackIds.value
        _activePlaylistDownloads.value = _activePlaylistDownloads.value + (playlistId to PlaylistDownloadProgress(
            playlistId = playlistId,
            completed = alreadyDownloaded,
            total = totalCount,
            isDownloading = true
        ))

        notifyBanner("Downloading ${missingTracks.size} songs for offline...", Icons.Rounded.Downloading)

        val job = scope.launch {
            var currentCompleted = alreadyDownloaded
            try {
                for (track in missingTracks) {
                    if (!isActive) break

                    downloadSemaphore.withPermit {
                        if (!isActive) return@withPermit
                        delay(300L) // Stagger between stream resolutions to prevent rate-limiting

                        val success = downloadSingleTrackInternal(track, maxRetries = 2)
                        playlistQueuedTrackIds[playlistId] = (playlistQueuedTrackIds[playlistId] ?: emptySet()) - track.id
                        recomputeQueuedTrackIds()
                        if (success) {
                            currentCompleted++
                            if (_activePlaylistDownloads.value.containsKey(playlistId)) {
                                _activePlaylistDownloads.value = _activePlaylistDownloads.value + (playlistId to PlaylistDownloadProgress(
                                    playlistId = playlistId,
                                    completed = currentCompleted,
                                    total = totalCount,
                                    isDownloading = true
                                ))
                            }
                        }
                    }
                }

                if (isActive) {
                    notifyBanner("Playlist download completed ($currentCompleted/$totalCount)", Icons.Rounded.DownloadDone)
                }
            } catch (e: CancellationException) {
                Log.i("OfflineDownloadManager", "Playlist $playlistId download cancelled")
            } catch (e: Exception) {
                Log.e("OfflineDownloadManager", "Error downloading playlist $playlistId: ${e.message}", e)
            } finally {
                _activePlaylistDownloads.value = _activePlaylistDownloads.value - playlistId
                playlistQueuedTrackIds.remove(playlistId)
                recomputeQueuedTrackIds()
                playlistJobs.remove(playlistId)
            }
        }
        playlistJobs[playlistId] = job
    }

    fun cancelPlaylistDownloads(playlistId: String, tracks: List<Track>) {
        playlistJobs[playlistId]?.cancel()
        playlistJobs.remove(playlistId)
        playlistQueuedTrackIds.remove(playlistId)

        val trackIds = tracks.map { it.id }.toSet()
        _downloadingTrackIds.value = _downloadingTrackIds.value - trackIds
        _queuedTrackIds.value = (_queuedTrackIds.value - trackIds)
        recomputeQueuedTrackIds()
        _activePlaylistDownloads.value = _activePlaylistDownloads.value - playlistId

        scope.launch {
            for (track in tracks) {
                val tmp = File(downloadsDir, "${track.id}.tmp")
                if (tmp.exists()) tmp.delete()
            }
        }

        notifyBanner("Cancelled playlist downloads", Icons.Rounded.Close)
    }

    fun deleteDownloadedPlaylist(tracks: List<Track>) {
        scope.launch {
            for (track in tracks) {
                try {
                    val f = File(downloadsDir, "${track.id}.m4a")
                    if (f.exists()) f.delete()
                    val artFile = File(downloadsDir, "${track.id}_art.jpg")
                    if (artFile.exists()) artFile.delete()
                    val coversArt = File(File(context.filesDir, "downloads/covers"), "${track.id}.jpg")
                    if (coversArt.exists()) coversArt.delete()
                    storage.removeDownloadedTrack(track.id)
                } catch (e: Exception) {
                    Log.e("OfflineDownloadManager", "Error deleting track ${track.id}: ${e.message}")
                }
            }
            updateDownloadedState(storage.getDownloadedTracks())
            notifyBanner("Deleted downloaded playlist", Icons.Rounded.DeleteOutline)
        }
    }

    private suspend fun downloadSingleTrackInternal(track: Track, maxRetries: Int = 2): Boolean {
        val targetFile = File(downloadsDir, "${track.id}.m4a")
        if (targetFile.exists() && targetFile.length() > 50_000) {
            return true
        }

        _queuedTrackIds.value = _queuedTrackIds.value - track.id
        _downloadingTrackIds.value = _downloadingTrackIds.value + track.id
        val tempFile = File(downloadsDir, "${track.id}.tmp")
        var succeeded = false

        for (attempt in 1..maxRetries) {
            try {
                // 1. Resolve direct audio stream URL
                var resolvedStreamUrl = if (track.mediaUri.isNotBlank() &&
                    !YouTubeAudioExtractor.isPreviewUrl(track.mediaUri) &&
                    !track.mediaUri.startsWith("vybe://") &&
                    !track.mediaUri.startsWith("file://")
                ) {
                    track.mediaUri
                } else {
                    YouTubeAudioExtractor.extractAudioStream(track, AudioQuality.MAX)
                }

                if (resolvedStreamUrl.isNullOrBlank() || YouTubeAudioExtractor.isPreviewUrl(resolvedStreamUrl)) {
                    if (attempt < maxRetries) {
                        delay(600L)
                        continue
                    } else {
                        throw IllegalStateException("Audio stream could not be resolved for '${track.title}'")
                    }
                }

                // 2. Download audio stream
                val request = Request.Builder()
                    .url(resolvedStreamUrl)
                    .header("User-Agent", YouTubeAudioExtractor.YOUTUBE_STREAM_USER_AGENT)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
                    val body = response.body ?: throw IllegalStateException("Empty body")

                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                if (tempFile.length() < 10_000) {
                    tempFile.delete()
                    throw IllegalStateException("File size too small: ${tempFile.length()} bytes")
                }

                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }

                // 3. Simultaneously download and cache artwork locally in filesDir/downloads/covers/
                var localArtUri = track.artworkUrl
                val validExistingCover = ArtworkRepairManager.getValidLocalCover(context, track.id)
                if (validExistingCover != null) {
                    localArtUri = Uri.fromFile(validExistingCover).toString()
                } else {
                    val coversDir = File(context.filesDir, "downloads/covers").apply { mkdirs() }
                    val artFile = File(coversDir, "${track.id}.jpg")
                    val artUrlToDownload = if (track.artworkUrl.isNotBlank() && !track.artworkUrl.startsWith("file://")) {
                        track.artworkUrl
                    } else {
                        TrackArtworkRepairer.resolveRepairedArtwork(context, track)
                    }

                    if (!artUrlToDownload.isNullOrBlank() && !artUrlToDownload.startsWith("file://")) {
                        try {
                            val artRequest = Request.Builder().url(artUrlToDownload).build()
                            httpClient.newCall(artRequest).execute().use { resp ->
                                if (resp.isSuccessful && resp.body != null) {
                                    resp.body!!.byteStream().use { input ->
                                        FileOutputStream(artFile).use { out -> input.copyTo(out) }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("OfflineDownloadManager", "Failed caching offline artwork: ${e.message}")
                        }
                    }

                    val validDownloadedCover = ArtworkRepairManager.getValidLocalCover(context, track.id)
                    if (validDownloadedCover != null) {
                        localArtUri = Uri.fromFile(validDownloadedCover).toString()
                    }
                }

                val properDuration = if (track.durationMs > 45000L) track.durationMs else 210000L
                val offlineTrack = track.copy(
                    mediaUri = Uri.fromFile(targetFile).toString(),
                    artworkUrl = localArtUri,
                    durationMs = properDuration
                )

                storage.addDownloadedTrack(offlineTrack)
                updateDownloadedState(storage.getDownloadedTracks())
                succeeded = true
                Log.i("OfflineDownloadManager", "Track '${track.title}' downloaded successfully to ${targetFile.absolutePath}")

                // Automatically export high-quality audio file to public storage Music/VYBE/
                exportToPublicMusicStorage(targetFile, offlineTrack)
                break
            } catch (e: Exception) {
                Log.w("OfflineDownloadManager", "Attempt $attempt failed for '${track.title}': ${e.message}")
                if (tempFile.exists()) tempFile.delete()
                if (attempt < maxRetries) {
                    delay(600L)
                }
            }
        }

        _downloadingTrackIds.value = _downloadingTrackIds.value - track.id
        _queuedTrackIds.value = _queuedTrackIds.value - track.id
        return succeeded
    }

    fun deleteDownloadedTrack(trackId: String) {
        scope.launch {
            try {
                val f = File(downloadsDir, "${trackId}.m4a")
                if (f.exists()) f.delete()
                val artFile = File(downloadsDir, "${trackId}_art.jpg")
                if (artFile.exists()) artFile.delete()
                val coversArt = File(File(context.filesDir, "downloads/covers"), "${trackId}.jpg")
                if (coversArt.exists()) coversArt.delete()

                storage.removeDownloadedTrack(trackId)
                updateDownloadedState(storage.getDownloadedTracks())

                notifyBanner("Removed from downloads", Icons.Rounded.DeleteOutline)
            } catch (e: Exception) {
                Log.e("OfflineDownloadManager", "Failed to delete track $trackId: ${e.message}", e)
            }
        }
    }

    private fun notifyBanner(message: String, icon: ImageVector? = null) {
        mainHandler.post {
            onBannerMessage?.invoke(message, icon)
        }
    }

    private fun exportToPublicMusicStorage(sourceFile: File, track: Track) {
        try {
            val sanitizedArtist = track.artist.replace(Regex("""[/\\:*?"<>|]"""), "_").trim().ifBlank { "Unknown Artist" }
            val sanitizedTitle = track.title.replace(Regex("""[/\\:*?"<>|]"""), "_").trim().ifBlank { "Track" }
            val fileName = "$sanitizedArtist - $sanitizedTitle.m4a"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentResolver = context.contentResolver
                val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME)
                val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf(fileName, "%${Environment.DIRECTORY_MUSIC}/VYBE%")

                val existingUri = contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    } else null
                }

                if (existingUri == null) {
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Audio.Media.TITLE, track.title)
                        put(MediaStore.Audio.Media.ARTIST, track.artist)
                        put(MediaStore.Audio.Media.ALBUM, track.album)
                        put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                        put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/VYBE")
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    }

                    val uri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        contentResolver.openOutputStream(uri)?.use { output ->
                            sourceFile.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                        values.clear()
                        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                        contentResolver.update(uri, values, null, null)
                        Log.i("OfflineDownloadManager", "Exported track to public MediaStore: $fileName")
                    }
                } else {
                    Log.d("OfflineDownloadManager", "Track already exists in Music/VYBE/: $fileName")
                }
            } else {
                val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "VYBE")
                if (!musicDir.exists()) musicDir.mkdirs()
                val destFile = File(musicDir, fileName)
                if (!destFile.exists() || destFile.length() < 10_000) {
                    sourceFile.copyTo(destFile, overwrite = true)
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(destFile.absolutePath),
                        arrayOf("audio/mp4"),
                        null
                    )
                    Log.i("OfflineDownloadManager", "Copied track to public storage: ${destFile.absolutePath}")
                }
            }
        } catch (e: Exception) {
            Log.w("OfflineDownloadManager", "Failed exporting track to public storage: ${e.message}")
        }
    }
}
