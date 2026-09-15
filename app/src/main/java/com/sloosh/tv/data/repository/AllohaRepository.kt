package com.sloosh.tv.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.sloosh.tv.data.api.AllohaApiResult
import com.sloosh.tv.data.api.AllohaEpisode
import com.sloosh.tv.data.api.AllohaMovie
import com.sloosh.tv.data.api.AllohaSeason
import com.sloosh.tv.data.api.AllohaTranslation
import com.sloosh.tv.data.api.AllohaResolvedStream
import com.sloosh.tv.data.api.AudioVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.security.cert.X509Certificate
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Normalizes an Alloha translation name to a clean, human-readable string.
 * Mirrors normalizedAllohaTranslationName() in iOS AllohaRepository.swift.
 */
fun normalizedAllohaTranslationName(raw: String?): String {
    var value = raw?.trim() ?: return ""
    if (value.isEmpty()) return ""

    value = value
        .replace(Regex("""(?i)\b(?:AC3|E-AC3|EAC3|DDP|DD|DTS-HD|DTS|TrueHD|AAC|FLAC|MP3|PCM|LPCM)\s*(?:5[.]?1|7[.]?1|2[.]?0|51)?(?:\s*@\s*\d+\s*(?:kbps|kbit|кбит/с|кб/с)?)?"""), "")
        .replace(Regex("""(?i)@\s*\d+\s*(?:kbps|kbit|кбит/с|кб/с)?"""), "")
        .replace(Regex("""(?i)\b\d+\s*(?:kbps|kbit|кбит/с|кб/с)\b"""), "")
        .replace(Regex("""(?i)\b(?:Blu-ray(?:\s*CEE)?|BDRip|WEB-DL|HDTV|Line)\b"""), "")
        .replace(Regex("""(?i)\((?:Russian|Ukrainian|Kazakh|English|Uzbek|Turkish|Georgian|Japanese|Korean|Chinese)\)"""), "")
        .replace(Regex("""(?i)\bDUB\b"""), "Дубляж")
        .replace(Regex("""(?i)\bMVO\b"""), "Многоголосый")
        .replace(Regex("""(?i)\bDVO\b"""), "Двухголосый")
        .replace(Regex("""(?i)\bAVO\b"""), "Авторский")
        .replace(Regex("""(?i)\bПМ\b"""), "Проф. многоголосый")
        .replace(Regex("""(?i)\bПД\b"""), "Проф. двухголосый")
        .replace(Regex("""(?i)\bЛМ\b"""), "Люб. многоголосый")
        .replace(Regex("""(?i)\bЛД\b"""), "Люб. двухголосый")
        .replace("[", " ")
        .replace("]", " ")
        .replace("(", " ")
        .replace(")", " ")
        .replace("|", " ")
        .trim()

    while (value.startsWith("-") || value.startsWith(",") || value.startsWith("–") || value.startsWith("—")) {
        value = value.drop(1).trim()
    }
    while (value.endsWith("-") || value.endsWith(",") || value.endsWith("–") || value.endsWith("—")) {
        value = value.dropLast(1).trim()
    }

    value = value.replace(Regex("""\s+"""), " ").trim()
    return value
}

/**
 * Detects regional/original language tag (ukr, kaz, uzb, eng, geo, tur, jap, kor, chi, rus).
 * Mirrors detectLanguageTag() in iOS AllohaRepository.swift.
 */
fun detectLanguageTag(text: String): String? {
    val lower = text.lowercase(Locale.ROOT)
    if (lower.contains("украин") || lower.contains("ukrain") || lower.contains("укр") || lower.contains("ukr")) return "ukr"
    if (lower.contains("казах") || lower.contains("kazakh") || lower.contains("каз") || lower.contains("kaz")) return "kaz"
    if (lower.contains("узбек") || lower.contains("uzbek") || lower.contains("узб") || lower.contains("uzb")) return "uzb"
    if (lower.contains("оригинал") || lower.contains("original") || lower.contains("english") || lower.contains("английск") || lower.contains("eng") || lower == "en") return "eng"
    if (lower.contains("грузин") || lower.contains("georgian") || lower.contains("geo")) return "geo"
    if (lower.contains("турец") || lower.contains("turkish") || lower.contains("turk")) return "tur"
    if (lower.contains("япон") || lower.contains("japan") || lower.contains("jap")) return "jap"
    if (lower.contains("корей") || lower.contains("korean") || lower.contains("kor")) return "kor"
    if (lower.contains("китай") || lower.contains("chinese") || lower.contains("chi")) return "chi"
    if (lower.contains("русск") || lower.contains("rus")) return "rus"
    return null
}

/**
 * Identifies noise words in translation names that shouldn't be matched as unique studios.
 * Mirrors isTranslationNoiseWord() in iOS AllohaRepository.swift.
 */
fun isTranslationNoiseWord(word: String): Boolean {
    val noise = setOf(
        "studio", "студия", "дубляж", "дублированный", "дублирование", "полное",
        "многоголосый", "двухголосый", "одноголосый", "авторский", "закадровый", "озвучка",
        "профессиональный", "проф", "любительский", "люб", "production", "films", "film",
        "team", "voice", "line", "перевод", "голос", "звук", "чистый", "версия", "театральная",
        "расширенная", "режиссерская", "режиссёрская"
    )
    return noise.contains(word.lowercase(Locale.ROOT))
}

enum class TranslationVoiceType {
    DUB, MVO, DVO, AVO, SUB, UNKNOWN
}

fun detectVoiceType(text: String): TranslationVoiceType {
    val lower = text.lowercase(Locale.ROOT)
    return when {
        lower.contains("субтитр") || lower.contains("subtitle") || lower.contains("sub") -> TranslationVoiceType.SUB
        lower.contains("дубл") || lower.contains("dub") -> TranslationVoiceType.DUB
        lower.contains("многоголос") || lower.contains("mvo") || lower.contains("пм") || lower.contains("лм") -> TranslationVoiceType.MVO
        lower.contains("двухголос") || lower.contains("dvo") || lower.contains("пд") || lower.contains("лд") -> TranslationVoiceType.DVO
        lower.contains("одноголос") || lower.contains("авторск") || lower.contains("закадров") || lower.contains("avo") -> TranslationVoiceType.AVO
        else -> TranslationVoiceType.UNKNOWN
    }
}

/**
 * Checks whether two Alloha translation names refer to the same dubbing studio or author.
 * Strictly differentiates between Dubbing, Multi-voice, Two-voice, and Author voiceovers.
 */
fun allohaTranslationNamesMatch(lhs: String?, rhs: String?, exactOnly: Boolean = false): Boolean {
    val lhsRaw = lhs?.trim().orEmpty()
    val rhsRaw = rhs?.trim().orEmpty()
    if (lhsRaw.isEmpty() || rhsRaw.isEmpty()) return false

    val left = normalizedAllohaTranslationName(lhsRaw).lowercase(Locale.ROOT)
    val right = normalizedAllohaTranslationName(rhsRaw).lowercase(Locale.ROOT)

    if (left == right) return true

    val leftWordsSorted = left.split(Regex("""\s+""")).filter { it.isNotBlank() }.sorted().joinToString(" ")
    val rightWordsSorted = right.split(Regex("""\s+""")).filter { it.isNotBlank() }.sorted().joinToString(" ")
    if (leftWordsSorted.isNotEmpty() && leftWordsSorted == rightWordsSorted) return true

    // Strict language mismatch check on raw strings
    val langLeft = detectLanguageTag(lhsRaw)
    val langRight = detectLanguageTag(rhsRaw)

    if (langLeft != null && langLeft != "rus") {
        if (langRight != langLeft) return false
    }
    if (langRight != null && langRight != "rus") {
        if (langLeft != langRight) return false
    }
    if (langLeft != null && langRight != null && langLeft != langRight) {
        return false
    }
    if (langLeft != null && langRight != null && langLeft == langRight && langLeft != "rus") {
        return true
    }

    val isOriginalOrEnglish: (String) -> Boolean = { name ->
        val n = name.lowercase(Locale.ROOT)
        n.contains("original") || n.contains("оригинал") || n.contains("english") || n.contains("английский") || n.contains("eng") || n == "en"
    }
    if (isOriginalOrEnglish(left) && isOriginalOrEnglish(right)) return true

    // ─── STRICT VOICE TYPE RULES ─────────────────────────────────────────────
    // Дубляж и многоголосый — принципиально разные озвучки!
    val typeLeft = detectVoiceType(lhsRaw)
    val typeRight = detectVoiceType(rhsRaw)

    // Subtitles must only match subtitles
    if ((typeLeft == TranslationVoiceType.SUB) != (typeRight == TranslationVoiceType.SUB)) {
        return false
    }

    // Dubbing must NEVER match non-dubbing
    if ((typeLeft == TranslationVoiceType.DUB) != (typeRight == TranslationVoiceType.DUB)) {
        return false
    }

    // Known different voiceover types must not conflict (e.g. MVO vs DVO vs AVO)
    if (typeLeft != TranslationVoiceType.UNKNOWN && typeRight != TranslationVoiceType.UNKNOWN && typeLeft != typeRight) {
        return false
    }

    // Check for specific studio names
    val studios = listOf(
        "red head sound", "rhs", "flarrow", "lostfilm", "tvshows", "newstudio", "newcomers",
        "alexfilm", "кубик", "hdrezka", "rezka", "baibako", "jaskier", "vsi", "iron voice",
        "кураж бамбей", "лостфильм", "ньюстудио", "пифагор", "невафильм", "мост видео",
        "coldfilm", "колдфильм", "anilibria", "анилибрия", "anidub", "анидаб", "force media", "good people"
    )
    val leftStudios = studios.filter { left.contains(it) }
    val rightStudios = studios.filter { right.contains(it) }

    if (leftStudios.isNotEmpty() && rightStudios.isNotEmpty()) {
        val shared = leftStudios.toSet().intersect(rightStudios.toSet())
        if (shared.isEmpty()) return false
        // Shared studio with same non-unknown voice type is an exact match
        if (typeLeft == typeRight && typeLeft != TranslationVoiceType.UNKNOWN) {
            return true
        }
        if (exactOnly) return false
        // Shared studio must still respect voice type
        return typeLeft == typeRight || (typeLeft == TranslationVoiceType.UNKNOWN || typeRight == TranslationVoiceType.UNKNOWN)
    }

    if (exactOnly) return false

    val exclusiveStudios = listOf(
        "red head sound", "rhs", "flarrow", "lostfilm", "tvshows", "newstudio", "newcomers",
        "alexfilm", "кубик", "baibako", "jaskier", "vsi", "iron voice", "кураж бамбей"
    )
    val leftHasExclusive = exclusiveStudios.any { left.contains(it) }
    val rightHasExclusive = exclusiveStudios.any { right.contains(it) }
    if (leftHasExclusive != rightHasExclusive) return false

    if (typeLeft == TranslationVoiceType.DUB && typeRight == TranslationVoiceType.DUB) {
        if (langLeft != null && langRight != null) return langLeft == langRight
        return true
    }

    val noiseWords = setOf(
        "studio", "студия", "полное",
        "закадровый", "озвучка",
        "профессиональный", "проф", "любительский", "люб", "production", "films", "film",
        "team", "voice", "line", "перевод", "голос", "звук", "чистый", "версия", "театральная",
        "расширенная", "режиссерская", "режиссёрская"
    )
    val stripNoise: (String) -> String = { name ->
        var n = name.lowercase(Locale.ROOT)
        for (word in noiseWords) {
            n = n.replace(Regex("""(?i)\b${Regex.escape(word)}\b"""), "")
        }
        n.replace(Regex("[-–—,]"), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    val leftCore = stripNoise(left)
    val rightCore = stripNoise(right)
    if (leftCore.isNotEmpty() && leftCore == rightCore) {
        return typeLeft == typeRight || (typeLeft == TranslationVoiceType.UNKNOWN || typeRight == TranslationVoiceType.UNKNOWN)
    }

    val extractDistinctiveWords: (String) -> List<String> = { s ->
        s.lowercase(Locale.ROOT)
            .split(Regex("[^a-zA-Zа-яА-ЯёЁ0-9]+"))
            .filter { it.length >= 4 && !isTranslationNoiseWord(it) }
    }
    val rightWords = extractDistinctiveWords(right)
    val leftWords = extractDistinctiveWords(left)
    if (rightWords.isNotEmpty()) {
        val hasSharedDistinctive = rightWords.any { rw ->
            leftWords.any { lw -> lw == rw || lw.contains(rw) || rw.contains(lw) }
        }
        if (hasSharedDistinctive) {
            return typeLeft == typeRight || (typeLeft == TranslationVoiceType.UNKNOWN || typeRight == TranslationVoiceType.UNKNOWN)
        }
    }

    if (leftCore.length >= 4 && rightCore.length >= 4) {
        if (leftCore.contains(rightCore) || rightCore.contains(leftCore)) {
            return typeLeft == typeRight || (typeLeft == TranslationVoiceType.UNKNOWN || typeRight == TranslationVoiceType.UNKNOWN)
        }
    }

    return false
}

/**
 * Finds the best matching audio variant from audioVariants list for a target voiceover name.
 * Mirrors findMatchingAudioVariant() in iOS AllohaRepository.swift with strict voice-type checking.
 */
fun findMatchingAudioVariant(
    audioVariants: List<AudioVariant>,
    targetVoice: String?,
    isDedicatedIframe: Boolean = false
): AudioVariant? {
    if (audioVariants.isEmpty()) return null
    val target = targetVoice?.trim().orEmpty()
    if (target.isEmpty()) {
        return audioVariants.firstOrNull { it.url.isNotBlank() }
    }

    // 1. Exact match (exactOnly = true)
    audioVariants.firstOrNull { allohaTranslationNamesMatch(it.title, target, exactOnly = true) }?.let { return it }

    // 2. Loose studio / author match (exactOnly = false, but strictly respecting voice type)
    audioVariants.firstOrNull { allohaTranslationNamesMatch(it.title, target, exactOnly = false) }?.let { return it }

    // 3. Language tag match (for non-Russian regional languages)
    val targetLang = detectLanguageTag(target)
    if (targetLang != null && targetLang != "rus") {
        audioVariants.firstOrNull { detectLanguageTag(it.title) == targetLang }?.let { return it }
    }

    // 4. Distinctive author/studio words with matching voice type
    val targetType = detectVoiceType(target)
    val targetWords = target.lowercase(Locale.ROOT)
        .split(Regex("[^a-zA-Zа-яА-ЯёЁ0-9]+"))
        .filter { it.length >= 4 && !isTranslationNoiseWord(it) }
    if (targetWords.isNotEmpty()) {
        for (word in targetWords) {
            val match = audioVariants.firstOrNull {
                val variantType = detectVoiceType(it.title)
                val typeCompatible = targetType == TranslationVoiceType.UNKNOWN ||
                        variantType == targetType ||
                        (targetType != TranslationVoiceType.DUB && variantType != TranslationVoiceType.DUB)
                it.title.lowercase(Locale.ROOT).contains(word) && typeCompatible
            }
            if (match != null) return match
        }
    }

    // 5. Dedicated iframe fallback (if target is not dub, prefer non-dub variant 1)
    if (isDedicatedIframe && audioVariants.size > 1) {
        val isTargetDub = targetType == TranslationVoiceType.DUB
        if (!isTargetDub) {
            val nonDub = audioVariants.firstOrNull {
                detectVoiceType(it.title) != TranslationVoiceType.DUB
            }
            if (nonDub != null) return nonDub
            return audioVariants.getOrNull(1)
        }
    }

    return null
}

/**
 * Injects a ?translation=<id> query parameter into an iframe URL, safely replacing any existing translation parameter.
 */
fun injectTranslationId(id: String, urlString: String): String {
    return try {
        val clean = urlString.replace(Regex("[?&]translation=[^&]*"), "")
        val sep = if (clean.contains("?")) "&" else "?"
        "$clean${sep}translation=$id"
    } catch (_: Exception) {
        urlString
    }
}

/**
 * Injects ?season=<season>&episode=<episode> query parameters into an iframe URL, safely replacing any existing ones.
 */
fun injectSeasonEpisode(season: Int, episode: Int, urlString: String): String {
    return try {
        val clean = urlString
            .replace(Regex("[?&]season=[^&]*"), "")
            .replace(Regex("[?&]episode=[^&]*"), "")
        val sep = if (clean.contains("?")) "&" else "?"
        "$clean${sep}season=$season&episode=$episode"
    } catch (_: Exception) {
        urlString
    }
}

class AllohaRepository(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("alloha_prefs", Context.MODE_PRIVATE)
    }

    // 5-minute in-memory cache keyed by identifier string
    private val cache = ConcurrentHashMap<String, Pair<AllohaApiResult, Long>>()
    private val cacheTtlMs = 5 * 60 * 1000L

    private val client: OkHttpClient by lazy {
        getUnsafeOkHttpClient()
    }

    private fun getUnsafeOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        return OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val token = "ffbd312217e27c4245f2678afe1881"

    private val userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) " +
        "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1"

    fun invalidateCache() {
        cache.clear()
    }

    suspend fun fetchAllohaData(
        mediaId: String,
        explicitKpId: Int? = null,
        imdbId: String? = null,
        tmdbId: Int? = null,
        title: String? = null
    ): AllohaApiResult? = withContext(Dispatchers.IO) {
        val kpId = explicitKpId ?: mediaId.replace("kp_", "").trim().toIntOrNull()
        val validKp = (kpId ?: 0).takeIf { it > 0 }
        val validTmdb = (tmdbId ?: 0).takeIf { it > 0 }

        // 1. Try KP ID first if positive
        if (validKp != null) {
            val res = performAllohaQuery("kp", validKp.toString())
            if (res != null) return@withContext res
        }

        // 2. Fallback to TMDB ID if available
        if (validTmdb != null) {
            val res = performAllohaQuery("tmdb", validTmdb.toString())
            if (res != null) return@withContext res
        }

        // 3. Fallback to IMDB ID if available
        if (!imdbId.isNullOrBlank()) {
            val res = performAllohaQuery("imdb", imdbId)
            if (res != null) return@withContext res
        }

        // 4. Fallback to title search if available
        if (!title.isNullOrBlank() && !title.startsWith("Без названия")) {
            val res = performAllohaQuery("name", title)
            if (res != null) return@withContext res
        }

        null
    }

    private suspend fun performAllohaQuery(param: String, value: String): AllohaApiResult? {
        val queryParam = "$param=$value"
        val cached = cache[queryParam]
        if (cached != null && System.currentTimeMillis() - cached.second < cacheTtlMs) {
            Log.d("AllohaRepository", "Returning cached Alloha result for $queryParam")
            return cached.first
        }

        return try {
            val encodedVal = URLEncoder.encode(value, "UTF-8")
            val url = "https://api.alloha.tv/?token=$token&$param=$encodedVal"
            Log.d("AllohaRepository", "Fetching Alloha catalog: $url")
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", userAgent)
                .build()

            val response = client.newCall(request).execute()
            Log.d("AllohaRepository", "Alloha API HTTP response: ${response.code} for $queryParam")
            if (!response.isSuccessful) {
                Log.w("AllohaRepository", "Alloha request unsuccessful: ${response.code} ${response.message}")
                return null
            }

            val body = response.body?.string() ?: run {
                Log.w("AllohaRepository", "Alloha response body is empty")
                return null
            }
            val json = JSONObject(body)
            val data = json.optJSONObject("data") ?: run {
                Log.w("AllohaRepository", "Alloha response has no 'data' object")
                return null
            }

            val title = data.optString("name", "Фильм")
            val result = parseAllohaData(title, data)

            if (result != null) {
                cache[queryParam] = Pair(result, System.currentTimeMillis())
                Log.d("AllohaRepository", "Successfully parsed Alloha data: title=${result.title}, isSerial=${result.isSerial}, seasons=${result.seasons.size}, movieTranslations=${result.movie?.translations?.size}")
            } else {
                Log.w("AllohaRepository", "Failed to parse valid stream sources from Alloha data for $queryParam")
            }
            result
        } catch (e: Exception) {
            Log.e("AllohaRepository", "Exception fetching Alloha catalog for $queryParam", e)
            null
        }
    }

    private suspend fun parseAllohaData(title: String, data: JSONObject): AllohaApiResult? {
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
                    // Keep the natural Alloha translation order (popular first, matching iOS)
                    val parsedTrans = parseTranslations(eDict).map { t ->
                        t.copy(iframeUrl = injectSeasonEpisode(seasonNum, episodeNum, t.iframeUrl))
                    }
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
            if (sortedSeasons.isEmpty()) {
                Log.w("AllohaRepository", "Serial seasons parsed to empty list")
                return null
            }
            return AllohaApiResult(title = title, isSerial = true, movie = null, seasons = sortedSeasons)
        }

        // ─── Movie: parse translations ────────────────────────────
        val parsedTrans = parseTranslations(data).toMutableList()
        var defaultIframe = data.optString("iframe", "").let {
            if (it.startsWith("//")) "https:$it" else it
        }
        if (defaultIframe.isEmpty()) defaultIframe = parsedTrans.firstOrNull()?.iframeUrl.orEmpty()

        // 3. If a single master-iframe contains hidden audioVariants (step 3 in iOS AllohaRepository)
        if (parsedTrans.size <= 1 && defaultIframe.isNotBlank()) {
            try {
                val resolved = resolver.resolve(defaultIframe)
                if (resolved.audioVariants.size > 1) {
                    val dynamicTrans = mutableListOf<AllohaTranslation>()
                    for ((idx, variant) in resolved.audioVariants.withIndex()) {
                        val vTitle = variant.title.ifBlank { "Озвучка ${idx + 1}" }
                        val vUrl = variant.url.trim()
                        val cleanTitle = normalizedAllohaTranslationName(vTitle)
                        val lower = cleanTitle.lowercase(Locale.ROOT)
                        if (!lower.contains("субтитр") && !lower.contains("subtitle")) {
                            dynamicTrans.add(
                                AllohaTranslation(
                                    id = idx.toString(),
                                    name = cleanTitle,
                                    iframeUrl = defaultIframe,
                                    streamUrl = vUrl.ifBlank { null }
                                )
                            )
                        }
                    }
                    if (dynamicTrans.isNotEmpty()) {
                        parsedTrans.clear()
                        parsedTrans.addAll(dynamicTrans)
                    }
                }
            } catch (e: Exception) {
                Log.d("AllohaRepository", "Could not pre-resolve single movie iframe for extra audio variants: ${e.message}")
            }
        }

        // 4. Final fallback
        if (parsedTrans.isNotEmpty()) {
            val movieIframe = if (defaultIframe.isNotBlank()) defaultIframe else parsedTrans.first().iframeUrl
            val movie = AllohaMovie(title = title, iframeUrl = movieIframe, translations = parsedTrans)
            return AllohaApiResult(title = title, isSerial = false, movie = movie, seasons = emptyList())
        } else if (defaultIframe.isNotBlank()) {
            val fallbackTrans = AllohaTranslation(id = "default", name = "Основной", iframeUrl = defaultIframe, streamUrl = null)
            val movie = AllohaMovie(title = title, iframeUrl = defaultIframe, translations = listOf(fallbackTrans))
            return AllohaApiResult(title = title, isSerial = false, movie = movie, seasons = emptyList())
        }

        return null
    }

    /**
     * Parses translation variants from a JSON object.
     * Mirrors iOS parseTranslations logic preserving Alloha order.
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

            val lower = cleanName.lowercase(Locale.ROOT)
            if (lower.contains("субтитр") || lower.contains("subtitle")) {
                return // filter out subtitle-only streams
            }

            result.add(AllohaTranslation(id = id, name = cleanName, iframeUrl = url, streamUrl = null))
        }

        fun processObject(obj: JSONObject) {
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
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
                    val id = item.optString("id").takeIf { it.isNotBlank() }
                        ?: item.optString("translation_id").takeIf { it.isNotBlank() }
                        ?: i.toString()
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

    private val resolver by lazy { AllohaRuntimeResolver(context) }

    suspend fun resolveStream(iframeUrl: String): AllohaResolvedStream {
        return resolver.resolve(iframeUrl)
    }
}
