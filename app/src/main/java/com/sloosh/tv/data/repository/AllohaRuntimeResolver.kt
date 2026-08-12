package com.sloosh.tv.data.repository

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.sloosh.tv.data.api.AllohaResolvedStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AllohaRuntimeResolver(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    suspend fun resolve(iframeUrl: String): AllohaResolvedStream = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            var webView: WebView? = null
            var isFinished = false

            val timeoutRunnable = Runnable {
                if (!isFinished) {
                    isFinished = true
                    webView?.destroy()
                    if (continuation.isActive) {
                        continuation.resumeWithException(RuntimeException("Alloha runtime resolver timed out"))
                    }
                }
            }

            mainHandler.postDelayed(timeoutRunnable, 20000)

            val jsBridge = object {
                @JavascriptInterface
                fun postPayload(payloadJson: String, headersJson: String) {
                    mainHandler.post {
                        if (isFinished) return@post
                        val headersMap = mutableMapOf<String, String>()
                        try {
                            val headersObj = JSONObject(headersJson)
                            val keys = headersObj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                headersMap[key] = headersObj.getString(key)
                            }
                        } catch (e: Exception) {
                            // ignore header parse
                        }

                        val resolved = AllohaRuntimeParser.parsePayload(payloadJson, iframeUrl, headersMap)
                        if (resolved != null && !isFinished) {
                            isFinished = true
                            mainHandler.removeCallbacks(timeoutRunnable)
                            webView?.destroy()
                            if (continuation.isActive) {
                                continuation.resume(resolved)
                            }
                        }
                    }
                }
            }

            try {
                webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"
                    addJavascriptInterface(jsBridge, "AndroidBridge")
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Inject monitoring script
                            val script = """
                                (function() {
                                    function checkPayloads() {
                                        if (window.player && window.player.config) {
                                            window.AndroidBridge.postPayload(JSON.stringify(window.player.config), "{}");
                                        }
                                        var scripts = document.getElementsByTagName('script');
                                        for (var i = 0; i < scripts.length; i++) {
                                            var text = scripts[i].innerText || scripts[i].textContent;
                                            if (text && text.indexOf('hlsSource') !== -1) {
                                                window.AndroidBridge.postPayload(text, "{}");
                                            }
                                        }
                                    }
                                    checkPayloads();
                                    setInterval(checkPayloads, 1000);
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(script, null)
                        }
                    }
                }

                val wrapperHtml = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="utf-8">
                        <style>html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; }</style>
                    </head>
                    <body>
                        <iframe src="$iframeUrl" width="100%" height="100%" frameborder="0" allowfullscreen></iframe>
                    </body>
                    </html>
                """.trimIndent()

                webView.loadDataWithBaseURL(iframeUrl, wrapperHtml, "text/html", "UTF-8", null)

            } catch (e: Exception) {
                mainHandler.removeCallbacks(timeoutRunnable)
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }

            continuation.invokeOnCancellation {
                mainHandler.post {
                    mainHandler.removeCallbacks(timeoutRunnable)
                    webView?.destroy()
                }
            }
        }
    }
}
