package com.vybe.music.utils

import com.vybe.music.model.Track
import java.util.Locale

object TrackUtils {

    /**
     * Normalizes title and primary artist to detect duplicate songs across different releases
     * (e.g. "One Of The Girls" vs "One Of The Girls - EP" vs "One Of The Girls (from The Idol...)").
     * Strict normalized key format: "$normalizedTitle|$primaryArtist"
     */
    fun normalizeTrackKey(title: String, artist: String): String {
        val normalizedTitle = title.lowercase(Locale.ROOT)
            .replace(Regex("(?i)\\s*-\\s*(ep|single|album|soundtrack|from.*)"), "")
            .replace(Regex("\\(.*?\\)|\\[.*?\\]"), "")
            .replace(Regex("[^a-z0-9]"), "")

        val primaryArtist = artist.lowercase(Locale.ROOT)
            .split(",", "&", "feat.", "feat", "ft.", "ft", "featuring", " x ", "/", "with", "•")
            .firstOrNull()?.trim()
            ?.replace(Regex("[^a-z0-9]"), "")
            ?: ""

        return "$normalizedTitle|$primaryArtist"
    }

    /**
     * Upgrades thumbnail and artwork URLs to high-resolution square 1:1 format.
     * In particular, upgrades lh3.googleusercontent.com thumbnails to `=w1080-h1080-l90-rj`,
     * iTunes/Apple artwork to 1200x1200bb, and Saavn artwork to 500x500.
     */
    fun upgradeArtworkUrl(url: String): String {
        if (url.isBlank()) return url
        return url
            .replace("100x100bb.jpg", "1200x1200bb.jpg")
            .replace("100x100bb.png", "1200x1200bb.png")
            .replace("100x100bb", "1200x1200bb")
            .replace("600x600bb", "1200x1200bb")
            .replace("1000x1000bb", "1200x1200bb")
            .replace(Regex("=w\\d+-h\\d+.*"), "=w1080-h1080-l90-rj")
            .replace(Regex("=s\\d+.*"), "=w1080-h1080-l90-rj")
            .replace("150x150", "500x500")
            .replace("50x50", "500x500")
    }

    /**
     * Scores track release and artwork quality so that:
     * - Official Album / Soundtrack releases (e.g., The Idol Episode 4) are preferred.
     * - High-res square lh3.googleusercontent.com artwork (=w1080-h1080-l90-rj) is preferred.
     * - Liked/saved status if already in user's library gets heavy priority.
     * - Versions whose album name ends with "- EP" or "- Single" are heavily penalized/dropped when a full album exists.
     * - ytimg.com / 16:9 letterboxed thumbnails are penalized.
     */
    fun getArtworkQualityScore(track: Track, favoriteTrackIds: Set<String>? = null): Int {
        val art = track.artworkUrl.lowercase(Locale.ROOT)
        val album = track.album.lowercase(Locale.ROOT)
        val title = track.title.lowercase(Locale.ROOT)
        var score = 100

        // 1. Liked/saved status in user library gets heavy preference
        if (favoriteTrackIds != null && favoriteTrackIds.contains(track.id)) {
            score += 100
        }

        // 2. High-res square artwork priority
        if (art.contains("lh3.googleusercontent.com") || art.contains("googleusercontent.com")) {
            score += 80
            if (art.contains("=w1080-h1080-l90-rj") || art.contains("=w1200") || art.contains("=s1080")) {
                score += 20
            }
        }
        if (art.contains("mzstatic.com") || art.contains("itunes.apple.com")) {
            score += 60
            if (art.contains("1200x1200bb") || art.contains("1000x1000bb")) {
                score += 20
            }
        }
        if (art.contains("saavncdn.com")) score += 40

        // Heavily penalize letterboxed 16:9 thumbnails or YouTube static video frames
        if (art.contains("ytimg.com") || art.contains("i.ytimg.com") || art.contains("hqdefault") || art.contains("mqdefault")) {
            score -= 80
        }

        // Low-res penalty
        if (art.contains("100x100") || art.contains("50x50")) {
            score -= 30
        }

        // 3. Album / Soundtrack release type preference
        if (album.contains("the idol episode") || album.contains("soundtrack") || album.contains("ost") || album.contains("original motion picture")) {
            score += 60
        }

        // Detect EP / Single vs Full Album
        val isAlbumEp = album.endsWith("- ep") || album.endsWith(" - ep") || album.endsWith(" ep") || album.contains("- ep") || album.contains(" - ep")
        val isAlbumSingle = album.endsWith("- single") || album.endsWith(" - single") || album.endsWith(" single") || album.contains("- single") || album.contains(" - single")
        val isTitleEp = title.endsWith("- ep") || title.endsWith(" - ep") || title.endsWith(" ep") || title.contains("- ep") || title.contains(" - ep")
        val isTitleSingle = title.endsWith("- single") || title.endsWith(" - single") || title.endsWith(" single") || title.contains("- single") || title.contains(" - single")

        // Full album bonus when album is non-empty, not equal to title, and not an EP/single
        if (album.isNotBlank() && !album.equals(title, ignoreCase = true) && !isAlbumEp && !isAlbumSingle) {
            score += 30
        }

        // Penalize / drop EP or Single versions when competing against a full album/soundtrack
        if (isAlbumEp) score -= 50
        if (isAlbumSingle) score -= 40
        if (isTitleEp) score -= 50
        if (isTitleSingle) score -= 40

        return score
    }

    /**
     * Deduplicates tracks by normalized (title + primary artist).
     * When two tracks share the same normalized key, scores them and keeps only the highest-scoring one,
     * ensuring upgraded high-res artwork.
     */
    fun deduplicateTracks(tracks: List<Track>, favoriteTrackIds: Set<String>? = null): List<Track> {
        if (tracks.isEmpty()) return emptyList()

        val keyOrder = mutableListOf<String>()
        val groups = mutableMapOf<String, MutableList<Track>>()

        for (track in tracks) {
            val key = normalizeTrackKey(track.title, track.artist)
            if (key !in groups) {
                keyOrder.add(key)
                groups[key] = mutableListOf()
            }
            groups[key]!!.add(track)
        }

        return keyOrder.mapNotNull { key ->
            val candidates = groups[key] ?: return@mapNotNull null
            val best = candidates.maxByOrNull { getArtworkQualityScore(it, favoriteTrackIds) } ?: candidates.first()
            val upgradedArtwork = upgradeArtworkUrl(best.artworkUrl)
            if (upgradedArtwork != best.artworkUrl) {
                best.copy(artworkUrl = upgradedArtwork)
            } else {
                best
            }
        }
    }
}
