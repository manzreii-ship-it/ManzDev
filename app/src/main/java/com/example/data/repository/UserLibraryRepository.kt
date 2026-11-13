package com.example.data.repository

import com.example.data.model.Anime
import com.example.data.model.AnimeEpisode
import com.example.data.model.WatchHistoryEntry
import com.example.data.model.WatchlistEpisode
import kotlinx.coroutines.flow.Flow

interface UserLibraryRepository {
    val isGuest: Boolean
    fun observeWatchlist(): Flow<List<WatchlistEpisode>>
    fun observeWatchHistory(): Flow<List<WatchHistoryEntry>>
    suspend fun toggleFavoriteEpisode(
        anime: Anime,
        episode: AnimeEpisode,
        streamUrl: String = "",
        watchedSeconds: Long = 0L,
        durationSeconds: Long = 0L,
        note: String = ""
    ): Result<Boolean>
    suspend fun updateWatchlistNote(entryId: String, note: String): Result<Unit>
    suspend fun removeWatchlistEpisode(entryId: String): Result<Unit>
    suspend fun savePlaybackProgress(
        animeId: Long,
        animeTitle: String,
        animeImageUrl: String,
        episodeNumber: Int,
        episodeTitle: String,
        streamUrl: String,
        positionMs: Long,
        durationMs: Long
    ): Result<Unit>
    suspend fun removeWatchHistoryEntry(historyId: String): Result<Unit>
    suspend fun clearWatchHistory(): Result<Unit>
}

class FirestoreLibraryRepository(
    private val watchlistRepository: WatchlistRepository,
    private val userId: String
) : UserLibraryRepository {
    override val isGuest: Boolean = false

    override fun observeWatchlist(): Flow<List<WatchlistEpisode>> =
        watchlistRepository.observeWatchlist(userId)

    override fun observeWatchHistory(): Flow<List<WatchHistoryEntry>> =
        watchlistRepository.observeWatchHistory(userId)

    override suspend fun toggleFavoriteEpisode(
        anime: Anime,
        episode: AnimeEpisode,
        streamUrl: String,
        watchedSeconds: Long,
        durationSeconds: Long,
        note: String
    ): Result<Boolean> = watchlistRepository.toggleFavoriteEpisode(
        anime = anime,
        episode = episode,
        streamUrl = streamUrl,
        watchedSeconds = watchedSeconds,
        durationSeconds = durationSeconds,
        note = note
    )

    override suspend fun updateWatchlistNote(entryId: String, note: String): Result<Unit> =
        watchlistRepository.updateWatchlistNote(entryId, note)

    override suspend fun removeWatchlistEpisode(entryId: String): Result<Unit> =
        watchlistRepository.removeWatchlistEpisode(entryId)

    override suspend fun savePlaybackProgress(
        animeId: Long,
        animeTitle: String,
        animeImageUrl: String,
        episodeNumber: Int,
        episodeTitle: String,
        streamUrl: String,
        positionMs: Long,
        durationMs: Long
    ): Result<Unit> = watchlistRepository.savePlaybackProgress(
        animeId = animeId,
        animeTitle = animeTitle,
        animeImageUrl = animeImageUrl,
        episodeNumber = episodeNumber,
        episodeTitle = episodeTitle,
        streamUrl = streamUrl,
        positionMs = positionMs,
        durationMs = durationMs
    )

    override suspend fun removeWatchHistoryEntry(historyId: String): Result<Unit> =
        watchlistRepository.removeWatchHistoryEntry(historyId)

    override suspend fun clearWatchHistory(): Result<Unit> =
        watchlistRepository.clearWatchHistory()
}

class GuestLibraryRepository(
    private val guestStorage: GuestStorageRepository
) : UserLibraryRepository {
    override val isGuest: Boolean = true

    override fun observeWatchlist(): Flow<List<WatchlistEpisode>> =
        guestStorage.observeWatchlist()

    override fun observeWatchHistory(): Flow<List<WatchHistoryEntry>> =
        guestStorage.observeWatchHistory()

    override suspend fun toggleFavoriteEpisode(
        anime: Anime,
        episode: AnimeEpisode,
        streamUrl: String,
        watchedSeconds: Long,
        durationSeconds: Long,
        note: String
    ): Result<Boolean> = guestStorage.toggleFavoriteEpisode(
        anime = anime,
        episode = episode,
        streamUrl = streamUrl,
        watchedSeconds = watchedSeconds,
        durationSeconds = durationSeconds,
        note = note
    )

    override suspend fun updateWatchlistNote(entryId: String, note: String): Result<Unit> =
        guestStorage.updateWatchlistNote(entryId, note)

    override suspend fun removeWatchlistEpisode(entryId: String): Result<Unit> =
        guestStorage.removeWatchlistEpisode(entryId)

    override suspend fun savePlaybackProgress(
        animeId: Long,
        animeTitle: String,
        animeImageUrl: String,
        episodeNumber: Int,
        episodeTitle: String,
        streamUrl: String,
        positionMs: Long,
        durationMs: Long
    ): Result<Unit> = guestStorage.savePlaybackProgress(
        animeId = animeId,
        animeTitle = animeTitle,
        animeImageUrl = animeImageUrl,
        episodeNumber = episodeNumber,
        episodeTitle = episodeTitle,
        streamUrl = streamUrl,
        positionMs = positionMs,
        durationMs = durationMs
    )

    override suspend fun removeWatchHistoryEntry(historyId: String): Result<Unit> =
        guestStorage.removeWatchHistoryEntry(historyId)

    override suspend fun clearWatchHistory(): Result<Unit> =
        guestStorage.clearWatchHistory()
}
