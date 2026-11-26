package com.vybe.music.model

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

data class LyricsResult(
    val trackId: String,
    val lines: List<LyricLine>,
    val isSynced: Boolean,
    val source: String
)

sealed class LyricsUiState {
    object Idle : LyricsUiState()
    object Loading : LyricsUiState()
    data class Success(val result: LyricsResult) : LyricsUiState()
    object NotFound : LyricsUiState()
}
