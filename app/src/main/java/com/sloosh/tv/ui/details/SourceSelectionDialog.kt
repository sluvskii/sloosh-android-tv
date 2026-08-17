package com.sloosh.tv.ui.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.sloosh.tv.data.api.AllohaApiResult
import com.sloosh.tv.data.api.AllohaTranslation
import com.sloosh.tv.data.repository.allohaTranslationNamesMatch
import com.sloosh.tv.ui.components.SlooshFocusableCard

// ─── Data class for dialog result ────────────────────────────────────────────

data class SourceSelectionResult(
    val translation: AllohaTranslation,
    val season: Int?,
    val episode: Int?
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

// ─── Main composable ─────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SourceSelectionDialog(
    allohaData: AllohaApiResult,
    kpId: Int?,
    savedVoiceover: String?,          // per-show last voiceover
    globalLastVoiceover: String?,     // global alloha_last_translation_name
    lastSeason: Int?,
    lastEpisode: Int?,
    onSelect: (SourceSelectionResult) -> Unit,
    onDismiss: () -> Unit
) {
    val isSerial = allohaData.isSerial || allohaData.seasons.isNotEmpty()

    val allTranslations = remember(allohaData) { allTranslationNames(allohaData) }
    val allSeasons = remember(allohaData) { allohaData.seasons.map { it.season } }

    // ─── Initial selection setup (mirrors setupInitialSelection() in iOS) ────
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

    // ─── Selection actions (mirrors iOS selectTranslation/Season/Episode) ────

    fun selectTranslation(name: String) {
        selectedTranslationName = name
        if (isSerial) {
            // If current season doesn't have this translation, switch to first season that does
            val curSeason = selectedSeason
            if (curSeason != null && !seasonHasTranslation(allohaData, curSeason, name)) {
                val newSeason = allohaData.seasons.firstOrNull { seasonHasTranslation(allohaData, it.season, name) }
                if (newSeason != null) selectedSeason = newSeason.season
            }
            // If current episode doesn't have this translation, switch to first that does
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
            onSelect(SourceSelectionResult(translation, s, e))
        } else {
            val tName = selectedTranslationName ?: return
            val translation = allohaData.movie?.translations?.firstOrNull {
                it.name == tName
            } ?: allohaData.movie?.translations?.firstOrNull() ?: return
            onSelect(SourceSelectionResult(translation, null, null))
        }
    }

    // ─── Focus management ────────────────────────────────────────────────────
    val playButtonFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try { playButtonFocusRequester.requestFocus() } catch (e: Exception) {}
    }

    // ─── UI ─────────────────────────────────────────────────────────────────
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 48.dp, vertical = 36.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 940.dp)
                    .fillMaxHeight()
                    .clip(ContinuousRoundedRectangle(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1C1C1E), Color(0xFF141416))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.12f), ContinuousRoundedRectangle(28.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* absorb clicks */ }
            ) {
                // ─── Header ─────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp, end = 36.dp, top = 28.dp, bottom = 0.dp)
                ) {
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
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Color.White.copy(alpha = 0.50f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )
                }

                // ─── Scrollable sections with multi-line FlowRow ──────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 36.dp)
                ) {
                    // ── Translations ─────────────────────────────────
                    if (allTranslations.isNotEmpty()) {
                        SectionLabel("Озвучка")
                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                        ) {
                            allTranslations.forEach { tName ->
                                val isSelected = selectedTranslationName == tName
                                val isAvailable = isTranslationAvailable(allohaData, tName, selectedSeason, selectedEpisode)
                                SelectorChip(
                                    label = com.sloosh.tv.ui.util.cleanTranslationName(tName),
                                    isSelected = isSelected,
                                    isAvailable = isAvailable,
                                    onClick = { selectTranslation(tName) }
                                )
                            }
                        }
                    }

                    // ── Seasons ──────────────────────────────────────
                    if (isSerial && allSeasons.isNotEmpty()) {
                        SectionLabel("Сезон")
                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
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

                    // ── Episodes ─────────────────────────────────────
                    if (isSerial && currentEpisodes.isNotEmpty()) {
                        SectionLabel("Серия")
                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
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
}

// ─── Loading state dialog ─────────────────────────────────────────────────────

@Composable
fun SourceSelectionLoadingDialog(title: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 48.dp, vertical = 36.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(480.dp)
                    .clip(ContinuousRoundedRectangle(28.dp))
                    .background(Color(0xFF1C1C1E))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), ContinuousRoundedRectangle(28.dp))
                    .padding(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Загрузка источников…",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = Color.White.copy(alpha = 0.7f)
                )
                if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
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
