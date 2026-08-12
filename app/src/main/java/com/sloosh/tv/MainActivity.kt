package com.sloosh.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sloosh.tv.ui.components.NavSection
import com.sloosh.tv.ui.components.SlooshSideDrawer
import com.sloosh.tv.ui.details.DetailsScreen
import com.sloosh.tv.ui.home.HomeScreen
import com.sloosh.tv.ui.player.PlayerScreen
import com.sloosh.tv.ui.profile.ProfileScreen
import com.sloosh.tv.ui.search.SearchScreen
import com.sloosh.tv.ui.settings.SettingsScreen
import com.sloosh.tv.ui.theme.SlooshTVTheme
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

                var selectedSection by remember { mutableStateOf(NavSection.HOME) }

                val showDrawer = currentRoute in listOf("home", "search", "favorites", "settings")

                Row(modifier = Modifier.fillMaxSize()) {
                    if (showDrawer) {
                        SlooshSideDrawer(
                            selectedSection = selectedSection,
                            onSectionSelected = { section ->
                                selectedSection = section
                                val targetRoute = when (section) {
                                    NavSection.HOME -> "home"
                                    NavSection.SEARCH -> "search"
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
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = "home"
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
                                    onPlayClick = { iframeUrl, _, _ ->
                                        val encodedUrl = URLEncoder.encode(iframeUrl, StandardCharsets.UTF_8.toString())
                                        navController.navigate("player/$encodedUrl/$mediaId")
                                    }
                                )
                            }

                            composable(
                                route = "player/{iframeUrl}/{mediaId}",
                                arguments = listOf(
                                    navArgument("iframeUrl") { type = NavType.StringType },
                                    navArgument("mediaId") { type = NavType.StringType }
                                )
                            ) { backStack ->
                                val encodedUrl = backStack.arguments?.getString("iframeUrl") ?: ""
                                val mediaId = backStack.arguments?.getString("mediaId") ?: ""
                                val decodedUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())

                                PlayerScreen(
                                    iframeUrl = decodedUrl,
                                    mediaId = mediaId,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
