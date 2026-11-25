package com.vybe.music.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vybe.music.model.ArtistSeed
import com.vybe.music.model.CustomPlaylist
import com.vybe.music.model.Track
import java.io.File

data class PlaybackStats(
    val totalScrobbles: Int = 0,
    val totalPlayTimeMs: Long = 0L,
    val uniqueTrackIds: Set<String> = emptySet(),
    val uniqueArtists: Set<String> = emptySet(),
    val uniqueAlbums: Set<String> = emptySet(),
    val recentTracks: List<Track> = emptyList(),
    val trackPlayCounts: Map<String, Int> = emptyMap(),
    val trackLastPlayed: Map<String, Long> = emptyMap()
) {
    val uniqueTrackCount: Int
        get() {
            val tracksCount = recentTracks.distinctBy { normalizedKey(it) }.size
            return if (tracksCount > 0) tracksCount else uniqueTrackIds.size
        }

    val uniqueArtistCount: Int
        get() {
            val artists = recentTracks.mapNotNull { cleanArtist(it.artist) }.distinct()
            return if (artists.isNotEmpty()) artists.size else uniqueArtists.mapNotNull { cleanArtist(it) }.distinct().size
        }

    val uniqueAlbumCount: Int
        get() {
            val albums = recentTracks.mapNotNull { cleanAlbum(it.album, it.title) }.distinct()
            return if (albums.isNotEmpty()) albums.size else uniqueAlbums.mapNotNull { cleanAlbum(it) }.distinct().size
        }

    val effectiveTotalScrobbles: Int
        get() {
            val sumCounts = recentTracks.distinctBy { normalizedKey(it) }.sumOf { getPlayCount(it) }
            return if (sumCounts > 0) sumCounts else totalScrobbles
        }

    fun getPlayCount(track: Track): Int {
        val normKey = normalizedKey(track)
        val legacyKey = "${track.title.trim().lowercase()}_${track.artist.trim().lowercase()}"
        return trackPlayCounts[track.id]
            ?: trackPlayCounts[normKey]
            ?: trackPlayCounts[legacyKey]
            ?: track.playCount.coerceAtLeast(1)
    }

    companion object {
        fun cleanArtist(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            val trimmed = raw.trim()
            val lower = trimmed.lowercase()
            if (lower in setOf("unknown", "unknown artist", "-", "&", "various artists", "various", "n/a", "null", "none")) {
                return null
            }
            return trimmed
        }

        fun cleanAlbum(rawAlbum: String?, trackTitle: String? = null): String? {
            if (rawAlbum.isNullOrBlank()) return null
            val trimmed = rawAlbum.trim()
            val lower = trimmed.lowercase()
            if (lower in setOf("unknown", "unknown album", "-", "&", "n/a", "null", "none", "single", "ep", "unreleased")) {
                return null
            }
            return trimmed
        }

        fun normalizedKey(track: Track): String {
            val cleanArtistName = cleanArtist(track.artist) ?: track.artist.trim()
            return "${track.title.trim().lowercase()}|${cleanArtistName.lowercase()}"
        }
    }
}

class LocalMusicStorage(
    private val context: Context,
    initialUserId: String = UserSessionManager.currentUserId
) {
    var userId: String = initialUserId
        private set

    init {
        UserSessionManager.migrateLegacyPrefsIfNeeded(context, userId)
        cleanGhostScrobblesIfNeeded()
    }

    fun switchUser(newUserId: String) {
        this.userId = newUserId
        UserSessionManager.migrateLegacyPrefsIfNeeded(context, newUserId)
        cleanGhostScrobblesIfNeeded()
    }

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(
            UserSessionManager.getUserPrefsName("vybe_local_storage", userId),
            Context.MODE_PRIVATE
        )
    private val gson = Gson()

    // ── Downloads ──
    fun getDownloadedTracks(): List<Track> {
        val json = prefs.getString("downloaded_tracks", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Track>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveDownloadedTracks(tracks: List<Track>) {
        val json = gson.toJson(tracks)
        prefs.edit().putString("downloaded_tracks", json).apply()
    }

    fun addDownloadedTrack(track: Track) {
        val current = getDownloadedTracks().filter { it.id != track.id }
        saveDownloadedTracks(listOf(track) + current)
    }

    fun removeDownloadedTrack(trackId: String) {
        val current = getDownloadedTracks().filter { it.id != trackId }
        saveDownloadedTracks(current)
    }

    fun isTrackDownloaded(trackId: String): Boolean {
        val file = getDownloadedFileForTrack(trackId)
        return file != null && file.exists() && file.length() > 0
    }

    fun getDownloadedFileForTrack(trackId: String): File? {
        val dir = UserSessionManager.getUserDownloadsDir(context, userId)
        val file = File(dir, "${trackId}.m4a")
        return if (file.exists() && file.length() > 0) file else null
    }

    // ── Favorites & Liked Songs ──
    fun getFavoriteTrackIds(): Set<String> {
        return prefs.getStringSet("favorite_track_ids", emptySet()) ?: emptySet()
    }

    fun saveFavoriteTrackIds(ids: Set<String>) {
        prefs.edit().putStringSet("favorite_track_ids", ids).apply()
    }

    fun getLikedTracks(): List<Track> {
        val json = prefs.getString("liked_tracks_full_v1", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Track>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveLikedTracks(tracks: List<Track>) {
        val json = gson.toJson(tracks)
        prefs.edit().putString("liked_tracks_full_v1", json).commit()
    }

    fun addLikedTrack(track: Track) {
        val current = getLikedTracks().filter { it.id != track.id }
        saveLikedTracks(listOf(track) + current)
    }

    fun removeLikedTrack(trackId: String) {
        val current = getLikedTracks().filter { it.id != trackId }
        saveLikedTracks(current)
    }

    fun updateTrackArtwork(trackId: String, newArtworkUrl: String) {
        updateTrackArtworkEverywhere(trackId, newArtworkUrl)
    }

    fun updateTrackArtworkEverywhere(trackId: String, newArtworkUrl: String) {
        if (trackId.isBlank() || newArtworkUrl.isBlank()) return
        // 1. Downloaded tracks
        val downloaded = getDownloadedTracks().map { if (it.id == trackId) it.copy(artworkUrl = newArtworkUrl) else it }
        saveDownloadedTracks(downloaded)
        // 2. Liked tracks
        val liked = getLikedTracks().map { if (it.id == trackId) it.copy(artworkUrl = newArtworkUrl) else it }
        saveLikedTracks(liked)
        // 3. Custom playlists
        val playlists = getCustomPlaylists()
        var playlistsModified = false
        val updatedPlaylists = playlists.map { pl ->
            val updatedTracks = pl.tracks.map { tr ->
                if (tr.id == trackId) {
                    playlistsModified = true
                    tr.copy(artworkUrl = newArtworkUrl)
                } else tr
            }
            if (updatedTracks != pl.tracks) pl.copy(tracks = updatedTracks) else pl
        }
        if (playlistsModified) {
            saveCustomPlaylists(updatedPlaylists)
        }
        // 4. Playback stats recent tracks
        val stats = getPlaybackStats()
        val updatedRecent = stats.recentTracks.map { if (it.id == trackId) it.copy(artworkUrl = newArtworkUrl) else it }
        if (updatedRecent != stats.recentTracks) {
            savePlaybackStats(stats.copy(recentTracks = updatedRecent))
        }
    }

    // ── Date-Keyed Global Top 5 Daily Cache ──
    fun getCachedGlobalTop5(dateKey: String): List<Track>? {
        val json = prefs.getString("global_top5_v3_$dateKey", null) ?: return null
        return try {
            val type = object : TypeToken<List<Track>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    fun saveCachedGlobalTop5(dateKey: String, tracks: List<Track>) {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("global_top5") && it != "global_top5_v3_$dateKey" }.forEach {
            editor.remove(it)
        }
        val json = gson.toJson(tracks)
        editor.putString("global_top5_v3_$dateKey", json).apply()
    }

    fun clearStaleGlobalTop5Cache(todayDateKey: String) {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("global_top5") && it != "global_top5_v3_$todayDateKey" }.forEach {
            editor.remove(it)
        }
        editor.apply()
    }

    // ── Playlists ──
    fun getCustomPlaylists(): List<CustomPlaylist> {
        val json = prefs.getString("custom_playlists", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CustomPlaylist>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCustomPlaylists(playlists: List<CustomPlaylist>) {
        val json = gson.toJson(playlists)
        prefs.edit().putString("custom_playlists", json).apply()
    }

    // ── Playback Stats ──
    fun getPlaybackStats(): PlaybackStats {
        val json = prefs.getString("playback_stats", null) ?: return PlaybackStats()
        return try {
            val parsed = gson.fromJson(json, PlaybackStats::class.java) ?: PlaybackStats()
            val playCounts = (parsed.trackPlayCounts ?: emptyMap()).toMutableMap()
            val lastPlayed = (parsed.trackLastPlayed ?: emptyMap()).toMutableMap()
            val mappedRecent = (parsed.recentTracks ?: emptyList()).map { track ->
                val count = playCounts[track.id]
                    ?: playCounts[PlaybackStats.normalizedKey(track)]
                    ?: track.playCount.coerceAtLeast(1)
                playCounts[track.id] = count
                track.copy(playCount = count)
            }
            parsed.copy(
                uniqueTrackIds = parsed.uniqueTrackIds ?: emptySet(),
                uniqueArtists = parsed.uniqueArtists ?: emptySet(),
                uniqueAlbums = parsed.uniqueAlbums ?: emptySet(),
                recentTracks = mappedRecent,
                trackPlayCounts = playCounts,
                trackLastPlayed = lastPlayed
            )
        } catch (e: Exception) {
            PlaybackStats()
        }
    }

    fun savePlaybackStats(stats: PlaybackStats) {
        val json = gson.toJson(stats)
        prefs.edit().putString("playback_stats", json).apply()
    }

    /**
     * Increments total listening time based on actual elapsed playback milliseconds.
     */
    @Synchronized
    fun addListeningTime(deltaMs: Long): PlaybackStats {
        if (deltaMs <= 0) return getPlaybackStats()
        val current = getPlaybackStats()
        val updated = current.copy(
            totalPlayTimeMs = current.totalPlayTimeMs + deltaMs
        )
        savePlaybackStats(updated)
        return updated
    }

    /**
     * Records a track play: increments cumulative playCount by +1, updates lastPlayedTimestamp,
     * bumps to top of recent tracks list, and increments total scrobbles.
     */
    @Synchronized
    fun recordTrackPlay(track: Track): PlaybackStats {
        val current = getPlaybackStats()
        val normKey = PlaybackStats.normalizedKey(track)
        val legacyKey = "${track.title.trim().lowercase()}_${track.artist.trim().lowercase()}"
        val currentCount = current.trackPlayCounts[track.id]
            ?: current.trackPlayCounts[normKey]
            ?: current.trackPlayCounts[legacyKey]
            ?: current.recentTracks.find { it.id == track.id || PlaybackStats.normalizedKey(it) == normKey }?.playCount
            ?: 0
        val newCount = currentCount + 1
        val now = System.currentTimeMillis()

        val updatedPlayCounts = (current.trackPlayCounts ?: emptyMap()).toMutableMap()
        updatedPlayCounts[track.id] = newCount
        updatedPlayCounts[normKey] = newCount

        val updatedLastPlayed = (current.trackLastPlayed ?: emptyMap()).toMutableMap()
        updatedLastPlayed[track.id] = now
        updatedLastPlayed[normKey] = now

        val updatedTrack = track.copy(playCount = newCount, lastPlayedTimestamp = now)

        // Deduplicate recent tracks: remove previous occurrence, then prepend at top
        val filteredRecent = (current.recentTracks ?: emptyList()).filter {
            it.id != track.id && PlaybackStats.normalizedKey(it) != normKey
        }
        val newRecent = (listOf(updatedTrack) + filteredRecent).take(2000)

        val cleanTrackIds = newRecent.map { it.id }.toSet()
        val cleanArtists = newRecent.mapNotNull { PlaybackStats.cleanArtist(it.artist) }.toSet()
        val cleanAlbums = newRecent.mapNotNull { PlaybackStats.cleanAlbum(it.album, it.title) }.toSet()
        val totalScrobbles = newRecent.sumOf {
            updatedPlayCounts[it.id] ?: updatedPlayCounts[PlaybackStats.normalizedKey(it)] ?: it.playCount.coerceAtLeast(1)
        }

        val updated = current.copy(
            totalScrobbles = totalScrobbles,
            uniqueTrackIds = cleanTrackIds,
            uniqueArtists = cleanArtists,
            uniqueAlbums = cleanAlbums,
            recentTracks = newRecent,
            trackPlayCounts = updatedPlayCounts,
            trackLastPlayed = updatedLastPlayed
        )
        savePlaybackStats(updated)
        return updated
    }

    // Backward-compatible alias
    @Synchronized
    fun recordScrobble(track: Track): PlaybackStats {
        return recordTrackPlay(track)
    }

    @Synchronized
    fun resetListeningStatistics(): PlaybackStats {
        val empty = PlaybackStats()
        savePlaybackStats(empty)
        return empty
    }

    /**
     * One-time startup migration to clean up inflated / ghost scrobbles and sync
     * uniqueTrackIds, uniqueArtists, uniqueAlbums, and totalScrobbles with actual deduplicated recent tracks.
     */
    private fun cleanGhostScrobblesIfNeeded() {
        if (prefs.getBoolean("stats_cleaned_v4", false)) return
        try {
            val current = getPlaybackStats()
            val existingRecent = current.recentTracks ?: emptyList()
            if (existingRecent.isEmpty() && current.totalScrobbles == 0) {
                prefs.edit().putBoolean("stats_cleaned_v4", true).apply()
                return
            }

            // Deduplicate tracks by normalized key
            val seenKeys = mutableSetOf<String>()
            val cleanedRecent = mutableListOf<Track>()
            val playCounts = (current.trackPlayCounts ?: emptyMap()).toMutableMap()
            val lastPlayed = (current.trackLastPlayed ?: emptyMap()).toMutableMap()

            for (track in existingRecent) {
                val key = PlaybackStats.normalizedKey(track)
                val legacyKey = "${track.title.trim().lowercase()}_${track.artist.trim().lowercase()}"
                if (seenKeys.add(key)) {
                    val count = playCounts[track.id]
                        ?: playCounts[key]
                        ?: playCounts[legacyKey]
                        ?: track.playCount.coerceAtLeast(1)
                    val safeCount = count.coerceIn(1, 150)
                    playCounts[track.id] = safeCount
                    playCounts[key] = safeCount
                    val ts = track.lastPlayedTimestamp.takeIf { it > 0L }
                        ?: lastPlayed[track.id]
                        ?: lastPlayed[key]
                        ?: System.currentTimeMillis()
                    lastPlayed[track.id] = ts
                    lastPlayed[key] = ts
                    cleanedRecent.add(track.copy(playCount = safeCount, lastPlayedTimestamp = ts))
                }
            }

            val cleanTrackIds = cleanedRecent.map { it.id }.toSet()
            val cleanArtists = cleanedRecent.mapNotNull { PlaybackStats.cleanArtist(it.artist) }.toSet()
            val cleanAlbums = cleanedRecent.mapNotNull { PlaybackStats.cleanAlbum(it.album, it.title) }.toSet()
            val totalScrobbles = cleanedRecent.sumOf { it.playCount }

            val cleanedStats = current.copy(
                totalScrobbles = totalScrobbles,
                uniqueTrackIds = cleanTrackIds,
                uniqueArtists = cleanArtists,
                uniqueAlbums = cleanAlbums,
                recentTracks = cleanedRecent,
                trackPlayCounts = playCounts,
                trackLastPlayed = lastPlayed
            )
            savePlaybackStats(cleanedStats)
            prefs.edit().putBoolean("stats_cleaned_v4", true).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── User Profile Display Name & Auth Cache ──
    fun getDisplayName(): String {
        return prefs.getString("user_display_name", "Ayush Mahato") ?: "Ayush Mahato"
    }

    fun saveDisplayName(name: String) {
        prefs.edit().putString("user_display_name", name.trim()).apply()
    }

    fun getCachedPhotoUrl(): String? = prefs.getString("user_photo_url", null)
    fun saveCachedPhotoUrl(url: String?) = prefs.edit().putString("user_photo_url", url).apply()

    fun getCachedEmail(): String? = prefs.getString("user_email", null)
    fun saveCachedEmail(email: String?) = prefs.edit().putString("user_email", email).apply()

    fun getLastSyncedTime(): Long = prefs.getLong("last_synced_time", 0L)
    fun saveLastSyncedTime(time: Long) = prefs.edit().putLong("last_synced_time", time).apply()

    // ── Streaming Audio Quality ──
    fun getStreamingQuality(): com.vybe.music.model.AudioQuality {
        val name = prefs.getString("streaming_audio_quality", com.vybe.music.model.AudioQuality.MAX.name)
        return try {
            com.vybe.music.model.AudioQuality.valueOf(name ?: com.vybe.music.model.AudioQuality.MAX.name)
        } catch (e: Exception) {
            com.vybe.music.model.AudioQuality.MAX
        }
    }

    fun saveStreamingQuality(quality: com.vybe.music.model.AudioQuality) {
        prefs.edit().putString("streaming_audio_quality", quality.name).apply()
    }

    // ── Recent Searches ──
    fun getRecentSearches(): List<String> {
        val json = prefs.getString("recent_searches", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank() || trimmed.length < 2) return
        val current = getRecentSearches().filter { !it.equals(trimmed, ignoreCase = true) }
        val updated = (listOf(trimmed) + current).take(20)
        prefs.edit().putString("recent_searches", gson.toJson(updated)).apply()
    }

    fun removeRecentSearch(query: String) {
        val trimmed = query.trim()
        val current = getRecentSearches().filter { !it.equals(trimmed, ignoreCase = true) }
        prefs.edit().putString("recent_searches", gson.toJson(current)).apply()
    }

    fun clearRecentSearches() {
        prefs.edit().remove("recent_searches").apply()
    }

    // ── Live Trending Cache (6-12 hours expiration) ──
    fun getCachedTrendingTracks(): List<Track>? {
        val timestamp = prefs.getLong("trending_cache_timestamp", 0L)
        val now = System.currentTimeMillis()
        if (now - timestamp > 8 * 3600 * 1000L) {
            return null
        }
        val json = prefs.getString("cached_trending_tracks", null) ?: return null
        return try {
            val type = object : TypeToken<List<Track>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    fun saveCachedTrendingTracks(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val json = gson.toJson(tracks)
        prefs.edit()
            .putString("cached_trending_tracks", json)
            .putLong("trending_cache_timestamp", System.currentTimeMillis())
            .apply()
    }

    // ── Personalized Onboarding Taste Preferences ──
    fun hasCompletedOnboarding(): Boolean {
        return prefs.getBoolean("has_completed_onboarding", false)
    }

    fun setCompletedOnboarding(completed: Boolean) {
        prefs.edit().putBoolean("has_completed_onboarding", completed).apply()
    }

    fun getSelectedLanguages(): Set<String> {
        return prefs.getStringSet("selected_languages", emptySet()) ?: emptySet()
    }

    fun getSelectedGenres(): Set<String> {
        return prefs.getStringSet("selected_genres", emptySet()) ?: emptySet()
    }

    fun getSelectedArtists(): Set<String> {
        return prefs.getStringSet("selected_artists", emptySet()) ?: emptySet()
    }

    fun getSelectedArtistSeeds(): List<ArtistSeed> {
        val json = prefs.getString("selected_artist_seeds_json", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ArtistSeed>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    fun saveTastePreferences(
        languages: Set<String>,
        genres: Set<String>,
        artists: Set<String>,
        artistSeeds: List<ArtistSeed> = emptyList(),
        completed: Boolean = true
    ) {
        val seedsToSave = if (artistSeeds.isNotEmpty()) {
            artistSeeds
        } else {
            artists.map { ArtistSeed(id = it.lowercase().replace(Regex("[^a-z0-9]"), "_"), name = it, imageUrl = "") }
        }
        prefs.edit()
            .putStringSet("selected_languages", languages)
            .putStringSet("selected_genres", genres)
            .putStringSet("selected_artists", artists)
            .putString("selected_artist_seeds_json", gson.toJson(seedsToSave))
            .putBoolean("has_completed_onboarding", completed)
            .apply()
    }

    // ── Local Taste Engine: computes user preferences from history, likes, and searches ──
    fun getUserTasteProfile(): UserTasteProfile {
        val stats = getPlaybackStats()
        val favorites = getFavoriteTrackIds()
        val recentSearches = getRecentSearches()
        val selectedLanguages = getSelectedLanguages()
        val selectedGenres = getSelectedGenres()
        val selectedArtists = getSelectedArtists()

        // 1. Compute top artists ranked by play count + recent presence + likes + onboarding choices
        val artistScores = mutableMapOf<String, Int>()
        // Give chosen onboarding artists strong base priority
        for (artist in selectedArtists) {
            val primary = artist.split(",", "&", "feat.", "ft.", "x", "/").firstOrNull()?.trim() ?: artist
            if (primary.isNotBlank()) {
                artistScores[primary] = 12
            }
        }
        for ((trackId, count) in stats.trackPlayCounts) {
            val track = stats.recentTracks.find { it.id == trackId }
            val artist = track?.artist ?: ""
            if (artist.isNotBlank()) {
                val primary = artist.split(",", "&", "feat.", "ft.", "x", "/").firstOrNull()?.trim() ?: artist
                artistScores[primary] = (artistScores[primary] ?: 0) + count * 2
            }
        }
        for (track in stats.recentTracks) {
            val primary = track.artist.split(",", "&", "feat.", "ft.", "x", "/").firstOrNull()?.trim() ?: track.artist
            if (primary.isNotBlank()) {
                val bonus = if (favorites.contains(track.id)) 4 else 1
                artistScores[primary] = (artistScores[primary] ?: 0) + bonus
            }
        }

        val topArtists = artistScores.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .filter { it.isNotBlank() }
            .take(6)
            .ifEmpty { selectedArtists.toList() }

        // 2. Infer top genres / languages
        val allArtistsAndTitles = (stats.recentTracks.map { "${it.artist} ${it.title} ${it.album}" } + recentSearches)
            .joinToString(" ")
            .lowercase()

        val detectedGenres = mutableListOf<String>()
        detectedGenres.addAll(selectedGenres)

        val punjabiKeywords = listOf("punjabi", "dosanjh", "aujla", "dhillon", "sidhu", "moose", "shubh", "karan", "diljit")
        val hindiKeywords = listOf("arijit", "pritam", "shreya", "kumar", "atif", "badshah", "honey", "neha", "jubin", "armaan", "bollywood", "hindi")
        val englishKeywords = listOf("weeknd", "swift", "lipa", "sheeran", "bieber", "drake", "billie", "sabrina", "travis", "post malone", "pop", "hip hop")
        val southKeywords = listOf("anirudh", "sid sriram", "ar rahman", "thaman", "devi sri", "tamil", "telugu")

        if (punjabiKeywords.any { allArtistsAndTitles.contains(it) } && !detectedGenres.contains("Punjabi Hits")) detectedGenres.add("Punjabi Hits")
        if (hindiKeywords.any { allArtistsAndTitles.contains(it) } && !detectedGenres.contains("Bollywood Hindi")) detectedGenres.add("Bollywood Hindi")
        if (englishKeywords.any { allArtistsAndTitles.contains(it) } && !detectedGenres.contains("Global Pop")) detectedGenres.add("Global Pop")
        if (southKeywords.any { allArtistsAndTitles.contains(it) } && !detectedGenres.contains("South Hits")) detectedGenres.add("South Hits")

        val seedQueries = mutableListOf<String>()
        // Prioritize chosen artists
        topArtists.forEach { seedQueries.add("$it Top Songs") }
        // Prioritize chosen genres & languages
        selectedGenres.forEach { seedQueries.add("$it Hits") }
        selectedLanguages.forEach { seedQueries.add("$it Songs") }
        detectedGenres.forEach { seedQueries.add(it) }

        val finalSeeds = seedQueries.distinct().take(10).ifEmpty {
            listOf("Trending Songs", "Top Hits India", "Global Top 50")
        }

        return UserTasteProfile(
            topArtists = topArtists,
            topGenres = detectedGenres,
            recentSearches = recentSearches,
            seedQueries = finalSeeds,
            selectedLanguages = selectedLanguages.toList(),
            selectedGenres = selectedGenres.toList(),
            selectedArtists = selectedArtists.toList()
        )
    }
}

data class UserTasteProfile(
    val topArtists: List<String> = emptyList(),
    val topGenres: List<String> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val seedQueries: List<String> = emptyList(),
    val selectedLanguages: List<String> = emptyList(),
    val selectedGenres: List<String> = emptyList(),
    val selectedArtists: List<String> = emptyList()
)
