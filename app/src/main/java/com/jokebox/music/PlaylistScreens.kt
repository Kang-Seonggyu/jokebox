package com.jokebox.music

import com.jokebox.music.ui.theme.JokeboxColors
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun PlaylistDetailContent(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    title: String = "Favorites",
    emptyMessage: String = "아직 재생한 음악이 없습니다.",
    onBackClick: (() -> Unit)? = null,
    allSongs: List<Song> = emptyList(),
    onEditSongs: ((List<Long>) -> Unit)? = null,
    coverUri: Uri? = null,
    onShuffleClick: (() -> Unit)? = null,
    onPlayClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    isCustomPlaylist: Boolean = false,
    onChangeCoverClick: (() -> Unit)? = null,
    onResetCoverClick: (() -> Unit)? = null,
) {
    var showEditSongsDialog by remember {
        mutableStateOf(value = false)
    }
    var showPlaylistMenu by remember {
        mutableStateOf(value = false)
    }
    var selectedSongIds by remember {
        mutableStateOf(setOf<Long>())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JokeboxColors.Background)
            .padding(16.dp),
    ) {
        // 총 재생시간 계산
        val totalSeconds = songs.sumOf { song ->
            val parts = song.duration.split(":")
            when (parts.size) {
                2 -> ((parts[0].toIntOrNull() ?: 0) * 60) + (parts[1].toIntOrNull() ?: 0)

                3 -> (parts[0].toIntOrNull() ?: 0) * 3600 + (parts[1].toIntOrNull()
                    ?: 0) * 60 + (parts[2].toIntOrNull() ?: 0)

                else -> 0
            }
        }

        val totalDurationText = if (totalSeconds >= 3600) {
            "${totalSeconds / 3600} hr ${(totalSeconds % 3600) / 60} min"
        } else {
            "${totalSeconds / 60} min ${totalSeconds % 60} sec"
        }

// 상단 뒤로가기 + 메뉴
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onBackClick?.invoke() }
            ) {
                Text(
                    text = "<", color = JokeboxColors.TextPrimary, fontSize = 28.sp
                )
            }

            Box {
                IconButton(
                    onClick = {
                        showPlaylistMenu = true
                    }) {
                    Text(
                        text = "⋮", color = JokeboxColors.TextPrimary, fontSize = 28.sp
                    )
                }

                DropdownMenu(
                    expanded = showPlaylistMenu, onDismissRequest = { showPlaylistMenu = false }) {
                    if (isCustomPlaylist) {
                        DropdownMenuItem(text = { Text("곡 편집") }, onClick = {
                            showPlaylistMenu = false
                            selectedSongIds = songs.asSequence().map { it.id }.toSet()
                            showEditSongsDialog = true
                        })

                        DropdownMenuItem(text = { Text("대표 이미지 변경") }, onClick = {
                            showPlaylistMenu = false
                            onChangeCoverClick?.invoke()
                        })

                        DropdownMenuItem(text = { Text("재생목록 제목 변경") }, onClick = {
                            showPlaylistMenu = false
                            onEditClick?.invoke()
                        })

                        DropdownMenuItem(text = { Text("재생목록 삭제", color = Color.Red) }, onClick = {
                            showPlaylistMenu = false
                            onDeleteClick?.invoke()
                        })
                    } else {
                        DropdownMenuItem(text = { Text("대표 이미지 변경") }, onClick = {
                            showPlaylistMenu = false
                            onChangeCoverClick?.invoke()
                        })

                        DropdownMenuItem(text = {
                            Text(
                                "기본 대표 이미지 설정", color = JokeboxColors.TextPrimary
                            )
                        }, onClick = {
                            showPlaylistMenu = false
                            onResetCoverClick?.invoke()
                        })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 앨범 커버 + 플레이리스트 정보
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            // 커버 이미지
            if (coverUri != null) {
                AsyncImage(
                    model = coverUri,
                    contentDescription = "Playlist Cover",
                    modifier = Modifier
                        .size(145.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(145.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(JokeboxColors.SurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "♫", color = JokeboxColors.Primary, fontSize = 52.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 제목, 곡 개수, 총 재생시간, 재생 버튼
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = JokeboxColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${songs.size} songs",
                    color = JokeboxColors.TextSecondary,
                    fontSize = 14.sp
                )

                Text(
                    text = totalDurationText, color = JokeboxColors.TextSecondary, fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onShuffleClick?.invoke() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD54A)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text(
                            text = "⤨", color = Color.Black, fontSize = 22.sp
                        )
                    }

                    Button(
                        onClick = { onPlayClick?.invoke() }, colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD54A)
                        ), contentPadding = PaddingValues(0.dp), modifier = Modifier.size(48.dp)
                    ) {
                        Text(
                            text = "▶", color = Color.Black, fontSize = 20.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (songs.isEmpty()) {
            Text(
                text = emptyMessage, color = JokeboxColors.TextSecondary, fontSize = 14.sp
            )
        } else {
            LazyColumn {
                items(songs) { song ->
                    SongItem(
                        song = song,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        }

        if (showEditSongsDialog) {
            AlertDialog(onDismissRequest = {
                showEditSongsDialog = false
                selectedSongIds = emptySet()
            }, containerColor = JokeboxColors.Surface, title = {
                Text(
                    text = "곡 선택", color = JokeboxColors.TextPrimary
                )
            }, text = {
                Column {
                    if (allSongs.isEmpty()) {
                        Text(
                            text = "추가할 수 있는 음악이 없습니다.", color = JokeboxColors.TextSecondary
                        )
                    } else {
                        Row(
                            modifier = Modifier.heightIn(max = 400.dp)
                        ) {
                            val songListState = rememberLazyListState()
                            val indexScope = rememberCoroutineScope()

                            val indexLetters =
                                listOf("#") + ('A'..'Z').map { it.toString() } + listOf(
                                    "ㄱ",
                                    "ㄴ",
                                    "ㄷ",
                                    "ㄹ",
                                    "ㅁ",
                                    "ㅂ",
                                    "ㅅ",
                                    "ㅇ",
                                    "ㅈ",
                                    "ㅊ",
                                    "ㅋ",
                                    "ㅌ",
                                    "ㅍ",
                                    "ㅎ"
                                )

                            fun getIndexKey(title: String): String {
                                val ch = title.firstOrNull { !it.isWhitespace() }?.uppercaseChar()
                                    ?: return "#"

                                if (ch in '가'..'힣') {
                                    val initials = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
                                    val index = (ch.code - '가'.code) / 588
                                    return when (initials[index]) {
                                        'ㄲ' -> "ㄱ"
                                        'ㄸ' -> "ㄷ"
                                        'ㅃ' -> "ㅂ"
                                        'ㅆ' -> "ㅅ"
                                        'ㅉ' -> "ㅈ"
                                        else -> initials[index].toString()
                                    }
                                }

                                return when {
                                    ch.isDigit() -> "#"
                                    ch in 'A'..'Z' -> ch.toString()
                                    else -> "#"
                                }
                            }

                            LazyColumn(
                                state = songListState, modifier = Modifier.weight(1f)
                            ) {
                                items(allSongs) { song ->
                                    val isSelected = song.id in selectedSongIds

                                    Row(modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedSongIds = if (isSelected) {
                                                selectedSongIds - song.id
                                            } else {
                                                selectedSongIds + song.id
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = isSelected, onCheckedChange = { checked ->
                                                selectedSongIds = if (checked) {
                                                    selectedSongIds + song.id
                                                } else {
                                                    selectedSongIds - song.id
                                                }
                                            })

                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = song.title,
                                                color = JokeboxColors.TextPrimary,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = song.artist,
                                                color = JokeboxColors.TextSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(400.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top
                            ) {
                                indexLetters.forEach { letter ->
                                    Text(
                                        text = letter,
                                        fontSize = 10.sp,
                                        lineHeight = 16.sp,
                                        color = JokeboxColors.TextSecondary,
                                        modifier = Modifier
                                            .clickable {
                                                val targetIndex = allSongs.indexOfFirst {
                                                    getIndexKey(it.title) == letter
                                                }

                                                if (targetIndex >= 0) {
                                                    indexScope.launch {
                                                        songListState.scrollToItem(targetIndex)
                                                    }
                                                }
                                            }
                                            .padding(vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }, confirmButton = {
                TextButton(
                    onClick = {
                        onEditSongs?.invoke(selectedSongIds.toList())
                        selectedSongIds = emptySet()
                        showEditSongsDialog = false
                    }) {
                    Text("저장", color = JokeboxColors.Primary)
                }
            }, dismissButton = {
                TextButton(
                    onClick = {
                        showEditSongsDialog = false
                        selectedSongIds = emptySet()
                    }) {
                    Text("취소", color = JokeboxColors.TextSecondary)
                }
            })
        }
    }
}

@Composable
fun PlaylistsContent(
    playlists: List<MusicPlaylist>,
    onCategoryClick: (String) -> Unit,
    onCreatePlaylist: (String, Uri?) -> Unit,
    onPlaylistClick: (MusicPlaylist) -> Unit,
    categoryCoverUris: Map<String, Uri>
) {
    var showCreateDialog by remember {
        mutableStateOf(value = false)
    }

    var newPlaylistName by remember {
        mutableStateOf("")
    }

    var selectedCoverUri by remember {
        mutableStateOf<Uri?>(null)
    }

    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedCoverUri = uri

        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // 권한을 유지할 수 없는 경우에도 선택한 이미지는 일단 사용
            }
        }
    }

    val categories = listOf(
        "최근에 추가한 곡" to "＋", "최근 재생한 곡" to "◷", "많이 재생한 곡" to "♫", "좋아요 표시한 곡" to "♥"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(JokeboxColors.Background),
        contentPadding = PaddingValues(
            start = 10.dp, end = 10.dp, top = 8.dp, bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 상단 고정 메뉴 4개
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                categories.chunked(2).forEach { rowCategories ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowCategories.forEach { (name, symbol) ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onCategoryClick(name)
                                    }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1.35f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(JokeboxColors.SurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val categoryKey = when (name) {
                                        "좋아요 표시한 곡" -> "Favorites"
                                        "최근에 추가한 곡" -> "Recently Added"
                                        "최근 재생한 곡" -> "Recently Played"
                                        "많이 재생한 곡" -> "Most Played"
                                        else -> ""
                                    }

                                    val coverUri = categoryCoverUris[categoryKey]

                                    if (coverUri != null) {
                                        AsyncImage(
                                            model = coverUri,
                                            contentDescription = "$name 커버",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = symbol,
                                            color = JokeboxColors.Primary,
                                            fontSize = 42.sp
                                        )
                                    }
                                }

                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )

                                Text(
                                    text = name,
                                    color = JokeboxColors.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        // 내 플레이리스트 제목
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "내 플레이리스트",
                    color = JokeboxColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "+",
                    color = JokeboxColors.Primary,
                    fontSize = 28.sp,
                    modifier = Modifier.clickable {
                        newPlaylistName = ""
                        showCreateDialog = true
                    })
            }
        }

        // 사용자가 만든 (커스텀)플레이리스트
        if (playlists.isEmpty()) {
            item {
                Text(
                    text = "아직 만든 플레이리스트가 없습니다.",
                    color = JokeboxColors.TextSecondary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else {
            items(playlists) { playlist ->
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onPlaylistClick(playlist)
                    }
                    .padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(JokeboxColors.SurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (playlist.coverUri != null) {
                            AsyncImage(
                                model = playlist.coverUri,
                                contentDescription = "플레이리스트 커버",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = "♫", color = JokeboxColors.Primary, fontSize = 30.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = playlist.name,
                            color = JokeboxColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "${playlist.songIds.size}곡",
                            color = JokeboxColors.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
            showCreateDialog = false
        },
            title = {
                Text("새 플레이리스트")
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("플레이리스트 이름") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }) {
                        Text("커버 이미지 선택")
                    }

                    selectedCoverUri?.let { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "플레이리스트 커버",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newPlaylistName.trim()

                    if (name.isNotEmpty()) {
                        onCreatePlaylist(name, selectedCoverUri)

                        // 다음 플레이리스트 생성을 위해 초기화
                        newPlaylistName = ""
                        selectedCoverUri = null

                        showCreateDialog = false
                    }
                }) {
                    Text("만들기")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    newPlaylistName = ""
                    selectedCoverUri = null
                    showCreateDialog = false
                }) {
                    Text("취소")
                }
            },
            containerColor = JokeboxColors.Surface,
            titleContentColor = JokeboxColors.TextPrimary,
            textContentColor = JokeboxColors.TextPrimary
        )
    }
}
