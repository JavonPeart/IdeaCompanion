# Verification

The Android SDK and Java 17 are installed locally. After fetching the required development dependencies, `gradlew.bat :app:assembleDebug :app:testDebugUnitTest` completed successfully. The two `SearchQueryTest` unit tests passed. The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.

After adding typed conversation and DataStore settings, the same build and unit test command passed again, including three command router tests. The newer APK installed on the connected device, launched, displayed the Conversation tab, and retained the existing saved idea. Conversation commands and TTS were not exercised on device.

## Connected device smoke test

On November 16, 2026, the debug APK installed through ADB on a Samsung SM-G991U1. The app launched and remained the resumed activity. The microphone permission prompt appeared, permission was granted, recording started, and the UI displayed its active state. Stopping and saving produced one idea in the Home count and Library. Its detail screen opened, and an app-private `.m4a` file existed under `files/recordings` (17,135 bytes). The playback button was triggered and the app process remained alive with no app crash in the inspected AndroidRuntime log. Audio output was not independently heard or measured. The test idea remains on the device.

Background recording, notification stop, deletion, permission denial, interruption handling, and spoken audio quality remain unverified. The full transcription and conversation journey cannot be validated until those features are implemented.

## Whisper integration device test

Official NDK 27.0.12077973 and CMake 3.22.1 archives were downloaded and their SHA-1 checksums matched Google's SDK manifest. The model `ggml-tiny.en.bin` matched the publisher's SHA-256 (`921e4cf8686fdd993dcd081a5da5b6c365bfde1162e72b08d75ac75289920b1f`). `gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline` passed with the native library. The APK installed on the Samsung SM-G991U1; the model imported into app-private files (77,704,715 bytes). A retry of the existing short recording reached `COMPLETE` and stored a nonempty transcript. The first retry exposed an absent optional PCM encoding field in the device decoder; the fallback was fixed and the second run completed. The resulting words were not checked against audible speech, so transcription accuracy and silence hallucination behavior remain unverified. The model also remains in the phone's Downloads folder as an import source.

## Current update

`gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline` passed after adding WorkManager, quiet-audio rejection, manual voice command capture, widget, and recording timer. Evidence citation tests passed. The APK installed and launched on the Samsung with two existing ideas preserved. The widget provider appeared in `dumpsys appwidget`; placement on the launcher remains untested. A short manually started conversation recording showed the listening indicator; stopping ran Whisper and produced `[MUSIC]` from ambient audio. The activation gate rejected it without executing a command or speaking. The temporary command file was absent from the app cache afterward. WorkManager process recovery remains untested.

## Local Ollama model device test

On November 17, 2026, the existing Ollama `lfm3:latest` model was identified as a 1,099,627,808-byte GGUF blob. The official llama.cpp source was added as a pinned submodule and built for the app's arm64-v8a ABI with NDK 27.0.12077973. `gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline` passed. The 35.6 MB debug APK installed on the connected Samsung SM-G991U1, preserving the three saved ideas and Whisper model. The Ollama blob was streamed into the app-private `files/models` directory and verified on the phone with SHA-256 `3793e1e6e2266919d6e015287221b41100175a98eddaba92e111aed3b05c93c4`. The model is not in the APK or Git repository.

The Conversation screen ran `Hello Companion show my recent ideas`, displayed deterministic matches, then generated a local answer using the GGUF model. The final answer cited `[3]`, `[2]`, and `[1]`, and the matching source records remained visible. The first unoptimized debug build was very slow; native `-O3` made a 160-token bounded generation finish in under a minute during the final run. The phone process remained alive. Early model attempts emitted long or unfinished `<think>` text; the current bridge suppresses the think-start token when it is a single GGUF token, and `EvidenceGuard` rejects unfinished thinking and answers without known idea citations. The model can still produce poor summaries, so drafts remain labeled and deterministic retrieval is available independently.
