package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.data.model.Anime
import com.example.data.model.AnimeEpisode
import com.example.data.model.StreamSource
import com.example.data.model.UiState
import com.example.data.model.WatchlistEpisode
import com.example.ui.components.ErrorStateBox
import com.example.ui.components.LoadingStateBox
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElectricCrimson
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.GlassOverlay
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PlayerUiContent
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    playerState: UiState<PlayerUiContent>,
    watchlistItems: List<WatchlistEpisode>,
    defaultSpeed: Float,
    autoPlayNext: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSelectEpisode: (Int) -> Unit,
    onSelectSource: (StreamSource, Long) -> Unit,
    onSaveProgress: (Long, Long) -> Unit,
    onToggleFavoriteEpisode: (Anime, AnimeEpisode, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var isFullscreen by remember { mutableStateOf(false) }

    // Exit fullscreen cleanly on system back
    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    // Apply orientation + immersive system bars when fullscreen toggles
    DisposableEffect(isFullscreen) {
        val window = activity?.window
        if (activity != null && window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isFullscreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (activity != null && window != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    when (playerState) {
        is UiState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(ObsidianNight)
                    .statusBarsPadding()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                LoadingStateBox(
                    message = "Preparing in-app HD video stream…",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        is UiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(ObsidianNight)
                    .statusBarsPadding()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                ErrorStateBox(
                    message = playerState.message,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        is UiState.Success -> {
            val content = playerState.data
            val exoPlayer = remember {
                ExoPlayer.Builder(context).build().apply {
                    playWhenReady = true
                }
            }

            var isPlaying by remember { mutableStateOf(true) }
            var isBuffering by remember { mutableStateOf(true) }
            var playbackError by remember { mutableStateOf<String?>(null) }
            var currentPositionMs by remember { mutableLongStateOf(content.resumePositionMs) }
            var totalDurationMs by remember { mutableLongStateOf(0L) }
            var isSeeking by remember { mutableStateOf(false) }
            var sliderPositionMs by remember { mutableLongStateOf(content.resumePositionMs) }
            var controlsVisible by remember { mutableStateOf(true) }
            var playbackSpeed by remember(defaultSpeed) { mutableFloatStateOf(defaultSpeed) }
            var resizeModeIndex by remember { mutableIntStateOf(0) }
            val resizeModes = remember {
                listOf(
                    AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit",
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Zoom",
                    AspectRatioFrameLayout.RESIZE_MODE_FILL to "Stretch"
                )
            }

            // Load selected stream URL whenever episode or server changes
            LaunchedEffect(content.selectedSource.url, content.currentEpisode.number) {
                playbackError = null
                isBuffering = true
                val mediaItem = MediaItem.fromUri(content.selectedSource.url)
                exoPlayer.setMediaItem(mediaItem)
                if (content.resumePositionMs > 1000L) {
                    exoPlayer.seekTo(content.resumePositionMs)
                }
                exoPlayer.setPlaybackSpeed(playbackSpeed)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }

            // Player event listener
            DisposableEffect(exoPlayer, content.currentEpisode.number, autoPlayNext) {
                val listener = object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isBuffering = playbackState == Player.STATE_BUFFERING
                        if (playbackState == Player.STATE_READY) {
                            totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
                        }
                        if (playbackState == Player.STATE_ENDED) {
                            onSaveProgress(exoPlayer.duration.coerceAtLeast(0L), exoPlayer.duration.coerceAtLeast(0L))
                            if (autoPlayNext) {
                                val nextEp = content.episodes.firstOrNull {
                                    it.number == content.currentEpisode.number + 1
                                }
                                if (nextEp != null) {
                                    onSelectEpisode(nextEp.number)
                                }
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        isBuffering = false
                        // Auto-fallback to next server if current stream fails!
                        val currentIndex = content.sources.indexOfFirst { it.id == content.selectedSource.id }
                        if (currentIndex >= 0 && currentIndex + 1 < content.sources.size) {
                            onSelectSource(content.sources[currentIndex + 1], exoPlayer.currentPosition)
                        } else {
                            playbackError = "Stream unavailable (${error.errorCodeName}). Try switching server."
                        }
                    }
                }
                exoPlayer.addListener(listener)
                onDispose {
                    exoPlayer.removeListener(listener)
                }
            }

            // Lifecycle pause/release
            DisposableEffect(lifecycleOwner, exoPlayer) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_PAUSE) {
                        onSaveProgress(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0L))
                        exoPlayer.pause()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    onSaveProgress(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0L))
                    exoPlayer.release()
                }
            }

            // Poll position + periodic Firestore sync every 10s
            LaunchedEffect(exoPlayer, content.currentEpisode.number) {
                var ticks = 0
                while (true) {
                    if (!isSeeking) {
                        currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                        sliderPositionMs = currentPositionMs
                        val dur = exoPlayer.duration
                        if (dur > 0L) totalDurationMs = dur
                    }
                    ticks++
                    if (ticks % 20 == 0 && exoPlayer.isPlaying && totalDurationMs > 0L) {
                        onSaveProgress(currentPositionMs, totalDurationMs)
                    }
                    delay(500L)
                }
            }

            // Auto-hide controls after 4 seconds while playing
            LaunchedEffect(controlsVisible, isPlaying) {
                if (controlsVisible && isPlaying) {
                    delay(4000L)
                    controlsVisible = false
                }
            }

            val savedEpNumbers = remember(watchlistItems, content.anime.id) {
                watchlistItems.filter { it.animeId == content.anime.id }
                    .map { it.episodeNumber.toInt() }
                    .toSet()
            }
            val isCurrentSaved = savedEpNumbers.contains(content.currentEpisode.number)

            val prevEp = content.episodes.lastOrNull { it.number < content.currentEpisode.number }
            val nextEp = content.episodes.firstOrNull { it.number > content.currentEpisode.number }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(ObsidianNight)
                    .then(if (!isFullscreen) Modifier.statusBarsPadding() else Modifier)
            ) {
                // Video Player Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isFullscreen) {
                                Modifier.fillMaxHeight()
                            } else {
                                Modifier.aspectRatio(16f / 9f)
                            }
                        )
                        .background(Color.Black)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { controlsVisible = !controlsVisible }
                        .testTag("in_app_video_surface")
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                player = exoPlayer
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { view ->
                            view.player = exoPlayer
                            view.resizeMode = resizeModes[resizeModeIndex].first
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isBuffering) {
                        CircularProgressIndicator(
                            color = ElectricCrimson,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    playbackError?.let { err ->
                        Surface(
                            color = ObsidianNight.copy(alpha = 0.88f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    // Overlay Controls
                    androidx.compose.animation.AnimatedVisibility(
                        visible = controlsVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.72f),
                                            Color.Black.copy(alpha = 0.25f),
                                            Color.Black.copy(alpha = 0.80f)
                                        )
                                    )
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            // Top Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (isFullscreen) {
                                                isFullscreen = false
                                            } else {
                                                onBack()
                                            }
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = Color.White
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = content.anime.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "EP ${content.currentEpisode.number} • ${content.currentEpisode.title}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LuminousCyan,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            onToggleFavoriteEpisode(
                                                content.anime,
                                                content.currentEpisode,
                                                content.selectedSource.url
                                            )
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("player_bookmark_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                            contentDescription = "Bookmark episode",
                                            tint = if (isCurrentSaved) ElectricCrimson else Color.White
                                        )
                                    }
                                }
                            }

                            // Center Transport Controls
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { prevEp?.let { onSelectEpisode(it.number) } },
                                    enabled = prevEp != null,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipPrevious,
                                        contentDescription = stringResource(id = R.string.prev_episode),
                                        tint = if (prevEp != null) Color.White else Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Replay10,
                                        contentDescription = "Rewind 10 seconds",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = ElectricCrimson.copy(alpha = 0.92f),
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            if (exoPlayer.isPlaying) {
                                                exoPlayer.pause()
                                            } else {
                                                exoPlayer.play()
                                            }
                                        }
                                        .testTag("player_play_pause_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause" else "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val target = exoPlayer.currentPosition + 10_000L
                                        val maxDur = totalDurationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
                                        exoPlayer.seekTo(target.coerceAtMost(maxDur))
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Forward10,
                                        contentDescription = "Forward 10 seconds",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { nextEp?.let { onSelectEpisode(it.number) } },
                                    enabled = nextEp != null,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipNext,
                                        contentDescription = stringResource(id = R.string.next_episode),
                                        tint = if (nextEp != null) Color.White else Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            // Bottom Scrubber & Quick Actions
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                            ) {
                                // Skip Intro + Speed + Aspect + Fullscreen row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ObsidianNight.copy(alpha = 0.78f),
                                        border = BorderStroke(1.dp, LuminousCyan.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                val target = exoPlayer.currentPosition + 85_000L
                                                val maxDur = totalDurationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
                                                exoPlayer.seekTo(target.coerceAtMost(maxDur))
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.FastForward,
                                                contentDescription = null,
                                                tint = LuminousCyan,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = stringResource(id = R.string.skip_intro),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Speed Cycle
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = ObsidianNight.copy(alpha = 0.75f),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                                                    val next = speeds[(speeds.indexOf(playbackSpeed) + 1) % speeds.size]
                                                    playbackSpeed = next
                                                    exoPlayer.setPlaybackSpeed(next)
                                                }
                                        ) {
                                            Text(
                                                text = "${playbackSpeed}x",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }

                                        // Resize Mode
                                        IconButton(
                                            onClick = {
                                                resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.AspectRatio,
                                                contentDescription = "Cycle video aspect ratio",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Fullscreen
                                        IconButton(
                                            onClick = { isFullscreen = !isFullscreen },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .testTag("player_fullscreen_button")
                                        ) {
                                            Icon(
                                                imageVector = if (isFullscreen) {
                                                    Icons.Filled.FullscreenExit
                                                } else {
                                                    Icons.Filled.Fullscreen
                                                },
                                                contentDescription = "Toggle fullscreen",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }

                                // Progress Slider
                                val durFloat = totalDurationMs.coerceAtLeast(1L).toFloat()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = formatMs(sliderPositionMs),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                    Slider(
                                        value = sliderPositionMs.toFloat().coerceIn(0f, durFloat),
                                        onValueChange = {
                                            isSeeking = true
                                            sliderPositionMs = it.toLong()
                                        },
                                        onValueChangeFinished = {
                                            exoPlayer.seekTo(sliderPositionMs)
                                            isSeeking = false
                                        },
                                        valueRange = 0f..durFloat,
                                        colors = SliderDefaults.colors(
                                            thumbColor = ElectricCrimson,
                                            activeTrackColor = ElectricCrimson,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = formatMs(totalDurationMs),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Portrait Mode: Stream Info, Server Switcher, and Episode Drawer
                if (!isFullscreen) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .navigationBarsPadding(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "EP ${content.currentEpisode.number} • ${content.currentEpisode.title}",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            onToggleFavoriteEpisode(
                                                content.anime,
                                                content.currentEpisode,
                                                content.selectedSource.url
                                            )
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                            contentDescription = "Save episode to watchlist",
                                            tint = if (isCurrentSaved) ElectricCrimson else TextSecondary
                                        )
                                    }
                                }
                                Text(
                                    text = "${content.anime.title} • Playing via ${content.selectedSource.provider} (${content.selectedSource.quality})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LuminousCyan
                                )
                            }
                        }

                        // Server / Source Switcher
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Video Stream Server",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextSecondary
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(content.sources, key = { it.id }) { source ->
                                        val isSelected = source.id == content.selectedSource.id
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) ElectricCrimson else ElevatedCard,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) ElectricCrimson else HairlineBorder
                                            ),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    onSelectSource(source, exoPlayer.currentPosition)
                                                }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = source.label,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = source.quality,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else TextMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "All Episodes (${content.episodes.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }

                        items(content.episodes, key = { it.number }) { ep ->
                            val isCurrent = ep.number == content.currentEpisode.number
                            val isSaved = savedEpNumbers.contains(ep.number)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrent) ElectricCrimson.copy(alpha = 0.16f) else ElevatedCard,
                                border = BorderStroke(
                                    1.dp,
                                    if (isCurrent) ElectricCrimson else HairlineBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectEpisode(ep.number) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) ElectricCrimson else CharcoalSurface,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = ep.number.toString().padStart(2, '0'),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ep.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isCurrent) "Now Playing" else "${ep.durationMinutes} min",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isCurrent) LuminousCyan else TextMuted
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            onToggleFavoriteEpisode(
                                                content.anime,
                                                ep,
                                                content.selectedSource.url
                                            )
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                            contentDescription = "Save episode",
                                            tint = if (isSaved) ElectricCrimson else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return "%02d:%02d".format(mins, secs)
}
