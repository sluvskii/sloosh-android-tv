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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.sloosh.tv.ui.theme.BackgroundDark
import com.sloosh.tv.ui.theme.GlassBorderFocusedDark
import com.sloosh.tv.ui.theme.GlassSurfaceDark
import com.sloosh.tv.ui.theme.SlooshAccentDark

@Composable
fun SlooshButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    icon: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "btn_scale"
    )

    val shape = RoundedCornerShape(50)

    val backgroundColor = when {
        isPrimary && isFocused -> SlooshAccentDark
        isPrimary -> SlooshAccentDark.copy(alpha = 0.85f)
        isFocused -> Color.White.copy(alpha = 0.25f)
        else -> GlassSurfaceDark
    }

    val contentColor = when {
        isPrimary -> BackgroundDark
        else -> Color.White
    }

    val borderColor = if (isFocused) GlassBorderFocusedDark else Color.Transparent

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(2.dp, borderColor), shape)
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                style = androidx.tv.material3.MaterialTheme.typography.labelLarge
            )
        }
    }
}
