package com.sloosh.tv.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import java.net.URL
import java.util.ArrayDeque
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "AllohaResolver"

class AllohaRuntimeResolver(private val context: Context) {

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
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun resolve(iframeUrl: String): AllohaResolvedStream = withContext(Dispatchers.IO) {
        val cleanUrl = if (iframeUrl.startsWith("//")) "https:$iframeUrl" else iframeUrl

        val cached = cache[cleanUrl]
        if (cached != null && (System.currentTimeMillis() - cached.second) < CACHE_TTL_MS) {
            Log.d(TAG, "Returning cached stream for $cleanUrl")
            return@withContext cached.first
        }

        // 0. Direct stream URL bypass if input is already a direct playable m3u8/mp4
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

        // 1. Fast HTTP Hop Loop (resolve direct movies/iframes via OkHttp in ~100-200ms)
        val httpHopResult = resolveViaHttpHops(cleanUrl)
        if (httpHopResult != null) {
            Log.d(TAG, "Resolved via HTTP hops: ${httpHopResult.videoUrl}")
            cache[cleanUrl] = Pair(httpHopResult, System.currentTimeMillis())
            return@withContext httpHopResult
        }

        // 2. Headless WebView with clean passive network interception (parity with iOS AllohaRuntimeResolver.swift)
        val webViewResult = withContext(Dispatchers.Main) {
            resolveWithWebView(cleanUrl)
        }
        cache[cleanUrl] = Pair(webViewResult, System.currentTimeMillis())
        webViewResult
    }

    private fun resolveViaHttpHops(startUrl: String): AllohaResolvedStream? {
        val visited = mutableSetOf<String>()
        var currentUrl = startUrl

        repeat(3) {
            if (!visited.add(currentUrl)) return null
            val uri = runCatching { URI(currentUrl) }.getOrNull() ?: return null
            val origin = "${uri.scheme}://${uri.host}"
            val headers = mapOf(
                "user-agent" to nextUserAgent(),
                "referer" to "$origin/",
                "origin" to origin,
                "accept" to "*/*"
            )

            val html = try {
                val req = Request.Builder()
                    .url(currentUrl)
                    .addHeader("User-Agent", headers["user-agent"]!!)
                    .addHeader("Referer", headers["referer"]!!)
                    .build()
                val resp = httpClient.newCall(req).execute()
                if (!resp.isSuccessful) return null
                resp.body?.string() ?: return null
            } catch (e: Exception) {
                return null
            }

            val parsed = AllohaRuntimeParser.parsePayload(html, currentUrl, headers)
            if (parsed != null && (parsed.videoUrl.isNotBlank() || parsed.audioVariants.isNotEmpty())) {
                HlsProxyServer.shared.updateHeaders(headers)
                HlsProxyServer.shared.updateMasterUrl(parsed.videoUrl)
                return parsed
            }

            extractDirectStreamUrl(html, origin)?.let { streamUrl ->
                HlsProxyServer.shared.updateHeaders(headers)
                HlsProxyServer.shared.updateMasterUrl(streamUrl)
                return AllohaResolvedStream(
                    videoUrl = streamUrl,
                    audioVariants = emptyList(),
                    qualityVariants = emptyList(),
                    subtitles = emptyList(),
                    headers = headers
                )
            }

            val nested = extractIframeSrc(html)
            if (!nested.isNullOrBlank()) {
                currentUrl = runCatching { URI(currentUrl).resolve(nested).toString() }.getOrDefault(nested)
            } else {
                return null
            }
        }
        return null
    }

    private fun extractIframeSrc(html: String): String? {
        val match = Regex("""<iframe[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)
        return match?.groupValues?.getOrNull(1)
    }

    private fun extractDirectStreamUrl(html: String, baseUrl: String): String? {
        val patterns = listOf(
            Regex("""https?:\\\/\\\/[^"'\s>]+?\.(m3u8|mpd|mp4)[^"'\s>]*""", RegexOption.IGNORE_CASE),
            Regex("""https?://[^"'\s>]+?\.(m3u8|mpd|mp4)[^"'\s>]*""", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val raw = pattern.find(html)?.value?.replace("\\/", "/") ?: continue
            if (raw.contains("blank.mp4") || raw.contains("cdn.plyr.io")) continue
            return runCatching { URI(baseUrl).resolve(raw).toString() }.getOrDefault(raw)
        }
        return null
    }

    private fun isLikelyURL(payload: String): Boolean {
        val trimmed = payload.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("<") || trimmed.contains(" ") || trimmed.contains("\n") || trimmed.contains("\r")) {
            return false
        }
        if (trimmed.length > 2048) return false
        return trimmed.startsWith("http://", ignoreCase = true) ||
               trimmed.startsWith("https://", ignoreCase = true) ||
               trimmed.startsWith("//")
    }

    private fun isMasterPlaylistPayload(payload: String): Boolean {
        if (!isLikelyURL(payload)) return false
        return payload.contains("master.m3u8", ignoreCase = true)
    }

    private fun isPlayableURL(url: String): Boolean {
        val lower = url.lowercase(Locale.ROOT)
        if (lower.contains("blank.mp4") || lower.contains("cdn.plyr.io")) return false
        return lower.contains(".m3u8") || lower.contains(".mp4") || lower.contains(".mpd")
    }

    private fun isPlayablePayload(payload: String): Boolean {
        if (!isLikelyURL(payload)) return false
        return isPlayableURL(payload)
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
                        v.removeJavascriptInterface("allohaResolver")
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
            !capturedHeaders["accepts-controls"].isNullOrBlank()

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
                if (parsed != null) {
                    if (parsed.audioVariants.isNotEmpty()) {
                        val chosenUrl = if (!bestMasterPayload.isNullOrBlank() && isMasterPlaylistPayload(bestMasterPayload!!)) {
                            bestMasterPayload!!
                        } else {
                            parsed.audioVariants.firstOrNull { it.url.isNotBlank() }?.url ?: parsed.videoUrl
                        }
                        if (chosenUrl.isNotBlank() && isPlayableURL(chosenUrl)) {
                            finishOk(parsed.copy(videoUrl = chosenUrl, headers = capturedHeaders))
                            return
                        }
                    }
                    if (parsed.videoUrl.isNotBlank() && isPlayableURL(parsed.videoUrl)) {
                        finishOk(parsed.copy(headers = capturedHeaders))
                        return
                    }
                }
            }

            // Fallback to master playlist URL if intercepted
            val masterUrl = bestMasterPayload
            if (!masterUrl.isNullOrBlank() && isLikelyURL(masterUrl) && isPlayableURL(masterUrl)) {
                finishOk(
                    AllohaResolvedStream(
                        videoUrl = masterUrl,
                        audioVariants = emptyList(),
                        qualityVariants = emptyList(),
                        subtitles = emptyList(),
                        headers = capturedHeaders
                    )
                )
                return
            }

            // Fallback to direct playable URL
            val directUrl = bestDirectPayload
            if (!directUrl.isNullOrBlank() && isLikelyURL(directUrl) && isPlayableURL(directUrl)) {
                finishOk(
                    AllohaResolvedStream(
                        videoUrl = directUrl,
                        audioVariants = emptyList(),
                        qualityVariants = emptyList(),
                        subtitles = emptyList(),
                        headers = capturedHeaders
                    )
                )
                return
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
            if (payload.contains("hlsSource")) {
                bestHlsSourcePayload = payload
                val delay = if (hasAllohaPlaybackHeaders()) 500L else 4500L
                scheduleFallbackResolve(payload, delay)
                if (hasAllohaPlaybackHeaders()) {
                    resolveBestAvailable(payload)
                }
                return
            }

            if (isPlayablePayload(payload)) {
                bestDirectPayload = payload
                val delay = if (hasAllohaPlaybackHeaders()) 1500L else 5000L
                scheduleFallbackResolve(payload, delay)
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
                            val v = incomingHeaders.optString(k, "")
                            if (v.isNotBlank()) {
                                capturedHeaders[k.lowercase(Locale.ROOT)] = v
                            }
                        }
                        HlsProxyServer.shared.updateHeaders(capturedHeaders)
                        resolveBestPayloadIfReady()
                    }

                    val edgeHash = obj.optString("edge_hash")
                    if (edgeHash.isNotBlank()) {
                        capturedHeaders["accepts-controls"] = edgeHash
                        val ttl = obj.optInt("ttl", 0)
                        if (ttl > 0) capturedHeaders["x-neo-config-ttl"] = ttl.toString()
                        HlsProxyServer.shared.updateHeaders(capturedHeaders)
                        resolveBestPayloadIfReady()
                    }

                    val payload = obj.optString("payload", "")
                    if (payload.isNotBlank()) {
                        pendingPayloads.addLast(payload)
                        while (pendingPayloads.size > 16) pendingPayloads.removeFirst()

                        if (isMasterPlaylistPayload(payload)) {
                            bestMasterPayload = payload
                            scheduleFallbackResolve(payload, if (bestHlsSourcePayload == null) 2400L else 800L)
                            return@runCatching
                        }

                        resolveIfReady(payload)
                    }
                }.onFailure { e ->
                    Log.w(TAG, "processIncomingMessage error: ${e.message}")
                }
            }
        }

        val jsBridge = object {
            @JavascriptInterface
            fun post(raw: String?) {
                processIncomingMessage(raw)
            }

            @JavascriptInterface
            fun postMessage(raw: String?) {
                processIncomingMessage(raw)
            }
        }

        try {
            val activity = SlooshApplication.currentActivity
            val webViewContext = if (activity != null && !activity.isFinishing && !activity.isDestroyed) activity else context
            val wv = WebView(webViewContext)
            webView = wv

            // Attach WebView to foreground activity window as an active background layer with near-zero alpha
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

            wv.addJavascriptInterface(jsBridge, "allohaResolver")
            wv.addJavascriptInterface(jsBridge, "AndroidAllohaResolver")
            wv.addJavascriptInterface(jsBridge, "AndroidBridge")

            // Modern AndroidX WebKit document_start script injection into ALL frames (including iframes)
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

                    // Passively capture request headers
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

                    // Passively observe master playlist or direct playable URL
                    if (isMasterPlaylistPayload(reqUrl) || (isPlayableURL(reqUrl) && !reqUrl.contains("blank"))) {
                        mainHandler.post {
                            if (!isFinished) {
                                bestMasterPayload = reqUrl
                                HlsProxyServer.shared.updateHeaders(capturedHeaders)
                                if (hasAllohaPlaybackHeaders()) {
                                    resolveBestAvailable(reqUrl)
                                } else {
                                    scheduleFallbackResolve(reqUrl, 800L)
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
            val parsed = runCatching { URL(cleanUrl) }.getOrNull()
            val baseUrl = if (parsed != null) {
                val path = parsed.path.substringBeforeLast('/', "")
                "${parsed.protocol}://${parsed.host}${if (parsed.port != -1 && parsed.port != 80 && parsed.port != 443) ":${parsed.port}" else ""}$path/"
            } else {
                cleanUrl
            }
            wv.loadDataWithBaseURL(baseUrl, wrapper, "text/html", "UTF-8", baseUrl)

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
            (function() {
                window.addEventListener('message', function(evt) {
                    if (!evt || !evt.data) return;
                    try {
                        var raw = typeof evt.data === 'string' ? evt.data : JSON.stringify(evt.data);
                        if (window.allohaWebMessageBridge && typeof window.allohaWebMessageBridge.postMessage === 'function') {
                            window.allohaWebMessageBridge.postMessage(raw);
                        }
                        var bridge = window.allohaResolver || window.AndroidAllohaResolver || window.AndroidBridge;
                        if (bridge && typeof bridge.postMessage === 'function') {
                            bridge.postMessage(raw);
                        } else if (bridge && typeof bridge.post === 'function') {
                            bridge.post(raw);
                        }
                    } catch(e) {}
                });
            })();
            </script>
        </head>
        <body>
            <iframe id="alloha_iframe" src="$escapedUrl" allow="autoplay; fullscreen; encrypted-media; picture-in-picture" allowfullscreen frameborder="0" referrerpolicy="unsafe-url" style="width:100%;height:100%;border:none;"></iframe>
        </body>
        </html>
        """.trimIndent()
    }
}

private const val HOOK_JS = """
(function() {
  if (window.__slooshAllohaResolverInstalled) return;
  window.__slooshAllohaResolverInstalled = true;

  // Guard: prevent anti-framing scripts from destroying <body> and fileList
  try {
    if (typeof Element !== 'undefined' && Element.prototype && Element.prototype.remove) {
      var origElemRemove = Element.prototype.remove;
      Element.prototype.remove = function() {
        if (this && this.tagName && this.tagName.toLowerCase() === 'body') {
          return;
        }
        return origElemRemove.apply(this, arguments);
      };
    }
  } catch(e) {}

  var capturedHeaders = {};
  var lastPayload = '';
  var lastM3u8 = '';

  function post(type, payload) {
    try {
      var data = JSON.stringify({ type: type, payload: payload || '', headers: capturedHeaders });
      if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.allohaResolver) {
        try { window.webkit.messageHandlers.allohaResolver.postMessage({ type: type, payload: payload || '', headers: capturedHeaders }); } catch(e) {}
      }
      if (window.parent && window.parent !== window) {
        try { window.parent.postMessage(data, '*'); } catch(e) {}
      }
      if (window.top && window.top !== window) {
        try { window.top.postMessage(data, '*'); } catch(e) {}
      }
      if (window.allohaWebMessageBridge && typeof window.allohaWebMessageBridge.postMessage === 'function') {
        try { window.allohaWebMessageBridge.postMessage(data); } catch(e) {}
      }
      var bridge = window.allohaResolver || window.AndroidAllohaResolver || window.AndroidBridge;
      if (bridge && typeof bridge.postMessage === 'function') {
        bridge.postMessage(data);
      } else if (bridge && typeof bridge.post === 'function') {
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
      if (win && win.location && win.location.origin) {
        putHeader('origin', win.location.origin);
        putHeader('referer', win.location.origin + '/');
      }
      if (win && win.navigator && win.navigator.userAgent) {
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

  function report(payload) {
    if (!looksPlayable(payload)) return;
    if (payload === lastPayload) return;
    lastPayload = payload;
    post('payload', payload);
  }

  function scan(win) {
    try {
      if (!win) return;
      defaultHeaders(win);
      var chunks = [];
      if (win.location && win.location.href && looksPlayable(win.location.href)) chunks.push(win.location.href);
      if (win.document && win.document.documentElement) {
        var html = win.document.documentElement.outerHTML || '';
        if (looksPlayable(html)) chunks.push(html);
      }
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
      if (chunks.length > 0) {
        report(chunks.join('\n'));
      }
    } catch(e) {}
  }

  function install(win) {
    try {
      if (!win || win.__slooshAllohaHooksInstalled) return;
      win.__slooshAllohaHooksInstalled = true;
      defaultHeaders(win);

      var originalOpen = win.XMLHttpRequest && win.XMLHttpRequest.prototype.open;
      var originalSetHeader = win.XMLHttpRequest && win.XMLHttpRequest.prototype.setRequestHeader;
      if (originalOpen && originalSetHeader) {
        win.XMLHttpRequest.prototype.open = function(method, requestUrl) {
          this.__slooshUrl = requestUrl || '';
          this.addEventListener('load', function() {
            var responseUrl = this.responseURL || this.__slooshUrl || '';
            var responseText = '';
            try { responseText = this.responseText || ''; } catch(e) {}
            if (responseUrl.indexOf('/bnsi/') !== -1 && responseText) report(responseText);
            if (responseText && responseText.indexOf('hlsSource') !== -1) report(responseText);
            if (looksPlayable(responseText)) report(responseText);
            if (responseUrl.indexOf('master.m3u8') !== -1 && responseUrl !== lastM3u8) {
              lastM3u8 = responseUrl;
              post('payload', responseUrl);
            }
          });
          return originalOpen.apply(this, arguments);
        };
        win.XMLHttpRequest.prototype.setRequestHeader = function(name, value) {
          putHeader(name, value);
          return originalSetHeader.apply(this, arguments);
        };
      }

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
            if (looksPlayable(requestUrl)) post('payload', requestUrl);
          } catch(e) {}

          return originalFetch.apply(this, arguments).then(function(response) {
            try {
              var responseUrl = response.url || '';
              if (looksPlayable(responseUrl)) post('payload', responseUrl);
              var clone = response.clone();
              clone.text().then(function(text) { report(text); }).catch(function(){});
            } catch(e) {}
            return response;
          });
        };
      }

      var originalSend = win.WebSocket && win.WebSocket.prototype.send;
      if (originalSend) {
        win.WebSocket.prototype.send = function(data) {
          if (!this.__slooshWsHooked) {
            this.__slooshWsHooked = true;
            this.addEventListener('message', function(event) {
              try {
                var msg = typeof event.data === 'string' ? JSON.parse(event.data) : null;
                if (msg && msg.type === 'config_update' && msg.edge_hash) {
                  putHeader('accepts-controls', msg.edge_hash);
                  if (msg.ttl) putHeader('x-neo-config-ttl', String(msg.ttl));
                  post('headers', '');
                }
                if (typeof event.data === 'string' && looksPlayable(event.data)) {
                  report(event.data);
                }
              } catch(e) {
                if (typeof event.data === 'string' && looksPlayable(event.data)) {
                  report(event.data);
                }
              }
            });
          }
          return originalSend.apply(this, arguments);
        };
      }
    } catch(e) {}
  }

  function tick() {
    install(window);
    scan(window);
    try {
      var frames = document.querySelectorAll('iframe');
      for (var i = 0; i < frames.length; i++) {
        try {
          if (frames[i].contentWindow) {
            install(frames[i].contentWindow);
            scan(frames[i].contentWindow);
          }
        } catch(e) {}
      }
    } catch(e) {}
  }

  install(window);
  scan(window);
  tick();
  setInterval(tick, 500);
  window.addEventListener('load', tick);
})();
"""
