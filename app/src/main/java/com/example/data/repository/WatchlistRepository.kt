package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Anime
import com.example.data.model.AnimeEpisode
import com.example.data.model.OperationType
import com.example.data.model.WatchHistoryEntry
import com.example.data.model.WatchlistEpisode
import com.example.data.model.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class WatchlistRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun watchlistDocId(animeId: Long, episodeNumber: Int): String = "${animeId}_ep_${episodeNumber}"

    fun historyDocId(animeId: Long): String = "anime_${animeId}"

    fun observeWatchlist(userId: String): Flow<List<WatchlistEpisode>> {
        val path = "users/$userId/watchlist_episodes"
        return db.collection("users")
            .document(userId)
            .collection("watchlist_episodes")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    WatchlistEpisode::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                }
                throw error
            }
    }

    fun observeWatchHistory(userId: String): Flow<List<WatchHistoryEntry>> {
        val path = "users/$userId/watch_history"
        return db.collection("users")
            .document(userId)
            .collection("watch_history")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    WatchHistoryEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                }
                throw error
            }
    }

    suspend fun toggleFavoriteEpisode(
        anime: Anime,
        episode: AnimeEpisode,
        streamUrl: String = "",
        watchedSeconds: Long = 0L,
        durationSeconds: Long = 0L,
        note: String = ""
    ): Result<Boolean> {
        val uid = requireUserId()
        val entryId = watchlistDocId(anime.id, episode.number)
        val docRef = db.collection("users")
            .document(uid)
            .collection("watchlist_episodes")
            .document(entryId)

        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.delete().await()
                Result.success(false)
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "animeId" to anime.id,
                    "animeTitle" to anime.title.take(200).ifBlank { "Anime #${anime.id}" },
                    "animeImageUrl" to (anime.largeImageUrl.ifBlank { anime.imageUrl }).take(600),
                    "episodeNumber" to episode.number.toLong(),
                    "episodeTitle" to episode.title.take(200).ifBlank { "Episode ${episode.number}" },
                    "streamUrl" to streamUrl.take(800),
                    "genres" to anime.genres.map { it.name }.take(10),
                    "note" to note.take(300),
                    "watchedSeconds" to watchedSeconds.coerceAtLeast(0L),
                    "durationSeconds" to durationSeconds.coerceAtLeast(0L),
                    "isFavorite" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun getWatchlistEpisode(entryId: String): Result<WatchlistEpisode?> {
        val uid = requireUserId()
        val docRef = db.collection("users")
            .document(uid)
            .collection("watchlist_episodes")
            .document(entryId)
        return try {
            val snap = docRef.get().await()
            val item = snap.toObject(
                WatchlistEpisode::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(item)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateWatchlistNote(entryId: String, note: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users")
            .document(uid)
            .collection("watchlist_episodes")
            .document(entryId)
        return try {
            docRef.update(
                mapOf(
                    "note" to note.take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun removeWatchlistEpisode(entryId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users")
            .document(uid)
            .collection("watchlist_episodes")
            .document(entryId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
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
    ): Result<Unit> {
        val uid = requireUserId()
        val historyRef = db.collection("users")
            .document(uid)
            .collection("watch_history")
            .document(historyDocId(animeId))
        val watchlistRef = db.collection("users")
            .document(uid)
            .collection("watchlist_episodes")
            .document(watchlistDocId(animeId, episodeNumber))

        return try {
            val safePosMs = positionMs.coerceAtLeast(0L)
            val safeDurMs = durationMs.coerceAtLeast(0L)
            val existingHistory = historyRef.get().await()
            if (existingHistory.exists()) {
                historyRef.update(
                    mapOf(
                        "animeTitle" to animeTitle.take(200).ifBlank { "Anime #$animeId" },
                        "animeImageUrl" to animeImageUrl.take(600),
                        "lastEpisodeNumber" to episodeNumber.toLong(),
                        "lastEpisodeTitle" to episodeTitle.take(200).ifBlank { "Episode $episodeNumber" },
                        "positionMs" to safePosMs,
                        "durationMs" to safeDurMs,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                historyRef.set(
                    mapOf(
                        "userId" to uid,
                        "animeId" to animeId,
                        "animeTitle" to animeTitle.take(200).ifBlank { "Anime #$animeId" },
                        "animeImageUrl" to animeImageUrl.take(600),
                        "lastEpisodeNumber" to episodeNumber.toLong(),
                        "lastEpisodeTitle" to episodeTitle.take(200).ifBlank { "Episode $episodeNumber" },
                        "positionMs" to safePosMs,
                        "durationMs" to safeDurMs,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val existingWatchlist = watchlistRef.get().await()
            if (existingWatchlist.exists()) {
                watchlistRef.update(
                    mapOf(
                        "streamUrl" to streamUrl.take(800),
                        "watchedSeconds" to (safePosMs / 1000L),
                        "durationSeconds" to (safeDurMs / 1000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, historyRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun removeWatchHistoryEntry(historyId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users")
            .document(uid)
            .collection("watch_history")
            .document(historyId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun clearWatchHistory(): Result<Unit> {
        val uid = requireUserId()
        val colRef = db.collection("users")
            .document(uid)
            .collection("watch_history")
        return try {
            val docs = colRef.get().await().documents
            if (docs.isNotEmpty()) {
                val batch = db.batch()
                docs.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colRef.path, auth)
            Result.failure(e)
        }
    }
}
