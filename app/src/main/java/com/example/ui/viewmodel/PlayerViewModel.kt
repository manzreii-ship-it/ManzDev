package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Anime
import com.example.data.model.AnimeEpisode
import com.example.data.model.StreamSource
import com.example.data.model.UiState
import com.example.data.repository.AnimeRepository
import com.example.data.repository.StreamResolverRepository
import com.example.data.repository.UserLibraryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiContent(
    val anime: Anime,
    val episodes: List<AnimeEpisode>,
    val currentEpisode: AnimeEpisode,
    val sources: List<StreamSource>,
    val selectedSource: StreamSource,
    val resumePositionMs: Long
)

class PlayerViewModel(
    private val animeId: Long,
    initialEpisodeNumber: Int,
    private val initialSeekMs: Long,
    private val animeRepository: AnimeRepository,
    private val streamResolverRepository: StreamResolverRepository,
    private val libraryRepository: UserLibraryRepository
) : ViewModel() {

    private val _playerState = MutableStateFlow<UiState<PlayerUiContent>>(UiState.Loading)
    val playerState: StateFlow<UiState<PlayerUiContent>> = _playerState.asStateFlow()

    init {
        loadEpisode(initialEpisodeNumber, initialSeekMs)
    }

    fun loadEpisode(episodeNumber: Int, seekMs: Long = 0L) {
        viewModelScope.launch {
            _playerState.value = UiState.Loading
            try {
                val (anime, episodes) = animeRepository.getAnimeDetailAndEpisodes(animeId)
                val targetEpisode = episodes.firstOrNull { it.number == episodeNumber }
                    ?: AnimeEpisode(number = episodeNumber, title = "Episode $episodeNumber")
                val sources = streamResolverRepository.resolveStreamsForEpisode(
                    animeId = anime.id,
                    animeTitle = anime.title,
                    episodeNumber = targetEpisode.number
                )
                val defaultSource = sources.first()
                _playerState.value = UiState.Success(
                    PlayerUiContent(
                        anime = anime,
                        episodes = episodes,
                        currentEpisode = targetEpisode,
                        sources = sources,
                        selectedSource = defaultSource,
                        resumePositionMs = seekMs
                    )
                )
                // Immediately record that this anime/episode was opened so Watch History updates right away
                libraryRepository.savePlaybackProgress(
                    animeId = anime.id,
                    animeTitle = anime.title,
                    animeImageUrl = anime.largeImageUrl.ifBlank { anime.imageUrl },
                    episodeNumber = targetEpisode.number,
                    episodeTitle = targetEpisode.title,
                    streamUrl = defaultSource.url,
                    positionMs = seekMs,
                    durationMs = targetEpisode.durationMinutes * 60_000L
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _playerState.value = UiState.Error(
                    e.localizedMessage ?: "Failed to load episode stream."
                )
            }
        }
    }

    fun selectSource(source: StreamSource, currentPosMs: Long) {
        _playerState.update { state ->
            if (state is UiState.Success) {
                state.copy(
                    data = state.data.copy(
                        selectedSource = source,
                        resumePositionMs = currentPosMs
                    )
                )
            } else {
                state
            }
        }
    }

    fun saveProgress(positionMs: Long, durationMs: Long) {
        val current = (_playerState.value as? UiState.Success)?.data ?: return
        val effectiveDurationMs = if (durationMs > 0L) {
            durationMs
        } else {
            current.currentEpisode.durationMinutes * 60_000L
        }
        viewModelScope.launch {
            libraryRepository.savePlaybackProgress(
                animeId = current.anime.id,
                animeTitle = current.anime.title,
                animeImageUrl = current.anime.largeImageUrl.ifBlank { current.anime.imageUrl },
                episodeNumber = current.currentEpisode.number,
                episodeTitle = current.currentEpisode.title,
                streamUrl = current.selectedSource.url,
                positionMs = positionMs,
                durationMs = effectiveDurationMs
            )
        }
    }
}
