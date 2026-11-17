package com.javonpeart.ideacompanion.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class TranscriptionStatus { PENDING, PROCESSING, COMPLETE, FAILED }

@Entity(tableName = "ideas")
data class Idea(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val title: String? = null,
    val transcript: String = "",
    val audioFilePath: String,
    val durationMs: Long,
    val transcriptionStatus: TranscriptionStatus = TranscriptionStatus.PENDING,
    val transcriptionError: String? = null
)

@Fts4(contentEntity = Idea::class)
@Entity(tableName = "ideas_fts")
data class IdeaFts(val title: String?, val transcript: String)

class StatusConverters {
    @TypeConverter fun fromStatus(value: TranscriptionStatus) = value.name
    @TypeConverter fun toStatus(value: String) = TranscriptionStatus.valueOf(value)
}

@Dao interface IdeaDao {
    @Insert suspend fun insert(idea: Idea): Long
    @Update suspend fun update(idea: Idea)
    @Delete suspend fun delete(idea: Idea)
    @Query("SELECT * FROM ideas WHERE id = :id") suspend fun byId(id: Long): Idea?
    @Query("SELECT * FROM ideas ORDER BY createdAt DESC, id DESC") fun all(): Flow<List<Idea>>
    @Query("SELECT ideas.* FROM ideas JOIN ideas_fts ON ideas.id = ideas_fts.rowid WHERE ideas_fts MATCH :query ORDER BY ideas.createdAt DESC, ideas.id DESC") fun search(query: String): Flow<List<Idea>>
}

@Database(entities = [Idea::class, IdeaFts::class], version = 1, exportSchema = true)
@TypeConverters(StatusConverters::class)
abstract class IdeaDatabase : RoomDatabase() { abstract fun ideas(): IdeaDao }
