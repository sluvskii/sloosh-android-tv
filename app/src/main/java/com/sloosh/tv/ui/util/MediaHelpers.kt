package com.sloosh.tv.ui.util

/**
 * Cleans and prettifies technical voiceover/translation names ported 1:1 from iOS MediaHelpers.swift.
 * Strips release tags (e.g. 2160p, WEB-DL, BDRip, x265) and prepends flag emojis for language identifiers.
 */
fun cleanTranslationName(rawName: String): String {
    var name = rawName.trim()
    if (name.isEmpty()) return "По умолчанию"

    // 1. Remove bulky scene / release tags
    val sceneTagRegex = Regex("(?i)[a-z0-9._-]{3,}(?:2160p|1080p|720p|480p|internal|web-dl|web-dlrip|bdrip|bluray|hdr10|hdr|dv|hevc|x264|x265|spacehd\\d*)[a-z0-9._-]*")
    name = sceneTagRegex.replace(name, "").trim()

    // 2. Normalize brackets and pipes into clean spaces
    name = name
        .replace("(", " ")
        .replace(")", " ")
        .replace("[", " ")
        .replace("]", " ")
        .replace("|", " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    if (name.isEmpty()) {
        name = rawName.split(" ").firstOrNull() ?: rawName
    }

    // 3. Language mapping with country flag emoji
    val languageMappings = listOf(
        Pair("Russian", Pair("🇷🇺", "Русский")),
        Pair("Русский", Pair("🇷🇺", "Русский")),
        Pair("English", Pair("🇺🇸", "Английский")),
        Pair("Английский", Pair("🇺🇸", "Английский")),
        Pair("Ukrainian", Pair("🇺🇦", "Украинский")),
        Pair("Украинский", Pair("🇺🇦", "Украинский")),
        Pair("Kazakh", Pair("🇰🇿", "Казахский")),
        Pair("Казахский", Pair("🇰🇿", "Казахский")),
        Pair("Georgian", Pair("🇬🇪", "Грузинский")),
        Pair("Грузинский", Pair("🇬🇪", "Грузинский")),
        Pair("Spanish", Pair("🇪🇸", "Испанский")),
        Pair("Испанский", Pair("🇪🇸", "Испанский")),
        Pair("German", Pair("🇩🇪", "Немецкий")),
        Pair("Немецкий", Pair("🇩🇪", "Немецкий")),
        Pair("French", Pair("🇫🇷", "Французский")),
        Pair("Французский", Pair("🇫🇷", "Французский")),
        Pair("Italian", Pair("🇮🇹", "Итальянский")),
        Pair("Итальянский", Pair("🇮🇹", "Итальянский")),
        Pair("Japanese", Pair("🇯🇵", "Японский")),
        Pair("Японский", Pair("🇯🇵", "Японский")),
        Pair("Korean", Pair("🇰🇷", "Корейский")),
        Pair("Корейский", Pair("🇰🇷", "Корейский")),
        Pair("Chinese", Pair("🇨🇳", "Китайский")),
        Pair("Китайский", Pair("🇨🇳", "Китайский"))
    )

    var baseTitle = name
    for ((lang, pair) in languageMappings) {
        val (flag, defaultName) = pair
        if (name.startsWith(lang, ignoreCase = true)) {
            val remainder = name.substring(lang.length).trim()
            baseTitle = if (remainder.isEmpty()) {
                "$flag $defaultName"
            } else {
                "$flag $remainder"
            }
            break
        }
    }

    return baseTitle
}

/**
 * Returns a unique, clean display label for a translation accounting for duplicates in the list.
 */
fun displayTranslationName(rawName: String, indexInAll: Int, allRawNames: List<String>): String {
    val cleaned = cleanTranslationName(rawName)
    val totalDuplicates = allRawNames.count { cleanTranslationName(it) == cleaned }

    if (totalDuplicates > 1) {
        var occurrence = 0
        val maxIdx = indexInAll.coerceIn(0, allRawNames.size - 1)
        for (i in 0..maxIdx) {
            if (cleanTranslationName(allRawNames[i]) == cleaned) {
                occurrence++
            }
        }
        return "$cleaned #$occurrence"
    }

    return cleaned
}
