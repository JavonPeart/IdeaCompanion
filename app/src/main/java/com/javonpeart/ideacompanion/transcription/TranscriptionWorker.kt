package com.javonpeart.ideacompanion.transcription

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.javonpeart.ideacompanion.IdeaApp
import com.javonpeart.ideacompanion.data.TranscriptionStatus
import kotlinx.coroutines.CancellationException
import java.io.File

class TranscriptionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    companion object { const val ID = "idea_id" }
    override suspend fun doWork(): Result {
        val id = inputData.getLong(ID, -1)
        if (id < 0) return Result.failure()
        val app = applicationContext as IdeaApp
        val repo = app.repository
        val idea = repo.byId(id) ?: return Result.success()
        try {
            repo.update(idea.copy(transcriptionStatus = TranscriptionStatus.PROCESSING, transcriptionError = null))
            val text = WhisperEngine().transcribe(File(idea.audioFilePath), app.modelFile)
            repo.byId(id)?.let { repo.update(it.copy(transcript = text, transcriptionStatus = TranscriptionStatus.COMPLETE)) }
            return Result.success()
        } catch (e: CancellationException) { throw e }
        catch (e: Throwable) {
            val message = if (e is UnsatisfiedLinkError) "Native Whisper library is unavailable" else e.message ?: "Transcription failed"
            repo.byId(id)?.let { repo.update(it.copy(transcriptionStatus = TranscriptionStatus.FAILED, transcriptionError = message)) }
            return Result.failure()
        }
    }
}
