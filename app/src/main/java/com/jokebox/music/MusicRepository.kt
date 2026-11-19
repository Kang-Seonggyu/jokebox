package com.jokebox.music

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Song(
    val title: String,
    val artist: String,
    val duration: String,
    val id: Long,
    val uri: Uri,
    val albumArtUri: Uri?,
    val dateAdded: Long,
)

data class MusicPlaylist(
    val id: Long, val name: String, val songIds: Set<Long> = emptySet(), val coverUri: Uri? = null,
)

suspend fun loadMusic(context: Context): List<Song> = withContext(Dispatchers.IO) {

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
    val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

    context.contentResolver.query(
        collection, projection, selection, null, sortOrder
    )?.use { cursor ->

        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)

        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)

        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

        val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

        val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

        while (cursor.moveToNext()) {

            val id = cursor.getLong(idColumn)
            val albumId = cursor.getLong(albumIdColumn)
            val dateAdded = cursor.getLong(dateAddedColumn)

            val contentUri = ContentUris.withAppendedId(collection, id)

            val albumArtUri = if (albumId > 0) {
                ContentUris.withAppendedId(
                    "content://media/external/audio/albumart".toUri(), albumId
                )
            } else {
                null
            }

            val title = cursor.getString(titleColumn) ?: "Unknown"

            val artist = cursor.getString(artistColumn) ?: "Unknown Artist"

            val duration = cursor.getLong(durationColumn)

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
