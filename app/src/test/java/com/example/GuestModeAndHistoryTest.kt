package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Anime
import com.example.data.model.AnimeEpisode
import com.example.data.model.AnimeGenre
import com.example.data.repository.GuestLibraryRepository
import com.example.data.repository.GuestStorageRepository
import com.example.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GuestModeAndHistoryTest {

    @Test
    fun `guest mode preference toggles cleanly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesRepository(context)

        assertFalse(prefs.preferencesFlow.first().isGuestMode)
        prefs.setGuestMode(true)
        assertTrue(prefs.preferencesFlow.first().isGuestMode)
        prefs.setGuestMode(false)
        assertFalse(prefs.preferencesFlow.first().isGuestMode)
    }

    @Test
    fun `guest library persists watchlist and watch history locally`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = GuestStorageRepository(context)
        val library = GuestLibraryRepository(storage)

        val anime1 = Anime(
            id = 5114L,
            title = "Fullmetal Alchemist: Brotherhood",
            genres = listOf(AnimeGenre(1, "Action"))
        )
        val ep1 = AnimeEpisode(number = 1, title = "Fullmetal Alchemist")

        // Watchlist toggle add -> update note -> remove
        val added = library.toggleFavoriteEpisode(anime1, ep1, "https://example.com/ep1.mp4")
        assertTrue(added.getOrThrow())
        assertEquals(1, library.observeWatchlist().first().size)

        library.updateWatchlistNote("5114_ep_1", "Peak opening")
        assertEquals("Peak opening", library.observeWatchlist().first().first().note)

        // Watch history record + update + delete single + clear all
        library.savePlaybackProgress(
            animeId = 5114L,
            animeTitle = "Fullmetal Alchemist: Brotherhood",
            animeImageUrl = "",
            episodeNumber = 1,
            episodeTitle = "Fullmetal Alchemist",
            streamUrl = "https://example.com/ep1.mp4",
            positionMs = 120_000L,
            durationMs = 1_440_000L
        )
        library.savePlaybackProgress(
            animeId = 21L,
            animeTitle = "One Piece",
            animeImageUrl = "",
            episodeNumber = 1071,
            episodeTitle = "Luffy's Peak",
            streamUrl = "https://example.com/op1071.mp4",
            positionMs = 45_000L,
            durationMs = 1_440_000L
        )

        val history = library.observeWatchHistory().first()
        assertEquals(2, history.size)
        assertTrue(history.any { it.animeId == 5114L && it.positionMs == 120_000L })
        assertTrue(history.any { it.animeId == 21L && it.lastEpisodeNumber == 1071L })

        library.removeWatchHistoryEntry("anime_5114")
        val afterRemove = library.observeWatchHistory().first()
        assertEquals(1, afterRemove.size)
        assertEquals(21L, afterRemove.first().animeId)

        library.clearWatchHistory()
        assertTrue(library.observeWatchHistory().first().isEmpty())

        // Watchlist remained intact after clearing history
        assertEquals(1, library.observeWatchlist().first().size)
    }
}
