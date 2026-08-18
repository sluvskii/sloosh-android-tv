package com.sloosh.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.NavigationDrawer
import com.sloosh.tv.ui.components.NavSection
import com.sloosh.tv.ui.components.SlooshSideDrawer
import com.sloosh.tv.ui.continue_watching.ContinueScreen
import com.sloosh.tv.ui.details.DetailsScreen
import com.sloosh.tv.ui.home.HomeScreen
import com.sloosh.tv.ui.home.HomeViewModel
import com.sloosh.tv.ui.player.PlayerScreen
import com.sloosh.tv.ui.profile.ProfileScreen
import com.sloosh.tv.ui.search.SearchScreen
import com.sloosh.tv.ui.settings.SettingsScreen
import com.sloosh.tv.ui.theme.BackgroundDark
import com.sloosh.tv.ui.theme.SlooshTVTheme
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SlooshTVTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val context = androidx.compose.ui.platform.LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                val updateManager = remember { com.sloosh.tv.data.update.UpdateManager(context) }
                var availableUpdate by remember { mutableStateOf<com.sloosh.tv.data.update.AppUpdateInfo?>(null) }

                LaunchedEffect(Unit) {
                    val update = updateManager.checkForUpdates()
                    if (update != null) {
                        availableUpdate = update
                    }
                }

                var selectedSection by remember { mutableStateOf(NavSection.HOME) }

                LaunchedEffect(currentRoute) {
                    when (currentRoute) {
                        "home" -> selectedSection = NavSection.HOME
                        "search" -> selectedSection = NavSection.SEARCH
                        "continue" -> selectedSection = NavSection.CONTINUE
                        "favorites" -> selectedSection = NavSection.FAVORITES
                        "settings" -> selectedSection = NavSection.SETTINGS
                    }
                }

                val showDrawer = currentRoute in listOf("home", "search", "continue", "favorites", "settings")

                val drawerState = androidx.tv.material3.rememberDrawerState(androidx.tv.material3.DrawerValue.Closed)
                val isDrawerOpen = drawerState.currentValue == androidx.tv.material3.DrawerValue.Open
                val scrimAlpha by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isDrawerOpen) 0.40f else 0.0f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
                    label = "drawerScrim"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundDark)
                ) {
                    // Native NavigationDrawer Layer
                    if (showDrawer) {
                        NavigationDrawer(
                            drawerState = drawerState,
                            drawerContent = { drawerValue ->
                                SlooshSideDrawer(
                                    selectedSection = selectedSection,
                                    drawerValue = drawerValue,
                                    onSectionSelected = { section ->
                                        selectedSection = section
                                        val targetRoute = when (section) {
                                            NavSection.HOME -> "home"
                                            NavSection.SEARCH -> "search"
                                            NavSection.CONTINUE -> "continue"
                                            NavSection.FAVORITES -> "favorites"
                                            NavSection.SETTINGS -> "settings"
                                        }
                                        navController.navigate(targetRoute) {
                                            popUpTo("home") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AppNavHost(navController = navController)
                                if (scrimAlpha > 0.01f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = scrimAlpha))
                                    )
                                }
                            }
                        }
                    } else {
                        AppNavHost(navController = navController)
                    }

                    // ─── Global App Update Dialog ─────────────────────
                    availableUpdate?.let { updateInfo ->
                        com.sloosh.tv.ui.components.UpdateDialog(
                            updateInfo = updateInfo,
                            onDismiss = { availableUpdate = null },
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
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = {
            androidx.compose.animation.fadeIn(
                animationSpec = androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) + androidx.compose.animation.scaleIn(
                initialScale = 0.96f,
                animationSpec = androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        },
        exitTransition = {
            androidx.compose.animation.fadeOut(
                animationSpec = androidx.compose.animation.core.tween(180, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) + androidx.compose.animation.scaleOut(
                targetScale = 1.03f,
                animationSpec = androidx.compose.animation.core.tween(180, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            androidx.compose.animation.fadeIn(
                animationSpec = androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) + androidx.compose.animation.scaleIn(
                initialScale = 1.03f,
                animationSpec = androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            androidx.compose.animation.fadeOut(
                animationSpec = androidx.compose.animation.core.tween(180, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) + androidx.compose.animation.scaleOut(
                targetScale = 0.96f,
                animationSpec = androidx.compose.animation.core.tween(180, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        },
        modifier = Modifier.fillMaxSize()
    ) {
        composable("home") {
            HomeScreen(
                onMediaSelected = { mediaId ->
                    navController.navigate("details/${mediaId}")
                }
            )
        }

        composable("search") {
            SearchScreen(
                onMediaSelected = { mediaId ->
                    navController.navigate("details/${mediaId}")
                }
            )
        }

        composable("continue") {
            ContinueScreen(
                onMediaSelected = { mediaId ->
                    navController.navigate("details/${mediaId}")
                }
            )
        }

        composable("favorites") {
            ProfileScreen(
                onMediaSelected = { mediaId ->
                    navController.navigate("details/${mediaId}")
                }
            )
        }

        composable("settings") {
            SettingsScreen()
        }

        composable(
            route = "details/{mediaId}",
            arguments = listOf(navArgument("mediaId") { type = NavType.StringType })
        ) { backStack ->
            val mediaId = backStack.arguments?.getString("mediaId") ?: ""
            DetailsScreen(
                mediaId = mediaId,
                onBackClick = { navController.popBackStack() },
                onPlayClick = { iframeUrl, season, episode, movieTitle ->
                    val encodedUrl = android.util.Base64.encodeToString(
                        iframeUrl.toByteArray(StandardCharsets.UTF_8),
                        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                    )
                    val encodedTitle = android.util.Base64.encodeToString(
                        movieTitle.toByteArray(StandardCharsets.UTF_8),
                        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                    )
                    val seasonParam = season ?: -1
                    val epParam = episode ?: -1
                    navController.navigate("player/$encodedUrl/$mediaId/$seasonParam/$epParam/$encodedTitle")
                }
            )
        }

        composable(
            route = "player/{iframeUrl}/{mediaId}/{season}/{episode}/{title}",
            arguments = listOf(
                navArgument("iframeUrl") { type = NavType.StringType },
                navArgument("mediaId") { type = NavType.StringType },
                navArgument("season") { type = NavType.IntType; defaultValue = -1 },
                navArgument("episode") { type = NavType.IntType; defaultValue = -1 },
                navArgument("title") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStack ->
            val rawUrlParam = backStack.arguments?.getString("iframeUrl") ?: ""
            val mediaId = backStack.arguments?.getString("mediaId") ?: ""
            val season = backStack.arguments?.getInt("season")?.takeIf { it > 0 }
            val episode = backStack.arguments?.getInt("episode")?.takeIf { it > 0 }
            val rawTitleParam = backStack.arguments?.getString("title") ?: ""
            val decodedUrl = try {
                val bytes = android.util.Base64.decode(rawUrlParam, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                String(bytes, StandardCharsets.UTF_8)
            } catch (e: Exception) {
                rawUrlParam
            }
            val decodedTitle = try {
                val bytes = android.util.Base64.decode(rawTitleParam, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                String(bytes, StandardCharsets.UTF_8)
            } catch (e: Exception) {
                rawTitleParam
            }

            PlayerScreen(
                iframeUrl = decodedUrl,
                mediaId = mediaId,
                title = decodedTitle.ifEmpty { "Просмотр" },
                season = season,
                episode = episode,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
