package com.vybe.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vybe.music.ui.theme.FrostedDarkSurface
import com.vybe.music.ui.theme.TranslucentGlassHighlight

@Composable
fun GlassmorphicSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = FrostedDarkSurface,
    elevation: Dp = 8.dp,
    borderStrokeColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val boxModifier = modifier
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color.Black,
            spotColor = Color.Black
        )
        .clip(shape)
        .background(backgroundColor)

    Box(
        modifier = boxModifier,
        content = content
    )
}
