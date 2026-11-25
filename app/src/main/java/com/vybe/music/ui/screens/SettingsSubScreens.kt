package com.vybe.music.ui.screens

import android.app.Activity
import android.content.Context
import com.vybe.music.data.UserSessionManager
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Public
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
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.Coil
import com.vybe.music.R
import com.vybe.music.data.AppUpdateManager
import com.vybe.music.data.LocalMusicStorage
import com.vybe.music.model.AudioQuality
import com.vybe.music.model.CustomPlaylist
import com.vybe.music.model.Track
import com.vybe.music.ui.components.bouncyClickable
import com.vybe.music.ui.theme.AmoledBlack
import com.vybe.music.ui.theme.PureWhite
import com.vybe.music.viewmodel.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

enum class SettingsSubScreen {
    NONE,
    APPEARANCE_VISUALS,
    AUDIO_PLAYBACK,
    LIBRARY_CONTENT,
    DATA_STORAGE,
    ABOUT_SYSTEM,
    HOME_SECTIONS,
    EXCLUDED_SONGS
}

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * 1. Appearance & Visuals Sub-Screen
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Composable
fun AppearanceVisualsScreen(
    onBackClick: () -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("vybe_appearance_prefs", Context.MODE_PRIVATE) }

    var selectedTheme by remember { mutableStateOf(prefs.getString("theme", "Dark") ?: "Dark") }
    var selectedIcon by remember { mutableStateOf(prefs.getString("app_icon", "Obsidian Black") ?: "Obsidian Black") }
    var selectedAccent by remember { mutableStateOf(prefs.getString("accent_name", "Ocean") ?: "Ocean") }

    var amoledMode by remember { mutableStateOf(prefs.getBoolean("amoled_mode", true)) }
    var dynamicColor by remember { mutableStateOf(prefs.getBoolean("dynamic_color", true)) }
    var liquidGlass by remember { mutableStateOf(prefs.getBoolean("liquid_glass", true)) }
    var collapsingNav by remember { mutableStateOf(prefs.getBoolean("collapsing_nav", true)) }
    var dynamicNowPlaying by remember { mutableStateOf(prefs.getBoolean("dynamic_now_playing", true)) }
    var hideNavTabs by remember { mutableStateOf(prefs.getBoolean("hide_nav_tabs", false)) }

    var wordByWordLyrics by remember { mutableStateOf(prefs.getBoolean("word_by_word_lyrics", true)) }
    var wavySeekbar by remember { mutableStateOf(prefs.getBoolean("wavy_seekbar", false)) }

    val accentColors = listOf(
        "Crimson" to Color(0xFFE53935),
        "Violet" to Color(0xFF8E24AA),
        "Ocean" to Color(0xFF1E88E5),
        "Sage" to Color(0xFF43A047),
        "Amber" to Color(0xFFFFB300),
        "Rose" to Color(0xFFD81B60),
        "Mono" to Color(0xFFFFFFFF),
        "Custom" to Color(0xFF00E5FF)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .statusBarsPadding()
    ) {
        SubScreenHeader(title = "Appearance & Visuals", onBackClick = onBackClick)

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
            // ── Section 1: Theme Selector ──
            item {
                SectionTitle("Theme")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF141414))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("System", "Light", "Dark").forEach { theme ->
                            val isSelected = selectedTheme == theme
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSelected) Color(0xFF2E2E32) else Color(0xFF1B1B1E))
                                    .bouncyClickable(pressedScale = 0.94f) {
                                        selectedTheme = theme
                                        prefs.edit().putString("theme", theme).apply()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = theme,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) PureWhite else Color(0xFFA0A0A0)
                                )
                            }
                        }
                    }
                }
            }

            // ── Section 2: App Icon ──
            item {
                SectionTitle("App Icon")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF141414))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val icons = listOf("Default" to Color(0xFF00E5FF), "Pure White" to Color(0xFFFFFFFF), "Obsidian Black" to Color(0xFF1A1A1A))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        icons.forEach { (name, tint) ->
                            val isSelected = selectedIcon == name
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isSelected) Color(0xFF26262C) else Color(0xFF1A1A1E))
                                    .bouncyClickable(pressedScale = 0.94f) {
                                        selectedIcon = name
                                        prefs.edit().putString("app_icon", name).apply()
                                        Toast.makeText(context, "$name icon selected", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 14.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(tint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (name == "Pure White") Color.Black else Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PureWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // ── Section 3: Grouped Visual Toggles ──
            item {
                SectionTitle("Display & UI Effects")
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val toggles = listOf(
                        Triple("AMOLED Mode", "Pure black pitch background", amoledMode) to { v: Boolean ->
                            amoledMode = v
                            prefs.edit().putBoolean("amoled_mode", v).apply()
                        },
                        Triple("Dynamic Color", "Use wallpaper accent tones", dynamicColor) to { v: Boolean ->
                            dynamicColor = v
                            prefs.edit().putBoolean("dynamic_color", v).apply()
                        },
                        Triple("Liquid Glass", "Translucent frosted glass surfaces", liquidGlass) to { v: Boolean ->
                            liquidGlass = v
                            prefs.edit().putBoolean("liquid_glass", v).apply()
                        },
                        Triple("Collapsing Navigation Bar", "Shrink bar to tab icon on scroll", collapsingNav) to { v: Boolean ->
                            collapsingNav = v
                            prefs.edit().putBoolean("collapsing_nav", v).apply()
                        },
                        Triple("Dynamic Now Playing", "Match player colors to artwork", dynamicNowPlaying) to { v: Boolean ->
                            dynamicNowPlaying = v
                            prefs.edit().putBoolean("dynamic_now_playing", v).apply()
                        },
                        Triple("Hide Navigation Tabs", "Ultra minimal full-screen layout", hideNavTabs) to { v: Boolean ->
                            hideNavTabs = v
                            prefs.edit().putBoolean("hide_nav_tabs", v).apply()
                        }
                    )

                    toggles.forEachIndexed { index, (triple, onToggle) ->
                        val (title, subtitle, checked) = triple
                        SettingsToggleRow(
                            title = title,
                            subtitle = subtitle,
                            isChecked = checked,
                            onCheckedChange = onToggle,
                            shape = getGroupedCardShape(index, toggles.size)
                        )
                    }
                }
            }

            // ── Section 4: Accent Color (2x4 Grid) ──
            item {
                SectionTitle("Accent Color")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF141414))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        accentColors.chunked(4).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                row.forEach { (name, color) ->
                                    val isSelected = selectedAccent == name
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(color)
                                            .then(
                                                if (isSelected) Modifier.border(2.dp, Color.White, RoundedCornerShape(14.dp))
                                                else Modifier
                                            )
                                            .bouncyClickable(pressedScale = 0.90f) {
                                                selectedAccent = name
                                                prefs.edit().putString("accent_name", name).apply()
                                                Toast.makeText(context, "$name accent activated", Toast.LENGTH_SHORT).show()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = if (name == "Mono" || name == "Amber") Color.Black else Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Section 5: Experimental Features ──
            item {
                SectionTitle("Experimental")
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    SettingsToggleRow(
                        title = "Word-by-word Lyrics",
                        subtitle = "Real-time synchronized animated syllables",
                        isChecked = wordByWordLyrics,
                        onCheckedChange = {
                            wordByWordLyrics = it
                            prefs.edit().putBoolean("word_by_word_lyrics", it).apply()
                        },
                        shape = getGroupedCardShape(0, 2)
                    )
                    SettingsToggleRow(
                        title = "Wavy Seekbar",
                        subtitle = "Animated undulating audio waveform scrubber",
                        isChecked = wavySeekbar,
                        onCheckedChange = {
                            wavySeekbar = it
                            prefs.edit().putBoolean("wavy_seekbar", it).apply()
                        },
                        shape = getGroupedCardShape(1, 2)
                    )
                }
            }
        }
    }
}

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * 2. Audio & Playback Sub-Screen
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

    var loudnessNormalization by remember { mutableStateOf(prefs.getBoolean("loudness_norm", true)) }
    var systemAudioEffects by remember { mutableStateOf(prefs.getBoolean("system_audio_effects", false)) }
    var studioClarity by remember { mutableStateOf(prefs.getBoolean("studio_clarity", true)) }
    var autoEq by remember { mutableStateOf(prefs.getBoolean("auto_eq", false)) }

    var showEqSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .statusBarsPadding()
    ) {
        SubScreenHeader(title = "Audio & Playback", onBackClick = onBackClick)

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
                SectionTitle("Streaming Quality")
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
                                .background(if (isSelected) Color(0xFF1E1E22) else Color(0xFF141414))
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
                SectionTitle("Sound & Processing")
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Equalizer Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(getGroupedCardShape(0, 5))
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

                    // Loudness Normalization
                    SettingsToggleRow(
                        title = "Loudness Normalization",
                        subtitle = "Consistent volume across all audio tracks",
                        isChecked = loudnessNormalization,
                        onCheckedChange = {
                            loudnessNormalization = it
                            prefs.edit().putBoolean("loudness_norm", it).apply()
                        },
                        shape = getGroupedCardShape(1, 5)
                    )

                    // Studio Master Clarity
                    SettingsToggleRow(
                        title = "Studio Master Clarity",
                        subtitle = "Subtle high-frequency sparkle and dynamic depth boost",
                        isChecked = studioClarity,
                        onCheckedChange = {
                            studioClarity = it
                            prefs.edit().putBoolean("studio_clarity", it).apply()
                        },
                        shape = getGroupedCardShape(2, 5)
                    )

                    // AutoEQ
                    SettingsToggleRow(
                        title = "AutoEQ",
                        subtitle = "Automatically tunes audio profile based on listening genre",
                        isChecked = autoEq,
                        onCheckedChange = {
                            autoEq = it
                            prefs.edit().putBoolean("auto_eq", it).apply()
                        },
                        shape = getGroupedCardShape(3, 5)
                    )

                    // System Audio Effects
                    SettingsToggleRow(
                        title = "System Audio Effects",
                        subtitle = "Let Dolby Atmos / phone system equalizer process sound",
                        isChecked = systemAudioEffects,
                        onCheckedChange = {
                            systemAudioEffects = it
                            prefs.edit().putBoolean("system_audio_effects", it).apply()
                        },
                        shape = getGroupedCardShape(4, 5)
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
 * 3. Equalizer Dialog (5-Band Sound Sliders)
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Composable
fun EqualizerDialog(onDismiss: () -> Unit) {
    val bands = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
    val gains = remember { mutableStateMapOf<Int, Float>().apply { (0..4).forEach { put(it, 0f) } } }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF161618))
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Equalizer",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "5-band studio precision sound shaping",
                    fontSize = 13.sp,
                    color = Color(0xFF9E9E9E)
                )

                Spacer(modifier = Modifier.height(18.dp))

                bands.forEachIndexed { index, bandName ->
                    val currentVal = gains[index] ?: 0f
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = bandName, color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${if (currentVal > 0) "+" else ""}${currentVal.toInt()} dB",
                                color = Color(0xFFA0A0A0),
                                fontSize = 12.sp
                            )
                        }
                        Slider(
                            value = currentVal,
                            onValueChange = { gains[index] = it },
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

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF242428))
                            .bouncyClickable(pressedScale = 0.94f) {
                                (0..4).forEach { gains[it] = 0f }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Reset", color = Color(0xFFB0B0B0), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(50))
                            .background(PureWhite)
                            .bouncyClickable(pressedScale = 0.94f) {
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
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
 * 4. Library & Content Sub-Screen
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Composable
fun LibraryContentScreen(
    onNavigateToHomeSections: () -> Unit,
    onNavigateToExcludedSongs: () -> Unit,
    onNavigateToMusicTaste: () -> Unit = {},
    onCreatePlaylist: (String, List<Track>) -> Unit,
    onBackClick: () -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showYtDialog by remember { mutableStateOf(false) }
    var ytUrlInput by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val tracks = mutableListOf<Track>()
                    var playlistName = "Imported Playlist"
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val reader = BufferedReader(InputStreamReader(stream))
                        val lines = reader.readLines()
                        lines.forEachIndexed { i, line ->
                            val clean = line.trim()
                            if (clean.isNotBlank() && !clean.startsWith("#EXTM3U") && !clean.startsWith("#")) {
                                val parts = clean.split(",")
                                if (parts.size >= 2) {
                                    val title = parts[0].replace("\"", "")
                                    val artist = parts[1].replace("\"", "")
                                    tracks.add(
                                        Track(
                                            id = "imported_${System.currentTimeMillis()}_$i",
                                            title = title,
                                            artist = artist,
                                            album = "Imported",
                                            durationMs = 210000L,
                                            artworkUrl = ""
                                        )
                                    )
                                } else {
                                    tracks.add(
                                        Track(
                                            id = "imported_${System.currentTimeMillis()}_$i",
                                            title = clean,
                                            artist = "Various",
                                            album = "Imported",
                                            durationMs = 210000L,
                                            artworkUrl = ""
                                        )
                                    )
                                }
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onCreatePlaylist(playlistName, tracks)
                        Toast.makeText(context, "Imported ${tracks.size} songs successfully", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to import file: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .statusBarsPadding()
    ) {
        SubScreenHeader(title = "Library & Content", onBackClick = onBackClick)

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
                        title = "Music Taste",
                        subtitle = "Update preferred languages, vibes & artists",
                        shape = getGroupedCardShape(0, 1),
                        onClick = onNavigateToMusicTaste
                    )
                }
            }

            item {
                SectionTitle("Home Customization")
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    SettingsNavRow(
                        title = "Home sections",
                        subtitle = "Reorder, pin, and toggle shelves",
                        shape = getGroupedCardShape(0, 2),
                        onClick = onNavigateToHomeSections
                    )
                    SettingsNavRow(
                        title = "Excluded Songs",
                        subtitle = "Songs marked \"Don't recommend again\"",
                        shape = getGroupedCardShape(1, 2),
                        onClick = onNavigateToExcludedSongs
                    )
                }
            }

            item {
                SectionTitle("Playlist & Library Imports")
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    SettingsNavRow(
                        title = "Import Playlist from File",
                        subtitle = "Supports .csv, .m3u, .m3u8, .txt files",
                        shape = getGroupedCardShape(0, 2),
                        onClick = {
                            filePicker.launch(arrayOf("text/*", "audio/*", "*/*"))
                        }
                    )
                    SettingsNavRow(
                        title = "Import from YouTube Music",
                        subtitle = "Paste a public playlist URL or link",
                        shape = getGroupedCardShape(1, 2),
                        onClick = {
                            ytUrlInput = ""
                            showYtDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showYtDialog) {
        Dialog(onDismissRequest = { showYtDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF161618))
                    .padding(22.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Import YouTube Music Playlist", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Paste link to public playlist", fontSize = 12.5.sp, color = Color(0xFF9E9E9E))

                    Spacer(modifier = Modifier.height(14.dp))

                    BasicTextField(
                        value = ytUrlInput,
                        onValueChange = { ytUrlInput = it },
                        textStyle = TextStyle(color = PureWhite, fontSize = 14.sp),
                        cursorBrush = SolidColor(PureWhite),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF222226))
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (ytUrlInput.isEmpty()) {
                                    Text(text = "https://music.youtube.com/playlist?list=...", color = Color(0xFF757575), fontSize = 13.sp)
                                }
                                innerTextField()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF242428))
                                .bouncyClickable(pressedScale = 0.94f) { showYtDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Cancel", color = Color(0xFFB0B0B0), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(50))
                                .background(PureWhite)
                                .bouncyClickable(pressedScale = 0.94f) {
                                    if (ytUrlInput.isNotBlank()) {
                                        onCreatePlaylist("Imported YTM Playlist", emptyList())
                                        showYtDialog = false
                                        Toast.makeText(context, "Playlist import queued", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Import", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * 5. Home Sections Reordering & Visibility Screen
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Composable
fun HomeSectionsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(UserSessionManager.currentUserId) {
        context.getSharedPreferences(
            UserSessionManager.getUserPrefsName("vybe_home_sections"),
            Context.MODE_PRIVATE
        )
    }

    val defaultSections = listOf(
        "Infinite Radio",
        "Quick access",
        "Quick picks",
        "Fresh finds",
        "Mixes to explore",
        "Trending Artists",
        "Jump back in"
    )

    var sections by remember {
        mutableStateOf(defaultSections)
    }
    val visibilityMap = remember {
        mutableStateMapOf<String, Boolean>().apply {
            defaultSections.forEach { put(it, prefs.getBoolean("show_$it", true)) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E22))
                        .bouncyClickable(pressedScale = 0.88f, onClick = onBackClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(text = "Home sections", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PureWhite)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF222226))
                        .bouncyClickable(pressedScale = 0.90f) {
                            sections.forEach {
                                visibilityMap[it] = true
                                prefs.edit().putBoolean("show_$it", true).apply()
                            }
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Show all", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PureWhite)
                }

                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF222226))
                        .bouncyClickable(pressedScale = 0.90f) {
                            sections = defaultSections
                            defaultSections.forEach {
                                visibilityMap[it] = true
                                prefs.edit().putBoolean("show_$it", true).apply()
                            }
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Reset", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PureWhite)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            itemsIndexed(sections, key = { _, s -> s }) { index, shelfName ->
                val isVisible = visibilityMap[shelfName] ?: true
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(getGroupedCardShape(index, sections.size))
                        .background(Color(0xFF141414))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DragHandle,
                            contentDescription = "Reorder",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = shelfName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PureWhite,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isVisible,
                            onCheckedChange = { v ->
                                visibilityMap[shelfName] = v
                                prefs.edit().putBoolean("show_$shelfName", v).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = Color(0xFF388E3C),
                                uncheckedThumbColor = Color(0xFF8E8E93),
                                uncheckedTrackColor = Color(0xFF26262A)
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * 6. Excluded Songs Screen
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Composable
fun ExcludedSongsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(UserSessionManager.currentUserId) {
        context.getSharedPreferences(
            UserSessionManager.getUserPrefsName("vybe_excluded_tracks"),
            Context.MODE_PRIVATE
        )
    }
    var excludedList by remember { mutableStateOf(listOf("Sample Excluded Song - Artist Name")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .statusBarsPadding()
    ) {
        SubScreenHeader(title = "Excluded Songs", onBackClick = onBackClick)

        if (excludedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No songs currently excluded.\nSongs you mark \"Don't recommend again\" appear here.",
                    color = Color(0xFF757575),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                itemsIndexed(excludedList) { index, item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(getGroupedCardShape(index, excludedList.size))
                            .background(Color(0xFF141414))
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item,
                                color = PureWhite,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF26262C))
                                    .bouncyClickable(pressedScale = 0.90f) {
                                        excludedList = excludedList - item
                                        Toast.makeText(context, "Restored to recommendations", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = "Restore", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * 7. Data & Storage Sub-Screen
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Composable
fun DataStorageScreen(
    storage: LocalMusicStorage,
    onResetStats: () -> Unit = {},
    onBackClick: () -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheSizeMb by remember { mutableStateOf("Calculating...") }
    var showClearDialog by remember { mutableStateOf(false) }
    var showResetStatsDialog by remember { mutableStateOf(false) }

    fun calculateCache() {
        val cache1 = context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        val cache2 = context.externalCacheDir?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
        val totalMb = (cache1 + cache2) / (1024 * 1024)
        cacheSizeMb = "${totalMb.coerceAtLeast(1)} MB"
    }

    LaunchedEffect(Unit) {
        calculateCache()
    }

    // Backup JSON Creator
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val root = JSONObject().apply {
                        put("version", 1)
                        put("exportedAt", System.currentTimeMillis())
                        val liked = JSONArray()
                        storage.getLikedTracks().forEach { t ->
                            liked.put(JSONObject().apply {
                                put("id", t.id)
                                put("title", t.title)
                                put("artist", t.artist)
                                put("album", t.album)
                                put("artworkUrl", t.artworkUrl)
                                put("durationMs", t.durationMs)
                            })
                        }
                        put("likedTracks", liked)

                        val playlists = JSONArray()
                        storage.getCustomPlaylists().forEach { p ->
                            playlists.put(JSONObject().apply {
                                put("id", p.id)
                                put("title", p.title)
                                put("createdAt", p.createdAt)
                                val pTracks = JSONArray()
                                p.tracks.forEach { pt ->
                                    pTracks.put(JSONObject().apply {
                                        put("id", pt.id)
                                        put("title", pt.title)
                                        put("artist", pt.artist)
                                        put("album", pt.album)
                                        put("durationMs", pt.durationMs)
                                    })
                                }
                                put("tracks", pTracks)
                            })
                        }
                        put("customPlaylists", playlists)
                    }

                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(root.toString(2).toByteArray())
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Restore JSON Picker
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val jsonStr = context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().readText()
                    }
                    if (!jsonStr.isNullOrBlank()) {
                        val root = JSONObject(jsonStr)
                        val likedArr = root.optJSONArray("likedTracks") ?: JSONArray()
                        val importedLiked = mutableListOf<Track>()
                        for (i in 0 until likedArr.length()) {
                            val o = likedArr.getJSONObject(i)
                            importedLiked.add(
                                Track(
                                    id = o.optString("id", "t_$i"),
                                    title = o.optString("title", "Unknown"),
                                    artist = o.optString("artist", "Unknown"),
                                    album = o.optString("album", ""),
                                    artworkUrl = o.optString("artworkUrl", ""),
                                    durationMs = o.optLong("durationMs", 0L)
                                )
                            )
                        }
                        storage.saveLikedTracks(importedLiked)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Restored ${importedLiked.size} liked songs & playlists", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .statusBarsPadding()
    ) {
        SubScreenHeader(title = "Data & Storage", onBackClick = onBackClick)

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
            // Storage
            item {
                SectionTitle("Cache & Storage")
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
                                .background(Color(0xFF26262C))
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

            // Backup & Restore
            item {
                SectionTitle("Backup & Restore")
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    SettingsNavRow(
                        title = "Backup Library",
                        subtitle = "Export playlists, likes & settings to .json file",
                        shape = getGroupedCardShape(0, 2),
                        onClick = {
                            backupLauncher.launch("vybe_backup_${System.currentTimeMillis()}.json")
                        }
                    )
                    SettingsNavRow(
                        title = "Restore Library",
                        subtitle = "Restore data from a previously saved .json backup",
                        shape = getGroupedCardShape(1, 2),
                        onClick = {
                            restoreLauncher.launch(arrayOf("application/json", "*/*"))
                        }
                    )
                }
            }

            // Data Management
            item {
                SectionTitle("Data Management")
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Reset Listening Statistics
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF141414))
                            .bouncyClickable(pressedScale = 0.97f) {
                                showResetStatsDialog = true
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

                    // Clear All Saved Data
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
                                Text(text = "Wipes local library, cached playlists & stats", fontSize = 13.sp, color = Color(0xFF9E9E9E))
                            }
                            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }

    if (showResetStatsDialog) {
        Dialog(onDismissRequest = { showResetStatsDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF161618))
                    .padding(22.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Reset Listening Statistics?", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Clear all listening history, scrobbles, and play counts? Your Liked Songs and Playlists will NOT be affected.",
                        fontSize = 13.sp,
                        color = Color(0xFFA0A0A0),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF242428))
                                .bouncyClickable(pressedScale = 0.94f) { showResetStatsDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Cancel", color = Color(0xFFB0B0B0), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFFF9800))
                                .bouncyClickable(pressedScale = 0.94f) {
                                    storage.resetListeningStatistics()
                                    onResetStats()
                                    showResetStatsDialog = false
                                    Toast.makeText(context, "Listening statistics reset", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Reset", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
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
                    .background(Color(0xFF161618))
                    .padding(22.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Clear All Saved Data?", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "This will permanently remove all your locally cached playlists, downloads, and playback history.", fontSize = 13.sp, color = Color(0xFFA0A0A0), lineHeight = 18.sp)

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF242428))
                            .bouncyClickable(pressedScale = 0.94f) { showClearDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Cancel", color = Color(0xFFB0B0B0), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFFF5252))
                                .bouncyClickable(pressedScale = 0.94f) {
                                    storage.saveLikedTracks(emptyList())
                                    storage.saveCustomPlaylists(emptyList())
                                    storage.resetListeningStatistics()
                                    onResetStats()
                                    showClearDialog = false
                                    Toast.makeText(context, "All saved data wiped", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
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
 * 8. About & System Sub-Screen
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
        SubScreenHeader(title = "About & System", onBackClick = onBackClick)

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
                        Text(
                            text = "VYBE",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PureWhite,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Flagship Material 3 Music Player",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFB0B0B0)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Version 1.2.0 (Release Build)",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }

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
                                Text(text = "Verify latest build on Firebase Firestore", fontSize = 12.5.sp, color = Color(0xFF9E9E9E))
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
                        title = "Source Code",
                        subtitle = "GitHub • TheAyushMahato/VYBE",
                        shape = getGroupedCardShape(1, 2),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/TheAyushMahato/VYBE"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            item {
                SectionTitle("About")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF141414))
                        .padding(22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_vybe_logo),
                            contentDescription = "VYBE Logo",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF000000))
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "VYBE Music",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Version 1.0.0 (Flagship Release)",
                            fontSize = 12.sp,
                            color = Color(0xFF888888)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Crafted with Jetpack Compose, Material 3 Expressive, ExoPlayer, and Cloud Firestore sync. Designed for smooth, high-fidelity music streaming without boundaries.",
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
                .background(Color(0xFF1E1E22))
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
                    uncheckedTrackColor = Color(0xFF26262A)
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
