package com.example.data.model

data class Anime(
    val id: Long,
    val title: String,
    val titleJapanese: String = "",
    val imageUrl: String = "",
    val largeImageUrl: String = "",
    val synopsis: String = "",
    val score: Double? = null,
    val episodes: Int? = null,
    val status: String = "",
    val type: String = "TV",
    val year: Int? = null,
    val season: String = "",
    val studio: String = "",
    val rating: String = "",
    val duration: String = "24 min per ep",
    val genres: List<AnimeGenre> = emptyList(),
    val trailerYoutubeId: String? = null
)

data class AnimeGenre(
    val id: Int,
    val name: String,
    val count: Int = 0
)

data class AnimeEpisode(
    val number: Int,
    val title: String,
    val titleJapanese: String = "",
    val aired: String = "",
    val filler: Boolean = false,
    val recap: Boolean = false,
    val durationMinutes: Int = 24
)

data class StreamSource(
    val id: String,
    val label: String,
    val provider: String,
    val quality: String,
    val url: String,
    val isHls: Boolean = false
)

data class AnimeFilter(
    val query: String = "",
    val selectedGenreIds: Set<Int> = emptySet(),
    val type: String? = null,      // tv, movie, ova, ona, special
    val status: String? = null,    // airing, complete, upcoming
    val orderBy: String = "popularity", // popularity, score, start_date, title
    val sort: String = "asc"       // asc or desc
) {
    val activeFilterCount: Int
        get() = selectedGenreIds.size +
            (if (type != null) 1 else 0) +
            (if (status != null) 1 else 0) +
            (if (orderBy != "popularity") 1 else 0)
}
