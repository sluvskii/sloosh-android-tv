package com.sloosh.tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sloosh.tv.ui.theme.GlassBorderFocusedDark
import com.sloosh.tv.ui.theme.GlassBorderUnfocusedDark
import com.sloosh.tv.ui.theme.GlassSurfaceDark

@Composable
fun SlooshFocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    focusedScale: Float = 1.08f,
    focusedBorderColor: Color = GlassBorderFocusedDark,
    unfocusedBorderColor: Color = GlassBorderUnfocusedDark,
    borderWidth: Dp = 2.dp,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "card_scale"
    )

    val currentBorderColor = if (isFocused) focusedBorderColor else unfocusedBorderColor

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(GlassSurfaceDark)
            .border(
                border = BorderStroke(if (isFocused) borderWidth else 1.dp, currentBorderColor),
                shape = shape
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        content(isFocused)
    }
}
