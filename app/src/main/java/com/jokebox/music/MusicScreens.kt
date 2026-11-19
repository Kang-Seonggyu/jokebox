package com.jokebox.music

import com.jokebox.music.ui.theme.JokeboxColors
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
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
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
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
@Composable
fun NowPlayingContent(
    song: Song,
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
    onToggleFavorite: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JokeboxColors.Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 상단 뒤로 가기
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⌄",
                fontSize = 32.sp,
                color = JokeboxColors.TextPrimary,
                modifier = Modifier.clickable {
                    onBack()
                })

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "NOW PLAYING",
                color = JokeboxColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(text = "♫", color = JokeboxColors.Primary)
        }

        Spacer(modifier = Modifier.weight(1f))

        // 앨범 이미지
        if (song.albumArtUri != null) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = "Album Art",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(
                        JokeboxColors.Surface, RoundedCornerShape(20.dp)
                    ), contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "♫", fontSize = 100.sp, color = JokeboxColors.Primary
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // 곡 정보
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    color = JokeboxColors.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = song.artist, color = JokeboxColors.TextSecondary, fontSize = 16.sp
                )
            }

            Text(
                text = if (isFavorite) "♥" else "♡", color = if (isFavorite) {
                JokeboxColors.Primary
            } else {
                JokeboxColors.TextSecondary
            }, fontSize = 30.sp, modifier = Modifier
                    .clickable {
                        onToggleFavorite()
                    }
                    .padding(8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 재생 진행 바
        Slider(
            value = currentPosition.toFloat().coerceIn(
                0f, totalDuration.toFloat().coerceAtLeast(1f)
            ),
            onValueChange = {
                onSeek(it.toLong())
            },
            valueRange = 0f..totalDuration.toFloat().coerceAtLeast(1f),
            enabled = totalDuration > 0L,
            colors = SliderDefaults.colors(
                thumbColor = JokeboxColors.Primary, activeTrackColor = JokeboxColors.Primary
            )
        )

        // 셔플 및 반복 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⤨", fontSize = 24.sp, color = if (shuffleEnabled) {
                JokeboxColors.Primary
            } else {
                JokeboxColors.TextSecondary
            }, modifier = Modifier
                    .clickable {
                        onToggleShuffle()
                    }
                    .padding(8.dp))

            Text(
                text = when (repeatMode) {
                Player.REPEAT_MODE_ALL -> "↻"
                Player.REPEAT_MODE_ONE -> "↻¹"
                else -> "↻"
            }, fontSize = 24.sp, color = if (repeatMode != Player.REPEAT_MODE_OFF) {
                JokeboxColors.Primary
            } else {
                JokeboxColors.TextSecondary
            }, modifier = Modifier
                    .clickable {
                        onCycleRepeat()
                    }
                    .padding(8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                color = JokeboxColors.TextSecondary,
                fontSize = 12.sp
            )

            Text(
                text = formatTime(totalDuration),
                color = JokeboxColors.TextSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 재생 컨트롤
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⏮",
                fontSize = 30.sp,
                color = JokeboxColors.TextPrimary,
                modifier = Modifier.clickable {
                    onPrevious()
                })

            Text(
                text = if (isPlaying) "Ⅱ" else "▶",
                fontSize = 42.sp,
                color = JokeboxColors.Primary,
                modifier = Modifier.clickable {
                    onPlayPause()
                })

            Text(
                text = "⏭",
                fontSize = 30.sp,
                color = JokeboxColors.TextPrimary,
                modifier = Modifier.clickable {
                    onNext()
                })
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun AlbumsContent(
    albums: List<MusicAlbum>,
    onAlbumClick: (MusicAlbum) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (albums.isEmpty()) {
            item {
                Text(
                    text = "앨범이 없습니다.",
                    color = JokeboxColors.TextSecondary,
                    fontSize = 14.sp,
                )
            }
        } else {
            items(albums, key = { it.id }) { album ->
                MusicCollectionRow(
                    title = album.title,
                    subtitle = "${album.artist} · ${album.songs.size}곡",
                    coverSong = album.songs.firstOrNull(),
                    onClick = { onAlbumClick(album) },
                )
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

