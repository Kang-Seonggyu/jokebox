package com.jokebox.music

import com.jokebox.music.ui.theme.JokeboxColors
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.media3.common.Player
import coil.compose.AsyncImage

@Composable
fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Joke",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = JokeboxColors.TextPrimary
            )

            Text(
                text = "box",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = JokeboxColors.Primary
            )
        }

        Text(
            text = "♫", fontSize = 28.sp, color = JokeboxColors.Primary
        )
    }
}

@Composable
fun SongItem(
    song: Song,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val rowPadding = if (compact) 8.dp else 14.dp
    val artworkSize = if (compact) 40.dp else 48.dp
    val artworkShape = RoundedCornerShape(if (compact) 8.dp else 10.dp)

    Surface(
        color = JokeboxColors.Surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(rowPadding), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(artworkSize)
                    .clip(artworkShape)
                    .background(JokeboxColors.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = "Album Art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "♫", color = JokeboxColors.Primary, fontSize = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(if (compact) 10.dp else 14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = song.title,
                    color = JokeboxColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (compact) 13.sp else 14.sp,
                    lineHeight = if (compact) 14.sp else 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = song.artist,
                    color = JokeboxColors.TextSecondary,
                    fontSize = if (compact) 11.sp else 12.sp,
                    lineHeight = if (compact) 12.sp else 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = song.duration, color = JokeboxColors.TextSecondary, fontSize = 12.sp
            )
        }
    }
}

@Composable
fun MiniPlayer(
    song: Song?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    onQueueClick: () -> Unit,
) {
    val albumRotation = remember(song?.id) { Animatable(0f) }

    LaunchedEffect(song?.id, isPlaying) {
        if (song?.albumArtUri != null && isPlaying) {
            while (true) {
                albumRotation.animateTo(
                    targetValue = albumRotation.value + 360f,
                    animationSpec = tween(durationMillis = 8_000, easing = LinearEasing),
                )
            }
        }
    }

    Surface(
        color = JokeboxColors.SurfaceVariant,
        shape = RoundedCornerShape(percent = 50),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .pointerInput(song?.id, onExpand) {
                var verticalDrag = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        verticalDrag += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        if (song != null && verticalDrag < -48.dp.toPx()) {
                            onExpand()
                        }
                    },
                    onDragCancel = { verticalDrag = 0f },
                )
            }
            .clickable(enabled = song != null, onClick = onExpand),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = with(sharedTransitionScope) {
                    Modifier
                        .sharedElement(
                            rememberSharedContentState(key = "player-artwork-${song?.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> tween(durationMillis = 420) },
                        )
                        .size(42.dp)
                        .clip(CircleShape)
                }
                    .background(JokeboxColors.Surface),
                contentAlignment = Alignment.Center,
            ) {
                if (song?.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = "Album Art",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = albumRotation.value },
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = "♫",
                        color = JokeboxColors.Primary,
                        fontSize = 22.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = song?.title ?: "재생할 곡이 없습니다.",
                    color = JokeboxColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            initialDelayMillis = 1_000,
                        ),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song?.artist ?: "음악을 선택하세요.",
                    color = JokeboxColors.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            initialDelayMillis = 1_000,
                        ),
                )
            }

            MiniPlayerControl(text = "⏮", enabled = song != null, onClick = onPrevious)
            MiniPlayerPlayPauseControl(
                isPlaying = isPlaying,
                enabled = song != null,
                onClick = onPlayPause,
            )
            MiniPlayerControl(text = "⏭", enabled = song != null, onClick = onNext)
            MiniPlayerQueueControl(onClick = onQueueClick)
        }
    }
}

@Composable
private fun MiniPlayerControl(
    text: String,
    enabled: Boolean = true,
    color: Color = JokeboxColors.TextPrimary,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color.copy(alpha = if (enabled) 1f else 0.4f),
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun MiniPlayerQueueControl(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "재생 대기열"
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val color = JokeboxColors.TextPrimary
            val unit = size.width / 24f
            val strokeWidth = 2f * unit

            drawLine(
                color = color,
                start = Offset(3f * unit, 6f * unit),
                end = Offset(14f * unit, 6f * unit),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(3f * unit, 10f * unit),
                end = Offset(14f * unit, 10f * unit),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(3f * unit, 14f * unit),
                end = Offset(10f * unit, 14f * unit),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )

            val note = Path().apply {
                moveTo(19f * unit, 5f * unit)
                lineTo(23f * unit, 5f * unit)
                lineTo(23f * unit, 7f * unit)
                lineTo(21f * unit, 7f * unit)
                lineTo(21f * unit, 17f * unit)
                lineTo(19f * unit, 17f * unit)
                close()
            }
            drawPath(path = note, color = color)
            drawCircle(
                color = color,
                radius = 2.5f * unit,
                center = Offset(17.5f * unit, 18f * unit),
            )
        }
    }
}

@Composable
private fun MiniPlayerPlayPauseControl(
    isPlaying: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                contentDescription = if (isPlaying) "일시정지" else "재생"
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val color = JokeboxColors.Primary.copy(alpha = if (enabled) 1f else 0.4f)
            if (isPlaying) {
                val barWidth = size.width * 0.24f
                val barHeight = size.height * 0.72f
                val gap = size.width * 0.18f
                val totalWidth = barWidth * 2 + gap
                val startX = (size.width - totalWidth) / 2f
                val startY = (size.height - barHeight) / 2f

                drawRect(
                    color = color,
                    topLeft = Offset(startX, startY),
                    size = Size(barWidth, barHeight),
                )
                drawRect(
                    color = color,
                    topLeft = Offset(startX + barWidth + gap, startY),
                    size = Size(barWidth, barHeight),
                )
            } else {
                val path = Path().apply {
                    moveTo(size.width * 0.27f, size.height * 0.12f)
                    lineTo(size.width * 0.82f, size.height * 0.5f)
                    lineTo(size.width * 0.27f, size.height * 0.88f)
                    close()
                }
                drawPath(path = path, color = color)
            }
        }
    }
}
fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun MusicContent(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
) {
    val songListState = rememberLazyListState()
    val indexLetters = listOf("#") +
        ('A'..'Z').map { it.toString() } +
        listOf("ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ")

    fun getIndexKey(title: String): String {
        val firstCharacter = title.firstOrNull { !it.isWhitespace() }?.uppercaseChar()
            ?: return "#"

        if (firstCharacter in '가'..'힣') {
            val initials = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
            val initialIndex = (firstCharacter.code - '가'.code) / 588
            return when (initials[initialIndex]) {
                'ㄲ' -> "ㄱ"
                'ㄸ' -> "ㄷ"
                'ㅃ' -> "ㅂ"
                'ㅆ' -> "ㅅ"
                'ㅉ' -> "ㅈ"
                else -> initials[initialIndex].toString()
            }
        }

        return when {
            firstCharacter.isDigit() -> "#"
            firstCharacter in 'A'..'Z' -> firstCharacter.toString()
            else -> "#"
        }
    }

    var activeIndex by remember { mutableIntStateOf(-1) }
    var isIndexTouching by remember { mutableStateOf(false) }
    var touchFraction by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(activeIndex, songs) {
        val letter = indexLetters.getOrNull(activeIndex) ?: return@LaunchedEffect
        val targetIndex = songs.indexOfFirst { getIndexKey(it.title) == letter }
        if (targetIndex >= 0) {
            songListState.scrollToItem(targetIndex)
        }
    }

    if (songs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "음악 파일이 없습니다.",
                color = JokeboxColors.TextSecondary,
                fontSize = 14.sp,
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(JokeboxColors.Background),
        ) {
            LazyColumn(
                state = songListState,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(songs, key = { it.id }) { song ->
                    SongItem(song = song, compact = true) { onSongClick(song) }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .width(28.dp)
                    .fillMaxHeight()
                    .pointerInput(indexLetters, songs) {
                        val railHeight = size.height.toFloat().coerceAtLeast(1f)

                        fun updateIndexAt(y: Float) {
                            touchFraction = (y / railHeight).coerceIn(0f, 1f)
                            activeIndex = (
                                touchFraction * indexLetters.size
                            ).toInt().coerceIn(indexLetters.indices)
                        }

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isIndexTouching = true
                            try {
                                updateIndexAt(down.position.y)

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull {
                                        it.id == down.id
                                    } ?: break
                                    if (!change.pressed) break

                                    updateIndexAt(change.position.y)
                                    change.consume()
                                }
                            } finally {
                                isIndexTouching = false
                            }
                        }
                    },
            ) {
                val letterLineHeight = (maxHeight / indexLetters.size)
                    .coerceAtMost(12.dp)
                    .coerceAtLeast(8.dp)

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    indexLetters.forEachIndexed { index, letter ->
                        Text(
                            text = letter,
                            fontSize = (letterLineHeight.value * 0.8f).sp,
                            lineHeight = letterLineHeight.value.sp,
                            color = JokeboxColors.TextSecondary,
                            modifier = Modifier.clickable {
                                activeIndex = index
                            },
                        )
                    }
                }

                if (isIndexTouching && activeIndex >= 0) {
                    val tooltipHeight = 32.dp
                    val tooltipY = (maxHeight * touchFraction - tooltipHeight / 2)
                        .coerceIn(0.dp, (maxHeight - tooltipHeight).coerceAtLeast(0.dp))

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-46).dp, y = tooltipY),
                        color = JokeboxColors.Primary,
                        shape = RoundedCornerShape(10.dp),
                        shadowElevation = 6.dp,
                    ) {
                        Text(
                            text = indexLetters[activeIndex],
                            color = Color.Black,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingContent(
    song: Song,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    isPlaying: Boolean,
    currentPosition: Long,
    totalDuration: Long,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    lyricsAvailable: Boolean,
    lyrics: String,
    playlists: List<MusicPlaylist>,
    onAddToPlaylist: (MusicPlaylist) -> Unit,
    onOpenQueue: () -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
) {
    var lyricsVisible by remember(song.id) { mutableStateOf(false) }
    var showPlaylistPicker by remember(song.id) { mutableStateOf(false) }
    var lastAudibleVolume by remember(song.id) {
        mutableFloatStateOf(volume.takeIf { it > 0f } ?: 1f)
    }

    LaunchedEffect(volume) {
        if (volume > 0f) lastAudibleVolume = volume
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(onBack) {
                var verticalDrag = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        verticalDrag += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        if (verticalDrag > 72.dp.toPx()) onBack()
                        verticalDrag = 0f
                    },
                    onDragCancel = { verticalDrag = 0f },
                )
            }
            .background(JokeboxColors.Background)
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(
                text = song.albumTitle,
                color = JokeboxColors.TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.58f)
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 1_000,
                    ),
            )
            PlayerCollapseButton(
                modifier = Modifier.align(Alignment.CenterStart),
                onClick = onBack,
            )
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InstantMixIcon()
                MiniPlayerQueueControl(onClick = onOpenQueue)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val artworkSize = minOf(maxWidth * 0.92f, maxHeight)
            Box(
                modifier = with(sharedTransitionScope) {
                    Modifier
                        .sharedElement(
                            rememberSharedContentState(key = "player-artwork-${song.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ -> tween(durationMillis = 420) },
                        )
                        .size(artworkSize)
                        .clip(RoundedCornerShape(24.dp))
                },
            ) {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = "앨범 사진",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(JokeboxColors.Surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("♫", fontSize = 100.sp, color = JokeboxColors.Primary)
                    }
                }

                if (lyricsVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = lyrics.ifBlank { "가사가 표시되면 여기에 나타납니다." },
                            color = Color.White,
                            fontSize = 18.sp,
                            lineHeight = 28.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = song.title,
                    color = JokeboxColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            initialDelayMillis = 1_000,
                        ),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artist,
                    color = JokeboxColors.TextSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            initialDelayMillis = 1_000,
                        ),
                )
            }

            PlayerActionButton(
                icon = "가",
                description = "가사",
                enabled = lyricsAvailable,
                selected = lyricsVisible,
                onClick = { lyricsVisible = !lyricsVisible },
            )
            PlayerDrawableButton(
                iconRes = if (isFavorite) {
                    R.drawable.favorite_24
                } else {
                    R.drawable.favorite_border_24
                },
                description = "좋아요",
                selected = isFavorite,
                onClick = onToggleFavorite,
            )
            PlayerDrawableButton(
                iconRes = R.drawable.playlist_add_24,
                description = "재생목록에 추가",
                onClick = { showPlaylistPicker = true },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            val progressColors = SliderDefaults.colors(
                thumbColor = JokeboxColors.Primary,
                activeTrackColor = JokeboxColors.Primary,
                inactiveTrackColor = JokeboxColors.SurfaceVariant,
            )
            Slider(
                value = currentPosition.toFloat().coerceIn(
                    0f, totalDuration.toFloat().coerceAtLeast(1f)
                ),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..totalDuration.toFloat().coerceAtLeast(1f),
                enabled = totalDuration > 0L,
                colors = progressColors,
                modifier = Modifier.height(28.dp),
                thumb = {},
                track = { state ->
                    SliderDefaults.Track(
                        sliderState = state,
                        modifier = Modifier.height(3.dp),
                        colors = progressColors,
                        drawStopIndicator = null,
                        thumbTrackGapSize = 0.dp,
                    )
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatTime(currentPosition),
                    color = JokeboxColors.TextSecondary,
                    fontSize = 12.sp,
                )
                Text(
                    text = formatTime(totalDuration),
                    color = JokeboxColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PlayerDrawableButton(
                iconRes = R.drawable.shuffle_24,
                description = "셔플",
                size = 48.dp,
                iconSize = 28.dp,
                selected = shuffleEnabled,
                onClick = onToggleShuffle,
            )
            PlayerActionButton(icon = "|◀", description = "이전 곡", onClick = onPrevious)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(JokeboxColors.Primary)
                    .clickable(onClick = onPlayPause)
                    .semantics {
                        contentDescription = if (isPlaying) "일시정지" else "재생"
                        role = Role.Button
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(
                        if (isPlaying) R.drawable.pause_24 else R.drawable.play_arrow_24
                    ),
                    contentDescription = if (isPlaying) "일시정지" else "재생",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
            PlayerActionButton(icon = "▶|", description = "다음 곡", onClick = onNext)
            PlayerDrawableButton(
                iconRes = if (repeatMode == Player.REPEAT_MODE_ONE) {
                    R.drawable.repeat_one_24
                } else {
                    R.drawable.repeat_24
                },
                description = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> "한 곡 반복"
                    Player.REPEAT_MODE_ALL -> "전체 반복"
                    else -> "반복 안 함"
                },
                size = 48.dp,
                iconSize = 28.dp,
                selected = repeatMode != Player.REPEAT_MODE_OFF,
                onClick = onCycleRepeat,
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerDrawableButton(
                iconRes = if (volume <= 0f) {
                    R.drawable.volume_off_24
                } else {
                    R.drawable.volume_up_24
                },
                description = if (volume <= 0f) "소리 켜기" else "음소거",
                size = 48.dp,
                onClick = {
                    if (volume > 0f) {
                        lastAudibleVolume = volume
                        onVolumeChange(0f)
                    } else {
                        onVolumeChange(lastAudibleVolume.coerceIn(0.1f, 1f))
                    }
                },
            )
            val volumeColors = SliderDefaults.colors(
                thumbColor = JokeboxColors.Primary,
                activeTrackColor = JokeboxColors.Primary,
                inactiveTrackColor = JokeboxColors.SurfaceVariant,
            )
            Slider(
                value = volume.coerceIn(0f, 1f),
                onValueChange = onVolumeChange,
                valueRange = 0f..1f,
                colors = volumeColors,
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(JokeboxColors.Primary, CircleShape),
                    )
                },
                track = { state ->
                    SliderDefaults.Track(
                        sliderState = state,
                        modifier = Modifier.height(3.dp),
                        colors = volumeColors,
                        drawStopIndicator = null,
                        thumbTrackGapSize = 0.dp,
                    )
                },
            )
        }
    }

    if (showPlaylistPicker) {
        AlertDialog(
            onDismissRequest = { showPlaylistPicker = false },
            title = { Text("재생목록에 추가") },
            text = {
                if (playlists.isEmpty()) {
                    Text("추가할 재생목록이 없습니다.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(playlists, key = { it.id }) { playlist ->
                            TextButton(
                                onClick = {
                                    onAddToPlaylist(playlist)
                                    showPlaylistPicker = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = playlist.name,
                                    color = JokeboxColors.TextPrimary,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistPicker = false }) {
                    Text("닫기")
                }
            },
            containerColor = JokeboxColors.Surface,
        )
    }
}

@Composable
private fun PlayerCollapseButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "미니플레이어로 축소"
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val color = JokeboxColors.TextSecondary
            drawLine(
                color = color,
                start = Offset(size.width * 0.22f, size.height * 0.38f),
                end = Offset(size.width * 0.5f, size.height * 0.66f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(size.width * 0.5f, size.height * 0.66f),
                end = Offset(size.width * 0.78f, size.height * 0.38f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun InstantMixIcon() {
    Box(
        modifier = Modifier
            .size(44.dp)
            .semantics { contentDescription = "Instant Mix" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.instant_mix_24),
            contentDescription = null,
            tint = JokeboxColors.TextSecondary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun PlayerDrawableButton(
    iconRes: Int,
    description: String,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (selected) JokeboxColors.Primary else JokeboxColors.TextSecondary,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun PlayerActionButton(
    icon: String,
    description: String,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = icon,
            color = when {
                !enabled -> JokeboxColors.TextSecondary.copy(alpha = 0.35f)
                selected -> JokeboxColors.Primary
                else -> JokeboxColors.TextSecondary
            },
            fontSize = if (icon == "가") 12.sp else 20.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}


@Composable
private fun AlbumSortIcon() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val color = JokeboxColors.Primary
        val stroke = size.minDimension * 0.11f
        val left = size.width * 0.08f
        val right = size.width * 0.58f
        val top = size.height * 0.22f
        val gap = size.height * 0.28f

        drawLine(color, Offset(left, top), Offset(right, top), stroke, StrokeCap.Round)
        drawLine(color, Offset(left, top + gap), Offset(size.width * 0.48f, top + gap), stroke, StrokeCap.Round)
        drawLine(color, Offset(left, top + gap * 2), Offset(size.width * 0.36f, top + gap * 2), stroke, StrokeCap.Round)
        drawLine(
            color,
            Offset(size.width * 0.82f, size.height * 0.2f),
            Offset(size.width * 0.82f, size.height * 0.72f),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(size.width * 0.65f, size.height * 0.56f),
            Offset(size.width * 0.82f, size.height * 0.76f),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(size.width * 0.99f, size.height * 0.56f),
            Offset(size.width * 0.82f, size.height * 0.76f),
            stroke,
            StrokeCap.Round,
        )
    }
}

@Composable
private fun AlbumDropdownArrowIcon() {
    Canvas(modifier = Modifier.size(16.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.38f)
            lineTo(size.width * 0.5f, size.height * 0.66f)
            lineTo(size.width * 0.8f, size.height * 0.38f)
        }
        drawPath(
            path = path,
            color = JokeboxColors.TextSecondary,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = size.minDimension * 0.12f,
                cap = StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
    }
}

@Composable
fun AlbumsContent(
    albums: List<MusicAlbum>,
    sortMode: String,
    onSortModeChange: (String) -> Unit,
    onAlbumClick: (MusicAlbum) -> Unit,
    onGridScrollStateChanged: (Boolean) -> Unit,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    var isScrollbarVisible by remember { mutableStateOf(false) }
    val showScrollToTop by remember(gridState) {
        derivedStateOf {
            gridState.firstVisibleItemIndex >= gridState.layoutInfo.visibleItemsInfo.size
        }
    }
    val sortedAlbums = remember(albums, sortMode) {
        when (sortMode) {
            "발매일 순" -> albums.sortedWith(
                compareByDescending<MusicAlbum> { album ->
                    album.songs.mapNotNull { it.year }.maxOrNull() ?: 0
                }.thenBy { it.title.lowercase() }
            )
            "아티스트 순" -> albums.sortedWith(
                compareBy<MusicAlbum> { it.artist.lowercase() }
                    .thenBy { it.title.lowercase() }
            )
            else -> albums.sortedBy { it.title.lowercase() }
        }
    }

    LaunchedEffect(gridState.isScrollInProgress) {
        onGridScrollStateChanged(gridState.isScrollInProgress)
        if (gridState.isScrollInProgress) {
            isScrollbarVisible = true
        } else {
            delay(1_400)
            isScrollbarVisible = false
        }
    }

    DisposableEffect(Unit) {
        onDispose { onGridScrollStateChanged(false) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 136.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        Surface(
                            color = JokeboxColors.SurfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.clickable { sortMenuExpanded = true },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AlbumSortIcon()
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = sortMode,
                                    color = JokeboxColors.TextPrimary,
                                    fontSize = 13.sp,
                                )
                                Spacer(Modifier.width(8.dp))
                                AlbumDropdownArrowIcon()
                            }
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                        ) {
                            listOf("발매일 순", "이름 순", "아티스트 순").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        onSortModeChange(option)
                                        sortMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "${albums.size}개 앨범",
                        color = JokeboxColors.TextSecondary,
                        fontSize = 13.sp,
                    )
                }
            }

            if (sortedAlbums.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "앨범이 없습니다.",
                            color = JokeboxColors.TextSecondary,
                            fontSize = 14.sp,
                        )
                    }
                }
            } else {
                items(sortedAlbums, key = { it.id }) { album ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAlbumClick(album) },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(JokeboxColors.SurfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            val coverUri = album.songs.firstOrNull()?.albumArtUri
                            if (coverUri != null) {
                                AsyncImage(
                                    model = coverUri,
                                    contentDescription = "${album.title} 앨범 표지",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Text(
                                    text = "♫",
                                    color = JokeboxColors.Primary,
                                    fontSize = 36.sp,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = album.title,
                            color = JokeboxColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            lineHeight = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = album.artist,
                                modifier = Modifier.weight(1f),
                                color = JokeboxColors.TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = " | ${album.songs.size}곡",
                                color = JokeboxColors.TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 12.sp,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

                if (isScrollbarVisible && gridState.layoutInfo.totalItemsCount > 0) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(24.dp)
                            .pointerInput(sortedAlbums.size) {
                                var dragY = 0f

                                fun scrollToFraction(y: Float) {
                                    val fraction = (y / size.height.coerceAtLeast(1))
                                        .coerceIn(0f, 1f)
                                    val lastIndex = (gridState.layoutInfo.totalItemsCount - 1)
                                        .coerceAtLeast(0)
                                    coroutineScope.launch {
                                        gridState.scrollToItem((fraction * lastIndex).toInt())
                                    }
                                }

                                detectVerticalDragGestures(
                                    onDragStart = { offset ->
                                        dragY = offset.y
                                        scrollToFraction(dragY)
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        dragY = (dragY + dragAmount).coerceIn(0f, size.height.toFloat())
                                        scrollToFraction(dragY)
                                        change.consume()
                                    },
                                )
                            },
                    ) {
                        val totalItems = gridState.layoutInfo.totalItemsCount.coerceAtLeast(1)
                        val visibleItems = gridState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
                        val thumbHeight = (maxHeight * (visibleItems.toFloat() / totalItems))
                            .coerceAtLeast(36.dp)
                            .coerceAtMost(maxHeight)
                        val lastScrollIndex = (totalItems - visibleItems).coerceAtLeast(1)
                        val scrollFraction = (gridState.firstVisibleItemIndex.toFloat() /
                            lastScrollIndex).coerceIn(0f, 1f)

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (maxHeight - thumbHeight) * scrollFraction)
                                .width(4.dp)
                                .height(thumbHeight)
                                .background(JokeboxColors.Primary, CircleShape),
                        )
                    }
                }

                if (showScrollToTop) {
                    Surface(
                        color = JokeboxColors.Primary,
                        shape = CircleShape,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .size(48.dp)
                            .clickable {
                                coroutineScope.launch { gridState.animateScrollToItem(0) }
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "↑",
                                color = JokeboxColors.Background,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
}

@Composable
fun ArtistsContent(
    artists: Map<String, List<Song>>,
    onArtistClick: (String) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (artists.isEmpty()) {
            item {
                Text(
                    text = "아티스트가 없습니다.",
                    color = JokeboxColors.TextSecondary,
                    fontSize = 14.sp,
                )
            }
        } else {
            items(artists.toList(), key = { it.first }) { (artist, artistSongs) ->
                MusicCollectionRow(
                    title = artist,
                    subtitle = "${artistSongs.size}곡",
                    coverSong = artistSongs.firstOrNull(),
                    onClick = { onArtistClick(artist) },
                )
            }
        }
    }
}

@Composable
private fun MusicCollectionRow(
    title: String,
    subtitle: String,
    coverSong: Song?,
    onClick: () -> Unit,
) {
    Surface(
        color = JokeboxColors.Surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        JokeboxColors.SurfaceVariant,
                        RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (coverSong?.albumArtUri != null) {
                    AsyncImage(
                        model = coverSong.albumArtUri,
                        contentDescription = "$title 커버",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = "♫",
                        color = JokeboxColors.Primary,
                        fontSize = 26.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = JokeboxColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = JokeboxColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

