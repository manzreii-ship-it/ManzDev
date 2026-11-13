package com.example.data.remote

import com.example.data.model.Anime
import com.example.data.model.AnimeEpisode
import com.example.data.model.AnimeGenre
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class JikanListResponse<T>(
    val data: List<T> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JikanSingleResponse<T>(
    val data: T? = null
)

@JsonClass(generateAdapter = true)
data class ImageUrlsDto(
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "large_image_url") val largeImageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class ImagesDto(
    val jpg: ImageUrlsDto? = null,
    val webp: ImageUrlsDto? = null
)

@JsonClass(generateAdapter = true)
data class NamedEntityDto(
    @Json(name = "mal_id") val malId: Int = 0,
    val name: String = "",
    val count: Int? = null
)

@JsonClass(generateAdapter = true)
data class TrailerDto(
    @Json(name = "youtube_id") val youtubeId: String? = null
)

@JsonClass(generateAdapter = true)
data class AnimeDto(
    @Json(name = "mal_id") val malId: Long = 0L,
    val title: String = "",
    @Json(name = "title_english") val titleEnglish: String? = null,
    @Json(name = "title_japanese") val titleJapanese: String? = null,
    val images: ImagesDto? = null,
    val synopsis: String? = null,
    val score: Double? = null,
    val episodes: Int? = null,
    val status: String? = null,
    val type: String? = null,
    val year: Int? = null,
    val season: String? = null,
    val duration: String? = null,
    val rating: String? = null,
    val studios: List<NamedEntityDto> = emptyList(),
    val genres: List<NamedEntityDto> = emptyList(),
    val themes: List<NamedEntityDto> = emptyList(),
    val trailer: TrailerDto? = null
) {
    fun toDomain(): Anime {
        val primaryImg = images?.webp?.largeImageUrl
            ?: images?.jpg?.largeImageUrl
            ?: images?.webp?.imageUrl
            ?: images?.jpg?.imageUrl
            ?: ""
        val thumbImg = images?.webp?.imageUrl
            ?: images?.jpg?.imageUrl
            ?: primaryImg
        val allGenres = (genres + themes)
            .distinctBy { it.malId }
            .map { AnimeGenre(id = it.malId, name = it.name, count = it.count ?: 0) }
        return Anime(
            id = malId,
            title = titleEnglish?.takeIf { it.isNotBlank() } ?: title,
            titleJapanese = titleJapanese.orEmpty(),
            imageUrl = thumbImg,
            largeImageUrl = primaryImg,
            synopsis = synopsis?.trim().orEmpty().ifBlank { "No synopsis available yet." },
            score = score,
            episodes = episodes,
            status = status.orEmpty(),
            type = type ?: "TV",
            year = year,
            season = season?.replaceFirstChar { it.uppercase() }.orEmpty(),
            studio = studios.firstOrNull()?.name.orEmpty(),
            rating = rating.orEmpty(),
            duration = duration.orEmpty().ifBlank { "24 min per ep" },
            genres = allGenres,
            trailerYoutubeId = trailer?.youtubeId
        )
    }
}

@JsonClass(generateAdapter = true)
data class EpisodeDto(
    @Json(name = "mal_id") val malId: Int = 1,
    val title: String? = null,
    @Json(name = "title_japanese") val titleJapanese: String? = null,
    val aired: String? = null,
    val filler: Boolean? = null,
    val recap: Boolean? = null
) {
    fun toDomain(): AnimeEpisode = AnimeEpisode(
        number = malId,
        title = title?.takeIf { it.isNotBlank() } ?: "Episode $malId",
        titleJapanese = titleJapanese.orEmpty(),
        aired = aired?.take(10).orEmpty(),
        filler = filler ?: false,
        recap = recap ?: false
    )
}

@JsonClass(generateAdapter = true)
data class GenreDto(
    @Json(name = "mal_id") val malId: Int = 0,
    val name: String = "",
    val count: Int? = null
) {
    fun toDomain(): AnimeGenre = AnimeGenre(
        id = malId,
        name = name,
        count = count ?: 0
    )
}

interface JikanApiService {
    @GET("seasons/now")
    suspend fun getSeasonNow(
        @Query("sfw") sfw: Boolean = true,
        @Query("limit") limit: Int = 18
    ): JikanListResponse<AnimeDto>

    @GET("top/anime")
    suspend fun getTopAnime(
        @Query("filter") filter: String? = null,
        @Query("type") type: String? = null,
        @Query("sfw") sfw: Boolean = true,
        @Query("limit") limit: Int = 20
    ): JikanListResponse<AnimeDto>

    @GET("anime")
    suspend fun searchAnime(
        @Query("q") query: String? = null,
        @Query("genres") genres: String? = null,
        @Query("type") type: String? = null,
        @Query("status") status: String? = null,
        @Query("order_by") orderBy: String? = "popularity",
        @Query("sort") sort: String? = "asc",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 24,
        @Query("sfw") sfw: Boolean = true
    ): JikanListResponse<AnimeDto>

    @GET("genres/anime")
    suspend fun getAnimeGenres(
        @Query("filter") filter: String = "genres"
    ): JikanListResponse<GenreDto>

    @GET("anime/{id}/full")
    suspend fun getAnimeFull(
        @Path("id") animeId: Long
    ): JikanSingleResponse<AnimeDto>

    @GET("anime/{id}/episodes")
    suspend fun getAnimeEpisodes(
        @Path("id") animeId: Long,
        @Query("page") page: Int = 1
    ): JikanListResponse<EpisodeDto>
}
