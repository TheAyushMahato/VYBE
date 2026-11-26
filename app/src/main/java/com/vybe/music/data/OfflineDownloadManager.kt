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
import android.os.PowerManager
import android.provider.MediaStore
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.ui.graphics.vector.ImageVector
import com.vybe.music.data.audio.StreamCandidate
import com.vybe.music.data.audio.YouTubeAudioExtractor
import com.vybe.music.data.repository.LocalAudioRepository
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
import kotlinx.coroutines.withContext
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
    private val inFlightTrackDownloads = ConcurrentHashMap.newKeySet<String>()

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

    private val _failedTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val failedTrackIds: StateFlow<Set<String>> = _failedTrackIds.asStateFlow()

    private val _activePlaylistDownloads = MutableStateFlow<Map<String, PlaylistDownloadProgress>>(emptyMap())
    val activePlaylistDownloads: StateFlow<Map<String, PlaylistDownloadProgress>> = _activePlaylistDownloads.asStateFlow()
    val playlistProgress: StateFlow<Map<String, PlaylistDownloadProgress>> = _activePlaylistDownloads.asStateFlow()

    private var wakeLock: PowerManager.WakeLock? = null

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "vybe:playlist_download")?.apply {
                    setReferenceCounted(false)
                }
            }
            wakeLock?.acquire(35 * 60 * 1000L) // 35 min safe max timeout
            Log.d("OfflineDownloadManager", "WakeLock acquired for background playlist download")
        } catch (e: Exception) {
            Log.w("OfflineDownloadManager", "Could not acquire WakeLock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d("OfflineDownloadManager", "WakeLock released")
            }
        } catch (e: Exception) {
            Log.w("OfflineDownloadManager", "Could not release WakeLock: ${e.message}")
        }
    }

    init {
        // Startup reconciliation: verify local & MediaStore file presence, respect File Manager deletion
        scope.launch {
            delay(1000L)
            reconcileWithMediaStore()
        }
    }

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
        _failedTrackIds.value = emptySet()
        _activePlaylistDownloads.value = emptyMap()
        releaseWakeLock()
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

        _failedTrackIds.value = _failedTrackIds.value - track.id
        _queuedTrackIds.value = _queuedTrackIds.value + track.id
        notifyBanner("Downloading…", Icons.Rounded.Downloading)
        acquireWakeLock()
        scope.launch {
            try {
                downloadSemaphore.withPermit {
                    downloadSingleTrackInternal(track, maxRetries = 3)
                }
            } finally {
                if (_activePlaylistDownloads.value.isEmpty() && _downloadingTrackIds.value.isEmpty()) {
                    releaseWakeLock()
                }
            }
        }
    }

    /**
     * Bulletproof bulk download queue:
     * - Bounded semaphore of max 2 concurrent downloads.
     * - Gentle 400ms pacing delay between extractions to prevent YouTube 403/429 rate limiting.
     * - Smart per-track retry (up to 3 times) with exponential backoff (1s, 2.5s, 5s) and multi-stream fallback.
     * - Automatic second retry pass at the end of the playlist for any failed tracks.
     * - Keep-alive via partial WakeLock during the entire batch.
     * - Auto-backfill of existing cached tracks to public Music/VYBE/ storage.
     * - Live MediaStore refresh trigger to keep Local songs count in sync.
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

        // Remove from failed tracks set as we are starting download
        val missingIds = missingTracks.map { it.id }.toSet()
        _failedTrackIds.value = _failedTrackIds.value - missingIds

        playlistQueuedTrackIds[playlistId] = missingIds
        _queuedTrackIds.value = (_queuedTrackIds.value + missingIds) - _downloadingTrackIds.value
        _activePlaylistDownloads.value = _activePlaylistDownloads.value + (playlistId to PlaylistDownloadProgress(
            playlistId = playlistId,
            completed = alreadyDownloaded,
            total = totalCount,
            isDownloading = true
        ))

        notifyBanner("Downloading ${missingTracks.size} songs for offline...", Icons.Rounded.Downloading)
        acquireWakeLock()

        val job = scope.launch {
            var currentCompleted = alreadyDownloaded
            try {
                // Pass 1: Controlled concurrency with max 2 downloads & 400ms gentle pacing
                for (track in missingTracks) {
                    if (!isActive) break

                    downloadSemaphore.withPermit {
                        if (!isActive) return@withPermit
                        delay(400L) // Pacing delay to avoid YouTube rate limits (403/429)

                        val success = downloadSingleTrackInternal(track, maxRetries = 3)
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

                // Pass 2: Auto-Retry Failed Pass at End of Playlist (after 3s cooldown)
                val remainingFailed = missingTracks.filter { !isTrackDownloaded(it.id) }
                if (remainingFailed.isNotEmpty() && isActive) {
                    Log.i("OfflineDownloadManager", "Running auto-retry pass for ${remainingFailed.size} failed tracks after 3s cooldown...")
                    delay(3000L)
                    for (track in remainingFailed) {
                        if (!isActive) break

                        downloadSemaphore.withPermit {
                            if (!isActive) return@withPermit
                            delay(500L)

                            val success = downloadSingleTrackInternal(track, maxRetries = 3)
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
                }

                if (isActive) {
                    LocalAudioRepository.refreshLocalAudio()
                    val stillFailedCount = missingTracks.count { !isTrackDownloaded(it.id) }
                    if (stillFailedCount > 0) {
                        notifyBanner("Downloaded $currentCompleted/$totalCount ($stillFailedCount failed)", Icons.Rounded.ErrorOutline)
                    } else {
                        notifyBanner("Playlist download completed ($currentCompleted/$totalCount)", Icons.Rounded.DownloadDone)
                    }
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
                if (_activePlaylistDownloads.value.isEmpty()) {
                    releaseWakeLock()
                }
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

        if (_activePlaylistDownloads.value.isEmpty()) {
            releaseWakeLock()
        }

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
                    _failedTrackIds.value = _failedTrackIds.value - track.id
                } catch (e: Exception) {
                    Log.e("OfflineDownloadManager", "Error deleting track ${track.id}: ${e.message}")
                }
            }
            updateDownloadedState(storage.getDownloadedTracks())
            notifyBanner("Deleted downloaded playlist", Icons.Rounded.DeleteOutline)
            LocalAudioRepository.refreshLocalAudio()
        }
    }

    private fun normalizeForComparison(input: String): String {
        return input.replace(Regex("""(?i)\(.*?\)|\\[.*?\\]"""), "")
            .replace(Regex("""[^a-zA-Z0-9\s]"""), "")
            .trim()
            .lowercase()
    }

    private fun findExistingPublicAudio(track: Track): Uri? {
        try {
            val cleanArtist = sanitizeForFileName(track.artist, maxLength = 40)
            val cleanTitle = sanitizeForFileName(track.title, maxLength = 50)
            val baseName = "$cleanArtist - $cleanTitle"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentResolver = context.contentResolver
                val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME)
                val selection = "(${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ? OR ${MediaStore.Audio.Media.TITLE} = ?) AND ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf("$baseName%", track.title, "%${Environment.DIRECTORY_MUSIC}/VYBE%")
                contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    }
                }
            } else {
                val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "VYBE")
                if (musicDir.exists()) {
                    val matching = musicDir.listFiles { _, name ->
                        name.startsWith(baseName, ignoreCase = true)
                    }
                    val best = matching?.firstOrNull { it.length() > 50_000 }
                    if (best != null) return Uri.fromFile(best)
                }
            }
        } catch (e: Exception) {
            Log.w("OfflineDownloadManager", "Error finding existing public audio: ${e.message}")
        }
        return null
    }

    private suspend fun downloadSingleTrackInternal(track: Track, maxRetries: Int = 3): Boolean {
        val targetFile = File(downloadsDir, "${track.id}.m4a")
        if (targetFile.exists() && targetFile.length() > 50_000) {
            _failedTrackIds.value = _failedTrackIds.value - track.id
            return true
        }

        // 1. Strict anti-duplicate in-flight lock
        if (!inFlightTrackDownloads.add(track.id)) {
            Log.d("OfflineDownloadManager", "Download already in-flight for track ${track.id}")
            return false
        }

        try {
            // 2. Strict anti-duplicate registry check: check if already downloaded by normalized title & artist
            val normTitle = normalizeForComparison(track.title)
            val normArtist = normalizeForComparison(track.artist)
            val registeredMatch = _downloadedTracks.value.firstOrNull {
                normalizeForComparison(it.title) == normTitle && normalizeForComparison(it.artist) == normArtist
            }
            if (registeredMatch != null) {
                val existingFile = File(downloadsDir, "${registeredMatch.id}.m4a")
                if (existingFile.exists() && existingFile.length() > 50_000) {
                    if (!targetFile.exists()) {
                        try { existingFile.copyTo(targetFile, overwrite = true) } catch (_: Exception) {}
                    }
                    val linkedTrack = track.copy(mediaUri = Uri.fromFile(targetFile).toString())
                    storage.addDownloadedTrack(linkedTrack)
                    updateDownloadedState(storage.getDownloadedTracks())
                    _failedTrackIds.value = _failedTrackIds.value - track.id
                    return true
                }
            }

            // 3. Strict anti-duplicate MediaStore check: check if file exists in Music/VYBE/ (including numbered copies)
            val existingPublicUri = findExistingPublicAudio(track)
            if (existingPublicUri != null) {
                Log.i("OfflineDownloadManager", "Track '${track.title}' already exists in Music/VYBE/, linking...")
                if (!targetFile.exists() || targetFile.length() <= 50_000) {
                    try {
                        context.contentResolver.openInputStream(existingPublicUri)?.use { input ->
                            FileOutputStream(targetFile).use { output -> input.copyTo(output) }
                        }
                    } catch (_: Exception) {}
                }
                val linkedTrack = track.copy(
                    mediaUri = if (targetFile.exists()) Uri.fromFile(targetFile).toString() else existingPublicUri.toString(),
                    durationMs = if (track.durationMs > 45000L) track.durationMs else 210000L
                )
                storage.addDownloadedTrack(linkedTrack)
                updateDownloadedState(storage.getDownloadedTracks())
                _failedTrackIds.value = _failedTrackIds.value - track.id
                return true
            }

            _queuedTrackIds.value = _queuedTrackIds.value - track.id
            _downloadingTrackIds.value = _downloadingTrackIds.value + track.id
            val tempFile = File(downloadsDir, "${track.id}.tmp")
            var succeeded = false
            val backoffDelays = listOf(1000L, 2500L, 5000L)

        for (attempt in 1..maxRetries) {
            try {
                if (attempt > 1) {
                    val backoff = backoffDelays.getOrElse(attempt - 2) { 2500L }
                    Log.d("OfflineDownloadManager", "Backing off for ${backoff}ms before attempt $attempt for '${track.title}'")
                    delay(backoff)
                }

                // 1. Resolve candidates with priority order (audio/mp4 first, then audio/webm)
                // Force a fresh stream extraction on retry to never reuse a 403 or stale token URL
                val candidates = YouTubeAudioExtractor.extractAudioStreamCandidates(
                    track,
                    AudioQuality.MAX,
                    forceRefresh = (attempt > 1)
                )

                if (candidates.isEmpty()) {
                    Log.w("OfflineDownloadManager", "No stream candidates found on attempt $attempt for '${track.title}'")
                    continue
                }

                // Try candidates in priority order
                var downloadedMimeType = "audio/mp4"
                var candidateDownloaded = false

                for (candidate in candidates) {
                    try {
                        val request = Request.Builder()
                            .url(candidate.url)
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

                        downloadedMimeType = candidate.mimeType
                        candidateDownloaded = true
                        Log.i("OfflineDownloadManager", "Downloaded candidate [${candidate.label}] for '${track.title}'")
                        break
                    } catch (e: Exception) {
                        Log.w("OfflineDownloadManager", "Candidate ${candidate.label} failed for '${track.title}': ${e.message}")
                        if (tempFile.exists()) tempFile.delete()
                        // Continue to next candidate
                    }
                }

                if (!candidateDownloaded) {
                    continue
                }

                // 2. Cache artwork locally in filesDir/downloads/covers/
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
                _failedTrackIds.value = _failedTrackIds.value - track.id
                Log.i("OfflineDownloadManager", "Track '${track.title}' downloaded successfully to ${targetFile.absolutePath}")

                // Export to public MediaStore Music/VYBE/
                exportToPublicMusicStorage(targetFile, offlineTrack, downloadedMimeType)
                break
            } catch (e: Exception) {
                Log.w("OfflineDownloadManager", "Attempt $attempt failed for '${track.title}': ${e.message}")
                if (tempFile.exists()) tempFile.delete()
            }
        }

        if (!succeeded) {
            _failedTrackIds.value = _failedTrackIds.value + track.id
            Log.e("OfflineDownloadManager", "Failed to download track '${track.title}' after $maxRetries attempts")
        }

        return succeeded
    } finally {
        inFlightTrackDownloads.remove(track.id)
        _downloadingTrackIds.value = _downloadingTrackIds.value - track.id
        _queuedTrackIds.value = _queuedTrackIds.value - track.id
    }
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
                TrackMetadataIndex.remove(context, trackId)
                _failedTrackIds.value = _failedTrackIds.value - trackId
                updateDownloadedState(storage.getDownloadedTracks())

                notifyBanner("Removed from downloads", Icons.Rounded.DeleteOutline)
                LocalAudioRepository.refreshLocalAudio()
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

    fun backfillMissingPublicExports() {
        // No-op: automatic background/startup public backfill removed.
        // Public downloads happen only upon explicit user action.
    }

    /**
     * Reconcile downloads with physical storage and MediaStore.
     * Respects File Manager deletions: if a user deleted an exported file from File Manager,
     * the private cached copy is purged and marked as NOT downloaded.
     */
    suspend fun reconcileWithMediaStore() = withContext(Dispatchers.IO) {
        try {
            val tracks = storage.getDownloadedTracks()
            val validTracks = mutableListOf<Track>()
            var stateChanged = false

            for (track in tracks) {
                val privateFile = File(downloadsDir, "${track.id}.m4a")
                val meta = TrackMetadataIndex.get(context, track.id)
                val wasExported = meta != null && meta.fileName.isNotBlank()
                val publicFileExists = if (wasExported) {
                    checkPublicAudioFileExists(meta!!.fileName, meta.mediaStoreUri, track)
                } else true

                if (wasExported && !publicFileExists) {
                    Log.i("OfflineDownloadManager", "Track '${track.title}' deleted from File Manager; clearing private download.")
                    if (privateFile.exists()) privateFile.delete()
                    val artFile = File(downloadsDir, "${track.id}_art.jpg")
                    if (artFile.exists()) artFile.delete()
                    val coverFile = File(File(context.filesDir, "downloads/covers"), "${track.id}.jpg")
                    if (coverFile.exists()) coverFile.delete()
                    TrackMetadataIndex.remove(context, track.id)
                    storage.removeDownloadedTrack(track.id)
                    stateChanged = true
                } else if (privateFile.exists() && privateFile.length() > 50_000) {
                    validTracks.add(track)
                } else {
                    storage.removeDownloadedTrack(track.id)
                    stateChanged = true
                }
            }

            if (stateChanged) {
                updateDownloadedState(storage.getDownloadedTracks())
                LocalAudioRepository.refreshLocalAudio()
            }
        } catch (e: Exception) {
            Log.w("OfflineDownloadManager", "Error in reconcileWithMediaStore: ${e.message}")
        }
    }

    fun checkPublicAudioFileExists(fileName: String, mediaStoreUriStr: String, track: Track): Boolean {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val projection = arrayOf(MediaStore.Audio.Media._ID)
                val selection = "(${MediaStore.Audio.Media.DISPLAY_NAME} = ? OR ${MediaStore.Audio.Media.TITLE} = ?) AND ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf(fileName, track.title, "%${Environment.DIRECTORY_MUSIC}/VYBE%")
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) return true
                }
            } else {
                val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "VYBE")
                val f = File(musicDir, fileName)
                if (f.exists() && f.length() > 10_000) return true
            }
        } catch (_: Exception) {}
        return false
    }

    fun isTrackExportedToPublic(track: Track): Boolean {
        try {
            val cleanArtist = sanitizeForFileName(track.artist, maxLength = 40)
            val cleanTitle = sanitizeForFileName(track.title, maxLength = 50)
            val baseName = "$cleanArtist - $cleanTitle"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentResolver = context.contentResolver
                val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME)
                val selection = "(${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ? OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ? OR ${MediaStore.Audio.Media.TITLE} = ?) AND ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf(
                    "$baseName%",
                    "%${track.id.takeLast(4)}%",
                    track.title,
                    "%${Environment.DIRECTORY_MUSIC}/VYBE%"
                )

                contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) return true
                }
            } else {
                val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "VYBE")
                if (musicDir.exists()) {
                    val matching = musicDir.listFiles { _, name ->
                        name.startsWith(baseName, ignoreCase = true) || name.contains(track.id.takeLast(4), ignoreCase = true)
                    }
                    if (!matching.isNullOrEmpty() && matching.any { it.length() > 10_000 }) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("OfflineDownloadManager", "Error checking public export for ${track.title}: ${e.message}")
        }
        return false
    }

    fun sanitizeForFileName(input: String, maxLength: Int = 45): String {
        val clean = input
            .replace(Regex("""[/\\:*?"<>|\p{Cntrl}]"""), " ")
            .replace(Regex("""[\p{So}\p{Sk}\p{Cn}]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trim('.', '-', ' ')
        return if (clean.length > maxLength) {
            clean.take(maxLength).trimEnd('.', '-', ' ')
        } else if (clean.isBlank()) {
            "Track"
        } else {
            clean
        }
    }

    private fun exportToPublicMusicStorage(sourceFile: File, track: Track, mimeType: String = "audio/mp4") {
        try {
            val cleanArtist = sanitizeForFileName(track.artist, maxLength = 40)
            val cleanTitle = sanitizeForFileName(track.title, maxLength = 50)
            val baseName = "$cleanArtist - $cleanTitle"

            val ext = when {
                mimeType.contains("webm", ignoreCase = true) -> "webm"
                mimeType.contains("ogg", ignoreCase = true) || mimeType.contains("opus", ignoreCase = true) -> "opus"
                mimeType.contains("mp3", ignoreCase = true) -> "mp3"
                else -> "m4a"
            }
            val effectiveMime = when (ext) {
                "webm" -> "audio/webm"
                "opus" -> "audio/ogg"
                "mp3" -> "audio/mpeg"
                else -> "audio/mp4"
            }

            var fileName = "$baseName.$ext"
            if (fileName.length > 100) {
                fileName = "${baseName.take(90).trimEnd('.', '-', ' ')}.$ext"
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentResolver = context.contentResolver
                val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.TITLE)
                val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf(fileName, "%${Environment.DIRECTORY_MUSIC}/VYBE%")

                var existingId: Long? = null
                var isSameTrack = false

                contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        existingId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                        val existingTitle = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                        if (existingTitle.equals(track.title, ignoreCase = true)) {
                            isSameTrack = true
                        }
                    }
                }

                if (existingId != null && isSameTrack) {
                    Log.d("OfflineDownloadManager", "Track already exists in Music/VYBE/: $fileName")
                    val existingUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, existingId!!)
                    TrackMetadataIndex.save(
                        context,
                        TrackMetadataEntry(
                            trackId = track.id,
                            title = track.title,
                            artist = track.artist,
                            album = track.album,
                            artworkUrl = track.artworkUrl,
                            durationMs = track.durationMs,
                            fileName = fileName,
                            mediaStoreUri = existingUri.toString()
                        )
                    )
                    return
                }

                // If collision with a different track title, append short unique track ID suffix
                if (existingId != null && !isSameTrack) {
                    val suffix = "_${track.id.takeLast(4)}"
                    val maxBase = 90 - suffix.length
                    fileName = "${baseName.take(maxBase).trimEnd('.', '-', ' ')}$suffix.$ext"
                }

                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.TITLE, track.title.take(100))
                    put(MediaStore.Audio.Media.ARTIST, track.artist.take(100))
                    put(MediaStore.Audio.Media.ALBUM, track.album.take(100))
                    put(MediaStore.Audio.Media.MIME_TYPE, effectiveMime)
                    put(MediaStore.Audio.Media.IS_MUSIC, 1)
                    put(MediaStore.Audio.Media.DURATION, if (track.durationMs > 0) track.durationMs else 210000L)
                    put(MediaStore.Audio.Media.SIZE, sourceFile.length())
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/VYBE")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }

                var uri: Uri? = null
                try {
                    uri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        contentResolver.openOutputStream(uri)?.use { output ->
                            sourceFile.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                        values.clear()
                        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                        contentResolver.update(uri, values, null, null)
                        TrackMetadataIndex.save(
                            context,
                            TrackMetadataEntry(
                                trackId = track.id,
                                title = track.title,
                                artist = track.artist,
                                album = track.album,
                                artworkUrl = track.artworkUrl,
                                durationMs = track.durationMs,
                                fileName = fileName,
                                mediaStoreUri = uri.toString()
                            )
                        )
                        Log.i("OfflineDownloadManager", "Exported track to public MediaStore: $fileName")
                        LocalAudioRepository.refreshLocalAudio()
                    }
                } catch (e: Exception) {
                    Log.w("OfflineDownloadManager", "Failed writing to MediaStore ($fileName): ${e.message}")
                    if (uri != null) {
                        try {
                            contentResolver.delete(uri, null, null)
                        } catch (_: Exception) {}
                    }
                }
            } else {
                val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "VYBE")
                if (!musicDir.exists()) musicDir.mkdirs()
                var destFile = File(musicDir, fileName)
                if (destFile.exists() && destFile.length() > 10_000) {
                    // Check if collision
                    val suffix = "_${track.id.takeLast(4)}"
                    val maxBase = 90 - suffix.length
                    fileName = "${baseName.take(maxBase).trimEnd('.', '-', ' ')}$suffix.$ext"
                    destFile = File(musicDir, fileName)
                }

                if (!destFile.exists() || destFile.length() < 10_000) {
                    sourceFile.copyTo(destFile, overwrite = true)
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(destFile.absolutePath),
                        arrayOf(effectiveMime),
                        null
                    )
                    Log.i("OfflineDownloadManager", "Copied track to public storage: ${destFile.absolutePath}")
                    TrackMetadataIndex.save(
                        context,
                        TrackMetadataEntry(
                            trackId = track.id,
                            title = track.title,
                            artist = track.artist,
                            album = track.album,
                            artworkUrl = track.artworkUrl,
                            durationMs = track.durationMs,
                            fileName = fileName,
                            mediaStoreUri = Uri.fromFile(destFile).toString()
                        )
                    )
                    LocalAudioRepository.refreshLocalAudio()
                }
            }
        } catch (e: Exception) {
            Log.w("OfflineDownloadManager", "Failed exporting track to public storage: ${e.message}")
        }
    }
}
