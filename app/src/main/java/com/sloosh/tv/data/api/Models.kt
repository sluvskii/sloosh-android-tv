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
    @SerializedName("items") val items: List<MediaDto>? = null,
    @SerializedName("pages") val pages: Int?,
    @SerializedName("total") val total: Int?,
    @SerializedName("total_pages") val totalPages: Int?,
    @SerializedName("total_results") val totalResults: Int?
) {
    val allItems: List<MediaDto> get() = items ?: results ?: emptyList()
    val effectiveTotalPages: Int get() = pages ?: totalPages ?: 1
    val effectiveTotalResults: Int get() = total ?: totalResults ?: allItems.size
}

data class MediaDto(
    @SerializedName("id") val originalId: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("originalTitle") val originalTitle: String?,
    @SerializedName("year") val year: Any?,
    @SerializedName("rating") val rating: Double?,
    @SerializedName("ratings") val ratings: RatingsV2Dto? = null,
    @SerializedName("poster") val poster: String? = null,
    @SerializedName("posterUrl") val posterUrl: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("genres") val genres: List<GenreDto>? = null,
    @SerializedName("externalIds") val externalIds: ExternalIdsDto? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop") val backdrop: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null
) {
    val identifier: String
        get() {
            if (!originalId.isNullOrEmpty()) return originalId
            val titlePart = (title ?: name ?: originalTitle ?: "unknown").trim().lowercase()
            val yearPart = year?.toString() ?: ""
            val posterPart = (poster ?: posterUrl ?: posterPath ?: "").trim().lowercase()
            val typePart = (type ?: "unknown").lowercase()
            return "fallback|$typePart|$titlePart|$yearPart|$posterPart"
        }

    val displayTitle: String get() = title ?: name ?: originalTitle ?: "Без названия"

    val yearString: String get() = when (year) {
        is Double -> year.toInt().toString()
        else -> year?.toString() ?: ""
    }

    fun getDisplayPosterUrl(isLowQuality: Boolean = false): String? {
        val rawUrl = poster ?: posterUrl ?: posterPath
        return normalizeImageUrl(path = rawUrl, id = originalId, isLowQuality = isLowQuality)
    }

    fun getDisplayBackdropUrl(isLowQuality: Boolean = false): String? {
        val rawUrl = backdrop ?: backdropPath
        return normalizeImageUrl(path = rawUrl, id = originalId, isLowQuality = isLowQuality) ?: getDisplayPosterUrl(isLowQuality)
    }

    val isTvSeries: Boolean
        get() {
            val typeLower = type?.lowercase()?.trim() ?: ""
            return typeLower in listOf("tv", "series", "serial", "show")
        }

    val isCartoon: Boolean
        get() {
            val typeLower = type?.lowercase()?.trim() ?: ""
            if (typeLower in listOf("cartoon", "animated", "anime")) return true
            return genres?.any { g ->
                val name = (g.name ?: g.id ?: "").lowercase()
                name.contains("мульт") || name.contains("аним")
            } == true
        }

    val isMovie: Boolean
        get() = !isTvSeries && !isCartoon

    val id: String get() = originalId ?: identifier
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
    @SerializedName("backdrops") val backdrops: List<String>? = null,
    @SerializedName("logo") val logo: String? = null,
    @SerializedName("cast") val cast: List<CastMemberDto>? = null,
    @SerializedName("directors") val directors: List<CrewMemberDto>? = null,
    @SerializedName("writers") val writers: List<CrewMemberDto>? = null,
    @SerializedName("crew") val crew: List<CrewMemberDto>? = null,
    @SerializedName("similar") val similar: List<MediaDto>? = null,
    @SerializedName("seasons") val seasons: List<TvSeasonSummaryDto>? = null,
    @SerializedName("ratings") val ratings: RatingsV2Dto? = null,
    @SerializedName("ids") val ids: IdsDto? = null,
    @SerializedName("externalIds") val externalIds: ExternalIdsDto? = null,
    @SerializedName("budget") val budget: Long? = null,
    @SerializedName("revenue") val revenue: Long? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("ageRating") val ageRating: String? = null,
    @SerializedName("collection") val collection: MovieCollectionDto? = null
) {
    val displayTitle: String get() = title ?: originalTitle ?: "Без названия"
    val rating: Double? get() = ratings?.kp ?: ratings?.imdb ?: ratings?.tmdb
    val yearString: String get() = year?.toString() ?: ""

    val isTvSeries: Boolean
        get() {
            val typeLower = type?.lowercase()?.trim() ?: ""
            return typeLower in listOf("tv", "series", "serial", "show") || !seasons.isNullOrEmpty()
        }

    fun getDisplayPosterUrl(isLowQuality: Boolean = false): String? {
        return normalizeImageUrl(path = poster, id = id, isLowQuality = isLowQuality)
    }

    fun getDisplayBackdropUrl(isLowQuality: Boolean = false): String? {
        if (!backdrop.isNullOrEmpty()) {
            return normalizeImageUrl(path = backdrop, id = id, isLowQuality = isLowQuality) ?: backdrop
        }
        if (!poster.isNullOrEmpty()) {
            return normalizeImageUrl(path = poster, id = id, isLowQuality = isLowQuality) ?: poster
        }
        val validId = id?.replace("kp_", "")?.trim() ?: return null
        if (validId.isEmpty()) return null
        return "https://api-sloosh.vercel.app/api/v1/images/backdrops/$validId/original"
    }

    fun getPreviewBackdropUrl(): String? {
        if (!backdrop.isNullOrEmpty()) {
            return normalizeImageUrl(path = backdrop, id = id, isLowQuality = true) ?: backdrop
        }
        if (!poster.isNullOrEmpty()) {
            return normalizeImageUrl(path = poster, id = id, isLowQuality = true) ?: poster
        }
        val validId = id?.replace("kp_", "")?.trim() ?: return null
        if (validId.isEmpty()) return null
        return "https://api-sloosh.vercel.app/api/v1/images/backdrops/$validId/small"
    }

    fun getDisplayLogoUrl(): String? {
        if (!logo.isNullOrEmpty()) {
            return normalizeImageUrl(path = logo, id = id) ?: logo
        }
        val validId = id?.replace("kp_", "")?.trim() ?: return null
        if (validId.isEmpty()) return null
        return "https://api-sloosh.vercel.app/api/v1/images/logos/$validId/original"
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

data class CastMemberDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("originalName") val originalName: String? = null,
    @SerializedName("character") val character: String? = null,
    @SerializedName("photo") val photo: String? = null
) {
    fun getDisplayPhotoUrl(): String? {
        val p = photo ?: return null
        return adjustExternalImageUrl(p, isLowQuality = false)
    }
}

data class CrewMemberDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("originalName") val originalName: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("photo") val photo: String? = null
) {
    fun getDisplayPhotoUrl(): String? {
        val p = photo ?: return null
        return adjustExternalImageUrl(p, isLowQuality = false)
    }
}

data class StreamConfigDto(
    @SerializedName("tokens") val tokens: List<String> = emptyList()
)

data class PersonDetailDto(
    @SerializedName("id") val id: Any?,
    @SerializedName("name") val name: String?,
    @SerializedName("originalName") val originalName: String? = null,
    @SerializedName("biography") val biography: String? = null,
    @SerializedName("birthday") val birthday: String? = null,
    @SerializedName("deathday") val deathday: String? = null,
    @SerializedName("placeOfBirth") val placeOfBirth: String? = null,
    @SerializedName("photo") val photo: String? = null,
    @SerializedName("knownForDepartment") val knownForDepartment: String? = null,
    @SerializedName("department") val department: String? = null,
    @SerializedName("gender") val gender: Int? = null,
    @SerializedName("filmography") val filmography: List<MediaDto>? = null,
    @SerializedName("photos") val photos: List<String>? = null,
    @SerializedName("awards") val awards: String? = null,
    @SerializedName("keyProjects") val keyProjects: String? = null,
    @SerializedName("interestingFact") val interestingFact: String? = null
) {
    val displayId: String get() = id?.toString() ?: ""
    val displayName: String get() = name ?: originalName ?: "Неизвестно"

    fun getDisplayPhotoUrl(): String? {
        val p = photo ?: return null
        return adjustExternalImageUrl(p, isLowQuality = false)
    }

    val age: Int?
        get() {
            val bday = birthday?.trim() ?: return null
            if (bday.length < 4) return null
            return try {
                val birthYear = bday.substring(0, 4).toIntOrNull() ?: return null
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val deathYear = deathday?.trim()?.takeIf { it.length >= 4 }?.substring(0, 4)?.toIntOrNull()
                (deathYear ?: currentYear) - birthYear
            } catch (_: Exception) {
                null
            }
        }

    val formattedBirthdayWithAge: String?
        get() {
            val bday = birthday?.trim() ?: return null
            if (bday.isEmpty()) return null
            val calculatedAge = age
            return if (calculatedAge != null && calculatedAge > 0) {
                val suffix = when {
                    calculatedAge % 100 in 11..19 -> "лет"
                    calculatedAge % 10 == 1 -> "год"
                    calculatedAge % 10 in 2..4 -> "года"
                    else -> "лет"
                }
                "$bday ($calculatedAge $suffix)"
            } else {
                bday
            }
        }
}

data class CategorySectionDto(
    @SerializedName("section") val section: String,
    @SerializedName("items") val items: List<CategoryItemDto> = emptyList()
)

data class CategoryItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("backdrop") val backdrop: String? = null
)

data class MovieCollectionDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("overview") val overview: String? = null,
    @SerializedName("poster") val poster: String? = null,
    @SerializedName("backdrop") val backdrop: String? = null,
    @SerializedName("parts") val parts: List<MediaDto>? = null
)

data class RelatedStudioResponse(
    @SerializedName("items") val items: List<MediaDto>? = null,
    @SerializedName("results") val results: List<MediaDto>? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("page") val page: Int? = null,
    @SerializedName("totalPages") val totalPages: Int? = null,
    @SerializedName("totalResults") val totalResults: Int? = null
) {
    val allItems: List<MediaDto>
        get() = items ?: results ?: emptyList()
}

data class TvSeasonSummaryDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("seasonNumber") val seasonNumber: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("episodeCount") val episodeCount: Int?,
    @SerializedName("airDate") val airDate: String?,
    @SerializedName("poster") val poster: String?
)

data class TvSeasonEpisodeDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("airDate") val airDate: String?,
    @SerializedName("episodeNumber") val episodeNumber: Int?,
    @SerializedName("seasonNumber") val seasonNumber: Int?,
    @SerializedName("stillPath") val stillPath: String?,
    @SerializedName("voteAverage") val voteAverage: Double?,
    @SerializedName("duration") val duration: Int?
) {
    fun getDisplayStillUrl(): String? {
        if (!stillPath.isNullOrEmpty()) {
            if (stillPath.startsWith("http")) return stillPath
            return "https://api-sloosh.vercel.app/api/v1/images/tmdb/w500$stillPath"
        }
        return null
    }
}

data class TvSeasonDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("seasonNumber") val seasonNumber: Int?,
    @SerializedName("poster") val poster: String?,
    @SerializedName("airDate") val airDate: String?,
    @SerializedName("episodes") val episodes: List<TvSeasonEpisodeDto>?
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
        result = result.replace("https://image.tmdb.org/t/p/", "https://api-sloosh.vercel.app/api/v1/images/tmdb/")
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
    if (path != null && path.contains("no-poster")) return null
    val baseUrl = "https://api-sloosh.vercel.app"
    var rawUrl = path
    if (rawUrl != null) {
        if (rawUrl.contains("no-poster")) return null
        rawUrl = adjustExternalImageUrl(rawUrl, isLowQuality)
    }

    val trimmed = rawUrl?.trim()
    if (!trimmed.isNullOrEmpty()) {
        if (trimmed.contains("no-poster")) return null
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed.replace("https://image.tmdb.org/t/p/", "$baseUrl/api/v1/images/tmdb/")
        }
        if (trimmed.startsWith("/")) {
            if (trimmed.endsWith(".jpg") || trimmed.endsWith(".png") || trimmed.endsWith(".jpeg") || trimmed.endsWith(".webp")) {
                val size = if (isLowQuality) "w342" else "w500"
                return "$baseUrl/api/v1/images/tmdb/$size$trimmed"
            }
            return "$baseUrl$trimmed"
        }
        if (trimmed.startsWith("api/")) {
            return "$baseUrl/$trimmed"
        }
    }

    val sanitizedId = id?.replace("kp_", "")?.trim() ?: return null
    if (!sanitizedId.all { it.isDigit() }) return null
    val qualityPath = if (isLowQuality) "kp_small" else "kp"
    return "$baseUrl/api/v1/images/$qualityPath/$sanitizedId?fallback=true"
}
