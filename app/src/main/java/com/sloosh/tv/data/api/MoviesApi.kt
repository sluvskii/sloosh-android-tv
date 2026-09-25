package com.sloosh.tv.data.api

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface MoviesApiService {
    @GET("api/v1/movies/popular")
    suspend fun getPopularMovies(
        @Query("page") page: Int = 1
    ): ApiEnvelope<MediaResponse>

    @GET("api/v1/movies/top-rated")
    suspend fun getTopMovies(
        @Query("page") page: Int = 1
    ): ApiEnvelope<MediaResponse>

    @GET("api/v1/tv/top-rated")
    suspend fun getTopTv(
        @Query("page") page: Int = 1
    ): ApiEnvelope<MediaResponse>

    @GET("api/v1/cartoons")
    suspend fun getCartoons(
        @Query("page") page: Int = 1
    ): ApiEnvelope<MediaResponse>

    @GET("api/v1/anime")
    suspend fun getAnime(
        @Query("page") page: Int = 1,
        @Query("order") order: String? = null
    ): ApiEnvelope<MediaResponse>

    @GET("api/v1/trending")
    suspend fun getTrending(
        @Query("page") page: Int = 1,
        @Query("window") window: String = "week"
    ): ApiEnvelope<MediaResponse>

    @GET("api/v2/movie/{id}")
    suspend fun getMovieDetails(
        @Path("id") id: String,
        @Query("v") version: String = "6"
    ): ApiEnvelope<MediaDetailsDto>

    @GET("api/v2/tv/{id}")
    suspend fun getTvDetails(
        @Path("id") id: String,
        @Query("v") version: String = "6"
    ): ApiEnvelope<MediaDetailsDto>

    @GET("api/v1/config/streams")
    suspend fun getStreamTokens(): ApiEnvelope<StreamConfigDto>

    @GET("api/v2/person/{id}")
    suspend fun getPersonDetails(
        @Path("id") id: String
    ): ApiEnvelope<PersonDetailDto>

    @GET("api/v1/categories")
    suspend fun getCategories(): ApiEnvelope<List<CategorySectionDto>>

    @GET("api/v1/collection/{id}")
    suspend fun getCollection(
        @Path("id") id: String,
        @Query("page") page: Int = 1
    ): ApiEnvelope<MediaResponse>

    @GET("api/v1/media/movie/{id}/collection")
    suspend fun getMovieCollection(
        @Path("id") id: String
    ): ApiEnvelope<MovieCollectionDto>

    @GET("api/v1/media/{type}/{id}/related/studio")
    suspend fun getRelatedByStudio(
        @Path("type") type: String,
        @Path("id") id: String,
        @Query("page") page: Int = 1
    ): ApiEnvelope<RelatedStudioResponse>

    @GET("api/v1/tv/{id}/season/{season}")
    suspend fun getSeason(
        @Path("id") id: String,
        @Path("season") season: Int
    ): ApiEnvelope<TvSeasonDto>

    @GET("api/v1/tv/{id}/season/{season}/episode/{episode}")
    suspend fun getEpisodeDetails(
        @Path("id") id: String,
        @Path("season") season: Int,
        @Path("episode") episode: Int
    ): ApiEnvelope<TvEpisodeDetailsDto>

    @GET("api/v1/search")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): ApiEnvelope<MediaResponse>
}

object MoviesApi {
    const val DEFAULT_BASE_URL = "https://api.sloosh.workers.dev/"
    const val DEFAULT_IMAGES_BASE_URL = "https://api.sloosh.workers.dev"
    const val API_KEY = "sloosh_app_sec_v1_8f93e14b2d07"

    private const val PREFS_NAME = "sloosh_network_config"
    private const val KEY_CACHED_BASE_URL = "cached_api_base_url"
    private const val KEY_CACHED_IMAGES_BASE_URL = "cached_images_base_url"

    @Volatile
    var activeBaseUrl: String = DEFAULT_BASE_URL
        private set

    @Volatile
    var activeImagesBaseUrl: String = DEFAULT_IMAGES_BASE_URL
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedApi = prefs.getString(KEY_CACHED_BASE_URL, null)
        val cachedImages = prefs.getString(KEY_CACHED_IMAGES_BASE_URL, null)

        if (!cachedApi.isNullOrBlank() && !cachedApi.contains("vercel.app")) {
            activeBaseUrl = if (cachedApi.endsWith("/")) cachedApi else "$cachedApi/"
        } else {
            activeBaseUrl = DEFAULT_BASE_URL
            prefs.edit().remove(KEY_CACHED_BASE_URL).apply()
        }

        if (!cachedImages.isNullOrBlank() && !cachedImages.contains("vercel.app")) {
            activeImagesBaseUrl = cachedImages.trimEnd('/')
        } else {
            activeImagesBaseUrl = DEFAULT_IMAGES_BASE_URL
            prefs.edit().remove(KEY_CACHED_IMAGES_BASE_URL).apply()
        }

        CoroutineScope(Dispatchers.IO).launch {
            loadRemoteConfig(context)
        }
    }

    suspend fun loadRemoteConfig(context: Context? = null) {
        val configUrls = listOf(
            "https://raw.githubusercontent.com/sluvskii/iOS-sloosh/main/endpoint.json",
            "https://sluvskii.github.io/iOS-sloosh/endpoint.json"
        )
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        for (urlStr in configUrls) {
            try {
                val req = Request.Builder().url(urlStr).build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: return@use
                        val json = JSONObject(body)
                        val apiBase = json.optString("apiBaseUrl", "").trim()
                        val imagesBase = json.optString("imagesBaseUrl", "").trim()

                        var updated = false
                        if (apiBase.isNotBlank() && !apiBase.contains("vercel.app")) {
                            activeBaseUrl = if (apiBase.endsWith("/")) apiBase else "$apiBase/"
                            updated = true
                        }
                        if (imagesBase.isNotBlank() && !imagesBase.contains("vercel.app")) {
                            activeImagesBaseUrl = imagesBase.trimEnd('/')
                            updated = true
                        }

                        if (updated && context != null) {
                            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                .edit()
                                .putString(KEY_CACHED_BASE_URL, activeBaseUrl)
                                .putString(KEY_CACHED_IMAGES_BASE_URL, activeImagesBaseUrl)
                                .apply()
                        }
                        return
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun resolveEffectiveImageUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val base = activeImagesBaseUrl
        if (url.contains("api-sloosh.vercel.app")) {
            return url.replace("https://api-sloosh.vercel.app", base)
                .replace("http://api-sloosh.vercel.app", base)
        }
        if (url.contains("image.tmdb.org/t/p/")) {
            return url.replace("https://image.tmdb.org/t/p/", "$base/api/v1/images/tmdb/")
                .replace("http://image.tmdb.org/t/p/", "$base/api/v1/images/tmdb/")
        }
        return url
    }

    private class DynamicHostInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()
            val currentBase = activeBaseUrl
            val hostUrl = currentBase.toHttpUrlOrNull()
            if (hostUrl != null) {
                val newUrl = request.url.newBuilder()
                    .scheme(hostUrl.scheme)
                    .host(hostUrl.host)
                    .port(hostUrl.port)
                    .build()
                request = request.newBuilder().url(newUrl).build()
            }
            return chain.proceed(request)
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(DynamicHostInterceptor())
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("X-API-Key", API_KEY)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }

    val service: MoviesApiService by lazy {
        Retrofit.Builder()
            .baseUrl(DEFAULT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MoviesApiService::class.java)
    }
}
