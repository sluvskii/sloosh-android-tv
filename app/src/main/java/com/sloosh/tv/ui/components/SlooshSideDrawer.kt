package com.sloosh.tv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.NavigationDrawerItemDefaults
import androidx.tv.material3.NavigationDrawerScope
import androidx.tv.material3.Text

enum class NavSection {
    HOME, SEARCH, CONTINUE, FAVORITES, SETTINGS
}

@Composable
fun NavigationDrawerScope.SlooshSideDrawer(
    selectedSection: NavSection,
    drawerValue: DrawerValue,
    onSectionSelected: (NavSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val isClosed = drawerValue == DrawerValue.Closed

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 32.dp, horizontal = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            DrawerNavItem(
                icon = Icons.Default.Home,
                label = "Главная",
                isSelected = selectedSection == NavSection.HOME,
                isClosed = isClosed,
                onClick = { onSectionSelected(NavSection.HOME) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            DrawerNavItem(
                icon = Icons.Default.Search,
                label = "Поиск",
                isSelected = selectedSection == NavSection.SEARCH,
                isClosed = isClosed,
                onClick = { onSectionSelected(NavSection.SEARCH) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            DrawerNavItem(
                icon = Icons.Default.Schedule,
                label = "Продолжить",
                isSelected = selectedSection == NavSection.CONTINUE,
                isClosed = isClosed,
                onClick = { onSectionSelected(NavSection.CONTINUE) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            DrawerNavItem(
                icon = Icons.Default.Favorite,
                label = "Избранное",
                isSelected = selectedSection == NavSection.FAVORITES,
                isClosed = isClosed,
                onClick = { onSectionSelected(NavSection.FAVORITES) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            DrawerNavItem(
                icon = Icons.Default.Settings,
                label = "Настройки",
                isSelected = selectedSection == NavSection.SETTINGS,
                isClosed = isClosed,
                onClick = { onSectionSelected(NavSection.SETTINGS) }
            )
        }
    }
}

@Composable
private fun NavigationDrawerScope.DrawerNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isClosed: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val isActive = isSelected || isFocused

    val animatedScale by animateFloatAsState(
        targetValue = if (isActive) 1.15f else 1.0f,
        animationSpec = tween(durationMillis = 180),
        label = "drawerItemScale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1.0f else 0.40f,
        animationSpec = tween(durationMillis = 180),
        label = "drawerItemAlpha"
    )

    NavigationDrawerItem(
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier.onFocusChanged { isFocused = it.isFocused },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White.copy(alpha = animatedAlpha),
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        colors = NavigationDrawerItemDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White.copy(alpha = 0.40f),
            focusedContainerColor = Color.Transparent,
            focusedContentColor = Color.White,
            selectedContainerColor = Color.Transparent,
            selectedContentColor = Color.White,
            focusedSelectedContainerColor = Color.Transparent,
            focusedSelectedContentColor = Color.White
        ),
        shape = NavigationDrawerItemDefaults.shape(
            shape = RectangleShape
        ),
        scale = NavigationDrawerItemDefaults.scale(focusedScale = 1.0f)
    ) {
        AnimatedVisibility(
            visible = !isClosed,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(100))
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = if (isActive) 16.sp else 15.sp
                ),
                color = Color.White.copy(alpha = animatedAlpha),
                modifier = Modifier
                    .padding(start = 10.dp, end = 16.dp)
                    .graphicsLayer {
                        scaleX = if (isActive) 1.05f else 1.0f
                        scaleY = if (isActive) 1.05f else 1.0f
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    },
                maxLines = 1
            )
        }
    }
}
