package com.sloosh.tv.data.repository

import com.sloosh.tv.data.api.MediaDetailsDto
import com.sloosh.tv.data.api.MediaDto
import com.sloosh.tv.data.api.MediaResponse
import com.sloosh.tv.data.api.MoviesApi
import com.sloosh.tv.data.api.TvEpisodeDetailsDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MoviesRepository {

    private val api = MoviesApi.service

    suspend fun getPopularMovies(page: Int = 1): List<MediaDto> = withContext(Dispatchers.IO) {
        try {
            api.getPopularMovies(page).data?.results ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTopMovies(page: Int = 1): List<MediaDto> = withContext(Dispatchers.IO) {
        try {
            api.getTopMovies(page).data?.results ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTopTv(page: Int = 1): List<MediaDto> = withContext(Dispatchers.IO) {
        try {
            api.getTopTv(page).data?.results ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDetails(id: String): MediaDetailsDto? = withContext(Dispatchers.IO) {
        try {
            api.getDetails(id).data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getEpisodeDetails(id: String, season: Int, episode: Int): TvEpisodeDetailsDto? = withContext(Dispatchers.IO) {
        try {
            api.getEpisodeDetails(id, season, episode).data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchMovies(query: String, page: Int = 1): List<MediaDto> = withContext(Dispatchers.IO) {
        try {
            api.searchMovies(query, page).data?.results ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
