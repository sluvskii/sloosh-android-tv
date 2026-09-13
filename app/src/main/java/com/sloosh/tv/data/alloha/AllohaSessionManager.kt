package com.sloosh.tv.data.alloha

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "AllohaSession"

class AllohaSessionManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var parser: AllohaParser? = null
        private set

    var hlsProxy: HlsProxyServer? = null
        private set

    val proxyMasterUrl: String
        get() = hlsProxy?.fixedMasterUrl ?: HlsProxyServer.shared.fixedMasterUrl

    val activeHeaders: Map<String, String> get() = _activeHeaders
    private val _activeHeaders = ConcurrentHashMap<String, String>()

    var lastSelectedQuality: String = ""
        private set
    var lastQualityMap: Map<String, String> = emptyMap()
        private set

    var lastSkipRanges: List<LongRange> = emptyList()
        private set

    var onStreamReady: ((qualityMap: Map<String, String>, defaultUrl: String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onM3u8Updated: ((newUrl: String) -> Unit)? = null

    private var isRestarting = false
    private var lastRestartUrl: String? = null
    private var failureCount = 0

    fun ensureInitialized() {
        if (parser == null) parser = AllohaParser(context)
        if (hlsProxy == null) {
            hlsProxy = HlsProxyServer.shared.also {
                it.start(_activeHeaders)
            }
        }
    }

    fun startSession(iframeUrl: String, isRestart: Boolean = false, attempt: Int = 1) {
        ensureInitialized()
        lastRestartUrl = iframeUrl
        isRestarting = isRestart

        Log.d(TAG, "Starting Alloha session (attempt=$attempt, restart=$isRestart): $iframeUrl")

        val timeoutJob = scope.launch {
            delay(18_000)
            if (isActive) {
                Log.w(TAG, "Session init timed out after 18s (attempt $attempt)")
                failureCount++
                parser?.rotateUserAgent()
                if (attempt < 3) {
                    Log.d(TAG, "Retrying session init (attempt ${attempt + 1})...")
                    startSession(iframeUrl, isRestart, attempt = attempt + 1)
                } else {
                    onError?.invoke("Превышено время ожидания видеопотока")
                }
            }
        }

        parser?.parse(iframeUrl, object : AllohaParser.Callback {
            override fun onHlsLinksReceived(json: String, extraHeaders: Map<String, String>) {
                timeoutJob.cancel()
                failureCount = 0
                _activeHeaders.putAll(extraHeaders)
                hlsProxy?.updateHeaders(_activeHeaders)

                val result = parseBnsiJson(json)
                if (result == null) {
                    if (attempt < 3) {
                        parser?.rotateUserAgent()
                        startSession(iframeUrl, isRestart, attempt = attempt + 1)
                    } else {
                        onError?.invoke("Ошибка декодирования потока")
                    }
                    return
                }

                lastQualityMap = result.qualityMap
                lastSkipRanges = result.skipRanges
                AllohaSessionHolder.skipRanges = result.skipRanges

                val initialQuality = pickBestQuality(result.qualityMap)
                val initialUrl = result.qualityMap[initialQuality] ?: result.defaultUrl
                lastSelectedQuality = initialQuality

                hlsProxy?.subtitleTracks = result.subtitles
                hlsProxy?.updateMasterUrl(initialUrl)

                if (isRestart) {
                    onM3u8Updated?.invoke(initialUrl)
                } else {
                    onStreamReady?.invoke(result.qualityMap, initialUrl)
                }
            }

            override fun onConfigUpdate(edgeHash: String, ttlSeconds: Int, extraHeaders: Map<String, String>) {
                _activeHeaders.putAll(extraHeaders)
                _activeHeaders["accepts-controls"] = edgeHash
                hlsProxy?.updateHeaders(_activeHeaders)
                // Note: Proactive TTL restarts removed. Video stream tokens on Alloha CDN
                // remain valid for the movie duration. Scheduled restarts were destroying
                // active ExoPlayer TCP connections and causing playback to freeze at ~7-8 minutes.
            }

            override fun onM3u8Refreshed(url: String, extraHeaders: Map<String, String>) {
                _activeHeaders.putAll(extraHeaders)
                hlsProxy?.updateHeaders(_activeHeaders)
                Log.d(TAG, "CDN master URL refreshed: $url")
                hlsProxy?.updateMasterUrl(url)
                onM3u8Updated?.invoke(url)
            }

            override fun onStreamHeadersUpdated(extraHeaders: Map<String, String>) {
                _activeHeaders.putAll(extraHeaders)
                hlsProxy?.updateHeaders(_activeHeaders)
            }

            override fun onError(error: String) {
                timeoutJob.cancel()
                if (attempt < 3) {
                    parser?.rotateUserAgent()
                    startSession(iframeUrl, isRestart, attempt = attempt + 1)
                } else {
                    this@AllohaSessionManager.onError?.invoke(error)
                }
            }
        })
    }

    fun switchQuality(qualityKey: String): Boolean {
        val url = lastQualityMap[qualityKey] ?: return false
        lastSelectedQuality = qualityKey
        hlsProxy?.updateMasterUrl(url)
        return true
    }

    fun release() {
        scope.cancel()
        parser?.release()
        parser = null
        hlsProxy = null
        _activeHeaders.clear()
        Log.d(TAG, "AllohaSessionManager released")
    }

    private data class BnsiResult(
        val qualityMap: Map<String, String>,
        val defaultUrl: String,
        val subtitles: List<Triple<String, String, String>> = emptyList(),
        val skipRanges: List<LongRange> = emptyList()
    )

    private fun parseBnsiJson(json: String): BnsiResult? = try {
        val root = JSONObject(json)
        val hlsSource = root.getJSONArray("hlsSource").getJSONObject(0)
        val qualityObj = hlsSource.getJSONObject("quality")
        val qualityMap = mutableMapOf<String, String>()
        val keys = qualityObj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val raw = qualityObj.getString(k)
            val url = if (raw.contains(" or ")) raw.split(" or ")[0].trim() else raw.trim()
            qualityMap[k] = url
        }
        val defaultUrl = qualityMap["1080"] ?: qualityMap["720"] ?: qualityMap.values.firstOrNull() ?: ""

        val subtitles = mutableListOf<Triple<String, String, String>>()
        if (hlsSource.has("tracks")) {
            val tracks = hlsSource.getJSONObject("tracks")
            val tKeys = tracks.keys()
            while (tKeys.hasNext()) {
                val lang = tKeys.next()
                val trackUrl = tracks.getString(lang)
                val label = when (lang.lowercase()) {
                    "rus", "ru" -> "Русские"
                    "eng", "en" -> "English"
                    "ukr", "uk" -> "Українська"
                    else -> lang.replaceFirstChar { it.uppercase() }
                }
                subtitles.add(Triple(lang, label, trackUrl))
            }
        }

        val skipRanges = mutableListOf<LongRange>()
        if (hlsSource.has("skipTime")) {
            val skipArr = hlsSource.getJSONArray("skipTime")
            for (i in 0 until skipArr.length()) {
                val item = skipArr.getJSONObject(i)
                val start = item.optLong("start", -1L)
                val end = item.optLong("end", -1L)
                if (start in 0 until end) skipRanges.add(start..end)
            }
        }

        BnsiResult(qualityMap, defaultUrl, subtitles, skipRanges)
    } catch (e: Exception) {
        Log.e(TAG, "parseBnsiJson error: ${e.message}")
        null
    }

    private fun pickBestQuality(qualityMap: Map<String, String>): String {
        val preferred = listOf("1080", "720", "480", "360", "2160")
        return preferred.firstOrNull { qualityMap.containsKey(it) } ?: qualityMap.keys.firstOrNull() ?: "1080"
    }
}
