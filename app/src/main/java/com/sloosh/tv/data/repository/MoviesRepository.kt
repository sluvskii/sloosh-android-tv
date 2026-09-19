package com.sloosh.tv.data.repository

import com.sloosh.tv.data.api.IdsDto
import com.sloosh.tv.data.api.MediaDetailsDto
import com.sloosh.tv.data.api.MediaDto
import com.sloosh.tv.data.api.MovieCollectionDto
import com.sloosh.tv.data.api.MoviesApi
import com.sloosh.tv.data.api.PersonDetailDto
import com.sloosh.tv.data.api.RelatedStudioResponse
import com.sloosh.tv.data.api.TvEpisodeDetailsDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class MoviesRepository {

    private val api = MoviesApi.service

    companion object {
        val instance by lazy { MoviesRepository() }

        private val detailsCache = ConcurrentHashMap<String, MediaDetailsDto>()
        private val previewDetailsCache = ConcurrentHashMap<String, MediaDetailsDto>()
        private val personCache = ConcurrentHashMap<String, PersonDetailDto>()
        private val popularMoviesCache = ConcurrentHashMap<Int, List<MediaDto>>()
        private val topMoviesCache = ConcurrentHashMap<Int, List<MediaDto>>()
        private val topTvCache = ConcurrentHashMap<Int, List<MediaDto>>()
        private val cartoonsCache = ConcurrentHashMap<Int, List<MediaDto>>()
        private val animeCache = ConcurrentHashMap<String, List<MediaDto>>()
        private val trendingCache = ConcurrentHashMap<Int, List<MediaDto>>()
    }

    suspend fun getPopularMovies(page: Int = 1): List<MediaDto> = withContext(Dispatchers.IO) {
        popularMoviesCache[page]?.let { return@withContext it }
        try {
            val results = api.getPopularMovies(page).data?.allItems ?: emptyList()
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
            val results = api.getTopMovies(page).data?.allItems ?: emptyList()
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
            val results = api.getTopTv(page).data?.allItems ?: emptyList()
            if (results.isNotEmpty()) {
                topTvCache[page] = results
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCartoons(page: Int = 1): List<MediaDto> = withContext(Dispatchers.IO) {
        cartoonsCache[page]?.let { return@withContext it }
        try {
            val results = api.getCartoons(page).data?.allItems ?: emptyList()
            if (results.isNotEmpty()) {
                cartoonsCache[page] = results
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAnime(page: Int = 1, order: String? = null): List<MediaDto> = withContext(Dispatchers.IO) {
        val cacheKey = if (order != null) "${page}_$order" else "$page"
        animeCache[cacheKey]?.let { return@withContext it }
        try {
            val results = api.getAnime(page, order).data?.allItems ?: emptyList()
            if (results.isNotEmpty()) {
                animeCache[cacheKey] = results
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTrending(page: Int = 1, window: String = "week"): List<MediaDto> = withContext(Dispatchers.IO) {
        trendingCache[page]?.let { return@withContext it }
        try {
            val results = api.getTrending(page, window).data?.allItems ?: emptyList()
            if (results.isNotEmpty()) {
                trendingCache[page] = results
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDetails(id: String, type: String? = null): MediaDetailsDto? = withContext(Dispatchers.IO) {
        detailsCache[id]?.let { return@withContext it }
        val cleanId = id.replace("tv_", "").replace("movie_", "")
        val isTv = type?.lowercase() in listOf("tv", "series", "serial", "show") || id.startsWith("tv_")

        try {
            val details = if (isTv) {
                try {
                    api.getTvDetails(cleanId).data
                } catch (e: Exception) {
                    api.getMovieDetails(cleanId).data
                }
            } else {
                try {
                    api.getMovieDetails(cleanId).data
                } catch (e: Exception) {
                    api.getTvDetails(cleanId).data
                }
            }

            if (details != null) {
                detailsCache[id] = details
                detailsCache[cleanId] = details
            }
            details
        } catch (e: Exception) {
            null
        }
    }

    fun getPreviewDetails(id: String): MediaDetailsDto? {
        val cleanId = id.replace("tv_", "").replace("movie_", "")
        return detailsCache[id] ?: detailsCache[cleanId] ?: previewDetailsCache[id] ?: previewDetailsCache[cleanId]
    }

    fun setPreviewDetails(item: MediaDto) {
        val cleanId = item.identifier.replace("tv_", "").replace("movie_", "")
        val parsedYear = when (val y = item.year) {
            is Number -> y.toInt()
            is String -> y.toIntOrNull()
            else -> null
        }
        val preview = MediaDetailsDto(
            id = item.id,
            title = item.displayTitle,
            originalTitle = item.originalTitle,
            description = item.description,
            type = item.type,
            year = parsedYear,
            releaseDate = null,
            genres = item.genres?.mapNotNull { it.name ?: it.id },
            countries = null,
            duration = null,
            poster = item.poster ?: item.posterUrl ?: item.posterPath,
            backdrop = item.backdrop ?: item.backdropPath,
            rawRating = item.rating,
            ratings = item.ratings,
            externalIds = item.externalIds,
            ids = IdsDto(
                kp = item.id.replace("kp_", "").replace("tv_", "").replace("movie_", "").toIntOrNull() ?: item.externalIds?.kp,
                imdb = item.externalIds?.imdb,
                tmdb = item.externalIds?.tmdb
            )
        )
        previewDetailsCache[item.identifier] = preview
        previewDetailsCache[cleanId] = preview
    }

    suspend fun getMovieCollection(id: String): MovieCollectionDto? = withContext(Dispatchers.IO) {
        val cleanId = id.replace("kp_", "").replace("tv_", "").replace("movie_", "")
        try {
            api.getMovieCollection(cleanId).data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRelatedByStudio(type: String, id: String, page: Int = 1): RelatedStudioResponse? = withContext(Dispatchers.IO) {
        val cleanType = if (type.lowercase().contains("tv") || type.lowercase().contains("serial")) "tv" else "movie"
        val cleanId = id.replace("kp_", "").replace("tv_", "").replace("movie_", "")
        try {
            api.getRelatedByStudio(cleanType, cleanId, page).data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSeason(id: String, season: Int) = withContext(Dispatchers.IO) {
        val cleanId = id.replace("tv_", "").replace("movie_", "")
        try {
            api.getSeason(cleanId, season).data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getEpisodeDetails(id: String, season: Int, episode: Int): TvEpisodeDetailsDto? = withContext(Dispatchers.IO) {
        val cleanId = id.replace("tv_", "").replace("movie_", "")
        try {
            api.getEpisodeDetails(cleanId, season, episode).data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchMovies(query: String, page: Int = 1): List<MediaDto> = withContext(Dispatchers.IO) {
        try {
            api.searchMovies(query, page).data?.allItems ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPersonDetails(id: String): PersonDetailDto? = withContext(Dispatchers.IO) {
        val cleanId = id.replace("person_", "").trim()
        personCache[cleanId]?.let { return@withContext it }
        try {
            val details = api.getPersonDetails(cleanId).data
            if (details != null) {
                personCache[cleanId] = details
            }
            details
        } catch (e: Exception) {
            null
        }
    }

    fun clearCache() {
        detailsCache.clear()
        personCache.clear()
        popularMoviesCache.clear()
        topMoviesCache.clear()
        topTvCache.clear()
        cartoonsCache.clear()
        animeCache.clear()
    }
}
