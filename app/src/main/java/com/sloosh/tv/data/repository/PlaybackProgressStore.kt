package com.sloosh.tv.data.repository

// PlaybackProgressStore wrapper for DB access
import android.content.Context
import com.sloosh.tv.data.db.AppDatabase
import com.sloosh.tv.data.db.ProgressEntity
import com.sloosh.tv.data.db.FavoriteEntity
import kotlinx.coroutines.flow.Flow

class PlaybackProgressStore(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val progressDao = db.progressDao()
    private val favoritesDao = db.favoritesDao()

    val allProgress: Flow<List<ProgressEntity>> = progressDao.getAllProgress()
    val allFavorites: Flow<List<FavoriteEntity>> = favoritesDao.getAllFavorites()

    suspend fun getProgress(mediaId: String): ProgressEntity? = progressDao.getProgress(mediaId)

    suspend fun saveProgress(progress: ProgressEntity) = progressDao.saveProgress(progress)

    suspend fun deleteProgress(mediaId: String) = progressDao.deleteProgress(mediaId)

    suspend fun isFavorite(mediaId: String): Boolean = favoritesDao.getFavorite(mediaId) != null

    suspend fun toggleFavorite(favorite: FavoriteEntity) {
        val existing = favoritesDao.getFavorite(favorite.mediaId)
        if (existing != null) {
            favoritesDao.deleteFavorite(favorite.mediaId)
        } else {
            favoritesDao.insertFavorite(favorite)
        }
    }
}
