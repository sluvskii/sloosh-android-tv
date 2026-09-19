package com.sloosh.tv.ui.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.sloosh.tv.data.api.AllohaApiResult
import com.sloosh.tv.data.api.AllohaTranslation
import com.sloosh.tv.data.repository.AppSettings
import com.sloosh.tv.data.repository.VideoQualityPreference
import com.sloosh.tv.data.repository.allohaTranslationNamesMatch
import com.sloosh.tv.ui.components.SlooshButton
import com.sloosh.tv.ui.components.SlooshFocusableCard

// ─── Data class for dialog result ────────────────────────────────────────────

data class SourceSelectionResult(
    val translation: AllohaTranslation,
    val season: Int?,
    val episode: Int?,
    val preferredQuality: String? = null
)

// ─── Availability helpers (ported 1:1 from iOS SourceSelectionView.swift) ───

private fun seasonHasTranslation(result: AllohaApiResult, season: Int, tName: String): Boolean {
    val s = result.seasons.find { it.season == season } ?: return false
    return s.episodes.any { ep -> ep.translations.any { allohaTranslationNamesMatch(it.name, tName, exactOnly = true) } }
}

private fun episodeHasTranslation(result: AllohaApiResult, season: Int, episode: Int, tName: String): Boolean {
    val s = result.seasons.find { it.season == season } ?: return false
    val ep = s.episodes.find { it.episode == episode } ?: return false
    return ep.translations.any { allohaTranslationNamesMatch(it.name, tName, exactOnly = true) }
}

private fun isTranslationAvailable(result: AllohaApiResult, tName: String, season: Int?, episode: Int?): Boolean {
    return if (result.isSerial) {
        if (season == null || episode == null) false
        else episodeHasTranslation(result, season, episode, tName)
    } else {
        result.movie?.translations?.any { it.name == tName } == true
    }
}

private fun isSeasonAvailable(result: AllohaApiResult, seasonNum: Int, tName: String?): Boolean {
    if (tName == null) return true
    return seasonHasTranslation(result, seasonNum, tName)
}

private fun isEpisodeAvailable(result: AllohaApiResult, season: Int?, episodeNum: Int, tName: String?): Boolean {
    if (season == null || tName == null) return true
    return episodeHasTranslation(result, season, episodeNum, tName)
}

// ─── All unique translation names across the entire result ───────────────────

private fun allTranslationNames(result: AllohaApiResult): List<String> {
    return if (result.isSerial) {
        val names = linkedSetOf<String>()
        result.seasons.forEach { s -> s.episodes.forEach { ep -> ep.translations.forEach { t -> names.add(t.name) } } }
        names.toList().sorted()
    } else {
        result.movie?.translations?.map { it.name }?.sorted() ?: emptyList()
    }
}

// ─── Preferred translation selection (mirrors iOS preferredTranslation()) ────

private fun preferredTranslation(
    translations: List<AllohaTranslation>,
    preferredName: String?,
    globalLastName: String?
): AllohaTranslation? {
    if (translations.isEmpty()) return null
    // 1. Per-show preference
    if (preferredName != null) {
        translations.find { allohaTranslationNamesMatch(it.name, preferredName, exactOnly = true) }
            ?.let { return it }
    }
    // 2. Global last-used
    if (globalLastName != null) {
        translations.find { allohaTranslationNamesMatch(it.name, globalLastName, exactOnly = false) }
            ?.let { return it }
    }
    // 3. First available
    return translations.first()
}

// ─── Primary Full-Screen Overlay ─────────────────────────────────────────────

@Composable
fun SourceSelectionOverlay(
    state: DetailsUiState,
    title: String,
    onSelect: (SourceSelectionResult) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .padding(horizontal = 48.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            state.isFetchingSources -> {
                SourceSelectionLoadingView(
                    title = title,
                    onDismiss = onDismiss
                )
            }
            state.sourceFetchError != null || (state.allohaData != null && state.allohaData.isSerial && state.allohaData.seasons.isEmpty()) || (state.allohaData != null && !state.allohaData.isSerial && state.allohaData.movie?.translations.isNullOrEmpty()) -> {
                SourceSelectionErrorView(
                    title = title,
                    errorMessage = state.sourceFetchError ?: "Источники для воспроизведения не найдены",
                    onRetry = onRetry,
                    onDismiss = onDismiss
                )
            }
            state.allohaData != null -> {
                SourceSelectionContentView(
                    allohaData = state.allohaData,
                    savedVoiceover = state.savedVoiceover,
                    globalLastVoiceover = state.globalLastVoiceover,
                    lastSeason = state.lastSeason ?: state.progress?.season,
                    lastEpisode = state.lastEpisode ?: state.progress?.episode,
                    onSelect = onSelect,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

// ─── Content View (Translation, Season, Episode Pickers) ──────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SourceSelectionContentView(
    allohaData: AllohaApiResult,
    savedVoiceover: String?,
    globalLastVoiceover: String?,
    lastSeason: Int?,
    lastEpisode: Int?,
    onSelect: (SourceSelectionResult) -> Unit,
    onDismiss: () -> Unit
) {
    val isSerial = allohaData.isSerial || allohaData.seasons.isNotEmpty()

    val allTranslations = remember(allohaData) { allTranslationNames(allohaData) }
    val allSeasons = remember(allohaData) { allohaData.seasons.map { it.season } }

    var selectedSeason by remember {
        mutableStateOf(
            if (isSerial) {
                lastSeason?.let { ls ->
                    allohaData.seasons.find { it.season == ls }?.season
                } ?: allohaData.seasons.firstOrNull()?.season
            } else null
        )
    }
    var selectedEpisode by remember {
        mutableStateOf(
            if (isSerial) {
                val season = selectedSeason
                val seasonObj = allohaData.seasons.find { it.season == season }
                lastEpisode?.let { le ->
                    seasonObj?.episodes?.find { it.episode == le }?.episode
                } ?: seasonObj?.episodes?.firstOrNull()?.episode
            } else null
        )
    }
    var selectedTranslationName by remember {
        mutableStateOf(
            if (isSerial) {
                val season = selectedSeason
                val episode = selectedEpisode
                val seasonObj = allohaData.seasons.find { it.season == season }
                val epObj = seasonObj?.episodes?.find { it.episode == episode }
                epObj?.let { ep ->
                    preferredTranslation(ep.translations, savedVoiceover, globalLastVoiceover)?.name
                }
            } else {
                val translations = allohaData.movie?.translations ?: emptyList()
                preferredTranslation(translations, savedVoiceover, globalLastVoiceover)?.name
            }
        )
    }

    val currentEpisodes = remember(selectedSeason, allohaData) {
        allohaData.seasons.find { it.season == selectedSeason }?.episodes?.map { it.episode } ?: emptyList()
    }

    val isReadyToPlay = remember(selectedTranslationName, selectedSeason, selectedEpisode) {
        if (selectedTranslationName == null) false
        else if (isSerial) selectedSeason != null && selectedEpisode != null
        else true
    }

    fun selectTranslation(name: String) {
        selectedTranslationName = name
        if (isSerial) {
            val curSeason = selectedSeason
            if (curSeason != null && !seasonHasTranslation(allohaData, curSeason, name)) {
                val newSeason = allohaData.seasons.firstOrNull { seasonHasTranslation(allohaData, it.season, name) }
                if (newSeason != null) selectedSeason = newSeason.season
            }
            val s = selectedSeason
            val e = selectedEpisode
            if (s != null && e != null && !episodeHasTranslation(allohaData, s, e, name)) {
                val seasonObj = allohaData.seasons.find { it.season == s }
                val newEp = seasonObj?.episodes?.firstOrNull { ep ->
                    ep.translations.any { allohaTranslationNamesMatch(it.name, name, exactOnly = true) }
                }
                if (newEp != null) selectedEpisode = newEp.episode
            }
        }
    }

    fun selectSeason(s: Int) {
        selectedSeason = s
        val seasonObj = allohaData.seasons.find { it.season == s } ?: return
        if (selectedEpisode == null || !seasonObj.episodes.any { it.episode == selectedEpisode }) {
            selectedEpisode = seasonObj.episodes.firstOrNull()?.episode ?: 1
        }
        val curT = selectedTranslationName
        val curE = selectedEpisode
        if (curT != null && curE != null && !episodeHasTranslation(allohaData, s, curE, curT)) {
            val epObj = seasonObj.episodes.find { it.episode == curE }
            val firstT = epObj?.translations?.firstOrNull()
            if (firstT != null) selectedTranslationName = firstT.name
        }
    }

    fun selectEpisode(e: Int) {
        selectedEpisode = e
        val s = selectedSeason ?: return
        val curT = selectedTranslationName
        if (curT != null && !episodeHasTranslation(allohaData, s, e, curT)) {
            val seasonObj = allohaData.seasons.find { it.season == s }
            val epObj = seasonObj?.episodes?.find { it.episode == e }
            val firstT = epObj?.translations?.firstOrNull()
            if (firstT != null) selectedTranslationName = firstT.name
        }
    }

    val context = LocalContext.current
    val appSettings = remember { AppSettings(context) }
    var showQualityPicker by remember { mutableStateOf(false) }
    var pendingTranslation by remember { mutableStateOf<AllohaTranslation?>(null) }
    var pendingSeason by remember { mutableStateOf<Int?>(null) }
    var pendingEpisode by remember { mutableStateOf<Int?>(null) }

    BackHandler(enabled = showQualityPicker) {
        showQualityPicker = false
    }

    fun finishAction() {
        if (isSerial) {
            val s = selectedSeason ?: return
            val e = selectedEpisode ?: return
            val tName = selectedTranslationName ?: return
            val seasonObj = allohaData.seasons.find { it.season == s } ?: return
            val epObj = seasonObj.episodes.find { it.episode == e } ?: return
            val translation = epObj.translations.firstOrNull {
                allohaTranslationNamesMatch(it.name, tName, exactOnly = true)
            } ?: epObj.translations.firstOrNull() ?: return

            if (appSettings.preferredQuality == VideoQualityPreference.ASK) {
                pendingTranslation = translation
                pendingSeason = s
                pendingEpisode = e
                showQualityPicker = true
            } else {
                onSelect(SourceSelectionResult(translation, s, e, appSettings.preferredQuality.id))
            }
        } else {
            val tName = selectedTranslationName ?: return
            val translation = allohaData.movie?.translations?.firstOrNull {
                it.name == tName
            } ?: allohaData.movie?.translations?.firstOrNull() ?: return

            if (appSettings.preferredQuality == VideoQualityPreference.ASK) {
                pendingTranslation = translation
                pendingSeason = null
                pendingEpisode = null
                showQualityPicker = true
            } else {
                onSelect(SourceSelectionResult(translation, null, null, appSettings.preferredQuality.id))
            }
        }
    }

    val playButtonFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try {
            playButtonFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    if (showQualityPicker && pendingTranslation != null) {
        QualitySelectionContentView(
            title = allohaData.title,
            initialQuality = VideoQualityPreference.AUTO,
            onConfirm = { chosenQuality, rememberChoice ->
                if (rememberChoice) {
                    appSettings.preferredQuality = chosenQuality
                }
                onSelect(
                    SourceSelectionResult(
                        translation = pendingTranslation!!,
                        season = pendingSeason,
                        episode = pendingEpisode,
                        preferredQuality = chosenQuality.id
                    )
                )
            },
            onBack = {
                showQualityPicker = false
            }
        )
    } else {
        Column(
            modifier = Modifier
                .widthIn(max = 940.dp)
                .fillMaxHeight()
                .clip(ContinuousRoundedRectangle(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1C1C1E), Color(0xFF141416))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.12f), ContinuousRoundedRectangle(24.dp))
        ) {
            // ─── Header ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp, end = 28.dp, top = 24.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = allohaData.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = if (isSerial) "Выбор озвучки, сезона и серии" else "Выбор озвучки",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onDismiss() }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            // ─── Scrollable Pickers ──────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 36.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Translation picker
                SectionLabel("Озвучка")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    allTranslations.forEach { tName ->
                        val isSelected = selectedTranslationName != null && allohaTranslationNamesMatch(tName, selectedTranslationName!!, exactOnly = true)
                        val isAvailable = isTranslationAvailable(allohaData, tName, selectedSeason, selectedEpisode)
                        SelectorChip(
                            label = tName,
                            isSelected = isSelected,
                            isAvailable = isAvailable,
                            onClick = { selectTranslation(tName) }
                        )
                    }
                }

                // 2. Season picker (if serial)
                if (isSerial && allSeasons.size > 1) {
                    SectionLabel("Сезон")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        allSeasons.forEach { sNum ->
                            val isSelected = selectedSeason == sNum
                            val isAvailable = isSeasonAvailable(allohaData, sNum, selectedTranslationName)
                            SelectorChip(
                                label = "$sNum сезон",
                                isSelected = isSelected,
                                isAvailable = isAvailable,
                                onClick = { selectSeason(sNum) }
                            )
                        }
                    }
                }

                // 3. Episode picker (if serial)
                if (isSerial && currentEpisodes.isNotEmpty()) {
                    SectionLabel("Серия")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        currentEpisodes.forEach { eNum ->
                            val isSelected = selectedEpisode == eNum
                            val isAvailable = isEpisodeAvailable(allohaData, selectedSeason, eNum, selectedTranslationName)
                            SelectorChip(
                                label = "$eNum серия",
                                isSelected = isSelected,
                                isAvailable = isAvailable,
                                onClick = { selectEpisode(eNum) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
            }

            // ─── Bottom "Смотреть" button ────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp, vertical = 20.dp)
            ) {
                PlayButton(
                    isReadyToPlay = isReadyToPlay,
                    focusRequester = playButtonFocusRequester,
                    onClick = { if (isReadyToPlay) finishAction() }
                )
            }
        }
    }
}

// ─── Quality Selection View (iOS Parity) ──────────────────────────────────────

@Composable
fun QualitySelectionContentView(
    title: String,
    initialQuality: VideoQualityPreference = VideoQualityPreference.AUTO,
    onConfirm: (VideoQualityPreference, Boolean) -> Unit,
    onBack: () -> Unit
) {
    var selectedQuality by remember { mutableStateOf(initialQuality) }
    var rememberChoice by remember { mutableStateOf(false) }

    val continueButtonFocusRequester = remember { FocusRequester() }
    val qualityOptions = remember {
        listOf(
            VideoQualityPreference.AUTO,
            VideoQualityPreference.Q1080,
            VideoQualityPreference.Q720,
            VideoQualityPreference.Q480,
            VideoQualityPreference.Q360
        )
    }

    LaunchedEffect(Unit) {
        try {
            continueButtonFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .widthIn(max = 620.dp)
            .fillMaxHeight()
            .clip(ContinuousRoundedRectangle(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1C1C1E), Color(0xFF141416))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), ContinuousRoundedRectangle(24.dp))
    ) {
        // ─── Header ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp, end = 28.dp, top = 28.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Качество видео",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )
                Text(
                    text = title.ifBlank { "Выберите качество для воспроизведения" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    ),
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрыть",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onBack() }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f))
        )

        // ─── Options List ────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 36.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            qualityOptions.forEach { quality ->
                val isSelected = selectedQuality == quality
                var isFocused by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ContinuousRoundedRectangle(14.dp))
                        .background(
                            if (isFocused) Color.White.copy(alpha = 0.16f)
                            else if (isSelected) Color.White.copy(alpha = 0.08f)
                            else Color.White.copy(alpha = 0.03f)
                        )
                        .border(
                            width = if (isFocused) 1.5.dp else if (isSelected) 1.dp else 0.dp,
                            color = if (isFocused) Color.White else if (isSelected) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                            shape = ContinuousRoundedRectangle(14.dp)
                        )
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        .clickable { selectedQuality = quality }
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                                (keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                                 keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER)) {
                                selectedQuality = quality
                                true
                            } else false
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = quality.title,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 16.sp
                            ),
                            color = if (isSelected || isFocused) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ─── "Запомнить выбор" toggle row ────────────────
            var isToggleFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ContinuousRoundedRectangle(14.dp))
                    .background(
                        if (isToggleFocused) Color.White.copy(alpha = 0.16f)
                        else Color.White.copy(alpha = 0.05f)
                    )
                    .border(
                        width = if (isToggleFocused) 1.5.dp else 1.dp,
                        color = if (isToggleFocused) Color.White else Color.White.copy(alpha = 0.08f),
                        shape = ContinuousRoundedRectangle(14.dp)
                    )
                    .onFocusChanged { isToggleFocused = it.isFocused }
                    .focusable()
                    .clickable { rememberChoice = !rememberChoice }
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                            (keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                             keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER)) {
                            rememberChoice = !rememberChoice
                            true
                        } else false
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Запомнить выбор",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "Вы всегда можете изменить качество по умолчанию в настройках",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (rememberChoice) Color.White else Color.White.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (rememberChoice) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f))
        )

        // ─── Bottom Actions ──────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SlooshButton(
                text = "Назад",
                onClick = onBack,
                isWhite = false,
                icon = {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            )

            SlooshButton(
                text = "Продолжить",
                onClick = { onConfirm(selectedQuality, rememberChoice) },
                isWhite = true,
                icon = {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier
                    .weight(1.5f)
                    .focusRequester(continueButtonFocusRequester)
            )
        }
    }
}

// ─── Loading State View ──────────────────────────────────────────────────────

@Composable
fun SourceSelectionLoadingView(title: String, onDismiss: () -> Unit) {
    val cancelFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try {
            cancelFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .width(460.dp)
            .clip(ContinuousRoundedRectangle(24.dp))
            .background(Color(0xFF1C1C1E))
            .border(1.dp, Color.White.copy(alpha = 0.12f), ContinuousRoundedRectangle(24.dp))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(38.dp)
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Загрузка источников…",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = Color.White
        )
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        SlooshButton(
            text = "Отмена",
            onClick = onDismiss,
            isWhite = false,
            modifier = Modifier
                .focusRequester(cancelFocusRequester)
                .fillMaxWidth(0.6f)
        )
    }
}

// ─── Error State View ────────────────────────────────────────────────────────

@Composable
fun SourceSelectionErrorView(
    title: String,
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val retryFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try {
            retryFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .width(480.dp)
            .clip(ContinuousRoundedRectangle(24.dp))
            .background(Color(0xFF1C1C1E))
            .border(1.dp, Color.White.copy(alpha = 0.12f), ContinuousRoundedRectangle(24.dp))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Источники не найдены",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = Color.White
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = Color.White.copy(alpha = 0.60f),
            textAlign = TextAlign.Center
        )

        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Color.White.copy(alpha = 0.40f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SlooshButton(
                text = "Повторить",
                onClick = onRetry,
                isWhite = true,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(retryFocusRequester)
            )

            SlooshButton(
                text = "Закрыть",
                onClick = onDismiss,
                isWhite = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─── Backward-compatible Dialog wrappers ─────────────────────────────────────

@Composable
fun SourceSelectionDialog(
    allohaData: AllohaApiResult,
    kpId: Int? = null,
    savedVoiceover: String? = null,
    globalLastVoiceover: String? = null,
    lastSeason: Int? = null,
    lastEpisode: Int? = null,
    onSelect: (SourceSelectionResult) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .padding(horizontal = 48.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        SourceSelectionContentView(
            allohaData = allohaData,
            savedVoiceover = savedVoiceover,
            globalLastVoiceover = globalLastVoiceover,
            lastSeason = lastSeason,
            lastEpisode = lastEpisode,
            onSelect = onSelect,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun SourceSelectionLoadingDialog(title: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .padding(horizontal = 48.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        SourceSelectionLoadingView(title = title, onDismiss = onDismiss)
    }
}

// ─── Section label ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            letterSpacing = (-0.3).sp
        ),
        color = Color.White
    )
}

// ─── Selector chip using TV Button for native D-Pad navigation ────────────────

@Composable
private fun SelectorChip(
    label: String,
    isSelected: Boolean,
    isAvailable: Boolean,
    onClick: () -> Unit
) {
    val shape = ContinuousRoundedRectangle(16.dp)

    val contentAlpha = when {
        isSelected -> 1f
        isAvailable -> 0.85f
        else -> 0.35f
    }

    SlooshFocusableCard(
        onClick = onClick,
        shape = shape,
        focusedScale = 1.05f
    ) { isFocused ->
        val bgColor = when {
            isFocused && isSelected -> Color.White
            isFocused -> Color.White.copy(alpha = 0.22f)
            isSelected -> Color.White
            else -> Color.White.copy(alpha = 0.08f)
        }
        val textColor = when {
            isSelected -> Color.Black
            isFocused -> Color.White
            else -> Color.White.copy(alpha = contentAlpha)
        }

        Box(
            modifier = Modifier
                .clip(shape)
                .background(bgColor)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp
                ),
                color = textColor
            )
        }
    }
}

// ─── Play button using SlooshFocusableCard ────────────────────────────────────

@Composable
private fun PlayButton(
    isReadyToPlay: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    val shape = ContinuousCapsule

    SlooshFocusableCard(
        onClick = onClick,
        shape = shape,
        focusedScale = 1.03f,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
    ) { isFocused ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(shape)
                .background(if (isReadyToPlay) Color.White else Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isReadyToPlay) Color.Black else Color.Black.copy(alpha = 0.40f),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Смотреть",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = if (isReadyToPlay) Color.Black else Color.Black.copy(alpha = 0.40f)
                )
            }
        }
    }
}
