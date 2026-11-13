package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class WatchlistEpisode(
    @DocumentId val id: String = "",
    val userId: String = "",
    val animeId: Long = 0L,
    val animeTitle: String = "",
    val animeImageUrl: String = "",
    val episodeNumber: Long = 1L,
    val episodeTitle: String = "",
    val streamUrl: String = "",
    val genres: List<String> = emptyList(),
    val note: String = "",
    val watchedSeconds: Long = 0L,
    val durationSeconds: Long = 0L,
    val isFavorite: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val progressFraction: Float
        get() = if (durationSeconds > 0L) {
            (watchedSeconds.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val isCompleted: Boolean
        get() = durationSeconds > 0L && progressFraction >= 0.90f
}

@IgnoreExtraProperties
data class WatchHistoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val animeId: Long = 0L,
    val animeTitle: String = "",
    val animeImageUrl: String = "",
    val lastEpisodeNumber: Long = 1L,
    val lastEpisodeTitle: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
