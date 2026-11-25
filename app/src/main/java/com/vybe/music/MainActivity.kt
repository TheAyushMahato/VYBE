package com.vybe.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.messaging.FirebaseMessaging
import com.vybe.music.data.repository.MusicRepositoryImpl
import com.vybe.music.ui.components.AppUpdateDialog
import com.vybe.music.ui.screens.MainScreen
import com.vybe.music.ui.screens.OnboardingTasteScreen
import com.vybe.music.ui.screens.WelcomeSignInScreen
import com.vybe.music.ui.theme.VybeTheme
import com.vybe.music.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "POST_NOTIFICATIONS permission granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Request POST_NOTIFICATIONS runtime permission cleanly on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Subscribe automatically to the FCM topic app_updates
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("app_updates")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("MainActivity", "Subscribed to FCM topic app_updates")
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            VybeTheme {
                val playerViewModel: PlayerViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            val app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                                ?: application
                            PlayerViewModel(
                                application = app,
                                musicRepository = MusicRepositoryImpl()
                            )
                        }
                    }
                )

                val currentUser by playerViewModel.currentUser.collectAsState()
                val hasCompletedOnboarding by playerViewModel.hasCompletedOnboarding.collectAsState()
                val isOnboardingOpen by playerViewModel.isOnboardingOpen.collectAsState()
                val selectedLanguages by playerViewModel.selectedLanguages.collectAsState()
                val selectedGenres by playerViewModel.selectedGenres.collectAsState()
                val selectedArtists by playerViewModel.selectedArtists.collectAsState()

                // ── Auth & Onboarding Flow: Welcome -> Taste Onboarding -> Main ──
                val navigationTarget = when {
                    currentUser == null -> "welcome"
                    !hasCompletedOnboarding || isOnboardingOpen -> "onboarding"
                    else -> "main"
                }

                Crossfade(
                    targetState = navigationTarget,
                    animationSpec = tween(durationMillis = 350),
                    label = "root_navigation"
                ) { target ->
                    when (target) {
                        "welcome" -> WelcomeSignInScreen(viewModel = playerViewModel)
                        "onboarding" -> OnboardingTasteScreen(
                            initialLanguages = selectedLanguages,
                            initialGenres = selectedGenres,
                            initialArtists = selectedArtists,
                            canDismiss = hasCompletedOnboarding && isOnboardingOpen,
                            musicRepository = playerViewModel.musicRepository,
                            onFinish = { langs, genres, artists, seeds ->
                                playerViewModel.completeOnboarding(langs, genres, artists, seeds)
                            },
                            onDismiss = {
                                playerViewModel.closeTasteOnboarding()
                            }
                        )
                        else -> MainScreen(viewModel = playerViewModel)
                    }
                }

                // ── In-App Update CTA (Always active across all auth states) ──
                val appUpdateInfo by playerViewModel.appUpdateInfo.collectAsState()
                val updateDownloadState by playerViewModel.updateDownloadState.collectAsState()
                val updateDownloadProgress by playerViewModel.updateDownloadProgress.collectAsState()
                val isUpdateDismissed by playerViewModel.isUpdateDismissed.collectAsState()
                val context = LocalContext.current

                if (appUpdateInfo != null && !isUpdateDismissed) {
                    AppUpdateDialog(
                        updateInfo = appUpdateInfo!!,
                        downloadState = updateDownloadState,
                        downloadProgress = updateDownloadProgress,
                        onUpdateClick = { playerViewModel.startAppUpdate(context) },
                        onDismiss = { playerViewModel.dismissAppUpdate() }
                    )
                }
            }
        }
    }
}
