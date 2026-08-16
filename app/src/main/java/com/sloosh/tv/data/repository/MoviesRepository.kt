package com.sloosh.tv.data.repository

import com.sloosh.tv.data.api.MediaDetailsDto
import com.sloosh.tv.data.api.MediaDto
import com.sloosh.tv.data.api.MoviesApi
import com.sloosh.tv.data.api.TvEpisodeDetailsDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class MoviesRepository {

    private val api = MoviesApi.service

    private val detailsCache = ConcurrentHashMap<String, MediaDetailsDto>()
    private val popularMoviesCache = ConcurrentHashMap<Int, List<MediaDto>>()
    private val topMoviesCache = ConcurrentHashMap<Int, List<MediaDto>>()
    private val topTvCache = ConcurrentHashMap<Int, List<MediaDto>>()

    suspend fun getPopularMovies(page: Int = 1): List<MediaDto> = withContext(Dispatchers.IO) {
        popularMoviesCache[page]?.let { return@withContext it }
        try {
            val results = api.getPopularMovies(page).data?.results ?: emptyList()
            if (results.isNotEmpty()) {
                popularMoviesCache[page] = results
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTopMovies(page: Int = 1): List<MediaDto> = withContext(Dispatchers.IO) {
        topMoviesCache[page]?.let { return@withContext it }
        try {
            val results = api.getTopMovies(page).data?.results ?: emptyList()
            if (results.isNotEmpty()) {
                topMoviesCache[page] = results
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTopTv(page: Int = 1): List<MediaDto> = withContext(Dispatchers.IO) {
        topTvCache[page]?.let { return@withContext it }
        try {
            val results = api.getTopTv(page).data?.results ?: emptyList()
            if (results.isNotEmpty()) {
                topTvCache[page] = results
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDetails(id: String): MediaDetailsDto? = withContext(Dispatchers.IO) {
        detailsCache[id]?.let { return@withContext it }
        try {
            val details = api.getDetails(id).data
            if (details != null) {
                detailsCache[id] = details
            }
            details
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

    fun clearCache() {
        detailsCache.clear()
        popularMoviesCache.clear()
        topMoviesCache.clear()
        topTvCache.clear()
    }
}
