package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Anime
import com.example.data.model.AnimeEpisode
import com.example.data.model.AnimeFilter
import com.example.data.model.AnimeGenre
import com.example.data.model.UiState
import com.example.data.model.WatchHistoryEntry
import com.example.data.model.WatchlistEpisode
import com.example.data.repository.AnimeRepository
import com.example.data.repository.HomeFeedData
import com.example.data.repository.PlayerPreferences
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.UserLibraryRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val currentSessionId: String,
    private val animeRepository: AnimeRepository,
    private val libraryRepository: UserLibraryRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _homeFeedState = MutableStateFlow<UiState<HomeFeedData>>(UiState.Loading)
    val homeFeedState: StateFlow<UiState<HomeFeedData>> = _homeFeedState.asStateFlow()

    private val _exploreFilter = MutableStateFlow(AnimeFilter())
    val exploreFilter: StateFlow<AnimeFilter> = _exploreFilter.asStateFlow()

    private val _exploreResultsState = MutableStateFlow<UiState<List<Anime>>>(UiState.Loading)
    val exploreResultsState: StateFlow<UiState<List<Anime>>> = _exploreResultsState.asStateFlow()

    private val _genres = MutableStateFlow<List<AnimeGenre>>(emptyList())
    val genres: StateFlow<List<AnimeGenre>> = _genres.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    val watchlistState: StateFlow<UiState<List<WatchlistEpisode>>> =
        libraryRepository.observeWatchlist()
            .map<List<WatchlistEpisode>, UiState<List<WatchlistEpisode>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing watchlist for $currentSessionId", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load watchlist"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val watchHistoryState: StateFlow<UiState<List<WatchHistoryEntry>>> =
        libraryRepository.observeWatchHistory()
            .map<List<WatchHistoryEntry>, UiState<List<WatchHistoryEntry>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing watch history for $currentSessionId", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load watch history"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val playerPreferences: StateFlow<PlayerPreferences> =
        preferencesRepository.preferencesFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = PlayerPreferences()
        )

    private var searchJob: Job? = null

    init {
        refreshHome()
        executeSearch(immediate = true)
    }

    fun refreshHome() {
        viewModelScope.launch {
            _homeFeedState.value = UiState.Loading
            try {
                val feed = animeRepository.getHomeFeed()
                _genres.value = feed.genres
                _homeFeedState.value = UiState.Success(feed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load home feed", e)
                _homeFeedState.value = UiState.Error(
                    e.localizedMessage ?: "Unable to connect to anime catalog. Tap Retry."
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _exploreFilter.update { it.copy(query = query) }
        executeSearch(immediate = false)
    }

    fun submitSearchNow() {
        executeSearch(immediate = true)
    }

    fun toggleGenreFilter(genreId: Int) {
        _exploreFilter.update { current ->
            val updated = current.selectedGenreIds.toMutableSet()
            if (!updated.add(genreId)) {
                updated.remove(genreId)
            }
            current.copy(selectedGenreIds = updated)
        }
        executeSearch(immediate = true)
    }

    fun selectSingleGenreAndExplore(genreId: Int) {
        _exploreFilter.value = AnimeFilter(selectedGenreIds = setOf(genreId))
        executeSearch(immediate = true)
    }

    fun updateTypeFilter(type: String?) {
        _exploreFilter.update { it.copy(type = type) }
        executeSearch(immediate = true)
    }

    fun updateStatusFilter(status: String?) {
        _exploreFilter.update { it.copy(status = status) }
        executeSearch(immediate = true)
    }

    fun updateOrderBy(orderBy: String) {
        _exploreFilter.update { it.copy(orderBy = orderBy) }
        executeSearch(immediate = true)
    }

    fun clearExploreFilters() {
        _exploreFilter.value = AnimeFilter()
        executeSearch(immediate = true)
    }

    private fun executeSearch(immediate: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (!immediate) delay(380L)
            _exploreResultsState.value = UiState.Loading
            try {
                if (_genres.value.isEmpty()) {
                    _genres.value = animeRepository.getGenres()
                }
                val results = animeRepository.searchAnime(_exploreFilter.value)
                _exploreResultsState.value = UiState.Success(results)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Search failed", e)
                _exploreResultsState.value = UiState.Error(
                    e.localizedMessage ?: "Search failed. Check connection and try again."
                )
            }
        }
    }

    fun toggleFavoriteEpisode(
        anime: Anime,
        episode: AnimeEpisode,
        streamUrl: String = ""
    ) {
        viewModelScope.launch {
            val result = libraryRepository.toggleFavoriteEpisode(
                anime = anime,
                episode = episode,
                streamUrl = streamUrl,
                durationSeconds = episode.durationMinutes * 60L
            )
            result.onSuccess { added ->
                _snackbarMessage.tryEmit(
                    if (added) {
                        "Saved Ep ${episode.number} of ${anime.title} to Watchlist"
                    } else {
                        "Removed Ep ${episode.number} from Watchlist"
                    }
                )
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "Failed to update watchlist")
            }
        }
    }

    fun updateWatchlistNote(entryId: String, note: String) {
        viewModelScope.launch {
            libraryRepository.updateWatchlistNote(entryId, note)
                .onSuccess { _snackbarMessage.tryEmit("Episode note saved") }
                .onFailure { _snackbarMessage.tryEmit(it.localizedMessage ?: "Failed to save note") }
        }
    }

    fun removeWatchlistEpisode(entryId: String) {
        viewModelScope.launch {
            libraryRepository.removeWatchlistEpisode(entryId)
                .onSuccess { _snackbarMessage.tryEmit("Removed from Watchlist") }
                .onFailure { _snackbarMessage.tryEmit(it.localizedMessage ?: "Failed to remove item") }
        }
    }

    fun removeWatchHistoryEntry(historyId: String) {
        viewModelScope.launch {
            libraryRepository.removeWatchHistoryEntry(historyId)
                .onSuccess { _snackbarMessage.tryEmit("Removed from Watch History") }
                .onFailure { _snackbarMessage.tryEmit(it.localizedMessage ?: "Failed to remove history") }
        }
    }

    fun clearAllWatchHistory() {
        viewModelScope.launch {
            libraryRepository.clearWatchHistory()
                .onSuccess { _snackbarMessage.tryEmit("Watch History cleared") }
                .onFailure { _snackbarMessage.tryEmit(it.localizedMessage ?: "Failed to clear history") }
        }
    }

    fun setDefaultPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            preferencesRepository.setDefaultSpeed(speed)
        }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoPlayNextEpisode(enabled)
        }
    }

    fun exitGuestMode() {
        viewModelScope.launch {
            preferencesRepository.setGuestMode(false)
        }
    }

    private companion object {
        const val TAG = "MainViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
