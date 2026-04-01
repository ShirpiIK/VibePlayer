package com.vibeplayer.app.lyrics

import androidx.room.*

@Entity(tableName = "lyrics_cache")
data class LyricsCacheEntity(
    @PrimaryKey val key: String,
    @ColumnInfo(name = "synced_lyrics") val syncedLyrics: String = "",
    @ColumnInfo(name = "plain_lyrics") val plainLyrics: String = ""
)

@Dao
interface LyricsCacheDao {
    @Query("SELECT * FROM lyrics_cache WHERE `key` = :key LIMIT 1")
    suspend fun getLyrics(key: String): LyricsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LyricsCacheEntity)

    @Query("DELETE FROM lyrics_cache")
    suspend fun clearAll()
}

@Database(entities = [LyricsCacheEntity::class], version = 1, exportSchema = false)
abstract class VibeDatabase : RoomDatabase() {
    abstract fun lyricsCacheDao(): LyricsCacheDao
}
