package com.vybe.music.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.firebase.auth.FirebaseUser
import com.vybe.music.data.AppUpdateInfo
import com.vybe.music.data.AppUpdateManager
import com.vybe.music.data.FirebaseSyncManager
import com.vybe.music.data.LocalMusicStorage
import com.vybe.music.data.OfflineDownloadManager
import com.vybe.music.data.PlaybackStats
import com.vybe.music.data.SyncStatus
import com.vybe.music.data.UpdateDownloadState
import com.vybe.music.data.UserSessionManager
import com.vybe.music.data.audio.VideoCandidate
import com.vybe.music.data.audio.VideoQualityStream
import com.vybe.music.data.audio.VideoStreamResult
import com.vybe.music.data.audio.YouTubeAudioExtractor
import com.vybe.music.data.audio.YouTubeVideoExtractor
import com.vybe.music.data.repository.ArtistRepository
import com.vybe.music.data.repository.GlobalChartRepository
import com.vybe.music.data.repository.MusicRepository
import com.vybe.music.data.repository.MusicRepositoryImpl
import com.vybe.music.model.AlbumDetail
import com.vybe.music.model.ArtistDetail
import com.vybe.music.model.ArtistSeed
import com.vybe.music.model.AudioQuality
import com.vybe.music.ui.components.VybeBannerData
import com.vybe.music.model.AudioTrackFormatInfo
import com.vybe.music.model.CustomPlaylist
import com.vybe.music.model.NavTab
import com.vybe.music.model.RepeatMode
import com.vybe.music.model.Track
import com.vybe.music.service.VybePlaybackService
import com.vybe.music.utils.PaletteUtils
import com.vybe.music.utils.TrackUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel @JvmOverloads constructor(
    application: Application,
    val musicRepository: MusicRepository = MusicRepositoryImpl()
) : AndroidViewModel(application) {

    private var mediaController: MediaController? = null
    private var pendingTrackToPlay: Track? = null
    private var pendingQueueToPlay: List<Track>? = null
    private var pendingStartIndex: Int = 0

    private val localMusicStorage = LocalMusicStorage(getApplication())

    private val firebaseSyncManager = FirebaseSyncManager(getApplication(), localMusicStorage)

    val currentUser: StateFlow<FirebaseUser?> = firebaseSyncManager.currentUser
    val syncStatus: StateFlow<SyncStatus> = firebaseSyncManager.syncStatus
    val lastSyncedTimestamp: StateFlow<Long> = firebaseSyncManager.lastSyncedTimestamp

    private val _userPhotoUrl = MutableStateFlow<String?>(
        firebaseSyncManager.currentUser.value?.photoUrl?.toString() ?: localMusicStorage.getCachedPhotoUrl()
    )
    val userPhotoUrl: StateFlow<String?> = _userPhotoUrl.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(
        firebaseSyncManager.currentUser.value?.email ?: localMusicStorage.getCachedEmail()
    )
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _showAccountSync = MutableStateFlow(false)
    val showAccountSync: StateFlow<Boolean> = _showAccountSync.asStateFlow()

    // ── Personalized Onboarding Taste Preferences ──
    private val _hasCompletedOnboarding = MutableStateFlow(localMusicStorage.hasCompletedOnboarding())
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _isOnboardingOpen = MutableStateFlow(false)
    val isOnboardingOpen: StateFlow<Boolean> = _isOnboardingOpen.asStateFlow()

    private val _selectedLanguages = MutableStateFlow(localMusicStorage.getSelectedLanguages())
    val selectedLanguages: StateFlow<Set<String>> = _selectedLanguages.asStateFlow()

    private val _selectedGenres = MutableStateFlow(localMusicStorage.getSelectedGenres())
    val selectedGenres: StateFlow<Set<String>> = _selectedGenres.asStateFlow()

    private val _selectedArtists = MutableStateFlow(localMusicStorage.getSelectedArtists())
    val selectedArtists: StateFlow<Set<String>> = _selectedArtists.asStateFlow()

    fun completeOnboarding(
        languages: Set<String>,
        genres: Set<String>,
        artists: Set<String>,
        artistSeeds: List<ArtistSeed> = emptyList()
    ) {
        localMusicStorage.saveTastePreferences(languages, genres, artists, artistSeeds, completed = true)
        _hasCompletedOnboarding.value = true
        _isOnboardingOpen.value = false
        _selectedLanguages.value = languages
        _selectedGenres.value = genres
        _selectedArtists.value = artists
        artists.firstOrNull()?.let { _selectedArtist.value = it }
        scheduleCloudSync()
        generatePersonalizedFeed()
    }

    fun openTasteOnboarding() {
        _isOnboardingOpen.value = true
    }

    fun closeTasteOnboarding() {
        _isOnboardingOpen.value = false
    }

    fun openAccountSync() {
        _selectedTab.value = NavTab.SETTINGS
        _showAccountSync.value = true
    }

    fun openSettings() {
        _showAccountSync.value = false
        _selectedTab.value = NavTab.SETTINGS
    }

    fun closeAccountSync() {
        _showAccountSync.value = false
    }

    private val appUpdateManager = AppUpdateManager(getApplication())
    val appUpdateInfo: StateFlow<AppUpdateInfo?> = appUpdateManager.updateInfo
    val updateDownloadState: StateFlow<UpdateDownloadState> = appUpdateManager.downloadState
    val updateDownloadProgress: StateFlow<Float> = appUpdateManager.downloadProgress
    val isUpdateDismissed: StateFlow<Boolean> = appUpdateManager.isDismissed

    fun startAppUpdate(context: Context) {
        viewModelScope.launch {
            appUpdateManager.startApkDownloadAndInstall(context)
        }
    }

    fun dismissAppUpdate() {
        appUpdateManager.dismissUpdate()
    }

    private val _bannerMessage = MutableStateFlow<VybeBannerData?>(null)
    val bannerMessage: StateFlow<VybeBannerData?> = _bannerMessage.asStateFlow()

    fun showBanner(message: String, icon: ImageVector? = null) {
        _bannerMessage.value = VybeBannerData(message = message, icon = icon)
    }

    fun dismissBanner() {
        _bannerMessage.value = null
    }

    private val offlineDownloadManager = OfflineDownloadManager.getInstance(
        getApplication(),
        localMusicStorage,
        onBannerMessage = { message, icon ->
            showBanner(message, icon)
        }
    ).apply {
        setBannerCallback { message, icon -> showBanner(message, icon) }
    }

    val downloadedTracks: StateFlow<List<Track>> = offlineDownloadManager.downloadedTracks
    val downloadedTrackIds: StateFlow<Set<String>> = offlineDownloadManager.downloadedTrackIds
    val downloadingTrackIds: StateFlow<Set<String>> = offlineDownloadManager.downloadingTrackIds
    val queuedTrackIds: StateFlow<Set<String>> = offlineDownloadManager.queuedTrackIds
    val activePlaylistDownloads: StateFlow<Map<String, com.vybe.music.data.PlaylistDownloadProgress>> = offlineDownloadManager.activePlaylistDownloads
    val playlistProgress = offlineDownloadManager.playlistProgress

    private val _localTracks = MutableStateFlow<List<Track>>(emptyList())
    val localTracks: StateFlow<List<Track>> = _localTracks.asStateFlow()

    private val _isScanningLocalAudio = MutableStateFlow(false)
    val isScanningLocalAudio: StateFlow<Boolean> = _isScanningLocalAudio.asStateFlow()

    fun scanLocalAudio() {
        viewModelScope.launch {
            _isScanningLocalAudio.value = true
            try {
                val tracks = com.vybe.music.data.repository.LocalAudioRepository.queryDeviceAudio(getApplication())
                _localTracks.value = tracks
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error scanning local audio: ${e.message}", e)
            } finally {
                _isScanningLocalAudio.value = false
            }
        }
    }

    private val _queue = MutableStateFlow<List<Track>>(
        localMusicStorage.getPlaybackStats().recentTracks
            .ifEmpty { localMusicStorage.getDownloadedTracks() }
            .ifEmpty { localMusicStorage.getCachedTrendingTracks() ?: emptyList() }
    )
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(_queue.value.firstOrNull())
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(_currentTrack.value?.durationMs ?: 200000L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isYouTubeResolving = MutableStateFlow(false)
    val isYouTubeResolving: StateFlow<Boolean> = _isYouTubeResolving.asStateFlow()

    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    private val _likedTracks = MutableStateFlow<List<Track>>(localMusicStorage.getLikedTracks())
    val likedTracks: StateFlow<List<Track>> = _likedTracks.asStateFlow()

    private val _favoriteTrackIds = MutableStateFlow<Set<String>>(
        (localMusicStorage.getFavoriteTrackIds() + localMusicStorage.getLikedTracks().map { it.id }).toSet()
    )
    val favoriteTrackIds: StateFlow<Set<String>> = _favoriteTrackIds.asStateFlow()

    private val _customPlaylists = MutableStateFlow<List<CustomPlaylist>>(localMusicStorage.getCustomPlaylists())
    val customPlaylists: StateFlow<List<CustomPlaylist>> = _customPlaylists.asStateFlow()

    private val _playbackStats = MutableStateFlow<PlaybackStats>(localMusicStorage.getPlaybackStats())
    val playbackStats: StateFlow<PlaybackStats> = _playbackStats.asStateFlow()

    private val _currentAudioFormat = MutableStateFlow(AudioTrackFormatInfo())
    val currentAudioFormat: StateFlow<AudioTrackFormatInfo> = _currentAudioFormat.asStateFlow()

    private val _streamingQuality = MutableStateFlow(localMusicStorage.getStreamingQuality())
    val streamingQuality: StateFlow<AudioQuality> = _streamingQuality.asStateFlow()

    private val _displayName = MutableStateFlow(localMusicStorage.getDisplayName())
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _selectedTab = MutableStateFlow(NavTab.HOME)
    val selectedTab: StateFlow<NavTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(localMusicStorage.getRecentSearches())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            localMusicStorage.addRecentSearch(trimmed)
            _recentSearches.value = localMusicStorage.getRecentSearches()
        }
    }

    fun removeRecentSearch(query: String) {
        localMusicStorage.removeRecentSearch(query)
        _recentSearches.value = localMusicStorage.getRecentSearches()
    }

    fun clearRecentSearches() {
        localMusicStorage.clearRecentSearches()
        _recentSearches.value = emptyList()
    }

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

    private val _trendingTracks = MutableStateFlow<List<Track>>(
        localMusicStorage.getCachedTrendingTracks() ?: emptyList()
    )
    val trendingTracks: StateFlow<List<Track>> = _trendingTracks.asStateFlow()

    private val _top5DailyTracks = MutableStateFlow<List<Track>>(emptyList())
    val top5DailyTracks: StateFlow<List<Track>> = _top5DailyTracks.asStateFlow()

    private val _quickPicks = MutableStateFlow<List<Track>>(emptyList())
    val quickPicks: StateFlow<List<Track>> = _quickPicks.asStateFlow()

    private val _freshFinds = MutableStateFlow<List<Track>>(emptyList())
    val freshFinds: StateFlow<List<Track>> = _freshFinds.asStateFlow()

    private val _selectedArtist = MutableStateFlow(
        localMusicStorage.getUserTasteProfile().topArtists.firstOrNull() ?: "Arijit Singh"
    )
    val selectedArtist: StateFlow<String> = _selectedArtist.asStateFlow()

    private val _artistSpotlightTracks = MutableStateFlow<List<Track>>(emptyList())
    val artistSpotlightTracks: StateFlow<List<Track>> = _artistSpotlightTracks.asStateFlow()

    private val _isArtistLoading = MutableStateFlow(false)
    val isArtistLoading: StateFlow<Boolean> = _isArtistLoading.asStateFlow()

    private val _isHomeLoading = MutableStateFlow(false)
    val isHomeLoading: StateFlow<Boolean> = _isHomeLoading.asStateFlow()

    private val _currentArtistDetail = MutableStateFlow<ArtistDetail?>(null)
    val currentArtistDetail: StateFlow<ArtistDetail?> = _currentArtistDetail.asStateFlow()

    private val _isArtistDetailLoading = MutableStateFlow(false)
    val isArtistDetailLoading: StateFlow<Boolean> = _isArtistDetailLoading.asStateFlow()

    private val _currentAlbumDetail = MutableStateFlow<AlbumDetail?>(null)
    val currentAlbumDetail: StateFlow<AlbumDetail?> = _currentAlbumDetail.asStateFlow()

    private val _isAlbumLoading = MutableStateFlow(false)
    val isAlbumLoading: StateFlow<Boolean> = _isAlbumLoading.asStateFlow()

    private val _dynamicColor = MutableStateFlow(
        Color(_currentTrack.value?.defaultDominantColorHex ?: 0xFF1A1A1E)
    )
    val dynamicColor: StateFlow<Color> = _dynamicColor.asStateFlow()

    private val _darkVibrantColor = MutableStateFlow(Color(0xFF121216))
    val darkVibrantColor: StateFlow<Color> = _darkVibrantColor.asStateFlow()

    private val _vibrantColor = MutableStateFlow(Color(0xFFE50914))
    val vibrantColor: StateFlow<Color> = _vibrantColor.asStateFlow()

    private val _mutedColor = MutableStateFlow(Color(0xFF2B2B36))
    val mutedColor: StateFlow<Color> = _mutedColor.asStateFlow()

    private val _isVideoMode = MutableStateFlow(false)
    val isVideoMode: StateFlow<Boolean> = _isVideoMode.asStateFlow()

    private val _isVideoLoading = MutableStateFlow(false)
    val isVideoLoading: StateFlow<Boolean> = _isVideoLoading.asStateFlow()

    private var videoFallbackIndex: Int = 0

    private val _isVideoSynced = MutableStateFlow(true)
    val isVideoSynced: StateFlow<Boolean> = _isVideoSynced.asStateFlow()

    private val _currentVideoStream = MutableStateFlow<VideoStreamResult?>(null)
    val currentVideoStream: StateFlow<VideoStreamResult?> = _currentVideoStream.asStateFlow()

    private val _selectedVideoQuality = MutableStateFlow("HD Auto")
    val selectedVideoQuality: StateFlow<String> = _selectedVideoQuality.asStateFlow()

    private val _alternateVideos = MutableStateFlow<List<VideoCandidate>>(emptyList())
    val alternateVideos: StateFlow<List<VideoCandidate>> = _alternateVideos.asStateFlow()

    private val _isSearchingAlternates = MutableStateFlow(false)
    val isSearchingAlternates: StateFlow<Boolean> = _isSearchingAlternates.asStateFlow()

    private val _mediaControllerState = MutableStateFlow<MediaController?>(null)
    val mediaControllerState: StateFlow<MediaController?> = _mediaControllerState.asStateFlow()
    val videoPlayerState: StateFlow<Player?> = _mediaControllerState.asStateFlow()

    private var progressJob: Job? = null
    private var playJob: Job? = null
    private var videoSwitchJob: Job? = null  // Tracks background video extraction so it can be cancelled on track skip
    private var originalQueue: List<Track> = emptyList()
    private var isSwitchingStreamMode = false
    private var isLoadingNewTrack = false
    private var currentTrackListenedMs = 0L
    private var hasScrobbledCurrentTrack = false
    private var pendingListeningTimeAccumulatorMs = 0L
    private var lastProgressSampleTime = 0L
    private var lastVideoSyncTime = 0L
    private var isFetchingUpcoming = false
    private val playedTrackIdsInSession = mutableSetOf<String>()
    private var pendingSeekPositionMs: Long? = null
    private var seekRequestTimestamp: Long = 0L

    init {
        YouTubeAudioExtractor.clearCache()
        initializeMediaController()
        loadGlobalTop5Daily(forceRefresh = false)
        loadTrendingTracks()
        generatePersonalizedFeed()
        observeSearchQuery()

        currentTrack.value?.let { track ->
            extractPaletteForTrack(track)
        }

        // Sync legacy favorite IDs into full liked tracks if needed
        if (_likedTracks.value.isEmpty() && _favoriteTrackIds.value.isNotEmpty()) {
            val allKnown = (localMusicStorage.getCachedTrendingTracks().orEmpty() +
                localMusicStorage.getDownloadedTracks() +
                localMusicStorage.getPlaybackStats().recentTracks).distinctBy { it.id }
            val matched = allKnown.filter { _favoriteTrackIds.value.contains(it.id) }
            if (matched.isNotEmpty()) {
                _likedTracks.value = matched
                localMusicStorage.saveLikedTracks(matched)
            }
        }

        // Live artwork auto-repair listener: updates in-memory StateFlows immediately
        viewModelScope.launch {
            com.vybe.music.data.TrackArtworkRepairer.artworkRepairedFlow.collect { (trackId, newArtUrl) ->
                _likedTracks.value = _likedTracks.value.map { if (it.id == trackId) it.copy(artworkUrl = newArtUrl) else it }
                _customPlaylists.value = _customPlaylists.value.map { pl ->
                    pl.copy(tracks = pl.tracks.map { if (it.id == trackId) it.copy(artworkUrl = newArtUrl) else it })
                }
                _queue.value = _queue.value.map { if (it.id == trackId) it.copy(artworkUrl = newArtUrl) else it }
                if (_currentTrack.value?.id == trackId) {
                    _currentTrack.value = _currentTrack.value?.copy(artworkUrl = newArtUrl)
                }
            }
        }

        viewModelScope.launch {
            firebaseSyncManager.currentUser.collect { user ->
                _userPhotoUrl.value = user?.photoUrl?.toString() ?: localMusicStorage.getCachedPhotoUrl()
                _userEmail.value = user?.email ?: localMusicStorage.getCachedEmail()
                if (user != null && user.displayName.orEmpty().isNotBlank()) {
                    _displayName.value = user.displayName!!
                    localMusicStorage.saveDisplayName(user.displayName!!)
                }
            }
        }

        if (firebaseSyncManager.currentUser.value != null) {
            syncFromCloud()
        }

        if (com.vybe.music.data.repository.LocalAudioRepository.hasAudioPermission(getApplication())) {
            scanLocalAudio()
        }

        viewModelScope.launch {
            var lastUserId = UserSessionManager.currentUserId
            UserSessionManager.currentUserIdFlow.collect { newUserId ->
                if (newUserId != lastUserId) {
                    Log.d("PlayerViewModel", "Account switch observed: $lastUserId -> $newUserId")
                    lastUserId = newUserId
                    handleUserAccountSwitch(newUserId)
                }
            }
        }

        viewModelScope.launch {
            appUpdateManager.checkForUpdates()
        }
    }

    private fun initializeMediaController() {
        try {
            val sessionToken = SessionToken(
                getApplication(),
                ComponentName(getApplication(), VybePlaybackService::class.java)
            )
            val controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
            controllerFuture.addListener({
                try {
                    val controller = controllerFuture.get()
                    mediaController = controller
                    _mediaControllerState.value = controller
                    setupPlayerListener(controller)

                    val pendingList = pendingQueueToPlay
                    if (!pendingList.isNullOrEmpty()) {
                        pendingQueueToPlay = null
                        val idx = pendingStartIndex
                        pendingStartIndex = 0
                        playQueue(pendingList, idx)
                    } else {
                        pendingTrackToPlay?.let { track ->
                            pendingTrackToPlay = null
                            playTrack(track)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(getApplication()))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createMediaItem(track: Track): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(if (track.artworkUrl.isNotBlank()) Uri.parse(track.artworkUrl) else null)
            .build()

        // STRICTLY reject preview URLs! Never pass audio-ssl.itunes.apple.com to ExoPlayer
        val uriString = if (track.mediaUri.isNotBlank() && !YouTubeAudioExtractor.isPreviewUrl(track.mediaUri)) {
            track.mediaUri
        } else {
            "vybe://unresolved/${track.id}"
        }

        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(uriString)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun setupPlayerListener(controller: MediaController) {
        controller.volume = 1f
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    controller.volume = 1f
                    startProgressTracker()
                } else {
                    flushPendingListeningTime()
                    progressJob?.cancel()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _isBuffering.value = true
                    }
                    Player.STATE_READY -> {
                        isLoadingNewTrack = false
                        controller.volume = 1f
                        _isBuffering.value = false
                        if (_isVideoMode.value) {
                            _isVideoLoading.value = false
                            _isVideoSynced.value = true
                        }
                        val realDuration = controller.duration
                        // Only adopt ExoPlayer duration if it's a full-length track (> 45s), never a 30s preview
                        if (realDuration > 45000L) {
                            _durationMs.value = realDuration
                        }
                        val targetSeek = pendingSeekPositionMs
                        if (targetSeek != null && kotlin.math.abs(controller.currentPosition - targetSeek) <= 1500L) {
                            pendingSeekPositionMs = null
                        }
                    }
                    Player.STATE_ENDED -> {
                        _isBuffering.value = false
                        if (isLoadingNewTrack || isSwitchingStreamMode) {
                            Log.d("PlayerViewModel", "STATE_ENDED ignored because track is loading or switching stream mode")
                            return
                        }
                        if (_currentTrack.value == null || controller.mediaItemCount == 0) {
                            _isPlaying.value = false
                            return
                        }
                        when (_repeatMode.value) {
                            RepeatMode.ONE -> {
                                currentTrackListenedMs = 0L
                                hasScrobbledCurrentTrack = false
                                controller.seekTo(0)
                                controller.play()
                            }
                            RepeatMode.ALL -> {
                                if (controller.hasNextMediaItem()) {
                                    controller.seekToNextMediaItem()
                                    controller.play()
                                } else {
                                    controller.seekTo(0, 0L)
                                    controller.play()
                                }
                            }
                            RepeatMode.OFF -> {
                                if (controller.hasNextMediaItem()) {
                                    controller.seekToNextMediaItem()
                                    controller.play()
                                } else {
                                    ensureUpcomingQueue(controller)
                                    if (controller.hasNextMediaItem()) {
                                        controller.seekToNextMediaItem()
                                        controller.play()
                                    } else {
                                        _isPlaying.value = false
                                    }
                                }
                            }
                        }
                    }
                    Player.STATE_IDLE -> {
                        _isBuffering.value = false
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_AUDIO && group.isSelected) {
                        for (i in 0 until group.length) {
                            if (group.isTrackSelected(i)) {
                                val format = group.getTrackFormat(i)
                                updateAudioFormatFromExo(format)
                                return
                            }
                        }
                    }
                }
            }

            override fun onRenderedFirstFrame() {
                if (_isVideoMode.value) {
                    _isVideoLoading.value = false
                    _isVideoSynced.value = true
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val httpCode = (error.cause as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException)?.responseCode ?: -1
                Log.e(
                    "VYBE_PLAYER",
                    "PlayerViewModel onPlayerError [${error.errorCodeName} / code ${error.errorCode} / httpCode $httpCode]: ${error.message}",
                    error
                )
                if (_isVideoMode.value) {
                    val isRecoverable = error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
                            error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                            error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED

                    if (isRecoverable && attemptVideoFallback()) {
                        Log.i("PlayerViewModel", "Automatically attempted fallback stream for video playback")
                        return
                    }
                    Log.w("PlayerViewModel", "Video playback failed, falling back to studio audio mode")
                    toggleVideoMode(false)
                    showBanner("Video unavailable, playing audio", Icons.Rounded.VideocamOff)
                    return
                }
                handlePlaybackError(controller, error, httpCode)
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                if (isSwitchingStreamMode) {
                    Log.d("PlayerViewModel", "onMediaMetadataChanged ignored during stream mode switch")
                    return
                }
                val current = _currentTrack.value ?: return
                val newTitle = mediaMetadata.title?.toString()
                val newArtist = mediaMetadata.artist?.toString()
                if (!newTitle.isNullOrBlank() && newTitle != "Unknown Title" && current.title == "Unknown Title") {
                    _currentTrack.value = current.copy(title = newTitle, artist = newArtist ?: current.artist)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (isSwitchingStreamMode) {
                    Log.d("PlayerViewModel", "onMediaItemTransition ignored during stream mode switch")
                    return
                }
                if (mediaItem == null) {
                    _currentTrack.value = null
                    _isPlaying.value = false
                    return
                }
                val item = mediaItem
                Log.d("PlayerViewModel", "onMediaItemTransition: id=${item.mediaId}, reason=$reason")

                val wasInVideoMode = _isVideoMode.value
                val isSameTrack = _currentTrack.value?.id == item.mediaId
                if (isSameTrack) {
                    Log.d("PlayerViewModel", "Same track transition, ignoring")
                    return
                }

                val found = _queue.value.find { it.id == item.mediaId }
                val current = if (found != null) {
                    found
                } else {
                    val metadata = item.mediaMetadata
                    Track(
                        id = item.mediaId,
                        title = metadata.title?.toString() ?: "Unknown Title",
                        artist = metadata.artist?.toString() ?: "Unknown Artist",
                        album = metadata.albumTitle?.toString() ?: "",
                        durationMs = controller.duration.takeIf { it > 0 } ?: 180000L,
                        artworkUrl = metadata.artworkUri?.toString() ?: "",
                        mediaUri = item.requestMetadata.mediaUri?.toString() ?: ""
                    )
                }

                val prevTrack = _currentTrack.value
                val isSameTrackMetadata = prevTrack?.id == current.id ||
                    (prevTrack != null && PlaybackStats.normalizedKey(prevTrack) == PlaybackStats.normalizedKey(current))
                val isRepeat = reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT

                _currentTrack.value = current
                _durationMs.value = current.durationMs
                _playbackPositionMs.value = 0L

                // Flush pending listening time from previous track
                flushPendingListeningTime()

                if (!isSameTrackMetadata || isRepeat) {
                    currentTrackListenedMs = 0L
                    hasScrobbledCurrentTrack = false
                }
                lastProgressSampleTime = System.currentTimeMillis()
                playedTrackIdsInSession.add(current.id)
                extractPaletteForTrack(current)
                YouTubeAudioExtractor.getCachedTrackFormat(current.id)?.let {
                    _currentAudioFormat.value = it
                }

                if (wasInVideoMode) {
                    _isVideoLoading.value = true
                    toggleVideoMode(true)
                } else {
                    _isVideoMode.value = false
                    _isVideoLoading.value = false
                }

                // Ensure official studio audio stream is loaded (skip if offline track or currently in video mode)
                val isOffline = current.isLocal || current.mediaUri.startsWith("file:") || current.mediaUri.startsWith("content:") || offlineDownloadManager.isTrackDownloaded(current.id)
                if (isOffline) {
                    Log.d("PlayerViewModel", "Next track is offline downloaded: ${current.title}")
                } else if (!wasInVideoMode) {
                        playJob?.cancel()
                        playJob = viewModelScope.launch {
                            try {
                                if (current.mediaUri.isBlank() || YouTubeAudioExtractor.isPreviewUrl(current.mediaUri)) {
                                    _isBuffering.value = true
                                }
                                val fullAudioUrl = YouTubeAudioExtractor.extractAudioStream(current, _streamingQuality.value)
                                if (!isActive || _currentTrack.value?.id != current.id) return@launch

                                YouTubeAudioExtractor.getCachedTrackFormat(current.id)?.let {
                                    _currentAudioFormat.value = it
                                }
                                if (!fullAudioUrl.isNullOrBlank() && !YouTubeAudioExtractor.isPreviewUrl(fullAudioUrl)) {
                                    if (!isActive || _currentTrack.value?.id != current.id) return@launch

                                    val properDuration = if (current.durationMs > 45000L) current.durationMs else 210000L
                                    val upgradedTrack = current.copy(mediaUri = fullAudioUrl, durationMs = properDuration)
                                    _currentTrack.value = upgradedTrack
                                    _durationMs.value = properDuration
                                    _queue.value = _queue.value.map { if (it.id == current.id) upgradedTrack else it }
                                    val ctrl = mediaController
                                    if (ctrl != null && ctrl.currentMediaItem?.mediaId == current.id && _currentTrack.value?.id == current.id) {
                                        val currentPos = ctrl.currentPosition
                                        val isPlaying = ctrl.isPlaying
                                        val currentIdx = ctrl.currentMediaItemIndex
                                        val newMediaItem = createMediaItem(upgradedTrack)
                                        ctrl.replaceMediaItem(currentIdx, newMediaItem)
                                        if (currentPos > 0) ctrl.seekTo(currentPos)
                                        if (isPlaying) ctrl.play()
                                        _isBuffering.value = false
                                        Log.d("PlayerViewModel", "Seamlessly upgraded next track to studio stream: $fullAudioUrl")
                                    }
                                }
                            } catch (e: Exception) {
                                if (e !is kotlinx.coroutines.CancellationException) {
                                    Log.w("PlayerViewModel", "Next track stream upgrade error: ${e.message}")
                                }
                                _isBuffering.value = false
                            }
                        }
                    }

                    // Auto-Queue / Continuous Playback: check if we are near the end of the ExoPlayer queue
                    val currentIndex = controller.currentMediaItemIndex
                    val totalCount = controller.mediaItemCount
                    if (totalCount > 0 && currentIndex >= totalCount - 2) {
                        ensureUpcomingQueue(controller)
                    }
                }
            })
        }

    private fun ensureUpcomingQueue(controller: MediaController) {
        if (isFetchingUpcoming) return
        if (_repeatMode.value == RepeatMode.ONE) return

        isFetchingUpcoming = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentQueue = _queue.value
                val existingIds = (currentQueue.map { it.id } + playedTrackIdsInSession).toMutableSet()
                val existingTitles = currentQueue.map { it.title.lowercase().trim() }.toMutableSet()

                val taste = localMusicStorage.getUserTasteProfile()
                val current = _currentTrack.value
                val primaryArtist = current?.artist?.split(",", "&", "feat.", "ft.", "x", "/")?.firstOrNull()?.trim()
                    ?: taste.topArtists.firstOrNull() ?: "The Weeknd"

                val fetchedCandidates = mutableListOf<Track>()

                // 1. Fetch tracks for the current primary artist or user's top artist
                val artistResult = musicRepository.getArtistTracks(primaryArtist)
                artistResult.getOrNull()?.let { tracks ->
                    fetchedCandidates.addAll(tracks)
                }

                // 2. Fetch tracks based on user's taste seed queries
                val seed = taste.seedQueries.randomOrNull() ?: "Top Hits"
                val searchResult = musicRepository.searchTracks(seed)
                searchResult.getOrNull()?.let { tracks ->
                    fetchedCandidates.addAll(tracks)
                }

                // 3. Fallback candidates from live trending
                _trendingTracks.value.let { fetchedCandidates.addAll(it) }

                // 4. Filter out recently played tracks in session and existing queue
                val dedupedCandidates = TrackUtils.deduplicateTracks(fetchedCandidates)
                val freshTracks = dedupedCandidates.filter { track ->
                    val key = TrackUtils.normalizeTrackKey(track.title, track.artist)
                    val isNewId = track.id !in existingIds
                    val isNewKey = key !in existingTitles
                    if (isNewId && isNewKey) {
                        existingIds.add(track.id)
                        existingTitles.add(key)
                        true
                    } else {
                        false
                    }
                }.take(20)

                if (freshTracks.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val newMediaItems = freshTracks.map { createMediaItem(it) }
                        _queue.value = currentQueue + freshTracks
                        try {
                            controller.addMediaItems(newMediaItems)
                            Log.d("PlayerViewModel", "ensureUpcomingQueue: successfully appended ${freshTracks.size} fresh tracks to queue")
                        } catch (e: Exception) {
                            Log.e("PlayerViewModel", "Error adding media items: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error in ensureUpcomingQueue: ${e.message}")
            } finally {
                isFetchingUpcoming = false
            }
        }
    }

    fun loadTrendingTracks(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                val liveTracks = GlobalChartRepository.fetchLiveTrendingCharts(
                    storage = localMusicStorage,
                    forceRefresh = forceRefresh
                )
                val deduped = TrackUtils.deduplicateTracks(liveTracks)
                if (deduped.isNotEmpty()) {
                    _trendingTracks.value = deduped
                    if (_queue.value.isEmpty()) {
                        _queue.value = deduped
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isCleanSingleTrack(track: Track): Boolean {
        val durationSec = track.durationMs / 1000L
        if (durationSec > 540L || durationSec < 60L) return false
        val lower = "${track.title} ${track.artist} ${track.album}".lowercase()
        val spamKeywords = listOf(
            "trending songs", "2026", "2025", "instagram", "nonstop", "jukebox",
            "mashup", "playlist", "compilation", "best songs", "fall in love",
            "top 50", "1 hour", "full album", "all songs"
        )
        return spamKeywords.none { lower.contains(it) }
    }

    fun generatePersonalizedFeed() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val taste = localMusicStorage.getUserTasteProfile()
                val userArtists = _selectedArtists.value.toList().ifEmpty { taste.selectedArtists.toList() }
                val topArtist = userArtists.firstOrNull()
                    ?: taste.topArtists.firstOrNull()
                    ?: "Featured"
                withContext(Dispatchers.Main) {
                    _selectedArtist.value = topArtist
                }

                val stats = localMusicStorage.getPlaybackStats()
                val downloaded = localMusicStorage.getDownloadedTracks()
                val liked = localMusicStorage.getLikedTracks()
                val trending = _trendingTracks.value.ifEmpty {
                    localMusicStorage.getCachedTrendingTracks() ?: emptyList()
                }

                val candidatePool = mutableListOf<Track>()

                // 1. Fetch real official tracks rotating across ALL of user's selected artists
                if (userArtists.isNotEmpty()) {
                    for (artist in userArtists) {
                        try {
                            val res = musicRepository.getArtistTracks(artist)
                            res.getOrNull()?.let { tracks ->
                                candidatePool.addAll(tracks.filter { isCleanSingleTrack(it) }.take(4))
                            }
                        } catch (_: Exception) {}
                    }
                }

                // 2. Add liked and recent listening history
                candidatePool.addAll(liked.filter { isCleanSingleTrack(it) })
                candidatePool.addAll(stats.recentTracks.filter { isCleanSingleTrack(it) })
                candidatePool.addAll(downloaded.filter { isCleanSingleTrack(it) })

                // 3. Fallback seeds and trending
                for (seed in taste.seedQueries.take(4)) {
                    val searchRes = musicRepository.searchTracks(seed)
                    searchRes.getOrNull()?.let { tracks ->
                        candidatePool.addAll(tracks.filter { isCleanSingleTrack(it) })
                    }
                }
                candidatePool.addAll(trending.filter { isCleanSingleTrack(it) })

                val deduped = TrackUtils.deduplicateTracks(candidatePool)
                val quickPicks = deduped.take(16)
                val freshFinds = deduped.drop(16).take(16).ifEmpty { deduped.shuffled().take(16) }

                withContext(Dispatchers.Main) {
                    _quickPicks.value = quickPicks
                    _freshFinds.value = freshFinds
                }

                if (topArtist.isNotBlank() && topArtist != "Featured") {
                    selectArtist(topArtist)
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error generating personalized feed: ${e.message}")
            }
        }
    }

    fun selectArtist(artistName: String) {
        _selectedArtist.value = artistName
        viewModelScope.launch {
            _isArtistLoading.value = true
            try {
                val result = musicRepository.getArtistTracks(artistName)
                result.onSuccess { tracks ->
                    if (tracks.isNotEmpty()) {
                        _artistSpotlightTracks.value = tracks
                    }
                }.onFailure {
                    val matches = _trendingTracks.value.filter {
                        it.artist.contains(artistName, ignoreCase = true)
                    }
                    if (matches.isNotEmpty()) {
                        _artistSpotlightTracks.value = matches
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isArtistLoading.value = false
            }
        }
    }

    fun searchArtist(artistName: String) {
        _searchQuery.value = artistName
        _selectedTab.value = NavTab.SEARCH
    }

    fun loadArtistDetail(artistName: String) {
        _selectedArtist.value = artistName
        _isArtistDetailLoading.value = true
        viewModelScope.launch {
            try {
                val detail = ArtistRepository.getArtistDetail(artistName, musicRepository)
                _currentArtistDetail.value = detail
                if (detail.topSongs.isNotEmpty()) {
                    _artistSpotlightTracks.value = detail.topSongs
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error loading artist detail: ${e.message}", e)
            } finally {
                _isArtistDetailLoading.value = false
            }
        }
    }

    fun playArtist(artistName: String, shuffle: Boolean = false) {
        loadArtistDetail(artistName)
        viewModelScope.launch {
            val tracks = _currentArtistDetail.value?.topSongs?.takeIf { it.isNotEmpty() }
                ?: musicRepository.getArtistTracks(artistName).getOrNull()
                ?: emptyList()
            if (tracks.isNotEmpty()) {
                playQueue(tracks, startIndex = 0, shuffle = shuffle)
            }
        }
    }

    fun openAlbum(
        browseId: String? = null,
        albumTitle: String,
        artistName: String,
        artworkUrl: String = "",
        year: String = ""
    ) {
        _isAlbumLoading.value = true
        _currentAlbumDetail.value = AlbumDetail(
            id = browseId ?: "${albumTitle}_${artistName}".hashCode().toString(),
            title = albumTitle,
            artist = artistName,
            year = year,
            artworkUrl = artworkUrl,
            browseId = browseId
        )
        viewModelScope.launch {
            try {
                val detail = com.vybe.music.data.repository.AlbumRepository.getAlbumDetails(
                    browseId = browseId,
                    albumTitle = albumTitle,
                    artistName = artistName,
                    fallbackArtwork = artworkUrl,
                    fallbackYear = year
                )
                if (detail != null) {
                    _currentAlbumDetail.value = detail
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error loading album detail for '$albumTitle': ${e.message}", e)
            } finally {
                _isAlbumLoading.value = false
            }
        }
    }

    fun closeAlbum() {
        _currentAlbumDetail.value = null
    }

    fun navigateToSearch() {
        _selectedTab.value = NavTab.SEARCH
    }

    fun loadGlobalTop5Daily(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                val tracks = GlobalChartRepository.fetchGlobalTop5Daily(
                    storage = localMusicStorage,
                    forceRefresh = forceRefresh
                )
                val deduped = TrackUtils.deduplicateTracks(tracks)
                if (deduped.isNotEmpty()) {
                    _top5DailyTracks.value = deduped
                    Log.d("PlayerViewModel", "Loaded ${deduped.size} tracks into Top 5 Daily StateFlow")
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Failed to fetch Global Top 5 Daily: ${e.message}")
            }
        }
    }

    fun refreshRecommendations() {
        loadGlobalTop5Daily(forceRefresh = true)
        loadTrendingTracks(forceRefresh = true)
        generatePersonalizedFeed()
    }

    fun filterHomeByMood(mood: String) {
        if (mood.equals("All", ignoreCase = true)) {
            refreshRecommendations()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isHomeLoading.value = true
            showBanner("Loading $mood music...")
            try {
                val query = when (mood.lowercase().trim()) {
                    "podcasts" -> "Podcasts"
                    "workout" -> "Workout hits music"
                    "feel good" -> "Feel good upbeat songs"
                    "relax" -> "Relax chill acoustic"
                    "focus" -> "Focus instrumental lo-fi"
                    "party" -> "Party dance hits"
                    else -> "$mood songs official music"
                }
                val res = musicRepository.searchTracks(query)
                val rawTracks = res.getOrNull()?.let { TrackUtils.deduplicateTracks(it) } ?: emptyList()
                val tracks = rawTracks.filter { !YouTubeAudioExtractor.isUnofficialUpload(it.title, it.artist) }
                if (tracks.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        _trendingTracks.value = tracks
                        _quickPicks.value = tracks.take(12)
                        _freshFinds.value = tracks.drop(12).ifEmpty { tracks.shuffled().take(12) }
                    }
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error filtering by mood: ${e.message}")
            } finally {
                _isHomeLoading.value = false
            }
        }
    }

    fun startGenreMix(genre: String) {
        viewModelScope.launch(Dispatchers.IO) {
            showBanner("Starting $genre mix")
            try {
                val query = "best $genre songs official audio"
                val res = musicRepository.searchTracks(query)
                val rawTracks = res.getOrNull()?.let { TrackUtils.deduplicateTracks(it) } ?: emptyList()
                val tracks = rawTracks.filter { !YouTubeAudioExtractor.isUnofficialUpload(it.title, it.artist) }
                val finalTracks = tracks.take(20).ifEmpty { rawTracks.take(20) }
                if (finalTracks.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        playQueue(finalTracks, startIndex = 0)
                    }
                } else {
                    showBanner("Could not find songs for $genre")
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error starting genre mix: ${e.message}")
            }
        }
    }

    fun startDiscoverMix() {
        viewModelScope.launch(Dispatchers.IO) {
            showBanner("Starting Discover mix")
            try {
                val taste = localMusicStorage.getUserTasteProfile()
                val genres = _selectedGenres.value.ifEmpty { taste.selectedGenres }.ifEmpty { listOf("Pop", "Rock", "Indie") }
                val languages = _selectedLanguages.value.ifEmpty { taste.selectedLanguages }
                val seedQueries = mutableListOf<String>()
                for (g in genres.take(3)) {
                    for (l in languages.take(2)) {
                        seedQueries.add("latest $l $g songs official")
                    }
                    if (languages.isEmpty()) {
                        seedQueries.add("latest $g songs official")
                    }
                }
                if (seedQueries.isEmpty()) {
                    seedQueries.add("trending discover new music songs official")
                }
                val candidatePool = mutableListOf<Track>()
                for (query in seedQueries.take(3)) {
                    val res = musicRepository.searchTracks(query)
                    res.getOrNull()?.let { candidatePool.addAll(it) }
                }
                val recentTrackIds = localMusicStorage.getPlaybackStats().recentTracks.map { it.id }.toSet()
                val fresh = candidatePool.filter { it.id !in recentTrackIds }
                val pool = if (fresh.size >= 10) fresh else candidatePool
                val deduped = TrackUtils.deduplicateTracks(pool)
                    .filter { !YouTubeAudioExtractor.isUnofficialUpload(it.title, it.artist) }
                    .take(20)
                if (deduped.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        playQueue(deduped, startIndex = 0)
                    }
                } else {
                    val fallback = _trendingTracks.value.shuffled().take(20)
                    if (fallback.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            playQueue(fallback, startIndex = 0)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error starting discover mix: ${e.message}")
            }
        }
    }

    fun startPersonalizedMix() {
        viewModelScope.launch(Dispatchers.IO) {
            showBanner("Starting Your Mix")
            try {
                val taste = localMusicStorage.getUserTasteProfile()
                val topArtists = (taste.topArtists + taste.selectedArtists).distinct().filter { it.isNotBlank() }.take(3)
                val candidatePool = mutableListOf<Track>()
                if (topArtists.isNotEmpty()) {
                    for (artist in topArtists) {
                        val res = musicRepository.searchTracks("$artist best songs official")
                        res.getOrNull()?.let { candidatePool.addAll(it) }
                    }
                }
                if (candidatePool.isEmpty()) {
                    candidatePool.addAll(_likedTracks.value)
                    candidatePool.addAll(_trendingTracks.value)
                }
                val deduped = TrackUtils.deduplicateTracks(candidatePool)
                    .filter { !YouTubeAudioExtractor.isUnofficialUpload(it.title, it.artist) }
                    .shuffled()
                    .take(25)
                if (deduped.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        playQueue(deduped, startIndex = 0)
                    }
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error starting personalized mix: ${e.message}")
            }
        }
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(400L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        _searchResults.value = emptyList()
                        _isSearching.value = false
                    } else {
                        _isSearching.value = true
                        try {
                            val result = musicRepository.searchTracks(query)
                            result.onSuccess { tracks ->
                                _searchResults.value = TrackUtils.deduplicateTracks(tracks)
                            }.onFailure {
                                _searchResults.value = emptyList()
                            }
                        } catch (e: Exception) {
                            _searchResults.value = emptyList()
                        }
                        _isSearching.value = false
                    }
                }
        }
    }

    fun playTrack(track: Track, contextQueue: List<Track>? = null) {
        // Determine full queue
        val fullList = when {
            !contextQueue.isNullOrEmpty() -> contextQueue
            _queue.value.any { it.id == track.id } -> _queue.value
            _searchResults.value.any { it.id == track.id } -> _searchResults.value
            _artistSpotlightTracks.value.any { it.id == track.id } -> _artistSpotlightTracks.value
            _trendingTracks.value.any { it.id == track.id } -> _trendingTracks.value
            else -> listOf(track) + _trendingTracks.value.filter { it.id != track.id }
        }

        val startIndex = fullList.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playQueue(fullList, startIndex)
    }

    fun playTrackList(tracks: List<Track>, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        playQueue(tracks, startIndex = 0, shuffle = shuffle)
    }

    fun playQueue(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return

        if (_isVideoMode.value) {
            _isVideoLoading.value = true
        }

        // Cancel previous in-flight stream resolution Job immediately
        playJob?.cancel()
        videoSwitchJob?.cancel()
        videoSwitchJob = null
        if (_isVideoMode.value) {
            _isVideoMode.value = false
            _isVideoLoading.value = false
            _currentVideoStream.value = null
        }

        val controller = mediaController
        if (controller == null) {
            val selected = tracks.getOrNull(startIndex) ?: tracks.first()
            pendingTrackToPlay = selected
            pendingQueueToPlay = tracks
            pendingStartIndex = startIndex
            _currentTrack.value = selected
            _playbackPositionMs.value = 0L
            _durationMs.value = selected.durationMs
            _queue.value = tracks
            _isPlaying.value = true
            return
        }

        // Immediately stop and clear media items before starting async stream lookup
        try {
            controller.stop()
            controller.clearMediaItems()
        } catch (e: Exception) {
            Log.e("VYBE_PLAYER", "Error stopping previous playback: ${e.message}", e)
        }

        val mappedTracks = tracks.map { t ->
            val localUri = offlineDownloadManager.getLocalFileUri(t.id)
            if (localUri != null) {
                t.copy(mediaUri = localUri.toString())
            } else {
                t
            }
        }
        originalQueue = mappedTracks

        val listToPlay = if (shuffle) {
            val selected = mappedTracks.getOrNull(startIndex) ?: mappedTracks.first()
            val remaining = mappedTracks.filter { it.id != selected.id }.shuffled()
            listOf(selected) + remaining
        } else {
            mappedTracks
        }
        val safeIndex = if (shuffle) 0 else startIndex.coerceIn(0, listToPlay.size - 1)
        val selectedTrack = listToPlay.getOrNull(safeIndex) ?: listToPlay.first()
        _isShuffleEnabled.value = shuffle
        isLoadingNewTrack = true

        val prevTrack = _currentTrack.value
        val isSameTrack = prevTrack?.id == selectedTrack.id ||
            (prevTrack != null && PlaybackStats.normalizedKey(prevTrack) == PlaybackStats.normalizedKey(selectedTrack))

        flushPendingListeningTime()
        if (!isSameTrack) {
            currentTrackListenedMs = 0L
            hasScrobbledCurrentTrack = false
        }
        lastProgressSampleTime = System.currentTimeMillis()
        playedTrackIdsInSession.add(selectedTrack.id)

        // Auto-Queue / Continuous Playback:
        // If queue is short or near the end, ensure fresh tracks are queued up
        if (listToPlay.size <= 2 || safeIndex >= listToPlay.size - 2) {
            ensureUpcomingQueue(controller)
        }

        _queue.value = listToPlay
        val current = listToPlay[safeIndex]
        _currentTrack.value = current
        _durationMs.value = current.durationMs
        _playbackPositionMs.value = 0L
        _dynamicColor.value = Color(current.defaultDominantColorHex)
        _darkVibrantColor.value = Color(0xFF121216)
        extractPaletteForTrack(current)
        YouTubeAudioExtractor.getCachedTrackFormat(current.id)?.let {
            _currentAudioFormat.value = it
        }

        val isOffline = current.isLocal || current.mediaUri.startsWith("file:") || current.mediaUri.startsWith("content:") || offlineDownloadManager.isTrackDownloaded(current.id)
        val needsResolution = !isOffline && (current.mediaUri.isBlank() || YouTubeAudioExtractor.isPreviewUrl(current.mediaUri))

        if (needsResolution) {
            _isBuffering.value = true
            _isPlaying.value = true

            playJob = viewModelScope.launch {
                try {
                    val fullAudioUrl = YouTubeAudioExtractor.extractAudioStream(current, _streamingQuality.value)

                    // Verify that resolved stream still matches latest currentTrack (discard stale async results)
                    if (!isActive || _currentTrack.value?.id != current.id) {
                        Log.d("VYBE_PLAYER", "Discarding stale stream resolution for '${current.title}' (latest is '${_currentTrack.value?.title}')")
                        return@launch
                    }

                    val properDuration = if (current.durationMs > 45000L) current.durationMs else 210000L

                    if (!fullAudioUrl.isNullOrBlank() && !YouTubeAudioExtractor.isPreviewUrl(fullAudioUrl)) {
                        if (!isActive || _currentTrack.value?.id != current.id) {
                            return@launch
                        }

                        val upgradedTrack = current.copy(mediaUri = fullAudioUrl, durationMs = properDuration)
                        _currentTrack.value = upgradedTrack
                        _durationMs.value = properDuration
                        val updatedList = listToPlay.map { if (it.id == current.id) upgradedTrack else it }
                        _queue.value = updatedList

                        YouTubeAudioExtractor.getCachedTrackFormat(current.id)?.let {
                            _currentAudioFormat.value = it
                        }

                        val ctrl = mediaController
                        if (ctrl != null && _currentTrack.value?.id == current.id) {
                            val mediaItems = updatedList.map { createMediaItem(it) }
                            ctrl.volume = 1f
                            ctrl.playbackParameters = androidx.media3.common.PlaybackParameters(1.0f, 1.0f)
                            ctrl.playWhenReady = true
                            ctrl.setMediaItems(mediaItems, safeIndex, 0L)
                            ctrl.prepare()
                            ctrl.play()
                            startProgressTracker()
                            _isBuffering.value = false
                            Log.d("VYBE_PLAYER", "playQueue: Resolved full stream for '${current.title}' and started playback: $fullAudioUrl")

                            val nextIdx = safeIndex + 1
                            if (nextIdx in updatedList.indices) {
                                val nextTrack = updatedList[nextIdx]
                                if (!nextTrack.isLocal) {
                                    viewModelScope.launch(Dispatchers.IO) {
                                        try {
                                            YouTubeAudioExtractor.preloadStream(nextTrack, _streamingQuality.value)
                                        } catch (_: Exception) {}
                                    }
                                }
                            }
                        }
                    } else {
                        Log.e("VYBE_PLAYER", "Failed to resolve full stream for '${current.title}'")
                        _isBuffering.value = false
                    }
                } catch (e: Exception) {
                    if (e !is kotlinx.coroutines.CancellationException) {
                        Log.e("VYBE_PLAYER", "Stream resolution error: ${e.message}", e)
                    }
                    _isBuffering.value = false
                }
            }
        } else {
            try {
                val mediaItems = listToPlay.map { createMediaItem(it) }
                controller.volume = 1f
                controller.playbackParameters = androidx.media3.common.PlaybackParameters(1.0f, 1.0f)
                controller.playWhenReady = true
                controller.setMediaItems(mediaItems, safeIndex, 0L)
                controller.prepare()
                controller.play()
                startProgressTracker()
                Log.d("VYBE_PLAYER", "playQueue: setMediaItems loaded ${mediaItems.size} items, startIndex=$safeIndex, playWhenReady=true")

                val nextIdx = safeIndex + 1
                if (nextIdx in listToPlay.indices) {
                    val nextTrack = listToPlay[nextIdx]
                    if (!nextTrack.isLocal) {
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                YouTubeAudioExtractor.preloadStream(nextTrack, _streamingQuality.value)
                            } catch (_: Exception) {}
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("VYBE_PLAYER", "playQueue error: ${e.message}", e)
            }

            if (!isOffline) {
                // Asynchronously extract official studio audio stream and seamlessly upgrade ExoPlayer
                playJob = viewModelScope.launch {
                    try {
                        val fullAudioUrl = YouTubeAudioExtractor.extractAudioStream(current, _streamingQuality.value)
                        if (!isActive || _currentTrack.value?.id != current.id) return@launch

                        YouTubeAudioExtractor.getCachedTrackFormat(current.id)?.let {
                            _currentAudioFormat.value = it
                        }
                        if (!fullAudioUrl.isNullOrBlank() && fullAudioUrl != current.mediaUri && !YouTubeAudioExtractor.isPreviewUrl(fullAudioUrl)) {
                            if (!isActive || _currentTrack.value?.id != current.id) return@launch

                            val upgradedTrack = current.copy(mediaUri = fullAudioUrl)
                            _currentTrack.value = upgradedTrack
                            _queue.value = _queue.value.map { if (it.id == current.id) upgradedTrack else it }

                            val ctrl = mediaController
                            if (ctrl != null && ctrl.currentMediaItem?.mediaId == current.id && _currentTrack.value?.id == current.id) {
                                val currentPos = ctrl.currentPosition
                                val isPlaying = ctrl.isPlaying
                                val currentIdx = ctrl.currentMediaItemIndex
                                val newMediaItem = createMediaItem(upgradedTrack)
                                ctrl.replaceMediaItem(currentIdx, newMediaItem)
                                if (currentPos > 0) ctrl.seekTo(currentPos)
                                ctrl.volume = 1f
                                if (isPlaying) ctrl.play()
                                Log.d("PlayerViewModel", "Seamlessly upgraded ExoPlayer to stream: $fullAudioUrl")
                            }
                        }
                    } catch (e: Exception) {
                        if (e !is kotlinx.coroutines.CancellationException) {
                            Log.w("PlayerViewModel", "Stream upgrade failed: ${e.message}")
                        }
                    }
                }
            } else {
                Log.d("PlayerViewModel", "Playing offline track '${current.title}' directly from: ${current.mediaUri}")
            }
        }
    }

    fun isTrackDownloaded(trackId: String): Boolean {
        return offlineDownloadManager.isTrackDownloaded(trackId)
    }

    fun toggleDownloadTrack(track: Track) {
        viewModelScope.launch {
            offlineDownloadManager.toggleDownload(track)
        }
    }

    fun deleteDownloadedTrack(trackId: String) {
        viewModelScope.launch {
            offlineDownloadManager.deleteDownloadedTrack(trackId)
        }
    }

    fun openSmartYouTubeVideo(track: Track) {
        toggleVideoMode(true)
    }

    private fun handlePlaybackError(controller: MediaController, error: PlaybackException, httpCode: Int = -1) {
        val current = _currentTrack.value ?: return
        Log.w("VYBE_PLAYER", "Playback error for '${current.title}' [${error.errorCodeName} / code ${error.errorCode} / httpCode $httpCode]. Checking automatic fallback stream...")

        playJob?.cancel()
        playJob = viewModelScope.launch {
            val fallback = YouTubeAudioExtractor.getNextFallbackStream(current.id, httpCode)
            if (!isActive || _currentTrack.value?.id != current.id) return@launch

            if (fallback != null && !YouTubeAudioExtractor.isPreviewUrl(fallback.url)) {
                Log.i("VYBE_PLAYER", "Automatically retrying '${current.title}' with fallback stream [${fallback.source}, itag=${fallback.itag}]: ${fallback.url}")
                val properDuration = if (current.durationMs > 45000L) current.durationMs else 210000L
                val upgradedTrack = current.copy(mediaUri = fallback.url, durationMs = properDuration)
                _currentTrack.value = upgradedTrack
                _durationMs.value = properDuration
                _queue.value = _queue.value.map { if (it.id == current.id) upgradedTrack else it }

                if (!isActive || _currentTrack.value?.id != current.id) return@launch

                val currentIdx = controller.currentMediaItemIndex
                val resumePos = maxOf(controller.currentPosition, _playbackPositionMs.value)
                val newMediaItem = createMediaItem(upgradedTrack)
                controller.replaceMediaItem(currentIdx, newMediaItem)
                if (resumePos > 0L) {
                    controller.seekTo(currentIdx, resumePos)
                }
                controller.prepare()
                controller.playWhenReady = true
                controller.volume = 1f
                controller.play()
            } else {
                // If no fallback candidate, perform full stream resolution
                val fullUrl = YouTubeAudioExtractor.extractAudioStream(current, _streamingQuality.value)
                if (!isActive || _currentTrack.value?.id != current.id) return@launch

                if (!fullUrl.isNullOrBlank() && !YouTubeAudioExtractor.isPreviewUrl(fullUrl)) {
                    val properDuration = if (current.durationMs > 45000L) current.durationMs else 210000L
                    val upgradedTrack = current.copy(mediaUri = fullUrl, durationMs = properDuration)
                    _currentTrack.value = upgradedTrack
                    _durationMs.value = properDuration
                    _queue.value = _queue.value.map { if (it.id == current.id) upgradedTrack else it }

                    if (!isActive || _currentTrack.value?.id != current.id) return@launch

                    val currentIdx = controller.currentMediaItemIndex
                    val resumePos = maxOf(controller.currentPosition, _playbackPositionMs.value)
                    val newMediaItem = createMediaItem(upgradedTrack)
                    controller.replaceMediaItem(currentIdx, newMediaItem)
                    if (resumePos > 0L) {
                        controller.seekTo(currentIdx, resumePos)
                    }
                    controller.prepare()
                    controller.playWhenReady = true
                    controller.volume = 1f
                    controller.play()
                } else {
                    Log.e("VYBE_PLAYER", "All fallback streams exhausted for '${current.title}'")
                    _isPlaying.value = false
                    _isBuffering.value = false
                }
            }
        }
    }

    private fun updateAudioFormatFromExo(format: Format) {
        val sampleMime = format.sampleMimeType ?: ""
        val codecs = format.codecs ?: ""
        val isOpus = codecs.contains("opus", ignoreCase = true) || sampleMime.contains("opus", ignoreCase = true) || sampleMime.contains("webm", ignoreCase = true)
        val isAac = codecs.contains("mp4a", ignoreCase = true) || sampleMime.contains("mp4", ignoreCase = true) || sampleMime.contains("aac", ignoreCase = true)
        val isFlac = sampleMime.contains("flac", ignoreCase = true)

        val codec = when {
            isOpus -> "Opus"
            isAac -> "AAC"
            isFlac -> "FLAC"
            else -> format.sampleMimeType?.substringAfter("/")?.uppercase()?.takeIf { it.isNotBlank() } ?: "Opus"
        }

        val bitrateKbps = when {
            format.bitrate > 0 -> (format.bitrate / 1000)
            format.averageBitrate > 0 -> (format.averageBitrate / 1000)
            else -> {
                val uri = _currentTrack.value?.mediaUri ?: ""
                when {
                    uri.contains("_320") -> 320
                    uri.contains("_160") -> 160
                    isOpus -> 160
                    isAac -> 256
                    else -> 320
                }
            }
        }

        val sampleRate = if (format.sampleRate > 0) format.sampleRate else 48000
        val sampleRateStr = if (sampleRate % 1000 == 0) "${sampleRate / 1000} kHz" else "%.1f kHz".format(sampleRate / 1000.0)
        val channelCount = if (format.channelCount > 0) format.channelCount else 2

        val label = "$codec • $bitrateKbps kbps • $sampleRateStr"
        _currentAudioFormat.value = AudioTrackFormatInfo(
            codec = codec,
            bitrateKbps = bitrateKbps,
            sampleRateHz = sampleRate,
            channelCount = channelCount,
            label = label
        )
        Log.i("PlayerViewModel", "Live ExoPlayer Audio Format: $label")
    }

    fun setStreamingQuality(quality: AudioQuality) {
        localMusicStorage.saveStreamingQuality(quality)
        _streamingQuality.value = quality
        val current = _currentTrack.value ?: return

        playJob?.cancel()
        playJob = viewModelScope.launch {
            try {
                val fullAudioUrl = YouTubeAudioExtractor.extractAudioStream(current, quality)
                if (!isActive || _currentTrack.value?.id != current.id) return@launch

                YouTubeAudioExtractor.getCachedTrackFormat(current.id)?.let {
                    _currentAudioFormat.value = it
                }
                if (!fullAudioUrl.isNullOrBlank() && fullAudioUrl != current.mediaUri) {
                    if (!isActive || _currentTrack.value?.id != current.id) return@launch
                    val upgradedTrack = current.copy(mediaUri = fullAudioUrl)
                    _currentTrack.value = upgradedTrack
                    _queue.value = _queue.value.map { if (it.id == current.id) upgradedTrack else it }
                    val ctrl = mediaController
                    if (ctrl != null && ctrl.currentMediaItem?.mediaId == current.id && _currentTrack.value?.id == current.id) {
                        val currentPos = ctrl.currentPosition
                        val isPlaying = ctrl.isPlaying
                        val currentIdx = ctrl.currentMediaItemIndex
                        val newMediaItem = createMediaItem(upgradedTrack)
                        ctrl.replaceMediaItem(currentIdx, newMediaItem)
                        if (currentPos > 0) ctrl.seekTo(currentPos)
                        ctrl.volume = 1f
                        if (isPlaying) ctrl.play()
                        Log.i("PlayerViewModel", "Upgraded live stream to quality ${quality.name}: $fullAudioUrl")
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.w("PlayerViewModel", "Quality switch error: ${e.message}")
                }
            }
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        try {
            if (controller.isPlaying) {
                controller.pause()
            } else {
                if ((controller.playbackState == Player.STATE_IDLE || controller.mediaItemCount == 0) && _currentTrack.value != null) {
                    _currentTrack.value?.let { playTrack(it) }
                } else {
                    controller.volume = 1f
                    controller.playWhenReady = true
                    controller.prepare()
                    controller.play()
                }
            }
        } catch (e: Exception) {
            Log.e("VYBE_PLAYER", "togglePlayPause error: ${e.message}", e)
        }
    }

    fun getProgressFraction(): Float {
        val dur = _durationMs.value
        val pos = _playbackPositionMs.value
        return if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        try {
            val bounded = positionMs.coerceIn(0L, _durationMs.value)
            pendingSeekPositionMs = bounded
            seekRequestTimestamp = System.currentTimeMillis()
            _playbackPositionMs.value = bounded
            controller.seekTo(bounded)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun nextTrack() {
        val controller = mediaController
        _playbackPositionMs.value = 0L
        if (_isVideoMode.value) {
            _isVideoLoading.value = true
        }
        val currentList = _queue.value
        if (currentList.isEmpty()) return
        val currentIndex = currentList.indexOfFirst { it.id == _currentTrack.value?.id }
        val nextIndex = if (currentIndex in 0 until currentList.size - 1) currentIndex + 1 else 0

        if (controller != null && controller.hasNextMediaItem()) {
            val totalCount = controller.mediaItemCount
            if (totalCount > 0 && currentIndex >= totalCount - 2) {
                ensureUpcomingQueue(controller)
            }
            controller.seekToNextMediaItem()
            controller.play()
            return
        }
        playTrack(currentList[nextIndex], currentList)
    }

    fun previousTrack() {
        val controller = mediaController
        _playbackPositionMs.value = 0L
        if (_isVideoMode.value) {
            _isVideoLoading.value = true
        }
        if (controller != null && controller.currentPosition > 3000L) {
            controller.seekTo(0L)
            return
        }
        val currentList = _queue.value
        if (currentList.isEmpty()) return
        val currentIndex = currentList.indexOfFirst { it.id == _currentTrack.value?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else currentList.size - 1

        if (controller != null && controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
            controller.play()
            return
        }
        playTrack(currentList[prevIndex], currentList)
    }

    fun toggleShuffle() {
        val newShuffle = !_isShuffleEnabled.value
        _isShuffleEnabled.value = newShuffle
        val controller = mediaController
        val current = _currentTrack.value

        if (newShuffle) {
            val baseList = if (originalQueue.isNotEmpty()) originalQueue else _queue.value
            if (current != null) {
                val remaining = baseList.filter { it.id != current.id }.shuffled()
                val shuffled = listOf(current) + remaining
                _queue.value = shuffled
                if (controller != null) {
                    try {
                        val mediaItems = shuffled.map { createMediaItem(it) }
                        val currentPos = controller.currentPosition
                        val isPlaying = controller.isPlaying
                        controller.setMediaItems(mediaItems, 0, currentPos)
                        controller.prepare()
                        if (isPlaying) controller.play()
                    } catch (e: Exception) {
                        Log.e("PlayerViewModel", "Error applying shuffle queue: ${e.message}", e)
                    }
                }
            }
        } else {
            if (originalQueue.isNotEmpty()) {
                val origIdx = if (current != null) originalQueue.indexOfFirst { it.id == current.id }.coerceAtLeast(0) else 0
                _queue.value = originalQueue
                if (controller != null) {
                    try {
                        val mediaItems = originalQueue.map { createMediaItem(it) }
                        val currentPos = controller.currentPosition
                        val isPlaying = controller.isPlaying
                        controller.setMediaItems(mediaItems, origIdx, currentPos)
                        controller.prepare()
                        if (isPlaying) controller.play()
                    } catch (e: Exception) {
                        Log.e("PlayerViewModel", "Error restoring original queue: ${e.message}", e)
                    }
                }
            }
        }
    }

    fun toggleRepeat() {
        val nextMode = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _repeatMode.value = nextMode
        try {
            mediaController?.repeatMode = when (nextMode) {
                RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun attemptVideoFallback(): Boolean {
        val streamResult = _currentVideoStream.value ?: return false
        val controller = mediaController ?: return false
        val current = _currentTrack.value ?: return false
        val fallbacks = streamResult.fallbackUrls
        if (videoFallbackIndex < fallbacks.size) {
            val nextUrl = fallbacks[videoFallbackIndex]
            videoFallbackIndex++
            Log.w("PlayerViewModel", "Attempting video stream fallback ($videoFallbackIndex/${fallbacks.size}): $nextUrl")
            val currentPos = controller.currentPosition
            val isPlaying = controller.isPlaying
            val currentIdx = controller.currentMediaItemIndex
            return try {
                val extras = Bundle().apply {
                    if (!streamResult.audioUrl.isNullOrBlank()) {
                        putString("video_audio_url", streamResult.audioUrl)
                    }
                    putString("is_video_mode", "true")
                }
                val videoMediaItem = MediaItem.Builder()
                    .setMediaId(current.id)
                    .setUri(nextUrl)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(current.title)
                            .setArtist(current.artist)
                            .setArtworkUri(if (current.artworkUrl.isNotBlank()) Uri.parse(current.artworkUrl) else null)
                            .setExtras(extras)
                            .build()
                    )
                    .build()
                controller.replaceMediaItem(currentIdx, videoMediaItem)
                controller.seekTo(currentIdx, currentPos)
                if (isPlaying) controller.play()
                true
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Fallback stream setup failed: ${e.message}")
                false
            }
        }
        return false
    }

    fun toggleVideoMode(enable: Boolean? = null) {
        val shouldEnable = enable ?: !_isVideoMode.value
        val activeTrack = _currentTrack.value ?: return
        val controller = mediaController

        // Cancel any previous in-flight video extraction immediately
        videoSwitchJob?.cancel()
        videoSwitchJob = null

        if (shouldEnable) {
            _isVideoMode.value = true
            _isVideoLoading.value = true
            _isVideoSynced.value = true
            videoFallbackIndex = 0

            videoSwitchJob = viewModelScope.launch(Dispatchers.IO) {
                // 8-second hard watchdog — revert to audio if extraction stalls
                val timeoutJob = launch {
                    delay(8000)
                    if (_isVideoLoading.value) {
                        Log.w("PlayerViewModel", "Video loading watchdog timeout fired after 8s")
                        withContext(Dispatchers.Main) {
                            _isVideoMode.value = false
                            _isVideoLoading.value = false
                            showBanner("Video unavailable, continuing audio", Icons.Rounded.VideocamOff)
                        }
                    }
                }
                try {
                    val videoResult = YouTubeVideoExtractor.extractVideoStream(activeTrack)
                    withContext(Dispatchers.Main) {
                        timeoutJob.cancel()
                        if (!isActive) return@withContext
                        if (videoResult != null && _isVideoMode.value && _currentTrack.value?.id == activeTrack.id) {
                            _currentVideoStream.value = videoResult
                            _selectedVideoQuality.value = "HD Auto"
                            videoFallbackIndex = 0
                            loadVideoStreamIntoPlayer(videoResult, activeTrack)
                        } else if (_isVideoMode.value) {
                            _isVideoMode.value = false
                            _isVideoLoading.value = false
                            showBanner("Official video not available for this track", Icons.Rounded.VideocamOff)
                        }
                    }
                } catch (e: Exception) {
                    timeoutJob.cancel()
                    if (!isActive) return@launch
                    Log.e("PlayerViewModel", "Failed to switch to video: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        _isVideoMode.value = false
                        _isVideoLoading.value = false
                        showBanner("Video unavailable, continuing audio", Icons.Rounded.VideocamOff)
                    }
                }
            }
        } else {
            _isVideoMode.value = false
            _isVideoLoading.value = false
            _currentVideoStream.value = null

            if (controller != null) {
                // Snapshot position BEFORE any swap so seek is atomic
                val currentPos = controller.currentPosition
                val isPlaying = controller.isPlaying
                val currentIdx = controller.currentMediaItemIndex

                val studioAudioUrl = YouTubeAudioExtractor.getCachedAudioStream(activeTrack.id)
                val audioTrack = if (!studioAudioUrl.isNullOrBlank() && studioAudioUrl != activeTrack.mediaUri) {
                    activeTrack.copy(mediaUri = studioAudioUrl)
                } else {
                    activeTrack
                }
                _currentTrack.value = audioTrack

                isSwitchingStreamMode = true
                try {
                    val audioMediaItem = createMediaItem(audioTrack)
                    controller.replaceMediaItem(currentIdx, audioMediaItem)
                    controller.seekTo(currentIdx, currentPos)
                    if (isPlaying) controller.play()
                    Log.d("PlayerViewModel", "Switched back from video to studio audio at ${currentPos}ms")
                } finally {
                    viewModelScope.launch {
                        delay(400)
                        isSwitchingStreamMode = false
                    }
                }
            }
        }
    }

    private fun loadVideoStreamIntoPlayer(streamResult: VideoStreamResult, activeTrack: Track) {
        val controller = mediaController ?: return
        val current = activeTrack
        // Snapshot position and playing state at the EXACT moment of swap — before any ExoPlayer mutation
        val resumePosMs = controller.currentPosition
        val wasPlaying = controller.isPlaying
        val currentIdx = controller.currentMediaItemIndex
        val videoDurationMs = if (streamResult.durationMs > 0) streamResult.durationMs else Long.MAX_VALUE

        Log.i("PlayerViewModel", "loadVideoStreamIntoPlayer: videoId=${streamResult.videoId}, title=${streamResult.title}, streamUrl=${streamResult.streamUrl.take(75)}..., audioUrl=${streamResult.audioUrl?.take(75)}..., resumePos=${resumePosMs}ms")

        isSwitchingStreamMode = true
        try {
            // Build extras: video_audio_url is consumed by VybePlaybackService.mediaSourceFactory
            // to create a MergingMediaSource(videoOnlySource, audioOnlySource) for hardware A/V sync
            val extras = Bundle().apply {
                if (!streamResult.audioUrl.isNullOrBlank()) {
                    putString("video_audio_url", streamResult.audioUrl)
                }
                putString("is_video_mode", "true")
            }

            val videoMediaItem = MediaItem.Builder()
                .setMediaId(current.id)
                .setUri(streamResult.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(current.title)
                        .setArtist(current.artist)
                        .setAlbumTitle(current.album)
                        .setArtworkUri(if (current.artworkUrl.isNotBlank()) Uri.parse(current.artworkUrl) else null)
                        .setExtras(extras)
                        .build()
                )
                .build()

            // Atomic swap: replace → seek to exact resume position (clamped to video duration) → restore play state
            controller.replaceMediaItem(currentIdx, videoMediaItem)
            controller.seekTo(currentIdx, resumePosMs.coerceAtMost(videoDurationMs))
            controller.playWhenReady = wasPlaying
            _isVideoSynced.value = true
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Failed to load video stream: ${e.message}", e)
            if (!attemptVideoFallback()) {
                _isVideoLoading.value = false
                showBanner("Failed to load video stream", Icons.Rounded.ErrorOutline)
            }
        } finally {
            // Clear switching flag once ExoPlayer reaches STATE_READY (via onPlaybackStateChanged)
            // but set a 600ms safety timeout in case STATE_READY fires before the flag clears
            viewModelScope.launch {
                delay(600)
                isSwitchingStreamMode = false
            }
        }
    }

    fun setVideoQuality(quality: String) {
        _selectedVideoQuality.value = quality
        val streamResult = _currentVideoStream.value ?: return
        val controller = mediaController ?: return
        val current = _currentTrack.value ?: return
        val targetUrl = if (quality.contains("Auto", ignoreCase = true)) {
            streamResult.streamUrl
        } else {
            val cleanQuality = quality.replace("HD ", "").trim()
            streamResult.availableQualities.firstOrNull {
                it.qualityLabel.contains(cleanQuality, ignoreCase = true)
            }?.url ?: streamResult.streamUrl
        }
        val currentPos = controller.currentPosition
        val isPlaying = controller.isPlaying
        val currentIdx = controller.currentMediaItemIndex
        try {
            val extras = Bundle().apply {
                if (!streamResult.audioUrl.isNullOrBlank()) {
                    putString("video_audio_url", streamResult.audioUrl)
                }
                putString("is_video_mode", "true")
            }
            val videoMediaItem = MediaItem.Builder()
                .setMediaId(current.id)
                .setUri(targetUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(current.title)
                        .setArtist(current.artist)
                        .setArtworkUri(if (current.artworkUrl.isNotBlank()) Uri.parse(current.artworkUrl) else null)
                        .setExtras(extras)
                        .build()
                )
                .build()
            controller.replaceMediaItem(currentIdx, videoMediaItem)
            controller.seekTo(currentIdx, currentPos)
            if (isPlaying) controller.play()
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Error switching video quality: ${e.message}")
        }
    }

    fun selectAlternateVideo(videoId: String) {
        _isVideoLoading.value = true
        _isVideoMode.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val timeoutJob = launch {
                delay(8000)
                if (_isVideoLoading.value) {
                    _isVideoLoading.value = false
                }
            }
            try {
                val streamResult = YouTubeVideoExtractor.extractStreamForVideoId(videoId)
                withContext(Dispatchers.Main) {
                    timeoutJob.cancel()
                    val active = _currentTrack.value
                    if (streamResult != null && _isVideoMode.value && active != null) {
                        _currentVideoStream.value = streamResult
                        _selectedVideoQuality.value = "HD Auto"
                        loadVideoStreamIntoPlayer(streamResult, active)
                    } else {
                        showBanner("Could not load stream for selected video", Icons.Rounded.ErrorOutline)
                        _isVideoLoading.value = false
                    }
                }
            } catch (e: Exception) {
                timeoutJob.cancel()
                withContext(Dispatchers.Main) {
                    _isVideoLoading.value = false
                    showBanner("Failed to load video: ${e.message}", Icons.Rounded.ErrorOutline)
                }
            }
        }
    }

    fun searchAlternateVideos(customQuery: String? = null) {
        val query = customQuery?.takeIf { it.isNotBlank() }
            ?: _currentTrack.value?.let { "${it.title} ${it.artist}" }
            ?: return
        _isSearchingAlternates.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val results = YouTubeVideoExtractor.searchAlternateVideos(query)
            withContext(Dispatchers.Main) {
                _alternateVideos.value = results
                _isSearchingAlternates.value = false
            }
        }
    }

    fun seekRelative(deltaMs: Long) {
        val current = mediaController?.currentPosition ?: _playbackPositionMs.value
        val total = mediaController?.duration?.takeIf { it > 0 } ?: _durationMs.value
        val target = (current + deltaMs).coerceIn(0L, total)
        seekTo(target)
    }

    fun toggleLike(track: Track) {
        val currentList = _likedTracks.value.toMutableList()
        val normKey = TrackUtils.normalizeTrackKey(track.title, track.artist)
        val existingIndex = currentList.indexOfFirst {
            it.id == track.id || (normKey.isNotBlank() && TrackUtils.normalizeTrackKey(it.title, it.artist) == normKey)
        }
        val currentFavs = _favoriteTrackIds.value.toMutableSet()
        if (existingIndex >= 0) {
            val removed = currentList.removeAt(existingIndex)
            currentFavs.remove(removed.id)
            currentFavs.remove(track.id)
        } else {
            currentList.add(0, track)
            currentFavs.add(track.id)
        }
        _likedTracks.value = currentList
        _favoriteTrackIds.value = currentFavs
        localMusicStorage.saveLikedTracks(currentList)
        localMusicStorage.saveFavoriteTrackIds(currentFavs)
        scheduleCloudSync()
    }

    fun toggleFavorite(trackId: String) {
        val track = if (currentTrack.value?.id == trackId) {
            currentTrack.value
        } else {
            _queue.value.find { it.id == trackId }
                ?: _trendingTracks.value.find { it.id == trackId }
                ?: _likedTracks.value.find { it.id == trackId }
                ?: _searchResults.value.find { it.id == trackId }
                ?: _artistSpotlightTracks.value.find { it.id == trackId }
                ?: localMusicStorage.getDownloadedTracks().find { it.id == trackId }
        }
        if (track != null) {
            toggleLike(track)
        } else {
            val currentFavorites = _favoriteTrackIds.value.toMutableSet()
            if (currentFavorites.contains(trackId)) {
                currentFavorites.remove(trackId)
                val currentList = _likedTracks.value.filter { it.id != trackId }
                _likedTracks.value = currentList
                localMusicStorage.saveLikedTracks(currentList)
            } else {
                currentFavorites.add(trackId)
            }
            _favoriteTrackIds.value = currentFavorites
            localMusicStorage.saveFavoriteTrackIds(currentFavorites)
            scheduleCloudSync()
        }
    }

    fun createPlaylist(title: String) {
        val newPl = CustomPlaylist(
            id = "custom_${System.currentTimeMillis()}",
            title = title,
            tracks = emptyList(),
            createdAt = "Created today"
        )
        val updated = _customPlaylists.value + newPl
        _customPlaylists.value = updated
        localMusicStorage.saveCustomPlaylists(updated)
        scheduleCloudSync()
    }

    fun deletePlaylist(playlistId: String) {
        val updated = _customPlaylists.value.filter { it.id != playlistId }
        _customPlaylists.value = updated
        localMusicStorage.saveCustomPlaylists(updated)
        scheduleCloudSync()
    }

    fun updatePlaylistTracks(playlistId: String, tracks: List<Track>) {
        val updated = _customPlaylists.value.map { pl ->
            if (pl.id == playlistId) pl.copy(tracks = tracks) else pl
        }
        _customPlaylists.value = updated
        localMusicStorage.saveCustomPlaylists(updated)
        scheduleCloudSync()
    }

    fun updateLikedTracks(tracks: List<Track>) {
        _likedTracks.value = tracks
        localMusicStorage.saveLikedTracks(tracks)
        scheduleCloudSync()
    }

    fun downloadAllTracks(tracks: List<Track>, playlistId: String = "playlist_all") {
        offlineDownloadManager.downloadPlaylist(playlistId, tracks)
    }

    fun cancelPlaylistDownloads(playlistId: String, tracks: List<Track>) {
        offlineDownloadManager.cancelPlaylistDownloads(playlistId, tracks)
    }

    fun deleteDownloadedPlaylist(tracks: List<Track>) {
        offlineDownloadManager.deleteDownloadedPlaylist(tracks)
    }

    // ── Sleep Timer ──
    private val _sleepTimerRemainingSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Int?> = _sleepTimerRemainingSeconds.asStateFlow()
    private var sleepTimerJob: Job? = null

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemainingSeconds.value = null
            showBanner("Sleep timer turned off")
            return
        }
        val totalSec = minutes * 60
        _sleepTimerRemainingSeconds.value = totalSec
        showBanner("Sleep timer set for $minutes minutes")
        sleepTimerJob = viewModelScope.launch {
            var remaining = totalSec
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                _sleepTimerRemainingSeconds.value = remaining
            }
            if (isActive) {
                mediaController?.pause()
                _sleepTimerRemainingSeconds.value = null
                showBanner("Playback paused by sleep timer")
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingSeconds.value = null
        showBanner("Sleep timer turned off")
    }

    // ── Overflow Menu Player Actions ──
    fun startMixWithTrack(track: Track) {
        viewModelScope.launch {
            showBanner("Starting mix with ${track.title}...")
            try {
                val seed = "${track.artist} ${track.title}"
                val searchResult = musicRepository.searchTracks(seed)
                val fetched = searchResult.getOrNull()?.filter { it.id != track.id } ?: emptyList()
                val mix = (listOf(track) + fetched).distinctBy { it.id }
                if (mix.isNotEmpty()) {
                    playQueue(mix, startIndex = 0)
                    showBanner("Mix started • ${mix.size} tracks")
                } else {
                    playTrack(track)
                }
            } catch (e: Exception) {
                playTrack(track)
            }
        }
    }

    fun playNext(track: Track) {
        val controller = mediaController
        val currentIdx = controller?.currentMediaItemIndex ?: 0
        val targetIdx = (currentIdx + 1).coerceAtMost(controller?.mediaItemCount ?: 0)
        controller?.addMediaItem(targetIdx, createMediaItem(track))
        val currentQueue = _queue.value.toMutableList()
        if (targetIdx <= currentQueue.size) {
            currentQueue.add(targetIdx, track)
        } else {
            currentQueue.add(track)
        }
        _queue.value = currentQueue
        showBanner("Playing next: ${track.title}")
    }

    fun addToQueue(track: Track) {
        val controller = mediaController
        controller?.addMediaItem(createMediaItem(track))
        val currentQueue = _queue.value.toMutableList()
        currentQueue.add(track)
        _queue.value = currentQueue
        showBanner("Added to queue: ${track.title}")
    }

    fun addTrackToPlaylists(track: Track, playlistIds: Set<String>) {
        var addedCount = 0
        val isLikedSelected = playlistIds.contains("liked_songs")
        if (isLikedSelected && _likedTracks.value.none { it.id == track.id }) {
            toggleLike(track)
            addedCount++
        }
        val customIds = playlistIds.filter { it != "liked_songs" }.toSet()
        if (customIds.isNotEmpty()) {
            val updated = _customPlaylists.value.map { pl ->
                if (customIds.contains(pl.id)) {
                    if (pl.tracks.none { it.id == track.id }) {
                        addedCount++
                        pl.copy(tracks = pl.tracks + track)
                    } else pl
                } else pl
            }
            _customPlaylists.value = updated
            localMusicStorage.saveCustomPlaylists(updated)
            scheduleCloudSync()
        }
        showBanner("Added to $addedCount playlist${if (addedCount == 1) "" else "s"}")
    }

    fun createPlaylistWithTrack(title: String, track: Track) {
        val newPl = CustomPlaylist(
            id = "custom_${System.currentTimeMillis()}",
            title = title,
            tracks = listOf(track),
            createdAt = "Created today"
        )
        val updated = _customPlaylists.value + newPl
        _customPlaylists.value = updated
        localMusicStorage.saveCustomPlaylists(updated)
        scheduleCloudSync()
        showBanner("Created playlist \"$title\" and added song")
    }

    fun dislikeTrack(track: Track) {
        showBanner("We won't recommend this song again")
        nextTrack()
    }

    fun dismissPlayer() {
        try {
            flushPendingListeningTime()
            progressJob?.cancel()
            progressJob = null
            mediaController?.let { controller ->
                controller.playWhenReady = false
                controller.stop()
                controller.clearMediaItems()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _isPlaying.value = false
        _currentTrack.value = null
        _isNowPlayingExpanded.value = false
        _playbackPositionMs.value = 0L
        pendingTrackToPlay = null
        pendingQueueToPlay = null
    }

    fun resetListeningStatistics() {
        viewModelScope.launch(Dispatchers.IO) {
            val emptyStats = localMusicStorage.resetListeningStatistics()
            _playbackStats.value = emptyStats
            currentTrackListenedMs = 0L
            hasScrobbledCurrentTrack = false
            pendingListeningTimeAccumulatorMs = 0L
        }
    }

    fun recordTrackPlay(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = localMusicStorage.recordTrackPlay(track)
            _playbackStats.value = updated
            scheduleCloudSync()
        }
    }

    fun expandNowPlaying() {
        _isNowPlayingExpanded.value = true
    }

    fun collapseNowPlaying() {
        _isNowPlayingExpanded.value = false
    }

    fun selectTab(tab: NavTab) {
        _selectedTab.value = tab
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.trim().length >= 3) {
            localMusicStorage.addRecentSearch(query)
        }
    }

    fun updateDisplayName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            localMusicStorage.saveDisplayName(trimmed)
            _displayName.value = trimmed
            scheduleCloudSync()
        }
    }

    fun scheduleCloudSync() {
        if (firebaseSyncManager.currentUser.value == null) return
        viewModelScope.launch {
            firebaseSyncManager.pushToFirestore(
                likedTracks = _likedTracks.value,
                customPlaylists = _customPlaylists.value,
                playbackStats = _playbackStats.value,
                displayName = _displayName.value
            )
        }
    }

    fun syncFromCloud() {
        viewModelScope.launch {
            val merged = firebaseSyncManager.pullAndMergeCloudData(
                localLiked = _likedTracks.value,
                localPlaylists = _customPlaylists.value,
                localStats = _playbackStats.value,
                localDisplayName = _displayName.value
            )
            if (merged != null) {
                _likedTracks.value = merged.likedTracks
                _favoriteTrackIds.value = merged.favoriteTrackIds
                localMusicStorage.saveLikedTracks(merged.likedTracks)
                localMusicStorage.saveFavoriteTrackIds(merged.favoriteTrackIds)

                _customPlaylists.value = merged.customPlaylists
                localMusicStorage.saveCustomPlaylists(merged.customPlaylists)

                _playbackStats.value = merged.playbackStats
                localMusicStorage.savePlaybackStats(merged.playbackStats)

                if (merged.displayName.isNotBlank()) {
                    _displayName.value = merged.displayName
                    localMusicStorage.saveDisplayName(merged.displayName)
                }
            }
        }
    }

    fun syncNow() {
        if (firebaseSyncManager.currentUser.value == null) {
            showBanner("Sign in with Google to sync to cloud")
            return
        }
        viewModelScope.launch {
            val merged = firebaseSyncManager.pullAndMergeCloudData(
                localLiked = _likedTracks.value,
                localPlaylists = _customPlaylists.value,
                localStats = _playbackStats.value,
                localDisplayName = _displayName.value
            )
            if (merged != null) {
                _likedTracks.value = merged.likedTracks
                _favoriteTrackIds.value = merged.favoriteTrackIds
                localMusicStorage.saveLikedTracks(merged.likedTracks)
                localMusicStorage.saveFavoriteTrackIds(merged.favoriteTrackIds)

                _customPlaylists.value = merged.customPlaylists
                localMusicStorage.saveCustomPlaylists(merged.customPlaylists)

                _playbackStats.value = merged.playbackStats
                localMusicStorage.savePlaybackStats(merged.playbackStats)

                if (merged.displayName.isNotBlank()) {
                    _displayName.value = merged.displayName
                    localMusicStorage.saveDisplayName(merged.displayName)
                }
                showBanner("All synced with Cloud Firestore", Icons.Filled.CloudDone)
            } else {
                val pushed = firebaseSyncManager.pushToFirestore(
                    likedTracks = _likedTracks.value,
                    customPlaylists = _customPlaylists.value,
                    playbackStats = _playbackStats.value,
                    displayName = _displayName.value
                )
                if (pushed) {
                    showBanner("All synced with Cloud Firestore", Icons.Filled.CloudDone)
                } else {
                    showBanner("Sync failed. Check connection.")
                }
            }
        }
    }

    fun handleUserAccountSwitch(newUserId: String) {
        try {
            // 1. Immediately stop ExoPlayer, cancel progress tracking, and clear active queue & currentTrack
            dismissPlayer()
            _queue.value = emptyList()
            _currentTrack.value = null
            _isPlaying.value = false
            _isBuffering.value = false
            _playbackPositionMs.value = 0L
            _durationMs.value = 200000L
            _isNowPlayingExpanded.value = false
            _isVideoMode.value = false
            playedTrackIdsInSession.clear()

            // 2. Point repositories & local storage to the new user ID
            localMusicStorage.switchUser(newUserId)
            offlineDownloadManager.switchUser(newUserId)

            // 3. Reset and reload all in-memory StateFlows for the new account
            val newLiked = localMusicStorage.getLikedTracks()
            _likedTracks.value = newLiked
            _favoriteTrackIds.value = (localMusicStorage.getFavoriteTrackIds() + newLiked.map { it.id }).toSet()
            _customPlaylists.value = localMusicStorage.getCustomPlaylists()
            _playbackStats.value = localMusicStorage.getPlaybackStats()
            _recentSearches.value = localMusicStorage.getRecentSearches()
            _searchQuery.value = ""
            _searchResults.value = emptyList()
            _isSearching.value = false
            _streamingQuality.value = localMusicStorage.getStreamingQuality()

            _hasCompletedOnboarding.value = localMusicStorage.hasCompletedOnboarding()
            _isOnboardingOpen.value = false
            _selectedLanguages.value = localMusicStorage.getSelectedLanguages()
            _selectedGenres.value = localMusicStorage.getSelectedGenres()
            _selectedArtists.value = localMusicStorage.getSelectedArtists()

            val user = firebaseSyncManager.currentUser.value
            _userPhotoUrl.value = user?.photoUrl?.toString() ?: localMusicStorage.getCachedPhotoUrl()
            _userEmail.value = user?.email ?: localMusicStorage.getCachedEmail()
            _displayName.value = user?.displayName?.ifBlank { null } ?: localMusicStorage.getDisplayName()

            // Refresh personalized feed according to the new user's taste profile
            _selectedArtist.value = localMusicStorage.getUserTasteProfile().topArtists.firstOrNull() ?: "Arijit Singh"
            generatePersonalizedFeed()

            // 4. If logged in with Google account, pull that user's cloud data
            if (newUserId != "guest" && user != null) {
                syncFromCloud()
            }
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Error handling account switch: ${e.message}", e)
        }
    }

    fun signInWithGoogle(context: Context, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            dismissPlayer()
            val result = firebaseSyncManager.signInWithGoogle(context)
            result.onSuccess { user ->
                val newUid = UserSessionManager.computeUserId(user)
                handleUserAccountSwitch(newUid)

                // Wait for cloud check & sync FIRST before routing
                withContext(Dispatchers.IO) {
                    try {
                        val cloudData = firebaseSyncManager.pullAndMergeCloudData(
                            localLiked = localMusicStorage.getLikedTracks(),
                            localPlaylists = localMusicStorage.getCustomPlaylists(),
                            localStats = localMusicStorage.getPlaybackStats(),
                            localDisplayName = localMusicStorage.getDisplayName()
                        )
                        if (cloudData != null) {
                            localMusicStorage.saveLikedTracks(cloudData.likedTracks)
                            localMusicStorage.saveFavoriteTrackIds(cloudData.favoriteTrackIds)
                            localMusicStorage.saveCustomPlaylists(cloudData.customPlaylists)
                            localMusicStorage.savePlaybackStats(cloudData.playbackStats)
                            if (cloudData.displayName.isNotBlank()) {
                                localMusicStorage.saveDisplayName(cloudData.displayName)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("PlayerViewModel", "Cloud pull failed during sign in: ${e.message}")
                    }
                }

                // Update local state flows
                val hasCompleted = localMusicStorage.hasCompletedOnboarding()
                _hasCompletedOnboarding.value = hasCompleted
                _isOnboardingOpen.value = false
                _selectedLanguages.value = localMusicStorage.getSelectedLanguages()
                _selectedGenres.value = localMusicStorage.getSelectedGenres()
                _selectedArtists.value = localMusicStorage.getSelectedArtists()
                _likedTracks.value = localMusicStorage.getLikedTracks()
                _favoriteTrackIds.value = localMusicStorage.getFavoriteTrackIds()
                _customPlaylists.value = localMusicStorage.getCustomPlaylists()
                _playbackStats.value = localMusicStorage.getPlaybackStats()

                localMusicStorage.getSelectedArtists().firstOrNull()?.let {
                    _selectedArtist.value = it
                }
                generatePersonalizedFeed()

                showBanner("Signed in as ${user.displayName ?: user.email ?: "Google User"}", Icons.Filled.CloudDone)
                onComplete(true, null)
            }.onFailure { err ->
                if (err !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                    showBanner("Sign-in failed: ${err.localizedMessage ?: "Unknown error"}")
                }
                onComplete(false, err.localizedMessage)
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            dismissPlayer()
            _queue.value = emptyList()
            _currentTrack.value = null
            firebaseSyncManager.signOut(context)
            handleUserAccountSwitch("guest")
            closeAccountSync()
            showBanner("Signed out of Google account", Icons.AutoMirrored.Filled.Logout)
        }
    }

    private fun flushPendingListeningTime() {
        if (pendingListeningTimeAccumulatorMs > 0L) {
            val delta = pendingListeningTimeAccumulatorMs
            pendingListeningTimeAccumulatorMs = 0L
            viewModelScope.launch(Dispatchers.IO) {
                val updatedStats = localMusicStorage.addListeningTime(delta)
                _playbackStats.value = updatedStats
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        lastProgressSampleTime = System.currentTimeMillis()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val controller = mediaController
                val now = System.currentTimeMillis()
                val isActivelyPlaying = controller != null &&
                    controller.isPlaying &&
                    controller.playbackState == Player.STATE_READY &&
                    !isSwitchingStreamMode &&
                    !_isVideoLoading.value &&
                    !_isBuffering.value

                if (isActivelyPlaying && controller != null) {
                    val pos = controller.currentPosition
                    val dur = controller.duration
                    val targetSeek = pendingSeekPositionMs
                    val nowMs = System.currentTimeMillis()

                    if (targetSeek != null) {
                        if (kotlin.math.abs(pos - targetSeek) <= 1500L || (nowMs - seekRequestTimestamp) > 1500L) {
                            pendingSeekPositionMs = null
                            if (pos >= 0) {
                                _playbackPositionMs.value = pos
                            }
                        } else {
                            _playbackPositionMs.value = targetSeek
                        }
                    } else {
                        if (pos >= 0) {
                            _playbackPositionMs.value = pos
                        }
                    }

                    if (dur > 0) {
                        _durationMs.value = dur
                    }

                    // Compute actual elapsed playback delta
                    if (lastProgressSampleTime > 0L) {
                        val delta = (now - lastProgressSampleTime).coerceIn(0L, 1000L)
                        if (delta > 0) {
                            currentTrackListenedMs += delta
                            pendingListeningTimeAccumulatorMs += delta

                            // Batch listening time writes to disk every 5 seconds (5000ms)
                            if (pendingListeningTimeAccumulatorMs >= 5000L) {
                                val flushDelta = pendingListeningTimeAccumulatorMs
                                pendingListeningTimeAccumulatorMs = 0L
                                val updatedStats = withContext(Dispatchers.IO) {
                                    localMusicStorage.addListeningTime(flushDelta)
                                }
                                _playbackStats.value = updatedStats
                            }

                            // Check Scrobble threshold: at least 30s or 50% of track length if < 60s
                            val track = _currentTrack.value
                            val scrobbleThreshold = if (dur in 1L..59_999L) dur / 2 else 30_000L
                            if (!hasScrobbledCurrentTrack && track != null && currentTrackListenedMs >= scrobbleThreshold) {
                                hasScrobbledCurrentTrack = true
                                val scrobbledStats = withContext(Dispatchers.IO) {
                                    localMusicStorage.recordScrobble(track)
                                }
                                _playbackStats.value = scrobbledStats
                                scheduleCloudSync()
                                Log.d("PlayerViewModel", "Recorded valid scrobble for: ${track.title} (listened ${currentTrackListenedMs}ms, threshold ${scrobbleThreshold}ms)")
                            }
                        }
                    }
                } else if (controller != null && controller.isPlaying) {
                    val pos = controller.currentPosition
                    if (pos >= 0) {
                        _playbackPositionMs.value = pos
                    }
                }
                lastProgressSampleTime = now
                delay(300L)
            }
        }
    }

    private fun extractPaletteForTrack(track: Track) {
        viewModelScope.launch {
            try {
                val colors = PaletteUtils.extractColorsFromUrl(
                    context = getApplication(),
                    imageUrl = track.artworkUrl,
                    fallbackDominant = Color(track.defaultDominantColorHex)
                )
                _dynamicColor.value = colors.dominant
                _darkVibrantColor.value = colors.darkVibrant
                _vibrantColor.value = colors.vibrant
                _mutedColor.value = colors.muted
                Log.d(
                    "PaletteExtraction",
                    "Extracted Palette for '${track.title}': primary=#${Integer.toHexString(colors.dominant.toArgb()).uppercase()}, deep=#${Integer.toHexString(colors.darkVibrant.toArgb()).uppercase()}, vibrant=#${Integer.toHexString(colors.vibrant.toArgb()).uppercase()}"
                )
            } catch (e: Exception) {
                Log.e("PaletteExtraction", "Failed to extract colors for ${track.title}: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        progressJob?.cancel()
        videoSwitchJob?.cancel()
        mediaController?.release()
        mediaController = null
        super.onCleared()
    }
}
