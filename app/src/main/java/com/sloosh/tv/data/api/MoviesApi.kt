package com.sloosh.tv.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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
    const val BASE_URL = "https://api-sloosh.vercel.app/"
    const val API_KEY = "sloosh_app_sec_v1_8f93e14b2d07"

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
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
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MoviesApiService::class.java)
    }
}
