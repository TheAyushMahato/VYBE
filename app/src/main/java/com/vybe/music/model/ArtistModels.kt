package com.vybe.music.model

data class ArtistAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val year: String,
    val artworkUrl: String,
    val isSingleOrEp: Boolean = false,
    val trackCount: Int = 1,
    val browseId: String? = null
)

data class AlbumDetail(
    val id: String,
    val title: String,
    val artist: String,
    val year: String,
    val artworkUrl: String,
    val trackCount: Int = 0,
    val tracks: List<Track> = emptyList(),
    val browseId: String? = null
)

data class ArtistDetail(
    val name: String,
    val imageUrl: String,
    val backdropUrl: String = imageUrl,
    val bio: String = "",
    val genres: List<String> = emptyList(),
    val monthlyListeners: String = "",
    val topSongs: List<Track> = emptyList(),
    val albums: List<ArtistAlbum> = emptyList(),
    val singlesAndEps: List<ArtistAlbum> = emptyList(),
    val relatedArtists: List<ArtistSummary> = emptyList(),
    val topVideoId: String = "",
    val browseId: String? = null
)

data class ArtistSummary(
    val name: String,
    val imageUrl: String,
    val genre: String = "Pop / Contemporary",
    val topTrackTitle: String = "",
    val browseId: String? = null
)

data class ArtistSeed(
    val id: String = "",
    val name: String = "",
    val imageUrl: String = ""
)

data class CuratedArtist(
    val name: String,
    val imageUrl: String,
    val languages: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val id: String = name.lowercase().replace(Regex("[^a-z0-9]"), "_")
)
