package com.sloosh.tv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.sloosh.tv.ui.theme.GlassSurfaceDark
import com.sloosh.tv.ui.theme.SlooshAccentDark

enum class NavSection {
    HOME, SEARCH, FAVORITES, SETTINGS
}

@Composable
fun SlooshSideDrawer(
    selectedSection: NavSection,
    onSectionSelected: (NavSection) -> Unit,
    modifier: Modifier = Modifier
) {
    var homeFocused by remember { mutableStateOf(false) }
    var searchFocused by remember { mutableStateOf(false) }
    var favFocused by remember { mutableStateOf(false) }
    var settingsFocused by remember { mutableStateOf(false) }

    val isDrawerFocused = homeFocused || searchFocused || favFocused || settingsFocused

    val drawerWidth by animateDpAsState(
        targetValue = if (isDrawerFocused) 220.dp else 72.dp,
        animationSpec = tween(durationMillis = 200),
        label = "drawer_width"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(drawerWidth)
            .background(Color.Black.copy(alpha = 0.90f))
            .padding(vertical = 24.dp, horizontal = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Brand header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SlooshAccentDark),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "S",
                        color = Color.Black,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                AnimatedVisibility(visible = isDrawerFocused) {
                    Text(
                        text = "Sloosh",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            // Navigation Items
            DrawerNavItem(
                icon = Icons.Default.Home,
                label = "Главная",
                isSelected = selectedSection == NavSection.HOME,
                isExpanded = isDrawerFocused,
                onFocusStateChanged = { homeFocused = it },
                onClick = { onSectionSelected(NavSection.HOME) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            DrawerNavItem(
                icon = Icons.Default.Search,
                label = "Поиск",
                isSelected = selectedSection == NavSection.SEARCH,
                isExpanded = isDrawerFocused,
                onFocusStateChanged = { searchFocused = it },
                onClick = { onSectionSelected(NavSection.SEARCH) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            DrawerNavItem(
                icon = Icons.Default.Favorite,
                label = "Избранное",
                isSelected = selectedSection == NavSection.FAVORITES,
                isExpanded = isDrawerFocused,
                onFocusStateChanged = { favFocused = it },
                onClick = { onSectionSelected(NavSection.FAVORITES) }
            )

            Spacer(modifier = Modifier.weight(1f))

            DrawerNavItem(
                icon = Icons.Default.Settings,
                label = "Настройки",
                isSelected = selectedSection == NavSection.SETTINGS,
                isExpanded = isDrawerFocused,
                onFocusStateChanged = { settingsFocused = it },
                onClick = { onSectionSelected(NavSection.SETTINGS) }
            )
        }
    }
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isExpanded: Boolean,
    onFocusStateChanged: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        onFocusStateChanged(isFocused)
    }

    val backgroundColor = when {
        isFocused -> SlooshAccentDark
        isSelected -> GlassSurfaceDark
        else -> Color.Transparent
    }

    val contentColor = when {
        isFocused -> Color.Black
        isSelected -> SlooshAccentDark
        else -> Color.White.copy(alpha = 0.7f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .onFocusChanged { state ->
                onFocusStateChanged(state.isFocused)
            }
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        AnimatedVisibility(visible = isExpanded) {
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp),
                maxLines = 1
            )
        }
    }
}
