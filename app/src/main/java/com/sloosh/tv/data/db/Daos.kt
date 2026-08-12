package com.sloosh.tv.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM playback_progress ORDER BY updatedAtMs DESC")
    fun getAllProgress(): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM playback_progress WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getProgress(mediaId: String): ProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ProgressEntity)

    @Query("DELETE FROM playback_progress WHERE mediaId = :mediaId")
    suspend fun deleteProgress(mediaId: String)
}

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites ORDER BY addedAtMs DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getFavorite(mediaId: String): FavoriteEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId)")
    fun isFavoriteFlow(mediaId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaId = :mediaId")
    suspend fun deleteFavorite(mediaId: String)
}
