package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.model.Anime
import com.example.data.model.UiState
import com.example.data.model.WatchHistoryEntry
import com.example.data.repository.HomeFeedData
import com.example.ui.components.AnimePosterCard
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.ErrorStateBox
import com.example.ui.components.GenreChipPill
import com.example.ui.components.LoadingStateBox
import com.example.ui.components.SectionHeader
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.StarGold
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    homeFeedState: UiState<HomeFeedData>,
    watchHistoryState: UiState<List<WatchHistoryEntry>>,
    onRetry: () -> Unit,
    onAnimeClick: (Long) -> Unit,
    onWatchEpisodeClick: (animeId: Long, episodeNumber: Int, resumeMs: Long) -> Unit,
    onGenreClick: (Int) -> Unit,
    onSearchIconClick: () -> Unit,
    onSeeAllHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianNight)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Top Brand Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ElectricCrimson,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "HD ANIME STREAM",
                            style = MaterialTheme.typography.labelSmall,
                            color = LuminousCyan
                        )
                    }
                }

                IconButton(
                    onClick = onSearchIconClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("home_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search anime",
                        tint = Color.White
                    )
                }
            }
        }

        when (homeFeedState) {
            is UiState.Loading -> {
                item {
                    LoadingStateBox(
                        message = "Fetching seasonal anime catalog…",
                        modifier = Modifier.height(340.dp)
                    )
                }
            }

            is UiState.Error -> {
                item {
                    ErrorStateBox(
                        message = homeFeedState.message,
                        onRetry = onRetry
                    )
                }
            }

            is UiState.Success -> {
                val feed = homeFeedState.data

                // Hero Featured Carousel
                if (feed.featuredAiring.isNotEmpty()) {
                    item {
                        FeaturedHeroCarousel(
                            featuredList = feed.featuredAiring.take(5),
                            onWatchClick = { anime -> onWatchEpisodeClick(anime.id, 1, 0L) },
                            onDetailClick = { anime -> onAnimeClick(anime.id) }
                        )
                    }
                }

                // Continue Watching from Firestore
                val historyItems = (watchHistoryState as? UiState.Success)?.data.orEmpty()
                if (historyItems.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader(
                                title = "Continue Watching",
                                subtitle = "Pick up right where you left off",
                                actionLabel = "All History",
                                onActionClick = onSeeAllHistoryClick
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(historyItems, key = { it.id }) { entry ->
                                    ContinueWatchingCard(
                                        entry = entry,
                                        onClick = {
                                            onWatchEpisodeClick(
                                                entry.animeId,
                                                entry.lastEpisodeNumber.toInt(),
                                                entry.positionMs
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Genre Chips
                if (feed.genres.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionHeader(
                                title = "Genres",
                                actionLabel = "All Filters",
                                onActionClick = onSearchIconClick
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(feed.genres.take(15), key = { it.id }) { genre ->
                                    GenreChipPill(
                                        label = genre.name,
                                        selected = false,
                                        onClick = { onGenreClick(genre.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Airing This Season
                if (feed.featuredAiring.isNotEmpty()) {
                    item {
                        AnimeHorizontalSection(
                            title = "Ongoing This Season",
                            subtitle = "Fresh simulcast episodes",
                            animeList = feed.featuredAiring,
                            onAnimeClick = onAnimeClick,
                            onSeeAllClick = onSearchIconClick
                        )
                    }
                }

                // Most Popular
                if (feed.topPopular.isNotEmpty()) {
                    item {
                        AnimeHorizontalSection(
                            title = "Popular on Manznime",
                            subtitle = "All-time fan favorites",
                            animeList = feed.topPopular,
                            onAnimeClick = onAnimeClick,
                            onSeeAllClick = onSearchIconClick
                        )
                    }
                }

                // Top Anime Movies
                if (feed.topMovies.isNotEmpty()) {
                    item {
                        AnimeHorizontalSection(
                            title = "Anime Movies",
                            subtitle = "Feature-length theatrical hits",
                            animeList = feed.topMovies,
                            onAnimeClick = onAnimeClick,
                            onSeeAllClick = onSearchIconClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedHeroCarousel(
    featuredList: List<Anime>,
    onWatchClick: (Anime) -> Unit,
    onDetailClick: (Anime) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { featuredList.size })

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp
        ) { page ->
            val anime = featuredList[page]
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onDetailClick(anime) }
                    .testTag("hero_card_${anime.id}"),
                shape = RoundedCornerShape(20.dp),
                color = CharcoalSurface,
                border = BorderStroke(1.dp, HairlineBorder)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SubcomposeAsyncImage(
                        model = anime.largeImageUrl.ifBlank { anime.imageUrl },
                        contentDescription = "${anime.title} featured banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        ObsidianNight.copy(alpha = 0.15f),
                                        ObsidianNight.copy(alpha = 0.55f),
                                        ObsidianNight.copy(alpha = 0.96f)
                                    )
                                )
                            )
                    )

                    // Top badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = ElectricCrimson
                        ) {
                            Text(
                                text = "FEATURED #${page + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        anime.score?.let { score ->
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = ObsidianNight.copy(alpha = 0.8f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = StarGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format("%.2f", score),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Bottom info + CTA buttons
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = anime.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = anime.genres.take(3).joinToString(" • ") { it.name },
                            style = MaterialTheme.typography.bodySmall,
                            color = LuminousCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onWatchClick(anime) },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCrimson),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Watch Now", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onDetailClick(anime) },
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Episodes", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Pager dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(featuredList.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (isSelected) 20.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) ElectricCrimson else ElevatedCard)
                )
            }
        }
    }
}

@Composable
private fun AnimeHorizontalSection(
    title: String,
    subtitle: String,
    animeList: List<Anime>,
    onAnimeClick: (Long) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = title,
            subtitle = subtitle,
            actionLabel = "Explore",
            onActionClick = onSeeAllClick
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(animeList, key = { it.id }) { anime ->
                AnimePosterCard(
                    anime = anime,
                    onClick = { onAnimeClick(anime.id) },
                    modifier = Modifier.width(142.dp)
                )
            }
        }
    }
}
