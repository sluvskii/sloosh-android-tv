# Known Errors & Diagnostics

## 1. Alloha Stream Resolution Timeout (`Таймаут загрузки видеопотока`) — RESOLVED ✅

### Symptoms & Log Trace
```
2026-09-06 23:45:03.820 16167-16167 AllohaResolver          com.sloosh.tv                        W  Timeout task firing at 45s. Checking accumulated payloads...
2026-09-06 23:45:04.044 16167-16167 AllohaResolver          com.sloosh.tv                        E  Resolver error: Таймаут загрузки видеопотока
2026-09-06 23:45:04.046 16167-16167 chromium                com.sloosh.tv                        I  [INFO:CONSOLE:1] "safetyTimeOut 33906.70000000298", source: https://antipathic-as.stravers.live/build/app.6f1e2bd3.js (1)
2026-09-06 23:45:04.047 16167-16167 chromium                com.sloosh.tv                        I  [INFO:CONSOLE:1] "Uncaught (in promise) NotSupportedError: The element has no supported sources.", source: https://antipathic-as.stravers.live/build/app.6f1e2bd3.js (1)
2026-09-06 23:45:04.165 16167-16167 PlayerViewModel         com.sloosh.tv                        E  initPlayer error: Таймаут загрузки видеопотока
```

### Root Cause Analysis
1. **Cross-Origin Bridge Breakdown in Android WebView**:
   - In Chromium Android WebView, `@JavascriptInterface` (`AndroidBridge` / `AndroidAllohaResolver`) is strictly bound to the **top-level frame**. Accessing it from inside a cross-origin `<iframe>` throws a DOM `SecurityException`.
   - `WebViewCompat.addWebMessageListener` is not universally supported or active across all child frames.
   - The fallback image/fetch beacon `https://sloosh-bridge.internal/msg` failed because CORS preflight OPTIONS requests failed and `.internal` cannot resolve in Chromium.
   - Consequently, the payload response from Alloha's `/bnsi/` call was never delivered to Kotlin.

2. **34-Second VAST Preroll Ad Delay**:
   - Alloha player embeds Radiant Media Player with VAST ads (`rmp-vast.min.js`).
   - The config object in HTML specifies `"ads": {"enabled": true, "preroll": "jsf:rotateP", "midroll": [{"time": 0, "link": "jsf:rotatePM"}]}`.
   - The player started a 34-second preroll ad timer (`safetyTimeOut 33906.7`), postponing the stream resolution request `/bnsi/` until after ad playback.
   - This exceeded the resolver timeout (20s/45s), terminating resolution with an error.

3. **`NotSupportedError: The element has no supported sources`**:
   - Continuous `autoPlay()` calling `video.play()` on `<video>` when `src` was empty caused uncaught promise rejections in Chromium, freezing playback initialization.

### Solution Applied (`AllohaRuntimeResolver.kt`)
1. **HTML5 `postMessage` Cross-Frame Relay**:
   - Inside `IFRAME_INJECTED_HOOK_JS`, `sendBridge` sends payloads and headers via `window.parent.postMessage(json, '*')` and `window.top.postMessage(json, '*')`.
   - In `wrapperHtml`, added a listener `window.addEventListener('message', function(evt) { ... bridge.post(raw); })` that relays messages to `AndroidBridge.post(raw)` natively.
2. **Ad Neutralization in Intercepted HTML**:
   - In `shouldInterceptRequest`, when intercepting the Alloha player HTML, patched config to disable ads:
     - `"ads": {"enabled": false}`
     - `"preroll": ""`
     - `"midroll": []`
     - `"postroll": ""`
   - Defined dummy ad callback functions (`window.rotateP`, `window.rotatePM`, `window.rotateM1`, `window.rotatePO`, `window.rmpVast`) in `IFRAME_INJECTED_HOOK_JS` to immediately bypass any VAST logic.
3. **Guarded Playback Initialization**:
   - `autoPlay` now verifies `video.currentSrc || (video.src && video.src !== '' && video.src !== 'about:blank') || video.querySelector('source')` before invoking `play()`.
4. **Base URL Parity**:
   - `wv.loadDataWithBaseURL` uses `val baseUrl = "$origin/"`, ensuring matching origin between the wrapper and the iframe.
