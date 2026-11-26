package com.vybe.music.model

import androidx.compose.runtime.Immutable

@Immutable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artworkUrl: String,
    val mediaUri: String = "",
    val defaultDominantColorHex: Long = 0xFF1E1E1E,
    val playCount: Int = 1,
    val lastPlayedTimestamp: Long = 0L,
    val isLocal: Boolean = false
) {
    val durationFormatted: String
        get() = formatDuration(durationMs)

    companion object {
        fun formatDuration(ms: Long): String {
            val totalSeconds = (ms / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
    }
}

@Immutable
data class CustomPlaylist(
    val id: String,
    val title: String,
    val tracks: List<Track> = emptyList(),
    val createdAt: String = "Created today"
)

enum class RepeatMode {
    OFF, ALL, ONE
}

enum class NavTab {
    HOME, STATS, PLAYLISTS, SEARCH, SETTINGS
}

enum class AudioQuality(val title: String, val subtitle: String) {
    MAX("Highest / Max", "Opus 48 kHz / 320 kbps Studio (Lossless/High-Fi)"),
    HIGH("High Quality", "Opus ~160 kbps / AAC 256 kbps"),
    DATA_SAVER("Data Saver", "Opus 64 kbps / AAC 96 kbps (Minimal data)")
}

data class AudioTrackFormatInfo(
    val codec: String = "Opus",
    val bitrateKbps: Int = 320,
    val sampleRateHz: Int = 48000,
    val channelCount: Int = 2,
    val label: String = "Opus • 320 kbps • 48 kHz"
)
