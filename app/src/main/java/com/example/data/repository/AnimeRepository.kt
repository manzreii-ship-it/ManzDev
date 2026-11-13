package com.example.data.repository

import com.example.data.model.Anime
import com.example.data.model.AnimeEpisode
import com.example.data.model.AnimeFilter
import com.example.data.model.AnimeGenre
import com.example.data.remote.JikanApiService
import com.example.data.remote.NetworkModule
import kotlinx.coroutines.delay
import retrofit2.HttpException

data class HomeFeedData(
    val featuredAiring: List<Anime>,
    val topPopular: List<Anime>,
    val topMovies: List<Anime>,
    val genres: List<AnimeGenre>
)

class AnimeRepository(
    private val api: JikanApiService = NetworkModule.jikanApi
) {
    private var cachedGenres: List<AnimeGenre> = emptyList()
    private val animeCache = mutableMapOf<Long, Anime>()
    private val episodeCache = mutableMapOf<Long, List<AnimeEpisode>>()

    private suspend fun <T> withRateLimitRetry(block: suspend () -> T): T {
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: HttpException) {
                if (e.code() == 429 && attempt < 3) {
                    attempt++
                    delay(500L * attempt)
                } else {
                    throw e
                }
            }
        }
    }

    suspend fun getHomeFeed(): HomeFeedData {
        val seasonal = withRateLimitRetry { api.getSeasonNow(limit = 16).data.map { it.toDomain() } }
        delay(350L)
        val popular = withRateLimitRetry {
            api.getTopAnime(filter = "bypopularity", limit = 18).data.map { it.toDomain() }
        }
        delay(350L)
        val movies = withRateLimitRetry {
            api.getTopAnime(type = "movie", limit = 15).data.map { it.toDomain() }
        }
        val genres = getGenres()

        (seasonal + popular + movies).forEach { animeCache[it.id] = it }

        return HomeFeedData(
            featuredAiring = seasonal.distinctBy { it.id },
            topPopular = popular.distinctBy { it.id },
            topMovies = movies.distinctBy { it.id },
            genres = genres
        )
    }

    suspend fun getGenres(): List<AnimeGenre> {
        if (cachedGenres.isNotEmpty()) return cachedGenres
        return try {
            delay(350L)
            val fetched = withRateLimitRetry { api.getAnimeGenres().data.map { it.toDomain() } }
                .filter { it.name.isNotBlank() }
                .sortedByDescending { it.count }
            cachedGenres = fetched.ifEmpty { fallbackGenres() }
            cachedGenres
        } catch (_: Exception) {
            fallbackGenres()
        }
    }

    suspend fun searchAnime(filter: AnimeFilter, page: Int = 1): List<Anime> {
        val genresParam = filter.selectedGenreIds
            .takeIf { it.isNotEmpty() }
            ?.joinToString(",")
        val queryParam = filter.query.trim().takeIf { it.isNotBlank() }
        val sortParam = when (filter.orderBy) {
            "score", "start_date" -> "desc"
            "popularity" -> "asc"
            else -> filter.sort
        }
        val results = withRateLimitRetry {
            api.searchAnime(
                query = queryParam,
                genres = genresParam,
                type = filter.type,
                status = filter.status,
                orderBy = filter.orderBy,
                sort = sortParam,
                page = page,
                limit = 24
            ).data.map { it.toDomain() }
        }
        results.forEach { animeCache[it.id] = it }
        return results
    }

    suspend fun getAnimeDetailAndEpisodes(animeId: Long): Pair<Anime, List<AnimeEpisode>> {
        val cachedAnime = animeCache[animeId]
        val anime = try {
            withRateLimitRetry { api.getAnimeFull(animeId).data?.toDomain() }
                ?.also { animeCache[animeId] = it }
                ?: cachedAnime
                ?: throw IllegalStateException("Anime not found")
        } catch (e: Exception) {
            cachedAnime ?: throw e
        }

        val episodes = episodeCache[animeId] ?: run {
            delay(350L)
            val remoteEpisodes = try {
                withRateLimitRetry { api.getAnimeEpisodes(animeId, page = 1).data.map { it.toDomain() } }
            } catch (_: Exception) {
                emptyList()
            }
            val resolved = if (remoteEpisodes.isNotEmpty()) {
                remoteEpisodes
            } else {
                val count = (anime.episodes ?: if (anime.type.equals("Movie", ignoreCase = true)) 1 else 12)
                    .coerceIn(1, 24)
                (1..count).map { ep ->
                    AnimeEpisode(
                        number = ep,
                        title = if (anime.type.equals("Movie", ignoreCase = true)) {
                            "${anime.title} (Full Feature)"
                        } else {
                            "Episode $ep"
                        }
                    )
                }
            }
            episodeCache[animeId] = resolved
            resolved
        }

        return anime to episodes
    }

    private fun fallbackGenres(): List<AnimeGenre> = listOf(
        AnimeGenre(1, "Action", 5000),
        AnimeGenre(2, "Adventure", 4000),
        AnimeGenre(4, "Comedy", 7000),
        AnimeGenre(8, "Drama", 3000),
        AnimeGenre(10, "Fantasy", 5500),
        AnimeGenre(14, "Horror", 600),
        AnimeGenre(7, "Mystery", 900),
        AnimeGenre(22, "Romance", 2500),
        AnimeGenre(24, "Sci-Fi", 3200),
        AnimeGenre(36, "Slice of Life", 1800),
        AnimeGenre(30, "Sports", 800),
        AnimeGenre(37, "Supernatural", 1700),
        AnimeGenre(41, "Suspense", 500)
    )
}
