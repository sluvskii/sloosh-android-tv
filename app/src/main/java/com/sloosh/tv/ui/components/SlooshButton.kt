package com.sloosh.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sloosh.tv.ui.theme.SlooshGreen
import com.sloosh.tv.ui.theme.RatingGreenText

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

    val containerColor = when {
        isWhite -> Color.White
        isPrimary -> SlooshGreen
        else -> Color(0xFF1C1C1C)
    }
    val contentColor = when {
        isWhite -> Color.Black
        isPrimary -> RatingGreenText
        else -> Color.White.copy(alpha = 0.85f)
    }
    val focusedContainerColor = when {
        isWhite -> Color.White
        isPrimary -> SlooshGreen
        else -> Color(0xFF2A2A2A)
    }
    val focusedContentColor = when {
        isWhite -> Color.Black
        isPrimary -> RatingGreenText
        else -> Color.White
    }
    val borderColor = when {
        isWhite -> Color.White
        isPrimary -> SlooshGreen
        else -> Color(0xFF2A2A2A)
    }
    val focusedBorderColor = when {
        isWhite -> Color.White
        isPrimary -> SlooshGreen
        else -> Color.White
    }

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
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            )
        }
    }
}

