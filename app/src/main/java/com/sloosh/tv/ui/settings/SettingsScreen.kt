package com.sloosh.tv.ui.settings

import androidx.compose.animation.*
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
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
                // ─── Left Pane: Category Navigation ─────────────────
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
                            fadeIn(animationSpec = tween(180)) + scaleIn(
                                initialScale = 0.98f,
                                animationSpec = tween(180)
                            ) togetherWith fadeOut(animationSpec = tween(120))
                        },
                        label = "settingsPaneTransition",
                        modifier = Modifier.fillMaxSize()
                    ) { targetCat ->
                        val returnToCategory: () -> Unit = {
                            try {
                                categoryFocusRequesters[selectedCategory.ordinal].requestFocus()
                            } catch (e: Exception) { }
                        }

                        TvLazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 60.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            when (targetCat) {
                                SettingsCategory.PLAYBACK -> {
                                    item {
                                        SettingCard(
                                            icon = Icons.Default.FastForward,
                                            title = "Автопереход к следующей серии",
                                            description = "Автоматически включать следующую серию",
                                            control = {
                                                ExpressiveSwitch(
                                                    checked = isAutoplayEnabled,
                                                    onCheckedChange = {
                                                        isAutoplayEnabled = it
                                                        appSettings.isAutoplayEnabled = it
                                                    },
                                                    focusRequester = firstActionFocusRequester,
                                                    onNavigateLeft = returnToCategory
                                                )
                                            }
                                        )
                                    }
                                }

                                SettingsCategory.APPEARANCE -> {
                                    item {
                                        SettingCard(
                                            icon = Icons.Default.GridView,
                                            title = "Сетка карточек",
                                            description = "Количество постеров в ряду",
                                            control = {
                                                SegmentedToggle(
                                                    options = listOf("5 постеров", "6 постеров"),
                                                    selectedIndex = if (gridColumns == 6) 1 else 0,
                                                    onSelect = {
                                                        gridColumns = if (it == 1) 6 else 5
                                                        appSettings.gridColumns = gridColumns
                                                    },
                                                    focusRequester = firstActionFocusRequester,
                                                    onNavigateLeft = returnToCategory
                                                )
                                            }
                                        )
                                    }

                                    item {
                                        SettingCard(
                                            icon = Icons.Default.HighQuality,
                                            title = "Высокое качество постеров",
                                            description = "Загружать постеры в высоком разрешении",
                                            control = {
                                                ExpressiveSwitch(
                                                    checked = isHighPosterQuality,
                                                    onCheckedChange = {
                                                        isHighPosterQuality = it
                                                        appSettings.isHighPosterQuality = it
                                                    },
                                                    onNavigateLeft = returnToCategory
                                                )
                                            }
                                        )
                                    }
                                }

                                SettingsCategory.DATA -> {
                                    item {
                                        SettingCard(
                                            icon = Icons.Default.History,
                                            title = "История просмотров",
                                            description = "Очистить список «Продолжить»",
                                            control = {
                                                SlooshButton(
                                                    text = "Очистить",
                                                    isPrimary = false,
                                                    modifier = Modifier
                                                        .focusRequester(firstActionFocusRequester)
                                                        .onPreviewKeyEvent { keyEvent ->
                                                            if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                                                                keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT
                                                            ) {
                                                                returnToCategory()
                                                                true
                                                            } else false
                                                        },
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
                                            icon = Icons.Default.FavoriteBorder,
                                            title = "Избранное",
                                            description = "Удалить сохранённые фильмы и сериалы",
                                            control = {
                                                SlooshButton(
                                                    text = "Очистить",
                                                    isPrimary = false,
                                                    modifier = Modifier.onPreviewKeyEvent { keyEvent ->
                                                        if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                                                            keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT
                                                        ) {
                                                            returnToCategory()
                                                            true
                                                        } else false
                                                    },
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
                                            icon = Icons.Default.Search,
                                            title = "История поиска",
                                            description = "Очистить список поисковых запросов",
                                            control = {
                                                SlooshButton(
                                                    text = "Очистить",
                                                    isPrimary = false,
                                                    modifier = Modifier.onPreviewKeyEvent { keyEvent ->
                                                        if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                                                            keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT
                                                        ) {
                                                            returnToCategory()
                                                            true
                                                        } else false
                                                    },
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
                                }

                                SettingsCategory.ABOUT -> {
                                    item {
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
                                                .padding(horizontal = 22.dp, vertical = 20.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(
                                                        text = "sloosh",
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = "Версия ${com.sloosh.tv.BuildConfig.VERSION_NAME}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = TextSecondaryDark
                                                    )
                                                }

                                                SlooshButton(
                                                    text = if (isCheckingUpdates) "Проверка..." else "Проверить обновления",
                                                    isWhite = true,
                                                    modifier = Modifier
                                                        .focusRequester(firstActionFocusRequester)
                                                        .onPreviewKeyEvent { keyEvent ->
                                                            if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                                                                keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT
                                                            ) {
                                                                returnToCategory()
                                                                true
                                                            } else false
                                                        },
                                                    onClick = {
                                                        if (!isCheckingUpdates) {
                                                            isCheckingUpdates = true
                                                            coroutineScope.launch {
                                                                val update = updateManager.checkForUpdates()
                                                                isCheckingUpdates = false
                                                                if (update != null) {
                                                                    updateInfoForDialog = update
                                                                } else {
                                                                    statusMessage = "✓ У вас актуальная версия (${com.sloosh.tv.BuildConfig.VERSION_NAME})"
                                                                }
                                                            }
                                                        }
                                                    }
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

// ─── Clean Glass Setting Row Container ───────────────────────────────────────

@Composable
private fun SettingCard(
    icon: ImageVector,
    title: String,
    description: String,
    control: @Composable () -> Unit
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
            .padding(horizontal = 22.dp, vertical = 16.dp)
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
                        .size(40.dp)
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
            control()
        }
    }
}

// ─── Expressive Material 3 Switch Component with Direct TV Focus ─────────────

@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onNavigateLeft: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }

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

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.10f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "switchScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .width(52.dp)
            .height(30.dp)
            .clip(ContinuousCapsule)
            .background(trackColor)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else if (checked) Color.White else Color.White.copy(alpha = 0.15f),
                shape = ContinuousCapsule
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onCheckedChange(!checked)
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER -> {
                            onCheckedChange(!checked)
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (onNavigateLeft != null) {
                                onNavigateLeft()
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            },
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

// ─── Segmented Toggle with Direct Focus ──────────────────────────────────────

@Composable
private fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    focusRequester: FocusRequester? = null,
    onNavigateLeft: (() -> Unit)? = null
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
                            if (isSelected && isFocused) Color.White
                            else if (isSelected) Color.White.copy(alpha = 0.90f)
                            else if (isFocused) Color.White.copy(alpha = 0.25f)
                            else Color.Transparent
                        )
                        .then(
                            if (index == 0 && focusRequester != null) Modifier.focusRequester(focusRequester)
                            else Modifier
                        )
                        .onFocusChanged { isFocused = it.isFocused }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onSelect(index)
                        }
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                when (keyEvent.nativeKeyEvent.keyCode) {
                                    android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                    android.view.KeyEvent.KEYCODE_ENTER -> {
                                        onSelect(index)
                                        true
                                    }
                                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                        if (index == 0 && onNavigateLeft != null) {
                                            onNavigateLeft()
                                            true
                                        } else false
                                    }
                                    else -> false
                                }
                            } else false
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
