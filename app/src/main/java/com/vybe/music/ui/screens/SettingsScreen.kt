package com.vybe.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import com.vybe.music.ui.components.VybeCrystalHeader
import com.vybe.music.ui.components.VybeTransparentGlassHeader
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vybe.music.ui.components.bouncyClickable
import com.vybe.music.viewmodel.PlayerViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    displayName: String = "Ayush Mahato",
    likedCount: Int = 0,
    playlistsCount: Int = 0,
    historyCount: Int = 0,
    streamingQuality: com.vybe.music.model.AudioQuality = com.vybe.music.model.AudioQuality.MAX,
    onQualitySelected: (com.vybe.music.model.AudioQuality) -> Unit = {},
    onUpdateDisplayName: (String) -> Unit = {}
) {
    val showAccountSyncByVm by viewModel.showAccountSync.collectAsState()
    var showAccountSyncScreen by rememberSaveable { mutableStateOf(false) }
    var activeSubScreen by rememberSaveable { mutableStateOf(SettingsSubScreen.NONE) }
    var showQualitySheet by remember { mutableStateOf(false) }

    val currentUser by viewModel.currentUser.collectAsState()
    val userPhotoUrl by viewModel.userPhotoUrl.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()

    val context = LocalContext.current
    val storage = remember { com.vybe.music.data.LocalMusicStorage(context) }

    // Reset any nested sub-screens whenever Settings is navigated to / opened
    LaunchedEffect(Unit) {
        showAccountSyncScreen = false
        activeSubScreen = SettingsSubScreen.NONE
        viewModel.closeAccountSync()
    }

    if (showAccountSyncScreen || showAccountSyncByVm) {
        AccountCloudSyncScreen(
            viewModel = viewModel,
            onBackClick = {
                showAccountSyncScreen = false
                viewModel.closeAccountSync()
            },
            contentPadding = contentPadding,
            modifier = modifier
        )
        return
    }

    when (activeSubScreen) {
        SettingsSubScreen.APPEARANCE_VISUALS -> {
            AppearanceVisualsScreen(
                onBackClick = { activeSubScreen = SettingsSubScreen.NONE },
                contentPadding = contentPadding
            )
            return
        }
        SettingsSubScreen.AUDIO_PLAYBACK -> {
            AudioPlaybackScreen(
                currentQuality = streamingQuality,
                onQualitySelected = onQualitySelected,
                onBackClick = { activeSubScreen = SettingsSubScreen.NONE },
                contentPadding = contentPadding
            )
            return
        }
        SettingsSubScreen.LIBRARY_CONTENT -> {
            LibraryContentScreen(
                onNavigateToHomeSections = { activeSubScreen = SettingsSubScreen.HOME_SECTIONS },
                onNavigateToExcludedSongs = { activeSubScreen = SettingsSubScreen.EXCLUDED_SONGS },
                onNavigateToMusicTaste = { viewModel.openTasteOnboarding() },
                onCreatePlaylist = { name, tracks ->
                    viewModel.createPlaylist(name)
                },
                onBackClick = { activeSubScreen = SettingsSubScreen.NONE },
                contentPadding = contentPadding
            )
            return
        }
        SettingsSubScreen.HOME_SECTIONS -> {
            HomeSectionsScreen(
                onBackClick = { activeSubScreen = SettingsSubScreen.LIBRARY_CONTENT }
            )
            return
        }
        SettingsSubScreen.EXCLUDED_SONGS -> {
            ExcludedSongsScreen(
                onBackClick = { activeSubScreen = SettingsSubScreen.LIBRARY_CONTENT }
            )
            return
        }
        SettingsSubScreen.DATA_STORAGE -> {
            DataStorageScreen(
                storage = storage,
                onResetStats = { viewModel.resetListeningStatistics() },
                onBackClick = { activeSubScreen = SettingsSubScreen.NONE },
                contentPadding = contentPadding
            )
            return
        }
        SettingsSubScreen.ABOUT_SYSTEM -> {
            AboutSystemScreen(
                onBackClick = { activeSubScreen = SettingsSubScreen.NONE },
                contentPadding = contentPadding
            )
            return
        }
        else -> { /* Show main settings list */ }
    }

    val handleBack = {
        if (activeSubScreen != SettingsSubScreen.NONE) {
            if (activeSubScreen == SettingsSubScreen.HOME_SECTIONS || activeSubScreen == SettingsSubScreen.EXCLUDED_SONGS) {
                activeSubScreen = SettingsSubScreen.LIBRARY_CONTENT
            } else {
                activeSubScreen = SettingsSubScreen.NONE
            }
        } else {
            showAccountSyncScreen = false
            viewModel.closeAccountSync()
            onBackClick()
        }
    }

    BackHandler(onBack = handleBack)

    var activeSheetTitle by remember { mutableStateOf<String?>(null) }
    var activeSheetDescription by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        // ── Scrollable Settings Content (Passes smoothly underneath Crystal Header) ──
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = statusBarTop + 64.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            )
        ) {
            // ── Section 1: Account ──
            item {
                SectionHeader(title = "Account")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(getGroupedCardShape(0, 1))
                            .background(Color(0xFF141414))
                            .bouncyClickable(pressedScale = 0.97f) {
                                showAccountSyncScreen = true
                            }
                            .padding(horizontal = 16.dp, vertical = 15.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 48.dp × 48.dp profile avatar tile
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF1F1F1F)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!userPhotoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(userPhotoUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Profile Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = "Profile",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser?.displayName ?: displayName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentUser?.email ?: if (currentUser != null) "Google Connected" else "Sign in to backup & sync",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF9E9E9E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF9E9E9E),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
            }

            // ── Section 2: Playback & UI ──
            item {
                SectionHeader(title = "Playback & UI")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    SettingsOptionCard(
                        icon = Icons.Default.GraphicEq,
                        title = "Audio & Playback",
                        subtitle = "Quality, Equalizer, Clarity, Effects",
                        index = 0,
                        total = 3,
                        onClick = {
                            activeSubScreen = SettingsSubScreen.AUDIO_PLAYBACK
                        }
                    )
                    SettingsOptionCard(
                        icon = Icons.Default.Palette,
                        title = "Appearance & Visuals",
                        subtitle = "Theme, Icons, AMOLED, Accent colors",
                        index = 1,
                        total = 3,
                        onClick = {
                            activeSubScreen = SettingsSubScreen.APPEARANCE_VISUALS
                        }
                    )
                    SettingsOptionCard(
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        title = "Library & Content",
                        subtitle = "Home sections, Excluded, File imports",
                        index = 2,
                        total = 3,
                        onClick = {
                            activeSubScreen = SettingsSubScreen.LIBRARY_CONTENT
                        }
                    )
                }
                Spacer(modifier = Modifier.height(22.dp))
            }

            // ── Section 3: System & About ──
            item {
                SectionHeader(title = "System & About")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    SettingsOptionCard(
                        icon = Icons.Default.CloudQueue,
                        title = "Data & Storage",
                        subtitle = "Cache, Backup & Restore, Data wipe",
                        index = 0,
                        total = 2,
                        onClick = {
                            activeSubScreen = SettingsSubScreen.DATA_STORAGE
                        }
                    )
                    SettingsOptionCard(
                        icon = Icons.Default.AutoAwesome,
                        title = "About & System",
                        subtitle = "Version 1.2.0 • Check updates, GitHub",
                        index = 1,
                        total = 2,
                        onClick = {
                            activeSubScreen = SettingsSubScreen.ABOUT_SYSTEM
                        }
                    )
                }
                Spacer(modifier = Modifier.height(22.dp))
            }
        }

        // ── Single Source-of-Truth Crystal-Clear Zero-Blur Header ──
        VybeCrystalHeader(
            title = "Settings",
            showBackButton = true,
            onBackClick = handleBack,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    // Modal Sheet for Audio Quality Selection
    if (showQualitySheet) {
        ModalBottomSheet(
            onDismissRequest = { showQualitySheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF141417),
            scrimColor = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 12.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = "Streaming Audio Quality",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select default audio resolution for all playback streams",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF8E8E93)
                )

                Spacer(modifier = Modifier.height(18.dp))

                com.vybe.music.model.AudioQuality.values().forEach { quality ->
                    val isSelected = quality == streamingQuality
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) Color(0xFF222228) else Color(0xFF1A1A1E))
                            .clickable {
                                onQualitySelected(quality)
                                showQualitySheet = false
                            }
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = quality.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = quality.subtitle,
                                    fontSize = 12.5.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color(0xFF8E8E93)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet for Settings Item Details
    if (activeSheetTitle != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheetTitle = null },
            sheetState = sheetState,
            containerColor = Color(0xFF141417),
            scrimColor = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = activeSheetTitle.orEmpty(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = activeSheetDescription.orEmpty(),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFC7C7CC),
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(Color(0xFF26262C))
                        .clickable { activeSheetTitle = null }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Close",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF9E9E9E),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 20.dp, bottom = 10.dp)
    )
}

@Composable
private fun SettingsOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    index: Int,
    total: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(getGroupedCardShape(index, total))
            .background(Color(0xFF141414))
            .bouncyClickable(pressedScale = 0.97f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 44.dp × 44.dp dark rounded icon tile (#1F1F1F, RoundedCornerShape(14.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1F1F1F)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF9E9E9E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
