package com.sloosh.tv.data.alloha

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

private const val TAG = "SharedWebViewProvider"

/**
 * Manages a persistent hidden WebView session during playback.
 * Keeping the WebView alive prevents Alloha CDN edge server WebSocket disconnection,
 * which was previously causing edge hash revocation and stream termination after 5-8 minutes.
 */
object SharedWebViewProvider {

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var activeWebView: WebView? = null

    fun retain(webView: WebView) {
        mainHandler.post {
            val prev = activeWebView
            if (prev != null && prev !== webView) {
                runCatching {
                    prev.stopLoading()
                    prev.loadUrl("about:blank")
                    prev.destroy()
                }
            }
            activeWebView = webView
            Log.d(TAG, "Retained persistent session WebView to keep WebSocket alive")
        }
    }

    fun release() {
        mainHandler.post {
            val v = activeWebView
            activeWebView = null
            if (v != null) {
                Log.d(TAG, "Releasing persistent session WebView")
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
                    }, 500)
                }
            }
        }
    }
}
