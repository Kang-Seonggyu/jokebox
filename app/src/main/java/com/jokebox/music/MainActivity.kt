package com.jokebox.music

import com.jokebox.music.ui.theme.JokeboxColors
import android.Manifest
import android.os.Build
import android.os.Bundle
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import android.content.pm.PackageManager
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.Player
import androidx.media3.common.C
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JokeboxApp()
        }
    }
}

data class Song(
    val title: String,
    val artist: String,
    val duration: String,
    val id: Long,
    val uri: Uri,
    val albumArtUri: Uri?,
    val dateAdded: Long
)

data class MusicPlaylist(
    val id: Long,
    val name: String,
    val songIds: Set<Long> = emptySet(),
    val coverUri: Uri? = null
)

private suspend fun loadMusic(context: Context): List<Song> =
    withContext(Dispatchers.IO) {

        val songs = mutableListOf<Song>()

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->

            val idColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)

            val titleColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)

            val artistColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

            val durationColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            val albumIdColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            val dateAddedColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {

                val id = cursor.getLong(idColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)

                val contentUri =
                    ContentUris.withAppendedId(collection, id)

                val albumArtUri = if (albumId > 0) {
                    ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )
                } else {
                    null
                }

                val title =
                    cursor.getString(titleColumn) ?: "Unknown"

                val artist =
                    cursor.getString(artistColumn) ?: "Unknown Artist"

                val duration =
                    cursor.getLong(durationColumn)

                if (duration <= 0L) continue

                val minutes = duration / 1000 / 60
                val seconds = (duration / 1000) % 60

                songs.add(
                    Song(
                        title = title,
                        artist = artist,
                        duration = "%d:%02d".format(minutes, seconds),
                        id = id,
                        uri = contentUri,
                        albumArtUri = albumArtUri,
                        dateAdded = dateAdded
                    )
                )
            }
        }

        songs
    }

@Composable
fun JokeboxApp() {
    var selectedTab by remember { mutableStateOf("Home") }
    // 검색어 상태
    var searchQuery by remember {
        mutableStateOf("")
    }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    // 좋아요한 음악 ID 목록
    var favoriteSongIds by remember {
        mutableStateOf<Set<Long>>(emptySet())
    }
    // 사용자가 만든 재생목록
    var playlists by remember {
        mutableStateOf<List<MusicPlaylist>>(emptyList())
    }
    var playlistsLoaded by remember {
        mutableStateOf(false)
    }
    // 즐겨찾기 데이터 로딩 완료 여부
    var favoritesLoaded by remember {
        mutableStateOf(false)
    }
    var recentlyPlayedSongIds by remember {
        mutableStateOf<List<Long>>(emptyList())
    }
    var recentlyPlayedLoaded by remember {
        mutableStateOf(false)
    }
    var playCounts by remember {
        mutableStateOf<Map<Long, Int>>(emptyMap())
    }

    var playCountsLoaded by remember {
        mutableStateOf(false)
    }
    // 현재 곡의 실제 재생 누적 시간
    var listenedDuration by remember {
        mutableLongStateOf(0L)
    }
    // 현재 재생 세션에서 카운트했는지 확인
    var playCountRegistered by remember {
        mutableStateOf(false)
    }

    var showNowPlaying by remember {
        mutableStateOf(false)
    }
    var selectedCategory by remember {
        mutableStateOf<String?>(null)
    }
    var selectedPlaylist by remember {
        mutableStateOf<MusicPlaylist?>(null)
    }

    BackHandler(
        enabled = showNowPlaying ||
                selectedPlaylist != null ||
                selectedCategory != null
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
    val context = androidx.compose.ui.platform.LocalContext.current
    // 앱 실행 시 저장된 즐겨찾기 불러오기
    LaunchedEffect(Unit) {
        val preferences = context.getSharedPreferences(
            "jokebox_preferences",
            0
        )

        favoriteSongIds = preferences
            .getStringSet("favorite_song_ids", emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()

        favoritesLoaded = true
    }
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences(
            "jokebox_preferences",
            android.content.Context.MODE_PRIVATE
        )

        val savedCounts = prefs.getString(
            "play_counts",
            ""
        )

        playCounts = savedCounts
            ?.split(",")
            ?.mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val id = parts[0].toLongOrNull()
                    val count = parts[1].toIntOrNull()

                    if (id != null && count != null) {
                        id to count
                    } else null
                } else null
            }
            ?.toMap()
            ?: emptyMap()

        playCountsLoaded = true
    }
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences(
            "jokebox_preferences",
            Context.MODE_PRIVATE
        )

        val savedData = prefs.getString("playlists", null)

        if (savedData != null) {
            try {
                val jsonArray = org.json.JSONArray(savedData)

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
            } catch (e: Exception) {
                playlists = emptyList()
            }
        }

        playlistsLoaded = true
    }
    LaunchedEffect(playlists, playlistsLoaded) {
        if (playlistsLoaded) {
            val jsonArray = org.json.JSONArray()

            playlists.forEach { playlist ->
                val songIdsJson = org.json.JSONArray()

                playlist.songIds.forEach { songId ->
                    songIdsJson.put(songId)
                }

                val json = org.json.JSONObject().apply {
                    put("id", playlist.id)
                    put("name", playlist.name)
                    put("songIds", songIdsJson)
                    put("coverUri", playlist.coverUri?.toString())
                }

                jsonArray.put(json)
            }

            val prefs = context.getSharedPreferences(
                "jokebox_preferences",
                Context.MODE_PRIVATE
            )

            prefs.edit()
                .putString("playlists", jsonArray.toString())
                .apply()
        }
    }

    LaunchedEffect(playCounts, playCountsLoaded) {
        if (playCountsLoaded) {
            val prefs = context.getSharedPreferences(
                "jokebox_preferences",
                android.content.Context.MODE_PRIVATE
            )

            val savedCounts = playCounts.entries.joinToString(",") {
                "${it.key}:${it.value}"
            }

            prefs.edit()
                .putString("play_counts", savedCounts)
                .apply()
        }
    }
    LaunchedEffect(recentlyPlayedSongIds, recentlyPlayedLoaded) {
        if (recentlyPlayedLoaded) {
            val prefs = context.getSharedPreferences(
                "jokebox_preferences",
                android.content.Context.MODE_PRIVATE
            )

            prefs.edit()
                .putString(
                    "recently_played_song_ids",
                    recentlyPlayedSongIds.joinToString(",")
                )
                .apply()
        }
    }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences(
            "jokebox_preferences",
            android.content.Context.MODE_PRIVATE
        )

        val savedIds = prefs.getString(
            "recently_played_song_ids",
            ""
        )

        recentlyPlayedSongIds = savedIds
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull() }
            ?: emptyList()

        recentlyPlayedLoaded = true
    }

    // 즐겨찾기 목록이 변경되면 자동 저장
    LaunchedEffect(favoriteSongIds, favoritesLoaded) {
        if (favoritesLoaded) {
            val preferences = context.getSharedPreferences(
                "jokebox_preferences",
                0
            )

            preferences.edit()
                .putStringSet(
                    "favorite_song_ids",
                    favoriteSongIds.map { it.toString() }.toSet()
                )
                .apply()
        }
    }

    val player = remember {
        ExoPlayer.Builder(context.applicationContext).build()
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
                context,
                audioPermission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // 2. 권한 요청
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
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
                mediaItem: MediaItem?,
                reason: Int
            ) {
                val currentUri = player.currentMediaItem
                    ?.localConfiguration
                    ?.uri

                val currentSong = currentSongsState.value.find {
                    it.uri == currentUri
                }

                if (currentSong != null) {
                    selectedSong = currentSong

                    // 새로운 곡의 재생 기록 시작
                    listenedDuration = 0L
                    playCountRegistered = false

                    // 최근 재생 기록 갱신 (최대 100곡)
                    recentlyPlayedSongIds = (
                            listOf(currentSong.id) +
                                    recentlyPlayedSongIds.filterNot {
                                        it == currentSong.id
                                    }
                            ).take(100)

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
        if (hasAudioPermission) {
            songs = loadMusic(context)
        } else {
            songs = emptyList()
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
            val currentSong = songs.getOrNull(player.currentMediaItemIndex)

            if (currentSong != null && currentSong != selectedSong) {
                selectedSong = currentSong
            }

            // 실제 재생 중일 때만 시간 누적
            if (
                player.isPlaying &&
                selectedSong != null &&
                !playCountRegistered
            ) {
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

            delay(500)
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
            containerColor = JokeboxColors.Background,
            bottomBar = {
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
                                }
                            )
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
            }
        ) { innerPadding ->

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
                    }
                )

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
                                songs = songs,
                                selectedSong = selectedSong,
                                onSongClick = { song ->
                                    selectedSong = song

                                    val mediaItems = songs.map { item ->
                                        MediaItem.fromUri(item.uri)
                                    }

                                    val selectedIndex = songs.indexOf(song)

                                    if (selectedIndex >= 0) {
                                        player.setMediaItems(
                                            mediaItems,
                                            selectedIndex,
                                            0L
                                        )
                                        player.prepare()
                                        player.play()
                                    }
                                }
                            )
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
                                            mediaItems,
                                            selectedIndex,
                                            0L
                                        )
                                        player.prepare()
                                        player.play()
                                    }
                                }
                            )
                        }

                        "Playlists" -> {
                            if (selectedPlaylist != null) {
                                PlaylistDetailContent(
                                    songs = songs.filter {
                                        it.id in selectedPlaylist!!.songIds
                                    },
                                    onSongClick = { song ->
                                        selectedSong = song

                                        val mediaItems = songs.map { item ->
                                            MediaItem.fromUri(item.uri)
                                        }

                                        val selectedIndex = songs.indexOf(song)

                                        if (selectedIndex >= 0) {
                                            player.setMediaItems(
                                                mediaItems,
                                                selectedIndex,
                                                0L
                                            )
                                            player.prepare()
                                            player.play()
                                        }
                                    },
                                    title = selectedPlaylist!!.name,
                                    emptyMessage = "아직 추가된 음악이 없습니다.",
                                            onBackClick = {
                                        selectedPlaylist = null
                                    }
                                )
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
                                    }
                                )
                            } else {
                                when (selectedCategory) {
                                    "최근에 추가한 곡" -> {
                                        val oneMonthAgo = java.time.LocalDateTime.now()
                                            .minusMonths(1)
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .toEpochSecond()

                                        val recentSongs = songs
                                            .filter { it.dateAdded >= oneMonthAgo }
                                            .sortedByDescending { it.dateAdded }

                                        PlaylistDetailContent(
                                            songs = recentSongs,
                                            onSongClick = { song ->
                                                val mediaItems = songs.map { item ->
                                                    MediaItem.fromUri(item.uri)
                                                }

                                                val selectedIndex = songs.indexOf(song)

                                                if (selectedIndex >= 0) {
                                                    player.setMediaItems(
                                                        mediaItems,
                                                        selectedIndex,
                                                        0L
                                                    )
                                                    player.prepare()
                                                    player.play()

                                                    selectedSong = song
                                                    showNowPlaying = true
                                                }
                                            },
                                            title = "최근에 추가한 곡",
                                        )

                                    }
                                    "최근 재생한 곡" -> {
                                        val recentlyPlayedSongs = recentlyPlayedSongIds.mapNotNull { id ->
                                            songs.find { it.id == id }
                                        }

                                        PlaylistDetailContent(
                                            songs = recentlyPlayedSongs,
                                            onSongClick = { song ->
                                                val mediaItems = songs.map { item ->
                                                    MediaItem.fromUri(item.uri)
                                                }

                                                val selectedIndex = songs.indexOf(song)

                                                if (selectedIndex >= 0) {
                                                    player.setMediaItems(
                                                        mediaItems,
                                                        selectedIndex,
                                                        0L
                                                    )
                                                    player.prepare()
                                                    player.play()

                                                    selectedSong = song
                                                    showNowPlaying = true
                                                }
                                            },
                                            title = "최근 재생한 곡"
                                        )
                                    }
                                    "많이 재생한 곡" -> {
                                        val mostPlayedSongs = playCounts.entries
                                            .sortedByDescending { it.value }
                                            .take(100)
                                            .mapNotNull { entry ->
                                                songs.find { it.id == entry.key }
                                            }

                                        PlaylistDetailContent(
                                            songs = mostPlayedSongs,
                                            title = "많이 재생한 곡",
                                            onSongClick = { song ->
                                                val mediaItems = songs.map { item ->
                                                    MediaItem.fromUri(item.uri)
                                                }

                                                val selectedIndex = songs.indexOf(song)

                                                if (selectedIndex >= 0) {
                                                    player.setMediaItems(
                                                        mediaItems,
                                                        selectedIndex,
                                                        0L
                                                    )
                                                    player.prepare()
                                                    player.play()

                                                    selectedSong = song
                                                    showNowPlaying = true
                                                }
                                            }
                                        )
                                    }
                                    "좋아요 표시한 곡" -> {
                                        PlaylistDetailContent(
                                            songs = songs.filter { it.id in favoriteSongIds },
                                            onSongClick = { song ->
                                                val mediaItems = songs.map { item ->
                                                    MediaItem.fromUri(item.uri)
                                                }

                                                val selectedIndex = songs.indexOf(song)

                                                if (selectedIndex >= 0) {
                                                    player.setMediaItems(
                                                        mediaItems,
                                                        selectedIndex,
                                                        0L
                                                    )
                                                    player.prepare()
                                                    player.play()

                                                    selectedSong = song
                                                    showNowPlaying = true
                                                }
                                            },
                                            title = "좋아요 표시한 곡"
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
                                            }
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
            text = "♫",
            fontSize = 28.sp,
            color = JokeboxColors.Primary
        )
    }
}

@Composable
fun HomeContent(
    songs: List<Song>,
    selectedSong: Song?,
    onSongClick: (Song) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = selectedSong?.let {
                    "Last selected: ${it.title}"
                } ?: "No music played yet.",
                color = JokeboxColors.TextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Home",
                color = JokeboxColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))
        }

        if (songs.isEmpty()) {
            item {
                Text(
                    text = "No music files found.",
                    color = JokeboxColors.TextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            items(songs) { song ->
                SongItem(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
fun SongItem(
    song: Song,
    onClick: () -> Unit
) {
    Surface(
        color = JokeboxColors.Surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        JokeboxColors.SurfaceVariant,
                        RoundedCornerShape(10.dp)
                    ),
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
                        text = "♫",
                        color = JokeboxColors.Primary,
                        fontSize = 24.sp
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
                    text = song.artist,
                    color = JokeboxColors.TextSecondary,
                    fontSize = 12.sp
                )
            }

            Text(
                text = song.duration,
                color = JokeboxColors.TextSecondary,
                fontSize = 12.sp
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
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
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
                        JokeboxColors.Surface,
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "♫",
                    color = JokeboxColors.Primary,
                    fontSize = 24.sp
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
                        text = song.artist,
                        color = JokeboxColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "⏮",
                    color = JokeboxColors.TextPrimary,
                    modifier = Modifier.clickable {
                        onPrevious()
                    }.padding(8.dp)
                )

                Text(
                    text = if (isPlaying) "Ⅱ" else "▶",
                    color = JokeboxColors.Primary,
                    fontSize = 22.sp,
                    modifier = Modifier.clickable {
                        onPlayPause()
                    }.padding(8.dp)
                )

                Text(
                    text = "⏭",
                    color = JokeboxColors.TextPrimary,
                    modifier = Modifier.clickable {
                        onNext()
                    }.padding(8.dp)
                )
            }

            // 재생 진행 바
            Slider(
                value = currentPosition.toFloat().coerceIn(
                    0f,
                    totalDuration.toFloat().coerceAtLeast(1f)
                ),
                onValueChange = {
                    onSeek(it.toLong())
                },
                valueRange = 0f..totalDuration.toFloat().coerceAtLeast(1f),
                enabled = totalDuration > 0L
            )

            // 현재 시간과 전체 시간
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
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
fun PlaceholderContent(message: String) {
    Text(
        text = message,
        color = JokeboxColors.TextSecondary,
        fontSize = 14.sp
    )
}

@Composable
fun PlaylistDetailContent (
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    title: String = "Favorites",
    emptyMessage: String = "아직 좋아요한 음악이 없습니다.",
    onBackClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JokeboxColors.Background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick
                ) {
                    Text(
                        text = "←",
                        color = JokeboxColors.TextPrimary,
                        fontSize = 28.sp
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = title,
                color = JokeboxColors.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (songs.isEmpty()) {
            Text(
                text = emptyMessage,
                color = JokeboxColors.TextSecondary,
                fontSize = 14.sp
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
    }
}
@Composable
fun SearchContent(
    songs: List<Song>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSongClick: (Song) -> Unit
) {
    val filteredSongs = songs.filter { song ->
        song.title.contains(searchQuery, ignoreCase = true) ||
                song.artist.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JokeboxColors.Background)
            .padding(10.dp, 8.dp, 10.dp, 24.dp)

    ) {
        Text(
            text = "Search",
            color = JokeboxColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Search songs or artists...")
            },
            singleLine = true,
            leadingIcon = {
                Text("⌕", fontSize = 24.sp)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Text(
                        text = "✕",
                        modifier = Modifier
                            .clickable {
                                onSearchQueryChange("")
                            }
                            .padding(8.dp)
                    )
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
            searchQuery.isBlank() -> {
                Text(
                    text = "Search your music library.",
                    color = JokeboxColors.TextSecondary,
                    fontSize = 14.sp
                )
            }

            filteredSongs.isEmpty() -> {
                Text(
                    text = "No results found.",
                    color = JokeboxColors.TextSecondary,
                    fontSize = 14.sp
                )
            }

            else -> {
                Text(
                    text = "${filteredSongs.size} results",
                    color = JokeboxColors.TextSecondary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn {
                    items(filteredSongs) { song ->
                        SongItem(
                            song = song,
                            onClick = {
                                onSongClick(song)
                            }
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
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⌄",
                fontSize = 32.sp,
                color = JokeboxColors.TextPrimary,
                modifier = Modifier.clickable {
                    onBack()
                }
            )

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
                        JokeboxColors.Surface,
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "♫",
                    fontSize = 100.sp,
                    color = JokeboxColors.Primary
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // 곡 정보
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
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
                    text = song.artist,
                    color = JokeboxColors.TextSecondary,
                    fontSize = 16.sp
                )
            }

            Text(
                text = if (isFavorite) "♥" else "♡",
                color = if (isFavorite) {
                    JokeboxColors.Primary
                } else {
                    JokeboxColors.TextSecondary
                },
                fontSize = 30.sp,
                modifier = Modifier
                    .clickable {
                        onToggleFavorite()
                    }
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 재생 진행 바
        Slider(
            value = currentPosition.toFloat().coerceIn(
                0f,
                totalDuration.toFloat().coerceAtLeast(1f)
            ),
            onValueChange = {
                onSeek(it.toLong())
            },
            valueRange = 0f..totalDuration.toFloat().coerceAtLeast(1f),
            enabled = totalDuration > 0L,
            colors = SliderDefaults.colors(
                thumbColor = JokeboxColors.Primary,
                activeTrackColor = JokeboxColors.Primary
            )
        )

        // 셔플 및 반복 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⤨",
                fontSize = 24.sp,
                color = if (shuffleEnabled) {
                    JokeboxColors.Primary
                } else {
                    JokeboxColors.TextSecondary
                },
                modifier = Modifier.clickable {
                    onToggleShuffle()
                }.padding(8.dp)
            )

            Text(
                text = when (repeatMode) {
                    Player.REPEAT_MODE_ALL -> "↻"
                    Player.REPEAT_MODE_ONE -> "↻¹"
                    else -> "↻"
                },
                fontSize = 24.sp,
                color = if (repeatMode != Player.REPEAT_MODE_OFF) {
                    JokeboxColors.Primary
                } else {
                    JokeboxColors.TextSecondary
                },
                modifier = Modifier.clickable {
                    onCycleRepeat()
                }.padding(8.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
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
                }
            )

            Text(
                text = if (isPlaying) "Ⅱ" else "▶",
                fontSize = 42.sp,
                color = JokeboxColors.Primary,
                modifier = Modifier.clickable {
                    onPlayPause()
                }
            )

            Text(
                text = "⏭",
                fontSize = 30.sp,
                color = JokeboxColors.TextPrimary,
                modifier = Modifier.clickable {
                    onNext()
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun PlaylistsContent(
    playlists: List<MusicPlaylist>,
    onCategoryClick: (String) -> Unit,
    onCreatePlaylist: (String, Uri?) -> Unit,
    onPlaylistClick: (MusicPlaylist) -> Unit
) {
    var showCreateDialog by remember {
        mutableStateOf(false)
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
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // 권한을 유지할 수 없는 경우에도 선택한 이미지는 일단 사용
            }
        }
    }

    val categories = listOf(
        "최근에 추가한 곡" to "＋",
        "최근 재생한 곡" to "◷",
        "많이 재생한 곡" to "♫",
        "좋아요 표시한 곡" to "♥"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(JokeboxColors.Background),
        contentPadding = PaddingValues(
            start = 10.dp,
            end = 10.dp,
            top = 8.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 화면 제목
        item {
            Text(
                text = "Playlists",
                color = JokeboxColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

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
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1.35f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(JokeboxColors.SurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = symbol,
                                        color = JokeboxColors.Primary,
                                        fontSize = 42.sp
                                    )
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
                    }
                )
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onPlaylistClick(playlist)
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                text = "♫",
                                color = JokeboxColors.Primary,
                                fontSize = 30.sp
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

                    TextButton(
                        onClick = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    ) {
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
                TextButton(
                    onClick = {
                        val name = newPlaylistName.trim()

                        if (name.isNotEmpty()) {
                            onCreatePlaylist(name, selectedCoverUri)

                            // 다음 플레이리스트 생성을 위해 초기화
                            newPlaylistName = ""
                            selectedCoverUri = null

                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("만들기")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        newPlaylistName = ""
                        selectedCoverUri = null
                        showCreateDialog = false
                    }
                ) {
                    Text("취소")
                }
            },
            containerColor = JokeboxColors.Surface,
            titleContentColor = JokeboxColors.TextPrimary,
            textContentColor = JokeboxColors.TextPrimary
        )
    }
}