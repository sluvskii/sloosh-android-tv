package com.sloosh.tv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.sloosh.tv.ui.components.SlooshButton
import com.sloosh.tv.ui.theme.BackgroundDark
import com.sloosh.tv.ui.theme.GlassSurfaceDark
import com.sloosh.tv.ui.theme.SlooshAccentDark

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    var isHighPosterQuality by remember { mutableStateOf(true) }
    var isAutoplayEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(start = 36.dp, top = 28.dp, end = 36.dp, bottom = 28.dp)
    ) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            // Poster Quality
            item {
                SettingCard(
                    title = "Качество постеров",
                    description = if (isHighPosterQuality) "Высокое (Original)" else "Низкое (Экономия трафика)",
                    action = {
                        SlooshButton(
                            text = if (isHighPosterQuality) "Высокое" else "Низкое",
                            isPrimary = isHighPosterQuality,
                            onClick = { isHighPosterQuality = !isHighPosterQuality }
                        )
                    }
                )
            }

            // Autoplay
            item {
                SettingCard(
                    title = "Автовоспроизведение",
                    description = "Автоматически переключать на следующую серию",
                    action = {
                        SlooshButton(
                            text = if (isAutoplayEnabled) "Включено" else "Выключено",
                            isPrimary = isAutoplayEnabled,
                            onClick = { isAutoplayEnabled = !isAutoplayEnabled }
                        )
                    }
                )
            }

            // App Info Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassSurfaceDark)
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "Sloosh Android TV",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Версия 1.0.0 (Build 2026)",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlooshAccentDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Флагманский нативный клиент для Android TV с дизайном Liquid Glass.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    description: String,
    action: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassSurfaceDark)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            action()
        }
    }
}
