package com.vybe.music.model

import androidx.compose.runtime.Immutable

@Immutable
data class EditorialAlbum(
    val title: String,
    val subtitle: String,
    val artist: String = "",
    val year: String = "",
    val artworkUrl: String,
    val browseId: String? = null,
    val sampleTrack: Track? = null,
    val language: String = "English"
)
