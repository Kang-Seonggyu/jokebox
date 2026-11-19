package com.jokebox.music

import com.jokebox.music.ui.theme.JokeboxColors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
    song: Song, onClick: () -> Unit
) {
    Surface(
        color = JokeboxColors.Surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        JokeboxColors.SurfaceVariant, RoundedCornerShape(10.dp)
                    ), contentAlignment = Alignment.Center
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

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = JokeboxColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = song.artist, color = JokeboxColors.TextSecondary, fontSize = 12.sp
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
    song: Song,
    isPlaying: Boolean,
    currentPosition: Long,
    totalDuration: Long,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onExpand: () -> Unit
) {
    Surface(
        color = JokeboxColors.SurfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpand() }
            .padding(horizontal = 12.dp, vertical = 8.dp)) {
        if (song.albumArtUri != null) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = "Album Art",
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 12.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        JokeboxColors.Surface, RoundedCornerShape(10.dp)
                    ), contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "♫", color = JokeboxColors.Primary, fontSize = 24.sp
                )
            }
        }
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // 곡 정보와 재생 버튼
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = JokeboxColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = song.artist, color = JokeboxColors.TextSecondary, fontSize = 12.sp
                    )
                }

                Text(text = "⏮", color = JokeboxColors.TextPrimary, modifier = Modifier
                    .clickable {
                        onPrevious()
                    }
                    .padding(8.dp))

                Text(
                    text = if (isPlaying) "Ⅱ" else "▶",
                    color = JokeboxColors.Primary,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .clickable {
                            onPlayPause()
                        }
                        .padding(8.dp))

                Text(text = "⏭", color = JokeboxColors.TextPrimary, modifier = Modifier
                    .clickable {
                        onNext()
                    }
                    .padding(8.dp))
            }

            // 재생 진행 바
            Slider(
                value = currentPosition.toFloat().coerceIn(
                    0f, totalDuration.toFloat().coerceAtLeast(1f)
                ),
                onValueChange = {
                    onSeek(it.toLong())
                },
                valueRange = 0f..totalDuration.toFloat().coerceAtLeast(1f),
                enabled = totalDuration > 0L
            )

            // 현재 시간과 전체 시간
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    color = JokeboxColors.TextSecondary,
                    fontSize = 11.sp
                )

                Text(
                    text = formatTime(totalDuration),
                    color = JokeboxColors.TextSecondary,
                    fontSize = 11.sp
                )
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
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSongClick: (Song) -> Unit
) {
    val filteredSongs = songs.filter { song ->
        song.title.contains(searchQuery, ignoreCase = true) || song.artist.contains(
            searchQuery, ignoreCase = true
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JokeboxColors.Background)
            .padding(10.dp, 8.dp, 10.dp, 24.dp)

    ) {
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("곡 또는 아티스트 검색")
            },
            singleLine = true,
            leadingIcon = {
                Text("⌕", fontSize = 24.sp)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Text(text = "✕", modifier = Modifier
                        .clickable {
                            onSearchQueryChange("")
                        }
                        .padding(8.dp))
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = JokeboxColors.TextPrimary,
                unfocusedTextColor = JokeboxColors.TextPrimary,
                focusedBorderColor = JokeboxColors.Primary,
                unfocusedBorderColor = JokeboxColors.SurfaceVariant,
                cursorColor = JokeboxColors.Primary,
                focusedPlaceholderColor = JokeboxColors.TextSecondary,
                unfocusedPlaceholderColor = JokeboxColors.TextSecondary
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        when {
            filteredSongs.isEmpty() -> {
                Text(
                    text = if (songs.isEmpty()) "음악 파일이 없습니다." else "검색 결과가 없습니다.",
                    color = JokeboxColors.TextSecondary,
                    fontSize = 14.sp,
                )
            }

            else -> {
                Text(
                    text = "${filteredSongs.size}곡",
                    color = JokeboxColors.TextSecondary,
                    fontSize = 14.sp,
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn {
                    items(filteredSongs) { song ->
                        SongItem(
                            song = song,
                            onClick = { onSongClick(song) },
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

