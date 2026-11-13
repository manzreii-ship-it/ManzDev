package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Anime
import com.example.data.model.AnimeEpisode
import com.example.data.model.AnimeGenre
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class WatchlistRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleAnime(id: Long = (1000L..99999L).random()): Anime = Anime(
        id = id,
        title = "Frieren: Beyond Journey's End",
        imageUrl = "https://cdn.myanimelist.net/images/anime/1015/138006.jpg",
        largeImageUrl = "https://cdn.myanimelist.net/images/anime/1015/138006l.jpg",
        genres = listOf(AnimeGenre(2, "Adventure"), AnimeGenre(8, "Drama"), AnimeGenre(10, "Fantasy"))
    )

    private fun sampleEpisode(number: Int = 1): AnimeEpisode = AnimeEpisode(
        number = number,
        title = "The Journey's End",
        durationMinutes = 24
    )

    @Test
    fun toggleFavoriteEpisode_authenticatedOwner_createsUpdatesAndDeletes() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = WatchlistRepository(firestore, auth)
        val anime = sampleAnime()
        val episode = sampleEpisode(1)
        val docId = repository.watchlistDocId(anime.id, episode.number)

        val added = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.toggleFavoriteEpisode(
                anime = anime,
                episode = episode,
                streamUrl = "https://archive.org/download/sample/frieren_ep1.mp4",
                watchedSeconds = 90L,
                durationSeconds = 1440L,
                note = "Great opening!"
            )
        }
        assertTrue(added.isSuccess)
        assertTrue(added.getOrThrow())

        val list = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeWatchlist(uid).first { items -> items.any { it.id == docId } }
        }
        val saved = list.first { it.id == docId }
        assertEquals(anime.title, saved.animeTitle)
        assertEquals(1L, saved.episodeNumber)
        assertEquals("Great opening!", saved.note)

        val noteUpdate = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateWatchlistNote(docId, "Updated episode note")
        }
        assertTrue(noteUpdate.isSuccess)

        val removed = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.toggleFavoriteEpisode(anime, episode)
        }
        assertTrue(removed.isSuccess)
        assertFalse(removed.getOrThrow())
    }

    @Test
    fun savePlaybackProgress_authenticatedOwner_createsAndUpdatesWatchHistory() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = WatchlistRepository(firestore, auth)
        val animeId = Math.abs(UUID.randomUUID().mostSignificantBits % 900000L) + 10L

        val firstSave = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.savePlaybackProgress(
                animeId = animeId,
                animeTitle = "Solo Leveling",
                animeImageUrl = "https://cdn.myanimelist.net/images/anime/1/1.jpg",
                episodeNumber = 1,
                episodeTitle = "I'm Used to It",
                streamUrl = "https://archive.org/download/sample/ep1.mp4",
                positionMs = 15000L,
                durationMs = 1440000L
            )
        }
        assertTrue(firstSave.isSuccess)

        val secondSave = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.savePlaybackProgress(
                animeId = animeId,
                animeTitle = "Solo Leveling",
                animeImageUrl = "https://cdn.myanimelist.net/images/anime/1/1.jpg",
                episodeNumber = 2,
                episodeTitle = "If I Had One More Chance",
                streamUrl = "https://archive.org/download/sample/ep2.mp4",
                positionMs = 45000L,
                durationMs = 1440000L
            )
        }
        assertTrue(secondSave.isSuccess)

        val history = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeWatchHistory(uid).first { list ->
                list.any { it.animeId == animeId && it.lastEpisodeNumber == 2L }
            }
        }
        assertTrue(history.any { it.animeId == animeId && it.positionMs == 45000L })
    }

    @Test
    fun getWatchlistEpisode_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = WatchlistRepository(firestore, auth)
        val anime = sampleAnime()
        val episode = sampleEpisode(3)
        val docId = aliceRepo.watchlistDocId(anime.id, episode.number)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.toggleFavoriteEpisode(anime, episode).getOrThrow()
        }

        // Sign in as Bob and try to read Alice's watchlist document directly
        signInTestUser(BOB_EMAIL)
        var caught: Throwable? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users")
                    .document(aliceUid)
                    .collection("watchlist_episodes")
                    .document(docId)
                    .get()
                    .let { task -> com.google.android.gms.tasks.Tasks.await(task) }
            }
        } catch (e: Throwable) {
            caught = e
        }
        val firestoreEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException for cross-user read", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeWatchlist_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = WatchlistRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeWatchlist(aliceUid).first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        val firestoreEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException for unauthenticated caller", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_manznime@test.com"
        const val BOB_EMAIL = "bob_manznime@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
