package com.javonpeart.ideacompanion

import android.app.Application
import androidx.room.Room
import com.javonpeart.ideacompanion.data.IdeaDatabase
import com.javonpeart.ideacompanion.repository.IdeaRepository
import com.javonpeart.ideacompanion.settings.SettingsRepository
import com.javonpeart.ideacompanion.transcription.WhisperEngine
import androidx.work.*
import com.javonpeart.ideacompanion.transcription.TranscriptionWorker
import java.io.File

class IdeaApp : Application() {
    val database by lazy { Room.databaseBuilder(this, IdeaDatabase::class.java, "ideas.db").build() }
    val repository by lazy { IdeaRepository(database.ideas()) }
    val settings by lazy { SettingsRepository(this) }
    val modelFile get() = File(filesDir, "models/ggml-tiny.en.bin")
    val gemmaFile get() = File(filesDir, "models/gemma.litertlm")
    fun transcribe(id: Long) {
        val request = OneTimeWorkRequestBuilder<TranscriptionWorker>()
            .setInputData(workDataOf(TranscriptionWorker.ID to id)).build()
        WorkManager.getInstance(this).enqueueUniqueWork("transcribe-$id", ExistingWorkPolicy.KEEP, request)
    }
}
