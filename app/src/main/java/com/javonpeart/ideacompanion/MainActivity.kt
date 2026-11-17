package com.javonpeart.ideacompanion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.javonpeart.ideacompanion.audio.RecordingService
import com.javonpeart.ideacompanion.data.Idea
import com.javonpeart.ideacompanion.search.SearchQuery
import com.javonpeart.ideacompanion.conversation.*
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as IdeaApp).repository
        val app = application as IdeaApp
        val settingsRepo = (application as IdeaApp).settings
        setContent {
            MaterialTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val state by RecordingService.state.collectAsState()
                val ideas by repo.all().collectAsState(initial = emptyList())
                val settings by settingsRepo.settings.collectAsState(initial = com.javonpeart.ideacompanion.settings.Settings("Hello Companion", true))
                val speech = remember { SpeechOutput(context) }
                DisposableEffect(Unit) { onDispose { speech.close() } }
                var page by remember { mutableStateOf("Home") }
                var selected by remember { mutableStateOf<Idea?>(null) }
                var search by remember { mutableStateOf("") }
                val ftsQuery = remember(search) { SearchQuery.fromInput(search) }
                val searchFlow = remember(ftsQuery) { if (ftsQuery.isBlank()) repo.all() else repo.search(ftsQuery) }
                val searchResults by searchFlow.collectAsState(initial = emptyList())
                var permissionError by remember { mutableStateOf(false) }
                val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    if (granted) ContextCompat.startForegroundService(context, Intent(context, RecordingService::class.java).setAction(RecordingService.START))
                    else permissionError = true
                }
                var importMessage by remember { mutableStateOf("") }
                val modelImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            app.modelFile.parentFile?.mkdirs()
                            context.contentResolver.openInputStream(uri)!!.use { input -> app.modelFile.outputStream().use { input.copyTo(it) } }
                            importMessage = "Model imported (${app.modelFile.length() / 1048576} MB)"
                        } catch (e: Exception) { app.modelFile.delete(); importMessage = "Import failed: ${e.message}" }
                    }
                }
                fun dispatch(action: String) { context.startService(Intent(context, RecordingService::class.java).setAction(action)) }
                Scaffold(bottomBar = {
                    NavigationBar {
                        listOf("Home", "Library", "Conversation", "Settings").forEach { destination ->
                            NavigationBarItem(selected = page == destination, onClick = { page = destination; selected = null }, icon = { Text("●") }, label = { Text(destination) })
                        }
                    }
                }) { padding ->
                    Column(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when {
                            selected != null -> {
                                val idea = ideas.firstOrNull { it.id == selected!!.id } ?: selected!!
                                Text(idea.title ?: "Untitled idea", style = MaterialTheme.typography.headlineMedium)
                                Text(DateFormat.getDateTimeInstance().format(Date(idea.createdAt)))
                                Text("Transcription: ${idea.transcriptionStatus}")
                                idea.transcriptionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                                Button(onClick = { app.transcribe(idea.id) }) { Text("Transcribe / retry") }
                                var title by remember(idea.id, idea.updatedAt) { mutableStateOf(idea.title.orEmpty()) }
                                var transcript by remember(idea.id, idea.updatedAt) { mutableStateOf(idea.transcript) }
                                OutlinedTextField(title, { title = it }, label = { Text("Title") })
                                OutlinedTextField(transcript, { transcript = it }, label = { Text("Transcript") }, minLines = 4)
                                Button(onClick = { scope.launch { repo.update(idea.copy(title = title.ifBlank { null }, transcript = transcript)); selected = null } }) { Text("Save edits") }
                                var player by remember { mutableStateOf<MediaPlayer?>(null) }
                                DisposableEffect(idea.id) { onDispose { player?.release(); player = null } }
                                Button(onClick = {
                                    player?.release()
                                    player = try { MediaPlayer().apply { setDataSource(idea.audioFilePath); prepare(); start(); setOnCompletionListener { it.release(); player = null } } } catch (_: Exception) { null }
                                }) { Text("Play original audio") }
                                var confirm by remember { mutableStateOf(false) }
                                Button(onClick = { confirm = true }) { Text("Delete idea") }
                                if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Delete this idea?") }, text = { Text("Its original audio will also be deleted.") }, confirmButton = { TextButton(onClick = { scope.launch { repo.delete(idea); selected = null } }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
                                TextButton(onClick = { selected = null }) { Text("Back") }
                            }
                            page == "Home" -> {
                                Text("Idea Companion", style = MaterialTheme.typography.headlineLarge)
                                Text("Capture a thought privately, whenever you choose.")
                                if (state.active) {
                                    Text("Recording…")
                                    Button(onClick = { dispatch(RecordingService.STOP) }) { Text("Stop and save") }
                                    TextButton(onClick = { dispatch(RecordingService.CANCEL) }) { Text("Cancel recording") }
                                } else Button(onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                                        ContextCompat.startForegroundService(context, Intent(context, RecordingService::class.java).setAction(RecordingService.START))
                                    else permission.launch(Manifest.permission.RECORD_AUDIO)
                                }) { Text("Record an idea") }
                                if (permissionError) Text("Microphone permission is needed to record.")
                                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                                Text("${ideas.size} saved ideas")
                            }
                            page == "Library" -> {
                                Text("Ideas Library", style = MaterialTheme.typography.headlineMedium)
                                OutlinedTextField(search, { search = it }, label = { Text("Search transcripts") })
                                val filtered = searchResults
                                if (filtered.isEmpty()) Text("No matching ideas yet.")
                                LazyColumn { items(filtered, key = { it.id }) { idea ->
                                    TextButton(onClick = { selected = idea }) {
                                        Column { Text(idea.title ?: "Untitled idea"); Text(DateFormat.getDateTimeInstance().format(Date(idea.createdAt))) }
                                    }
                                } }
                            }
                            page == "Conversation" -> {
                                Text("Conversation", style = MaterialTheme.typography.headlineMedium)
                                Text("Type an activated command to search your saved ideas. Voice input requires the offline transcription engine.")
                                var request by remember { mutableStateOf("") }
                                var response by remember { mutableStateOf("") }
                                OutlinedTextField(request, { request = it }, label = { Text("Command") })
                                Button(onClick = {
                                    val command = CommandRouter.parse(request, settings.activationPhrase)
                                    val matches = when (command) {
                                        Command.Recent -> ideas.take(5)
                                        Command.Latest -> ideas.take(1)
                                        is Command.Find -> ideas.filter { it.transcript.contains(command.term, true) || it.title.orEmpty().contains(command.term, true) }.take(5)
                                        Command.Today -> ideas.filter { java.time.Instant.ofEpochMilli(it.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == java.time.LocalDate.now() }
                                        Command.Yesterday -> ideas.filter { java.time.Instant.ofEpochMilli(it.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == java.time.LocalDate.now().minusDays(1) }
                                        else -> emptyList()
                                    }
                                    response = when (command) {
                                        null -> "Activation phrase required at the beginning."
                                        Command.Unsupported -> "That command is not available in this prototype."
                                        else -> if (matches.isEmpty()) "No matching recorded ideas." else matches.joinToString(". ") { it.title ?: it.transcript.ifBlank { "Untitled recording from ${DateFormat.getDateTimeInstance().format(Date(it.createdAt))}" } }
                                    }
                                    if (command != null && settings.speechEnabled) speech.speak(response)
                                }) { Text("Run command") }
                                if (response.isNotBlank()) Text(response)
                            }
                            else -> {
                                Text("Settings", style = MaterialTheme.typography.headlineMedium)
                                var phrase by remember(settings.activationPhrase) { mutableStateOf(settings.activationPhrase) }
                                OutlinedTextField(phrase, { phrase = it }, label = { Text("Activation phrase") })
                                Button(onClick = { if (phrase.isNotBlank()) scope.launch { settingsRepo.setPhrase(phrase) } }) { Text("Save phrase") }
                                Row { Text("Speak responses"); Switch(checked = settings.speechEnabled, onCheckedChange = { scope.launch { settingsRepo.setSpeechEnabled(it) } }) }
                                Text(if (speech.available) "Offline English voice available" else "No offline English voice available")
                                Text(if (app.modelFile.isFile) "Whisper model: ${app.modelFile.length() / 1048576} MB" else "No Whisper model imported")
                                Button(onClick = { modelImport.launch(arrayOf("application/octet-stream", "*/*")) }) { Text("Import Whisper model") }
                                if (importMessage.isNotBlank()) Text(importMessage)
                                Text("Recordings and transcripts stay in app-private storage on this device. Android backup is disabled.")
                                Text("Voice command capture is not yet available in this build.")
                            }
                        }
                    }
                }
            }
        }
    }
}
