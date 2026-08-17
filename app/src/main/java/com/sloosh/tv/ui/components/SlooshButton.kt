package com.sloosh.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kyant.capsule.ContinuousCapsule

@Composable
fun SlooshButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isWhite: Boolean = false,
    icon: @Composable (() -> Unit)? = null
) {
    val shape = ContinuousCapsule
    val isSolidWhite = isPrimary || isWhite

    SlooshFocusableCard(
        onClick = onClick,
        modifier = modifier.wrapContentSize(),
        shape = shape,
        focusedScale = 1.05f
    ) { isFocused ->
        val bgColor = when {
            isFocused && isSolidWhite -> Color.White
            isFocused -> Color.White.copy(alpha = 0.28f)
            isSolidWhite -> Color.White
            else -> Color.White.copy(alpha = 0.16f)
        }
        val textColor = when {
            isSolidWhite -> Color.Black
            isFocused -> Color.White
            else -> Color.White.copy(alpha = 0.90f)
        }

        Box(
            modifier = Modifier
                .clip(shape)
                .background(bgColor)
                .padding(horizontal = 18.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    CompositionLocalProvider(LocalContentColor provides textColor) {
                        icon()
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    ),
                    color = textColor
                )
            }
        }
    }
}
