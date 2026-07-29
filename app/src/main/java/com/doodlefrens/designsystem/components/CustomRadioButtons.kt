package com.doodlefrens.designsystem.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun ColorRadioButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(targetValue = if (isSelected) 1.2f else 1f, label = "scale")
    val borderSize by animateDpAsState(targetValue = if (isSelected) 2.dp else 0.dp, label = "border")

    Box(
        modifier = modifier
            .padding(4.dp)
            .scale(scale)
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(borderSize, MaterialTheme.colorScheme.primary, CircleShape)
            .clickable(onClick = onClick)
    )
}

@Composable
fun IconRadioButton(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    unselectedTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedTint: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val scale by animateFloatAsState(targetValue = if (isSelected) 1.2f else 1f, label = "scale")
    
    Box(
        modifier = modifier
            .padding(4.dp)
            .scale(scale)
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) selectedTint else unselectedTint
        )
    }
}
