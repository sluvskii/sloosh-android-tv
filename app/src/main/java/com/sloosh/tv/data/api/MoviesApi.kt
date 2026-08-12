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

    @GET("api/v2/movie/{id}")
    suspend fun getDetails(
        @Path("id") id: String
    ): ApiEnvelope<MediaDetailsDto>

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
    private const val BASE_URL = "https://api.neome.uk/"

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
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
