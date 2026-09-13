package com.sloosh.tv.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.sloosh.tv.SlooshApplication
import com.sloosh.tv.data.alloha.HlsProxyServer
import com.sloosh.tv.data.api.AllohaResolvedStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.util.ArrayDeque
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "AllohaResolver"

class AllohaRuntimeResolver(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    companion object {
        private var uaIndex = 0
        private val userAgents = listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"
        )

        @Synchronized
        private fun nextUserAgent(): String {
            val idx = uaIndex % userAgents.size
            uaIndex++
            return userAgents[idx]
        }

        private val cache = ConcurrentHashMap<String, Pair<AllohaResolvedStream, Long>>()
        private const val CACHE_TTL_MS = 20_000L

        fun invalidateCache(iframeUrl: String) {
            val clean = if (iframeUrl.startsWith("//")) "https:$iframeUrl" else iframeUrl
            cache.remove(clean)
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    suspend fun resolve(iframeUrl: String): AllohaResolvedStream = withContext(Dispatchers.IO) {
        val cleanUrl = if (iframeUrl.startsWith("//")) "https:$iframeUrl" else iframeUrl

        val cached = cache[cleanUrl]
        if (cached != null && (System.currentTimeMillis() - cached.second) < CACHE_TTL_MS) {
            Log.d(TAG, "Returning cached stream for $cleanUrl")
            return@withContext cached.first
        }

        // Direct stream URL bypass if input is already a direct playable m3u8/mp4
        if (isPlayableURL(cleanUrl)) {
            val uri = runCatching { URI(cleanUrl) }.getOrNull()
            val origin = uri?.let { "${it.scheme}://${it.host}" } ?: "https://alloha.tv"
            val direct = AllohaResolvedStream(
                videoUrl = cleanUrl,
                audioVariants = emptyList(),
                qualityVariants = emptyList(),
                subtitles = emptyList(),
                headers = mapOf(
                    "user-agent" to nextUserAgent(),
                    "referer" to "$origin/"
                )
            )
            cache[cleanUrl] = Pair(direct, System.currentTimeMillis())
            return@withContext direct
        }

        val webViewResult = withContext(Dispatchers.Main) {
            resolveWithWebView(cleanUrl)
        }
        cache[cleanUrl] = Pair(webViewResult, System.currentTimeMillis())
        webViewResult
    }

    private fun isLikelyURL(payload: String): Boolean {
        val trimmed = payload.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("<") || trimmed.contains(" ") || trimmed.contains("\n") || trimmed.contains("\r")) {
            return false
        }
        if (trimmed.length > 1024) return false
        return trimmed.startsWith("http://", ignoreCase = true) ||
               trimmed.startsWith("https://", ignoreCase = true) ||
               trimmed.startsWith("//")
    }

    private fun isMasterPlaylistPayload(payload: String): Boolean {
        if (!isLikelyURL(payload)) return false
        val lower = payload.lowercase(Locale.ROOT)
        return lower.contains("master.m3u8")
    }

    private fun isPlayableURL(url: String): Boolean {
        if (!isLikelyURL(url)) return false
        val lower = url.lowercase(Locale.ROOT)
        if (lower.contains("blank.mp4") || lower.contains("cdn.plyr.io")) return false
        return lower.contains(".m3u8") || lower.contains(".mp4") || lower.contains(".mpd")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun resolveWithWebView(cleanUrl: String): AllohaResolvedStream = suspendCancellableCoroutine { continuation ->
        var webView: WebView? = null
        var attachedContainer: ViewGroup? = null
        var isFinished = false
        val capturedHeaders = ConcurrentHashMap<String, String>()
        val parsedUrl = try { URL(cleanUrl) } catch (e: Exception) { null }
        val origin = if (parsedUrl != null) {
            val portSuffix = if (parsedUrl.port != -1 && parsedUrl.port != 80 && parsedUrl.port != 443) ":${parsedUrl.port}" else ""
            "${parsedUrl.protocol}://${parsedUrl.host.lowercase(Locale.ROOT)}$portSuffix"
        } else {
            "https://alloha.tv"
        }
        val selectedUserAgent = nextUserAgent()

        capturedHeaders["user-agent"] = selectedUserAgent
        capturedHeaders["referer"] = "$origin/"
        capturedHeaders["origin"] = origin
        capturedHeaders["accept"] = "*/*"

        var bestMasterPayload: String? = null
        var bestHlsSourcePayload: String? = null
        var bestDirectPayload: String? = null
        val pendingPayloads = ArrayDeque<String>()

        var timeoutRunnable: Runnable? = null
        var fallbackRunnable: Runnable? = null
        var isCleanedUp = false

        fun cleanup() {
            if (isCleanedUp) return
            isCleanedUp = true
            timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
            fallbackRunnable?.let { mainHandler.removeCallbacks(it) }
            timeoutRunnable = null
            fallbackRunnable = null

            mainHandler.post {
                val container = attachedContainer
                attachedContainer = null
                if (container != null) {
                    runCatching {
                        (container.parent as? ViewGroup)?.removeView(container)
                        container.removeAllViews()
                    }
                }

                val v = webView
                webView = null
                if (v != null) {
                    runCatching {
                        v.stopLoading()
                        v.clearHistory()
                        v.clearCache(true)
                        v.removeJavascriptInterface("AndroidAllohaResolver")
                        v.removeJavascriptInterface("AndroidBridge")
                        v.webViewClient = WebViewClient()
                        v.webChromeClient = WebChromeClient()
                        v.loadUrl("about:blank")
                        mainHandler.postDelayed({
                            runCatching { v.destroy() }
                        }, 1000)
                    }
                }
            }
        }

        fun finishOk(result: AllohaResolvedStream) {
            if (isFinished) return
            if (!isPlayableURL(result.videoUrl)) {
                Log.w(TAG, "finishOk: rejecting non-playable stream URL: ${result.videoUrl.take(80)}")
                return
            }
            isFinished = true
            Log.d(TAG, "Resolved stream successfully: videoUrl=${result.videoUrl}, audioVariants=${result.audioVariants.size}")
            cleanup()
            HlsProxyServer.shared.updateHeaders(result.headers)
            HlsProxyServer.shared.updateMasterUrl(result.videoUrl)
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }

        fun finishError(message: String) {
            if (isFinished) return
            isFinished = true
            Log.e(TAG, "Resolver error: $message")
            cleanup()
            if (continuation.isActive) {
                continuation.resumeWithException(RuntimeException(message))
            }
        }

        fun hasAllohaPlaybackHeaders(): Boolean =
            !capturedHeaders["authorizations"].isNullOrBlank() ||
            !capturedHeaders["authorization"].isNullOrBlank() ||
            !capturedHeaders["accepts-controls"].isNullOrBlank() ||
            !capturedHeaders["accepts-control"].isNullOrBlank()

        fun resolveBestAvailable(fallback: String) {
            if (isFinished) return
            val payloads = listOfNotNull(
                bestHlsSourcePayload,
                bestMasterPayload,
                bestDirectPayload,
                fallback.ifBlank { null }
            ) + pendingPayloads

            val seen = mutableSetOf<String>()

            for (payload in payloads) {
                if (!seen.add(payload)) continue
                val parsed = AllohaRuntimeParser.parsePayload(payload, cleanUrl, capturedHeaders)
                if (parsed != null && (isPlayableURL(parsed.videoUrl) || parsed.audioVariants.isNotEmpty())) {
                    val finalStream = if (!bestMasterPayload.isNullOrBlank() && isMasterPlaylistPayload(bestMasterPayload!!)) {
                        parsed.copy(videoUrl = bestMasterPayload!!)
                    } else {
                        parsed
                    }
                    if (isPlayableURL(finalStream.videoUrl)) {
                        finishOk(finalStream)
                        return
                    }
                }
            }

            // Only fallback to direct playable URL if it is a strictly verified URL
            val direct = bestMasterPayload?.takeIf { isLikelyURL(it) && isPlayableURL(it) }
                ?: bestDirectPayload?.takeIf { isLikelyURL(it) && isPlayableURL(it) }
                ?: fallback.takeIf { isLikelyURL(it) && isPlayableURL(it) }

            if (!direct.isNullOrBlank() && isPlayableURL(direct)) {
                val directStream = AllohaResolvedStream(
                    videoUrl = direct,
                    audioVariants = emptyList(),
                    qualityVariants = emptyList(),
                    subtitles = emptyList(),
                    headers = capturedHeaders
                )
                finishOk(directStream)
            }
        }

        fun scheduleFallbackResolve(payload: String, delayMs: Long) {
            fallbackRunnable?.let { mainHandler.removeCallbacks(it) }
            val task = Runnable {
                if (!isFinished) resolveBestAvailable(payload)
            }
            fallbackRunnable = task
            mainHandler.postDelayed(task, delayMs)
        }

        fun resolveIfReady(payload: String) {
            if (isFinished) return
            when {
                payload.contains("\"hlsSource\"") || payload.contains("hlsSource") -> {
                    bestHlsSourcePayload = payload
                    val delay = if (hasAllohaPlaybackHeaders()) 500L else 3500L
                    scheduleFallbackResolve(payload, delay)
                    if (hasAllohaPlaybackHeaders()) {
                        resolveBestAvailable(payload)
                    }
                }
                isMasterPlaylistPayload(payload) -> {
                    bestMasterPayload = payload
                    val delay = if (bestHlsSourcePayload == null) 2000L else 500L
                    scheduleFallbackResolve(payload, delay)
                    if (hasAllohaPlaybackHeaders()) {
                        resolveBestAvailable(payload)
                    }
                }
                isPlayableURL(payload) -> {
                    bestDirectPayload = payload
                    scheduleFallbackResolve(payload, if (hasAllohaPlaybackHeaders()) 1500L else 4000L)
                }
            }
        }

        fun resolveBestPayloadIfReady() {
            if (isFinished) return
            if (hasAllohaPlaybackHeaders() && (bestHlsSourcePayload != null || bestMasterPayload != null)) {
                resolveBestAvailable(bestHlsSourcePayload ?: bestMasterPayload.orEmpty())
            } else if (hasAllohaPlaybackHeaders() && bestDirectPayload != null) {
                resolveBestAvailable(bestDirectPayload.orEmpty())
            }
        }

        val timeoutTask = Runnable {
            if (!isFinished) {
                Log.w(TAG, "Timeout task firing at 20s. Checking accumulated payloads...")
                val payloads = listOfNotNull(bestHlsSourcePayload, bestMasterPayload, bestDirectPayload) + pendingPayloads
                for (p in payloads) {
                    val parsed = AllohaRuntimeParser.parsePayload(p, cleanUrl, capturedHeaders)
                    if (parsed != null && (isPlayableURL(parsed.videoUrl) || parsed.audioVariants.isNotEmpty())) {
                        val finalStream = if (!bestMasterPayload.isNullOrBlank() && isMasterPlaylistPayload(bestMasterPayload!!)) {
                            parsed.copy(videoUrl = bestMasterPayload!!)
                        } else {
                            parsed
                        }
                        if (isPlayableURL(finalStream.videoUrl)) {
                            finishOk(finalStream)
                            return@Runnable
                        }
                    }
                }
                val direct = bestMasterPayload?.takeIf { isLikelyURL(it) && isPlayableURL(it) }
                    ?: bestDirectPayload?.takeIf { isLikelyURL(it) && isPlayableURL(it) }
                if (!direct.isNullOrBlank() && isPlayableURL(direct)) {
                    val directStream = AllohaResolvedStream(
                        videoUrl = direct,
                        audioVariants = emptyList(),
                        qualityVariants = emptyList(),
                        subtitles = emptyList(),
                        headers = capturedHeaders
                    )
                    finishOk(directStream)
                    return@Runnable
                }
                finishError("Таймаут загрузки видеопотока")
            }
        }
        timeoutRunnable = timeoutTask
        mainHandler.postDelayed(timeoutTask, 20_000L)

        fun parseAndMergeHeaders(headersJson: String?) {
            if (headersJson.isNullOrBlank()) return
            runCatching {
                val hObj = JSONObject(headersJson)
                val keys = hObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = hObj.optString(k, "")
                    if (v.isNotBlank()) {
                        capturedHeaders[k.lowercase(Locale.ROOT)] = v
                    }
                }
            }
        }

        fun processIncomingMessage(raw: String?) {
            if (raw.isNullOrBlank() || isFinished) return
            mainHandler.post {
                if (isFinished) return@post
                runCatching {
                    val obj = JSONObject(raw)
                    val incomingHeaders = obj.optJSONObject("headers")
                    if (incomingHeaders != null) {
                        val keys = incomingHeaders.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val v = incomingHeaders.getString(k)
                            if (v.isNotBlank()) {
                                capturedHeaders[k.lowercase(Locale.ROOT)] = v
                            }
                        }
                        HlsProxyServer.shared.updateHeaders(capturedHeaders)
                        resolveBestPayloadIfReady()
                    }

                    val payload = obj.optString("payload", "")
                    if (payload.isNotBlank()) {
                        pendingPayloads.addLast(payload)
                        while (pendingPayloads.size > 16) pendingPayloads.removeFirst()

                        if (payload.contains("\"hlsSource\"") || payload.contains("hlsSource")) {
                            bestHlsSourcePayload = payload
                            if (hasAllohaPlaybackHeaders()) {
                                resolveBestAvailable(payload)
                                return@runCatching
                            }
                        }

                        if (isMasterPlaylistPayload(payload)) {
                            bestMasterPayload = payload
                            scheduleFallbackResolve(payload, if (bestHlsSourcePayload == null) 2000L else 500L)
                            if (hasAllohaPlaybackHeaders()) {
                                resolveBestAvailable(payload)
                            }
                            return@runCatching
                        }

                        if (isPlayableURL(payload)) {
                            bestDirectPayload = payload
                            scheduleFallbackResolve(payload, if (hasAllohaPlaybackHeaders()) 1500L else 4000L)
                            return@runCatching
                        }

                        resolveIfReady(payload)
                    }
                }
            }
        }

        val jsBridge = object {
            @JavascriptInterface
            fun post(raw: String?) {
                processIncomingMessage(raw)
            }

            @JavascriptInterface
            fun onReady(jsonResponse: String?, headersJson: String?) {
                if (jsonResponse.isNullOrBlank() || isFinished) return
                mainHandler.post {
                    if (isFinished) return@post
                    runCatching {
                        parseAndMergeHeaders(headersJson)
                        val parsed = AllohaRuntimeParser.parsePayload(jsonResponse, cleanUrl, capturedHeaders)
                        if (parsed != null && (parsed.videoUrl.isNotBlank() || parsed.audioVariants.isNotEmpty())) {
                            finishOk(parsed)
                        }
                    }
                }
            }

            @JavascriptInterface
            fun onConfigUpdate(edgeHash: String?, ttl: Int, headersJson: String?) {
                mainHandler.post {
                    if (isFinished) return@post
                    if (!edgeHash.isNullOrBlank()) {
                        capturedHeaders["accepts-controls"] = edgeHash
                    }
                    if (ttl > 0) {
                        capturedHeaders["x-neo-config-ttl"] = ttl.toString()
                    }
                    parseAndMergeHeaders(headersJson)
                    HlsProxyServer.shared.updateHeaders(capturedHeaders)
                    resolveBestPayloadIfReady()
                }
            }

            @JavascriptInterface
            fun onM3u8Refreshed(url: String?, headersJson: String?) {
                mainHandler.post {
                    if (isFinished) return@post
                    parseAndMergeHeaders(headersJson)
                    if (!url.isNullOrBlank() && isPlayableURL(url)) {
                        bestMasterPayload = url
                        HlsProxyServer.shared.updateMasterUrlSilently(url)
                        HlsProxyServer.shared.updateHeaders(capturedHeaders)
                        scheduleFallbackResolve(url, 500L)
                    }
                }
            }

            @JavascriptInterface
            fun onStreamHeaders(headersJson: String?) {
                mainHandler.post {
                    if (isFinished) return@post
                    parseAndMergeHeaders(headersJson)
                    HlsProxyServer.shared.updateHeaders(capturedHeaders)
                    resolveBestPayloadIfReady()
                }
            }

            @JavascriptInterface
            fun onLog(msg: String?) {
                Log.d(TAG, "JS: $msg")
            }
        }

        try {
            val activity = SlooshApplication.currentActivity
            val webViewContext = if (activity != null && !activity.isFinishing && !activity.isDestroyed) activity else context
            val wv = WebView(webViewContext)
            webView = wv

            // Attach WebView to foreground activity window as an active background layer with near-zero alpha.
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                val decorView = activity.window?.decorView as? ViewGroup
                if (decorView != null) {
                    val container = android.widget.FrameLayout(activity).apply {
                        visibility = View.VISIBLE
                        alpha = 0.01f
                        isFocusable = false
                        isFocusableInTouchMode = false
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        addView(wv, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                    }
                    decorView.addView(container, 0)
                    attachedContainer = container
                }
            }

            wv.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_NO_CACHE
                userAgentString = selectedUserAgent
                allowFileAccess = false
                allowContentAccess = false
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
            }

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(wv, true)

            wv.addJavascriptInterface(jsBridge, "AndroidAllohaResolver")
            wv.addJavascriptInterface(jsBridge, "AndroidBridge")

            // Modern AndroidX WebKit injection: document_start script injection into ALL frames (including cross-origin iframes)
            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                try {
                    WebViewCompat.addDocumentStartJavaScript(wv, HOOK_JS, setOf("*"))
                } catch (e: Throwable) {
                    Log.w(TAG, "Could not addDocumentStartJavaScript: ${e.message}")
                }
            }

            if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                try {
                    WebViewCompat.addWebMessageListener(wv, "allohaWebMessageBridge", setOf("*")) { _, message, _, _, _ ->
                        processIncomingMessage(message.data)
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Could not addWebMessageListener: ${e.message}")
                }
            }

            wv.webChromeClient = object : WebChromeClient() {
                override fun getDefaultVideoPoster(): Bitmap {
                    return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(Color.TRANSPARENT)
                    }
                }
            }

            wv.webViewClient = object : WebViewClient() {
                @SuppressLint("WebViewClientOnReceivedSslError")
                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    handler?.proceed()
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    view?.evaluateJavascript(HOOK_JS, null)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.evaluateJavascript(HOOK_JS, null)
                }

                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    val reqUrl = request?.url?.toString() ?: return null

                    // 1. Intercept Bridge requests from injected JS in iframe
                    if (reqUrl.startsWith("https://sloosh-bridge.internal/")) {
                        val uri = runCatching { Uri.parse(reqUrl) }.getOrNull()
                        val edgeHash = uri?.getQueryParameter("edge_hash")
                        val ttl = uri?.getQueryParameter("ttl")?.toIntOrNull() ?: 0
                        var payload = uri?.getQueryParameter("payload")

                        val headerPayload = request.requestHeaders?.get("x-sloosh-payload") ?: request.requestHeaders?.get("X-Sloosh-Payload")
                        if (!headerPayload.isNullOrBlank()) {
                            val decoded = runCatching { URLDecoder.decode(headerPayload, "UTF-8") }.getOrNull()
                            if (!decoded.isNullOrBlank()) payload = decoded
                        }

                        if (!edgeHash.isNullOrBlank()) {
                            capturedHeaders["accepts-controls"] = edgeHash
                            if (ttl > 0) capturedHeaders["x-neo-config-ttl"] = ttl.toString()
                            HlsProxyServer.shared.updateHeaders(capturedHeaders)
                            mainHandler.post { resolveBestPayloadIfReady() }
                        }

                        if (!payload.isNullOrBlank()) {
                            val msg = JSONObject().apply {
                                put("type", "payload")
                                put("payload", payload)
                            }.toString()
                            processIncomingMessage(msg)
                        }

                        val emptyStream = ByteArrayInputStream(ByteArray(0))
                        val bridgeHeaders = mapOf(
                            "Access-Control-Allow-Origin" to "*",
                            "Access-Control-Allow-Methods" to "GET, POST, OPTIONS",
                            "Access-Control-Allow-Headers" to "*"
                        )
                        return WebResourceResponse("text/plain", "UTF-8", 200, "OK", bridgeHeaders, emptyStream)
                    }

                    // Passively capture request headers sent from all frames/XHRs
                    request.requestHeaders?.forEach { (k, v) ->
                        val lower = k.lowercase(Locale.ROOT)
                        if (lower != "host" && lower != "connection" && lower != "accept-encoding") {
                            capturedHeaders[lower] = v
                        }
                    }

                    val refererFromReq = request.requestHeaders?.get("Referer") ?: request.requestHeaders?.get("referer")
                    if (!refererFromReq.isNullOrBlank() && !refererFromReq.contains("about:blank") && !refererFromReq.contains("127.0.0.1")) {
                        capturedHeaders["referer"] = refererFromReq
                    }

                    // 2. Intercept /bnsi/ endpoint directly
                    if (reqUrl.contains("/bnsi/")) {
                        Log.d(TAG, "Intercepted /bnsi/ request: $reqUrl")
                        try {
                            val bnsiBuilder = Request.Builder().url(reqUrl)
                            request.requestHeaders?.forEach { (k, v) ->
                                val lower = k.lowercase(Locale.ROOT)
                                if (lower != "host" && lower != "connection" && lower != "accept-encoding") {
                                    bnsiBuilder.header(k, v)
                                }
                            }
                            capturedHeaders.forEach { (k, v) ->
                                if (bnsiBuilder.build().header(k) == null) {
                                    bnsiBuilder.header(k, v)
                                }
                            }
                            if (bnsiBuilder.build().header("Referer") == null) {
                                bnsiBuilder.header("Referer", "https://allplay.tv/")
                            }
                            if (bnsiBuilder.build().header("Origin") == null) {
                                bnsiBuilder.header("Origin", "https://allplay.tv")
                            }
                            val bnsiResp = httpClient.newCall(bnsiBuilder.build()).execute()
                            val bnsiBody = bnsiResp.body?.string().orEmpty()
                            Log.d(TAG, "Intercepted /bnsi/ response: code=${bnsiResp.code}, len=${bnsiBody.length}")

                            if (bnsiBody.isNotBlank()) {
                                mainHandler.post {
                                    val msg = JSONObject().apply {
                                        put("type", "payload")
                                        put("payload", bnsiBody)
                                    }.toString()
                                    processIncomingMessage(msg)
                                }
                            }

                            val respHeaders = sanitizeInterceptResponseHeaders(bnsiResp.headers)
                            val bnsiBytes = bnsiBody.toByteArray(Charsets.UTF_8)
                            val contentType = bnsiResp.header("Content-Type") ?: "application/json"
                            return WebResourceResponse(
                                contentType,
                                "UTF-8",
                                bnsiResp.code,
                                bnsiResp.message.ifBlank { "OK" },
                                respHeaders,
                                ByteArrayInputStream(bnsiBytes)
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to intercept /bnsi/: ${e.message}")
                        }
                    }

                    // 3. Intercept Alloha iframe HTML request and inject early hooks
                    val isAllohaHtml = (reqUrl.contains("token_movie=") || reqUrl.contains("token=")) &&
                        (request.requestHeaders?.get("Accept")?.contains("text/html") == true ||
                         request.isForMainFrame || (!reqUrl.contains(".js") && !reqUrl.contains(".css") && !reqUrl.contains(".m3u8") && !reqUrl.contains(".mp4") && !reqUrl.contains(".vtt")))

                    if (isAllohaHtml) {
                        Log.d(TAG, "Intercepted Alloha HTML page: $reqUrl")
                        try {
                            val htmlReqBuilder = Request.Builder().url(reqUrl)
                                .header("User-Agent", selectedUserAgent)
                                .header("Referer", "https://allplay.tv/")
                                .header("Origin", "https://allplay.tv")
                                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                            request.requestHeaders?.forEach { (k, v) ->
                                val lower = k.lowercase(Locale.ROOT)
                                if (lower != "host" && lower != "connection" && lower != "accept-encoding" &&
                                    lower != "user-agent" && lower != "referer" && lower != "origin") {
                                    htmlReqBuilder.header(k, v)
                                }
                            }
                            val htmlResp = httpClient.newCall(htmlReqBuilder.build()).execute()
                            val htmlBody = htmlResp.body?.string().orEmpty()
                            Log.d(TAG, "Fetched Alloha HTML body: len=${htmlBody.length}")

                            // Patch Alloha anti-framing check so body is never removed
                            val patchedHtml = htmlBody
                                .replace("var isFramed=false;", "var isFramed=true;")
                                .replace("var isFramed = false;", "var isFramed = true;")

                            // Try direct parse from HTML body in case stream data is embedded
                            val directParsed = AllohaRuntimeParser.parsePayload(patchedHtml, cleanUrl, capturedHeaders)
                            if (directParsed != null && (directParsed.videoUrl.isNotBlank() || directParsed.audioVariants.isNotEmpty())) {
                                Log.d(TAG, "Stream parsed directly from HTML body: ${directParsed.videoUrl}")
                                mainHandler.post { finishOk(directParsed) }
                            }

                            // Inject early hook script right after <head>
                            val scriptToInject = "<script>$IFRAME_INJECTED_HOOK_JS</script>"
                            val modifiedHtml = when {
                                patchedHtml.contains("<head>", ignoreCase = true) ->
                                    patchedHtml.replaceFirst(Regex("<head>", RegexOption.IGNORE_CASE), "<head>$scriptToInject")
                                patchedHtml.contains("<html>", ignoreCase = true) ->
                                    patchedHtml.replaceFirst(Regex("<html>", RegexOption.IGNORE_CASE), "<html><head>$scriptToInject</head>")
                                else -> "$scriptToInject$patchedHtml"
                            }

                            val respHeaders = sanitizeInterceptResponseHeaders(htmlResp.headers)
                            val htmlBytes = modifiedHtml.toByteArray(Charsets.UTF_8)

                            return WebResourceResponse(
                                "text/html",
                                "UTF-8",
                                htmlResp.code,
                                htmlResp.message.ifBlank { "OK" },
                                respHeaders,
                                ByteArrayInputStream(htmlBytes)
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to intercept Alloha HTML: ${e.message}")
                        }
                    }

                    // 4. Passive direct m3u8 capture without hijacking the network request
                    if (isMasterPlaylistPayload(reqUrl) || (isPlayableURL(reqUrl) && !reqUrl.contains("blank"))) {
                        mainHandler.post {
                            if (!isFinished) {
                                bestMasterPayload = reqUrl
                                HlsProxyServer.shared.updateHeaders(capturedHeaders)
                                if (hasAllohaPlaybackHeaders()) {
                                    resolveBestAvailable(reqUrl)
                                } else {
                                    scheduleFallbackResolve(reqUrl, 500L)
                                }
                            }
                        }
                    }

                    return null
                }
            }

            wv.onResume()
            wv.resumeTimers()

            val wrapper = wrapperHtml(cleanUrl)
            wv.loadDataWithBaseURL(cleanUrl, wrapper, "text/html", "UTF-8", cleanUrl)

        } catch (e: Exception) {
            finishError(e.localizedMessage ?: "Ошибка инициализации WebView")
        }

        continuation.invokeOnCancellation {
            mainHandler.post { cleanup() }
        }
    }

    private fun wrapperHtml(url: String): String {
        val escapedUrl = url.replace("\"", "&quot;")
        return """
        <!doctype html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
            <meta name="referrer" content="always">
            <meta name="referrer" content="unsafe-url">
            <style>html, body, iframe { margin:0; padding:0; width:100%; height:100%; background:#000; overflow:hidden; border:0; }</style>
            <script>
            $HOOK_JS
            </script>
        </head>
        <body>
            <iframe id="alloha_iframe" src="$escapedUrl" allow="autoplay; fullscreen; encrypted-media; picture-in-picture" allowfullscreen frameborder="0" referrerpolicy="unsafe-url" style="width:100%;height:100%;border:none;"></iframe>
            <script>
            (function() {
                var f = document.getElementById('alloha_iframe');
                if (f) {
                    f.onload = function() {
                        try {
                            if (typeof window.__slooshTick === 'function') window.__slooshTick();
                        } catch(e) {}
                    };
                }
            })();
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    private fun sanitizeInterceptResponseHeaders(headers: okhttp3.Headers): MutableMap<String, String> {
        val map = mutableMapOf<String, String>()
        for (i in 0 until headers.size) {
            val name = headers.name(i)
            val lower = name.lowercase(Locale.ROOT)
            if (lower != "content-encoding" &&
                lower != "content-length" &&
                lower != "transfer-encoding" &&
                lower != "content-security-policy" &&
                lower != "content-security-policy-report-only" &&
                lower != "x-frame-options") {
                map[name] = headers.value(i)
            }
        }
        map["Access-Control-Allow-Origin"] = "*"
        map["Access-Control-Allow-Methods"] = "GET, POST, OPTIONS"
        map["Access-Control-Allow-Headers"] = "*"
        return map
    }
}

private const val HOOK_JS = """
(function() {
  if (window.__slooshAllohaResolverInstalled) return;
  window.__slooshAllohaResolverInstalled = true;
  var capturedHeaders = {};
  var lastPayload = '';
  var lastM3u8 = '';

  function fixVisibility(targetDoc) {
    try {
      if (!targetDoc) return;
      Object.defineProperty(targetDoc, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
      Object.defineProperty(targetDoc, 'hidden', { get: function() { return false; }, configurable: true });
    } catch(e) {}
  }

  function getBridge(win) {
    if (win && win.AndroidAllohaResolver && typeof win.AndroidAllohaResolver.post === 'function') return win.AndroidAllohaResolver;
    if (win && win.AndroidBridge && typeof win.AndroidBridge.post === 'function') return win.AndroidBridge;
    if (window.AndroidAllohaResolver && typeof window.AndroidAllohaResolver.post === 'function') return window.AndroidAllohaResolver;
    if (window.AndroidBridge && typeof window.AndroidBridge.post === 'function') return window.AndroidBridge;
    try {
      if (window.top && window.top.AndroidAllohaResolver && typeof window.top.AndroidAllohaResolver.post === 'function') return window.top.AndroidAllohaResolver;
      if (window.top && window.top.AndroidBridge && typeof window.top.AndroidBridge.post === 'function') return window.top.AndroidBridge;
    } catch(e) {}
    try {
      if (window.parent && window.parent.AndroidAllohaResolver && typeof window.parent.AndroidAllohaResolver.post === 'function') return window.parent.AndroidAllohaResolver;
      if (window.parent && window.parent.AndroidBridge && typeof window.parent.AndroidBridge.post === 'function') return window.parent.AndroidBridge;
    } catch(e) {}
    return null;
  }

  function post(type, payload, targetWin) {
    try {
      var data = JSON.stringify({ type: type, payload: payload || '', headers: capturedHeaders });
      if (window.allohaWebMessageBridge && typeof window.allohaWebMessageBridge.postMessage === 'function') {
        window.allohaWebMessageBridge.postMessage(data);
      }
      var bridge = getBridge(targetWin);
      if (bridge) {
        bridge.post(data);
      }
    } catch(e) {}
  }

  function putHeader(name, value) {
    if (!name || !value) return;
    capturedHeaders[String(name).toLowerCase()] = String(value);
  }

  function defaultHeaders(win) {
    try {
      if (win.location && win.location.origin) {
        putHeader('origin', win.location.origin);
        putHeader('referer', win.location.origin + '/');
      }
      if (win.navigator && win.navigator.userAgent) {
        putHeader('user-agent', win.navigator.userAgent);
      }
      putHeader('accept', '*/*');
      putHeader('sec-fetch-dest', 'empty');
      putHeader('sec-fetch-mode', 'cors');
      putHeader('sec-fetch-site', 'cross-site');
    } catch(e) {}
  }

  function looksPlayable(text) {
    return typeof text === 'string' && (
      text.indexOf('hlsSource') !== -1 ||
      text.indexOf('.m3u8') !== -1 ||
      text.indexOf('.mp4') !== -1 ||
      text.indexOf('.vtt') !== -1
    );
  }

  function report(payload, targetWin) {
    if (!looksPlayable(payload)) return;
    if (payload === lastPayload) return;
    lastPayload = payload;
    post('payload', payload, targetWin);
  }

  function scan(win) {
    try {
      if (!win || !win.document) return;
      defaultHeaders(win);
      var chunks = [];
      if (win.location && win.location.href && looksPlayable(win.location.href)) chunks.push(win.location.href);
      var media = win.document ? win.document.querySelectorAll('video, source, track') : [];
      for (var i = 0; i < media.length; i++) {
        var s = media[i].currentSrc || media[i].src || media[i].getAttribute('src') || '';
        if (looksPlayable(s)) chunks.push(s);
      }
      if (win.performance && win.performance.getEntriesByType) {
        var entries = win.performance.getEntriesByType('resource');
        for (var p = 0; p < entries.length; p++) {
          var name = entries[p].name || '';
          if (looksPlayable(name)) chunks.push(name);
        }
      }
      if (win.hlsSource) {
        try { chunks.push(JSON.stringify({ hlsSource: win.hlsSource })); } catch(e) {}
      }
      if (win.player && win.player.config) {
        try { chunks.push(JSON.stringify(win.player.config)); } catch(e) {}
      }
      if (win.fileList) {
        try { chunks.push(JSON.stringify(win.fileList)); } catch(e) {}
      }
      for (var c = 0; c < chunks.length; c++) {
        report(chunks[c], win);
      }
    } catch(e) {}
  }

  function triggerPlay(win) {
    try {
      if (!win || !win.document) return;
      fixVisibility(win.document);

      // 1. Always dismiss "Continue watching" modal (time_save) prompt if present
      var timeSaveBtn = win.document.querySelector('.time_save__btn') || win.document.querySelector('.time_save:not(.hidden) button');
      if (timeSaveBtn && typeof timeSaveBtn.click === 'function') {
        timeSaveBtn.click();
      }

      // 2. Direct <video> play
      var video = win.document.querySelector('video');
      if (video) {
        video.muted = true;
        if (video.paused) {
          video.play().catch(function(){});
        }
      }

      // 3. Plyr player API if available
      if (win.player && typeof win.player.play === 'function') {
        try { win.player.play(); } catch(e) {}
      }

      // 4. Click all known play button selectors
      var playSelectors = [
        '.plyr__control--overlaid',
        'button[data-plyr="play"]',
        '.plyr__control[data-plyr="play"]',
        '.allplay__play-btn',
        'button.play',
        '.play-btn',
        '.vjs-big-play-button',
        '.jw-display-icon-container',
        '[data-plyr="play"]'
      ];
      for (var s = 0; s < playSelectors.length; s++) {
        var el = win.document.querySelector(playSelectors[s]);
        if (el && typeof el.click === 'function') {
          el.click();
        }
      }
    } catch(e) {}
  }

  function hookWsInstance(ws, targetWin) {
    try {
      if (!ws || ws.__slooshWsHooked) return;
      ws.__slooshWsHooked = true;
      ws.addEventListener('message', function(event) {
        try {
          var msg = typeof event.data === 'string' ? JSON.parse(event.data) : null;
          if (msg && msg.type === 'config_update' && msg.edge_hash) {
            putHeader('accepts-controls', msg.edge_hash);
            if (msg.ttl) putHeader('x-neo-config-ttl', String(msg.ttl));
            post('headers', '', targetWin);
            var bridge = getBridge(targetWin);
            if (bridge && typeof bridge.onConfigUpdate === 'function') {
              bridge.onConfigUpdate(msg.edge_hash, msg.ttl || 0, JSON.stringify(capturedHeaders));
            }
          }
          if (typeof event.data === 'string' && looksPlayable(event.data)) {
            report(event.data, targetWin);
          }
        } catch(e) {
          if (typeof event.data === 'string' && looksPlayable(event.data)) {
            report(event.data, targetWin);
          }
        }
      });
    } catch(e) {}
  }

  function install(win) {
    try {
      if (!win) return;
      fixVisibility(win.document);
      defaultHeaders(win);

      if (!win.__slooshAllohaHooksInstalled) {
        win.__slooshAllohaHooksInstalled = true;

        // 1. Hook XMLHttpRequest
        var originalOpen = win.XMLHttpRequest && win.XMLHttpRequest.prototype.open;
        var originalSetHeader = win.XMLHttpRequest && win.XMLHttpRequest.prototype.setRequestHeader;
        if (originalOpen && originalSetHeader) {
          win.XMLHttpRequest.prototype.open = function(method, requestUrl) {
            this.__slooshUrl = requestUrl || '';
            this.addEventListener('load', function() {
              var responseUrl = this.responseURL || this.__slooshUrl || '';
              var responseText = '';
              try { responseText = this.responseText || ''; } catch(e) {}
              if (responseUrl.indexOf('/bnsi/') !== -1 && responseText) report(responseText, win);
              if (responseText && responseText.indexOf('hlsSource') !== -1) report(responseText, win);
              if (looksPlayable(responseText)) report(responseText, win);
              if (responseUrl.indexOf('master.m3u8') !== -1 && responseUrl !== lastM3u8) {
                lastM3u8 = responseUrl;
                post('payload', responseUrl, win);
              }
            });
            return originalOpen.apply(this, arguments);
          };
          win.XMLHttpRequest.prototype.setRequestHeader = function(name, value) {
            putHeader(name, value);
            return originalSetHeader.apply(this, arguments);
          };
        }

        // 2. Hook Fetch
        var originalFetch = win.fetch;
        if (originalFetch) {
          win.fetch = function(input, init) {
            try {
              var requestUrl = (typeof input === 'string') ? input : (input && input.url ? input.url : '');
              if (init && init.headers) {
                if (typeof init.headers.forEach === 'function') init.headers.forEach(function(value, name) { putHeader(name, value); });
                else for (var key in init.headers) putHeader(key, init.headers[key]);
              }
              if (input && input.headers && typeof input.headers.forEach === 'function') input.headers.forEach(function(value, name) { putHeader(name, value); });
              if (looksPlayable(requestUrl)) post('payload', requestUrl, win);
            } catch(e) {}

            return originalFetch.apply(this, arguments).then(function(response) {
              try {
                var responseUrl = response.url || '';
                if (looksPlayable(responseUrl)) post('payload', responseUrl, win);
                var clone = response.clone();
                clone.text().then(function(text) { report(text, win); }).catch(function(){});
              } catch(e) {}
              return response;
            });
          };
        }

        // 3. Hook WebSocket
        var OriginalWebSocket = win.WebSocket;
        if (OriginalWebSocket) {
          win.WebSocket = function(url, protocols) {
            var ws = protocols ? new OriginalWebSocket(url, protocols) : new OriginalWebSocket(url);
            hookWsInstance(ws, win);
            return ws;
          };
          win.WebSocket.prototype = OriginalWebSocket.prototype;
          win.WebSocket.CONNECTING = OriginalWebSocket.CONNECTING;
          win.WebSocket.OPEN = OriginalWebSocket.OPEN;
          win.WebSocket.CLOSING = OriginalWebSocket.CLOSING;
          win.WebSocket.CLOSED = OriginalWebSocket.CLOSED;
        }
      }
    } catch(e) {}
  }

  function tick() {
    install(window);
    scan(window);
    triggerPlay(window);
    try {
      var frames = document.querySelectorAll('iframe');
      for (var i = 0; i < frames.length; i++) {
        try {
          var cw = frames[i].contentWindow;
          if (cw) {
            install(cw);
            scan(cw);
            triggerPlay(cw);
          }
        } catch(e) {}
      }
    } catch(e) {}
  }

  window.__slooshTick = tick;
  install(window);
  scan(window);
  triggerPlay(window);
  tick();
  setInterval(tick, 500);
  window.addEventListener('load', tick);
})();
"""

private const val IFRAME_INJECTED_HOOK_JS = """
(function() {
  if (window.__slooshIframeHookInstalled) return;
  window.__slooshIframeHookInstalled = true;

  try {
    Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
    Object.defineProperty(document, 'hidden', { get: function() { return false; }, configurable: true });
  } catch(e) {}

  function sendBridge(params) {
    var json = JSON.stringify(params);

    // 1. Direct JavascriptInterface call (available if same-origin or main frame)
    try {
      var bridge = window.AndroidAllohaResolver || window.AndroidBridge ||
                   (window.top && window.top.AndroidAllohaResolver) ||
                   (window.top && window.top.AndroidBridge) ||
                   (window.parent && window.parent.AndroidAllohaResolver) ||
                   (window.parent && window.parent.AndroidBridge);
      if (bridge && typeof bridge.post === 'function') {
        bridge.post(json);
      }
    } catch(e) {}

    // 2. AndroidX WebMessageListener bridge
    try {
      if (window.allohaWebMessageBridge && typeof window.allohaWebMessageBridge.postMessage === 'function') {
        window.allohaWebMessageBridge.postMessage(json);
      }
    } catch(e) {}

    // 3. Image beacon for lightweight signals (edge_hash, config_update)
    try {
      var qs = [];
      for (var k in params) {
        if (params.hasOwnProperty(k)) {
          var val = String(params[k]);
          if (val.length < 512) {
            qs.push(encodeURIComponent(k) + '=' + encodeURIComponent(val));
          }
        }
      }
      var img = new Image();
      img.src = 'https://sloosh-bridge.internal/msg?' + qs.join('&');
    } catch(e) {}

    // 4. Fetch fallback with payload header for longer strings (m3u8 URLs, bnsi JSON)
    if (params.payload && typeof params.payload === 'string' && params.payload.length > 0) {
      try {
        fetch('https://sloosh-bridge.internal/msg', {
          headers: { 'x-sloosh-payload': encodeURIComponent(params.payload.slice(0, 8192)) }
        }).catch(function(){});
      } catch(e) {}
    }
  }

  function handleWsMessage(raw) {
    try {
      var msg = typeof raw === 'string' ? JSON.parse(raw) : null;
      if (msg && msg.type === 'config_update' && msg.edge_hash) {
        sendBridge({ type: 'config_update', edge_hash: msg.edge_hash, ttl: msg.ttl || 0 });
      }
      if (typeof raw === 'string' && (raw.indexOf('hlsSource') !== -1 || raw.indexOf('.m3u8') !== -1 || raw.indexOf('.mp4') !== -1)) {
        sendBridge({ type: 'payload', payload: raw });
      }
    } catch(e) {
      if (typeof raw === 'string' && (raw.indexOf('hlsSource') !== -1 || raw.indexOf('.m3u8') !== -1 || raw.indexOf('.mp4') !== -1)) {
        sendBridge({ type: 'payload', payload: raw });
      }
    }
  }

  // 1. Early WebSocket Hook - executes before Alloha's __ws_factory is defined
  var RealWebSocket = window.WebSocket;
  if (RealWebSocket) {
    function SlooshWebSocket(url, protocols) {
      var ws = protocols ? new RealWebSocket(url, protocols) : new RealWebSocket(url);
      try {
        ws.addEventListener('message', function(evt) {
          handleWsMessage(evt.data);
        });
      } catch(e) {}
      return ws;
    }
    SlooshWebSocket.prototype = RealWebSocket.prototype;
    SlooshWebSocket.CONNECTING = RealWebSocket.CONNECTING;
    SlooshWebSocket.OPEN = RealWebSocket.OPEN;
    SlooshWebSocket.CLOSING = RealWebSocket.CLOSING;
    SlooshWebSocket.CLOSED = RealWebSocket.CLOSED;

    var origAddEvt = RealWebSocket.prototype.addEventListener;
    if (origAddEvt) {
      RealWebSocket.prototype.addEventListener = function(type, listener, options) {
        if (type === 'message') {
          var wrappedListener = function(evt) {
            handleWsMessage(evt.data);
            return listener.apply(this, arguments);
          };
          return origAddEvt.call(this, type, wrappedListener, options);
        }
        return origAddEvt.apply(this, arguments);
      };
    }

    try {
      var origOnMsgDesc = Object.getOwnPropertyDescriptor(RealWebSocket.prototype, 'onmessage');
      Object.defineProperty(RealWebSocket.prototype, 'onmessage', {
        get: function() {
          return origOnMsgDesc && origOnMsgDesc.get ? origOnMsgDesc.get.call(this) : this.__slooshOnMsg;
        },
        set: function(fn) {
          var wrapped = function(evt) {
            handleWsMessage(evt.data);
            if (typeof fn === 'function') return fn.apply(this, arguments);
          };
          if (origOnMsgDesc && origOnMsgDesc.set) {
            origOnMsgDesc.set.call(this, wrapped);
          } else {
            this.__slooshOnMsg = wrapped;
          }
        },
        configurable: true,
        enumerable: true
      });
    } catch(e) {}

    window.WebSocket = SlooshWebSocket;
  }

  // 2. Hook XMLHttpRequest
  var origOpen = window.XMLHttpRequest && window.XMLHttpRequest.prototype.open;
  if (origOpen) {
    window.XMLHttpRequest.prototype.open = function(method, url) {
      var self = this;
      this.__slooshReqUrl = url || '';
      this.addEventListener('load', function() {
        try {
          var resUrl = self.responseURL || self.__slooshReqUrl || '';
          if (resUrl.indexOf('/bnsi/') !== -1 || (self.responseText && self.responseText.indexOf('hlsSource') !== -1)) {
            if (self.responseText && self.responseText.length < 16384) {
              sendBridge({ type: 'payload', payload: self.responseText });
            }
          }
          if (resUrl.indexOf('master.m3u8') !== -1) {
            sendBridge({ type: 'payload', payload: resUrl });
          }
        } catch(e) {}
      });
      return origOpen.apply(this, arguments);
    };
  }

  // 3. Hook Fetch
  var origFetch = window.fetch;
  if (origFetch) {
    window.fetch = function(input, init) {
      try {
        var reqUrl = (typeof input === 'string') ? input : (input && input.url ? input.url : '');
        if (reqUrl.indexOf('.m3u8') !== -1 || reqUrl.indexOf('.mp4') !== -1) {
          sendBridge({ type: 'payload', payload: reqUrl });
        }
      } catch(e) {}
      return origFetch.apply(this, arguments).then(function(res) {
        try {
          var rUrl = res.url || '';
          if (rUrl.indexOf('.m3u8') !== -1) {
            sendBridge({ type: 'payload', payload: rUrl });
          }
          if (rUrl.indexOf('/bnsi/') !== -1 || rUrl.indexOf('config') !== -1) {
            res.clone().text().then(function(t) {
              if (t && (t.indexOf('hlsSource') !== -1 || t.indexOf('.m3u8') !== -1)) {
                sendBridge({ type: 'payload', payload: t });
              }
            }).catch(function(){});
          }
        } catch(e) {}
        return res;
      });
    };
  }

  // 4. Auto-play trigger to kick-start playback
  function autoPlay() {
    try {
      var timeSaveBtn = document.querySelector('.time_save__btn') || document.querySelector('.time_save:not(.hidden) button');
      if (timeSaveBtn && typeof timeSaveBtn.click === 'function') timeSaveBtn.click();

      var video = document.querySelector('video');
      if (video) {
        video.muted = true;
        if (video.paused) video.play().catch(function(){});
        if (video.src && (video.src.indexOf('.m3u8') !== -1 || video.src.indexOf('.mp4') !== -1)) {
          sendBridge({ type: 'payload', payload: video.src });
        }
      }
      var playSelectors = ['.plyr__control--overlaid', 'button[data-plyr="play"]', '.allplay__play-btn', 'button.play', '.play-btn', '.vjs-big-play-button', '[data-plyr="play"]'];
      for (var i = 0; i < playSelectors.length; i++) {
        var el = document.querySelector(playSelectors[i]);
        if (el && typeof el.click === 'function') el.click();
      }
    } catch(e) {}
  }

  setInterval(autoPlay, 300);
  window.addEventListener('load', autoPlay);
  window.addEventListener('DOMContentLoaded', autoPlay);
})();
"""

