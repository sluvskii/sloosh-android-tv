package com.sloosh.tv.data.repository

import com.sloosh.tv.data.api.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.Locale

object AllohaRuntimeParser {

    fun parsePayload(payload: String, baseUrl: String, headers: Map<String, String>): AllohaResolvedStream? {
        val uri = try { URI(baseUrl) } catch (e: Exception) { return null }

        parseAllohaBNsiStream(payload, uri, headers)?.let { return it }

        firstPreferredStreamURL(payload, uri)?.let { fallback ->
            return AllohaResolvedStream(
                videoUrl = fallback,
                audioVariants = emptyList(),
                qualityVariants = emptyList(),
                subtitles = subtitleTracks(payload, uri),
                headers = headers,
                introRange = null,
                outroRange = null
            )
        }

        return null
    }

    private fun parseAllohaBNsiStream(payload: String, baseUrl: URI, headers: Map<String, String>): AllohaResolvedStream? {
        val candidates = listOf(payload) + embeddedJSONObjectCandidates(payload)

        for (candidate in candidates) {
            val obj = try { JSONObject(candidate) } catch (e: Exception) { continue }
            val source = when {
                obj.optJSONArray("hlsSource") != null -> obj.optJSONArray("hlsSource")
                obj.optJSONObject("hlsSource") != null -> JSONArray().apply { put(obj.getJSONObject("hlsSource")) }
                obj.optJSONArray("sources") != null -> obj.optJSONArray("sources")
                obj.optJSONArray("playlist") != null -> obj.optJSONArray("playlist")
                else -> null
            } ?: continue

            val qualityVariants = mutableListOf<QualityVariant>()
            val audioVariants = mutableListOf<AudioVariant>()
            var masterURL: String? = null

            for (i in 0 until source.length()) {
                val item = try { source.getJSONObject(i) } catch (e: Exception) { continue }
                val quality = item.optJSONObject("quality") ?: item.optJSONObject("qualities") ?: item

                val itemVariants = mutableListOf<QualityVariant>()
                var itemMasterURL: String? = null

                val keys = quality.keys()
                while (keys.hasNext()) {
                    val label = keys.next()
                    val rawValue = quality.get(label)
                    for (rawURL in qualityURLStrings(rawValue)) {
                        val urls = allohaURLs(rawURL, baseUrl)
                        if (masterURL == null) masterURL = urls.firstOrNull { isMasterM3u8(it) }
                        if (itemMasterURL == null) itemMasterURL = urls.firstOrNull { isMasterM3u8(it) }
                        val target = urls.firstOrNull { !isMasterM3u8(it) } ?: urls.firstOrNull() ?: continue

                        val variant = QualityVariant(
                            label = normalizedQualityLabel(label),
                            url = target
                        )
                        itemVariants.add(variant)
                        qualityVariants.add(variant)
                    }
                }

                val sortedItemVariants = itemVariants.sortedBy { qualityHeight(it.label) }
                val chosenURL = itemMasterURL ?: sortedItemVariants.lastOrNull()?.url
                if (chosenURL != null) {
                    audioVariants.add(
                        AudioVariant(
                            id = "$i-$chosenURL",
                            title = audioVariantTitle(item, i),
                            url = chosenURL,
                            qualityVariants = sortedItemVariants
                        )
                    )
                }
            }

            qualityVariants.sortBy { qualityHeight(it.label) }
            val deduped = deduplicatedAudioVariants(audioVariants)
            val pickedURL = deduped.firstOrNull()?.url
                ?: masterURL
                ?: qualityVariants.lastOrNull()?.url
            pickedURL ?: continue

            val skips = extractSkips(obj)

            return AllohaResolvedStream(
                videoUrl = pickedURL,
                audioVariants = deduped,
                qualityVariants = qualityVariants.distinctBy { it.label },
                subtitles = subtitleTracks(payload, baseUrl),
                headers = headers,
                introRange = skips.first,
                outroRange = skips.second
            )
        }

        return null
    }

    private fun isMasterM3u8(url: String): Boolean {
        val path = url.substringAfterLast("/").substringBefore("?").lowercase(Locale.ROOT)
        return path.contains("master.m3u8")
    }

    private fun deduplicatedAudioVariants(variants: List<AudioVariant>): List<AudioVariant> {
        val seen = mutableSetOf<String>()
        return variants.filter { variant ->
            seen.add(variant.url)
        }
    }

    private fun audioVariantTitle(item: JSONObject, index: Int): String {
        firstAudioTitle(item)?.let { return it }
        return "Озвучка ${index + 1}"
    }

    private fun firstAudioTitle(value: Any?): String? {
        when (value) {
            is JSONObject -> {
                for (key in preferredAudioTitleKeys) {
                    stringTitle(value.opt(key))?.let { return it }
                }
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (isLikelyAudioTitleKey(key)) {
                        stringTitle(value.opt(key))?.let { return it }
                    }
                }
                for (key in preferredAudioContainerKeys) {
                    firstAudioTitle(value.opt(key))?.let { return it }
                }
                val allKeys = value.keys()
                while (allKeys.hasNext()) {
                    val key = allKeys.next()
                    if (!isIgnoredAudioTitleContainer(key)) {
                        firstAudioTitle(value.opt(key))?.let { return it }
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until value.length()) {
                    firstAudioTitle(value.opt(i))?.let { return it }
                }
            }
        }
        return null
    }

    private val preferredAudioTitleKeys = listOf(
        "translation", "translationName", "translation_name", "translator", "translatorName", "translator_name",
        "studio", "studioName", "studio_name", "voice", "voiceName", "voice_name", "voiceover", "dub",
        "dubbing", "name", "title", "label"
    )

    private val preferredAudioContainerKeys = listOf("translation", "translator", "voice", "voiceover", "dub", "dubbing", "studio", "data")

    private fun isLikelyAudioTitleKey(key: String): Boolean {
        val lower = key.lowercase(Locale.ROOT)
        return lower.contains("translation") ||
            lower.contains("translator") ||
            lower.contains("studio") ||
            lower.contains("voice") ||
            lower.contains("dub") ||
            lower == "name" ||
            lower == "title" ||
            lower == "label"
    }

    private fun isIgnoredAudioTitleContainer(key: String): Boolean {
        val lower = key.lowercase(Locale.ROOT)
        return lower == "quality" ||
            lower.contains("source") ||
            lower.contains("hls") ||
            lower.contains("url") ||
            lower.contains("file")
    }

    private fun stringTitle(value: Any?): String? {
        val raw = value as? String ?: return null
        val clean = decodeJavaScriptString(raw)
            .replace("_", " ")
            .trim()
        if (clean.isEmpty()) return null
        if (clean.contains(".m3u8", ignoreCase = true)) return null
        if (Regex("""^[a-zA-Z][a-zA-Z0-9+\-.]*://""").containsMatchIn(clean)) return null
        return clean
    }

    private fun qualityURLStrings(value: Any): List<String> {
        return when (value) {
            is String -> listOf(value)
            is JSONArray -> (0 until value.length()).flatMap { index ->
                value.opt(index)?.let { qualityURLStrings(it) } ?: emptyList()
            }
            is JSONObject -> {
                val preferredKeys = listOf("url", "file", "src", "hls", "master", "manifest", "link")
                val preferred = preferredKeys.flatMap { key ->
                    value.opt(key)?.let { qualityURLStrings(it) } ?: emptyList()
                }
                if (preferred.isNotEmpty()) preferred
                else {
                    val all = mutableListOf<String>()
                    val keys = value.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        value.opt(key)?.let { all += qualityURLStrings(it) }
                    }
                    all
                }
            }
            else -> emptyList()
        }
    }

    private fun splitAllohaURLList(rawValue: String): List<String> {
        return rawValue.split(Regex("""\s+or\s+""", RegexOption.IGNORE_CASE))
    }

    private fun decodeJavaScriptString(value: String): String {
        return value
            .replace("\\/", "/")
            .replace("\\u0026", "&")
            .replace("\\u003d", "=")
            .replace("\\u002f", "/")
            .replace("\\u003a", ":")
            .replace("\\u0025", "%")
            .replace("\\n", "")
            .replace("\\t", "")
            .replace("\\\\", "\\")
    }

    private fun makeURL(rawValue: String, baseUrl: URI): String? {
        val cleanValue = decodeJavaScriptString(rawValue)
            .replace("&amp;", "&")
            .replace("&#x2F;", "/")
            .replace("&#47;", "/")
            .trim()
            .trim('"', '\'')
        if (cleanValue.isEmpty()) return null
        return when {
            cleanValue.startsWith("//") -> "https:$cleanValue"
            cleanValue.startsWith("http://") || cleanValue.startsWith("https://") -> cleanValue
            else -> runCatching { baseUrl.resolve(cleanValue).toString() }.getOrNull()
        }
    }

    private fun isPlayable(url: String): Boolean {
        val path = url.lowercase(Locale.ROOT)
        if (path.contains("blank.mp4") || path.contains("cdn.plyr.io")) return false
        return path.contains(".m3u8") || path.contains(".mpd") || path.contains(".mp4")
    }

    private fun qualityHeight(label: String): Int {
        return label.lowercase(Locale.ROOT).replace("p", "").toIntOrNull() ?: 0
    }

    private fun normalizedQualityLabel(label: String): String {
        val clean = label.trim()
        return when {
            clean.isEmpty() -> "Поток"
            clean.lowercase(Locale.ROOT).endsWith("p") -> clean
            clean.toIntOrNull() != null -> "${clean}p"
            else -> clean
        }
    }

    private fun allohaURLs(raw: String, baseUrl: URI): List<String> {
        return splitAllohaURLList(raw)
            .map { it.trim() }
            .mapNotNull { makeURL(it, baseUrl) }
            .filter { isPlayable(it) }
    }

    private fun firstPreferredStreamURL(payload: String, baseUrl: URI): String? {
        val keys = listOf("hls", "dash", "mp4", "file", "url", "src", "stream", "manifest")
        for (key in keys) {
            val patterns = listOf(
                """\b$key\s*:\s*"([^"]+)"""",
                """\b$key\s*:\s*'([^']+)'""",
                """\"$key\"\s*:\s*"([^"]+)"""",
                """\"$key\"\s*:\s*'([^']+)'""",
                """\b$key\s*=\s*"([^"]+)"""",
                """\b$key\s*=\s*'([^']+)'"""
            )
            for (pattern in patterns) {
                val value = firstCapture(payload, pattern) ?: continue
                val url = makeURL(value, baseUrl) ?: continue
                if (isPlayable(url)) return url
            }
        }
        return firstURL(payload, listOf("m3u8", "mp4", "mpd"), baseUrl)
            ?: firstEscapedURL(payload, listOf("m3u8", "mp4", "mpd"), baseUrl)
    }

    private fun subtitleTracks(payload: String, baseUrl: URI): List<SubtitleTrack> {
        val tracks = mutableListOf<SubtitleTrack>()

        for (candidate in listOf(payload) + embeddedJSONObjectCandidates(payload)) {
            try {
                val obj = JSONObject(candidate)
                val tracksArray = obj.optJSONArray("tracks")
                if (tracksArray != null) {
                    for (i in 0 until tracksArray.length()) {
                        val track = tracksArray.optJSONObject(i) ?: continue
                        if (track.optString("kind") != "captions") continue
                        val url = track.optString("src", "")
                        val lang = track.optString("language", "und")
                        val name = track.optString("label", lang)
                        if (url.isNotBlank()) {
                            val fullUrl = if (url.startsWith("//")) "https:$url" else url
                            tracks += SubtitleTrack(label = name, language = lang, url = fullUrl)
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        if (tracks.isEmpty()) {
            val patterns = listOf(
                """\{\s*"url"\s*:\s*"([^"]+\.(?:vtt|srt)(?:\?[^"]*)?)"\s*,\s*"name"\s*:\s*"([^"]+)"""",
                """\{\s*url\s*:\s*"([^"]+\.(?:vtt|srt)(?:\?[^"]*)?)"\s*,\s*name\s*:\s*"([^"]+)"""",
                """\{\s*"name"\s*:\s*"([^"]+)"\s*,\s*"url"\s*:\s*"([^"]+\.(?:vtt|srt)(?:\?[^"]*)?)""""
            )
            patterns.forEach { pattern ->
                tracks += subtitleTracksByPattern(payload, pattern, baseUrl)
            }
        }

        if (tracks.isEmpty()) {
            firstURL(payload, listOf("vtt", "srt"), baseUrl)?.let { url ->
                tracks += SubtitleTrack(label = "Субтитры", language = "ru", url = url)
            }
        }
        val seen = mutableSetOf<String>()
        return tracks.filter { track -> seen.add(track.url) }
    }

    private fun subtitleTracksByPattern(text: String, pattern: String, baseUrl: URI): List<SubtitleTrack> {
        val regex = Regex(pattern, setOf(RegexOption.IGNORE_CASE))
        return regex.findAll(text).mapNotNull { match ->
            if (match.groupValues.size < 3) return@mapNotNull null
            val first = match.groupValues[1]
            val second = match.groupValues[2]
            val firstLower = first.lowercase(Locale.ROOT)
            val urlCandidate = if (firstLower.contains(".vtt") || firstLower.contains(".srt")) first else second
            val nameCandidate = if (urlCandidate == first) second else first
            val url = makeURL(urlCandidate, baseUrl) ?: return@mapNotNull null
            SubtitleTrack(label = decodeJavaScriptString(nameCandidate), language = "ru", url = url)
        }.toList()
    }

    private fun extractSkips(obj: JSONObject): Pair<SkipTimeRange?, SkipTimeRange?> {
        var intro: SkipTimeRange? = null
        var outro: SkipTimeRange? = null

        val skipTime = obj.optString("skipTime", "")
        if (skipTime.isNotBlank()) {
            val parts = skipTime.split(",")
            if (parts.isNotEmpty()) {
                val times = parts[0].split("-")
                if (times.size == 2) {
                    val s = times[0].toDoubleOrNull()
                    val e = times[1].toDoubleOrNull()
                    if (s != null && e != null) intro = SkipTimeRange(s, e)
                }
            }
            if (parts.size >= 2) {
                val times = parts[1].split("-")
                if (times.size == 2) {
                    val s = times[0].toDoubleOrNull()
                    val e = times[1].toDoubleOrNull()
                    if (s != null && e != null) outro = SkipTimeRange(s, e)
                }
            }
        }
        return Pair(intro, outro)
    }

    private fun firstCapture(text: String, pattern: String, captureGroup: Int = 1): String? {
        val regex = Regex(pattern, setOf(RegexOption.IGNORE_CASE))
        val match = regex.find(text) ?: return null
        if (match.groupValues.size <= captureGroup) return null
        return match.groupValues[captureGroup]
    }

    private fun firstURL(text: String, extensions: List<String>, baseUrl: URI): String? {
        val ext = extensions.joinToString("|") { Regex.escape(it) }
        val pattern = """https?:\/\/[^\s"'<>\\]+\.($ext)(?:\?[^\s"'<>\\]*)?"""
        val value = firstCapture(text, pattern, captureGroup = 0) ?: return null
        return makeURL(value, baseUrl)
    }

    private fun firstEscapedURL(text: String, extensions: List<String>, baseUrl: URI): String? {
        val ext = extensions.joinToString("|") { Regex.escape(it) }
        val pattern = """https?:\\\/\\\/[^\s"'<>]+\.($ext)(?:\?[^\s"'<>\\]*)?"""
        val value = firstCapture(text, pattern, captureGroup = 0) ?: return null
        return makeURL(value, baseUrl)
    }

    private fun embeddedJSONObjectCandidates(payload: String): List<String> {
        val candidates = mutableListOf<String>()
        candidates.addAll(balancedJSONObjectCandidates("\"hlsSource\"", payload))
        candidates.addAll(balancedJSONObjectCandidates("hlsSource", payload))
        val pattern = Regex("""\{[^{}]*"hlsSource"\s*:\s*\[[\s\S]*?]\s*(?:,[\s\S]*?)?\}""", RegexOption.IGNORE_CASE)
        candidates.addAll(pattern.findAll(payload).map { it.value })
        return candidates.distinct()
    }

    private fun balancedJSONObjectCandidates(marker: String, payload: String): List<String> {
        val candidates = mutableListOf<String>()
        var searchStart = 0
        while (true) {
            val markerIndex = payload.indexOf(marker, searchStart, ignoreCase = true)
            if (markerIndex < 0) break
            val objectStart = payload.lastIndexOf('{', markerIndex)
            if (objectStart < 0) {
                searchStart = markerIndex + marker.length
                continue
            }
            val objectEnd = balancedObjectEnd(objectStart, payload)
            if (objectEnd < 0) {
                searchStart = markerIndex + marker.length
                continue
            }
            candidates.add(payload.substring(objectStart, objectEnd + 1))
            searchStart = markerIndex + marker.length
        }
        return candidates
    }

    private fun balancedObjectEnd(start: Int, payload: String): Int {
        var depth = 0
        var quoted = false
        var escaped = false
        var i = start
        while (i < payload.length) {
            when {
                escaped -> escaped = false
                payload[i] == '\\' -> escaped = true
                payload[i] == '"' -> quoted = !quoted
                !quoted && payload[i] == '{' -> depth++
                !quoted && payload[i] == '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
            i++
        }
        return -1
    }
}
