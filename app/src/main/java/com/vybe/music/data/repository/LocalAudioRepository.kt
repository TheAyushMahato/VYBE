package com.vybe.music.data.repository

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import com.vybe.music.data.ArtworkRepairManager
import com.vybe.music.data.TrackMetadataIndex
import com.vybe.music.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

object LocalAudioRepository {
    private const val TAG = "LocalAudioRepo"

    private val _refreshTrigger = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val refreshTrigger: SharedFlow<Unit> = _refreshTrigger.asSharedFlow()

    fun refreshLocalAudio() {
        _refreshTrigger.tryEmit(Unit)
    }

    fun hasAudioPermission(context: Context): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun getRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    suspend fun queryDeviceAudio(context: Context): List<Track> = withContext(Dispatchers.IO) {
        if (!hasAudioPermission(context)) {
            Log.w(TAG, "Audio permission not granted")
            return@withContext emptyList()
        }

        val tracks = mutableListOf<Track>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA
        )

        // Select songs longer than 15s OR any valid song in Music/VYBE/ even if duration is not yet indexed
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "(${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 15000) OR (${MediaStore.Audio.Media.RELATIVE_PATH} LIKE '%Music/VYBE%' AND ${MediaStore.Audio.Media.SIZE} > 50000) OR (${MediaStore.Audio.Media.DATA} LIKE '%/Music/VYBE/%' AND ${MediaStore.Audio.Media.SIZE} > 50000)"
        } else {
            "(${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 15000) OR (${MediaStore.Audio.Media.DATA} LIKE '%/Music/VYBE/%' AND ${MediaStore.Audio.Media.SIZE} > 50000)"
        }
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val displayNameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    var rawTitle = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Unknown"
                    var rawArtist = cursor.getString(artistCol)?.takeIf { it != "<unknown>" && it.isNotBlank() } ?: "Unknown Artist"
                    var rawAlbum = cursor.getString(albumCol)?.takeIf { it != "<unknown>" && it.isNotBlank() } ?: ""
                    var durationMs = cursor.getLong(durationCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val data = if (dataCol >= 0) cursor.getString(dataCol).orEmpty() else ""
                    val displayName = if (displayNameCol >= 0) cursor.getString(displayNameCol).orEmpty() else ""
                    val relPath = if (relPathCol >= 0) cursor.getString(relPathCol).orEmpty() else ""

                    val isVybeFolder = data.contains("/Music/VYBE/", ignoreCase = true) ||
                        relPath.contains("Music/VYBE", ignoreCase = true)

                    // Accept files in Music/VYBE/ even if duration hasn't finished indexing yet
                    if (isVybeFolder && durationMs <= 0L) {
                        durationMs = if (size > 0L) ((size * 8) / 256000L) * 1000L else 210000L
                    }

                    // Verify supported audio format extension
                    val fileNameLower = (displayName.ifBlank { data }).lowercase()
                    val isSupportedExt = fileNameLower.endsWith(".m4a") || fileNameLower.endsWith(".mp3") ||
                        fileNameLower.endsWith(".webm") || fileNameLower.endsWith(".opus") ||
                        fileNameLower.endsWith(".ogg") || fileNameLower.endsWith(".flac") ||
                        fileNameLower.endsWith(".wav") || fileNameLower.endsWith(".aac")

                    // Only skip if explicitly not a supported audio extension
                    if (fileNameLower.contains(".") && !isSupportedExt && !isVybeFolder) {
                        continue
                    }

                    // 1. High-precision metadata index lookup (preserves original YouTube / download metadata)
                    val indexedMeta = TrackMetadataIndex.findMatching(context, displayName, rawTitle, rawArtist)
                    if (indexedMeta != null) {
                        if (indexedMeta.title.isNotBlank()) rawTitle = indexedMeta.title
                        if (indexedMeta.artist.isNotBlank()) rawArtist = indexedMeta.artist
                        if (indexedMeta.album.isNotBlank()) rawAlbum = indexedMeta.album
                        if (indexedMeta.durationMs > 0 && durationMs <= 0) durationMs = indexedMeta.durationMs
                    }

                    // 2. Smart Filename Fallback: If artist is unknown or "VYBE", extract "Artist - Title" from filename
                    val isUnknownArtist = rawArtist.equals("Unknown Artist", ignoreCase = true) ||
                        rawArtist.equals("VYBE", ignoreCase = true) ||
                        rawArtist.equals("<unknown>", ignoreCase = true)
                    if (isUnknownArtist && displayName.isNotBlank()) {
                        val baseName = displayName.substringBeforeLast(".")
                            .replace(Regex("""\s*\(\d+\)$"""), "") // strip copy numbers like (1)
                        if (baseName.contains(" - ")) {
                            val parts = baseName.split(" - ", limit = 2)
                            if (parts.size == 2 && parts[0].trim().isNotBlank() && parts[1].trim().isNotBlank()) {
                                rawArtist = parts[0].trim()
                                if (rawTitle == "Unknown" || rawTitle.isBlank() || rawTitle == baseName) {
                                    rawTitle = parts[1].trim()
                                }
                            }
                        }
                    }

                    // 3. Sanitize Album: Never show "VYBE", "Unknown Album", or punctuation as album
                    val lowerAlbum = rawAlbum.trim().lowercase()
                    if (lowerAlbum == "vybe" || lowerAlbum == "<unknown>" || lowerAlbum == "unknown album" ||
                        lowerAlbum == "." || lowerAlbum == "," || lowerAlbum == "-" || lowerAlbum == "null" || lowerAlbum == "none") {
                        rawAlbum = ""
                    }

                    val contentUri = ContentUris.withAppendedId(collection, id).toString()
                    var artworkUri = indexedMeta?.artworkUrl?.takeIf { it.isNotBlank() } ?: ""

                    if (artworkUri.isBlank()) {
                        val validLocalCover = ArtworkRepairManager.getValidLocalCover(context, "local_$id")
                            ?: indexedMeta?.trackId?.let { ArtworkRepairManager.getValidLocalCover(context, it) }
                        if (validLocalCover != null) {
                            artworkUri = Uri.fromFile(validLocalCover).toString()
                        } else {
                            artworkUri = ContentUris.withAppendedId(
                                Uri.parse("content://media/external/audio/albumart"),
                                albumId
                            ).toString()
                        }
                    }

                    val track = Track(
                        id = indexedMeta?.trackId?.takeIf { it.isNotBlank() } ?: "local_$id",
                        title = rawTitle,
                        artist = rawArtist,
                        album = rawAlbum,
                        durationMs = durationMs,
                        artworkUrl = artworkUri,
                        mediaUri = contentUri,
                        isLocal = true
                    )

                    // Trigger non-blocking background artwork repair if missing
                    if (track.artworkUrl.isBlank() || track.artworkUrl.startsWith("content://media/external/audio/albumart")) {
                        ArtworkRepairManager.repairIfNeeded(context, track)
                    }

                    tracks.add(track)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying device audio: ${e.message}", e)
        }

        // Deduplicate numbered copies e.g. "Song (1).m4a" or duplicates with same artist + title
        val deduplicatedTracks = mutableListOf<Track>()
        val seenSignatures = HashSet<String>()

        for (track in tracks) {
            val cleanTitle = track.title.replace(Regex("""\s*\(\d+\)$"""), "")
                .replace(Regex("""[^a-zA-Z0-9\s]"""), "")
                .trim()
                .lowercase()
            val cleanArtist = track.artist
                .replace(Regex("""[^a-zA-Z0-9\s]"""), "")
                .trim()
                .lowercase()
            // Approximate duration bucket (within 3 seconds) to identify duplicates
            val durationBucket = (track.durationMs / 3000L)
            val signature = "$cleanArtist|$cleanTitle|$durationBucket"

            if (seenSignatures.add(signature)) {
                deduplicatedTracks.add(track)
            } else {
                Log.d(TAG, "Deduplicated duplicate copy: '${track.title}' by '${track.artist}'")
            }
        }

        Log.d(TAG, "Found ${tracks.size} tracks, deduplicated to ${deduplicatedTracks.size} unique device tracks")
        deduplicatedTracks
    }
}
