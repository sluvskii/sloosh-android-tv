package com.sloosh.tv.data.repository

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import com.sloosh.tv.data.api.AllohaResolvedStream
import com.sloosh.tv.data.api.AudioVariant
import com.sloosh.tv.data.api.QualityVariant
import com.sloosh.tv.data.api.SubtitleTrack
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun resolve(iframeUrl: String): AllohaResolvedStream = withContext(Dispatchers.IO) {
        val cleanUrl = if (iframeUrl.startsWith("//")) "https:$iframeUrl" else iframeUrl

        // 0. If cleanUrl is already a direct playable stream URL
        if (isPlayableURL(cleanUrl)) {
            val uri = runCatching { URI(cleanUrl) }.getOrNull()
            val origin = uri?.let { "${it.scheme}://${it.host}" } ?: "https://alloha.tv"
            return@withContext AllohaResolvedStream(
                videoUrl = cleanUrl,
                audioVariants = emptyList(),
                qualityVariants = emptyList(),
                subtitles = emptyList(),
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
                    "Referer" to "$origin/"
                )
            )
        }

        // 1. Try Fast HTTP Hop Loop (like neomovies-mobile)
        val directResult = resolveViaHttpHops(cleanUrl)
        if (directResult != null) {
            return@withContext directResult
        }

        // 2. Launch Attached Headless WebView with full JS hooks + Network Interceptor
        withContext(Dispatchers.Main) {
            resolveWithWebView(cleanUrl)
        }
    }

    private fun resolveViaHttpHops(startUrl: String): AllohaResolvedStream? {
        val visited = mutableSetOf<String>()
        var currentUrl = startUrl

        repeat(3) {
            if (!visited.add(currentUrl)) return null
            val uri = runCatching { URI(currentUrl) }.getOrNull() ?: return null
            val origin = "${uri.scheme}://${uri.host}"
            val headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
                "Referer" to "$origin/"
            )

            val html = try {
                val req = Request.Builder()
                    .url(currentUrl)
                    .addHeader("User-Agent", headers["User-Agent"]!!)
                    .addHeader("Referer", headers["Referer"]!!)
                    .build()
                val resp = httpClient.newCall(req).execute()
                if (!resp.isSuccessful) return null
                resp.body?.string() ?: return null
            } catch (e: Exception) {
                return null
            }

            val parsed = AllohaRuntimeParser.parsePayload(html, origin, headers)
            if (parsed != null && parsed.videoUrl.isNotBlank()) {
                return parsed
            }

            extractDirectStreamUrl(html, origin)?.let { streamUrl ->
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

    private fun isPlayableURL(url: String): Boolean {
        val lower = url.lowercase(Locale.ROOT)
        if (lower.contains("blank.mp4") || lower.contains("cdn.plyr.io")) return false
        return lower.contains(".m3u8") || lower.contains(".mp4") || lower.contains(".mpd")
    }

    private fun findActivity(ctx: Context): Activity? {
        var currentContext = ctx
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
    }

    private suspend fun resolveWithWebView(iframeUrl: String): AllohaResolvedStream = suspendCancellableCoroutine { continuation ->
        var webView: WebView? = null
        var isFinished = false
        val capturedHeaders = linkedMapOf<String, String>()
        val uri = try { URI(iframeUrl) } catch (e: Exception) { null }
        val origin = uri?.let { "${it.scheme}://${it.host}" } ?: "https://alloha.tv"
        val selectedUserAgent = nextUserAgent()

        capturedHeaders["user-agent"] = selectedUserAgent
        capturedHeaders["referer"] = "$origin/"
        capturedHeaders["origin"] = origin

        var bestMasterPayload: String? = null
        var bestHlsSourcePayload: String? = null
        var bestDirectPayload: String? = null
        val pendingPayloads = ArrayDeque<String>()

        val activity = findActivity(context)
        val rootView = activity?.findViewById<ViewGroup>(android.R.id.content)

        fun cleanup() {
            mainHandler.post {
                try {
                    val v = webView
                    if (v != null) {
                        rootView?.removeView(v)
                        v.stopLoading()
                        v.clearHistory()
                        v.clearCache(true)
                        v.removeJavascriptInterface("AndroidAllohaResolver")
                        v.removeJavascriptInterface("AndroidBridge")
                        v.webViewClient = WebViewClient()
                        v.webChromeClient = WebChromeClient()
                        v.loadUrl("about:blank")
                        v.destroy()
                    }
                    webView = null
                } catch (e: Exception) {}
            }
        }

        fun finishOk(result: AllohaResolvedStream) {
            if (isFinished) return
            isFinished = true
            cleanup()
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }

        fun finishError(message: String) {
            if (isFinished) return
            isFinished = true
            cleanup()
            if (continuation.isActive) {
                continuation.resumeWithException(RuntimeException(message))
            }
        }

        val timeoutRunnable = Runnable {
            if (!isFinished) {
                // Try to resolve from anything we collected before timeout
                val payloads = listOfNotNull(bestHlsSourcePayload, bestMasterPayload, bestDirectPayload) + pendingPayloads
                for (p in payloads) {
                    val parsed = AllohaRuntimeParser.parsePayload(p, iframeUrl, capturedHeaders)
                    if (parsed != null && (parsed.videoUrl.isNotBlank() || parsed.audioVariants.isNotEmpty())) {
                        finishOk(parsed)
                        return@Runnable
                    }
                }
                finishError("Таймаут загрузки видеопотока")
            }
        }
        mainHandler.postDelayed(timeoutRunnable, 20_000)

        fun resolveBestAvailable(fallback: String) {
            if (isFinished) return
            val payloads = listOfNotNull(bestHlsSourcePayload, bestMasterPayload, bestDirectPayload, fallback.ifBlank { null }) + pendingPayloads
            for (p in payloads) {
                val parsed = AllohaRuntimeParser.parsePayload(p, iframeUrl, capturedHeaders)
                if (parsed != null && (parsed.videoUrl.isNotBlank() || parsed.audioVariants.isNotEmpty())) {
                    mainHandler.removeCallbacks(timeoutRunnable)
                    finishOk(parsed)
                    return
                }
            }
        }

        val jsBridge = object {
            @JavascriptInterface
            fun post(raw: String?) {
                if (raw.isNullOrBlank() || isFinished) return
                try {
                    val obj = JSONObject(raw)
                    val incomingHeaders = obj.optJSONObject("headers")
                    if (incomingHeaders != null) {
                        val keys = incomingHeaders.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            capturedHeaders[k.lowercase(Locale.ROOT)] = incomingHeaders.getString(k)
                        }
                    }

                    val payload = obj.optString("payload", "")
                    if (payload.isNotBlank()) {
                        pendingPayloads.addLast(payload)
                        while (pendingPayloads.size > 12) pendingPayloads.removeFirst()

                        if (payload.contains("\"hlsSource\"") || payload.contains("hlsSource")) {
                            bestHlsSourcePayload = payload
                            resolveBestAvailable(payload)
                            return
                        }

                        if (payload.contains("master.m3u8", ignoreCase = true)) {
                            bestMasterPayload = payload
                            resolveBestAvailable(payload)
                            return
                        }

                        if (isPlayableURL(payload)) {
                            bestDirectPayload = payload
                            resolveBestAvailable(payload)
                            return
                        }

                        resolveBestAvailable(payload)
                    }
                } catch (e: Exception) {}
            }

            @JavascriptInterface
            fun onReady(jsonResponse: String?, headersJson: String?) {
                if (jsonResponse.isNullOrBlank() || isFinished) return
                try {
                    if (!headersJson.isNullOrBlank()) {
                        val hObj = JSONObject(headersJson)
                        val keys = hObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            capturedHeaders[k.lowercase(Locale.ROOT)] = hObj.getString(k)
                        }
                    }
                    val parsed = AllohaRuntimeParser.parsePayload(jsonResponse, iframeUrl, capturedHeaders)
                    if (parsed != null) {
                        mainHandler.removeCallbacks(timeoutRunnable)
                        finishOk(parsed)
                    }
                } catch (e: Exception) {}
            }
        }

        try {
            val wv = WebView(context).apply {
                visibility = View.INVISIBLE
                layoutParams = ViewGroup.LayoutParams(1, 1)
            }
            webView = wv
            rootView?.addView(wv)

            wv.settings.javaScriptEnabled = true
            wv.settings.domStorageEnabled = true
            wv.settings.mediaPlaybackRequiresUserGesture = false
            wv.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            wv.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            wv.settings.userAgentString = selectedUserAgent

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(wv, true)

            wv.addJavascriptInterface(jsBridge, "AndroidBridge")
            wv.addJavascriptInterface(jsBridge, "AndroidAllohaResolver")

            wv.webChromeClient = WebChromeClient()
            wv.webViewClient = object : WebViewClient() {
                @SuppressLint("WebViewClientOnReceivedSslError")
                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    handler?.proceed()
                }

                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    val reqUrl = request?.url?.toString() ?: return null
                    request.requestHeaders.forEach { (k, v) ->
                        capturedHeaders[k.lowercase(Locale.ROOT)] = v
                    }

                    val pageUri = runCatching { URI(view?.url ?: iframeUrl) }.getOrNull()
                    val reqUri = runCatching { URI(reqUrl) }.getOrNull()
                    val accurateReferer = when {
                        pageUri?.host != null && !pageUri.host.contains("about:blank") -> "${pageUri.scheme ?: "https"}://${pageUri.host}/"
                        reqUri?.host != null -> "${reqUri.scheme ?: "https"}://${reqUri.host}/"
                        else -> iframeUrl
                    }
                    if (!capturedHeaders.containsKey("referer") || capturedHeaders["referer"]?.contains("about:blank") == true) {
                        capturedHeaders["referer"] = accurateReferer
                    }

                    // Direct m3u8 capture
                    if (reqUrl.contains("master.m3u8", ignoreCase = true) ||
                        (reqUrl.contains(".m3u8", ignoreCase = true) && !reqUrl.contains("blank"))) {
                        mainHandler.post {
                            if (!isFinished) {
                                val stream = AllohaResolvedStream(
                                    videoUrl = reqUrl,
                                    audioVariants = emptyList(),
                                    qualityVariants = emptyList(),
                                    subtitles = emptyList(),
                                    headers = capturedHeaders
                                )
                                mainHandler.removeCallbacks(timeoutRunnable)
                                finishOk(stream)
                            }
                        }
                    }

                    // /bnsi/ payload interception
                    if (reqUrl.contains("/bnsi/")) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val reqBuilder = Request.Builder().url(reqUrl)
                                request.requestHeaders.forEach { (k, v) -> reqBuilder.addHeader(k, v) }
                                val resp = httpClient.newCall(reqBuilder.build()).execute()
                                if (resp.isSuccessful) {
                                    val body = resp.body?.string()
                                    if (!body.isNullOrBlank()) {
                                        val parsed = AllohaRuntimeParser.parsePayload(body, iframeUrl, capturedHeaders)
                                        if (parsed != null && !isFinished) {
                                            mainHandler.post {
                                                mainHandler.removeCallbacks(timeoutRunnable)
                                                finishOk(parsed)
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {}
                        }
                    }

                    return null
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val script = """
                        (function() {
                            function tryPlay() {
                                try {
                                    var btn = document.querySelector('.allplay__play-btn') || document.querySelector('button.play');
                                    if (btn) btn.click();
                                    var v = document.querySelector('video');
                                    if (v) { v.muted = true; v.play().catch(function(){}); }
                                } catch(e) {}
                            }
                            tryPlay();
                            setInterval(tryPlay, 1000);
                        })();
                    """.trimIndent()
                    view?.evaluateJavascript(script, null)
                }
            }

            // Load wrapper HTML to hook XHR/fetch/WebSockets with base URL
            wv.loadDataWithBaseURL(iframeUrl, wrapperHtml(iframeUrl), "text/html", "UTF-8", iframeUrl)

        } catch (e: Exception) {
            mainHandler.removeCallbacks(timeoutRunnable)
            finishError(e.localizedMessage ?: "Ошибка инициализации WebView")
        }

        continuation.invokeOnCancellation {
            mainHandler.post {
                mainHandler.removeCallbacks(timeoutRunnable)
                cleanup()
            }
        }
    }

    private fun wrapperHtml(url: String): String {
        return """
        <!doctype html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
            <style>html, body, iframe { margin:0; width:100%; height:100%; background:#000; overflow:hidden; }</style>
        </head>
        <body>
            <iframe id="alloha_iframe" src="$url" allow="autoplay; fullscreen; encrypted-media; picture-in-picture" allowfullscreen frameborder="0"></iframe>
            <script>
            (function() {
              if (window.__neoAllohaResolverInstalled) return;
              window.__neoAllohaResolverInstalled = true;
              var capturedHeaders = {};
              var lastPayload = '';
              var lastM3u8 = '';
              function post(type, payload) {
                try {
                  var data = JSON.stringify({ type: type, payload: payload || '', headers: capturedHeaders });
                  if (window.AndroidAllohaResolver) AndroidAllohaResolver.post(data);
                  if (window.AndroidBridge && window.AndroidBridge.post) AndroidBridge.post(data);
                } catch(e) {}
              }
              function putHeader(name, value) {
                if (!name || !value) return;
                capturedHeaders[String(name).toLowerCase()] = String(value);
              }
              function defaultHeaders(win) {
                try {
                  if (win.navigator && win.navigator.userAgent) putHeader('user-agent', win.navigator.userAgent);
                  if (win.location && win.location.href && win.location.href.indexOf('about:blank') === -1) {
                    putHeader('referer', win.location.href);
                  }
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
                  defaultHeaders(win);
                  var chunks = [];
                  if (win.location && win.location.href) chunks.push(win.location.href);
                  if (win.document && win.document.documentElement) chunks.push(win.document.documentElement.outerHTML);
                  var media = win.document ? win.document.querySelectorAll('video, source, track') : [];
                  for (var i = 0; i < media.length; i++) chunks.push(media[i].currentSrc || media[i].src || media[i].getAttribute('src') || '');
                  if (win.performance && win.performance.getEntriesByType) {
                    var entries = win.performance.getEntriesByType('resource');
                    for (var p = 0; p < entries.length; p++) chunks.push(entries[p].name || '');
                  }
                  report(chunks.join('\n'));
                } catch(e) {}
              }
              function install(win) {
                try {
                  if (!win || win.__neoAllohaHooksInstalled) return;
                  win.__neoAllohaHooksInstalled = true;
                  defaultHeaders(win);
                  var originalOpen = win.XMLHttpRequest && win.XMLHttpRequest.prototype.open;
                  var originalSetHeader = win.XMLHttpRequest && win.XMLHttpRequest.prototype.setRequestHeader;
                  if (originalOpen && originalSetHeader) {
                    win.XMLHttpRequest.prototype.open = function(method, requestUrl) {
                      this.__neoAllohaUrl = requestUrl || '';
                      this.addEventListener('load', function() {
                        var responseUrl = this.responseURL || this.__neoAllohaUrl || '';
                        var responseText = '';
                        try { responseText = this.responseText || ''; } catch(e) {}
                        if (responseUrl.indexOf('/bnsi/') !== -1 && responseText) report(responseText);
                        if (looksPlayable(responseText)) report(responseText);
                        if (responseUrl.indexOf('master.m3u8') !== -1 && responseUrl !== lastM3u8) { lastM3u8 = responseUrl; post('payload', responseUrl); }
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
                      if (!this.__neoAllohaWsHooked) {
                        this.__neoAllohaWsHooked = true;
                        this.addEventListener('message', function(event) {
                          try {
                            var msg = JSON.parse(event.data);
                            if (msg && msg.type === 'config_update' && msg.edge_hash) {
                              putHeader('accepts-controls', msg.edge_hash);
                              if (msg.ttl) putHeader('x-neo-config-ttl', String(msg.ttl));
                              post('headers', '');
                            }
                          } catch(e) {}
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
                    install(frames[i].contentWindow);
                    scan(frames[i].contentWindow);
                    try {
                      var doc = frames[i].contentDocument || frames[i].contentWindow.document;
                      var btn = doc.querySelector('.allplay__play-btn') || doc.querySelector('button.play');
                      if (btn) btn.click();
                      var v = doc.querySelector('video');
                      if (v) { v.muted = true; v.play().catch(function(){}); }
                    } catch(e) {}
                  }
                } catch(e) {}
              }
              tick();
              setInterval(tick, 700);
              window.addEventListener('load', tick);
            })();
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}
