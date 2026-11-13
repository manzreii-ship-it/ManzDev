package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.Anime
import com.example.data.model.AnimeEpisode
import com.example.data.model.UiState
import com.example.data.model.WatchlistEpisode
import com.example.ui.components.ErrorStateBox
import com.example.ui.components.GenreChipPill
import com.example.ui.components.LoadingStateBox
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.StarGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AnimeDetailData

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimeDetailScreen(
    detailState: UiState<AnimeDetailData>,
    watchlistItems: List<WatchlistEpisode>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlayEpisode: (animeId: Long, episodeNumber: Int) -> Unit,
    onToggleEpisodeFavorite: (Anime, AnimeEpisode) -> Unit,
    onGenreClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val savedEpisodeNumbers = remember(watchlistItems, detailState) {
        val animeId = (detailState as? UiState.Success)?.data?.anime?.id
        watchlistItems.filter { it.animeId == animeId }.map { it.episodeNumber.toInt() }.toSet()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianNight)
    ) {
        when (detailState) {
            is UiState.Loading -> {
                LoadingStateBox(
                    message = "Loading anime & episode list…",
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is UiState.Error -> {
                ErrorStateBox(
                    message = detailState.message,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is UiState.Success -> {
                val anime = detailState.data.anime
                val episodes = detailState.data.episodes
                var synopsisExpanded by remember { mutableStateOf(false) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Hero Poster Banner
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                        ) {
                            SubcomposeAsyncImage(
                                model = anime.largeImageUrl.ifBlank { anime.imageUrl },
                                contentDescription = anime.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                ObsidianNight.copy(alpha = 0.45f),
                                                ObsidianNight.copy(alpha = 0.35f),
                                                ObsidianNight.copy(alpha = 0.96f),
                                                ObsidianNight
                                            )
                                        )
                                    )
                            )

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = ElectricCrimson
                                    ) {
                                        Text(
                                            text = anime.type.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    anime.score?.let { score ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = CharcoalSurface
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Star,
                                                    contentDescription = null,
                                                    tint = StarGold,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = String.format("%.2f", score),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                    if (anime.status.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = CharcoalSurface
                                        ) {
                                            Text(
                                                text = anime.status,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LuminousCyan,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = anime.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White
                                )
                                if (anime.titleJapanese.isNotBlank()) {
                                    Text(
                                        text = anime.titleJapanese,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Primary Watch CTA + Metadata + Genres + Synopsis
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Button(
                                onClick = {
                                    val firstEp = episodes.firstOrNull()?.number ?: 1
                                    onPlayEpisode(anime.id, firstEp)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCrimson),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("detail_watch_ep1_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Watch Episode ${episodes.firstOrNull()?.number ?: 1} In-App",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Studio / Duration / Year strip
                            val metaBits = listOfNotNull(
                                anime.studio.takeIf { it.isNotBlank() },
                                anime.year?.toString(),
                                anime.duration.takeIf { it.isNotBlank() }
                            ).joinToString("  •  ")
                            if (metaBits.isNotBlank()) {
                                Text(
                                    text = metaBits,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            // Clickable Genre Chips
                            if (anime.genres.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    anime.genres.forEach { genre ->
                                        GenreChipPill(
                                            label = genre.name,
                                            selected = false,
                                            onClick = { onGenreClick(genre.id) }
                                        )
                                    }
                                }
                            }

                            // Synopsis
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Synopsis",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = anime.synopsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    maxLines = if (synopsisExpanded) Int.MAX_VALUE else 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (anime.synopsis.length > 160) {
                                    TextButton(
                                        onClick = { synopsisExpanded = !synopsisExpanded },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = if (synopsisExpanded) "Show Less" else "Read More",
                                            color = LuminousCyan
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Episodes (${episodes.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tap bookmark to save favorite",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    // Episodes List
                    items(episodes, key = { it.number }) { episode ->
                        val isSaved = savedEpisodeNumbers.contains(episode.number)
                        EpisodeRowItem(
                            episode = episode,
                            isSaved = isSaved,
                            onPlay = { onPlayEpisode(anime.id, episode.number) },
                            onToggleFavorite = { onToggleEpisodeFavorite(anime, episode) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Top Floating Back Button
        Surface(
            shape = CircleShape,
            color = ObsidianNight.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier
                .statusBarsPadding()
                .padding(14.dp)
                .align(Alignment.TopStart)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("detail_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun EpisodeRowItem(
    episode: AnimeEpisode,
    isSaved: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ElevatedCard,
        border = BorderStroke(
            1.dp,
            if (isSaved) ElectricCrimson.copy(alpha = 0.55f) else HairlineBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onPlay)
            .testTag("episode_row_${episode.number}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CharcoalSurface,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = episode.number.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.titleMedium,
                        color = LuminousCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = buildList {
                    if (episode.aired.isNotBlank()) add(episode.aired)
                    if (episode.filler) add("Filler")
                    if (episode.recap) add("Recap")
                    if (isEmpty()) add("${episode.durationMinutes} min")
                }.joinToString(" • ")
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("bookmark_ep_${episode.number}")
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = if (isSaved) "Remove episode from watchlist" else "Save episode to watchlist",
                    tint = if (isSaved) ElectricCrimson else TextSecondary
                )
            }

            Surface(
                shape = CircleShape,
                color = ElectricCrimson,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play episode ${episode.number}",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
