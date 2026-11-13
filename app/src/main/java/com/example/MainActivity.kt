package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.model.UiState
import com.example.data.repository.AnimeRepository
import com.example.data.repository.FirestoreLibraryRepository
import com.example.data.repository.GuestLibraryRepository
import com.example.data.repository.GuestStorageRepository
import com.example.data.repository.PlayerPreferences
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.StreamResolverRepository
import com.example.data.repository.UserLibraryRepository
import com.example.data.repository.WatchlistRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.AnimeDetailScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.WatchlistScreen
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.ManznimeTheme
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.AnimeDetailViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PlayerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object ExploreRoute
@Serializable data object WatchlistRoute
@Serializable data object HistoryRoute
@Serializable data object ProfileRoute
@Serializable data class AnimeDetailRoute(val animeId: Long)
@Serializable data class PlayerRoute(
    val animeId: Long,
    val episodeNumber: Int,
    val resumePositionMs: Long = 0L
)

private data class TopTabItem(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TOP_TABS = listOf(
    TopTabItem(HomeRoute, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
    TopTabItem(ExploreRoute, R.string.nav_explore, Icons.Filled.Explore, Icons.Outlined.Explore, "tab_explore"),
    TopTabItem(WatchlistRoute, R.string.nav_watchlist, Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "tab_watchlist"),
    TopTabItem(HistoryRoute, R.string.nav_history, Icons.Filled.History, Icons.Outlined.History, "tab_history"),
    TopTabItem(ProfileRoute, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person, "tab_profile")
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ManznimeTheme {
                ManznimeAppRoot()
            }
        }
    }
}

@Composable
fun ManznimeAppRoot(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesRepository = remember(context) {
        PreferencesRepository(context.applicationContext)
    }
    val preferences by preferencesRepository.preferencesFlow
        .collectAsStateWithLifecycle(initialValue = PlayerPreferences())
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    when {
        user != null -> {
            key(user.uid) {
                ManznimeSessionApp(
                    sessionKey = user.uid,
                    isGuestMode = false,
                    preferencesRepository = preferencesRepository
                )
            }
        }

        preferences.isGuestMode -> {
            key("guest_mode") {
                ManznimeSessionApp(
                    sessionKey = "guest_mode",
                    isGuestMode = true,
                    preferencesRepository = preferencesRepository
                )
            }
        }

        else -> {
            AuthScreen(
                onAuthSuccess = {
                    scope.launch { preferencesRepository.setGuestMode(false) }
                },
                onContinueAsGuest = {
                    scope.launch { preferencesRepository.setGuestMode(true) }
                }
            )
        }
    }
}

@Composable
fun ManznimeSessionApp(
    sessionKey: String,
    isGuestMode: Boolean,
    preferencesRepository: PreferencesRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val animeRepository = remember { AnimeRepository() }
    val streamResolverRepository = remember { StreamResolverRepository() }
    val libraryRepository: UserLibraryRepository = remember(sessionKey, isGuestMode) {
        if (isGuestMode) {
            GuestLibraryRepository(GuestStorageRepository(context.applicationContext))
        } else {
            val databaseId = context.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            FirestoreLibraryRepository(WatchlistRepository(db), sessionKey)
        }
    }

    val mainViewModel: MainViewModel = viewModel(
        key = sessionKey,
        factory = viewModelFactory {
            initializer {
                MainViewModel(
                    currentSessionId = sessionKey,
                    animeRepository = animeRepository,
                    libraryRepository = libraryRepository,
                    preferencesRepository = preferencesRepository
                )
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isTopLevelTab = currentDestination?.let { dest ->
        dest.hasRoute<HomeRoute>() ||
            dest.hasRoute<ExploreRoute>() ||
            dest.hasRoute<WatchlistRoute>() ||
            dest.hasRoute<HistoryRoute>() ||
            dest.hasRoute<ProfileRoute>()
    } ?: true

    val homeFeedState by mainViewModel.homeFeedState.collectAsStateWithLifecycle()
    val exploreFilter by mainViewModel.exploreFilter.collectAsStateWithLifecycle()
    val exploreResultsState by mainViewModel.exploreResultsState.collectAsStateWithLifecycle()
    val genres by mainViewModel.genres.collectAsStateWithLifecycle()
    val watchlistState by mainViewModel.watchlistState.collectAsStateWithLifecycle()
    val watchHistoryState by mainViewModel.watchHistoryState.collectAsStateWithLifecycle()
    val preferences by mainViewModel.playerPreferences.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        mainViewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navigateToTab: (Any) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val watchlistItems = (watchlistState as? UiState.Success)?.data.orEmpty()
    val historyItems = (watchHistoryState as? UiState.Success)?.data.orEmpty()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianNight)
    ) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianNight,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (isTopLevelTab && !useNavigationRail) {
                    NavigationBar(
                        containerColor = CharcoalSurface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        TOP_TABS.forEach { tab ->
                            val selected = currentDestination?.let { dest ->
                                when (tab.route) {
                                    is HomeRoute -> dest.hasRoute<HomeRoute>()
                                    is ExploreRoute -> dest.hasRoute<ExploreRoute>()
                                    is WatchlistRoute -> dest.hasRoute<WatchlistRoute>()
                                    is HistoryRoute -> dest.hasRoute<HistoryRoute>()
                                    is ProfileRoute -> dest.hasRoute<ProfileRoute>()
                                    else -> false
                                }
                            } ?: false

                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(id = tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(id = tab.labelRes),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricCrimson,
                                    selectedTextColor = Color.White,
                                    indicatorColor = ElectricCrimson.copy(alpha = 0.16f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevelTab && useNavigationRail) {
                    NavigationRail(
                        containerColor = CharcoalSurface,
                        windowInsets = WindowInsets.statusBars
                    ) {
                        TOP_TABS.forEach { tab ->
                            val selected = currentDestination?.let { dest ->
                                when (tab.route) {
                                    is HomeRoute -> dest.hasRoute<HomeRoute>()
                                    is ExploreRoute -> dest.hasRoute<ExploreRoute>()
                                    is WatchlistRoute -> dest.hasRoute<WatchlistRoute>()
                                    is HistoryRoute -> dest.hasRoute<HistoryRoute>()
                                    is ProfileRoute -> dest.hasRoute<ProfileRoute>()
                                    else -> false
                                }
                            } ?: false

                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(id = tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(id = tab.labelRes),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ElectricCrimson,
                                    selectedTextColor = Color.White,
                                    indicatorColor = ElectricCrimson.copy(alpha = 0.16f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = HomeRoute
                    ) {
                        composable<HomeRoute> {
                            HomeScreen(
                                homeFeedState = homeFeedState,
                                watchHistoryState = watchHistoryState,
                                onRetry = mainViewModel::refreshHome,
                                onAnimeClick = { animeId ->
                                    navController.navigate(AnimeDetailRoute(animeId))
                                },
                                onWatchEpisodeClick = { animeId, ep, resumeMs ->
                                    navController.navigate(PlayerRoute(animeId, ep, resumeMs))
                                },
                                onGenreClick = { genreId ->
                                    mainViewModel.selectSingleGenreAndExplore(genreId)
                                    navigateToTab(ExploreRoute)
                                },
                                onSearchIconClick = {
                                    navigateToTab(ExploreRoute)
                                },
                                onSeeAllHistoryClick = {
                                    navigateToTab(HistoryRoute)
                                }
                            )
                        }

                        composable<ExploreRoute> {
                            ExploreScreen(
                                filter = exploreFilter,
                                genres = genres,
                                resultsState = exploreResultsState,
                                onQueryChange = mainViewModel::updateSearchQuery,
                                onSearchSubmit = mainViewModel::submitSearchNow,
                                onToggleGenre = mainViewModel::toggleGenreFilter,
                                onTypeSelected = mainViewModel::updateTypeFilter,
                                onStatusSelected = mainViewModel::updateStatusFilter,
                                onOrderBySelected = mainViewModel::updateOrderBy,
                                onClearFilters = mainViewModel::clearExploreFilters,
                                onAnimeClick = { animeId ->
                                    navController.navigate(AnimeDetailRoute(animeId))
                                }
                            )
                        }

                        composable<WatchlistRoute> {
                            WatchlistScreen(
                                watchlistState = watchlistState,
                                onPlayEpisode = { animeId, ep, resumeMs ->
                                    navController.navigate(PlayerRoute(animeId, ep, resumeMs))
                                },
                                onAnimeClick = { animeId ->
                                    navController.navigate(AnimeDetailRoute(animeId))
                                },
                                onUpdateNote = mainViewModel::updateWatchlistNote,
                                onRemoveEpisode = mainViewModel::removeWatchlistEpisode,
                                onExploreClick = { navigateToTab(ExploreRoute) },
                                onRetry = mainViewModel::refreshHome
                            )
                        }

                        composable<HistoryRoute> {
                            HistoryScreen(
                                historyState = watchHistoryState,
                                isGuestMode = isGuestMode,
                                onResumeEpisode = { animeId, ep, resumeMs ->
                                    navController.navigate(PlayerRoute(animeId, ep, resumeMs))
                                },
                                onAnimeClick = { animeId ->
                                    navController.navigate(AnimeDetailRoute(animeId))
                                },
                                onRemoveHistoryEntry = mainViewModel::removeWatchHistoryEntry,
                                onClearAllHistory = mainViewModel::clearAllWatchHistory,
                                onExploreClick = { navigateToTab(ExploreRoute) },
                                onRetry = mainViewModel::refreshHome
                            )
                        }

                        composable<ProfileRoute> {
                            ProfileScreen(
                                isGuestMode = isGuestMode,
                                watchlistCount = watchlistItems.size,
                                historyCount = historyItems.size,
                                preferences = preferences,
                                onSpeedChanged = mainViewModel::setDefaultPlaybackSpeed,
                                onAutoPlayChanged = mainViewModel::setAutoPlayNext,
                                onExitGuestMode = mainViewModel::exitGuestMode,
                                onSignedInFromGuest = {
                                    scope.launch { preferencesRepository.setGuestMode(false) }
                                }
                            )
                        }

                        composable<AnimeDetailRoute> { entry ->
                            val route = entry.toRoute<AnimeDetailRoute>()
                            val detailViewModel: AnimeDetailViewModel = viewModel(
                                key = "detail_${route.animeId}",
                                factory = viewModelFactory {
                                    initializer {
                                        AnimeDetailViewModel(
                                            animeId = route.animeId,
                                            animeRepository = animeRepository
                                        )
                                    }
                                }
                            )
                            val detailState by detailViewModel.detailState.collectAsStateWithLifecycle()

                            AnimeDetailScreen(
                                detailState = detailState,
                                watchlistItems = watchlistItems,
                                onBack = { navController.popBackStack() },
                                onRetry = detailViewModel::loadDetail,
                                onPlayEpisode = { animeId, ep ->
                                    navController.navigate(PlayerRoute(animeId, ep, 0L))
                                },
                                onToggleEpisodeFavorite = { anime, ep ->
                                    mainViewModel.toggleFavoriteEpisode(anime, ep)
                                },
                                onGenreClick = { genreId ->
                                    mainViewModel.selectSingleGenreAndExplore(genreId)
                                    navigateToTab(ExploreRoute)
                                }
                            )
                        }

                        composable<PlayerRoute> { entry ->
                            val route = entry.toRoute<PlayerRoute>()
                            val playerViewModel: PlayerViewModel = viewModel(
                                key = "player_${sessionKey}_${route.animeId}_${route.episodeNumber}",
                                factory = viewModelFactory {
                                    initializer {
                                        PlayerViewModel(
                                            animeId = route.animeId,
                                            initialEpisodeNumber = route.episodeNumber,
                                            initialSeekMs = route.resumePositionMs,
                                            animeRepository = animeRepository,
                                            streamResolverRepository = streamResolverRepository,
                                            libraryRepository = libraryRepository
                                        )
                                    }
                                }
                            )
                            val playerState by playerViewModel.playerState.collectAsStateWithLifecycle()

                            PlayerScreen(
                                playerState = playerState,
                                watchlistItems = watchlistItems,
                                defaultSpeed = preferences.defaultSpeed,
                                autoPlayNext = preferences.autoPlayNextEpisode,
                                onBack = { navController.popBackStack() },
                                onRetry = { playerViewModel.loadEpisode(route.episodeNumber, route.resumePositionMs) },
                                onSelectEpisode = { epNum -> playerViewModel.loadEpisode(epNum, 0L) },
                                onSelectSource = { src, posMs -> playerViewModel.selectSource(src, posMs) },
                                onSaveProgress = { posMs, durMs -> playerViewModel.saveProgress(posMs, durMs) },
                                onToggleFavoriteEpisode = { anime, ep, url ->
                                    mainViewModel.toggleFavoriteEpisode(anime, ep, url)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
