package com.sloosh.tv.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.sloosh.tv.data.api.AllohaApiResult
import com.sloosh.tv.data.api.AllohaEpisode
import com.sloosh.tv.data.api.AllohaMovie
import com.sloosh.tv.data.api.AllohaSeason
import com.sloosh.tv.data.api.AllohaTranslation
import com.sloosh.tv.data.api.AllohaResolvedStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * Normalizes an Alloha translation name to a clean, human-readable string.
 * Mirrors normalizedAllohaTranslationName() in iOS AllohaRepository.swift.
 */
fun normalizedAllohaTranslationName(raw: String?): String {
    var value = raw?.trim() ?: return ""
    if (value.isEmpty()) return ""

    value = value
        .replace("(Russian)", "")
        .replace("AC3 51 @ 640 kbps - Blu-ray CEE", "")
        .replace("AC3 5.1 @ 640 kbps", "")
        .replace("DUB", "Дубляж")
        .replace("MVO", "Многоголосый")
        .replace("DVO", "Двухголосый")
        .replace("AVO", "Авторский")
        .replace("ПМ", "Проф. многоголосый")
        .replace("ПД", "Проф. двухголосый")
        .replace("ЛМ", "Люб. многоголосый")
        .replace("ЛД", "Люб. двухголосый")
        .replace("[", " ")
        .replace("]", " ")
        .replace("(", " ")
        .replace(")", " ")
        .replace("|", " ")
        .trim()

    while (value.startsWith("-") || value.startsWith(",")) {
        value = value.drop(1).trim()
    }
    while (value.endsWith("-") || value.endsWith(",")) {
        value = value.dropLast(1).trim()
    }

    value = value.replace(Regex("\\s+"), " ").trim()
    return value
}

/**
 * Checks whether two Alloha translation names refer to the same dubbing studio.
 * Mirrors allohaTranslationNamesMatch() in iOS AllohaRepository.swift.
 */
fun allohaTranslationNamesMatch(lhs: String?, rhs: String?, exactOnly: Boolean = false): Boolean {
    val left = normalizedAllohaTranslationName(lhs).lowercase()
    val right = normalizedAllohaTranslationName(rhs).lowercase()
    if (left.isEmpty() || right.isEmpty()) return false
    if (left == right) return true

    val leftWords = left.split(Regex("[^a-zA-Zа-яА-ЯёЁ0-9]+")).filter { it.length > 2 }.toSet()
    val rightWords = right.split(Regex("[^a-zA-Zа-яА-ЯёЁ0-9]+")).filter { it.length > 2 }.toSet()

    if (leftWords.isNotEmpty() && rightWords.isNotEmpty()) {
        if (leftWords == rightWords || leftWords.containsAll(rightWords) || rightWords.containsAll(leftWords)) {
            return true
        }
    } else if (left.contains(right) || right.contains(left)) {
        return true
    }

    val isOriginalOrEnglish: (String) -> Boolean = { name ->
        name.contains("original") || name.contains("оригинал") ||
        name.contains("english") || name.contains("английский") ||
        name.contains("eng") || name == "en"
    }
    if (isOriginalOrEnglish(left) && isOriginalOrEnglish(right)) return true

    return false
}

/**
 * Injects a ?translation=<id> query parameter into an iframe URL.
 */
fun injectTranslationId(id: String, urlString: String): String {
    return try {
        val sep = if (urlString.contains("?")) "&" else "?"
        "$urlString${sep}translation=$id"
    } catch (e: Exception) {
        urlString
    }
}

class AllohaRepository(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("alloha_prefs", Context.MODE_PRIVATE)
    }

    // 5-minute in-memory cache
    private val cache = mutableMapOf<Int, Pair<AllohaApiResult, Long>>()
    private val cacheTtlMs = 5 * 60 * 1000L

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val token = "ffbd312217e27c4245f2678afe1881"

    private val userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) " +
        "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1"

    suspend fun fetchAllohaData(mediaId: String, explicitKpId: Int? = null): AllohaApiResult? = withContext(Dispatchers.IO) {
        val kpId = explicitKpId ?: mediaId.replace("kp_", "").trim().toIntOrNull() ?: return@withContext null

        // Check cache
        val cached = cache[kpId]
        if (cached != null && System.currentTimeMillis() - cached.second < cacheTtlMs) {
            return@withContext cached.first
        }

        try {
            val url = "https://api.alloha.tv/?token=$token&kp=$kpId"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", userAgent)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val data = json.optJSONObject("data") ?: return@withContext null

            val title = data.optString("name", "Фильм")
            val result = parseAllohaData(title, data)

            if (result != null) {
                cache[kpId] = Pair(result, System.currentTimeMillis())
            }
            result
        } catch (e: Exception) {
            null
        }
    }

    private fun parseAllohaData(title: String, data: JSONObject): AllohaApiResult? {
        // ─── Serial: has "seasons" object ────────────────────────
        val seasonsObj = data.optJSONObject("seasons")
        if (seasonsObj != null) {
            val parsedSeasons = mutableListOf<AllohaSeason>()

            for (sKey in seasonsObj.keys()) {
                val seasonNum = sKey.toIntOrNull() ?: continue
                val sDict = seasonsObj.optJSONObject(sKey) ?: continue
                val episodesObj = sDict.optJSONObject("episodes") ?: continue

                val parsedEpisodes = mutableListOf<AllohaEpisode>()
                for (eKey in episodesObj.keys()) {
                    val episodeNum = eKey.toIntOrNull() ?: continue
                    val eDict = episodesObj.optJSONObject(eKey) ?: continue
                    val parsedTrans = parseTranslations(eDict).sortedBy { it.name }
                    if (parsedTrans.isNotEmpty()) {
                        parsedEpisodes.add(AllohaEpisode(season = seasonNum, episode = episodeNum, translations = parsedTrans))
                    }
                }

                parsedEpisodes.sortedBy { it.episode }.also { sorted ->
                    if (sorted.isNotEmpty()) {
                        parsedSeasons.add(AllohaSeason(season = seasonNum, episodes = sorted))
                    }
                }
            }

            val sortedSeasons = parsedSeasons.sortedBy { it.season }
            return AllohaApiResult(title = title, isSerial = true, movie = null, seasons = sortedSeasons)
        }

        // ─── Movie: parse translations ────────────────────────────
        val parsedTrans = parseTranslations(data)
        return when {
            parsedTrans.isNotEmpty() -> {
                var iframe = data.optString("iframe", "").let {
                    if (it.startsWith("//")) "https:$it" else it
                }
                if (iframe.isEmpty()) iframe = parsedTrans.first().iframeUrl
                val movie = AllohaMovie(title = title, iframeUrl = iframe, translations = parsedTrans.sortedBy { it.name })
                AllohaApiResult(title = title, isSerial = false, movie = movie, seasons = emptyList())
            }
            else -> {
                // Last-resort fallback: bare iframe field
                var iframe = data.optString("iframe", "")
                if (iframe.startsWith("//")) iframe = "https:$iframe"
                if (iframe.isEmpty()) return null
                val fallbackTrans = AllohaTranslation(id = "default", name = "Основной", iframeUrl = iframe, streamUrl = null)
                val movie = AllohaMovie(title = title, iframeUrl = iframe, translations = listOf(fallbackTrans))
                AllohaApiResult(title = title, isSerial = false, movie = movie, seasons = emptyList())
            }
        }
    }

    /**
     * Parses translation variants from a JSON object (either root `data` for movies,
     * or an episode dict for series).
     *
     * Alloha API can provide translations in several keys:
     *   1. "translation_iframe" -> { "id1": { "name" / "translation": "...", "iframe": "..." }, ... } or [ ... ]
     *   2. "translation" -> { "id1": { "name" / "translation": "...", "iframe": "..." }, ... } or [ ... ] or String
     *   3. "translations" -> [ ... ] or { ... }
     */
    private fun parseTranslations(dict: JSONObject): List<AllohaTranslation> {
        val result = mutableListOf<AllohaTranslation>()

        fun extractName(tDict: JSONObject): String {
            val raw = tDict.optString("name").takeIf { it.isNotBlank() }
                ?: tDict.optString("translation").takeIf { it.isNotBlank() }
                ?: tDict.optString("translation_name").takeIf { it.isNotBlank() }
                ?: tDict.optString("title").takeIf { it.isNotBlank() }
                ?: tDict.optString("voice").takeIf { it.isNotBlank() }
                ?: ""
            return normalizedAllohaTranslationName(raw)
        }

        fun extractIframe(tDict: JSONObject): String {
            var iframe = tDict.optString("iframe").takeIf { it.isNotBlank() }
                ?: tDict.optString("url").takeIf { it.isNotBlank() }
                ?: tDict.optString("link").takeIf { it.isNotBlank() }
                ?: ""
            if (iframe.startsWith("//")) iframe = "https:$iframe"
            return iframe
        }

        fun addTranslation(id: String, rawName: String, iframe: String) {
            var url = iframe
            if (url.startsWith("//")) url = "https:$url"
            if (url.isBlank()) return
            url = injectTranslationId(id, url)

            var cleanName = normalizedAllohaTranslationName(rawName)
            if (cleanName.isBlank()) cleanName = "Озвучка ${result.size + 1}"

            val lower = cleanName.lowercase()
            if (lower.contains("субтитр") || lower.contains("subtitle")) {
                return // filter out subtitle-only streams
            }

            result.add(AllohaTranslation(id = id, name = cleanName, iframeUrl = url, streamUrl = null))
        }

        fun processObject(obj: JSONObject) {
            for (key in obj.keys()) {
                val item = obj.opt(key)
                if (item is JSONObject) {
                    val iframe = extractIframe(item)
                    val name = extractName(item)
                    if (iframe.isNotBlank()) {
                        addTranslation(key, name, iframe)
                    }
                } else if (item is String && item.isNotBlank()) {
                    if (item.contains("http") || item.startsWith("//")) {
                        addTranslation(key, key, item)
                    }
                }
            }
        }

        fun processArray(arr: JSONArray) {
            for (i in 0 until arr.length()) {
                val item = arr.opt(i)
                if (item is JSONObject) {
                    val iframe = extractIframe(item)
                    val name = extractName(item)
                    val id = item.optString("id").takeIf { it.isNotBlank() } ?: i.toString()
                    if (iframe.isNotBlank()) {
                        addTranslation(id, name, iframe)
                    }
                }
            }
        }

        // 1. Check translation_iframe (standard Alloha movie catalog format)
        val transIframe = dict.opt("translation_iframe")
        if (transIframe is JSONObject) processObject(transIframe)
        else if (transIframe is JSONArray) processArray(transIframe)

        // 2. Check translation
        if (result.isEmpty()) {
            val transRaw = dict.opt("translation")
            when {
                transRaw is JSONObject -> processObject(transRaw)
                transRaw is JSONArray -> processArray(transRaw)
                transRaw is String && transRaw.isNotBlank() -> {
                    var iframe = dict.optString("iframe", "")
                    if (iframe.startsWith("//")) iframe = "https:$iframe"
                    if (iframe.isNotBlank()) {
                        addTranslation("default", transRaw, iframe)
                    }
                }
            }
        }

        // 3. Check translations
        if (result.isEmpty()) {
            val translations = dict.opt("translations")
            if (translations is JSONObject) processObject(translations)
            else if (translations is JSONArray) processArray(translations)
        }

        return result
    }

    // ─── Playback preference helpers ─────────────────────────────────────────

    fun saveLastTranslation(name: String) {
        prefs.edit().putString("alloha_last_translation_name", name).apply()
    }

    fun getLastTranslation(): String? = prefs.getString("alloha_last_translation_name", null)

    fun saveLastVoiceover(kpId: Int, voiceover: String) {
        prefs.edit().putString("alloha_voiceover_$kpId", voiceover).apply()
    }

    fun getLastVoiceover(kpId: Int): String? = prefs.getString("alloha_voiceover_$kpId", null)

    fun saveLastPlayed(kpId: Int, season: Int?, episode: Int?) {
        prefs.edit()
            .putInt("alloha_last_season_$kpId", season ?: -1)
            .putInt("alloha_last_episode_$kpId", episode ?: -1)
            .apply()
    }

    fun getLastSeason(kpId: Int): Int? = prefs.getInt("alloha_last_season_$kpId", -1).takeIf { it != -1 }
    fun getLastEpisode(kpId: Int): Int? = prefs.getInt("alloha_last_episode_$kpId", -1).takeIf { it != -1 }

    // ─── Stream resolution ───────────────────────────────────────────────────

    private val resolver = AllohaRuntimeResolver(context)

    suspend fun resolveStream(iframeUrl: String): AllohaResolvedStream {
        return resolver.resolve(iframeUrl)
    }
}
