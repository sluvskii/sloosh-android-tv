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

import com.sloosh.tv.ui.theme.BackgroundDark

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
            .width(if (isClosed) 68.dp else 176.dp)
            .background(if (isClosed) Color.Transparent else BackgroundDark.copy(alpha = 0.95f))
            .padding(vertical = 24.dp, horizontal = 12.dp)
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

            Spacer(modifier = Modifier.height(10.dp))

            DrawerNavItem(
                icon = Icons.Default.Search,
                label = "Поиск",
                isSelected = selectedSection == NavSection.SEARCH,
                isClosed = isClosed,
                onClick = { onSectionSelected(NavSection.SEARCH) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            DrawerNavItem(
                icon = Icons.Default.Schedule,
                label = "Продолжить",
                isSelected = selectedSection == NavSection.CONTINUE,
                isClosed = isClosed,
                onClick = { onSectionSelected(NavSection.CONTINUE) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            DrawerNavItem(
                icon = Icons.Default.Favorite,
                label = "Избранное",
                isSelected = selectedSection == NavSection.FAVORITES,
                isClosed = isClosed,
                onClick = { onSectionSelected(NavSection.FAVORITES) }
            )

            Spacer(modifier = Modifier.height(10.dp))

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

    // Differentiated states: Focused (1.0) vs Selected-unfocused (0.70) vs Inactive (0.32)
    val targetAlpha = when {
        isFocused -> 1.0f
        isSelected -> 0.68f
        else -> 0.32f
    }

    val targetScale = when {
        isFocused -> 1.14f
        else -> 1.0f
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 160),
        label = "drawerItemScale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 160),
        label = "drawerItemAlpha"
    )

    NavigationDrawerItem(
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier
            .height(38.dp)
            .onFocusChanged { isFocused = it.isFocused },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(38.dp)
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
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        colors = NavigationDrawerItemDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White.copy(alpha = 0.32f),
            focusedContainerColor = Color.Transparent,
            focusedContentColor = Color.White,
            selectedContainerColor = Color.Transparent,
            selectedContentColor = Color.White.copy(alpha = 0.68f),
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
            enter = fadeIn(animationSpec = tween(140)),
            exit = fadeOut(animationSpec = tween(90))
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = when {
                        isFocused -> FontWeight.Bold
                        isSelected -> FontWeight.SemiBold
                        else -> FontWeight.Normal
                    },
                    fontSize = if (isFocused) 15.5.sp else 14.5.sp
                ),
                color = Color.White.copy(alpha = animatedAlpha),
                modifier = Modifier
                    .padding(start = 6.dp, end = 12.dp)
                    .graphicsLayer {
                        scaleX = if (isFocused) 1.04f else 1.0f
                        scaleY = if (isFocused) 1.04f else 1.0f
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    },
                maxLines = 1
            )
        }
    }
}
