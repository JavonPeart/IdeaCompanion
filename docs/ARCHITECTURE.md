# Architecture and implementation plan

The app is a single Android module using Compose for UI, a foreground microphone service for explicit recording, Room for local metadata, and app-private files for AAC audio. `IdeaRepository` owns database operations and deletion. Room's FTS4 table indexes title and transcript. The library queries that index with sanitized token prefixes and orders results by recency.

## Current status

Recording and review are implemented as an initial vertical slice. The recording service finalizes audio before inserting metadata. Cancellation removes the temporary file. Service termination cancels an unfinished recording. The application disables Android backup and has no INTERNET permission.

Whisper integration has a pinned upstream submodule, CMake/JNI bridge, AAC decoding to mono 16 kHz float PCM, explicit model import, automatic transcription after recording when a model exists, and retry/status handling. Android NDK 27.0.12077973 and CMake 3.22.1 are installed on the development computer. The native APK built, and the tiny English model ran on the connected phone, storing a transcript in Room. Accuracy on the ambient test recording was not independently validated. Typed conversation commands use a strict leading activation phrase, stored ideas, and optional Android TTS that selects a non-network English voice. Activation phrase and speech preference persist in DataStore. Voice input and widget access remain to be implemented.

## Next milestones

1. Replace app-scoped transcription jobs with durable WorkManager jobs and add silence handling to avoid misleading transcripts from quiet recordings.
2. Add relevance ranking and date filtering to indexed search.
3. Feed a deliberately started listening session through the offline transcription engine into the existing command router.
4. Add a widget that opens a visible recording activity.
5. Add unit and instrumentation tests, then validate the full airplane-mode journey on a device.

## Optional local LLM

Keep command retrieval deterministic and available without a model. For optional conversational phrasing, evaluate Google's Gemma 3 270M instruction model through MediaPipe or LiteRT on device, or Gemma 3n E2B if memory and latency permit. Import models explicitly. Supply only retrieved snippets as context, require citations to idea IDs, and reject unsupported answers. No LLM integration exists yet.
