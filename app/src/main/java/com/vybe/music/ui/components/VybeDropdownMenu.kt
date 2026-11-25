package com.vybe.music.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties

val VybeDropdownShape = RoundedCornerShape(24.dp)
val VybeDropdownBg = Color(0xFF121212)
val VybeDropdownBorder: BorderStroke? = null

/**
 * Lastwave aesthetic dark rounded dropdown menu.
 * Pinned at 24.dp corner radius, #121212 dark glass fill, 0.6.dp 7% white border,
 * 14.dp shadow elevation, and 8.dp vertical offset.
 */
@Composable
fun VybeDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 8.dp),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .widthIn(min = 210.dp, max = 250.dp),
        offset = offset,
        properties = properties,
        shape = VybeDropdownShape,
        containerColor = VybeDropdownBg,
        shadowElevation = 14.dp,
        border = null,
        content = content
    )
}

/**
 * Standard Lastwave dropdown menu item with 48.dp row height, 18.dp horizontal padding,
 * 15.sp text (#ECECEC regular / White medium for active / #FF5E5E for destructive),
 * 20.dp rounded/outlined icon with 14.dp gap.
 */
@Composable
fun VybeDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isActive: Boolean = false,
    isDestructive: Boolean = false,
    enabled: Boolean = true
) {
    val textColor = when {
        isDestructive -> Color(0xFFFF5E5E)
        isActive -> Color.White
        else -> Color(0xFFECECEC)
    }
    val fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
    val iconTint = when {
        isDestructive -> Color(0xFFFF5E5E)
        isActive -> Color.White
        else -> Color(0xFFB0B0B0)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White.copy(alpha = 0.12f)),
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = fontWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingIcon()
        }
    }
}

/**
 * Composable text variant of VybeDropdownMenuItem for custom text layout or composable icons.
 */
@Composable
fun VybeDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.White.copy(alpha = 0.12f)),
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(14.dp))
            }
            text()
        }
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingIcon()
        }
    }
}

/**
 * Symmetrical Sort / Filter dropdown item.
 * Active sort displays ArrowUpward / ArrowDownward in White.
 * Inactive sort displays SwapVert in #7A7A7A.
 */
@Composable
fun VybeSortDropdownItem(
    text: String,
    isSelected: Boolean,
    isAscending: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    VybeDropdownMenuItem(
        text = text,
        onClick = onClick,
        modifier = modifier,
        isActive = isSelected,
        trailingIcon = {
            if (isSelected) {
                Icon(
                    imageVector = if (isAscending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                    contentDescription = if (isAscending) "Ascending" else "Descending",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.SwapVert,
                    contentDescription = "Inactive sort",
                    tint = Color(0xFF7A7A7A),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    )
}
