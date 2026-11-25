package com.vybe.music.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState

/**
 * Backward-compatible delegator to [VybeCrystalHeader].
 */
@Composable
fun VybeGlassTopHeader(
    title: String,
    onSearchClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    userPhotoUrl: String? = null,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    VybeCrystalHeader(
        title = title,
        onSearchClick = onSearchClick,
        onProfileClick = onProfileClick,
        userPhotoUrl = userPhotoUrl,
        showBackButton = showBackButton,
        onBackClick = onBackClick,
        modifier = modifier
    )
}
