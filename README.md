# VYBE — Modern Android Music Player

VYBE is an open-source Android music player built with **Kotlin**, **Jetpack Compose**, and **Material 3**, featuring a pure AMOLED dark theme, real-time frosted glassmorphism UI, listening stats, offline downloads, and cloud sync.

> **Educational Project:** Built as a personal learning project to explore modern Android architecture, custom Compose UI/UX, ExoPlayer (Media3) streaming, and Firebase integration. Feel free to read the code, learn from it, and build your own apps.

## Key Features

- **AMOLED & Frosted Glassmorphism UI:** Borderless smoky glass floating navigation bar, mini player, and transparent curved headers powered by Jetpack Compose + Haze blur.
- **Audio & Music Video Playback:** Built on AndroidX Media3 (ExoPlayer) with background playback service, media notifications, and Audio/Video switching.
- **Personalized Home & Onboarding:** Taste-based onboarding, Infinite Radio, artist spotlights, and dynamic recommendations.
- **Listening Statistics:** Tracks listening time, top tracks, and artists with cloud sync and reset support.
- **Offline Downloads & Local Audio:** High-quality offline downloads saved to `Music/VYBE/` alongside local device audio playback.
- **Firebase Cloud Sync:** Optional account sync for playlists, liked songs, listening history, and preferences.

## Tech Stack

- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose & Material 3
- **Blur / Glassmorphism:** [Haze](https://github.com/chrisbanes/haze)
- **Media Playback:** AndroidX Media3 (ExoPlayer)
- **Backend / Sync:** Firebase Auth & Cloud Firestore
- **Architecture:** MVVM (`ViewModel` + `StateFlow` + Repository pattern)

## Project Structure

```text
app/src/main/java/com/vybe/music/
├── data/               # Audio/video extraction, offline downloads, local storage, Firebase sync & repositories
├── model/              # Track, Artist, and Album data models
├── service/            # VybePlaybackService (Media3 background service)
├── ui/
│   ├── components/     # BottomNavBar, MiniPlayer, VybeCrystalHeader, GlassmorphicSurface, Shimmer
│   └── screens/        # Home, Stats, Library/Playlists, Search, NowPlaying, Onboarding, Artist, Album, Settings
└── viewmodel/          # PlayerViewModel and state management
```

## How to Build & Run Locally

1. **Clone the repository:**
   ```bash
   git clone https://github.com/TheAyushMahato/VYBE.git
   ```

2. **Add your Firebase config:**
   - Create a free project in [Firebase Console](https://console.firebase.google.com/) and register an Android app with package name `com.vybe.music`.
   - Download your `google-services.json` and place it at `app/google-services.json` (use `app/google-services.json.example` as reference).

3. **Build & Run:**
   - Open the `VYBE` folder in Android Studio (Ladybug or newer) and let Gradle sync finish.
   - Run on an Android device or emulator (Android 12+ recommended for real-time backdrop blur), or build via terminal:
     ```bash
     ./gradlew assembleDebug
     ```

## Disclaimer

This project is strictly for **educational and learning purposes**. It is not affiliated with, endorsed by, or connected to YouTube, Google, or any music streaming service. Users are responsible for complying with applicable terms of service and copyright laws.

## License

Licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
