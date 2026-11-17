# Architecture and implementation plan

The app is a single Android module using Compose for UI, a foreground microphone service for explicit recording, Room for local metadata, and app-private files for AAC audio. `IdeaRepository` owns database operations and deletion. Room's FTS4 table indexes title and transcript. The library queries that index with sanitized token prefixes and orders results by recency.

## Current status

Recording and review are implemented as an initial vertical slice. The recording service finalizes audio before inserting metadata. Cancellation removes the temporary file. Service termination cancels an unfinished recording. The application disables Android backup and has no INTERNET permission.

Whisper integration has a pinned upstream submodule, CMake/JNI bridge, AAC decoding to mono 16 kHz float PCM, explicit model import, and retry/status handling. Unique WorkManager jobs now persist transcription requests. A simple RMS threshold rejects quiet recordings before inference. The earlier native APK ran Whisper on the connected phone; these newer worker and silence changes still need device testing. Typed and deliberately recorded voice commands use a strict leading activation phrase, stored ideas, and optional Android TTS that selects a non-network English voice. Activation phrase and speech preference persist in DataStore. A widget opens the visible app.

## Next milestones

1. Validate WorkManager recovery and the quiet-audio threshold on real recordings. Use a foreground worker for long recordings.
2. Add relevance ranking and date filtering to indexed search.
3. Test deliberate voice command sessions and offline TTS on device with audible speech.
4. Test the widget on the device launcher.
5. Add unit and instrumentation tests, then validate the full airplane-mode journey on a device.

## Optional local LLM

Keep command retrieval deterministic and available without a model. Optional local generation now uses llama.cpp with the already downloaded Ollama LFM3 GGUF file. The app supplies only retrieved snippets as context, requires known idea-ID citations, labels output as an AI draft, and displays its sources. Citation checking cannot prove factual accuracy. The model is stored in app-private storage and generation runs on the phone CPU without an INTERNET permission.
