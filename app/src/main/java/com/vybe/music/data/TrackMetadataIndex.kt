package com.vybe.music.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class TrackMetadataEntry(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val artworkUrl: String = "",
    val durationMs: Long = 0L,
    val fileName: String = "",
    val mediaStoreUri: String = ""
)

/**
 * Persistent high-fidelity metadata index storing real artist, title, album,
 * HD artworkUrl, and MediaStore export tracking.
 * Keyed by trackId, filename, and normalized (artist|title).
 */
object TrackMetadataIndex {
    private const val TAG = "TrackMetadataIndex"
    private const val INDEX_FILE = "track_metadata_index.json"
    private val entries = ConcurrentHashMap<String, TrackMetadataEntry>()
    private val gson = Gson()
    private val lock = Any()
    @Volatile private var isInitialized = false

    private fun getIndexFile(context: Context): File {
        return File(context.filesDir, INDEX_FILE)
    }

    private fun ensureLoaded(context: Context) {
        if (isInitialized) return
        synchronized(lock) {
            if (isInitialized) return
            try {
                val file = getIndexFile(context)
                if (file.exists() && file.length() > 0) {
                    val json = file.readText()
                    val type = object : TypeToken<List<TrackMetadataEntry>>() {}.type
                    val list: List<TrackMetadataEntry>? = gson.fromJson(json, type)
                    list?.forEach { entry ->
                        if (entry.trackId.isNotBlank()) {
                            entries[entry.trackId] = entry
                        }
                        if (entry.fileName.isNotBlank()) {
                            entries[entry.fileName.lowercase()] = entry
                        }
                        val key = normalizeKey(entry.title, entry.artist)
                        if (key.isNotBlank()) {
                            entries[key] = entry
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error loading metadata index: ${e.message}")
            } finally {
                isInitialized = true
            }
        }
    }

    private fun flushToDisk(context: Context) {
        try {
            val file = getIndexFile(context)
            val distinctList = entries.values.distinctBy { it.trackId }
            val json = gson.toJson(distinctList)
            file.writeText(json)
        } catch (e: Exception) {
            Log.w(TAG, "Error flushing metadata index to disk: ${e.message}")
        }
    }

    fun normalizeKey(title: String, artist: String): String {
        val cleanT = title.replace(Regex("""(?i)\(.*?\)|\\[.*?\\]"""), "")
            .replace(Regex("""[^a-zA-Z0-9\s]"""), "")
            .trim()
            .lowercase()
        val cleanA = artist.replace(Regex("""[^a-zA-Z0-9\s]"""), "")
            .trim()
            .lowercase()
        return "$cleanT|$cleanA"
    }

    fun get(context: Context, key: String): TrackMetadataEntry? {
        ensureLoaded(context)
        return entries[key] ?: entries[key.lowercase()]
    }

    fun findMatching(context: Context, fileName: String, title: String, artist: String): TrackMetadataEntry? {
        ensureLoaded(context)
        if (fileName.isNotBlank()) {
            entries[fileName.lowercase()]?.let { return it }
            val baseName = fileName.substringBeforeLast(".").lowercase()
            entries[baseName]?.let { return it }
        }
        val norm = normalizeKey(title, artist)
        entries[norm]?.let { return it }
        return null
    }

    fun save(context: Context, entry: TrackMetadataEntry) {
        ensureLoaded(context)
        if (entry.trackId.isNotBlank()) {
            entries[entry.trackId] = entry
        }
        if (entry.fileName.isNotBlank()) {
            entries[entry.fileName.lowercase()] = entry
        }
        val key = normalizeKey(entry.title, entry.artist)
        if (key.isNotBlank()) {
            entries[key] = entry
        }
        synchronized(lock) {
            flushToDisk(context)
        }
    }

    fun remove(context: Context, trackId: String) {
        ensureLoaded(context)
        val existing = entries.remove(trackId)
        if (existing != null) {
            if (existing.fileName.isNotBlank()) {
                entries.remove(existing.fileName.lowercase())
            }
            val key = normalizeKey(existing.title, existing.artist)
            entries.remove(key)
            synchronized(lock) {
                flushToDisk(context)
            }
        }
    }

    fun getAll(context: Context): List<TrackMetadataEntry> {
        ensureLoaded(context)
        return entries.values.distinctBy { it.trackId }
    }

    /**
     * Clean and format artist + album subtitle without broken artifacts like "• VYBE", "• .", or "• Unknown".
     */
    fun formatSubtitle(artist: String, album: String): String {
        val cleanArtist = artist.trim().takeIf {
            it.isNotBlank() && it != "<unknown>" && !it.equals("Unknown Artist", ignoreCase = true)
        } ?: "Unknown Artist"

        val lowerAlbum = album.trim().lowercase()
        val isInvalidAlbum = album.isBlank() ||
            lowerAlbum == "<unknown>" ||
            lowerAlbum == "unknown album" ||
            lowerAlbum == "vybe" ||
            lowerAlbum == "." ||
            lowerAlbum == "," ||
            lowerAlbum == "-" ||
            lowerAlbum == "null" ||
            lowerAlbum == "none"

        return if (!isInvalidAlbum) {
            "$cleanArtist • ${album.trim()}"
        } else {
            cleanArtist
        }
    }
}
