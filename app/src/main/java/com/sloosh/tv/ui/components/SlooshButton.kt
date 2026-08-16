package com.sloosh.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
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

    val containerColor = if (isSolidWhite) Color.White else Color.White.copy(alpha = 0.09f)
    val contentColor = if (isSolidWhite) Color.Black else Color.White.copy(alpha = 0.85f)
    val focusedContainerColor = Color.White
    val focusedContentColor = Color.Black

    val borderColor = if (isSolidWhite) Color.White else Color.White.copy(alpha = 0.08f)
    val focusedBorderColor = Color.White

    Button(
        onClick = onClick,
        modifier = modifier,
        shape = ButtonDefaults.shape(shape = shape),
        colors = ButtonDefaults.colors(
            containerColor = containerColor,
            contentColor = contentColor,
            focusedContainerColor = focusedContainerColor,
            focusedContentColor = focusedContentColor
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.05f),
        border = ButtonDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, borderColor),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(2.5.dp, focusedBorderColor),
                shape = shape
            )
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp
                )
            )
        }
    }
}
