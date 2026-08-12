package com.sloosh.tv.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.sloosh.tv.data.api.MediaDto
import com.sloosh.tv.ui.home.MediaCard
import com.sloosh.tv.ui.theme.BackgroundDark

@Composable
fun ProfileScreen(
    onMediaSelected: (String) -> Unit,
    viewModel: ProfileViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(start = 36.dp, top = 28.dp, end = 36.dp, bottom = 28.dp)
    ) {
        Text(
            text = "Избранное",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (state.favorites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "У вас пока нет сохраненных фильмов или сериалов",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.favorites, key = { it.mediaId }) { fav ->
                    val dto = MediaDto(
                        originalId = fav.mediaId,
                        title = fav.title,
                        originalTitle = null,
                        year = fav.year,
                        rating = fav.rating,
                        posterUrl = fav.posterUrl,
                        description = null,
                        type = fav.type,
                        genres = null,
                        externalIds = null,
                        name = fav.title,
                        posterPath = fav.posterUrl
                    )
                    MediaCard(
                        item = dto,
                        onClick = { onMediaSelected(fav.mediaId) },
                        onFocus = {}
                    )
                }
            }
        }
    }
}
