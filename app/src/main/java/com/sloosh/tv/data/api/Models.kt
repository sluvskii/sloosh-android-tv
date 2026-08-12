package com.sloosh.tv.data.api

import com.google.gson.annotations.SerializedName
import java.net.URLEncoder

data class ApiEnvelope<T>(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("data") val data: T?
)

data class MediaResponse(
    @SerializedName("page") val page: Int?,
    @SerializedName("results") val results: List<MediaDto>?,
    @SerializedName("pages") val pages: Int?,
    @SerializedName("total") val total: Int?,
    @SerializedName("total_pages") val totalPages: Int?,
    @SerializedName("total_results") val totalResults: Int?
) {
    val effectiveTotalPages: Int get() = pages ?: totalPages ?: 1
    val effectiveTotalResults: Int get() = total ?: totalResults ?: (results?.size ?: 0)
}

data class MediaDto(
    @SerializedName("id") val originalId: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("originalTitle") val originalTitle: String?,
    @SerializedName("year") val year: Any?,
    @SerializedName("rating") val rating: Double?,
    @SerializedName("posterUrl") val posterUrl: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("genres") val genres: List<GenreDto>?,
    @SerializedName("externalIds") val externalIds: ExternalIdsDto?,
    @SerializedName("name") val name: String?,
    @SerializedName("poster_path") val posterPath: String?
) {
    val identifier: String
        get() {
            if (!originalId.isNullOrEmpty()) return originalId
            val titlePart = (title ?: name ?: originalTitle ?: "unknown").trim().lowercase()
            val yearPart = year?.toString() ?: ""
            val posterPart = (posterUrl ?: posterPath ?: "").trim().lowercase()
            val typePart = (type ?: "unknown").lowercase()
            return "fallback|$typePart|$titlePart|$yearPart|$posterPart"
        }

    val displayTitle: String get() = title ?: name ?: originalTitle ?: "Без названия"

    val yearString: String get() = when (year) {
        is Double -> year.toInt().toString()
        else -> year?.toString() ?: ""
    }

    fun getDisplayPosterUrl(isLowQuality: Boolean = false): String? {
        val rawUrl = posterUrl ?: posterPath
        return normalizeImageUrl(path = rawUrl, id = originalId, isLowQuality = isLowQuality)
    }
}

data class MediaDetailsDto(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("originalTitle") val originalTitle: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("year") val year: Int?,
    @SerializedName("releaseDate") val releaseDate: String?,
    @SerializedName("genres") val genres: List<String>?,
    @SerializedName("countries") val countries: List<String>?,
    @SerializedName("duration") val duration: Int?,
    @SerializedName("poster") val poster: String?,
    @SerializedName("backdrop") val backdrop: String?,
    @SerializedName("ratings") val ratings: RatingsV2Dto?,
    @SerializedName("ids") val ids: IdsDto?
) {
    fun getDisplayPosterUrl(isLowQuality: Boolean = false): String? {
        return normalizeImageUrl(path = poster, id = id, isLowQuality = isLowQuality)
    }

    fun getDisplayBackdropUrl(isLowQuality: Boolean = false): String? {
        val validId = id?.replace("kp_", "")?.trim() ?: return null
        if (validId.isEmpty()) return null
        val size = if (isLowQuality) "large" else "original"
        return "https://api.neome.uk/api/v1/images/backdrops/$validId/$size"
    }

    fun getPreviewBackdropUrl(): String? {
        val validId = id?.replace("kp_", "")?.trim() ?: return null
        if (validId.isEmpty()) return null
        return "https://api.neome.uk/api/v1/images/backdrops/$validId/small"
    }

    fun getDisplayLogoUrl(): String? {
        val validId = id?.replace("kp_", "")?.trim() ?: return null
        if (validId.isEmpty()) return null
        return "https://api.neome.uk/api/v1/images/logos/$validId/original"
    }
}

data class GenreDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null
)

data class ExternalIdsDto(
    @SerializedName("kp") val kp: Int?,
    @SerializedName("tmdb") val tmdb: Int?,
    @SerializedName("imdb") val imdb: String?
)

data class RatingsV2Dto(
    @SerializedName("kp") val kp: Double?,
    @SerializedName("imdb") val imdb: Double?,
    @SerializedName("tmdb") val tmdb: Double?
)

data class IdsDto(
    @SerializedName("kp") val kp: Int?,
    @SerializedName("imdb") val imdb: String?,
    @SerializedName("tmdb") val tmdb: Int?
)

data class TvEpisodeDetailsDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("airDate") val airDate: String?,
    @SerializedName("seasonNumber") val seasonNumber: Int?,
    @SerializedName("episodeNumber") val episodeNumber: Int?,
    @SerializedName("stillPath") val stillPath: String?,
    @SerializedName("language") val language: String?,
    @SerializedName("ratings") val ratings: EpisodeRatingsDto?
)

data class EpisodeRatingsDto(
    @SerializedName("kp") val kp: Double?,
    @SerializedName("tmdb") val tmdb: Double?,
    @SerializedName("imdb") val imdb: Double?
)

data class AllohaTranslation(
    val id: String,
    val name: String,
    val iframeUrl: String,
    val streamUrl: String? = null
)

data class AllohaEpisode(
    val season: Int,
    val episode: Int,
    val translations: List<AllohaTranslation>
)

data class AllohaSeason(
    val season: Int,
    val episodes: List<AllohaEpisode>
)

data class AllohaMovie(
    val title: String,
    val iframeUrl: String,
    val translations: List<AllohaTranslation>
)

data class AllohaApiResult(
    val title: String,
    val isSerial: Boolean,
    val movie: AllohaMovie?,
    val seasons: List<AllohaSeason>
)

data class QualityVariant(
    val label: String,
    val url: String
)

data class AudioVariant(
    val id: String,
    val title: String,
    val url: String,
    val qualityVariants: List<QualityVariant>
)

data class SubtitleTrack(
    val label: String,
    val language: String,
    val url: String
)

data class SkipTimeRange(
    val start: Double,
    val end: Double
)

data class AllohaResolvedStream(
    val videoUrl: String,
    val audioVariants: List<AudioVariant>,
    val qualityVariants: List<QualityVariant>,
    val subtitles: List<SubtitleTrack>,
    val headers: Map<String, String>,
    val introRange: SkipTimeRange? = null,
    val outroRange: SkipTimeRange? = null
)

fun adjustExternalImageUrl(urlStr: String, isLowQuality: Boolean): String {
    var result = urlStr
    if (result.contains("get-kinopoisk-image") || result.contains("mds.yandex.net")) {
        val lastSlash = result.lastIndexOf("/")
        if (lastSlash != -1) {
            val base = result.substring(0, lastSlash)
            val suffix = if (isLowQuality) "300x450" else "orig"
            result = "$base/$suffix"
        }
    } else if (result.contains("image.tmdb.org/t/p/")) {
        result = if (isLowQuality) {
            result.replace("/original/", "/w342/").replace("/w500/", "/w342/")
        } else {
            result.replace("/w342/", "/w500/")
        }
    } else {
        result = if (isLowQuality) {
            if (result.contains("/kp/")) result.replace("/kp/", "/kp_small/") else result
        } else {
            if (result.contains("/kp_small/")) result.replace("/kp_small/", "/kp/") else result
        }
    }
    return result
}

fun normalizeImageUrl(path: String?, id: String? = null, isLowQuality: Boolean = false): String? {
    val baseUrl = "https://api.neome.uk"
    var rawUrl = path
    if (rawUrl != null) {
        rawUrl = adjustExternalImageUrl(rawUrl, isLowQuality)
    }

    val trimmed = rawUrl?.trim()
    if (!trimmed.isNullOrEmpty()) {
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return encodeUrl(trimmed)
        }
        if (trimmed.startsWith("/")) {
            return encodeUrl(baseUrl + trimmed)
        }
        if (trimmed.startsWith("api/")) {
            return encodeUrl("$baseUrl/$trimmed")
        }
    }

    val sanitizedId = id?.replace("kp_", "")?.trim() ?: return null
    if (!sanitizedId.all { it.isDigit() }) return null
    val qualityPath = if (isLowQuality) "kp_small" else "kp"
    return "$baseUrl/api/v1/images/$qualityPath/$sanitizedId?fallback=true"
}

private fun encodeUrl(url: String): String {
    return try {
        url // Java URL/URLEncoder or keep as-is if already safe
    } catch (e: Exception) {
        url
    }
}
