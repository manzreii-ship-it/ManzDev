package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.model.UiState
import com.example.data.model.WatchlistEpisode
import com.example.ui.components.ErrorStateBox
import com.example.ui.components.GenreChipPill
import com.example.ui.components.LoadingStateBox
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.GlassOverlay
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

private enum class WatchlistTab(val label: String) {
    ALL("All"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed")
}

@Composable
fun WatchlistScreen(
    watchlistState: UiState<List<WatchlistEpisode>>,
    onPlayEpisode: (animeId: Long, episodeNumber: Int, resumeMs: Long) -> Unit,
    onAnimeClick: (Long) -> Unit,
    onUpdateNote: (entryId: String, note: String) -> Unit,
    onRemoveEpisode: (entryId: String) -> Unit,
    onExploreClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(WatchlistTab.ALL) }
    var filterQuery by remember { mutableStateOf("") }
    var editingNoteItem by remember { mutableStateOf<WatchlistEpisode?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianNight)
            .statusBarsPadding()
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Favorite Episodes",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    val count = (watchlistState as? UiState.Success)?.data?.size ?: 0
                    Text(
                        text = "$count saved episodes synced to cloud",
                        style = MaterialTheme.typography.bodySmall,
                        color = LuminousCyan
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = null,
                    tint = ElectricCrimson,
                    modifier = Modifier.size(28.dp)
                )
            }

            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                placeholder = {
                    Text(
                        text = "Filter saved episodes…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = LuminousCyan
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CharcoalSurface,
                    unfocusedContainerColor = CharcoalSurface,
                    focusedBorderColor = ElectricCrimson,
                    unfocusedBorderColor = HairlineBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("watchlist_search_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WatchlistTab.entries.forEach { tab ->
                    GenreChipPill(
                        label = tab.label,
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab }
                    )
                }
            }
        }

        when (watchlistState) {
            is UiState.Loading -> {
                LoadingStateBox(
                    message = "Syncing your episode watchlist…",
                    modifier = Modifier.weight(1f)
                )
            }

            is UiState.Error -> {
                ErrorStateBox(
                    message = watchlistState.message,
                    onRetry = onRetry,
                    modifier = Modifier.weight(1f)
                )
            }

            is UiState.Success -> {
                val filtered = remember(watchlistState.data, selectedTab, filterQuery) {
                    watchlistState.data.filter { item ->
                        val matchesTab = when (selectedTab) {
                            WatchlistTab.ALL -> true
                            WatchlistTab.IN_PROGRESS -> item.watchedSeconds > 0L && !item.isCompleted
                            WatchlistTab.COMPLETED -> item.isCompleted
                        }
                        val q = filterQuery.trim().lowercase()
                        val matchesQuery = q.isBlank() ||
                            item.animeTitle.lowercase().contains(q) ||
                            item.episodeTitle.lowercase().contains(q) ||
                            item.note.lowercase().contains(q)
                        matchesTab && matchesQuery
                    }
                }

                if (watchlistState.data.isEmpty()) {
                    EmptyWatchlistView(
                        onExploreClick = onExploreClick,
                        modifier = Modifier.weight(1f)
                    )
                } else if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved episodes match your current filter.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            WatchlistEpisodeCard(
                                item = item,
                                onPlay = {
                                    onPlayEpisode(
                                        item.animeId,
                                        item.episodeNumber.toInt(),
                                        item.watchedSeconds * 1000L
                                    )
                                },
                                onOpenDetail = { onAnimeClick(item.animeId) },
                                onEditNote = { editingNoteItem = item },
                                onRemove = { onRemoveEpisode(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    editingNoteItem?.let { item ->
        var noteText by remember(item.id) { mutableStateOf(item.note) }
        AlertDialog(
            onDismissRequest = { editingNoteItem = null },
            containerColor = CharcoalSurface,
            title = {
                Text(
                    text = "Episode ${item.episodeNumber} Note",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = item.animeTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = LuminousCyan
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { if (it.length <= 300) noteText = it },
                        label = { Text("Why this episode is a favorite") },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCrimson,
                            unfocusedBorderColor = HairlineBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateNote(item.id, noteText.trim())
                        editingNoteItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCrimson)
                ) {
                    Text("Save Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNoteItem = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun WatchlistEpisodeCard(
    item: WatchlistEpisode,
    onPlay: () -> Unit,
    onOpenDetail: () -> Unit,
    onEditNote: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ElevatedCard,
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("watchlist_item_${item.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail with play badge
                Box(
                    modifier = Modifier
                        .width(88.dp)
                        .height(118.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onPlay)
                ) {
                    SubcomposeAsyncImage(
                        model = item.animeImageUrl,
                        contentDescription = item.animeTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ObsidianNight.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ElectricCrimson.copy(alpha = 0.92f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play saved episode",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ElectricCrimson.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "EP ${item.episodeNumber.toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCrimson,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        if (item.isCompleted) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldLive.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "WATCHED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldLive,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = item.animeTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(onClick = onOpenDetail)
                    )
                    Text(
                        text = item.episodeTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.genres.isNotEmpty()) {
                        Text(
                            text = item.genres.take(3).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = LuminousCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (item.note.isNotBlank()) {
                        Text(
                            text = "“${item.note}”",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFD9DF),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (item.watchedSeconds > 0L && item.durationSeconds > 0L) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { item.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = ElectricCrimson,
                        trackColor = GlassOverlay
                    )
                    Text(
                        text = "Resume at ${formatSec(item.watchedSeconds)} / ${formatSec(item.durationSeconds)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEditNote,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EditNote,
                            contentDescription = "Edit episode note",
                            tint = LuminousCyan
                        )
                    }
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Remove from watchlist",
                            tint = TextSecondary
                        )
                    }
                }

                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCrimson),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (item.watchedSeconds > 5L) "Resume" else "Watch",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyWatchlistView(
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_watchlist_empty),
                contentDescription = "Empty watchlist illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .height(190.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            Text(
                text = stringResource(id = R.string.watchlist_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.watchlist_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onExploreClick,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCrimson),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.explore_anime_cta),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatSec(totalSec: Long): String {
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return "%02d:%02d".format(mins, secs)
}
