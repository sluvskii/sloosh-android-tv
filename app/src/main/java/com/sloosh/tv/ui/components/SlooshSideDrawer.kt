package com.sloosh.tv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.kyant.capsule.ContinuousCapsule

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

            Spacer(modifier = Modifier.height(18.dp))

            DrawerNavItem(
                icon = Icons.Default.Search,
                label = "Поиск",
                isSelected = selectedSection == NavSection.SEARCH,
                isClosed = isClosed,
                onClick = { onSectionSelected(NavSection.SEARCH) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            DrawerNavItem(
                icon = Icons.Default.Schedule,
                label = "Продолжить",
                isSelected = selectedSection == NavSection.CONTINUE,
                isClosed = isClosed,
                onClick = { onSectionSelected(NavSection.CONTINUE) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            DrawerNavItem(
                icon = Icons.Default.Favorite,
                label = "Избранное",
                isSelected = selectedSection == NavSection.FAVORITES,
                isClosed = isClosed,
                onClick = { onSectionSelected(NavSection.FAVORITES) }
            )

            Spacer(modifier = Modifier.height(18.dp))

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
    NavigationDrawerItem(
        selected = isSelected,
        onClick = onClick,
        leadingContent = {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        colors = NavigationDrawerItemDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.08f),
            contentColor = Color.White.copy(alpha = 0.75f),
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
            selectedContainerColor = Color.White.copy(alpha = 0.18f),
            selectedContentColor = Color.White
        ),
        shape = NavigationDrawerItemDefaults.shape(
            shape = if (isClosed) CircleShape else ContinuousCapsule
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
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                maxLines = 1
            )
        }
    }
}
