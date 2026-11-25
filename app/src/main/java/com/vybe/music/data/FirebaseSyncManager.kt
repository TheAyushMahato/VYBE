package com.vybe.music.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vybe.music.model.ArtistSeed
import com.vybe.music.model.CustomPlaylist
import com.vybe.music.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class CloudBackupData(
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val likedTracks: List<Track> = emptyList(),
    val favoriteTrackIds: Set<String> = emptySet(),
    val customPlaylists: List<CustomPlaylist> = emptyList(),
    val playbackStats: PlaybackStats = PlaybackStats(),
    val lastSyncedAt: Long = 0L
)

enum class SyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    ERROR
}

class FirebaseSyncManager(
    private val context: Context,
    private val localStorage: LocalMusicStorage
) {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val gson = Gson()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncedTimestamp = MutableStateFlow(localStorage.getLastSyncedTime())
    val lastSyncedTimestamp: StateFlow<Long> = _lastSyncedTimestamp.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            if (user != null) {
                user.photoUrl?.toString()?.let { localStorage.saveCachedPhotoUrl(it) }
                user.email?.let { localStorage.saveCachedEmail(it) }
                user.displayName?.let { if (it.isNotBlank()) localStorage.saveDisplayName(it) }
            }
        }
    }

    fun getWebClientId(): String {
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) {
                val id = context.getString(resId)
                if (id.isNotBlank()) return id
            }
            "443890209916-7bd73lh4g9i7r8nl91gn1ikc6nvcg6k5.apps.googleusercontent.com"
        } catch (_: Exception) {
            "443890209916-7bd73lh4g9i7r8nl91gn1ikc6nvcg6k5.apps.googleusercontent.com"
        }
    }

    private fun findActivity(ctx: Context): Activity? {
        var current = ctx
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> {
        return try {
            val hostContext = findActivity(activityContext) ?: activityContext
            val credentialManager = CredentialManager.create(hostContext)
            val webClientId = getWebClientId()

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = hostContext,
                request = request
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: throw IllegalStateException("FirebaseUser is null after sign-in")
                _currentUser.value = user
                user.photoUrl?.toString()?.let { localStorage.saveCachedPhotoUrl(it) }
                user.email?.let { localStorage.saveCachedEmail(it) }
                user.displayName?.let { if (it.isNotBlank()) localStorage.saveDisplayName(it) }
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unsupported credential type: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(activityContext: Context) {
        try {
            auth.signOut()
            _currentUser.value = null
            localStorage.switchUser("guest")
            localStorage.saveCachedPhotoUrl(null)
            localStorage.saveCachedEmail(null)
            val hostContext = findActivity(activityContext) ?: activityContext
            val credentialManager = CredentialManager.create(hostContext)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {}
    }

    suspend fun pullAndMergeCloudData(
        localLiked: List<Track>,
        localPlaylists: List<CustomPlaylist>,
        localStats: PlaybackStats,
        localDisplayName: String
    ): CloudBackupData? {
        val user = auth.currentUser ?: return null
        return try {
            _syncStatus.value = SyncStatus.SYNCING
            val userDocRef = firestore.collection("users").document(user.uid)
            var snapshot = userDocRef.get().await()

            if (!snapshot.exists() || snapshot.getString("likedTracksJson") == null) {
                // Check subcollection users/{uid}/sync/data if root doc is empty or restricted
                try {
                    val subDoc = userDocRef.collection("sync").document("data").get().await()
                    if (subDoc.exists()) {
                        snapshot = subDoc
                    }
                } catch (_: Exception) {}
            }

            if (!snapshot.exists()) {
                // First-time sign-in on cloud: Push local library to Firestore
                pushToFirestore(
                    likedTracks = localLiked,
                    customPlaylists = localPlaylists,
                    playbackStats = localStats,
                    displayName = user.displayName ?: localDisplayName
                )
                _syncStatus.value = SyncStatus.SYNCED
                return null
            }

            val cloudLikedJson = snapshot.getString("likedTracksJson")
            val cloudPlaylistsJson = snapshot.getString("customPlaylistsJson")
            val cloudStatsJson = snapshot.getString("playbackStatsJson")
            val cloudDisplayName = snapshot.getString("displayName") ?: user.displayName ?: localDisplayName
            val cloudEmail = snapshot.getString("email") ?: user.email ?: ""
            val cloudPhoto = snapshot.getString("photoUrl") ?: user.photoUrl?.toString() ?: ""
            val lastSynced = snapshot.getLong("lastSyncedAt") ?: System.currentTimeMillis()

            val cloudLiked: List<Track> = try {
                if (!cloudLikedJson.isNullOrBlank()) {
                    val type = object : TypeToken<List<Track>>() {}.type
                    gson.fromJson(cloudLikedJson, type) ?: emptyList()
                } else emptyList()
            } catch (_: Exception) { emptyList() }

            val cloudPlaylists: List<CustomPlaylist> = try {
                if (!cloudPlaylistsJson.isNullOrBlank()) {
                    val type = object : TypeToken<List<CustomPlaylist>>() {}.type
                    gson.fromJson(cloudPlaylistsJson, type) ?: emptyList()
                } else emptyList()
            } catch (_: Exception) { emptyList() }

            val cloudStats: PlaybackStats = try {
                if (!cloudStatsJson.isNullOrBlank()) {
                    gson.fromJson(cloudStatsJson, PlaybackStats::class.java) ?: PlaybackStats()
                } else PlaybackStats()
            } catch (_: Exception) { PlaybackStats() }

            // ── Auto-Skip Onboarding for Existing/Returning Users ──
            val cloudHasOnboarding = snapshot.getBoolean("onboardingCompleted")
                ?: snapshot.getBoolean("hasCompletedOnboarding")
                ?: false
            val cloudLangs = (snapshot.get("selectedLanguages") as? List<*>)?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
            val cloudGenres = (snapshot.get("selectedGenres") as? List<*>)?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()

            val rawArtistsList = (snapshot.get("selectedArtists") as? List<*>) ?: emptyList<Any>()
            val rawArtistSeeds = (snapshot.get("selectedArtistSeeds") as? List<*>) ?: emptyList<Any>()
            val cloudArtistNames = mutableSetOf<String>()
            val cloudSeeds = mutableListOf<ArtistSeed>()

            for (item in rawArtistSeeds) {
                if (item is Map<*, *>) {
                    val id = item["id"]?.toString() ?: ""
                    val name = item["name"]?.toString() ?: ""
                    val img = item["imageUrl"]?.toString() ?: ""
                    if (name.isNotBlank()) {
                        cloudArtistNames.add(name)
                        cloudSeeds.add(ArtistSeed(id = id.ifBlank { name.lowercase().replace(" ", "_") }, name = name, imageUrl = img))
                    }
                }
            }

            for (item in rawArtistsList) {
                if (item is String && item.isNotBlank()) {
                    cloudArtistNames.add(item)
                    if (cloudSeeds.none { it.name.equals(item, ignoreCase = true) }) {
                        cloudSeeds.add(ArtistSeed(id = item.lowercase().replace(" ", "_"), name = item, imageUrl = ""))
                    }
                } else if (item is Map<*, *>) {
                    val id = item["id"]?.toString() ?: ""
                    val name = item["name"]?.toString() ?: ""
                    val img = item["imageUrl"]?.toString() ?: ""
                    if (name.isNotBlank()) {
                        cloudArtistNames.add(name)
                        if (cloudSeeds.none { it.name.equals(name, ignoreCase = true) }) {
                            cloudSeeds.add(ArtistSeed(id = id.ifBlank { name.lowercase().replace(" ", "_") }, name = name, imageUrl = img))
                        }
                    }
                }
            }

            val isReturningUser = cloudHasOnboarding ||
                    cloudArtistNames.isNotEmpty() ||
                    cloudLiked.isNotEmpty() ||
                    cloudStats.totalScrobbles > 0 ||
                    cloudStats.recentTracks.isNotEmpty()

            if (isReturningUser) {
                val langsToSave = if (cloudLangs.isNotEmpty()) cloudLangs else localStorage.getSelectedLanguages()
                val genresToSave = if (cloudGenres.isNotEmpty()) cloudGenres else localStorage.getSelectedGenres()
                val artistsToSave = if (cloudArtistNames.isNotEmpty()) cloudArtistNames else localStorage.getSelectedArtists()
                val seedsToSave = if (cloudSeeds.isNotEmpty()) cloudSeeds else localStorage.getSelectedArtistSeeds()

                localStorage.saveTastePreferences(
                    languages = langsToSave,
                    genres = genresToSave,
                    artists = artistsToSave,
                    artistSeeds = seedsToSave,
                    completed = true
                )
            }

            // ── Merge Cloud and Local Data ──
            // 1. Liked Tracks: Merge by ID, keeping unique items with local taking precedence
            val mergedLikedMap = LinkedHashMap<String, Track>()
            localLiked.forEach { mergedLikedMap[it.id] = it }
            cloudLiked.forEach { if (!mergedLikedMap.containsKey(it.id)) mergedLikedMap[it.id] = it }
            val mergedLiked = mergedLikedMap.values.toList()
            val mergedFavIds = mergedLiked.map { it.id }.toSet()

            // 2. Custom Playlists: Merge by playlist ID
            val playlistMap = LinkedHashMap<String, CustomPlaylist>()
            localPlaylists.forEach { playlistMap[it.id] = it }
            cloudPlaylists.forEach { cloudPl ->
                val localPl = playlistMap[cloudPl.id]
                if (localPl == null) {
                    playlistMap[cloudPl.id] = cloudPl
                } else {
                    val pTracksMap = LinkedHashMap<String, Track>()
                    localPl.tracks.forEach { pTracksMap[it.id] = it }
                    cloudPl.tracks.forEach { if (!pTracksMap.containsKey(it.id)) pTracksMap[it.id] = it }
                    playlistMap[cloudPl.id] = localPl.copy(tracks = pTracksMap.values.toList())
                }
            }
            val mergedPlaylists = playlistMap.values.toList()

            // 3. Playback Stats: Merge
            val mergedScrobbles = maxOf(localStats.totalScrobbles, cloudStats.totalScrobbles)
            val mergedPlayTime = maxOf(localStats.totalPlayTimeMs, cloudStats.totalPlayTimeMs)
            val mergedTrackIds = (localStats.uniqueTrackIds ?: emptySet()) + (cloudStats.uniqueTrackIds ?: emptySet())
            val mergedArtists = (localStats.uniqueArtists ?: emptySet()) + (cloudStats.uniqueArtists ?: emptySet())
            val mergedAlbums = (localStats.uniqueAlbums ?: emptySet()) + (cloudStats.uniqueAlbums ?: emptySet())

            val mergedPlayCounts = (cloudStats.trackPlayCounts ?: emptyMap()).toMutableMap()
            (localStats.trackPlayCounts ?: emptyMap()).forEach { (k, v) ->
                mergedPlayCounts[k] = maxOf(mergedPlayCounts[k] ?: 0, v)
            }

            val mergedLastPlayed = (cloudStats.trackLastPlayed ?: emptyMap()).toMutableMap()
            (localStats.trackLastPlayed ?: emptyMap()).forEach { (k, v) ->
                mergedLastPlayed[k] = maxOf(mergedLastPlayed[k] ?: 0L, v)
            }

            val recentMap = LinkedHashMap<String, Track>()
            (localStats.recentTracks ?: emptyList()).forEach { recentMap[it.id] = it }
            (cloudStats.recentTracks ?: emptyList()).forEach { if (!recentMap.containsKey(it.id)) recentMap[it.id] = it }
            val mergedRecent = recentMap.values.sortedByDescending { it.lastPlayedTimestamp }.take(2000)

            val mergedStats = PlaybackStats(
                totalScrobbles = mergedScrobbles,
                totalPlayTimeMs = mergedPlayTime,
                uniqueTrackIds = mergedTrackIds,
                uniqueArtists = mergedArtists,
                uniqueAlbums = mergedAlbums,
                recentTracks = mergedRecent,
                trackPlayCounts = mergedPlayCounts,
                trackLastPlayed = mergedLastPlayed
            )

            val mergedDisplayName = if (localDisplayName.isNotBlank() && localDisplayName != "Ayush Mahato") {
                localDisplayName
            } else if (cloudDisplayName.isNotBlank()) {
                cloudDisplayName
            } else {
                user.displayName ?: localDisplayName
            }

            val mergedData = CloudBackupData(
                displayName = mergedDisplayName,
                email = cloudEmail.ifBlank { user.email ?: "" },
                photoUrl = cloudPhoto.ifBlank { user.photoUrl?.toString() ?: "" },
                likedTracks = mergedLiked,
                favoriteTrackIds = mergedFavIds,
                customPlaylists = mergedPlaylists,
                playbackStats = mergedStats,
                lastSyncedAt = System.currentTimeMillis()
            )

            // Save merged snapshot back to cloud
            pushToFirestore(
                likedTracks = mergedData.likedTracks,
                customPlaylists = mergedData.customPlaylists,
                playbackStats = mergedData.playbackStats,
                displayName = mergedData.displayName
            )

            _syncStatus.value = SyncStatus.SYNCED
            _lastSyncedTimestamp.value = mergedData.lastSyncedAt
            localStorage.saveLastSyncedTime(mergedData.lastSyncedAt)

            mergedData
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.ERROR
            null
        }
    }

    suspend fun pushToFirestore(
        likedTracks: List<Track>,
        customPlaylists: List<CustomPlaylist>,
        playbackStats: PlaybackStats,
        displayName: String
    ): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            _syncStatus.value = SyncStatus.SYNCING
            val now = System.currentTimeMillis()
            val tasteLangs = localStorage.getSelectedLanguages().toList()
            val tasteGenres = localStorage.getSelectedGenres().toList()
            val tasteArtists = localStorage.getSelectedArtists().toList()
            val tasteArtistSeeds = localStorage.getSelectedArtistSeeds()
            val tasteArtistSeedsMapList = tasteArtistSeeds.map { seed ->
                mapOf("id" to seed.id, "name" to seed.name, "imageUrl" to seed.imageUrl)
            }
            val hasOnboarding = localStorage.hasCompletedOnboarding()

            val data = hashMapOf<String, Any>(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "displayName" to (displayName.ifBlank { user.displayName ?: "User" }),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "lastSyncedAt" to now,
                "likedCount" to likedTracks.size,
                "playlistsCount" to customPlaylists.size,
                "totalScrobbles" to playbackStats.totalScrobbles,
                "likedTracksJson" to gson.toJson(likedTracks),
                "customPlaylistsJson" to gson.toJson(customPlaylists),
                "playbackStatsJson" to gson.toJson(playbackStats),
                "onboardingCompleted" to hasOnboarding,
                "hasCompletedOnboarding" to hasOnboarding,
                "selectedLanguages" to tasteLangs,
                "selectedGenres" to tasteGenres,
                "selectedArtists" to tasteArtists,
                "selectedArtistSeeds" to tasteArtistSeedsMapList
            )

            val userDocRef = firestore.collection("users").document(user.uid)
            userDocRef.set(data, SetOptions.merge()).await()

            // Also persist to users/{uid}/sync/data to support any recursive or subcollection security rules
            try {
                userDocRef.collection("sync").document("data")
                    .set(data, SetOptions.merge())
                    .await()
            } catch (_: Exception) {}

            _syncStatus.value = SyncStatus.SYNCED
            _lastSyncedTimestamp.value = now
            localStorage.saveLastSyncedTime(now)
            true
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.ERROR
            false
        }
    }
}
