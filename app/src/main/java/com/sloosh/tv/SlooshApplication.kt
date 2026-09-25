package com.sloosh.tv

import android.app.Activity
import android.app.Application
import android.os.Bundle
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.intercept.Interceptor
import coil.memory.MemoryCache
import com.sloosh.tv.data.api.MoviesApi
import com.sloosh.tv.data.db.AppDatabase
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class SlooshApplication : Application(), ImageLoaderFactory {

    companion object {
        var currentActivity: Activity? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        MoviesApi.init(this)
        AppDatabase.getDatabase(this)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                currentActivity = activity
            }
            override fun onActivityStarted(activity: Activity) {
                currentActivity = activity
            }
            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                if (currentActivity === activity) {
                    currentActivity = null
                }
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivity === activity) {
                    currentActivity = null
                }
            }
        })
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .connectionPool(okhttp3.ConnectionPool(8, 5, TimeUnit.MINUTES))
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .components {
                add(Interceptor { chain ->
                    val request = chain.request
                    val data = request.data
                    if (data is String) {
                        val resolved = MoviesApi.resolveEffectiveImageUrl(data)
                        if (resolved != null && resolved != data) {
                            return@Interceptor chain.proceed(request.newBuilder().data(resolved).build())
                        }
                    }
                    chain.proceed(request)
                })
            }
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
            .crossfade(false)
            .allowHardware(true)
            .allowRgb565(true)
            .respectCacheHeaders(false)
            .build()
    }
}
