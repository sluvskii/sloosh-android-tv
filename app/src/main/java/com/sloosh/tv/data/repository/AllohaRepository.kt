package com.sloosh.tv.data.repository

import android.content.Context
import com.sloosh.tv.data.api.AllohaApiResult
import com.sloosh.tv.data.api.AllohaMovie
import com.sloosh.tv.data.api.AllohaTranslation
import com.sloosh.tv.data.api.AllohaResolvedStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class AllohaRepository(private val context: Context) {

    private val client = OkHttpClient()
    private val resolver = AllohaRuntimeResolver(context)

    suspend fun fetchAllohaData(kpId: String): AllohaApiResult? = withContext(Dispatchers.IO) {
        val sanitizedId = kpId.replace("kp_", "").trim()
        val url = "https://alloha.tv/api/v1/movie/$sanitizedId?token=3665a396263599a25039f37d377b09"
        
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext fallbackIframe(sanitizedId)
            
            val json = JSONObject(body)
            if (!json.optBoolean("success", true) && !json.has("data")) {
                return@withContext fallbackIframe(sanitizedId)
            }

            val data = json.optJSONObject("data") ?: json
            val title = data.optString("name", "Фильм")
            val iframeUrl = data.optString("iframe_url", "")
            val effectiveIframe = if (iframeUrl.isNotEmpty()) iframeUrl else "https://alloha.tv/movie/$sanitizedId"

            val movie = AllohaMovie(
                title = title,
                iframeUrl = effectiveIframe,
                translations = listOf(
                    AllohaTranslation(
                        id = "default",
                        name = "Основной дубляж",
                        iframeUrl = effectiveIframe
                    )
                )
            )

            AllohaApiResult(
                title = title,
                isSerial = false,
                movie = movie,
                seasons = emptyList()
            )
        } catch (e: Exception) {
            fallbackIframe(sanitizedId)
        }
    }

    private fun fallbackIframe(kpId: String): AllohaApiResult {
        val iframeUrl = "https://alloha.tv/movie/$kpId"
        return AllohaApiResult(
            title = "Фильм",
            isSerial = false,
            movie = AllohaMovie(
                title = "Фильм",
                iframeUrl = iframeUrl,
                translations = listOf(AllohaTranslation("default", "Основной дубляж", iframeUrl))
            ),
            seasons = emptyList()
        )
    }

    suspend fun resolveStream(iframeUrl: String): AllohaResolvedStream {
        return resolver.resolve(iframeUrl)
    }
}
