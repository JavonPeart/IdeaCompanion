# Architecture and implementation plan

The app is a single Android module using Compose for UI, a foreground microphone service for explicit recording, Room for local metadata, and app-private files for AAC audio. `IdeaRepository` owns database operations and deletion. Room's FTS4 table indexes title and transcript. The library queries that index with sanitized token prefixes and orders results by recency.

## Current status

Recording and review are implemented as an initial vertical slice. The recording service finalizes audio before inserting metadata. Cancellation removes the temporary file. Service termination cancels an unfinished recording. The application disables Android backup and has no INTERNET permission.

The current build has no whisper.cpp native library or model import flow. Typed conversation commands now use a strict leading activation phrase, stored ideas, and optional Android TTS that selects a non-network English voice. Activation phrase and speech preference persist in DataStore. Voice input, widget access, and comprehensive automated tests remain to be implemented. The UI explicitly marks voice transcription unavailable.

## Next milestones

1. Add a `TranscriptionEngine` with whisper.cpp JNI, a model import screen, PCM conversion, and durable background work.
2. Add relevance ranking and date filtering to indexed search.
3. Feed a deliberately started listening session through the offline transcription engine into the existing command router.
4. Add a widget that opens a visible recording activity.
5. Add unit and instrumentation tests, then validate the full airplane-mode journey on a device.

## Optional local LLM

Keep command retrieval deterministic and available without a model. For optional conversational phrasing, evaluate Google's Gemma 3 270M instruction model through MediaPipe or LiteRT on device, or Gemma 3n E2B if memory and latency permit. Import models explicitly. Supply only retrieved snippets as context, require citations to idea IDs, and reject unsupported answers. No LLM integration exists yet.
