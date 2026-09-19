package com.sloosh.tv.ui.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import android.view.KeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.sloosh.tv.data.api.MediaDetailsDto
import com.sloosh.tv.data.api.MediaDto
import com.sloosh.tv.data.api.MovieCollectionDto
import com.sloosh.tv.data.api.RelatedStudioResponse
import com.sloosh.tv.ui.components.SlooshButton
import com.sloosh.tv.ui.components.SlooshFocusableCard
import com.sloosh.tv.ui.theme.*
import com.sloosh.tv.ui.util.rememberAdaptiveAmbientColor
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun DetailsScreen(
    mediaId: String,
    onPlayClick: (iframeUrl: String, season: Int?, episode: Int?, title: String, voice: String?, streamUrl: String?, quality: String?) -> Unit,
    onBackClick: (() -> Unit)? = null,
    onNavigateToPerson: ((String) -> Unit)? = null,
    onNavigateToMedia: ((String) -> Unit)? = null,
    viewModel: DetailsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val watchButtonFocusRequester = remember { FocusRequester() }

    // Dismiss source sheet when Back button is pressed on TV remote
    androidx.activity.compose.BackHandler(enabled = state.showSourceSheet) {
        viewModel.dismissSourceSheet()
    }

    LaunchedEffect(mediaId) {
        viewModel.loadDetails(mediaId)
    }

    // ─── Loading ──────────────────────────────────────────────────────────────
    if (state.isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val details = state.details
    if (details == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Не удалось загрузить данные", color = Color.White)
        }
        return
    }

    val kpId = details.ids?.kp

    Box(modifier = modifier.fillMaxSize()) {
        SidePosterDetailsLayout(
            details = details,
            state = state,
            viewModel = viewModel,
            watchButtonFocusRequester = watchButtonFocusRequester,
            onBackClick = onBackClick,
            onNavigateToPerson = onNavigateToPerson,
            onNavigateToMedia = onNavigateToMedia
        )

        // ─── Source Selection Sheet (In-hierarchy full-screen overlay) ─────────
        AnimatedVisibility(
            visible = state.showSourceSheet,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            SourceSelectionOverlay(
                state = state,
                title = details.title ?: details.originalTitle ?: "",
                onSelect = { result ->
                    viewModel.saveLastPlaybackChoice(kpId, result.translation.name, result.season, result.episode)
                    viewModel.dismissSourceSheet()
                    onPlayClick(result.translation.iframeUrl, result.season, result.episode, details.displayTitle, result.translation.name, result.translation.streamUrl, result.preferredQuality)
                },
                onRetry = { viewModel.openSourceSheet() },
                onDismiss = { viewModel.dismissSourceSheet() }
            )
        }
    }
}

// ─── Details Layout With Side Poster ──────────────────────────────────────────

@Composable
private fun SidePosterDetailsLayout(
    details: MediaDetailsDto,
    state: DetailsUiState,
    viewModel: DetailsViewModel,
    watchButtonFocusRequester: FocusRequester,
    onBackClick: (() -> Unit)? = null,
    onNavigateToPerson: ((String) -> Unit)? = null,
    onNavigateToMedia: ((String) -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val castListState = rememberLazyListState()
    val franchiseListState = rememberLazyListState()
    val similarListState = rememberLazyListState()
    val studioListState = rememberLazyListState()
    val density = LocalDensity.current
    val edgePaddingPx = with(density) { 56.dp.toPx() }
    val scrollMarginPx = with(density) { 80.dp.toPx() }
    val backButtonFocusRequester = remember { FocusRequester() }
    val moreButtonFocusRequester = remember { FocusRequester() }
    val activeCastFocusRequester = remember { FocusRequester() }
    val activeFranchiseFocusRequester = remember { FocusRequester() }
    val activeSimilarFocusRequester = remember { FocusRequester() }
    val activeStudioFocusRequester = remember { FocusRequester() }
    var isExpanded by remember { mutableStateOf(false) }
    var canExpand by remember(details.description) { mutableStateOf(false) }

    var isFirstLaunch by rememberSaveable { mutableStateOf(true) }
    var lastFocusedCastIndex by rememberSaveable { mutableIntStateOf(0) }
    var lastFocusedFranchiseIndex by rememberSaveable { mutableIntStateOf(0) }
    var lastFocusedSimilarIndex by rememberSaveable { mutableIntStateOf(0) }
    var lastFocusedStudioIndex by rememberSaveable { mutableIntStateOf(0) }
    var focusedSection by rememberSaveable { mutableStateOf("top") } // "top", "cast", "franchise", "similar", "studio"

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (isFirstLaunch) {
                    isFirstLaunch = false
                    safeRequestFocus(watchButtonFocusRequester)
                } else {
                    when (focusedSection) {
                        "cast" -> {
                            coroutineScope.launch {
                                try {
                                    castListState.scrollToItem(lastFocusedCastIndex)
                                    delay(40)
                                    safeRequestFocus(activeCastFocusRequester)
                                } catch (_: Exception) {
                                    safeRequestFocus(activeCastFocusRequester)
                                }
                            }
                        }
                        "franchise" -> {
                            coroutineScope.launch {
                                try {
                                    franchiseListState.scrollToItem(lastFocusedFranchiseIndex)
                                    delay(40)
                                    safeRequestFocus(activeFranchiseFocusRequester)
                                } catch (_: Exception) {
                                    safeRequestFocus(activeFranchiseFocusRequester)
                                }
                            }
                        }
                        "similar" -> {
                            coroutineScope.launch {
                                try {
                                    similarListState.scrollToItem(lastFocusedSimilarIndex)
                                    delay(40)
                                    safeRequestFocus(activeSimilarFocusRequester)
                                } catch (_: Exception) {
                                    safeRequestFocus(activeSimilarFocusRequester)
                                }
                            }
                        }
                        "studio" -> {
                            coroutineScope.launch {
                                try {
                                    studioListState.scrollToItem(lastFocusedStudioIndex)
                                    delay(40)
                                    safeRequestFocus(activeStudioFocusRequester)
                                } catch (_: Exception) {
                                    safeRequestFocus(activeStudioFocusRequester)
                                }
                            }
                        }
                        else -> {
                            safeRequestFocus(watchButtonFocusRequester)
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (isFirstLaunch) {
            if (!safeRequestFocus(watchButtonFocusRequester)) {
                delay(60)
                safeRequestFocus(watchButtonFocusRequester)
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    val noOpBringIntoViewResponder = remember {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect = localRect
            override suspend fun bringChildIntoView(localRect: () -> Rect?) {
                // Intentionally consume child bringIntoView so child card focus doesn't trigger jumpy edge scrolls
            }
        }
    }

    // Centering scroll automation on focused section changes
    LaunchedEffect(focusedSection, castSectionY, franchiseSectionY, similarSectionY, studioSectionY) {
        when (focusedSection) {
            "top" -> {
                scrollState.animateScrollTo(0, animationSpec = tween(300, easing = FastOutSlowInEasing))
            }
            "cast" -> {
                if (castSectionY > 0f && screenHeightPx > 0f) {
                    val center = castSectionY + (castSectionHeight / 2f)
                    val target = (center - (screenHeightPx / 2f)).coerceAtLeast(0f).toInt()
                    scrollState.animateScrollTo(target, animationSpec = tween(300, easing = FastOutSlowInEasing))
                }
            }
            "franchise" -> {
                if (franchiseSectionY > 0f && screenHeightPx > 0f) {
                    val center = franchiseSectionY + (franchiseSectionHeight / 2f)
                    val target = (center - (screenHeightPx / 2f)).coerceAtLeast(0f).toInt()
                    scrollState.animateScrollTo(target, animationSpec = tween(300, easing = FastOutSlowInEasing))
                }
            }
            "similar" -> {
                if (similarSectionY > 0f && screenHeightPx > 0f) {
                    val center = similarSectionY + (similarSectionHeight / 2f)
                    val target = (center - (screenHeightPx / 2f)).coerceAtLeast(0f).toInt()
                    scrollState.animateScrollTo(target, animationSpec = tween(300, easing = FastOutSlowInEasing))
                }
            }
            "studio" -> {
                if (studioSectionY > 0f && screenHeightPx > 0f) {
                    val center = studioSectionY + (studioSectionHeight / 2f)
                    val target = (center - (screenHeightPx / 2f)).coerceAtLeast(0f).toInt()
                    scrollState.animateScrollTo(target, animationSpec = tween(300, easing = FastOutSlowInEasing))
                }
            }
        }
    }

    // Return to top section from cast or similar on TV remote Back press
    androidx.activity.compose.BackHandler(enabled = focusedSection != "top") {
        focusedSection = "top"
        safeRequestFocus(watchButtonFocusRequester)
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val posterUrl = details.getDisplayPosterUrl()
    val previewBackdropUrl = details.getPreviewBackdropUrl()
    val backdropUrl = details.getDisplayBackdropUrl() ?: posterUrl
    val ambientColor by rememberAdaptiveAmbientColor(
        primaryUrl = previewBackdropUrl ?: backdropUrl,
        fallbackUrl = posterUrl
    )

    val depthGradient = remember(ambientColor) {
        Brush.horizontalGradient(
            colorStops = arrayOf(
                0.0f to ambientColor,
                0.25f to ambientColor.copy(alpha = 0.90f),
                0.45f to ambientColor.copy(alpha = 0.60f),
                0.65f to ambientColor.copy(alpha = 0.25f),
                0.85f to Color.Transparent
            )
        )
    }

    val fadeGradientStops = remember {
        arrayOf(
            0.0f to Color.Transparent,
            0.15f to Color.Black.copy(alpha = 0.03f),
            0.30f to Color.Black.copy(alpha = 0.12f),
            0.45f to Color.Black.copy(alpha = 0.30f),
            0.60f to Color.Black.copy(alpha = 0.55f),
            0.75f to Color.Black.copy(alpha = 0.80f),
            0.90f to Color.Black.copy(alpha = 0.96f),
            1.0f to Color.Black
        )
    }


    // Плавное кинематографическое затухание постера при переходе между разделами
    val showBackdrop = (focusedSection == "top")
    val backdropAlpha by animateFloatAsState(
        targetValue = if (showBackdrop) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "backdropAlpha"
    )

    // ─── Cinematic Entrance Animation (GPU-only, zero CPU layout cost) ───
    var isEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isEntered = true
    }

    // 1. Ambient background bloom (fades in smoothly over 400ms)
    val ambientAlpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "ambientAlpha"
    )

    // 2. IMAX-style backdrop settle: softly settles from 1.05 to 1.00 over 650ms
    val backdropScale by animateFloatAsState(
        targetValue = if (isEntered) 1.0f else 1.05f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "backdropEntranceScale"
    )

    // 3. Left content panel staggered entrance (-24dp -> 0dp slide + fade)
    val contentEntranceOffset by animateDpAsState(
        targetValue = if (isEntered) 0.dp else (-24).dp,
        animationSpec = tween(durationMillis = 360, delayMillis = 40, easing = FastOutSlowInEasing),
        label = "contentEntranceOffset"
    )
    val contentEntranceAlpha by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(durationMillis = 320, delayMillis = 40, easing = FastOutSlowInEasing),
        label = "contentEntranceAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ambientColor.copy(alpha = ambientAlpha))
            .onGloballyPositioned { coordinates ->
                screenHeightPx = coordinates.size.height.toFloat()
            }
    ) {
        // Subtle depth gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(depthGradient)
        )

        // Native aspect-ratio backdrop shifted to the right, smoothly dissolving on scroll down
        if (backdropAlpha > 0.005f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = backdropAlpha
                        scaleX = backdropScale
                        scaleY = backdropScale
                    },
                contentAlignment = Alignment.CenterEnd
            ) {
                AsyncImage(
                    model = backdropUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(16f / 9f)
                        .offset(x = 155.dp)
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colorStops = fadeGradientStops,
                                    startX = 0f,
                                    endX = size.width * 0.72f
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        },
                    contentScale = ContentScale.FillHeight
                )
            }
        }

        // Full-screen vertical scrollable column (unrestricted width for carousels)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 64.dp)
        ) {
            // ─── Top Details Section (Left 54% width) ─────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.54f)
                    .offset(x = contentEntranceOffset)
                    .graphicsLayer { alpha = contentEntranceAlpha }
                    .padding(start = 56.dp, top = 36.dp, end = 24.dp)
            ) {
                // Top Back Button
                if (onBackClick != null) {
                    SlooshFocusableCard(
                        onClick = onBackClick,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(44.dp)
                            .focusRequester(backButtonFocusRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    focusedSection = "top"
                                }
                            }
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                                ) {
                                    safeRequestFocus(watchButtonFocusRequester)
                                    true
                                } else false
                            }
                    ) { isFocused ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (isFocused) Color.White.copy(alpha = 0.30f)
                                    else Color.White.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Назад",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Logo or Title
                val logoUrl = details.getDisplayLogoUrl()
                if (logoUrl != null) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = details.title,
                        modifier = Modifier
                            .height(80.dp)
                            .widthIn(max = 320.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Text(
                        text = details.title ?: details.originalTitle ?: "Без названия",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 42.sp,
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val kpRating = details.rating
                    if (kpRating != null && kpRating > 0) {
                        Box(
                            modifier = Modifier
                                .clip(ContinuousRoundedRectangle(7.dp))
                                .background(ratingColor(kpRating))
                                .padding(horizontal = 6.5.dp, vertical = 2.5.dp)
                        ) {
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", kpRating),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp,
                                    letterSpacing = (-0.2).sp
                                ),
                                color = Color.White
                            )
                        }
                    }

                    if (details.year != null) {
                        Text(
                            text = "${details.year}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondaryDark
                        )
                    }

                    if (details.duration != null && details.duration > 0) {
                        val h = details.duration / 60
                        val m = details.duration % 60
                        val durStr = if (h > 0) "${h} ч ${m} мин" else "$m мин"
                        Text(
                            text = durStr,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondaryDark
                        )
                    }

                    if (!details.ageRating.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(ContinuousCapsule)
                                .background(Color.White.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = details.ageRating,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Genres
                val genres = details.genres
                if (!genres.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = genres.joinToString(" • "),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            letterSpacing = (-0.1).sp
                        ),
                        color = TextSecondaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Description
                val desc = details.description
                if (!desc.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.5.sp,
                                lineHeight = 19.5.sp
                            ),
                            color = Color.White.copy(alpha = 0.72f),
                            textAlign = TextAlign.Start,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { textLayoutResult ->
                                if (!isExpanded) {
                                    canExpand = textLayoutResult.hasVisualOverflow || textLayoutResult.lineCount > 4
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (canExpand) {
                            Spacer(modifier = Modifier.height(8.dp))
                            SlooshFocusableCard(
                                onClick = { isExpanded = !isExpanded },
                                shape = ContinuousCapsule,
                                modifier = Modifier
                                    .wrapContentSize()
                                    .focusRequester(moreButtonFocusRequester)
                                    .onFocusChanged {
                                        if (it.isFocused) {
                                            focusedSection = "top"
                                        }
                                    }
                                    .onPreviewKeyEvent { keyEvent ->
                                        if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                            when (keyEvent.nativeKeyEvent.keyCode) {
                                                KeyEvent.KEYCODE_DPAD_UP -> {
                                                    if (onBackClick != null) {
                                                        safeRequestFocus(backButtonFocusRequester)
                                                        true
                                                    } else false
                                                }
                                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                    safeRequestFocus(watchButtonFocusRequester)
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                            ) { isFocused ->
                                Box(
                                    modifier = Modifier
                                        .clip(ContinuousCapsule)
                                        .background(
                                            if (isFocused) Color.White.copy(alpha = 0.28f)
                                            else Color.White.copy(alpha = 0.12f)
                                        )
                                        .padding(horizontal = 14.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isExpanded) "Свернуть" else "Ещё",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            letterSpacing = 0.sp
                                        ),
                                        color = if (isFocused) Color.White else Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Progress Bar
                val prog = state.progress
                if (prog != null && prog.progressFraction > 0.01f) {
                    val posSec = prog.positionSec.toInt()
                    val durSec = prog.durationSec.toInt()
                    val posStr = String.format("%02d:%02d", posSec / 60, posSec % 60)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "Просмотрено $posStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.90f)
                        )
                        if (durSec > 0) {
                            val durStr = if (durSec >= 3600)
                                String.format("%d:%02d:%02d", durSec / 3600, (durSec % 3600) / 60, durSec % 60)
                            else
                                String.format("%02d:%02d", durSec / 60, durSec % 60)
                            Text(
                                text = " / $durStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedDark
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { prog.progressFraction },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(3.dp)
                            .clip(ContinuousCapsule)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Action Buttons (Watch & Favorite)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val hasProgress = prog != null && prog.positionSec > 10
                    val buttonText = if (hasProgress) {
                        val posStr = String.format("%02d:%02d", prog!!.positionSec.toInt() / 60, prog.positionSec.toInt() % 60)
                        "Продолжить с $posStr"
                    } else "Смотреть"

                    SlooshButton(
                        text = buttonText,
                        onClick = { viewModel.openSourceSheet() },
                        isWhite = true,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .focusRequester(watchButtonFocusRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    focusedSection = "top"
                                }
                            }
                                     .onPreviewKeyEvent { keyEvent ->
                                         if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                             when (keyEvent.nativeKeyEvent.keyCode) {
                                                 KeyEvent.KEYCODE_DPAD_UP -> {
                                                     if (canExpand) {
                                                         safeRequestFocus(moreButtonFocusRequester)
                                                         true
                                                     } else if (onBackClick != null) {
                                                         safeRequestFocus(backButtonFocusRequester)
                                                         true
                                                     } else false
                                                 }
                                                 KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                     val cast = details.cast
                                                     val movieCollection = state.movieCollection ?: details.collection
                                                     val franchiseParts = movieCollection?.parts
                                                     val similar = details.similar
                                                     val studio = state.relatedStudio?.allItems
                                                     if (!cast.isNullOrEmpty()) {
                                                         focusedSection = "cast"
                                                         val target = lastFocusedCastIndex.coerceIn(0, cast.size - 1)
                                                         coroutineScope.launch {
                                                             try { castListState.scrollToItem(target); delay(24) } catch (_: Exception) {}
                                                             safeRequestFocus(activeCastFocusRequester)
                                                         }
                                                         true
                                                     } else if (!franchiseParts.isNullOrEmpty()) {
                                                         focusedSection = "franchise"
                                                         val target = lastFocusedFranchiseIndex.coerceIn(0, franchiseParts.size - 1)
                                                         coroutineScope.launch {
                                                             try { franchiseListState.scrollToItem(target); delay(24) } catch (_: Exception) {}
                                                             safeRequestFocus(activeFranchiseFocusRequester)
                                                         }
                                                         true
                                                     } else if (!similar.isNullOrEmpty()) {
                                                         focusedSection = "similar"
                                                         val target = lastFocusedSimilarIndex.coerceIn(0, similar.size - 1)
                                                         coroutineScope.launch {
                                                             try { similarListState.scrollToItem(target); delay(24) } catch (_: Exception) {}
                                                             safeRequestFocus(activeSimilarFocusRequester)
                                                         }
                                                         true
                                                     } else if (!studio.isNullOrEmpty()) {
                                                         focusedSection = "studio"
                                                         val target = lastFocusedStudioIndex.coerceIn(0, studio.size - 1)
                                                         coroutineScope.launch {
                                                             try { studioListState.scrollToItem(target); delay(24) } catch (_: Exception) {}
                                                             safeRequestFocus(activeStudioFocusRequester)
                                                         }
                                                         true
                                                     } else false
                                                 }
                                                 else -> false
                                             }
                                         } else false
                                     }
                    )

                    var favBounce by remember { mutableStateOf(false) }
                    val favScale by animateFloatAsState(
                        targetValue = if (favBounce) 1.4f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
                        finishedListener = { favBounce = false },
                        label = "favScale"
                    )
                    SlooshFocusableCard(
                        onClick = {
                            favBounce = true
                            viewModel.toggleFavorite()
                        },
                        shape = CircleShape,
                        modifier = Modifier
                            .size(52.dp)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    focusedSection = "top"
                                }
                            }
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                    when (keyEvent.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_UP -> {
                                            if (canExpand) {
                                                safeRequestFocus(moreButtonFocusRequester)
                                                true
                                            } else if (onBackClick != null) {
                                                safeRequestFocus(backButtonFocusRequester)
                                                true
                                            } else false
                                        }
                                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            val cast = details.cast
                                            val movieCollection = state.movieCollection ?: details.collection
                                            val franchiseParts = movieCollection?.parts
                                            val similar = details.similar
                                            val studio = state.relatedStudio?.allItems
                                            if (!cast.isNullOrEmpty()) {
                                                focusedSection = "cast"
                                                val target = lastFocusedCastIndex.coerceIn(0, cast.size - 1)
                                                coroutineScope.launch {
                                                    try { castListState.scrollToItem(target); delay(24) } catch (_: Exception) {}
                                                    safeRequestFocus(activeCastFocusRequester)
                                                }
                                                true
                                            } else if (!franchiseParts.isNullOrEmpty()) {
                                                focusedSection = "franchise"
                                                val target = lastFocusedFranchiseIndex.coerceIn(0, franchiseParts.size - 1)
                                                coroutineScope.launch {
                                                    try { franchiseListState.scrollToItem(target); delay(24) } catch (_: Exception) {}
                                                    safeRequestFocus(activeFranchiseFocusRequester)
                                                }
                                                true
                                            } else if (!similar.isNullOrEmpty()) {
                                                focusedSection = "similar"
                                                val target = lastFocusedSimilarIndex.coerceIn(0, similar.size - 1)
                                                coroutineScope.launch {
                                                    try { similarListState.scrollToItem(target); delay(24) } catch (_: Exception) {}
                                                    safeRequestFocus(activeSimilarFocusRequester)
                                                }
                                                true
                                            } else if (!studio.isNullOrEmpty()) {
                                                focusedSection = "studio"
                                                val target = lastFocusedStudioIndex.coerceIn(0, studio.size - 1)
                                                coroutineScope.launch {
                                                    try { studioListState.scrollToItem(target); delay(24) } catch (_: Exception) {}
                                                    safeRequestFocus(activeStudioFocusRequester)
                                                }
                                                true
                                            } else false
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                    ) { _ ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (state.isFavorite) Color.White.copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.1f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Избранное",
                                tint = if (state.isFavorite) Color.White else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(favScale)
                            )
                        }
                    }
                }
            }

            // ─── Cast / Actors Section (Full 100% Screen Width) ───────
            val cast = details.cast
            if (!cast.isNullOrEmpty()) {
                val castItems = remember(cast) { cast.take(24) }
                val castCount = castItems.size
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            castSectionY = coordinates.positionInRoot().y + scrollState.value
                            castSectionHeight = coordinates.size.height.toFloat()
                        }
                ) {
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = "В главных ролях",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = (-0.2).sp
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(start = 56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    val castFirstItemBringIntoViewResponder = remember(edgePaddingPx, scrollMarginPx) {
                        object : BringIntoViewResponder {
                            override fun calculateRectForParent(localRect: Rect): Rect {
                                return Rect(
                                    left = localRect.left - edgePaddingPx,
                                    top = localRect.top,
                                    right = localRect.right + scrollMarginPx,
                                    bottom = localRect.bottom
                                )
                            }
                            override suspend fun bringChildIntoView(localRect: () -> Rect?) {}
                        }
                    }
                    val castLastItemBringIntoViewResponder = remember(castCount, edgePaddingPx, scrollMarginPx) {
                        object : BringIntoViewResponder {
                            override fun calculateRectForParent(localRect: Rect): Rect {
                                return Rect(
                                    left = localRect.left - scrollMarginPx,
                                    top = localRect.top,
                                    right = localRect.right + edgePaddingPx,
                                    bottom = localRect.bottom
                                )
                            }
                            override suspend fun bringChildIntoView(localRect: () -> Rect?) {}
                        }
                    }
                    val castSingleItemBringIntoViewResponder = remember(edgePaddingPx) {
                        object : BringIntoViewResponder {
                            override fun calculateRectForParent(localRect: Rect): Rect {
                                return Rect(
                                    left = localRect.left - edgePaddingPx,
                                    top = localRect.top,
                                    right = localRect.right + edgePaddingPx,
                                    bottom = localRect.bottom
                                )
                            }
                            override suspend fun bringChildIntoView(localRect: () -> Rect?) {}
                        }
                    }
                    val castMiddleItemBringIntoViewResponder = remember(scrollMarginPx) {
                        object : BringIntoViewResponder {
                            override fun calculateRectForParent(localRect: Rect): Rect {
                                return Rect(
                                    left = localRect.left - scrollMarginPx,
                                    top = localRect.top,
                                    right = localRect.right + scrollMarginPx,
                                    bottom = localRect.bottom
                                )
                            }
                            override suspend fun bringChildIntoView(localRect: () -> Rect?) {}
                        }
                    }
                    LazyRow(
                        state = castListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewResponder(noOpBringIntoViewResponder),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(start = 56.dp, end = 56.dp)
                    ) {
                        itemsIndexed(castItems) { index, actor ->
                            val cardResponder = when {
                                castCount == 1 -> castSingleItemBringIntoViewResponder
                                index == 0 -> castFirstItemBringIntoViewResponder
                                index == castCount - 1 -> castLastItemBringIntoViewResponder
                                else -> castMiddleItemBringIntoViewResponder
                            }
                            val isTargetCast = (index == lastFocusedCastIndex.coerceIn(0, castCount - 1))
                            SlooshFocusableCard(
                                onClick = {
                                    onNavigateToPerson?.invoke(actor.id.toString())
                                },
                                shape = ContinuousRoundedRectangle(16.dp),
                                modifier = Modifier
                                    .width(104.dp)
                                    .then(if (isTargetCast) Modifier.focusRequester(activeCastFocusRequester) else Modifier)
                                    .bringIntoViewResponder(cardResponder)
                                    .onFocusChanged {
                                        if (it.isFocused) {
                                            focusedSection = "cast"
                                            lastFocusedCastIndex = index
                                        }
                                    }
                                    .onPreviewKeyEvent { keyEvent ->
                                        if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                            when (keyEvent.nativeKeyEvent.keyCode) {
                                                KeyEvent.KEYCODE_DPAD_UP -> {
                                                    focusedSection = "top"
                                                    safeRequestFocus(watchButtonFocusRequester)
                                                    true
                                                }
                                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                    val movieCollection = state.movieCollection ?: details.collection
                                                    val franchiseParts = movieCollection?.parts
                                                    val similar = details.similar
                                                    val studio = state.relatedStudio?.allItems
                                                    if (!franchiseParts.isNullOrEmpty()) {
                                                        focusedSection = "franchise"
                                                        val target = lastFocusedFranchiseIndex.coerceIn(0, franchiseParts.size - 1)
                                                        coroutineScope.launch {
                                                            try { franchiseListState.scrollToItem(target); delay(24) } catch (_: Exception) {}
                                                            safeRequestFocus(activeFranchiseFocusRequester)
                                                        }
                                                        true
                                                    } else if (!similar.isNullOrEmpty()) {
                                                        focusedSection = "similar"
                                                        val target = lastFocusedSimilarIndex.coerceIn(0, similar.size - 1)
                                                        coroutineScope.launch {
                                                            try { similarListState.scrollToItem(target); delay(24) } catch (_: Exception) {}
                                                            safeRequestFocus(activeSimilarFocusRequester)
                                                        }
                                                        true
                                                    } else if (!studio.isNullOrEmpty()) {
                                                        focusedSection = "studio"
                                                        val target = lastFocusedStudioIndex.coerceIn(0, studio.size - 1)
                                                        coroutineScope.launch {
                                                            try { studioListState.scrollToItem(target); delay(24) } catch (_: Exception) {}
                                                            safeRequestFocus(activeStudioFocusRequester)
                                                        }
                                                        true
                                                    } else true
                                                }
                                                KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                    if (index == 0) true else false
                                                }
                                                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                    if (index == castCount - 1) true else false
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                            ) { isFocused ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(if (isFocused) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val photoUrl = actor.getDisplayPhotoUrl()
                                        if (!photoUrl.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = photoUrl,
                                                contentDescription = actor.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = actor.name.take(1),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = actor.name,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 12.sp,
                                            lineHeight = 15.sp,
                                            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        ),
                                        color = if (isFocused) Color.White else Color.White.copy(alpha = 0.88f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!actor.character.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = actor.character,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                lineHeight = 12.sp,
                                                textAlign = TextAlign.Center
                                            ),
                                            color = TextMutedDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ─── Franchise Collection Section ("Все части франшизы") ──────
            val movieCollection = state.movieCollection ?: details.collection
            val franchiseParts = movieCollection?.parts
            if (!franchiseParts.isNullOrEmpty()) {
                val franchiseItems = remember(franchiseParts) { franchiseParts.take(20) }
                val franchiseCount = franchiseItems.size
                val franchiseResponderProvider = rememberCarouselResponders(franchiseCount, edgePaddingPx, scrollMarginPx)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            franchiseSectionY = coordinates.positionInRoot().y + scrollState.value
                            franchiseSectionHeight = coordinates.size.height.toFloat()
                        }
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Column(modifier = Modifier.padding(start = 56.dp)) {
                        Text(
                            text = "Все части франшизы",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.2).sp
                            ),
                            color = Color.White
                        )
                        if (!movieCollection.name.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = movieCollection.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = (-0.1).sp
                                ),
                                color = TextSecondaryDark
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    LazyRow(
                        state = franchiseListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewResponder(noOpBringIntoViewResponder),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(start = 56.dp, end = 56.dp)
                    ) {
                        itemsIndexed(franchiseItems) { index, item ->
                            val cardResponder = franchiseResponderProvider(index)
                            val isTargetFranchise = (index == lastFocusedFranchiseIndex.coerceIn(0, franchiseCount - 1))
                            MoviePosterRowCard(
                                item = item,
                                index = index,
                                count = franchiseCount,
                                targetFocusRequester = activeFranchiseFocusRequester,
                                isTarget = isTargetFranchise,
                                cardResponder = cardResponder,
                                onFocus = {
                                    focusedSection = "franchise"
                                    lastFocusedFranchiseIndex = index
                                },
                                onPreviewKeyEvent = { keyEvent ->
                                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                        when (keyEvent.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_DPAD_UP -> {
                                                val cast = details.cast
                                                if (!cast.isNullOrEmpty()) {
                                                    focusedSection = "cast"
                                                    val target = lastFocusedCastIndex.coerceIn(0, cast.size - 1)
                                                    coroutineScope.launch {
                                                        try { castListState.scrollToItem(target); delay(24) } catch (_: Exception) {}
                                                        safeRequestFocus(activeCastFocusRequester)
                                                    }
                                                    true
                                                } else {
                                                    focusedSection = "top"
                                                    safeRequestFocus(watchButtonFocusRequester)
                                                    true
                                                }
                                            }
                                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                val similar = details.similar
                                                val studio = state.relatedStudio?.allItems
                                                if (!similar.isNullOrEmpty()) {
                                                    focusedSection = "similar"
                                                    val target = lastFocusedSimilarIndex.coerceIn(0, similar.size - 1)
                                                    coroutineScope.launch {
                                                        try { similarListState.scrollToItem(target); delay(24) } catch (_: Exception) {}
                                                        safeRequestFocus(activeSimilarFocusRequester)
                                                    }
                                                    true
                                                } else if (!studio.isNullOrEmpty()) {
                                                    focusedSection = "studio"
                                                    val target = lastFocusedStudioIndex.coerceIn(0, studio.size - 1)
                                                    coroutineScope.launch {
                                                        try { studioListState.scrollToItem(target); delay(24) } catch (_: Exception) {}
                                                        safeRequestFocus(activeStudioFocusRequester)
                                                    }
                                                    true
                                                } else false
                                            }
                                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                if (index == 0) true else false
                                            }
                                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                if (index == franchiseCount - 1) true else false
                                            }
                                            else -> false
                                        }
                                    } else false
                                },
                                onClick = {
                                    val targetId = item.originalId ?: item.identifier
                                    if (targetId.isNotBlank()) {
                                        onNavigateToMedia?.invoke(targetId)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ─── Similar Movies / Series Section ───────────────────────
            val similar = details.similar
            if (!similar.isNullOrEmpty()) {
                val similarItems = remember(similar) { similar.take(20) }
                val similarCount = similarItems.size
                val similarResponderProvider = rememberCarouselResponders(similarCount, edgePaddingPx, scrollMarginPx)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            similarSectionY = coordinates.positionInRoot().y + scrollState.value
                            similarSectionHeight = coordinates.size.height.toFloat()
                        }
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    val similarTitle = if (details.isTvSeries) "Похожие сериалы" else "Похожие фильмы"
                    Text(
                        text = similarTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = (-0.2).sp
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(start = 56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    LazyRow(
                        state = similarListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewResponder(noOpBringIntoViewResponder),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(start = 56.dp, end = 56.dp)
                    ) {
                        itemsIndexed(similarItems) { index, item ->
                            val cardResponder = similarResponderProvider(index)
                            val isTargetSimilar = (index == lastFocusedSimilarIndex.coerceIn(0, similarCount - 1))
                            MoviePosterRowCard(
                                item = item,
                                index = index,
                                count = similarCount,
                                targetFocusRequester = activeSimilarFocusRequester,
                                isTarget = isTargetSimilar,
                                cardResponder = cardResponder,
                                onFocus = {
                                    focusedSection = "similar"
                                    lastFocusedSimilarIndex = index
                                },
                                onPreviewKeyEvent = { keyEvent ->
                                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                        when (keyEvent.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_DPAD_UP -> {
                                                val movieCollection = state.movieCollection ?: details.collection
                                                val franchiseParts = movieCollection?.parts
                                                val cast = details.cast
                                                if (!franchiseParts.isNullOrEmpty()) {
                                                    focusedSection = "franchise"
                                                    val target = lastFocusedFranchiseIndex.coerceIn(0, franchiseParts.size - 1)
                                                    coroutineScope.launch {
                                                        try { franchiseListState.scrollToItem(target); delay(24) } catch (_: Exception) {}
                                                        safeRequestFocus(activeFranchiseFocusRequester)
                                                    }
                                                    true
                                                } else if (!cast.isNullOrEmpty()) {
                                                    focusedSection = "cast"
                                                    val target = lastFocusedCastIndex.coerceIn(0, cast.size - 1)
                                                    coroutineScope.launch {
                                                        try { castListState.scrollToItem(target); delay(24) } catch (_: Exception) {}
                                                        safeRequestFocus(activeCastFocusRequester)
                                                    }
                                                    true
                                                } else {
                                                    focusedSection = "top"
                                                    safeRequestFocus(watchButtonFocusRequester)
                                                    true
                                                }
                                            }
                                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                val studio = state.relatedStudio?.allItems
                                                if (!studio.isNullOrEmpty()) {
                                                    focusedSection = "studio"
                                                    val target = lastFocusedStudioIndex.coerceIn(0, studio.size - 1)
                                                    coroutineScope.launch {
                                                        try { studioListState.scrollToItem(target); delay(24) } catch (_: Exception) {}
                                                        safeRequestFocus(activeStudioFocusRequester)
                                                    }
                                                    true
                                                } else true
                                            }
                                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                if (index == 0) true else false
                                            }
                                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                if (index == similarCount - 1) true else false
                                            }
                                            else -> false
                                        }
                                    } else false
                                },
                                onClick = {
                                    val targetId = item.originalId ?: item.identifier
                                    if (targetId.isNotBlank()) {
                                        onNavigateToMedia?.invoke(targetId)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ─── Related Studio Section ("Другие релизы") ──────────────
            val relatedStudio = state.relatedStudio
            val studioItems = relatedStudio?.allItems
            if (!studioItems.isNullOrEmpty()) {
                val studioList = remember(studioItems) { studioItems.take(20) }
                val studioCount = studioList.size
                val studioResponderProvider = rememberCarouselResponders(studioCount, edgePaddingPx, scrollMarginPx)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            studioSectionY = coordinates.positionInRoot().y + scrollState.value
                            studioSectionHeight = coordinates.size.height.toFloat()
                        }
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        modifier = Modifier.padding(start = 56.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Другие релизы",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.2).sp
                            ),
                            color = Color.White
                        )
                        if (!relatedStudio.label.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(ContinuousCapsule)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = relatedStudio.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = (-0.1).sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    LazyRow(
                        state = studioListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewResponder(noOpBringIntoViewResponder),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(start = 56.dp, end = 56.dp)
                    ) {
                        itemsIndexed(studioList) { index, item ->
                            val cardResponder = studioResponderProvider(index)
                            val isTargetStudio = (index == lastFocusedStudioIndex.coerceIn(0, studioCount - 1))
                            MoviePosterRowCard(
                                item = item,
                                index = index,
                                count = studioCount,
                                targetFocusRequester = activeStudioFocusRequester,
                                isTarget = isTargetStudio,
                                cardResponder = cardResponder,
                                onFocus = {
                                    focusedSection = "studio"
                                    lastFocusedStudioIndex = index
                                },
                                onPreviewKeyEvent = { keyEvent ->
                                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                        when (keyEvent.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_DPAD_UP -> {
                                                val similar = details.similar
                                                val movieCollection = state.movieCollection ?: details.collection
                                                val franchiseParts = movieCollection?.parts
                                                val cast = details.cast
                                                if (!similar.isNullOrEmpty()) {
                                                    focusedSection = "similar"
                                                    val target = lastFocusedSimilarIndex.coerceIn(0, similar.size - 1)
                                                    coroutineScope.launch {
                                                        try { similarListState.scrollToItem(target); delay(24) } catch (_: Exception) {}
                                                        safeRequestFocus(activeSimilarFocusRequester)
                                                    }
                                                    true
                                                } else if (!franchiseParts.isNullOrEmpty()) {
                                                    focusedSection = "franchise"
                                                    val target = lastFocusedFranchiseIndex.coerceIn(0, franchiseParts.size - 1)
                                                    coroutineScope.launch {
                                                        try { franchiseListState.scrollToItem(target); delay(24) } catch (_: Exception) {}
                                                        safeRequestFocus(activeFranchiseFocusRequester)
                                                    }
                                                    true
                                                } else if (!cast.isNullOrEmpty()) {
                                                    focusedSection = "cast"
                                                    val target = lastFocusedCastIndex.coerceIn(0, cast.size - 1)
                                                    coroutineScope.launch {
                                                        try { castListState.scrollToItem(target); delay(24) } catch (_: Exception) {}
                                                        safeRequestFocus(activeCastFocusRequester)
                                                    }
                                                    true
                                                } else {
                                                    focusedSection = "top"
                                                    safeRequestFocus(watchButtonFocusRequester)
                                                    true
                                                }
                                            }
                                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                // Bottom-most section on screen: block overscroll downwards
                                                true
                                            }
                                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                if (index == 0) true else false
                                            }
                                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                if (index == studioCount - 1) true else false
                                            }
                                            else -> false
                                        }
                                    } else false
                                },
                                onClick = {
                                    val targetId = item.originalId ?: item.identifier
                                    if (targetId.isNotBlank()) {
                                        onNavigateToMedia?.invoke(targetId)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

// ─── Helpers for Carousels & Poster Cards ─────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberCarouselResponders(
    count: Int,
    edgePaddingPx: Float,
    scrollMarginPx: Float
): (Int) -> BringIntoViewResponder {
    val firstResponder = remember(edgePaddingPx, scrollMarginPx) {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect {
                return Rect(
                    left = localRect.left - edgePaddingPx,
                    top = localRect.top,
                    right = localRect.right + scrollMarginPx,
                    bottom = localRect.bottom
                )
            }
            override suspend fun bringChildIntoView(localRect: () -> Rect?) {}
        }
    }
    val lastResponder = remember(count, edgePaddingPx, scrollMarginPx) {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect {
                return Rect(
                    left = localRect.left - scrollMarginPx,
                    top = localRect.top,
                    right = localRect.right + edgePaddingPx,
                    bottom = localRect.bottom
                )
            }
            override suspend fun bringChildIntoView(localRect: () -> Rect?) {}
        }
    }
    val singleResponder = remember(edgePaddingPx) {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect {
                return Rect(
                    left = localRect.left - edgePaddingPx,
                    top = localRect.top,
                    right = localRect.right + edgePaddingPx,
                    bottom = localRect.bottom
                )
            }
            override suspend fun bringChildIntoView(localRect: () -> Rect?) {}
        }
    }
    val middleResponder = remember(scrollMarginPx) {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect {
                return Rect(
                    left = localRect.left - scrollMarginPx,
                    top = localRect.top,
                    right = localRect.right + scrollMarginPx,
                    bottom = localRect.bottom
                )
            }
            override suspend fun bringChildIntoView(localRect: () -> Rect?) {}
        }
    }

    return remember(count, firstResponder, lastResponder, singleResponder, middleResponder) {
        { index: Int ->
            when {
                count == 1 -> singleResponder
                index == 0 -> firstResponder
                index == count - 1 -> lastResponder
                else -> middleResponder
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MoviePosterRowCard(
    item: MediaDto,
    index: Int,
    count: Int,
    targetFocusRequester: FocusRequester? = null,
    isTarget: Boolean = false,
    cardResponder: BringIntoViewResponder,
    onFocus: () -> Unit,
    onPreviewKeyEvent: (androidx.compose.ui.input.key.KeyEvent) -> Boolean,
    onClick: () -> Unit
) {
    val targetId = item.originalId ?: item.identifier
    SlooshFocusableCard(
        onClick = {
            if (targetId.isNotBlank()) {
                onClick()
            }
        },
        shape = ContinuousRoundedRectangle(16.dp),
        modifier = Modifier
            .width(130.dp)
            .height(195.dp)
            .then(if (isTarget && targetFocusRequester != null) Modifier.focusRequester(targetFocusRequester) else Modifier)
            .bringIntoViewResponder(cardResponder)
            .onFocusChanged {
                if (it.isFocused) {
                    onFocus()
                }
            }
            .onPreviewKeyEvent { onPreviewKeyEvent(it) }
    ) { isFocused ->
        Box(modifier = Modifier.fillMaxSize()) {
            val itemPoster = item.getDisplayPosterUrl()
            if (!itemPoster.isNullOrEmpty()) {
                AsyncImage(
                    model = itemPoster,
                    contentDescription = item.displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceDark),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.displayTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                        )
                    )
            )

            val rating = item.rating ?: item.ratings?.kp ?: item.ratings?.imdb
            if (rating != null && rating > 0.0) {
                val ratingColor = when {
                    rating >= 7.0 -> RatingIosGreen
                    rating >= 5.0 -> RatingIosGray
                    else -> RatingIosRed
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(ContinuousCapsule)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = String.format(Locale.ROOT, "%.1f", rating),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = ratingColor
                    )
                }
            }

            Text(
                text = item.displayTitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}

// ─── Focus Utilities ──────────────────────────────────────────────────────────

/**
 * Безопасный вызов requestFocus — не крашится если узел ещё не прикреплён к иерархии.
 * Возвращает true при успехе, false если узел не готов.
 */
private fun safeRequestFocus(requester: FocusRequester): Boolean {
    return try {
        requester.requestFocus()
        true
    } catch (e: IllegalStateException) {
        false
    } catch (e: Exception) {
        false
    }
}
