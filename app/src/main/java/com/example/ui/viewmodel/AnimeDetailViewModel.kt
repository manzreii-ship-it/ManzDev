package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Anime
import com.example.data.model.AnimeEpisode
import com.example.data.model.UiState
import com.example.data.repository.AnimeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnimeDetailData(
    val anime: Anime,
    val episodes: List<AnimeEpisode>
)

class AnimeDetailViewModel(
    private val animeId: Long,
    private val animeRepository: AnimeRepository
) : ViewModel() {

    private val _detailState = MutableStateFlow<UiState<AnimeDetailData>>(UiState.Loading)
    val detailState: StateFlow<UiState<AnimeDetailData>> = _detailState.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            _detailState.value = UiState.Loading
            try {
                val (anime, episodes) = animeRepository.getAnimeDetailAndEpisodes(animeId)
                _detailState.value = UiState.Success(AnimeDetailData(anime, episodes))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _detailState.value = UiState.Error(
                    e.localizedMessage ?: "Failed to load anime details."
                )
            }
        }
    }
}
