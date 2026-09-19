package com.sloosh.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.focus.FocusRequester
import androidx.navigation.navArgument
import com.sloosh.tv.ui.components.NavSection
import com.sloosh.tv.ui.components.SlooshSideDrawer
import com.sloosh.tv.ui.continue_watching.ContinueScreen
import com.sloosh.tv.ui.details.DetailsScreen
import com.sloosh.tv.ui.person.PersonDetailScreen
import com.sloosh.tv.ui.home.HomeScreen
import com.sloosh.tv.ui.home.HomeViewModel
import com.sloosh.tv.ui.player.PlayerScreen
import com.sloosh.tv.ui.profile.ProfileScreen
import com.sloosh.tv.ui.search.SearchScreen
import com.sloosh.tv.ui.settings.SettingsScreen
import com.sloosh.tv.ui.theme.BackgroundDark
import com.sloosh.tv.ui.theme.SlooshTVTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class SideDrawerFocusBridge(
    val drawerNavFocusRequesters: Map<NavSection, FocusRequester> = emptyMap()
) {
    var isDrawerOpen: Boolean = false
    var contentFocusCallback: (() -> Unit)? = null

    fun requestContentFocus(): Boolean {
        val cb = contentFocusCallback ?: return false
        return try {
            cb()
            true
        } catch (e: Exception) {
            false
        }
    }
}

val LocalSideDrawerFocusBridge = compositionLocalOf {
    SideDrawerFocusBridge()
}

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

                val homeFocusRequester = remember { FocusRequester() }
                val searchFocusRequester = remember { FocusRequester() }
                val continueFocusRequester = remember { FocusRequester() }
                val favoritesFocusRequester = remember { FocusRequester() }
                val settingsFocusRequester = remember { FocusRequester() }

                val focusBridge = remember {
                    SideDrawerFocusBridge(
                        drawerNavFocusRequesters = mapOf(
                            NavSection.HOME to homeFocusRequester,
                            NavSection.SEARCH to searchFocusRequester,
                            NavSection.CONTINUE to continueFocusRequester,
                            NavSection.FAVORITES to favoritesFocusRequester,
                            NavSection.SETTINGS to settingsFocusRequester
                        )
                    )
                }

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
                var isDrawerOpen by remember { mutableStateOf(false) }
                focusBridge.isDrawerOpen = isDrawerOpen

                LaunchedEffect(currentRoute) {
                    isDrawerOpen = false
                }

                CompositionLocalProvider(LocalSideDrawerFocusBridge provides focusBridge) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BackgroundDark)
                    ) {
                        // 1. Content Layer (AppNavHost permanently mounted, padded 72dp on tab screens)
                        val drawerDockWidth = 72.dp
                        val contentStartPadding = if (showDrawer) drawerDockWidth else 0.dp

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = contentStartPadding)
                        ) {
                            AppNavHost(navController = navController)
                        }

                        // 2. Cinematic Scrim over Content when Sidebar is Open
                        if (showDrawer) {
                            val scrimAlpha by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = if (isDrawerOpen) 0.60f else 0.0f,
                                animationSpec = androidx.compose.animation.core.tween(180),
                                label = "drawerScrim"
                            )
                            if (scrimAlpha > 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 72.dp)
                                        .background(Color.Black.copy(alpha = scrimAlpha))
                                )
                            }
                        }

                        // 3. Side Navigation Rail Layer (Seamless background, smooth in-place expansion)
                        if (showDrawer) {
                            SlooshSideDrawer(
                                selectedSection = selectedSection,
                                isOpen = isDrawerOpen,
                                onOpenChanged = { isDrawerOpen = it },
                                onSectionSelected = { section ->
                                    selectedSection = section
                                    isDrawerOpen = false
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
                                    coroutineScope.launch {
                                        delay(50)
                                        focusBridge.requestContentFocus()
                                    }
                                }
                            )
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
                onNavigateToPerson = { personId ->
                    navController.navigate("person/$personId")
                },
                onNavigateToMedia = { newMediaId ->
                    navController.navigate("details/$newMediaId")
                },
                onPlayClick = { iframeUrl, season, episode, movieTitle, voice, streamUrl, quality ->
                    val encodedUrl = android.util.Base64.encodeToString(
                        iframeUrl.toByteArray(StandardCharsets.UTF_8),
                        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                    )
                    val safeTitle = if (movieTitle.isBlank()) "none" else movieTitle
                    val encodedTitle = android.util.Base64.encodeToString(
                        safeTitle.toByteArray(StandardCharsets.UTF_8),
                        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                    )
                    val safeVoice = if (voice.isNullOrBlank()) "none" else voice
                    val encodedVoice = android.util.Base64.encodeToString(
                        safeVoice.toByteArray(StandardCharsets.UTF_8),
                        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                    )
                    val safeStream = if (streamUrl.isNullOrBlank()) "none" else streamUrl
                    val encodedStream = android.util.Base64.encodeToString(
                        safeStream.toByteArray(StandardCharsets.UTF_8),
                        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                    )
                    val safeQuality = if (quality.isNullOrBlank()) "auto" else quality
                    val encodedQuality = android.util.Base64.encodeToString(
                        safeQuality.toByteArray(StandardCharsets.UTF_8),
                        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
                    )
                    val seasonParam = season ?: -1
                    val epParam = episode ?: -1
                    navController.navigate("player/$encodedUrl/$mediaId/$seasonParam/$epParam/$encodedTitle?voice=$encodedVoice&streamUrl=$encodedStream&quality=$encodedQuality")
                }
            )
        }

        composable(
            route = "person/{personId}",
            arguments = listOf(navArgument("personId") { type = NavType.StringType })
        ) { backStack ->
            val personId = backStack.arguments?.getString("personId") ?: ""
            PersonDetailScreen(
                personId = personId,
                onBackClick = { navController.popBackStack() },
                onNavigateToMedia = { newMediaId ->
                    navController.navigate("details/$newMediaId")
                }
            )
        }

        composable(
            route = "player/{iframeUrl}/{mediaId}/{season}/{episode}/{title}?voice={voice}&streamUrl={streamUrl}&quality={quality}",
            arguments = listOf(
                navArgument("iframeUrl") { type = NavType.StringType },
                navArgument("mediaId") { type = NavType.StringType },
                navArgument("season") { type = NavType.IntType; defaultValue = -1 },
                navArgument("episode") { type = NavType.IntType; defaultValue = -1 },
                navArgument("title") { type = NavType.StringType; defaultValue = "none" },
                navArgument("voice") { type = NavType.StringType; defaultValue = "none" },
                navArgument("streamUrl") { type = NavType.StringType; defaultValue = "none" },
                navArgument("quality") { type = NavType.StringType; defaultValue = "auto" }
            )
        ) { backStack ->
            val rawUrlParam = backStack.arguments?.getString("iframeUrl") ?: ""
            val mediaId = backStack.arguments?.getString("mediaId") ?: ""
            val season = backStack.arguments?.getInt("season")?.takeIf { it > 0 }
            val episode = backStack.arguments?.getInt("episode")?.takeIf { it > 0 }
            val rawTitleParam = backStack.arguments?.getString("title") ?: ""
            val rawVoiceParam = backStack.arguments?.getString("voice") ?: "none"
            val rawStreamUrlParam = backStack.arguments?.getString("streamUrl") ?: "none"
            val rawQualityParam = backStack.arguments?.getString("quality") ?: "auto"

            val decodedUrl = try {
                val bytes = android.util.Base64.decode(rawUrlParam, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                String(bytes, StandardCharsets.UTF_8)
            } catch (e: Exception) {
                rawUrlParam
            }
            val decodedTitle = try {
                val bytes = android.util.Base64.decode(rawTitleParam, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                val str = String(bytes, StandardCharsets.UTF_8)
                if (str == "none" || str.isBlank()) "Просмотр" else str
            } catch (e: Exception) {
                if (rawTitleParam == "none" || rawTitleParam.isBlank()) "Просмотр" else rawTitleParam
            }
            val decodedVoice = if (rawVoiceParam != "none" && rawVoiceParam.isNotBlank()) {
                try {
                    val bytes = android.util.Base64.decode(rawVoiceParam, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                    String(bytes, StandardCharsets.UTF_8).takeIf { it.isNotBlank() && it != "none" }
                } catch (e: Exception) { null }
            } else null

            val decodedStreamUrl = if (rawStreamUrlParam != "none" && rawStreamUrlParam.isNotBlank()) {
                try {
                    val bytes = android.util.Base64.decode(rawStreamUrlParam, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                    String(bytes, StandardCharsets.UTF_8).takeIf { it.isNotBlank() && it != "none" }
                } catch (e: Exception) { null }
            } else null

            val decodedQuality = if (rawQualityParam != "auto" && rawQualityParam != "none" && rawQualityParam.isNotBlank()) {
                try {
                    val bytes = android.util.Base64.decode(rawQualityParam, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                    String(bytes, StandardCharsets.UTF_8).takeIf { it.isNotBlank() && it != "none" && it != "auto" }
                } catch (e: Exception) {
                    rawQualityParam.takeIf { it != "auto" && it != "none" }
                }
            } else null

            PlayerScreen(
                iframeUrl = decodedUrl,
                mediaId = mediaId,
                title = decodedTitle.ifEmpty { "Просмотр" },
                season = season,
                episode = episode,
                selectedVoice = decodedVoice,
                directStreamUrl = decodedStreamUrl,
                initialQuality = decodedQuality,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
