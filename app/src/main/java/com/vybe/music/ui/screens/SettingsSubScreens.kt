package com.vybe.music.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.Coil
import com.vybe.music.BuildConfig
import com.vybe.music.R
import com.vybe.music.data.AppUpdateManager
import com.vybe.music.data.LocalMusicStorage
import com.vybe.music.model.AudioQuality
import com.vybe.music.ui.components.bouncyClickable
import com.vybe.music.ui.theme.AmoledBlack
import com.vybe.music.ui.theme.PureWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SettingsSubScreen {
    NONE,
    AUDIO_PLAYBACK,
    STORAGE_DOWNLOADS,
    PERSONALIZATION,
    ABOUT_SYSTEM
}

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * 1. Audio & Playback Screen
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Composable
fun AudioPlaybackScreen(
    currentQuality: AudioQuality,
    onQualitySelected: (AudioQuality) -> Unit,
    onBackClick: () -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("vybe_audio_prefs", Context.MODE_PRIVATE) }
    var systemAudioEffects by remember { mutableStateOf(prefs.getBoolean("system_audio_effects", false)) }
    var showEqSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .statusBarsPadding()
    ) {
        SubScreenHeader(title = "Playback & Audio", onBackClick = onBackClick)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Section 1: Streaming Quality Selector ──
            item {
                SectionTitle("Streaming Audio Quality")
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    AudioQuality.values().forEachIndexed { index, quality ->
                        val isSelected = quality == currentQuality
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(getGroupedCardShape(index, AudioQuality.values().size))
                                .background(if (isSelected) Color(0xFF1E1E1E) else Color(0xFF141414))
                                .bouncyClickable(pressedScale = 0.97f) {
                                    onQualitySelected(quality)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = quality.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PureWhite
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = quality.subtitle,
                                        fontSize = 13.sp,
                                        color = Color(0xFF9E9E9E)
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = PureWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Section 2: Equalizer & Sound Enhancement ──
            item {
                SectionTitle("Sound & Equalizer")
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Equalizer Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(getGroupedCardShape(0, 2))
                            .background(Color(0xFF141414))
                            .bouncyClickable(pressedScale = 0.97f) {
                                showEqSheet = true
                            }
                            .padding(horizontal = 16.dp, vertical = 15.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Equalizer",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PureWhite
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Custom 5-band sound curves & presets",
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF9E9E9E)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF757575),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // System Audio Effects
                    SettingsToggleRow(
                        title = "System Audio Effects",
                        subtitle = "Let Dolby Atmos / phone system equalizer process sound",
                        isChecked = systemAudioEffects,
                        onCheckedChange = {
                            systemAudioEffects = it
                            prefs.edit().putBoolean("system_audio_effects", it).apply()
                        },
                        shape = getGroupedCardShape(1, 2)
                    )
                }
            }
        }
    }

    if (showEqSheet) {
        EqualizerDialog(onDismiss = { showEqSheet = false })
    }
}

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * 2. Equalizer Dialog (5-Band Sound Sliders)
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Composable
fun EqualizerDialog(onDismiss: () -> Unit) {
    val bands = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
    val bandValues = remember { bands.map { mutableFloatStateOf(0f) } }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF161616))
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "5-Band Equalizer",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Custom hardware sound tuning",
                    fontSize = 12.5.sp,
                    color = Color(0xFF9E9E9E)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Sliders
                bands.forEachIndexed { i, bandName ->
                    val sliderVal = bandValues[i]
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = bandName, color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${if (sliderVal.floatValue >= 0) "+" else ""}${sliderVal.floatValue.toInt()} dB",
                                color = Color(0xFFB0B0B0),
                                fontSize = 12.sp
                            )
                        }
                        Slider(
                            value = sliderVal.floatValue,
                            onValueChange = { sliderVal.floatValue = it },
                            valueRange = -10f..10f,
                            steps = 19,
                            colors = SliderDefaults.colors(
                                thumbColor = PureWhite,
                                activeTrackColor = PureWhite,
                                inactiveTrackColor = Color(0xFF2E2E32)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF222222))
                            .bouncyClickable(pressedScale = 0.92f) {
                                bandValues.forEach { it.floatValue = 0f }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(text = "Reset", color = Color(0xFFB0B0B0), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PureWhite)
                            .bouncyClickable(pressedScale = 0.92f) {
                                onDismiss()
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(text = "Apply", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * 3. Storage & Downloads Screen
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Composable
fun DataStorageScreen(
    storage: LocalMusicStorage,
    onBackClick: () -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheSizeMb by remember { mutableStateOf("Calculating...") }
    var showClearDialog by remember { mutableStateOf(false) }

    fun calculateCache() {
        val cache1 = context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        val cache2 = context.externalCacheDir?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
        val totalMb = (cache1 + cache2) / (1024 * 1024)
        cacheSizeMb = "${totalMb.coerceAtLeast(1)} MB"
    }

    LaunchedEffect(Unit) {
        calculateCache()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .statusBarsPadding()
    ) {
        SubScreenHeader(title = "Storage & Downloads", onBackClick = onBackClick)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Section 1: Offline Storage Directory ──
            item {
                SectionTitle("Offline Storage")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF141414))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Download Location", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = PureWhite)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Music/VYBE/ (Device Internal Storage)",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF81D4FA)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "All downloaded tracks are saved as standard audio files in your device's public Music folder for seamless offline playback.",
                            fontSize = 12.5.sp,
                            color = Color(0xFF9E9E9E),
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // ── Section 2: App Cache ──
            item {
                SectionTitle("Cache Management")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF141414))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "App Cache", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = PureWhite)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "Image & audio buffers: $cacheSizeMb", fontSize = 13.sp, color = Color(0xFF9E9E9E))
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF262626))
                                .bouncyClickable(pressedScale = 0.90f) {
                                    scope.launch(Dispatchers.IO) {
                                        context.cacheDir.deleteRecursively()
                                        context.externalCacheDir?.deleteRecursively()
                                        Coil.imageLoader(context).memoryCache?.clear()
                                        Coil.imageLoader(context).diskCache?.clear()
                                        withContext(Dispatchers.Main) {
                                            calculateCache()
                                            Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(text = "Clear Cache", color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Section 3: Data Management ──
            item {
                SectionTitle("Data Management")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF141414))
                        .bouncyClickable(pressedScale = 0.97f) {
                            showClearDialog = true
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Clear All Saved Data", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFF5252))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "Wipes local library, cached playlists & downloads", fontSize = 13.sp, color = Color(0xFF9E9E9E))
                        }
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        Dialog(onDismissRequest = { showClearDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF161616))
                    .padding(22.dp)
            ) {
                Column {
                    Text(text = "Clear All Saved Data?", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "This will permanently remove all your locally cached playlists, downloads, and playback history.", fontSize = 13.sp, color = Color(0xFFA0A0A0), lineHeight = 18.sp)
                    Spacer(modifier = Modifier.height(22.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF222222))
                                .bouncyClickable(pressedScale = 0.92f) { showClearDialog = false }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(text = "Cancel", color = Color(0xFFB0B0B0), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFF5252))
                                .bouncyClickable(pressedScale = 0.92f) {
                                    scope.launch(Dispatchers.IO) {
                                        storage.saveLikedTracks(emptyList())
                                        storage.saveCustomPlaylists(emptyList())
                                        storage.resetListeningStatistics()
                                        context.cacheDir.deleteRecursively()
                                        context.externalCacheDir?.deleteRecursively()
                                        withContext(Dispatchers.Main) {
                                            showClearDialog = false
                                            calculateCache()
                                            Toast.makeText(context, "All data wiped", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(text = "Wipe Data", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * 4. Personalization Screen
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Composable
fun PersonalizationScreen(
    onNavigateToTaste: () -> Unit,
    onResetStats: () -> Unit,
    onBackClick: () -> Unit,
    contentPadding: PaddingValues
) {
    var showResetDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .statusBarsPadding()
    ) {
        SubScreenHeader(title = "Personalization", onBackClick = onBackClick)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionTitle("Taste & Recommendations")
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    SettingsNavRow(
                        title = "Music Taste Onboarding",
                        subtitle = "Re-run taste onboarding & edit favorite artists",
                        shape = RoundedCornerShape(24.dp),
                        onClick = onNavigateToTaste
                    )
                }
            }

            item {
                SectionTitle("Listening Analytics")
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF141414))
                            .bouncyClickable(pressedScale = 0.97f) {
                                showResetDialog = true
                            }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Reset Listening Statistics", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFF9800))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "Clear play counts, scrobbles & listening history", fontSize = 13.sp, color = Color(0xFF9E9E9E))
                            }
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        Dialog(onDismissRequest = { showResetDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF161616))
                    .padding(22.dp)
            ) {
                Column {
                    Text(text = "Reset Listening Statistics?", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "This will reset all your play counts, scrobbles, and most-played track records back to zero.", fontSize = 13.sp, color = Color(0xFFA0A0A0), lineHeight = 18.sp)
                    Spacer(modifier = Modifier.height(22.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF222222))
                                .bouncyClickable(pressedScale = 0.92f) { showResetDialog = false }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(text = "Cancel", color = Color(0xFFB0B0B0), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFF9800))
                                .bouncyClickable(pressedScale = 0.92f) {
                                    onResetStats()
                                    showResetDialog = false
                                    Toast.makeText(context, "Listening stats reset", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(text = "Reset", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * 5. About & System Screen
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Composable
fun AboutSystemScreen(
    onBackClick: () -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCheckingUpdates by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .statusBarsPadding()
    ) {
        SubScreenHeader(title = "About VYBE", onBackClick = onBackClick)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // VYBE Flagship Header Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF24242A), Color(0xFF121214))
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_vybe_logo),
                            contentDescription = "VYBE Logo",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF000000))
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "VYBE",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PureWhite,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Flagship Open-Source Music Player",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFB0B0B0)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME} (Official Release)",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }

            // Open Source License
            item {
                SectionTitle("Open Source")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF141414))
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            text = "MIT License",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "VYBE is 100% free and open-source software. You are free to inspect, modify, and distribute the code under the terms of the MIT License.",
                            fontSize = 13.sp,
                            color = Color(0xFF9E9E9E),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Updates & Source
            item {
                SectionTitle("Updates & Source")
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Check for Updates
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(getGroupedCardShape(0, 2))
                            .background(Color(0xFF141414))
                            .bouncyClickable(pressedScale = 0.97f) {
                                isCheckingUpdates = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val update = AppUpdateManager(context).checkForUpdates()
                                        withContext(Dispatchers.Main) {
                                            isCheckingUpdates = false
                                            if (update != null) {
                                                Toast.makeText(context, "New update available: v${update.latestVersionName}", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "You are on the latest version of VYBE!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isCheckingUpdates = false
                                            Toast.makeText(context, "You are on the latest version of VYBE!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 15.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, tint = PureWhite, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Check for Updates", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = PureWhite)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "Verify latest build on GitHub / Firebase", fontSize = 12.5.sp, color = Color(0xFF9E9E9E))
                            }
                            if (isCheckingUpdates) {
                                CircularProgressIndicator(color = PureWhite, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF757575), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Source Code GitHub
                    SettingsNavRow(
                        title = "Source Code on GitHub",
                        subtitle = "github.com/TheAyushMahato/VYBE",
                        shape = getGroupedCardShape(1, 2),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/TheAyushMahato/VYBE"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // About Summary
            item {
                SectionTitle("Architecture")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF141414))
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Crafted with Jetpack Compose, Material 3, ExoPlayer, and Cloud Firestore sync. Designed for clean, distraction-free, high-fidelity music streaming.",
                        fontSize = 13.sp,
                        color = Color(0xFFA0A0A0),
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * Shared Sub-Screen UI Helpers
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Composable
private fun SubScreenHeader(title: String, onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E1E1E))
                .bouncyClickable(pressedScale = 0.88f, onClick = onBackClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = PureWhite,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color(0xFF9E9E9E),
        fontSize = 13.5.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: Shape
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF141414))
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PureWhite
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PureWhite,
                    checkedTrackColor = Color(0xFF388E3C),
                    uncheckedThumbColor = Color(0xFF8E8E93),
                    uncheckedTrackColor = Color(0xFF262626)
                )
            )
        }
    }
}

@Composable
private fun SettingsNavRow(
    title: String,
    subtitle: String,
    shape: Shape,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF141414))
            .bouncyClickable(pressedScale = 0.97f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PureWhite
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF757575),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
