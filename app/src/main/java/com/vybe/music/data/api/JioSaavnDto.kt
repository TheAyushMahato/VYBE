package com.vybe.music.data.api

import com.google.gson.annotations.SerializedName
import com.vybe.music.model.Track

// ── Top-level iTunes Search Response & DTOs ──────────────────────────────────
data class ITunesSearchResponse(
    @SerializedName("resultCount") val resultCount: Int,
    @SerializedName("results")     val results: List<ITunesSongDto>?
)

data class ITunesSongDto(
    @SerializedName("trackId")         val trackId: Long?,
    @SerializedName("trackName")       val trackName: String?,
    @SerializedName("artistName")      val artistName: String?,
    @SerializedName("collectionName")  val collectionName: String?,
    @SerializedName("artworkUrl100")   val artworkUrl100: String?,
    @SerializedName("previewUrl")      val previewUrl: String?,
    @SerializedName("trackTimeMillis") val trackTimeMillis: Long?
) {
    val ultraHdPosterUrl: String?
        get() = artworkUrl100
            ?.replace("100x100bb.jpg", "1200x1200bb.jpg")
            ?.replace("100x100bb.png", "1200x1200bb.png")
            ?.replace("100x100bb", "1200x1200bb")

    fun toTrack(): Track? {
        val title = trackName?.takeIf { it.isNotBlank() } ?: return null
        val artist = artistName?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
        val album = collectionName ?: "Single"
        val poster = ultraHdPosterUrl ?: artworkUrl100.orEmpty()
        // Ensure duration is full track length (never preview clip duration)
        val duration = if (trackTimeMillis != null && trackTimeMillis > 45000L) trackTimeMillis else 210000L
        // STRICTLY NEVER assign 30-second previewUrl as mediaUri!
        // Full tracks must ALWAYS be resolved from YouTube / Studio CDN.
        val audioUrl = ""

        val idStr = trackId?.toString() ?: "itunes_${title.hashCode()}_${artist.hashCode()}"
        val fallbackColor = when ((idStr.hashCode() % 6 + 6) % 6) {
            0 -> 0xFFE91E63L
            1 -> 0xFFFF5722L
            2 -> 0xFF00BCD4L
            3 -> 0xFFD32F2FL
            4 -> 0xFF9C27B0L
            else -> 0xFF1976D2L
        }.toLong()

        return Track(
            id = idStr,
            title = title,
            artist = artist,
            album = album,
            durationMs = duration,
            artworkUrl = poster,
            mediaUri = audioUrl,
            defaultDominantColorHex = fallbackColor
        )
    }
}

// ── Direct JioSaavn API Response (for DES Stream Decryption) ──────────────────
data class JioSaavnDirectSearchResponse(
    @SerializedName("total")   val total: Int?,
    @SerializedName("start")   val start: Int?,
    @SerializedName("results") val results: List<JioSaavnDirectSongDto>?
)

data class JioSaavnDirectSongDto(
    @SerializedName("id")                  val id: String?,
    @SerializedName("song")                val song: String?,
    @SerializedName("primary_artists")     val primaryArtists: String?,
    @SerializedName("singers")             val singers: String?,
    @SerializedName("album")               val album: String?,
    @SerializedName("year")                val year: String?,
    @SerializedName("image")               val image: String?,
    @SerializedName("duration")            val duration: String?,
    @SerializedName("encrypted_media_url") val encryptedMediaUrl: String?,
    @SerializedName("media_preview_url")   val mediaPreviewUrl: String?
)

// ── Legacy JioSaavn Mirror DTOs (Preserved for compatibility) ─────────────────
data class JioSaavnSearchResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data")    val data: JioSaavnSearchData?
)

data class JioSaavnSearchData(
    @SerializedName("total")   val total: Int,
    @SerializedName("start")   val start: Int,
    @SerializedName("results") val results: List<JioSaavnSongDto>?
)

data class JioSaavnSongDto(
    @SerializedName("id")          val id: String,
    @SerializedName("name")        val name: String?,
    @SerializedName("duration")    val duration: Long?,
    @SerializedName("album")       val album: JioSaavnAlbumDto?,
    @SerializedName("artists")     val artists: JioSaavnArtistsDto?,
    @SerializedName("image")       val image: List<JioSaavnQualityUrl>?,
    @SerializedName("downloadUrl") val downloadUrl: List<JioSaavnQualityUrl>?
)

data class JioSaavnAlbumDto(
    @SerializedName("id")   val id: String?,
    @SerializedName("name") val name: String?
)

data class JioSaavnArtistsDto(
    @SerializedName("primary")  val primary: List<JioSaavnArtistDto>?,
    @SerializedName("featured") val featured: List<JioSaavnArtistDto>?,
    @SerializedName("all")      val all: List<JioSaavnArtistDto>?
)

data class JioSaavnArtistDto(
    @SerializedName("id")   val id: String?,
    @SerializedName("name") val name: String?
)

data class JioSaavnQualityUrl(
    @SerializedName("quality") val quality: String?,
    @SerializedName("url")     val url: String?,
    @SerializedName("link")    val link: String?
) {
    val resolvedUrl: String? get() = url?.takeIf { it.isNotBlank() } ?: link?.takeIf { it.isNotBlank() }
}

data class ITunesPosterResponse(
    @SerializedName("resultCount") val resultCount: Int,
    @SerializedName("results")     val results: List<ITunesPosterItem>?
)

data class ITunesPosterItem(
    @SerializedName("artistName")    val artistName: String?,
    @SerializedName("trackName")     val trackName: String?,
    @SerializedName("artworkUrl100") val artworkUrl100: String?
) {
    val ultraHdPosterUrl: String?
        get() = artworkUrl100
            ?.replace("100x100bb.jpg", "1200x1200bb.jpg")
            ?.replace("100x100bb.png", "1200x1200bb.png")
            ?.replace("100x100bb", "1200x1200bb")
}
