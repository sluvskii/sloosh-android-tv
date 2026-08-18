package com.sloosh.tv.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.sloosh.tv.ui.components.SlooshButton
import com.sloosh.tv.ui.components.SlooshFocusableCard
import com.sloosh.tv.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SettingsCategory(val title: String, val icon: ImageVector) {
    PLAYBACK("Воспроизведение", Icons.Default.PlayArrow),
    APPEARANCE("Интерфейс", Icons.Default.GridView),
    DATA("Данные и хранилище", Icons.Default.DeleteOutline),
    ABOUT("О приложении", Icons.Default.Info)
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appSettings = remember { com.sloosh.tv.data.repository.AppSettings(context) }

    var selectedCategory by remember { mutableStateOf(SettingsCategory.PLAYBACK) }
    var isHighPosterQuality by remember { mutableStateOf(appSettings.isHighPosterQuality) }
    var isAutoplayEnabled by remember { mutableStateOf(appSettings.isAutoplayEnabled) }
    var gridColumns by remember { mutableStateOf(appSettings.gridColumns) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateInfoForDialog by remember { mutableStateOf<com.sloosh.tv.data.update.AppUpdateInfo?>(null) }
    val updateManager = remember { com.sloosh.tv.data.update.UpdateManager(context) }

    val categoryFocusRequesters = remember { Array(SettingsCategory.values().size) { FocusRequester() } }
    val firstActionFocusRequester = remember { FocusRequester() }

    // Auto-clear status message
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
                .padding(start = 24.dp, top = 32.dp, end = 32.dp, bottom = 24.dp)
        ) {
            // ─── Header ──────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(start = 4.dp, bottom = 28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(ContinuousCapsule)
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "Настройки",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.4).sp
                )
            }

            // ─── Two-Pane Layout ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // ─── Left Pane: Category Navigation (280dp) ─────────
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsCategory.values().forEachIndexed { index, cat ->
                        val isSelected = selectedCategory == cat
                        var isFocused by remember { mutableStateOf(false) }

                        val contentColor = when {
                            isFocused -> Color.Black
                            isSelected -> Color.White
                            else -> Color.White.copy(alpha = 0.65f)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(ContinuousRoundedRectangle(14.dp))
                                .background(
                                    if (isFocused) Color.White
                                    else if (isSelected) Color.White.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .focusRequester(categoryFocusRequesters[index])
                                .onFocusChanged {
                                    isFocused = it.isFocused
                                    if (it.isFocused && selectedCategory != cat) {
                                        selectedCategory = cat
                                    }
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    selectedCategory = cat
                                }
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                        when (keyEvent.nativeKeyEvent.keyCode) {
                                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                try {
                                                    firstActionFocusRequester.requestFocus()
                                                    true
                                                } catch (e: Exception) {
                                                    false
                                                }
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                                if (index > 0) {
                                                    try {
                                                        categoryFocusRequesters[index - 1].requestFocus()
                                                        true
                                                    } catch (e: Exception) {
                                                        false
                                                    }
                                                } else false
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                if (index < SettingsCategory.values().size - 1) {
                                                    try {
                                                        categoryFocusRequesters[index + 1].requestFocus()
                                                        true
                                                    } catch (e: Exception) {
                                                        false
                                                    }
                                                } else false
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = cat.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 15.sp
                                    ),
                                    color = contentColor
                                )
                            }
                        }
                    }
                }

                // ─── Right Pane: Content Panel ───────────────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    AnimatedContent(
                        targetState = selectedCategory,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)) + scaleIn(
                                initialScale = 0.98f,
                                animationSpec = tween(200)
                            ) togetherWith fadeOut(animationSpec = tween(150))
                        },
                        label = "settingsPaneTransition",
                        modifier = Modifier.fillMaxSize()
                    ) { targetCat ->
                        TvLazyColumn(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 60.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            when (targetCat) {
                                SettingsCategory.PLAYBACK -> {
                                    item {
                                        SettingToggleCard(
                                            icon = Icons.Default.FastForward,
                                            title = "Автопереход к следующей серии",
                                            description = "Автоматически начинать показ следующей серии после окончания текущей",
                                            checked = isAutoplayEnabled,
                                            focusRequester = firstActionFocusRequester,
                                            onToggle = {
                                                isAutoplayEnabled = !isAutoplayEnabled
                                                appSettings.isAutoplayEnabled = isAutoplayEnabled
                                            }
                                        )
                                    }
                                }

                                SettingsCategory.APPEARANCE -> {
                                    item {
                                        SettingSegmentedCard(
                                            icon = Icons.Default.GridView,
                                            title = "Сетка карточек",
                                            description = if (gridColumns == 6) "Компактный вид — 6 постеров в ряду" else "Крупный вид — 5 постеров в ряду",
                                            options = listOf("5 постеров", "6 постеров"),
                                            selectedIndex = if (gridColumns == 6) 1 else 0,
                                            focusRequester = firstActionFocusRequester,
                                            onSelect = {
                                                gridColumns = if (it == 1) 6 else 5
                                                appSettings.gridColumns = gridColumns
                                            }
                                        )
                                    }

                                    item {
                                        SettingToggleCard(
                                            icon = Icons.Default.HighQuality,
                                            title = "Высокое качество постеров",
                                            description = if (isHighPosterQuality) "Максимальное разрешение обложек (насыщенная картинка)" else "Экономичный режим (быстрая загрузка и меньший расход трафика)",
                                            checked = isHighPosterQuality,
                                            onToggle = {
                                                isHighPosterQuality = !isHighPosterQuality
                                                appSettings.isHighPosterQuality = isHighPosterQuality
                                            }
                                        )
                                    }
                                }

                                SettingsCategory.DATA -> {
                                    item {
                                        SettingActionCard(
                                            icon = Icons.Default.History,
                                            title = "Очистить историю просмотров",
                                            description = "Удалить все сохранённые позиции из раздела «Продолжить»",
                                            actionButtonText = "Очистить",
                                            focusRequester = firstActionFocusRequester,
                                            onAction = {
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

                                    item {
                                        SettingActionCard(
                                            icon = Icons.Default.FavoriteBorder,
                                            title = "Очистить избранное",
                                            description = "Удалить все сохранённые фильмы и сериалы из «Избранного»",
                                            actionButtonText = "Очистить",
                                            onAction = {
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

                                    item {
                                        SettingActionCard(
                                            icon = Icons.Default.Search,
                                            title = "Очистить историю поиска",
                                            description = "Удалить список сохранённых поисковых запросов",
                                            actionButtonText = "Очистить",
                                            onAction = {
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
                                }

                                SettingsCategory.ABOUT -> {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(ContinuousRoundedRectangle(20.dp))
                                                .background(GlassSurfaceDark)
                                                .border(
                                                    width = 1.dp,
                                                    color = Color.White.copy(alpha = 0.08f),
                                                    shape = ContinuousRoundedRectangle(20.dp)
                                                )
                                                .padding(24.dp)
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(54.dp)
                                                                .clip(ContinuousRoundedRectangle(16.dp))
                                                                .background(
                                                                    Brush.linearGradient(
                                                                        listOf(
                                                                            Color(0xFF2C2C2E),
                                                                            Color(0xFF1C1C1E)
                                                                        )
                                                                    )
                                                                )
                                                                .border(
                                                                    1.dp,
                                                                    Color.White.copy(alpha = 0.15f),
                                                                    ContinuousRoundedRectangle(16.dp)
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "S",
                                                                fontSize = 26.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = Color.White
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

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(1.dp)
                                                        .background(Color.White.copy(alpha = 0.08f))
                                                )

                                                Text(
                                                    text = "Премиальный нативный клиент для Android TV с кинематографическим дизайном, поддержкой видеобалансировщика Alloha и автоматическими бесшовными обновлениями.",
                                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                                    color = TextSecondaryDark
                                                )
                                            }
                                        }
                                    }
                                }
                            }
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
        AnimatedVisibility(
            visible = statusMessage != null,
            enter = fadeIn(animationSpec = tween(200)) + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        ) {
            if (statusMessage != null) {
                Box(
                    modifier = Modifier
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
}

// ─── Expressive Material 3 Switch Component ──────────────────────────────────

@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 3.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 450f),
        label = "switchThumbOffset"
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) Color.White else Color(0xFF242428),
        animationSpec = tween(180),
        label = "switchTrackColor"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) Color.Black else Color.White.copy(alpha = 0.70f),
        animationSpec = tween(180),
        label = "switchThumbColor"
    )

    Box(
        modifier = modifier
            .width(52.dp)
            .height(30.dp)
            .clip(ContinuousCapsule)
            .background(trackColor)
            .border(
                width = 1.dp,
                color = if (checked) Color.White else Color.White.copy(alpha = 0.15f),
                shape = ContinuousCapsule
            )
            .then(
                if (onCheckedChange != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCheckedChange(!checked) }
                } else Modifier
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(24.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

// ─── Setting Cards (Fully focusable & clickable with D-pad) ───────────────────

@Composable
private fun SettingToggleCard(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    focusRequester: FocusRequester? = null,
    onToggle: () -> Unit
) {
    SlooshFocusableCard(
        onClick = onToggle,
        shape = ContinuousRoundedRectangle(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
    ) { isFocused ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isFocused) Color(0xFF222226) else GlassSurfaceDark
                )
                .border(
                    width = 1.dp,
                    color = if (isFocused) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.08f),
                    shape = ContinuousRoundedRectangle(18.dp)
                )
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(ContinuousRoundedRectangle(12.dp))
                        .background(Color.White.copy(alpha = if (isFocused) 0.15f else 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isFocused) Color.White else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedDark
                    )
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            ExpressiveSwitch(
                checked = checked,
                onCheckedChange = null // Click is handled by the whole card
            )
        }
    }
}

@Composable
private fun SettingSegmentedCard(
    icon: ImageVector,
    title: String,
    description: String,
    options: List<String>,
    selectedIndex: Int,
    focusRequester: FocusRequester? = null,
    onSelect: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(18.dp))
            .background(GlassSurfaceDark)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = ContinuousRoundedRectangle(18.dp)
            )
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(ContinuousRoundedRectangle(12.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedDark
                    )
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Box(modifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier) {
                SegmentedToggle(
                    options = options,
                    selectedIndex = selectedIndex,
                    onSelect = onSelect
                )
            }
        }
    }
}

@Composable
private fun SettingActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    actionButtonText: String,
    focusRequester: FocusRequester? = null,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(18.dp))
            .background(GlassSurfaceDark)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = ContinuousRoundedRectangle(18.dp)
            )
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(ContinuousRoundedRectangle(12.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedDark
                    )
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Box(modifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier) {
                SlooshButton(
                    text = actionButtonText,
                    isPrimary = false,
                    onClick = onAction
                )
            }
        }
    }
}

@Composable
private fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .clip(ContinuousCapsule)
            .background(Color(0xFF141416).copy(alpha = 0.85f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = ContinuousCapsule
            )
            .padding(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            options.forEachIndexed { index, optionText ->
                val isSelected = selectedIndex == index
                var isFocused by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .clip(ContinuousCapsule)
                        .background(
                            if (isSelected) Color.White
                            else if (isFocused) Color.White.copy(alpha = 0.20f)
                            else Color.Transparent
                        )
                        .onFocusChanged { isFocused = it.isFocused }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onSelect(index)
                        }
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = optionText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
