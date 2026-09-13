package com.sloosh.tv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kyant.capsule.ContinuousCapsule
import com.sloosh.tv.LocalSideDrawerFocusBridge
import com.sloosh.tv.ui.theme.BackgroundDark

enum class NavSection {
    HOME, SEARCH, CONTINUE, FAVORITES, SETTINGS
}

@Composable
fun SlooshSideDrawer(
    selectedSection: NavSection,
    isOpen: Boolean,
    onOpenChanged: (Boolean) -> Unit,
    onSectionSelected: (NavSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusBridge = LocalSideDrawerFocusBridge.current
    val coroutineScope = rememberCoroutineScope()

    // Close on remote Back button
    BackHandler(enabled = isOpen) {
        onOpenChanged(false)
        val handled = focusBridge.requestContentFocus()
        if (!handled) {
            coroutineScope.launch {
                delay(30)
                focusBridge.requestContentFocus()
            }
        }
    }

    val drawerWidth by animateDpAsState(
        targetValue = if (isOpen) 210.dp else 72.dp,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "drawerWidth"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "drawerTextAlpha"
    )

    val drawerBgAlpha by animateFloatAsState(
        targetValue = if (isOpen) 0.98f else 0.0f,
        animationSpec = tween(durationMillis = 180),
        label = "drawerBgAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(drawerWidth)
            .background(BackgroundDark.copy(alpha = drawerBgAlpha))
            .onFocusChanged { drawerFocusState ->
                if (!drawerFocusState.hasFocus && isOpen) {
                    onOpenChanged(false)
                }
            }
            .padding(vertical = 32.dp, horizontal = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // Top spacer pushes main navigation items to exact vertical center
            Spacer(modifier = Modifier.weight(1f))

            // Navigation Items (Centered group: Главная, Поиск, Продолжить, Избранное)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                DrawerNavItem(
                    section = NavSection.HOME,
                    icon = Icons.Default.Home,
                    label = "Главная",
                    isSelected = selectedSection == NavSection.HOME,
                    isOpen = isOpen,
                    textAlpha = textAlpha,
                    onClick = { onSectionSelected(NavSection.HOME) },
                    onFocused = { onOpenChanged(true) },
                    onClose = { onOpenChanged(false) }
                )

                DrawerNavItem(
                    section = NavSection.SEARCH,
                    icon = Icons.Default.Search,
                    label = "Поиск",
                    isSelected = selectedSection == NavSection.SEARCH,
                    isOpen = isOpen,
                    textAlpha = textAlpha,
                    onClick = { onSectionSelected(NavSection.SEARCH) },
                    onFocused = { onOpenChanged(true) },
                    onClose = { onOpenChanged(false) }
                )

                DrawerNavItem(
                    section = NavSection.CONTINUE,
                    icon = Icons.Default.Schedule,
                    label = "Продолжить",
                    isSelected = selectedSection == NavSection.CONTINUE,
                    isOpen = isOpen,
                    textAlpha = textAlpha,
                    onClick = { onSectionSelected(NavSection.CONTINUE) },
                    onFocused = { onOpenChanged(true) },
                    onClose = { onOpenChanged(false) }
                )

                DrawerNavItem(
                    section = NavSection.FAVORITES,
                    icon = Icons.Default.Favorite,
                    label = "Избранное",
                    isSelected = selectedSection == NavSection.FAVORITES,
                    isOpen = isOpen,
                    textAlpha = textAlpha,
                    onClick = { onSectionSelected(NavSection.FAVORITES) },
                    onFocused = { onOpenChanged(true) },
                    onClose = { onOpenChanged(false) }
                )
            }

            // Bottom spacer between center items and bottom Settings
            Spacer(modifier = Modifier.weight(1f))

            // Bottom Settings Item
            DrawerNavItem(
                section = NavSection.SETTINGS,
                icon = Icons.Default.Settings,
                label = "Настройки",
                isSelected = selectedSection == NavSection.SETTINGS,
                isOpen = isOpen,
                textAlpha = textAlpha,
                onClick = { onSectionSelected(NavSection.SETTINGS) },
                onFocused = { onOpenChanged(true) },
                onClose = { onOpenChanged(false) }
            )
        }
    }
}

@Composable
private fun DrawerNavItem(
    section: NavSection,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isOpen: Boolean,
    textAlpha: Float,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusBridge = LocalSideDrawerFocusBridge.current
    val itemFocusRequester = focusBridge.drawerNavFocusRequesters[section]
    val coroutineScope = rememberCoroutineScope()
    var isFocused by remember { mutableStateOf(false) }

    fun handleExitOrSelect() {
        onClose()
        if (isSelected) {
            val handled = focusBridge.requestContentFocus()
            if (!handled) {
                coroutineScope.launch {
                    delay(30)
                    focusBridge.requestContentFocus()
                }
            }
        } else {
            onClick()
        }
    }

    val containerColor = when {
        isFocused -> Color.White
        isSelected -> Color.White.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    val contentColor = when {
        isFocused -> Color.Black
        isSelected -> Color.White
        else -> Color.White.copy(alpha = 0.50f)
    }

    val itemScale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        animationSpec = tween(120),
        label = "navItemScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            }
            .clip(ContinuousCapsule)
            .background(containerColor)
            .then(if (itemFocusRequester != null) Modifier.focusRequester(itemFocusRequester) else Modifier)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            handleExitOrSelect()
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER -> {
                            handleExitOrSelect()
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (section == NavSection.FAVORITES) {
                                try {
                                    focusBridge.drawerNavFocusRequesters[NavSection.SETTINGS]?.requestFocus()
                                    true
                                } catch (e: Exception) {
                                    false
                                }
                            } else false
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                            if (section == NavSection.SETTINGS) {
                                try {
                                    focusBridge.drawerNavFocusRequesters[NavSection.FAVORITES]?.requestFocus()
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
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) {
                    onFocused()
                }
            }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { handleExitOrSelect() }
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight()
        ) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (textAlpha > 0.02f) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 15.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    color = contentColor,
                    maxLines = 1,
                    modifier = Modifier.graphicsLayer { alpha = textAlpha }
                )
            }
        }
    }
}
