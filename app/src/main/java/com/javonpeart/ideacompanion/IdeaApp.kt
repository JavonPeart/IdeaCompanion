package com.javonpeart.ideacompanion

import android.app.Application
import androidx.room.Room
import com.javonpeart.ideacompanion.data.IdeaDatabase
import com.javonpeart.ideacompanion.repository.IdeaRepository
import com.javonpeart.ideacompanion.settings.SettingsRepository
import com.javonpeart.ideacompanion.transcription.WhisperEngine
import kotlinx.coroutines.*
import java.io.File

class IdeaApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val processing = mutableSetOf<Long>()
    val database by lazy { Room.databaseBuilder(this, IdeaDatabase::class.java, "ideas.db").build() }
    val repository by lazy { IdeaRepository(database.ideas()) }
    val settings by lazy { SettingsRepository(this) }
    val modelFile get() = File(filesDir, "models/ggml-tiny.en.bin")
    fun transcribe(id: Long) {
        synchronized(processing) { if (!processing.add(id)) return }
        scope.launch {
            try {
                val idea = repository.byId(id) ?: return@launch
                repository.update(idea.copy(transcriptionStatus = com.javonpeart.ideacompanion.data.TranscriptionStatus.PROCESSING, transcriptionError = null))
                val text = WhisperEngine().transcribe(File(idea.audioFilePath), modelFile)
                repository.byId(id)?.let { repository.update(it.copy(transcript = text, transcriptionStatus = com.javonpeart.ideacompanion.data.TranscriptionStatus.COMPLETE)) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                repository.byId(id)?.let { repository.update(it.copy(transcriptionStatus = com.javonpeart.ideacompanion.data.TranscriptionStatus.FAILED, transcriptionError = e.message ?: "Transcription failed")) }
            } catch (e: UnsatisfiedLinkError) {
                repository.byId(id)?.let { repository.update(it.copy(transcriptionStatus = com.javonpeart.ideacompanion.data.TranscriptionStatus.FAILED, transcriptionError = "Native Whisper library is unavailable")) }
            } finally { synchronized(processing) { processing.remove(id) } }
        }
    }
}
