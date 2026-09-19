package com.sloosh.tv.data.alloha

import android.annotation.SuppressLint
import android.util.Base64
import android.util.Log
import android.webkit.CookieManager
import kotlinx.coroutines.*
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import com.sloosh.tv.ui.util.CodecHelper
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.net.URLDecoder
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

private const val TAG = "HlsProxy"

class HlsProxyServer(
    initialHeaders: Map<String, String>? = null,
    var onSessionExpired: (() -> Unit)? = null
) {
    companion object {
        val shared: HlsProxyServer by lazy { HlsProxyServer() }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null

    /** Actual port assigned by the OS (available after [start]). */
    var port: Int = 0
        private set

    val fixedMasterUrl: String get() = "http://127.0.0.1:$port/master.m3u8"

    @Volatile
    var activeMasterUrl: String = ""
        private set

    val activeHeaders = ConcurrentHashMap<String, String>()

    init {
        if (initialHeaders != null) {
            initialHeaders.forEach { (k, v) ->
                activeHeaders[k.lowercase(Locale.ROOT)] = v
            }
        }
    }

    /** Subtitle tracks: list of (language, name, url) */
    @Volatile
    var subtitleTracks: List<Triple<String, String, String>> = emptyList()

    private val connectionPool = ConnectionPool(32, 5, TimeUnit.MINUTES)

    private val client: OkHttpClient = buildTrustingClient(connectionPool)

    private fun buildTrustingClient(pool: ConnectionPool): OkHttpClient {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(
                @SuppressLint("CustomX509TrustManager")
                object : X509TrustManager {
                    @SuppressLint("TrustAllX509TrustManager")
                    override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                    @SuppressLint("TrustAllX509TrustManager")
                    override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, trustAllCerts, SecureRandom())
            }
            OkHttpClient.Builder()
                .connectionPool(pool)
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to build trusting OkHttpClient: ${e.message}")
            OkHttpClient.Builder()
                .connectionPool(pool)
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }

    fun updateMasterUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank() || trimmed.startsWith("<") || trimmed.startsWith("{") || trimmed.contains("\n") || trimmed.contains(" ") ||
            trimmed.contains("token_movie=") || (!trimmed.contains(".m3u8") && !trimmed.contains(".mp4") && !trimmed.contains(".mpd"))) {
            Log.w(TAG, "Ignoring invalid master URL: ${trimmed.take(80)}")
            return
        }
        activeMasterUrl = trimmed
        Log.d(TAG, "Active master URL updated: $trimmed")
    }

    fun updateMasterUrlSilently(url: String) {
        updateMasterUrl(url)
    }

    fun updateHeaders(headers: Map<String, String>) {
        headers.forEach { (k, v) ->
            activeHeaders[k.lowercase(Locale.ROOT)] = v
        }
    }

    @Synchronized
    fun start(headers: Map<String, String> = emptyMap()) {
        updateHeaders(headers)
        if (serverSocket != null && !serverSocket!!.isClosed) return

        try {
            val ss = ServerSocket()
            ss.reuseAddress = true
            ss.bind(InetSocketAddress("127.0.0.1", 8181))
            serverSocket = ss
            port = 8181
        } catch (_: Exception) {
            try {
                val ss = ServerSocket()
                ss.reuseAddress = true
                ss.bind(InetSocketAddress("127.0.0.1", 0))
                serverSocket = ss
                port = ss.localPort
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start HlsProxyServer: ${e.message}", e)
                return
            }
        }

        scope.launch {
            Log.d(TAG, "HLS proxy started on port $port")
            supervisorScope {
                while (isActive) {
                    val socket = try {
                        serverSocket?.accept() ?: break
                    } catch (_: Exception) {
                        break
                    }
                    launch {
                        try {
                            handleConnection(socket)
                        } catch (t: Throwable) {
                            Log.w(TAG, "Connection handler error: ${t.message}")
                        }
                    }
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        runCatching { serverSocket?.close() }
        serverSocket = null
        port = 0
    }

    fun proxyUrl(originalUrl: String, baseUrl: String? = null): String {
        if (port == 0 || serverSocket == null || serverSocket!!.isClosed) {
            start()
        }
        val absolute = resolveAbsoluteUrl(originalUrl, baseUrl)
        val encoded = Base64.encodeToString(
            absolute.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val cleanUrl = absolute.substringBefore('?')
        val ext = runCatching {
            val path = URI(cleanUrl).path ?: ""
            path.substringAfterLast('.', "")
        }.getOrDefault("")

        val pathSuffix = when {
            ext.equals("m3u8", ignoreCase = true) -> "stream.m3u8"
            ext.isEmpty() -> "stream.m3u8"
            ext.isNotBlank() && ext.length <= 5 && !ext.contains("/") -> "stream.$ext"
            absolute.contains(".m3u8", ignoreCase = true) -> "stream.m3u8"
            else -> "stream.ts"
        }
        return "http://127.0.0.1:$port/proxy/$pathSuffix?url=$encoded"
    }

    fun proxyUrl(originalUrl: String): String = proxyUrl(originalUrl, null)

    private suspend fun handleConnection(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.use { s ->
                s.soTimeout = 30_000
                val input = s.getInputStream().bufferedReader(Charsets.UTF_8)
                val output = BufferedOutputStream(s.getOutputStream(), 64 * 1024)
                val requestLine = input.readLine() ?: return@use
                val parts = requestLine.split(" ")
                if (parts.size < 2) {
                    send404(output)
                    return@use
                }
                val method = parts[0].uppercase(Locale.ROOT)
                val rawPath = parts[1]

                val incomingHeaders = mutableMapOf<String, String>()
                while (true) {
                    val line = input.readLine() ?: break
                    if (line.isBlank()) break
                    val idx = line.indexOf(':')
                    if (idx > 0) {
                        val k = line.substring(0, idx).trim().lowercase(Locale.ROOT)
                        val v = line.substring(idx + 1).trim()
                        incomingHeaders[k] = v
                    }
                }

                if (method == "HEAD") {
                    val header = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nAccept-Ranges: bytes\r\nConnection: close\r\n\r\n"
                    output.write(header.toByteArray(Charsets.UTF_8))
                    output.flush()
                    return@use
                }

                if (rawPath.startsWith("/master.m3u8")) {
                    val master = activeMasterUrl
                    if (master.isBlank()) {
                        send404(output)
                        return@use
                    }
                    servePlaylist(master, output)
                    return@use
                }

                if (rawPath.startsWith("/sub/")) {
                    val trackIndex = rawPath.removePrefix("/sub/").substringBefore(".").toIntOrNull()
                    val track = trackIndex?.let { subtitleTracks.getOrNull(it) }
                    if (track != null) {
                        serveVtt(track.third, output)
                        return@use
                    }
                }

                if (rawPath.contains("/proxy")) {
                    val encoded = extractUrlParam(rawPath)
                    if (encoded.isNullOrBlank()) {
                        send404(output)
                        return@use
                    }
                    val decodedUrl = decodeProxyUrl(encoded)
                    if (decodedUrl.isNullOrBlank()) {
                        send404(output)
                        return@use
                    }

                    val pathWithoutQuery = rawPath.substringBefore('?').lowercase(Locale.ROOT)
                    val pathImpliesPlaylist = pathWithoutQuery.endsWith(".m3u8")
                    val isPlaylist = decodedUrl.contains(".m3u8", ignoreCase = true) || pathImpliesPlaylist
                    if (isPlaylist) {
                        servePlaylist(decodedUrl, output)
                    } else {
                        serveSegment(decodedUrl, incomingHeaders, output)
                    }
                    return@use
                }

                send404(output)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Connection error: ${e.message}")
        }
    }

    private fun extractUrlParam(path: String): String? {
        val match = Regex("""[?&]url=([^&\s]+)""").find(path)
        return match?.groupValues?.get(1)
    }

    private fun decodeProxyUrl(encoded: String): String? {
        // 1. Try URL_SAFE directly (without padding needed)
        runCatching {
            val bytes = Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP)
            val str = String(bytes, Charsets.UTF_8)
            if (str.startsWith("http://") || str.startsWith("https://")) return str
        }
        // 2. Fallback: URL-decoded URL_SAFE
        runCatching {
            val unescaped = URLDecoder.decode(encoded, "UTF-8")
            val bytes = Base64.decode(unescaped, Base64.URL_SAFE or Base64.NO_WRAP)
            val str = String(bytes, Charsets.UTF_8)
            if (str.startsWith("http://") || str.startsWith("https://")) return str
        }
        // 3. Fallback: standard Base64 with padding
        return runCatching {
            var raw = encoded.replace('-', '+').replace('_', '/')
            val remainder = raw.length % 4
            if (remainder > 0) raw = raw.padEnd(raw.length + (4 - remainder), '=')
            val bytes = Base64.decode(raw, Base64.DEFAULT)
            val str = String(bytes, Charsets.UTF_8)
            if (str.startsWith("http://") || str.startsWith("https://")) str else null
        }.getOrNull()
    }

    private fun extractSessionHash(url: String): String? {
        val pattern = Regex("""/(?:1|stream|video|hls)/([A-Za-z0-9_=-]{20,})/""")
        val match = pattern.find(url)
        if (match != null) return match.groupValues[1]

        val path = runCatching { URI(url.substringBefore('?')).path }.getOrNull() ?: url
        val segments = path.split('/')
        return segments.firstOrNull { it.length >= 32 && it.all { c -> c.isLetterOrDigit() || c == '_' || c == '-' } }
    }

    private fun remapSessionHash(targetUrl: String, masterUrl: String): String {
        if (masterUrl.isBlank()) return targetUrl
        val oldHash = extractSessionHash(targetUrl) ?: return targetUrl
        val newHash = extractSessionHash(masterUrl) ?: return targetUrl
        if (oldHash == newHash) return targetUrl

        val replaced = targetUrl.replace(oldHash, newHash)
        Log.i(TAG, "Remapping segment session hash: ...${oldHash.takeLast(8)} -> ...${newHash.takeLast(8)}")
        return replaced
    }

    private fun servePlaylist(url: String, out: OutputStream) {
        val cleanUrl = if (url.startsWith("//")) "https:$url" else url
        var targetUrl = cleanUrl
        var body = fetchText(targetUrl)
        if (body == null && activeMasterUrl.isNotBlank()) {
            val remapped = remapSessionHash(targetUrl, activeMasterUrl)
            if (remapped != targetUrl) {
                targetUrl = remapped
                body = fetchText(targetUrl)
            }
        }
        if (body == null && onSessionExpired != null) {
            Log.i(TAG, "servePlaylist: fetch failed, notifying onSessionExpired and waiting for refresh...")
            onSessionExpired?.invoke()
            val startWait = System.currentTimeMillis()
            while (System.currentTimeMillis() - startWait < 1500 && body == null) {
                try { Thread.sleep(100) } catch (_: Exception) {}
                if (activeMasterUrl.isNotBlank()) {
                    body = fetchText(activeMasterUrl)
                    if (body != null) targetUrl = activeMasterUrl
                }
            }
        }
        if (body == null || !body.contains("#EXT")) {
            Log.w(TAG, "servePlaylist: playlist unavailable or not starting with #EXT for ${targetUrl.take(80)}, sending 503 Retry-After: 1")
            send503(out)
            return
        }
        val rewritten = rewriteM3u8(body, targetUrl)
        val bytes = rewritten.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\nContent-Type: application/vnd.apple.mpegurl\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun serveVtt(url: String, out: OutputStream) {
        val cleanUrl = if (url.startsWith("//")) "https:$url" else url
        val body = fetchText(cleanUrl)
        if (body == null) {
            send503(out)
            return
        }
        val bytes = body.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\nContent-Type: text/vtt\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun serveSegment(url: String, incomingHeaders: Map<String, String>, out: OutputStream) {
        val cleanUrl = if (url.startsWith("//")) "https:$url" else url
        val range = incomingHeaders["range"]

        var candidateUrl = cleanUrl
        val remapped = remapSessionHash(cleanUrl, activeMasterUrl)
        if (remapped != cleanUrl) {
            candidateUrl = remapped
        }

        var response: okhttp3.Response? = null
        var attempts = 0
        while (attempts < 3) {
            attempts++
            try {
                val reqBuilder = buildRequest(candidateUrl)
                if (!range.isNullOrBlank()) {
                    reqBuilder.header("Range", range)
                }
                response = client.newCall(reqBuilder.build()).execute()
                if (response.isSuccessful) break

                val code = response.code
                if (code == 403 || code == 410 || code == 404 || code >= 500) {
                    if (candidateUrl == cleanUrl && remapped != cleanUrl) {
                        Log.i(TAG, "Segment returned $code, trying remapped URL...")
                        candidateUrl = remapped
                    } else if (onSessionExpired != null) {
                        Log.w(TAG, "serveSegment HTTP $code. Notifying onSessionExpired...")
                        onSessionExpired?.invoke()
                    }
                }
                response.close()
                response = null
            } catch (e: Exception) {
                Log.w(TAG, "serveSegment fetch error (attempt $attempts): ${e.message}")
                if (candidateUrl == cleanUrl && remapped != cleanUrl) {
                    candidateUrl = remapped
                }
            }
            if (attempts < 3) {
                try { Thread.sleep(150L * attempts) } catch (_: Exception) {}
            }
        }

        if (response == null || !response.isSuccessful) {
            val code = response?.code ?: 0
            response?.close()
            Log.w(TAG, "serveSegment: upstream failed ($code) for ${candidateUrl.take(80)}, sending 503 Retry-After: 1")
            send503(out)
            return
        }

        response.use { resp ->
            val statusCode = resp.code
            val reason = if (statusCode == 206) "Partial Content" else if (statusCode == 200) "OK" else "Success"
            val contentType = resolveContentType(candidateUrl, resp)
            val contentLength = resp.body?.contentLength() ?: -1L
            val contentRange = resp.header("Content-Range")

            val headerSb = StringBuilder()
            headerSb.append("HTTP/1.1 $statusCode $reason\r\n")
            headerSb.append("Content-Type: $contentType\r\n")
            if (contentLength >= 0) {
                headerSb.append("Content-Length: $contentLength\r\n")
            }
            if (!contentRange.isNullOrBlank()) {
                headerSb.append("Content-Range: $contentRange\r\n")
            }
            headerSb.append("Accept-Ranges: bytes\r\n")
            headerSb.append("Connection: close\r\n\r\n")

            out.write(headerSb.toString().toByteArray(Charsets.UTF_8))
            resp.body?.byteStream()?.use { input ->
                val buffer = ByteArray(32 * 1024)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
            }
            out.flush()
        }
    }

    private fun rewriteM3u8(content: String, baseUrl: String): String {
        val cleanBase = if (baseUrl.startsWith("//")) "https:$baseUrl" else baseUrl
        val cleanContent = content.removePrefix("\uFEFF").trimStart()
        val rawLines = cleanContent.lines()

        val hasStreamInf = rawLines.any { it.trim().startsWith("#EXT-X-STREAM-INF") }

        // Pass 1: Try rewriting with filters (AV1 check on devices lacking HW AV1, failover audio filter, deduplication)
        val shouldFilterAv1 = !CodecHelper.isHardwareAv1Supported()
        val filtered = doRewriteM3u8(rawLines, cleanBase, filterUnsupportedCodecs = shouldFilterAv1)

        // Safety check (mirrors iOS PlaybackHlsRewriter.swift):
        // If the original had #EXT-X-STREAM-INF, but the filtered result has NO #EXT-X-STREAM-INF left,
        // fallback to unfiltered rewriting so ExoPlayer never receives an empty/malformed playlist!
        val hasStreamInfAfterFilter = filtered.any { it.trim().startsWith("#EXT-X-STREAM-INF") }
        val finalLines = if (hasStreamInf && !hasStreamInfAfterFilter) {
            Log.w(TAG, "rewriteM3u8: all variants were filtered! Falling back to unfiltered variants.")
            doRewriteM3u8(rawLines, cleanBase, filterUnsupportedCodecs = false)
        } else {
            filtered
        }

        return finalLines.joinToString("\n")
    }

    private fun doRewriteM3u8(lines: List<String>, cleanBase: String, filterUnsupportedCodecs: Boolean): List<String> {
        val result = mutableListOf<String>()
        val seenVariantKeys = mutableSetOf<String>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            if (trimmed.isEmpty()) {
                result.add(line)
                i++
                continue
            }

            // Audio/Subtitle media lines
            if (trimmed.startsWith("#EXT-X-MEDIA")) {
                val groupId = extractQuotedAttribute(trimmed, "GROUP-ID")
                // Filter duplicate/unsupported failover audio groups (parity with iOS PlaybackHlsRewriter.swift)
                if (groupId?.lowercase(Locale.ROOT)?.startsWith("failover-") == true) {
                    i++
                    continue
                }
                if (trimmed.contains("URI=")) {
                    val modified = trimmed.replace(Regex("""URI\s*=\s*"([^"]+)"""")) { match ->
                        val uri = match.groupValues[1]
                        if (uri.isBlank() || uri == "none") match.value
                        else """URI="${proxyUrl(uri, cleanBase)}""""
                    }
                    result.add(modified)
                } else {
                    result.add(trimmed)
                }
                i++
                continue
            }

            // Stream-Inf lines
            if (trimmed.startsWith("#EXT-X-STREAM-INF")) {
                // Filter AV1 codecs only if requested
                if (filterUnsupportedCodecs && hlsLineHasAV1Codecs(trimmed)) {
                    i++
                    while (i < lines.size && lines[i].trim().isEmpty()) i++
                    if (i < lines.size && !lines[i].trim().startsWith("#")) i++
                    continue
                }

                // Filter failover audio streams
                val audioGroup = extractQuotedAttribute(trimmed, "AUDIO")
                if (audioGroup?.lowercase(Locale.ROOT)?.startsWith("failover-") == true) {
                    i++
                    while (i < lines.size && lines[i].trim().isEmpty()) i++
                    if (i < lines.size && !lines[i].trim().startsWith("#")) i++
                    continue
                }

                // Deduplicate identical variants
                val resolution = extractAttributeValue(trimmed, "RESOLUTION")
                val bandwidth = extractAttributeValue(trimmed, "BANDWIDTH")
                val codecs = extractQuotedAttribute(trimmed, "CODECS")
                val key = listOfNotNull(resolution, bandwidth, codecs, audioGroup).joinToString("|")
                if (key.isNotEmpty() && !seenVariantKeys.add(key)) {
                    i++
                    while (i < lines.size && lines[i].trim().isEmpty()) i++
                    if (i < lines.size && !lines[i].trim().startsWith("#")) i++
                    continue
                }

                // Normalize VIDEO-RANGE (strip PQ/HLG for non-HEVC/HDR to prevent decoder crash)
                val normalizedStreamInf = normalizeStreamInfVideoRange(trimmed)
                result.add(normalizedStreamInf)

                // Advance to the URI line (skipping any intervening empty lines)
                i++
                while (i < lines.size && lines[i].trim().isEmpty()) {
                    i++
                }
                if (i < lines.size) {
                    val nextLine = lines[i].trim()
                    if (!nextLine.startsWith("#") && nextLine.isNotBlank()) {
                        result.add(proxyUrl(nextLine, cleanBase))
                        i++
                        continue
                    }
                }
                continue
            }

            if (trimmed.startsWith("#")) {
                if (trimmed.contains("URI=")) {
                    val modified = trimmed.replace(Regex("""URI\s*=\s*"([^"]+)"""")) { match ->
                        val uri = match.groupValues[1]
                        if (uri.isBlank() || uri == "none") match.value
                        else """URI="${proxyUrl(uri, cleanBase)}""""
                    }
                    result.add(modified)
                } else {
                    result.add(trimmed)
                }
            } else {
                result.add(proxyUrl(trimmed, cleanBase))
            }
            i++
        }

        // Guarantee that the first line is #EXTM3U
        if (result.isNotEmpty() && !result[0].startsWith("#EXTM3U")) {
            result.add(0, "#EXTM3U")
        }

        return result
    }

    private fun normalizeStreamInfVideoRange(line: String): String {
        val lower = line.lowercase(Locale.ROOT)
        val isHdrCapable = lower.contains("hvc1") || lower.contains("hev1") || lower.contains("dvh1") || lower.contains("dvhe")
        if (isHdrCapable) return line
        return line.replace(Regex(""",?\s*VIDEO-RANGE=[^,\s]+""", RegexOption.IGNORE_CASE), "")
    }

    private fun extractQuotedAttribute(line: String, key: String): String? {
        val pattern = Regex("""\b${Regex.escape(key)}\s*=\s*"([^"]+)"""", RegexOption.IGNORE_CASE)
        return pattern.find(line)?.groupValues?.getOrNull(1)
    }

    private fun extractAttributeValue(line: String, key: String): String? {
        val pattern = Regex("""\b${Regex.escape(key)}\s*=\s*([^,\s]+)""", RegexOption.IGNORE_CASE)
        return pattern.find(line)?.groupValues?.getOrNull(1)
    }

    private fun hlsLineHasAV1Codecs(line: String): Boolean {
        val lower = line.lowercase(Locale.ROOT)
        return lower.contains("av01") || lower.contains("codecs=\"av01") || lower.contains("codecs=\"av1")
    }

    private fun resolveAbsoluteUrl(url: String, baseUrl: String?): String {
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        if (url.startsWith("//")) {
            val scheme = if (baseUrl?.startsWith("http://") == true) "http:" else "https:"
            return "$scheme$url"
        }
        if (baseUrl.isNullOrBlank()) return url

        val cleanBase = if (baseUrl.startsWith("//")) "https:$baseUrl" else baseUrl
        val baseQuery = cleanBase.substringAfter('?', "")

        return try {
            val baseUri = URI(cleanBase)
            val resolved = baseUri.resolve(url)
            val resolvedStr = resolved.toString()
            // Preserve query string from baseUrl if relative url did not have its own query
            if (!url.contains("?") && baseQuery.isNotBlank() && !resolvedStr.contains("?")) {
                "$resolvedStr?$baseQuery"
            } else {
                resolvedStr
            }
        } catch (_: Exception) {
            val baseWithoutQuery = cleanBase.substringBefore('?')
            val baseDir = baseWithoutQuery.substringBeforeLast("/") + "/"
            val combined = baseDir + url.removePrefix("/")
            if (!url.contains("?") && baseQuery.isNotBlank() && !combined.contains("?")) {
                "$combined?$baseQuery"
            } else {
                combined
            }
        }
    }

    fun fetchPlaylistText(url: String): String? = fetchText(url)

    private fun fetchText(url: String): String? {
        val cleanUrl = if (url.startsWith("//")) "https:$url" else url
        if (cleanUrl.contains("token_movie=") || cleanUrl.contains("<") || cleanUrl.contains("\n")) {
            Log.w(TAG, "fetchText: rejecting non-media URL: ${cleanUrl.take(80)}")
            return null
        }
        var attempts = 0
        while (attempts < 2) {
            attempts++
            try {
                client.newCall(buildRequest(cleanUrl).build()).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val bodyStr = resp.body?.string()
                        if (!bodyStr.isNullOrBlank()) {
                            return bodyStr.removePrefix("\uFEFF").trimStart()
                        }
                    } else {
                        Log.w(TAG, "fetchText HTTP ${resp.code} for ${cleanUrl.take(80)}")
                        if (resp.code == 403 || resp.code == 410) {
                            onSessionExpired?.invoke()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "fetchText error (attempt $attempts): ${e.message}")
            }
            if (attempts < 2) {
                try { Thread.sleep(150) } catch (_: Exception) {}
            }
        }
        return null
    }

    private fun buildRequest(url: String): Request.Builder {
        val cleanUrl = if (url.startsWith("//")) "https:$url" else url
        val builder = Request.Builder().url(cleanUrl)
            .header("User-Agent", activeHeaders["user-agent"] ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36")
            .header("Accept", "*/*")

        // Active referer/origin or fallback to https://alloha.tv/
        val activeRef = activeHeaders["referer"]?.takeIf {
            it.isNotBlank() && !it.contains("127.0.0.1") && !it.contains("localhost") && !it.contains("about:blank")
        } ?: "https://alloha.tv/"

        val activeOrig = activeHeaders["origin"]?.takeIf {
            it.isNotBlank() && !it.contains("127.0.0.1") && !it.contains("localhost") && !it.contains("about:blank")
        } ?: "https://alloha.tv"

        activeHeaders.forEach { (k, v) ->
            val lower = k.lowercase(Locale.ROOT)
            if (lower != "user-agent" && lower != "accept" && lower != "host" &&
                lower != "content-length" && lower != "connection" && lower != "range" &&
                lower != "accept-encoding" && lower != "referer" && lower != "origin") {
                builder.header(k, v)
                if (lower == "accepts-controls") {
                    builder.header("Accepts-Controls", v)
                }
            }
        }

        builder.header("Referer", activeRef)
        builder.header("Origin", activeOrig)

        return builder
    }

    private fun send404(out: OutputStream) {
        try {
            val response = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
            out.write(response.toByteArray(Charsets.UTF_8))
            out.flush()
        } catch (_: Throwable) {}
    }

    /**
     * Sends HTTP 503 Service Unavailable with Retry-After: 1.
     * Tells ExoPlayer that the playlist/segment is temporarily unavailable,
     * prompting an automatic retry after 1s rather than fatal termination.
     */
    private fun send503(out: OutputStream) {
        try {
            val response = "HTTP/1.1 503 Service Unavailable\r\nContent-Length: 0\r\nRetry-After: 1\r\nConnection: close\r\n\r\n"
            out.write(response.toByteArray(Charsets.UTF_8))
            out.flush()
        } catch (_: Throwable) {}
    }

    private fun resolveContentType(url: String, resp: okhttp3.Response): String {
        val clean = url.substringBefore('?')
        val path = runCatching { URI(clean).path.lowercase(Locale.ROOT) }.getOrDefault(clean.lowercase(Locale.ROOT))
        return when {
            path.endsWith(".ts") -> "video/MP2T"
            path.endsWith(".m4s") || path.endsWith(".mp4") -> "video/mp4"
            path.endsWith(".m4a") -> "audio/mp4"
            path.endsWith(".aac") -> "audio/aac"
            path.endsWith(".vtt") || path.endsWith(".webvtt") -> "text/vtt"
            path.endsWith(".m3u8") -> "application/vnd.apple.mpegurl"
            path.endsWith(".key") || path.endsWith(".bin") -> "application/octet-stream"
            else -> resp.header("Content-Type")?.takeIf { it.isNotBlank() && !it.contains("octet-stream") } ?: "video/MP2T"
        }
    }
}
