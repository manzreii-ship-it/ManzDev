package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Anime
import com.example.data.model.AnimeFilter
import com.example.data.model.AnimeGenre
import com.example.data.model.UiState
import com.example.ui.components.AnimePosterCard
import com.example.ui.components.ErrorStateBox
import com.example.ui.components.GenreChipPill
import com.example.ui.components.LoadingStateBox
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    filter: AnimeFilter,
    genres: List<AnimeGenre>,
    resultsState: UiState<List<Anime>>,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onToggleGenre: (Int) -> Unit,
    onTypeSelected: (String?) -> Unit,
    onStatusSelected: (String?) -> Unit,
    onOrderBySelected: (String) -> Unit,
    onClearFilters: () -> Unit,
    onAnimeClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianNight)
            .statusBarsPadding()
    ) {
        // Header + Search Bar
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
                        text = "Explore Anime",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Search titles & filter by genre, format, or rating",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                if (filter.activeFilterCount > 0 || filter.query.isNotBlank()) {
                    TextButton(onClick = onClearFilters) {
                        Text("Reset", color = LuminousCyan)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = filter.query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = LuminousCyan
                        )
                    },
                    trailingIcon = {
                        if (filter.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear search",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            onSearchSubmit()
                        }
                    ),
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
                        .weight(1f)
                        .testTag("explore_search_input")
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (filter.activeFilterCount > 0) ElectricCrimson else ElevatedCard,
                    border = BorderStroke(
                        1.dp,
                        if (filter.activeFilterCount > 0) ElectricCrimson else HairlineBorder
                    )
                ) {
                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("explore_filter_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (filter.activeFilterCount > 0) {
                                    Badge(containerColor = LuminousCyan) {
                                        Text(
                                            text = filter.activeFilterCount.toString(),
                                            color = ObsidianNight,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = "Open filters",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Quick Genre Chips Strip
        if (genres.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                item {
                    GenreChipPill(
                        label = "All Genres",
                        selected = filter.selectedGenreIds.isEmpty(),
                        onClick = onClearFilters
                    )
                }
                items(genres.take(20), key = { it.id }) { genre ->
                    GenreChipPill(
                        label = genre.name,
                        selected = filter.selectedGenreIds.contains(genre.id),
                        onClick = { onToggleGenre(genre.id) }
                    )
                }
            }
        }

        // Results Grid
        when (resultsState) {
            is UiState.Loading -> {
                LoadingStateBox(
                    message = "Searching anime database…",
                    modifier = Modifier.weight(1f)
                )
            }

            is UiState.Error -> {
                ErrorStateBox(
                    message = resultsState.message,
                    onRetry = onSearchSubmit,
                    modifier = Modifier.weight(1f)
                )
            }

            is UiState.Success -> {
                val list = resultsState.data
                if (list.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "No matching anime found",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Try adjusting your search keywords or clearing genre filters.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            OutlinedButton(onClick = onClearFilters) {
                                Text("Clear All Filters", color = LuminousCyan)
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 148.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(list, key = { it.id }) { anime ->
                            AnimePosterCard(
                                anime = anime,
                                onClick = { onAnimeClick(anime.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = CharcoalSurface,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.filter_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    TextButton(onClick = onClearFilters) {
                        Text("Reset All", color = LuminousCyan)
                    }
                }

                // Format / Type
                Text("Format", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf(
                        null to "All",
                        "tv" to "TV Series",
                        "movie" to "Movie",
                        "ova" to "OVA",
                        "ona" to "ONA",
                        "special" to "Special"
                    )
                    types.forEach { (value, label) ->
                        GenreChipPill(
                            label = label,
                            selected = filter.type == value,
                            onClick = { onTypeSelected(value) }
                        )
                    }
                }

                // Status
                Text("Airing Status", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statuses = listOf(
                        null to "All",
                        "airing" to "Ongoing",
                        "complete" to "Completed",
                        "upcoming" to "Upcoming"
                    )
                    statuses.forEach { (value, label) ->
                        GenreChipPill(
                            label = label,
                            selected = filter.status == value,
                            onClick = { onStatusSelected(value) }
                        )
                    }
                }

                // Sort By
                Text("Sort By", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sorts = listOf(
                        "popularity" to "Most Popular",
                        "score" to "Highest Rated",
                        "start_date" to "Newest Release",
                        "title" to "Title A-Z"
                    )
                    sorts.forEach { (value, label) ->
                        GenreChipPill(
                            label = label,
                            selected = filter.orderBy == value,
                            onClick = { onOrderBySelected(value) }
                        )
                    }
                }

                // Genres multi-select
                Text(
                    text = "Genres (${filter.selectedGenreIds.size} selected)",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genres.take(28).forEach { genre ->
                        GenreChipPill(
                            label = genre.name,
                            selected = filter.selectedGenreIds.contains(genre.id),
                            onClick = { onToggleGenre(genre.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = { showFilterSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCrimson),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Show Results", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
