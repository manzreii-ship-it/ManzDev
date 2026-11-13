package com.example.data.repository

import android.content.Context
import com.example.data.model.Anime
import com.example.data.model.AnimeEpisode
import com.example.data.model.WatchHistoryEntry
import com.example.data.model.WatchlistEpisode
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Date

@Serializable
private data class GuestWatchlistItemDto(
    val id: String,
    val animeId: Long,
    val animeTitle: String,
    val animeImageUrl: String,
    val episodeNumber: Long,
    val episodeTitle: String,
    val streamUrl: String = "",
    val genres: List<String> = emptyList(),
    val note: String = "",
    val watchedSeconds: Long = 0L,
    val durationSeconds: Long = 0L,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    fun toDomain(): WatchlistEpisode = WatchlistEpisode(
        id = id,
        userId = "guest",
        animeId = animeId,
        animeTitle = animeTitle,
        animeImageUrl = animeImageUrl,
        episodeNumber = episodeNumber,
        episodeTitle = episodeTitle,
        streamUrl = streamUrl,
        genres = genres,
        note = note,
        watchedSeconds = watchedSeconds,
        durationSeconds = durationSeconds,
        isFavorite = true,
        createdAt = Timestamp(Date(createdAtMillis)),
        updatedAt = Timestamp(Date(updatedAtMillis))
    )
}

@Serializable
private data class GuestWatchHistoryDto(
    val id: String,
    val animeId: Long,
    val animeTitle: String,
    val animeImageUrl: String,
    val lastEpisodeNumber: Long,
    val lastEpisodeTitle: String,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    fun toDomain(): WatchHistoryEntry = WatchHistoryEntry(
        id = id,
        userId = "guest",
        animeId = animeId,
        animeTitle = animeTitle,
        animeImageUrl = animeImageUrl,
        lastEpisodeNumber = lastEpisodeNumber,
        lastEpisodeTitle = lastEpisodeTitle,
        positionMs = positionMs,
        durationMs = durationMs,
        createdAt = Timestamp(Date(createdAtMillis)),
        updatedAt = Timestamp(Date(updatedAtMillis))
    )
}

class GuestStorageRepository(context: Context) {
    private val appContext = context.applicationContext
    private val watchlistFile = File(appContext.filesDir, "manznime_guest_watchlist.json")
    private val historyFile = File(appContext.filesDir, "manznime_guest_history.json")
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val watchlistFlow = MutableStateFlow(loadWatchlistFromDisk())
    private val historyFlow = MutableStateFlow(loadHistoryFromDisk())

    fun observeWatchlist(): Flow<List<WatchlistEpisode>> =
        watchlistFlow.map { list ->
            list.sortedByDescending { it.updatedAtMillis }.map { it.toDomain() }
        }

    fun observeWatchHistory(): Flow<List<WatchHistoryEntry>> =
        historyFlow.map { list ->
            list.sortedByDescending { it.updatedAtMillis }.map { it.toDomain() }
        }

    suspend fun toggleFavoriteEpisode(
        anime: Anime,
        episode: AnimeEpisode,
        streamUrl: String = "",
        watchedSeconds: Long = 0L,
        durationSeconds: Long = 0L,
        note: String = ""
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val id = "${anime.id}_ep_${episode.number}"
            val current = watchlistFlow.value.toMutableList()
            val idx = current.indexOfFirst { it.id == id }
            if (idx >= 0) {
                current.removeAt(idx)
                saveWatchlistToDisk(current)
                watchlistFlow.value = current
                Result.success(false)
            } else {
                val now = System.currentTimeMillis()
                current.add(
                    GuestWatchlistItemDto(
                        id = id,
                        animeId = anime.id,
                        animeTitle = anime.title.ifBlank { "Anime #${anime.id}" },
                        animeImageUrl = anime.largeImageUrl.ifBlank { anime.imageUrl },
                        episodeNumber = episode.number.toLong(),
                        episodeTitle = episode.title.ifBlank { "Episode ${episode.number}" },
                        streamUrl = streamUrl,
                        genres = anime.genres.map { it.name }.take(10),
                        note = note.take(300),
                        watchedSeconds = watchedSeconds.coerceAtLeast(0L),
                        durationSeconds = durationSeconds.coerceAtLeast(0L),
                        createdAtMillis = now,
                        updatedAtMillis = now
                    )
                )
                saveWatchlistToDisk(current)
                watchlistFlow.value = current
                Result.success(true)
            }
        }
    }

    suspend fun updateWatchlistNote(entryId: String, note: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val current = watchlistFlow.value.toMutableList()
                val idx = current.indexOfFirst { it.id == entryId }
                if (idx >= 0) {
                    current[idx] = current[idx].copy(
                        note = note.take(300),
                        updatedAtMillis = System.currentTimeMillis()
                    )
                    saveWatchlistToDisk(current)
                    watchlistFlow.value = current
                }
                Result.success(Unit)
            }
        }

    suspend fun removeWatchlistEpisode(entryId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val current = watchlistFlow.value.filterNot { it.id == entryId }
                saveWatchlistToDisk(current)
                watchlistFlow.value = current
                Result.success(Unit)
            }
        }

    suspend fun savePlaybackProgress(
        animeId: Long,
        animeTitle: String,
        animeImageUrl: String,
        episodeNumber: Int,
        episodeTitle: String,
        streamUrl: String,
        positionMs: Long,
        durationMs: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val safePos = positionMs.coerceAtLeast(0L)
            val safeDur = durationMs.coerceAtLeast(0L)

            val historyId = "anime_${animeId}"
            val currentHistory = historyFlow.value.toMutableList()
            val hIdx = currentHistory.indexOfFirst { it.id == historyId }
            val createdMs = if (hIdx >= 0) currentHistory[hIdx].createdAtMillis else now
            val updatedEntry = GuestWatchHistoryDto(
                id = historyId,
                animeId = animeId,
                animeTitle = animeTitle.ifBlank { "Anime #$animeId" },
                animeImageUrl = animeImageUrl,
                lastEpisodeNumber = episodeNumber.toLong(),
                lastEpisodeTitle = episodeTitle.ifBlank { "Episode $episodeNumber" },
                positionMs = safePos,
                durationMs = safeDur,
                createdAtMillis = createdMs,
                updatedAtMillis = now
            )
            if (hIdx >= 0) {
                currentHistory[hIdx] = updatedEntry
            } else {
                currentHistory.add(updatedEntry)
            }
            saveHistoryToDisk(currentHistory)
            historyFlow.value = currentHistory

            // Also sync progress onto matching saved watchlist episode if present
            val watchlistId = "${animeId}_ep_${episodeNumber}"
            val currentWatchlist = watchlistFlow.value.toMutableList()
            val wIdx = currentWatchlist.indexOfFirst { it.id == watchlistId }
            if (wIdx >= 0) {
                currentWatchlist[wIdx] = currentWatchlist[wIdx].copy(
                    streamUrl = streamUrl,
                    watchedSeconds = safePos / 1000L,
                    durationSeconds = safeDur / 1000L,
                    updatedAtMillis = now
                )
                saveWatchlistToDisk(currentWatchlist)
                watchlistFlow.value = currentWatchlist
            }

            Result.success(Unit)
        }
    }

    suspend fun removeWatchHistoryEntry(historyId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val current = historyFlow.value.filterNot { it.id == historyId }
                saveHistoryToDisk(current)
                historyFlow.value = current
                Result.success(Unit)
            }
        }

    suspend fun clearWatchHistory(): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            saveHistoryToDisk(emptyList())
            historyFlow.value = emptyList()
            Result.success(Unit)
        }
    }

    private fun loadWatchlistFromDisk(): List<GuestWatchlistItemDto> {
        return try {
            if (!watchlistFile.exists()) emptyList()
            else json.decodeFromString(watchlistFile.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveWatchlistToDisk(items: List<GuestWatchlistItemDto>) {
        try {
            watchlistFile.writeText(json.encodeToString(items))
        } catch (_: Exception) {
        }
    }

    private fun loadHistoryFromDisk(): List<GuestWatchHistoryDto> {
        return try {
            if (!historyFile.exists()) emptyList()
            else json.decodeFromString(historyFile.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveHistoryToDisk(items: List<GuestWatchHistoryDto>) {
        try {
            historyFile.writeText(json.encodeToString(items))
        } catch (_: Exception) {
        }
    }
}
