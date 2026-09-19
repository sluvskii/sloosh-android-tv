package com.sloosh.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
                        // 1. Content Layer (AppNavHost permanently mounted, full size without dynamic padding)
                        AppNavHost(navController = navController)

                        // 2. Cinematic Scrim over Content when Sidebar is Open
                        val scrimAlpha by animateFloatAsState(
                            targetValue = if (isDrawerOpen && showDrawer) 0.60f else 0.0f,
                            animationSpec = tween(180),
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

                        // 3. Side Navigation Rail Layer (Seamless background, smooth animated slide/fade)
                        val drawerAlpha by animateFloatAsState(
                            targetValue = if (showDrawer) 1f else 0f,
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                            label = "drawerAlpha"
                        )
                        val drawerOffset by animateDpAsState(
                            targetValue = if (showDrawer) 0.dp else (-72).dp,
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                            label = "drawerOffset"
                        )

                        if (drawerAlpha > 0.001f) {
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
                                },
                                modifier = Modifier
                                    .offset(x = drawerOffset)
                                    .graphicsLayer { alpha = drawerAlpha }
                                    .focusProperties { canFocus = showDrawer }
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
            fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing))
        },
        modifier = Modifier.fillMaxSize()
    ) {
        composable("home") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 72.dp)
            ) {
                HomeScreen(
                    onMediaSelected = { mediaId ->
                        navController.navigate("details/${mediaId}")
                    }
                )
            }
        }

        composable("search") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 72.dp)
            ) {
                SearchScreen(
                    onMediaSelected = { mediaId ->
                        navController.navigate("details/${mediaId}")
                    }
                )
            }
        }

        composable("continue") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 72.dp)
            ) {
                ContinueScreen(
                    onMediaSelected = { mediaId ->
                        navController.navigate("details/${mediaId}")
                    }
                )
            }
        }

        composable("favorites") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 72.dp)
            ) {
                ProfileScreen(
                    onMediaSelected = { mediaId ->
                        navController.navigate("details/${mediaId}")
                    }
                )
            }
        }

        composable("settings") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 72.dp)
            ) {
                SettingsScreen()
            }
        }

        composable(
            route = "details/{mediaId}",
            arguments = listOf(navArgument("mediaId") { type = NavType.StringType }),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> (fullWidth * 0.05f).toInt() },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> (fullWidth * 0.05f).toInt() },
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                )
            }
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
            arguments = listOf(navArgument("personId") { type = NavType.StringType }),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> (fullWidth * 0.05f).toInt() },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> (fullWidth * 0.05f).toInt() },
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                )
            }
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
