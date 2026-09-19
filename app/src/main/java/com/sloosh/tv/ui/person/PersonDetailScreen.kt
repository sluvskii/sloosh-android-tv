package com.sloosh.tv.ui.person

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.sloosh.tv.data.api.PersonDetailDto
import com.sloosh.tv.data.repository.MoviesRepository
import com.sloosh.tv.ui.components.SlooshFocusableCard
import com.sloosh.tv.ui.theme.*
import com.sloosh.tv.ui.util.rememberAdaptiveAmbientColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class PersonUiState(
    val isLoading: Boolean = true,
    val details: PersonDetailDto? = null,
    val errorMessage: String? = null
)

class PersonDetailViewModel : ViewModel() {
    private val repository = MoviesRepository.instance
    private val _uiState = MutableStateFlow(PersonUiState())
    val uiState: StateFlow<PersonUiState> = _uiState.asStateFlow()

    fun loadPerson(personId: String) {
        viewModelScope.launch {
            _uiState.value = PersonUiState(isLoading = true)
            val details = repository.getPersonDetails(personId)
            if (details != null) {
                _uiState.value = PersonUiState(isLoading = false, details = details)
            } else {
                _uiState.value = PersonUiState(isLoading = false, errorMessage = "Не удалось загрузить данные")
            }
        }
    }
}

data class ParsedBiography(
    val bio: String,
    val awards: String?,
    val keyProjects: String?,
    val interestingFact: String?
)

fun parseBiography(raw: String?, details: PersonDetailDto): ParsedBiography {
    var awards = details.awards?.takeIf { it.isNotBlank() }
    var keyProjects = details.keyProjects?.takeIf { it.isNotBlank() }
    var interestingFact = details.interestingFact?.takeIf { it.isNotBlank() }

    if (raw.isNullOrBlank()) {
        return ParsedBiography(bio = "", awards = awards, keyProjects = keyProjects, interestingFact = interestingFact)
    }

    var cleaned = raw.filter { char ->
        val codePoint = char.code
        !(codePoint in 0x1F300..0x1FAFF || codePoint in 0x2600..0x27BF || codePoint == 0x2B50 || codePoint == 0xFE0F)
    }.trim()

    if (awards == null) {
        val regex = Regex("""(?:\r?\n|^)\s*Главные награды\s*:\s*([\s\S]*?)(?=(?:\r?\n\s*(?:Главные проекты|Интересн))|$)""")
        val match = regex.find(cleaned)
        if (match != null) {
            val parts = match.groupValues[1].split(":")
            awards = (if (parts.size > 1) parts.drop(1).joinToString(":") else parts[0]).trim()
            cleaned = cleaned.removeRange(match.range).trim()
        }
    }

    if (keyProjects == null) {
        val regex = Regex("""(?:\r?\n|^)\s*Главные проекты\s*:\s*([\s\S]*?)(?=(?:\r?\n\s*(?:Главные награды|Интересн))|$)""")
        val match = regex.find(cleaned)
        if (match != null) {
            val parts = match.groupValues[1].split(":")
            keyProjects = (if (parts.size > 1) parts.drop(1).joinToString(":") else parts[0]).trim()
            cleaned = cleaned.removeRange(match.range).trim()
        }
    }

    if (interestingFact == null) {
        val regex = Regex("""(?:\r?\n|^)\s*Интересны[ей]\s+факты?\s*:\s*([\s\S]*?)(?=(?:\r?\n\s*(?:Главные награды|Главные проекты))|$)""")
        val match = regex.find(cleaned)
        if (match != null) {
            val parts = match.groupValues[1].split(":")
            interestingFact = (if (parts.size > 1) parts.drop(1).joinToString(":") else parts[0]).trim()
            cleaned = cleaned.removeRange(match.range).trim()
        }
    }

    val finalBio = cleaned.replace(Regex("""\r?\n\s*\r?\n+"""), "\n\n").trim()
    return ParsedBiography(
        bio = finalBio,
        awards = awards,
        keyProjects = keyProjects,
        interestingFact = interestingFact
    )
}

private fun declinedProjects(count: Int): String {
    val lastTwo = count % 100
    val last = count % 10
    if (lastTwo in 11..19) return "работ"
    if (last == 1) return "работа"
    if (last in 2..4) return "работы"
    return "работ"
}

@Composable
fun PersonDetailScreen(
    personId: String,
    onBackClick: () -> Unit,
    onNavigateToMedia: (String) -> Unit,
    viewModel: PersonDetailViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val backFocusRequester = remember { FocusRequester() }

    BackHandler {
        onBackClick()
    }

    LaunchedEffect(personId) {
        viewModel.loadPerson(personId)
    }

    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            try {
                backFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.errorMessage ?: "Не удалось загрузить данные",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                SlooshFocusableCard(
                    onClick = { viewModel.loadPerson(personId) },
                    shape = ContinuousCapsule,
                    modifier = Modifier.focusRequester(backFocusRequester)
                ) { isFocused ->
                    Box(
                        modifier = Modifier
                            .clip(ContinuousCapsule)
                            .background(if (isFocused) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Повторить",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
        return
    }

    val photoUrl = details.getDisplayPhotoUrl()
    val ambientColor by rememberAdaptiveAmbientColor(
        primaryUrl = photoUrl,
        fallbackUrl = null,
        defaultColor = BackgroundDark
    )

    val depthGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.45f),
                Color.Black.copy(alpha = 0.80f),
                Color.Black.copy(alpha = 0.95f),
                BackgroundDark
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

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ambientColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(depthGradient)
        )

        // Native aspect-ratio portrait shifted to the right with horizontal fade
        if (!photoUrl.isNullOrEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.55f)
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
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.58f)
                .verticalScroll(scrollState)
                .padding(start = 56.dp, top = 36.dp, end = 24.dp, bottom = 48.dp)
        ) {
            // ─── Back Button ─────────────────────────────────────────
            SlooshFocusableCard(
                onClick = onBackClick,
                shape = CircleShape,
                modifier = Modifier
                    .size(44.dp)
                    .focusRequester(backFocusRequester)
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

            // ─── Hero Header ─────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!photoUrl.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .width(96.dp)
                            .height(136.dp)
                            .clip(ContinuousRoundedRectangle(16.dp))
                            .background(SurfaceDark)
                    ) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = details.displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                }

                Column {
                    Text(
                        text = details.displayName,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color.White
                    )

                    if (!details.originalName.isNullOrBlank() && !details.originalName.equals(details.displayName, ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = details.originalName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                letterSpacing = (-0.2).sp
                            ),
                            color = TextSecondaryDark
                        )
                    }

                    val department = details.knownForDepartment ?: details.department
                    if (!department.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(ContinuousCapsule)
                                .background(Color.White.copy(alpha = 0.1f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = department,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ─── Metadata Section ────────────────────────────────────
            val birth = details.formattedBirthdayWithAge
            val place = details.placeOfBirth?.trim()
            val death = details.deathday?.trim()
            val filmographyCount = details.filmography?.size ?: 0

            if (birth != null || !place.isNullOrEmpty() || !death.isNullOrEmpty() || filmographyCount > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ContinuousRoundedRectangle(18.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (birth != null) {
                        PersonMetaRow(label = "Дата рождения", value = birth)
                    }
                    if (!place.isNullOrEmpty()) {
                        PersonMetaRow(label = "Место рождения", value = place)
                    }
                    if (!death.isNullOrEmpty()) {
                        PersonMetaRow(label = "Дата смерти", value = death)
                    }
                    if (filmographyCount > 0) {
                        PersonMetaRow(label = "Карьера", value = "$filmographyCount ${declinedProjects(filmographyCount)}")
                    }
                }
            }

            // ─── Biography ───────────────────────────────────────────
            val parsedBio = remember(details) { parseBiography(details.biography, details) }

            if (parsedBio.bio.isNotBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                var isBioExpanded by remember { mutableStateOf(false) }
                val isLongBio = parsedBio.bio.length > 280

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ContinuousRoundedRectangle(18.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Биография",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            letterSpacing = (-0.2).sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = parsedBio.bio,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        color = TextSecondaryDark,
                        maxLines = if (isBioExpanded) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isLongBio) {
                        Spacer(modifier = Modifier.height(10.dp))
                        SlooshFocusableCard(
                            onClick = { isBioExpanded = !isBioExpanded },
                            shape = ContinuousCapsule,
                            modifier = Modifier.wrapContentSize()
                        ) { isFocused ->
                            Box(
                                modifier = Modifier
                                    .clip(ContinuousCapsule)
                                    .background(if (isFocused) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.12f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isBioExpanded) "Свернуть" else "Читать далее",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // ─── Extra info cards (Awards, Key Projects, Facts) ─────
            parsedBio.awards?.takeIf { it.isNotBlank() }?.let { awards ->
                Spacer(modifier = Modifier.height(16.dp))
                PersonExtraInfoCard(title = "Главные награды", content = awards)
            }

            parsedBio.keyProjects?.takeIf { it.isNotBlank() }?.let { projects ->
                Spacer(modifier = Modifier.height(16.dp))
                PersonExtraInfoCard(title = "Главные проекты", content = projects)
            }

            parsedBio.interestingFact?.takeIf { it.isNotBlank() }?.let { fact ->
                Spacer(modifier = Modifier.height(16.dp))
                PersonExtraInfoCard(title = "Интересный факт", content = fact)
            }

            // ─── Filmography ─────────────────────────────────────────
            val filmography = details.filmography
            if (!filmography.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Фильмография",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(end = 24.dp)
                ) {
                    items(filmography) { item ->
                        val targetId = item.originalId ?: item.identifier
                        SlooshFocusableCard(
                            onClick = {
                                if (targetId.isNotBlank()) {
                                    onNavigateToMedia(targetId)
                                }
                            },
                            shape = ContinuousRoundedRectangle(14.dp),
                            modifier = Modifier
                                .width(120.dp)
                                .height(180.dp)
                        ) { _ ->
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
                                        .height(60.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
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
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            color = ratingColor
                                        )
                                    }
                                }

                                Text(
                                    text = item.displayTitle,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp
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
                }
            }

            // ─── Photos Section ──────────────────────────────────────
            val photos = details.photos
            if (!photos.isNullOrEmpty() && photos.size > 1) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Фотографии",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 24.dp)
                ) {
                    items(photos) { photoItem ->
                        SlooshFocusableCard(
                            onClick = {},
                            shape = ContinuousRoundedRectangle(14.dp),
                            modifier = Modifier
                                .width(120.dp)
                                .height(160.dp)
                        ) { _ ->
                            AsyncImage(
                                model = photoItem,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = TextMutedDark
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            ),
            color = Color.White,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun PersonExtraInfoCard(title: String, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                letterSpacing = (-0.2).sp
            ),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            color = TextSecondaryDark
        )
    }
}
