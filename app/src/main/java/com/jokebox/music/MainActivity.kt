package com.jokebox.music

import com.jokebox.music.ui.theme.JokeboxColors
import android.Manifest
import android.os.Build
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.Player
import androidx.media3.common.C
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JokeboxApp()
        }
    }
}

@Composable
fun JokeboxApp() {
    val context = LocalContext.current
    val categoryCoverPrefs = remember {
        context.getSharedPreferences(
            "jokebox_preferences", Context.MODE_PRIVATE
        )
    }

    var selectedTab by remember { mutableStateOf("Home") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var favoriteSongIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    var playlists by remember { mutableStateOf<List<MusicPlaylist>>(emptyList()) }
    var selectedPlaylist by remember { mutableStateOf<MusicPlaylist?>(null) }
    var favoritesLoaded by remember { mutableStateOf(false) }

    var recentlyPlayedSongIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var recentlyPlayedLoaded by remember { mutableStateOf(false) }

    var playCounts by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    var playCountsLoaded by remember { mutableStateOf(false) }
    var playCountRegistered by remember { mutableStateOf(false) }

    var showNowPlaying by remember { mutableStateOf(false) }
    var listenedDuration by remember { mutableLongStateOf(0L) }

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var playlistsLoaded by remember { mutableStateOf(false) }

    var showEditPlaylistDialog by remember { mutableStateOf(false) }
    var showDeletePlaylistDialog by remember { mutableStateOf(false) }

    var pendingPlaylistCoverId by remember { mutableStateOf<Long?>(null) }

    var categoryCoverUris by remember {
        mutableStateOf(
            listOf(
                "Favorites", "Recently Added", "Recently Played", "Most Played"
            ).mapNotNull { key ->
                val savedUri = categoryCoverPrefs.getString(
                    "category_cover_$key", null
                )

                if (savedUri != null) {
                    key to savedUri.toUri()
                } else {
                    null
                }
            }.toMap()
        )
    }

    BackHandler(
        enabled = showNowPlaying || selectedPlaylist != null || selectedCategory != null
    ) {
        when {
            showNowPlaying -> {
                showNowPlaying = false
            }

            selectedPlaylist != null -> {
                selectedPlaylist = null
            }

            selectedCategory != null -> {
                selectedCategory = null
            }
        }
    }


    // 앱 실행 시 저장된 즐겨찾기 불러오기
    LaunchedEffect(Unit) {
        val preferences = context.getSharedPreferences(
            "jokebox_preferences", 0
        )

        favoriteSongIds = preferences.getStringSet("favorite_song_ids", emptySet())
            ?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()

        favoritesLoaded = true
    }
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences(
            "jokebox_preferences", Context.MODE_PRIVATE
        )

        val savedCounts = prefs.getString(
            "play_counts", ""
        )

        playCounts = savedCounts?.split(",")?.mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val id = parts[0].toLongOrNull()
                val count = parts[1].toIntOrNull()

                if (id != null && count != null) {
                    id to count
                } else null
            } else null
        }?.toMap() ?: emptyMap()

        playCountsLoaded = true
    }
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences(
            "jokebox_preferences", Context.MODE_PRIVATE
        )

        val savedData = prefs.getString("playlists", null)

        if (savedData != null) {
            try {
                val jsonArray = JSONArray(savedData)

                playlists = List(jsonArray.length()) { index ->
                    val json = jsonArray.getJSONObject(index)

                    val songIdsJson = json.getJSONArray("songIds")
                    val songIds = buildSet {
                        for (i in 0 until songIdsJson.length()) {
                            add(songIdsJson.getLong(i))
                        }
                    }

                    MusicPlaylist(
                        id = json.getLong("id"),
                        name = json.getString("name"),
                        songIds = songIds,
                        coverUri = if (json.isNull("coverUri")) {
                            null
                        } else {
                            Uri.parse(json.getString("coverUri"))
                        }
                    )
                }
            } catch (_: Exception) {
                playlists = emptyList()
            }
        }

        playlistsLoaded = true
    }
    LaunchedEffect(playlists, playlistsLoaded) {
        if (playlistsLoaded) {
            val jsonArray = JSONArray()

            playlists.forEach { playlist ->
                val songIdsJson = JSONArray()

                playlist.songIds.forEach { songId ->
                    songIdsJson.put(songId)
                }

                val json = JSONObject().apply {
                    put("id", playlist.id)
                    put("name", playlist.name)
                    put("songIds", songIdsJson)
                    put("coverUri", playlist.coverUri?.toString())
                }

                jsonArray.put(json)
            }

            val prefs = context.getSharedPreferences(
                "jokebox_preferences", Context.MODE_PRIVATE
            )

            prefs.edit {
                putString("playlists", jsonArray.toString())
            }
        }
    }

    LaunchedEffect(playCounts, playCountsLoaded) {
        if (playCountsLoaded) {
            val prefs = context.getSharedPreferences(
                "jokebox_preferences", Context.MODE_PRIVATE
            )

            val savedCounts = playCounts.entries.joinToString(",") {
                "${it.key}:${it.value}"
            }

            prefs.edit {
                putString("play_counts", savedCounts)
            }
        }
    }
    LaunchedEffect(recentlyPlayedSongIds, recentlyPlayedLoaded) {
        if (recentlyPlayedLoaded) {
            val prefs = context.getSharedPreferences(
                "jokebox_preferences", Context.MODE_PRIVATE
            )

            prefs.edit {
                putString(
                    "recently_played_song_ids", recentlyPlayedSongIds.joinToString(",")
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences(
            "jokebox_preferences", Context.MODE_PRIVATE
        )

        val savedIds = prefs.getString(
            "recently_played_song_ids", ""
        )

        recentlyPlayedSongIds =
            savedIds?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()

        recentlyPlayedLoaded = true
    }

    // 즐겨찾기 목록이 변경되면 자동 저장
    LaunchedEffect(favoriteSongIds, favoritesLoaded) {
        if (favoritesLoaded) {
            val preferences = context.getSharedPreferences(
                "jokebox_preferences", 0
            )

            preferences.edit {
                putStringSet(
                    "favorite_song_ids", favoriteSongIds.map { it.toString() }.toSet()
                )
            }
        }
    }

    val player = remember {
        ExoPlayer.Builder(context.applicationContext).build()
    }

    fun playCategorySongs(categorySongs: List<Song>, shuffle: Boolean = false) {
        val queue = if (shuffle) categorySongs.shuffled() else categorySongs
        if (queue.isNotEmpty()) {
            val mediaItems = queue.map { song -> MediaItem.fromUri(song.uri) }
            selectedSong = queue.first()
            player.setMediaItems(mediaItems, 0, 0L)
            player.prepare()
            player.play()
        }
    }

    // 현재 재생 상태
    var isPlaying by remember {
        mutableStateOf(player.isPlaying)
    }

    var shuffleEnabled by remember {
        mutableStateOf(false)
    }

    var repeatMode by remember {
        mutableIntStateOf(Player.REPEAT_MODE_OFF)
    }

    var currentPosition by remember {
        mutableLongStateOf(0L)
    }

    var totalDuration by remember {
        mutableLongStateOf(0L)
    }

    // 1. 음악 접근 권한 확인
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, audioPermission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // 2. 권한 요청
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
    }
    var pendingCategoryCoverKey by remember {
        mutableStateOf<String?>(null)
    }

    val playlistCoverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val playlistId = pendingPlaylistCoverId

        if (uri != null && playlistId != null) {
            val updatedPlaylist = playlists.find {
                it.id == playlistId
            }?.copy(coverUri = uri)

            if (updatedPlaylist != null) {
                playlists = playlists.map { playlist ->
                    if (playlist.id == playlistId) {
                        updatedPlaylist
                    } else {
                        playlist
                    }
                }

                selectedPlaylist = updatedPlaylist
            }

            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
        }

        pendingPlaylistCoverId = null
    }
    val categoryCoverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && pendingCategoryCoverKey != null) {
            categoryCoverUris = categoryCoverUris + (pendingCategoryCoverKey!! to uri)
            categoryCoverPrefs.edit {
                putString("category_cover_${pendingCategoryCoverKey!!}", uri.toString())
            }

            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
        }
    }

    // 3. 실제 음악 목록
    var songs by remember {
        mutableStateOf(emptyList<Song>())
    }

    // 플레이어 상태 감지
    val currentSongsState = rememberUpdatedState(songs)

    DisposableEffect(player) {
        val listener = object : Player.Listener {

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(
                mediaItem: MediaItem?, reason: Int
            ) {
                val currentUri = player.currentMediaItem?.localConfiguration?.uri

                val currentSong = currentSongsState.value.find {
                    it.uri == currentUri
                }

                if (currentSong != null) {
                    selectedSong = currentSong

                    // 새로운 곡의 재생 기록 시작
                    listenedDuration = 0L
                    playCountRegistered = false

                    // 최근 재생 기록 갱신 (최대 100곡)
                    recentlyPlayedSongIds =
                        (listOf(currentSong.id) + recentlyPlayedSongIds.filterNot {
                            it == currentSong.id
                        }).take(100)

                    // todos: 재생 횟수 증가
                }
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }


    LaunchedEffect(Unit) {
        if (!hasAudioPermission) {
            permissionLauncher.launch(audioPermission)
        }
    }

    LaunchedEffect(hasAudioPermission) {
        songs = if (hasAudioPermission) {
            loadMusic(context)
        } else {
            emptyList()
        }
    }

    LaunchedEffect(player, isPlaying, selectedSong, songs) {
        while (true) {
            currentPosition = player.currentPosition.coerceAtLeast(0L)

            val duration = player.duration
            totalDuration = if (duration != C.TIME_UNSET) {
                duration.coerceAtLeast(0L)
            } else {
                0L
            }

            // 현재 재생 중인 곡을 화면에 반영
            val currentUri = player.currentMediaItem?.localConfiguration?.uri

            val currentSong = songs.find { it.uri == currentUri }

            if (currentSong != null && currentSong != selectedSong) {
                selectedSong = currentSong
            }

            // 실제 재생 중일 때만 시간 누적
            if (player.isPlaying && selectedSong != null && !playCountRegistered) {
                listenedDuration += 500L

                // 누적 재생 시간이 30초 이상이면 카운트
                if (listenedDuration >= 30_000L) {
                    val songId = selectedSong!!.id

                    playCounts = playCounts.toMutableMap().apply {
                        this[songId] = (this[songId] ?: 0) + 1
                    }

                    playCountRegistered = true
                }
            }

            delay(500.milliseconds)
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = JokeboxColors.Primary,
            background = JokeboxColors.Background,
            surface = JokeboxColors.Surface
        )
    ) {
        Scaffold(
            containerColor = JokeboxColors.Background, bottomBar = {
                if (!showNowPlaying) {
                    Column {
                        selectedSong?.let { song ->
                            MiniPlayer(
                                song = song,
                                isPlaying = isPlaying,
                                currentPosition = currentPosition,
                                totalDuration = totalDuration,
                                onPlayPause = {
                                    if (player.isPlaying) player.pause()
                                    else player.play()
                                },
                                onPrevious = {
                                    if (player.currentPosition > 3000L) {
                                        player.seekTo(0L)
                                    } else {
                                        player.seekToPreviousMediaItem()
                                    }
                                },
                                onNext = {
                                    player.seekToNextMediaItem()
                                },
                                onSeek = { position ->
                                    player.seekTo(position)
                                },
                                onExpand = {
                                    showNowPlaying = true
                                })
                        }

                        NavigationBar(
                            containerColor = JokeboxColors.Surface,
                            contentColor = JokeboxColors.TextPrimary
                        ) {
                            val tabs = listOf(
                                "Home" to "⌂",
                                "Search" to "⌕",
                                "Playlists" to "♫",
                                "Settings" to "⚙"
                            )

                            tabs.forEach { (tab, symbol) ->
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = {
                                        selectedTab = tab
                                        selectedPlaylist = null
                                        selectedCategory = null
                                    },
                                    icon = {
                                        Text(text = symbol, fontSize = 22.sp)
                                    },
                                    label = { Text(tab) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = JokeboxColors.Primary,
                                        selectedTextColor = JokeboxColors.Primary,
                                        indicatorColor = Color(0xFF33221F),
                                        unselectedIconColor = JokeboxColors.TextSecondary,
                                        unselectedTextColor = JokeboxColors.TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }) { innerPadding ->

            if (showNowPlaying && selectedSong != null) {

                NowPlayingContent(
                    song = selectedSong!!,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    totalDuration = totalDuration,
                    onBack = {
                        showNowPlaying = false
                    },
                    onPlayPause = {
                        if (player.isPlaying) player.pause()
                        else player.play()
                    },
                    onPrevious = {
                        if (player.currentPosition > 3000L) {
                            player.seekTo(0L)
                        } else {
                            player.seekToPreviousMediaItem()
                        }
                    },
                    onNext = {
                        player.seekToNextMediaItem()
                    },
                    onSeek = { position ->
                        player.seekTo(position)
                    },
                    shuffleEnabled = shuffleEnabled,
                    repeatMode = repeatMode,

                    onToggleShuffle = {
                        shuffleEnabled = !shuffleEnabled
                        player.shuffleModeEnabled = shuffleEnabled
                    },

                    onCycleRepeat = {
                        repeatMode = when (repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }

                        player.repeatMode = repeatMode
                    },
                    isFavorite = favoriteSongIds.contains(selectedSong!!.id),

                    onToggleFavorite = {
                        val songId = selectedSong!!.id

                        favoriteSongIds = if (favoriteSongIds.contains(songId)) {
                            favoriteSongIds - songId
                        } else {
                            favoriteSongIds + songId
                        }
                    })

            } else {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Header()

                    Spacer(modifier = Modifier.height(28.dp))

                    when (selectedTab) {
                        "Home" -> {
                            HomeContent(
                                songs = songs, selectedSong = selectedSong, onSongClick = { song ->
                                    selectedSong = song

                                    val mediaItems = songs.map { item ->
                                        MediaItem.fromUri(item.uri)
                                    }

                                    val selectedIndex = songs.indexOf(song)

                                    if (selectedIndex >= 0) {
                                        player.setMediaItems(
                                            mediaItems, selectedIndex, 0L
                                        )
                                        player.prepare()
                                        player.play()
                                    }
                                })
                        }

                        "Search" -> {
                            SearchContent(
                                songs = songs,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                onSongClick = { song ->
                                    selectedSong = song

                                    val mediaItems = songs.map { item ->
                                        MediaItem.fromUri(item.uri)
                                    }

                                    val selectedIndex = songs.indexOf(song)

                                    if (selectedIndex >= 0) {
                                        player.setMediaItems(
                                            mediaItems, selectedIndex, 0L
                                        )
                                        player.prepare()
                                        player.play()
                                    }
                                })
                        }

                        "Playlists" -> {
                            if (selectedPlaylist != null) {
                                val playlistSongs = songs.filter {
                                    it.id in selectedPlaylist!!.songIds
                                }
                                PlaylistDetailContent(
                                    songs = playlistSongs,
                                    onSongClick = { song ->
                                        val mediaItems = playlistSongs.map { item ->
                                            MediaItem.fromUri(item.uri)
                                        }

                                        val selectedIndex = playlistSongs.indexOfFirst {
                                            it.id == song.id
                                        }

                                        if (selectedIndex >= 0) {
                                            selectedSong = song

                                            player.setMediaItems(
                                                mediaItems, selectedIndex, 0L
                                            )
                                            player.prepare()
                                            player.play()
                                        }
                                    },
                                    onEditClick = {
                                        showEditPlaylistDialog = true
                                    },
                                    onDeleteClick = {
                                        showDeletePlaylistDialog = true
                                    },
                                    title = selectedPlaylist!!.name,
                                    emptyMessage = "아직 추가된 음악이 없습니다.",
                                    onBackClick = {
                                        selectedPlaylist = null
                                    },
                                    onEditSongs = { selectedIds ->
                                        val updatedPlaylist = selectedPlaylist!!.copy(
                                            songIds = selectedIds.toSet()
                                        )

                                        playlists = playlists.map { playlist ->
                                            if (playlist.id == updatedPlaylist.id) {
                                                updatedPlaylist
                                            } else {
                                                playlist
                                            }
                                        }

                                        selectedPlaylist = updatedPlaylist

                                        val jsonArray = JSONArray()

                                        playlists.forEach { playlist ->
                                            val jsonObject = JSONObject().apply {
                                                put("id", playlist.id)
                                                put("name", playlist.name)
                                                put("songIds", JSONArray(playlist.songIds.toList()))
                                                put("coverUri", playlist.coverUri?.toString())
                                            }

                                            jsonArray.put(jsonObject)
                                        }

                                        categoryCoverPrefs.edit {
                                            putString("playlists", jsonArray.toString())
                                        }
                                    },
                                    onPlayClick = {
                                        if (playlistSongs.isNotEmpty()) {
                                            val mediaItems = playlistSongs.map { item ->
                                                MediaItem.fromUri(item.uri)
                                            }

                                            selectedSong = playlistSongs.first()

                                            player.setMediaItems(mediaItems, 0, 0L)
                                            player.prepare()
                                            player.play()
                                        }
                                    },
                                    onShuffleClick = {
                                        if (playlistSongs.isNotEmpty()) {
                                            val shuffledSongs = playlistSongs.shuffled()

                                            val mediaItems = shuffledSongs.map { item ->
                                                MediaItem.fromUri(item.uri)
                                            }

                                            selectedSong = shuffledSongs.first()

                                            player.setMediaItems(mediaItems, 0, 0L)
                                            player.prepare()
                                            player.play()
                                        }
                                    },
                                    allSongs = songs,
                                    isCustomPlaylist = true,
                                    coverUri = selectedPlaylist!!.coverUri,
                                    onChangeCoverClick = {
                                        pendingPlaylistCoverId = selectedPlaylist!!.id

                                        playlistCoverPicker.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    })

                                if (showEditPlaylistDialog && selectedPlaylist != null) {
                                    var editedPlaylistName by remember(selectedPlaylist!!.id) {
                                        mutableStateOf(selectedPlaylist!!.name)
                                    }

                                    AlertDialog(onDismissRequest = {
                                        showEditPlaylistDialog = false
                                    }, title = {
                                        Text("Edit Playlist")
                                    }, text = {
                                        OutlinedTextField(
                                            value = editedPlaylistName,
                                            onValueChange = {
                                                editedPlaylistName = it
                                            },
                                            label = {
                                                Text("Playlist Name")
                                            },
                                            singleLine = true
                                        )
                                    }, confirmButton = {
                                        TextButton(
                                            onClick = {
                                                val updatedPlaylist = selectedPlaylist!!.copy(
                                                    name = editedPlaylistName.trim()
                                                )

                                                playlists = playlists.map { playlist ->
                                                    if (playlist.id == updatedPlaylist.id) {
                                                        updatedPlaylist
                                                    } else {
                                                        playlist
                                                    }
                                                }

                                                selectedPlaylist = updatedPlaylist

                                                val jsonArray = JSONArray()
                                                playlists.forEach { playlist ->
                                                    val jsonObject = JSONObject().apply {
                                                        put("id", playlist.id)
                                                        put("name", playlist.name)
                                                        put(
                                                            "songIds",
                                                            JSONArray(playlist.songIds.toList())
                                                        )
                                                        put(
                                                            "coverUri",
                                                            playlist.coverUri?.toString()
                                                        )
                                                    }
                                                    jsonArray.put(jsonObject)
                                                }

                                                categoryCoverPrefs.edit {
                                                    putString("playlists", jsonArray.toString())
                                                }

                                                showEditPlaylistDialog = false
                                            }) {
                                            Text("Save")
                                        }
                                    }, dismissButton = {
                                        TextButton(
                                            onClick = {
                                                showEditPlaylistDialog = false
                                            }) {
                                            Text("Cancel")
                                        }
                                    })
                                }
                                if (showDeletePlaylistDialog) {
                                    AlertDialog(onDismissRequest = {
                                        showDeletePlaylistDialog = false
                                    }, title = {
                                        Text("재생목록 삭제")
                                    }, text = {
                                        Text(" '${selectedPlaylist?.name}' 재생목록을 삭제하시겠습니까?\n삭제하면 복구할 수 없습니다.")
                                    }, confirmButton = {
                                        TextButton(
                                            onClick = {
                                                playlists = playlists.filter {
                                                    it.id != selectedPlaylist?.id
                                                }
                                                selectedPlaylist = null
                                                selectedTab = "Playlists"
                                                showDeletePlaylistDialog = false
                                            }) {
                                            Text("삭제", color = Color.Red)
                                        }
                                    }, dismissButton = {
                                        TextButton(
                                            onClick = {
                                                showDeletePlaylistDialog = false
                                            }) {
                                            Text("취소")
                                        }
                                    })
                                }
                            } else if (selectedCategory == null) {
                                PlaylistsContent(
                                    playlists = playlists,
                                    onCategoryClick = { category ->
                                        selectedCategory = category
                                    },
                                    onCreatePlaylist = { name, coverUri ->
                                        val newPlaylist = MusicPlaylist(
                                            id = System.currentTimeMillis(),
                                            name = name,
                                            coverUri = coverUri
                                        )
                                        playlists = playlists + newPlaylist
                                    },
                                    onPlaylistClick = { playlist ->
                                        selectedPlaylist = playlist
                                    },
                                    categoryCoverUris = categoryCoverUris
                                )
                            } else {
                                when (selectedCategory) {
                                    "최근에 추가한 곡" -> {
                                        val oneMonthAgo =
                                            java.time.LocalDateTime.now().minusMonths(1)
                                                .atZone(java.time.ZoneId.systemDefault())
                                                .toEpochSecond()

                                        val recentSongs =
                                            songs.filter { it.dateAdded >= oneMonthAgo }
                                                .sortedByDescending { it.dateAdded }

                                        PlaylistDetailContent(
                                            onBackClick = { selectedCategory = null },
                                            onPlayClick = { playCategorySongs(recentSongs) },
                                            onShuffleClick = { playCategorySongs(recentSongs, shuffle = true) },
                                            songs = recentSongs,
                                            onSongClick = { song ->
                                                val mediaItems = songs.map { item ->
                                                    MediaItem.fromUri(item.uri)
                                                }

                                                val selectedIndex = songs.indexOf(song)

                                                if (selectedIndex >= 0) {
                                                    player.setMediaItems(
                                                        mediaItems, selectedIndex, 0L
                                                    )
                                                    player.prepare()
                                                    player.play()

                                                    selectedSong = song
                                                    showNowPlaying = true
                                                }
                                            },
                                            emptyMessage = "최근에 추가한 음악이 없습니다.",
                                            coverUri = categoryCoverUris["Recently Added"],
                                            onChangeCoverClick = {
                                                pendingCategoryCoverKey = "Recently Added"
                                                categoryCoverPicker.launch(
                                                    PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                                    )
                                                )
                                            },
                                            onResetCoverClick = {
                                                categoryCoverUris =
                                                    categoryCoverUris - "Recently Added"

                                                categoryCoverPrefs.edit {
                                                    remove("category_cover_Recently Added")
                                                }
                                            },
                                            title = "최근에 추가한 곡",
                                            allSongs = songs,
                                        )

                                    }

                                    "최근 재생한 곡" -> {
                                        val recentlyPlayedSongs =
                                            recentlyPlayedSongIds.mapNotNull { id ->
                                                songs.find { it.id == id }
                                            }

                                        PlaylistDetailContent(
                                            onBackClick = { selectedCategory = null },
                                            onPlayClick = { playCategorySongs(recentlyPlayedSongs) },
                                            onShuffleClick = { playCategorySongs(recentlyPlayedSongs, shuffle = true) },
                                            songs = recentlyPlayedSongs,
                                            onSongClick = { song ->
                                                val mediaItems = songs.map { item ->
                                                    MediaItem.fromUri(item.uri)
                                                }

                                                val selectedIndex = songs.indexOf(song)

                                                if (selectedIndex >= 0) {
                                                    player.setMediaItems(
                                                        mediaItems, selectedIndex, 0L
                                                    )
                                                    player.prepare()
                                                    player.play()

                                                    selectedSong = song
                                                    showNowPlaying = true
                                                }
                                            },
                                            coverUri = categoryCoverUris["Recently Played"],
                                            onChangeCoverClick = {
                                                pendingCategoryCoverKey = "Recently Played"
                                                categoryCoverPicker.launch(
                                                    PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                                    )
                                                )
                                            },
                                            onResetCoverClick = {
                                                categoryCoverUris =
                                                    categoryCoverUris - "Recently Played"
                                                categoryCoverPrefs.edit {
                                                    remove("category_cover_Recently Played")
                                                }
                                            },
                                            title = "최근 재생한 곡",
                                            allSongs = songs,
                                        )
                                    }

                                    "많이 재생한 곡" -> {
                                        val mostPlayedSongs =
                                            playCounts.entries.sortedByDescending { it.value }
                                                .take(100).mapNotNull { entry ->
                                                    songs.find { it.id == entry.key }
                                                }

                                        PlaylistDetailContent(
                                            onBackClick = { selectedCategory = null },
                                            onPlayClick = { playCategorySongs(mostPlayedSongs) },
                                            onShuffleClick = { playCategorySongs(mostPlayedSongs, shuffle = true) },
                                            songs = mostPlayedSongs,
                                            onSongClick = { song ->
                                                val mediaItems = songs.map { item ->
                                                    MediaItem.fromUri(item.uri)
                                                }

                                                val selectedIndex = songs.indexOf(song)

                                                if (selectedIndex >= 0) {
                                                    player.setMediaItems(
                                                        mediaItems, selectedIndex, 0L
                                                    )
                                                    player.prepare()
                                                    player.play()

                                                    selectedSong = song
                                                    showNowPlaying = true
                                                }
                                            },
                                            coverUri = categoryCoverUris["Most Played"],
                                            onChangeCoverClick = {
                                                pendingCategoryCoverKey = "Most Played"
                                                categoryCoverPicker.launch(
                                                    PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                                    )
                                                )
                                            },
                                            onResetCoverClick = {
                                                categoryCoverUris =
                                                    categoryCoverUris - "Most Played"
                                                categoryCoverPrefs.edit {
                                                    remove("category_cover_Most Played")
                                                }
                                            },
                                            title = "많이 재생한 곡",
                                            allSongs = songs,
                                        )
                                    }

                                    "좋아요 표시한 곡" -> {
                                        PlaylistDetailContent(
                                            onBackClick = { selectedCategory = null },
                                            onPlayClick = { playCategorySongs(songs.filter { it.id in favoriteSongIds }) },
                                            onShuffleClick = { playCategorySongs(songs.filter { it.id in favoriteSongIds }, shuffle = true) },
                                            songs = songs.filter { it.id in favoriteSongIds },
                                            onSongClick = { song ->
                                                val mediaItems = songs.map { item ->
                                                    MediaItem.fromUri(item.uri)
                                                }

                                                val selectedIndex = songs.indexOf(song)

                                                if (selectedIndex >= 0) {
                                                    player.setMediaItems(
                                                        mediaItems, selectedIndex, 0L
                                                    )
                                                    player.prepare()
                                                    player.play()

                                                    selectedSong = song
                                                    showNowPlaying = true
                                                }
                                            },
                                            emptyMessage = "아직 좋아요한 음악이 없습니다.",
                                            title = "좋아요 표시한 곡",
                                            allSongs = songs,
                                            coverUri = categoryCoverUris["Favorites"],
                                            onChangeCoverClick = {
                                                pendingCategoryCoverKey = "Favorites"
                                                categoryCoverPicker.launch(
                                                    PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                                    )
                                                )
                                            },
                                            onResetCoverClick = {
                                                categoryCoverUris = categoryCoverUris - "Favorites"
                                                categoryCoverPrefs.edit {
                                                    remove("category_cover_Favorites")
                                                }
                                            },
                                        )
                                    }

                                    else -> {
                                        PlaylistsContent(
                                            playlists = playlists,
                                            onCategoryClick = { category ->
                                                selectedCategory = category
                                            },
                                            onCreatePlaylist = { name, coverUri ->
                                                val newPlaylist = MusicPlaylist(
                                                    id = System.currentTimeMillis(),
                                                    name = name,
                                                    coverUri = coverUri
                                                )

                                                playlists = playlists + newPlaylist
                                            },
                                            onPlaylistClick = { playlist ->
                                                selectedPlaylist = playlist
                                            },
                                            categoryCoverUris = categoryCoverUris
                                        )
                                    }
                                }
                            }
                        }

                        else -> {
                            PlaceholderContent("Jokebox settings will appear here.")
                        }
                    }
                }
            }
        }
    }
}



