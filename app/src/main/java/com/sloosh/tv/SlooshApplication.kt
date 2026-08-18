package com.sloosh.tv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.sloosh.tv.data.db.AppDatabase
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class SlooshApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        AppDatabase.getDatabase(this)
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .connectionPool(okhttp3.ConnectionPool(8, 5, TimeUnit.MINUTES))
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(200L * 1024 * 1024)
                    .build()
            }
            .crossfade(140)
            .allowHardware(true)
            .allowRgb565(true)
            .respectCacheHeaders(false)
            .build()
    }
}
