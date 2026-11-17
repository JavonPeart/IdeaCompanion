package com.javonpeart.ideacompanion.repository

import com.javonpeart.ideacompanion.data.*
import kotlinx.coroutines.flow.Flow
import java.io.File

class IdeaRepository(private val dao: IdeaDao) {
    fun all(): Flow<List<Idea>> = dao.all()
    fun search(query: String): Flow<List<Idea>> = dao.search(query)
    suspend fun byId(id: Long) = dao.byId(id)
    suspend fun saveRecording(path: String, durationMs: Long, createdAt: Long) = dao.insert(Idea(createdAt = createdAt, audioFilePath = path, durationMs = durationMs))
    suspend fun update(idea: Idea) = dao.update(idea.copy(updatedAt = System.currentTimeMillis()))
    suspend fun delete(idea: Idea) {
        // Keep the database row if the audio cannot be removed.
        val file = File(idea.audioFilePath)
        if (file.exists() && !file.delete()) throw IllegalStateException("Could not delete recording")
        dao.delete(idea)
    }
}
