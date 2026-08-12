package com.sloosh.tv.data.repository

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sloosh.tv.data.api.*

object AllohaRuntimeParser {

    fun parsePayload(payload: String, baseUrl: String, headers: Map<String, String>): AllohaResolvedStream? {
        val rootObject = try {
            JsonParser.parseString(payload).asJsonObject
        } catch (e: Exception) {
            null
        }

        if (rootObject != null) {
            val parsed = parseAllohaSource(rootObject, baseUrl, headers)
            if (parsed != null) return parsed
        }

        // Try extracting nested hlsSource payload
        val nestedPayload = extractNestedHlsSourcePayload(payload)
        if (nestedPayload != null) {
            val nestedObj = try { JsonParser.parseString(nestedPayload).asJsonObject } catch (e: Exception) { null }
            if (nestedObj != null) {
                val parsed = parseAllohaSource(nestedObj, baseUrl, headers)
                if (parsed != null) return parsed
            }
        }

        return null
    }

    private fun extractNestedHlsSourcePayload(payload: String): String? {
        val keys = listOf("data", "result", "payload", "response", "content", "body")
        try {
            val obj = JsonParser.parseString(payload).asJsonObject
            for (key in keys) {
                if (obj.has(key) && obj.get(key).isJsonObject) {
                    val nested = obj.getAsJsonObject(key)
                    if (nested.has("hlsSource")) {
                        return nested.toString()
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return null
    }

    private fun parseAllohaSource(objectObj: JsonObject, baseUrl: String, headers: Map<String, String>): AllohaResolvedStream? {
        if (!objectObj.has("hlsSource") || !objectObj.get("hlsSource").isJsonArray) return null
        val sourceArray = objectObj.getAsJsonArray("hlsSource")

        val qualityVariants = mutableListOf<QualityVariant>()
        val audioVariants = mutableListOf<AudioVariant>()
        var masterUrl: String? = null
        var adaptiveUrl: String? = null

        for (index in 0 until sourceArray.size()) {
            val item = sourceArray.get(index).asJsonObject
            if (!item.has("quality") || !item.get("quality").isJsonObject) continue
            val qualityObj = item.getAsJsonObject("quality")

            val itemVariants = mutableListOf<QualityVariant>()
            var itemMasterUrl: String? = null

            for (label in qualityObj.keySet()) {
                val valElement = qualityObj.get(label)
                val rawUrls = mutableListOf<String>()
                if (valElement.isJsonArray) {
                    valElement.asJsonArray.forEach { rawUrls.add(it.asString) }
                } else if (valElement.isJsonPrimitive) {
                    rawUrls.add(valElement.asString)
                }

                for (rawUrl in rawUrls) {
                    val fullUrl = resolveAllohaUrl(rawUrl, baseUrl)
                    if (masterUrl == null && fullUrl.lowercase().contains("master.m3u8")) {
                        masterUrl = fullUrl
                    }
                    if (itemMasterUrl == null && fullUrl.lowercase().contains("master.m3u8")) {
                        itemMasterUrl = fullUrl
                    }
                    if (adaptiveUrl == null && fullUrl.lowercase().contains("index.m3u8")) {
                        adaptiveUrl = fullUrl
                    }

                    val normLabel = normalizeQualityLabel(label)
                    val qVariant = QualityVariant(label = normLabel, url = fullUrl)
                    itemVariants.add(qVariant)
                    qualityVariants.add(qVariant)
                }
            }

            val chosenAudioUrl = itemMasterUrl ?: adaptiveUrl ?: itemVariants.lastOrNull()?.url
            if (chosenAudioUrl != null) {
                val title = getAudioTitle(item, index)
                audioVariants.add(
                    AudioVariant(
                        id = "$index-$chosenAudioUrl",
                        title = title,
                        url = chosenAudioUrl,
                        qualityVariants = itemVariants
                    )
                )
            }
        }

        val pickedUrl = masterUrl ?: adaptiveUrl ?: audioVariants.firstOrNull()?.url ?: qualityVariants.lastOrNull()?.url ?: return null
        val skips = extractSkips(objectObj)

        return AllohaResolvedStream(
            videoUrl = pickedUrl,
            audioVariants = audioVariants,
            qualityVariants = qualityVariants,
            subtitles = extractSubtitles(objectObj, baseUrl),
            headers = headers,
            introRange = skips.first,
            outroRange = skips.second
        )
    }

    private fun resolveAllohaUrl(rawUrl: String, baseUrl: String): String {
        if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) return rawUrl
        if (rawUrl.startsWith("//")) return "https:$rawUrl"
        val domain = if (baseUrl.startsWith("http")) {
            baseUrl.substringBefore("/", baseUrl)
        } else {
            "https://alloha.tv"
        }
        return if (rawUrl.startsWith("/")) "$domain$rawUrl" else "$domain/$rawUrl"
    }

    private fun normalizeQualityLabel(rawLabel: String): String {
        return when {
            rawLabel.contains("1080") -> "1080p"
            rawLabel.contains("720") -> "720p"
            rawLabel.contains("480") -> "480p"
            rawLabel.contains("360") -> "360p"
            else -> rawLabel
        }
    }

    private fun getAudioTitle(item: JsonObject, index: Int): String {
        if (item.has("title") && !item.get("title").isJsonNull) {
            return item.get("title").asString
        }
        if (item.has("name") && !item.get("name").isJsonNull) {
            return item.get("name").asString
        }
        if (item.has("translation") && !item.get("translation").isJsonNull) {
            return item.get("translation").asString
        }
        return "Озвучка #${index + 1}"
    }

    private fun extractSubtitles(obj: JsonObject, baseUrl: String): List<SubtitleTrack> {
        val tracks = mutableListOf<SubtitleTrack>()
        if (obj.has("subtitles") && obj.get("subtitles").isJsonArray) {
            val subsArray = obj.getAsJsonArray("subtitles")
            for (elem in subsArray) {
                if (elem.isJsonObject) {
                    val subObj = elem.asJsonObject
                    val label = subObj.get("label")?.asString ?: "Субтитры"
                    val lang = subObj.get("language")?.asString ?: "ru"
                    val url = subObj.get("url")?.asString ?: continue
                    tracks.add(SubtitleTrack(label = label, language = lang, url = resolveAllohaUrl(url, baseUrl)))
                }
            }
        }
        return tracks
    }

    private fun extractSkips(obj: JsonObject): Pair<SkipTimeRange?, SkipTimeRange?> {
        var intro: SkipTimeRange? = null
        var outro: SkipTimeRange? = null

        if (obj.has("skipTime") && obj.get("skipTime").isJsonPrimitive) {
            val skipTime = obj.get("skipTime").asString
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
}
