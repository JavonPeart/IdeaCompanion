package com.javonpeart.ideacompanion.audio

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import com.javonpeart.ideacompanion.IdeaApp
import com.javonpeart.ideacompanion.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

data class RecordingState(val active: Boolean = false, val startedAt: Long = 0, val error: String? = null)

class RecordingService : Service() {
    companion object {
        const val START = "record.start"
        const val STOP = "record.stop"
        const val CANCEL = "record.cancel"
        val state = MutableStateFlow(RecordingState())
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recorder: MediaRecorder? = null
    private var file: File? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("recording", "Active recording", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            START -> if (recorder == null) startRecording()
            STOP -> finishRecording(true)
            CANCEL -> finishRecording(false)
        }
        return START_NOT_STICKY
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getService(this, 1, Intent(this, RecordingService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this, "recording").setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Recording an idea").setContentText("Tap to return to Idea Companion")
            .setContentIntent(open).addAction(Notification.Action.Builder(null, "Stop and save", stop).build()).setOngoing(true).build()
    }

    private fun startRecording() {
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(100, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            else startForeground(100, notification())
            val directory = File(filesDir, "recordings").apply { mkdirs() }
            val target = File.createTempFile("idea_", ".m4a", directory)
            file = target
            @Suppress("DEPRECATION")
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setOutputFile(target.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            state.value = RecordingState(active = true, startedAt = System.currentTimeMillis())
        } catch (e: Exception) {
            recorder?.release(); recorder = null
            file?.delete(); file = null
            state.value = RecordingState(error = e.message ?: "Recording failed")
            stopSelf()
        }
    }

    private fun finishRecording(save: Boolean) {
        val r = recorder ?: return
        val started = state.value.startedAt
        var valid = save
        try { r.stop() } catch (_: RuntimeException) { valid = false }
        r.release(); recorder = null
        val target = file; file = null
        state.value = RecordingState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (valid && target != null && target.length() > 0) {
            scope.launch {
                try { (application as IdeaApp).repository.saveRecording(target.absolutePath, System.currentTimeMillis() - started, started) }
                catch (e: Exception) { state.value = RecordingState(error = "Could not save recording: ${e.message}") }
                finally { stopSelf() }
            }
        } else { target?.delete(); stopSelf() }
    }

    override fun onDestroy() {
        if (recorder != null) finishRecording(false)
        scope.cancel()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
