package com.sloosh.tv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.sloosh.tv.ui.components.SlooshButton
import com.sloosh.tv.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appSettings = remember { com.sloosh.tv.data.repository.AppSettings(context) }

    var isHighPosterQuality by remember { mutableStateOf(appSettings.isHighPosterQuality) }
    var isAutoplayEnabled by remember { mutableStateOf(appSettings.isAutoplayEnabled) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateInfoForDialog by remember { mutableStateOf<com.sloosh.tv.data.update.AppUpdateInfo?>(null) }
    val updateManager = remember { com.sloosh.tv.data.update.UpdateManager(context) }

    // Auto-clear status message after 3 seconds
    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            delay(3500)
            statusMessage = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 4.dp, top = 32.dp, end = 16.dp, bottom = 24.dp)
        ) {

            // ─── Header ──────────────────────────────────────────────
            Text(
                text = "Настройки",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(28.dp))

            TvLazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxWidth(0.72f)
            ) {

                // ─── Section: Воспроизведение ─────────────────────────
                item {
                    SectionHeader("Воспроизведение")
                }

                item {
                    SettingCard(
                        icon = Icons.Default.PlayArrow,
                        title = "Автопереход к серии",
                        description = "Автоматически переключать на следующую серию после окончания",
                        action = {
                            ToggleButton(
                                enabled = isAutoplayEnabled,
                                onToggle = {
                                    isAutoplayEnabled = it
                                    appSettings.isAutoplayEnabled = it
                                }
                            )
                        }
                    )
                }

                // ─── Section: Изображения ─────────────────────────────
                item {
                    SectionHeader("Изображения")
                }

                item {
                    SettingCard(
                        icon = Icons.Default.Star,
                        title = "Качество постеров",
                        description = if (isHighPosterQuality) "Высокое качество (больше трафика)" else "Низкое качество (экономия трафика)",
                        action = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SlooshButton(
                                    text = "Высокое",
                                    isPrimary = isHighPosterQuality,
                                    onClick = {
                                        isHighPosterQuality = true
                                        appSettings.isHighPosterQuality = true
                                    }
                                )
                                SlooshButton(
                                    text = "Низкое",
                                    isPrimary = !isHighPosterQuality,
                                    onClick = {
                                        isHighPosterQuality = false
                                        appSettings.isHighPosterQuality = false
                                    }
                                )
                            }
                        }
                    )
                }

                // ─── Section: Данные ──────────────────────────────────
                item {
                    SectionHeader("Данные и история")
                }

                item {
                    SettingCard(
                        icon = Icons.Default.Delete,
                        title = "История просмотров",
                        description = "Очистить список «Продолжить просмотр»",
                        action = {
                            SlooshButton(
                                text = "Очистить",
                                onClick = {
                                    coroutineScope.launch {
                                        com.sloosh.tv.data.db.AppDatabase
                                            .getDatabase(context)
                                            .progressDao()
                                            .clearAllProgress()
                                        statusMessage = "✓ История просмотров очищена"
                                    }
                                }
                            )
                        }
                    )
                }

                item {
                    SettingCard(
                        icon = Icons.Default.Delete,
                        title = "Избранное",
                        description = "Удалить все сохранённые фильмы и сериалы",
                        action = {
                            SlooshButton(
                                text = "Очистить",
                                onClick = {
                                    coroutineScope.launch {
                                        com.sloosh.tv.data.db.AppDatabase
                                            .getDatabase(context)
                                            .favoritesDao()
                                            .clearAllFavorites()
                                        statusMessage = "✓ Избранное очищено"
                                    }
                                }
                            )
                        }
                    )
                }

                item {
                    SettingCard(
                        icon = Icons.Default.Delete,
                        title = "История поиска",
                        description = "Очистить список сохранённых запросов",
                        action = {
                            SlooshButton(
                                text = "Очистить",
                                onClick = {
                                    coroutineScope.launch {
                                        com.sloosh.tv.data.db.AppDatabase
                                            .getDatabase(context)
                                            .searchHistoryDao()
                                            .clearAllSearchHistory()
                                        statusMessage = "✓ История поиска очищена"
                                    }
                                }
                            )
                        }
                    )
                }

                // ─── Section: О приложении ────────────────────────────
                item {
                    SectionHeader("О приложении")
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ContinuousRoundedRectangle(18.dp))
                            .background(GlassSurfaceDark)
                            .padding(22.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(ContinuousRoundedRectangle(14.dp))
                                            .background(Color.White.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "sloosh Android TV",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Версия ${com.sloosh.tv.BuildConfig.VERSION_NAME}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondaryDark
                                        )
                                    }
                                }

                                SlooshButton(
                                    text = if (isCheckingUpdates) "Проверка..." else "Проверить обновления",
                                    isWhite = true,
                                    onClick = {
                                        if (!isCheckingUpdates) {
                                            isCheckingUpdates = true
                                            coroutineScope.launch {
                                                val update = updateManager.checkForUpdates()
                                                isCheckingUpdates = false
                                                if (update != null) {
                                                    updateInfoForDialog = update
                                                } else {
                                                    statusMessage = "✓ У вас установлена последняя версия (${com.sloosh.tv.BuildConfig.VERSION_NAME})"
                                                }
                                            }
                                        }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Нативный клиент для Android TV с кинематографическим дизайном, поддержкой Alloha и автоматическими обновлениями с GitHub.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }
            }
        }

        // ─── Update Dialog ────────────────────────────────────────────
        updateInfoForDialog?.let { updateInfo ->
            com.sloosh.tv.ui.components.UpdateDialog(
                updateInfo = updateInfo,
                onDismiss = { updateInfoForDialog = null },
                onStartUpdate = { onProgress, onError ->
                    coroutineScope.launch {
                        updateManager.downloadAndInstall(
                            downloadUrl = updateInfo.downloadUrl,
                            onProgress = onProgress,
                            onError = onError
                        )
                    }
                }
            )
        }

        // ─── Status Message Toast ─────────────────────────────────────
        if (statusMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
                    .clip(ContinuousCapsule)
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = statusMessage!!,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = Color.White.copy(alpha = 0.70f),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
    )
}

@Composable
private fun SettingCard(
    icon: ImageVector,
    title: String,
    description: String,
    action: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(18.dp))
            .background(GlassSurfaceDark)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(ContinuousRoundedRectangle(12.dp))
                        .background(Color.White.copy(alpha = 0.07f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedDark
                    )
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            action()
        }
    }
}

@Composable
private fun ToggleButton(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SlooshButton(
            text = "Вкл",
            isPrimary = enabled,
            onClick = { onToggle(true) }
        )
        SlooshButton(
            text = "Выкл",
            isPrimary = !enabled,
            onClick = { onToggle(false) }
        )
    }
}
