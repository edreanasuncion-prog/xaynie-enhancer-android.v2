package com.xstudio.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "enhancement_history")
data class EnhancementHistoryEntity(
    @PrimaryKey val id: String,
    val mediaType: String,
    val originalUri: String,
    val enhancedFilePath: String,
    val resolutionLabel: String,
    val fpsLabel: String? = null,
    val isFavorite: Boolean = false,
    val createdAtEpochMs: Long,
    val expiresAtEpochMs: Long?
)

@Dao
interface HistoryDao {
    // Expired photo rows are hidden immediately; the cleanup worker removes them (and their files) shortly after.
    // Rows with a NULL expiry (videos) are always shown.
    @Query(
        "SELECT * FROM enhancement_history " +
            "WHERE expiresAtEpochMs IS NULL OR expiresAtEpochMs > (CAST(strftime('%s','now') AS INTEGER) * 1000) " +
            "ORDER BY createdAtEpochMs DESC"
    )
    fun observeAllHistory(): Flow<List<EnhancementHistoryEntity>>

    @Query(
        "SELECT * FROM enhancement_history WHERE isFavorite = 1 " +
            "AND (expiresAtEpochMs IS NULL OR expiresAtEpochMs > (CAST(strftime('%s','now') AS INTEGER) * 1000)) " +
            "ORDER BY createdAtEpochMs DESC"
    )
    fun observeFavorites(): Flow<List<EnhancementHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: EnhancementHistoryEntity)

    @Query("UPDATE enhancement_history SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("DELETE FROM enhancement_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM enhancement_history WHERE mediaType = 'PHOTO' AND expiresAtEpochMs IS NOT NULL AND expiresAtEpochMs <= :nowMs")
    suspend fun getExpiredPhotoItems(nowMs: Long): List<EnhancementHistoryEntity>
}

@Database(entities = [EnhancementHistoryEntity::class], version = 1, exportSchema = false)
abstract class XStudioDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: XStudioDatabase? = null

        fun getInstance(context: Context): XStudioDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    XStudioDatabase::class.java,
                    "xstudio_native.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
