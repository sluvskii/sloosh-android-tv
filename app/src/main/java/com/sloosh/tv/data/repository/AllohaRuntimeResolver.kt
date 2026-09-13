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
import com.sloosh.tv.SlooshApplication
import com.sloosh.tv.data.alloha.HlsProxyServer
import com.sloosh.tv.data.api.AllohaResolvedStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URI
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
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
        if (trimmed.startsWith("{") || trimmed.startsWith("<") || trimmed.contains(" ")) return false
        return trimmed.startsWith("http://", ignoreCase = true) ||
               trimmed.startsWith("https://", ignoreCase = true) ||
               trimmed.startsWith("//")
    }

    private fun isMasterPlaylistPayload(payload: String): Boolean {
        val lower = payload.lowercase(Locale.ROOT)
        return lower.contains("master.m3u8")
    }

    private fun isPlayableURL(url: String): Boolean {
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
        var livenessRunnable: Runnable? = null
        var fallbackRunnable: Runnable? = null
        var isCleanedUp = false

        fun cleanup() {
            if (isCleanedUp) return
            isCleanedUp = true
            timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
            livenessRunnable?.let { mainHandler.removeCallbacks(it) }
            fallbackRunnable?.let { mainHandler.removeCallbacks(it) }
            timeoutRunnable = null
            livenessRunnable = null
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
            val payloads = listOfNotNull(bestHlsSourcePayload, bestMasterPayload, bestDirectPayload, fallback.ifBlank { null }) + pendingPayloads
            val seen = mutableSetOf<String>()

            for (payload in payloads) {
                if (!seen.add(payload)) continue
                val parsed = AllohaRuntimeParser.parsePayload(payload, cleanUrl, capturedHeaders)
                if (parsed != null && (parsed.videoUrl.isNotBlank() || parsed.audioVariants.isNotEmpty())) {
                    val finalStream = if (!bestMasterPayload.isNullOrBlank() && bestMasterPayload?.contains(".m3u8", ignoreCase = true) == true) {
                        parsed.copy(videoUrl = bestMasterPayload!!)
                    } else {
                        parsed
                    }
                    finishOk(finalStream)
                    return
                }
            }

            // Fallback to direct playable URL if structured parser did not match
            val direct = bestMasterPayload ?: bestDirectPayload ?: (if (isPlayableURL(fallback)) fallback else null)
            if (!direct.isNullOrBlank()) {
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
                    if (hasAllohaPlaybackHeaders()) {
                        resolveBestAvailable(payload)
                    } else {
                        scheduleFallbackResolve(payload, 1500L)
                    }
                }
                isMasterPlaylistPayload(payload) -> {
                    bestMasterPayload = payload
                    val delay = if (bestHlsSourcePayload == null) 800L else 300L
                    scheduleFallbackResolve(payload, delay)
                    if (hasAllohaPlaybackHeaders()) {
                        resolveBestAvailable(payload)
                    }
                }
                isPlayableURL(payload) -> {
                    bestDirectPayload = payload
                    scheduleFallbackResolve(payload, if (hasAllohaPlaybackHeaders()) 400L else 1200L)
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

        val livenessTask = Runnable {
            if (!isFinished) {
                val hasCandidates = bestHlsSourcePayload != null || bestMasterPayload != null || bestDirectPayload != null || pendingPayloads.isNotEmpty()
                if (hasCandidates) {
                    Log.i(TAG, "Early resolution triggering at 3.0s with available candidates")
                    resolveBestAvailable(bestHlsSourcePayload ?: bestMasterPayload ?: bestDirectPayload ?: "")
                }
            }
        }
        livenessRunnable = livenessTask
        mainHandler.postDelayed(livenessTask, 3000L)

        val timeoutTask = Runnable {
            if (!isFinished) {
                Log.w(TAG, "Timeout task firing at 15s. Checking any accumulated payloads...")
                val payloads = listOfNotNull(bestHlsSourcePayload, bestMasterPayload, bestDirectPayload) + pendingPayloads
                for (p in payloads) {
                    val parsed = AllohaRuntimeParser.parsePayload(p, cleanUrl, capturedHeaders)
                    if (parsed != null && (parsed.videoUrl.isNotBlank() || parsed.audioVariants.isNotEmpty())) {
                        val finalStream = if (!bestMasterPayload.isNullOrBlank() && bestMasterPayload?.contains(".m3u8", ignoreCase = true) == true) {
                            parsed.copy(videoUrl = bestMasterPayload!!)
                        } else {
                            parsed
                        }
                        finishOk(finalStream)
                        return@Runnable
                    }
                }
                val direct = bestMasterPayload ?: bestDirectPayload
                if (!direct.isNullOrBlank()) {
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
        mainHandler.postDelayed(timeoutTask, 15_000L)

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

        val jsBridge = object {
            @JavascriptInterface
            fun post(raw: String?) {
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
                                scheduleFallbackResolve(payload, if (bestHlsSourcePayload == null) 800L else 300L)
                                if (hasAllohaPlaybackHeaders()) {
                                    resolveBestAvailable(payload)
                                }
                                return@runCatching
                            }

                            if (isPlayableURL(payload)) {
                                bestDirectPayload = payload
                                scheduleFallbackResolve(payload, if (hasAllohaPlaybackHeaders()) 400L else 1200L)
                                return@runCatching
                            }

                            resolveIfReady(payload)
                        }
                    }
                }
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
                    if (!url.isNullOrBlank()) {
                        bestMasterPayload = url
                        HlsProxyServer.shared.updateMasterUrlSilently(url)
                        HlsProxyServer.shared.updateHeaders(capturedHeaders)
                        scheduleFallbackResolve(url, 300L)
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
            // Critical for Android TV Chromium:
            // 1. Dimensions must NOT be 1x1 (which triggers Chromium tracking-pixel autoplay blocks and collapses responsive CSS).
            // 2. Added at index 0 (behind Compose hierarchy) so it never intercepts clicks/D-pad focus.
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

                    // Passively capture request headers sent from all frames/XHRs
                    request.requestHeaders?.forEach { (k, v) ->
                        val lower = k.lowercase(Locale.ROOT)
                        if (lower != "host" && lower != "connection" && lower != "accept-encoding") {
                            capturedHeaders[lower] = v
                        }
                    }

                    val refererFromReq = request.requestHeaders?.get("Referer") ?: request.requestHeaders?.get("referer")
                    if (!refererFromReq.isNullOrBlank() && !refererFromReq.contains("about:blank")) {
                        capturedHeaders["referer"] = refererFromReq
                    }

                    // Passive direct m3u8 capture without hijacking the network request
                    if (isMasterPlaylistPayload(reqUrl) || (isPlayableURL(reqUrl) && !reqUrl.contains("blank"))) {
                        mainHandler.post {
                            if (!isFinished) {
                                bestMasterPayload = reqUrl
                                HlsProxyServer.shared.updateHeaders(capturedHeaders)
                                if (hasAllohaPlaybackHeaders()) {
                                    resolveBestAvailable(reqUrl)
                                } else {
                                    scheduleFallbackResolve(reqUrl, 300L)
                                }
                            }
                        }
                    }

                    // Native Chromium network stack handles 100% of requests (cookies, SSL, Cloudflare, etc.)
                    return null
                }
            }

            wv.onResume()
            wv.resumeTimers()

            // Load iframe wrapper with base URL matching cleanUrl
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
      var bridge = getBridge(targetWin);
      if (bridge) {
        var data = JSON.stringify({ type: type, payload: payload || '', headers: capturedHeaders });
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
      if (win.location && win.location.href) chunks.push(win.location.href);
      if (win.document && win.document.documentElement) chunks.push(win.document.documentElement.outerHTML);
      var media = win.document ? win.document.querySelectorAll('video, source, track') : [];
      for (var i = 0; i < media.length; i++) {
        var s = media[i].currentSrc || media[i].src || media[i].getAttribute('src') || '';
        if (s) chunks.push(s);
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
      report(chunks.join('\n'), win);
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

      // 4. Click all known play button selectors (Plyr, Alloha, VideoJS, JWPlayer, etc.)
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
                if (typeof init.headers.forEach === 'function') init.headers.forEach(function(v, k) { putHeader(k, v); });
                else for (var key in init.headers) putHeader(key, init.headers[key]);
              }
              if (input && input.headers && typeof input.headers.forEach === 'function') input.headers.forEach(function(v, k) { putHeader(k, v); });
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

        // 3. Hook WebSocket constructor & prototypes
        if (win.WebSocket) {
          var OrigWS = win.WebSocket;
          win.WebSocket = function(url, protocols) {
            var ws = protocols ? new OrigWS(url, protocols) : new OrigWS(url);
            hookWsInstance(ws, win);
            return ws;
          };
          win.WebSocket.prototype = OrigWS.prototype;
          win.WebSocket.CONNECTING = OrigWS.CONNECTING;
          win.WebSocket.OPEN = OrigWS.OPEN;
          win.WebSocket.CLOSING = OrigWS.CLOSING;
          win.WebSocket.CLOSED = OrigWS.CLOSED;

          var origSend = OrigWS.prototype.send;
          if (origSend) {
            OrigWS.prototype.send = function(data) {
              hookWsInstance(this, win);
              return origSend.apply(this, arguments);
            };
          }

          var origAddEvt = OrigWS.prototype.addEventListener;
          if (origAddEvt) {
            OrigWS.prototype.addEventListener = function(type, listener, options) {
              hookWsInstance(this, win);
              return origAddEvt.apply(this, arguments);
            };
          }

          try {
            var origOnMessageDesc = Object.getOwnPropertyDescriptor(OrigWS.prototype, 'onmessage');
            Object.defineProperty(OrigWS.prototype, 'onmessage', {
              get: function() {
                return origOnMessageDesc && origOnMessageDesc.get ? origOnMessageDesc.get.call(this) : this.__slooshOnMessage;
              },
              set: function(fn) {
                hookWsInstance(this, win);
                if (origOnMessageDesc && origOnMessageDesc.set) {
                  origOnMessageDesc.set.call(this, fn);
                } else {
                  this.__slooshOnMessage = fn;
                }
              },
              configurable: true,
              enumerable: true
            });
          } catch(e) {}
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
          var fWin = frames[i].contentWindow;
          if (fWin) {
            install(fWin);
            scan(fWin);
            triggerPlay(fWin);
          }
        } catch(e) {}
      }
    } catch(e) {}
  }

  window.__slooshTick = tick;
  fixVisibility(document);
  install(window);
  scan(window);
  triggerPlay(window);
  tick();
  setInterval(tick, 150);
  window.addEventListener('load', tick);
  window.addEventListener('DOMContentLoaded', tick);
})();
"""
