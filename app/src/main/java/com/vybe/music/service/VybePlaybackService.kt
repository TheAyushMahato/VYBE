package com.vybe.music.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.vybe.music.data.audio.YouTubeAudioExtractor
import com.vybe.music.data.audio.YouTubeChunkedDataSourceFactory

@UnstableApi
class VybePlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()

            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setUserAgent(com.vybe.music.data.audio.YouTubeAudioExtractor.YOUTUBE_STREAM_USER_AGENT)
                .setDefaultRequestProperties(
                    mapOf(
                        "Accept" to "*/*",
                        "Accept-Encoding" to "identity"
                    )
                )
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(20000)

            val chunkedHttpFactory = YouTubeChunkedDataSourceFactory(httpDataSourceFactory)
            val upstreamDataSourceFactory = DefaultDataSource.Factory(this, chunkedHttpFactory)

            val defaultMediaSourceFactory = DefaultMediaSourceFactory(this)
                .setDataSourceFactory(upstreamDataSourceFactory)

            val mediaSourceFactory = object : MediaSource.Factory {
                override fun createMediaSource(mediaItem: androidx.media3.common.MediaItem): MediaSource {
                    val audioUrl = mediaItem.mediaMetadata.extras?.getString("video_audio_url")
                    val videoUri = mediaItem.localConfiguration?.uri
                    if (!audioUrl.isNullOrBlank() && videoUri != null && audioUrl != videoUri.toString()) {
                        Log.i("VybePlaybackService", "Creating MergingMediaSource for unified video+audio stream from exact same videoId")
                        val videoItem = mediaItem.buildUpon().setUri(videoUri).build()
                        val audioItem = mediaItem.buildUpon().setUri(audioUrl).build()
                        val videoSource = defaultMediaSourceFactory.createMediaSource(videoItem)
                        val audioSource = defaultMediaSourceFactory.createMediaSource(audioItem)
                        return MergingMediaSource(true, true, videoSource, audioSource)
                    }
                    return defaultMediaSourceFactory.createMediaSource(mediaItem)
                }

                override fun getSupportedTypes(): IntArray = defaultMediaSourceFactory.supportedTypes
                override fun setDrmSessionManagerProvider(drmSessionManagerProvider: androidx.media3.exoplayer.drm.DrmSessionManagerProvider) = apply {
                    defaultMediaSourceFactory.setDrmSessionManagerProvider(drmSessionManagerProvider)
                }
                override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy) = apply {
                    defaultMediaSourceFactory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                }
            }

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()

            val renderersFactory = DefaultRenderersFactory(this).apply {
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            }

            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    15_000,
                    50_000,
                    500,
                    1_000
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            val exoPlayer = ExoPlayer.Builder(this, renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .setSeekParameters(SeekParameters.CLOSEST_SYNC)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .build()

            // Explicitly ensure the main music audio player volume is unmuted (1.0f)
            exoPlayer.volume = 1f

            exoPlayer.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    // Strictly safeguard normal playback speed and pitch (never stuck above 1.0x)
                    exoPlayer.playbackParameters = androidx.media3.common.PlaybackParameters(1.0f, 1.0f)
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    if (playWhenReady) {
                        exoPlayer.volume = 1f
                        exoPlayer.playbackParameters = androidx.media3.common.PlaybackParameters(1.0f, 1.0f)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    val httpCode = (error.cause as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException)?.responseCode ?: -1
                    Log.e(
                        "VYBE_PLAYER",
                        "ExoPlayer playback error [${error.errorCodeName} / code ${error.errorCode} / httpCode $httpCode]: ${error.message}",
                        error
                    )
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> Log.d("VYBE_PLAYER", "ExoPlayer: Buffering stream...")
                        Player.STATE_READY -> {
                            exoPlayer.volume = 1f
                            Log.d("VYBE_PLAYER", "ExoPlayer: Stream ready, playing=${exoPlayer.isPlaying}, items=${exoPlayer.mediaItemCount}")
                        }
                        Player.STATE_ENDED -> Log.d("VYBE_PLAYER", "ExoPlayer: Playback ended")
                        Player.STATE_IDLE -> Log.d("VYBE_PLAYER", "ExoPlayer: Idle")
                    }
                }
            })

            player = exoPlayer

            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            val sessionActivityPendingIntent = if (launchIntent != null) {
                PendingIntent.getActivity(
                    this,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            } else null

            val sessionCallback = object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().build()
                    val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                        .add(Player.COMMAND_PLAY_PAUSE)
                        .add(Player.COMMAND_STOP)
                        .add(Player.COMMAND_SET_VIDEO_SURFACE)
                        .add(Player.COMMAND_SET_VOLUME)
                        .add(Player.COMMAND_GET_VOLUME)
                        .build()

                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .setAvailablePlayerCommands(playerCommands)
                        .build()
                }
            }

            val sessionBuilder = MediaSession.Builder(this, exoPlayer)
                .setId("VybeMediaSession")
                .setCallback(sessionCallback)

            if (sessionActivityPendingIntent != null) {
                sessionBuilder.setSessionActivity(sessionActivityPendingIntent)
            }

            mediaSession = sessionBuilder.build()
            Log.d(TAG, "VybePlaybackService initialized with Media3 ExoPlayer & HTTP cross-protocol redirects")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize VybePlaybackService: ${e.message}", e)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        try {
            val player = mediaSession?.player
            if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
                stopSelf()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        try {
            mediaSession?.run {
                player.release()
                release()
                mediaSession = null
            }
            player = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Vybe Playback",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Vybe Background Music Playback"
                    setShowBadge(false)
                }
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager?.createNotificationChannel(channel)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val TAG = "VybePlaybackService"
        const val CHANNEL_ID = "vybe_playback_channel"
    }
}
