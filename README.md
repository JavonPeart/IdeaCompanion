# Idea Companion

An early Android prototype for manual, private voice capture. It records AAC audio in app-private storage and stores metadata in Room. There is no account, cloud service, analytics, or INTERNET permission. Android backup is disabled.

This is a development prototype. whisper.cpp transcription builds and has run on a connected Android phone with an imported tiny English model. Conversation offers a manually started voice command session and deterministic retrieval. Optional local GGUF generation uses llama.cpp and requires explicit model import.

## Build

Install Android Studio, JDK 17, and Android SDK platform 35. Open this directory in Android Studio and sync Gradle, or run `gradlew.bat :app:assembleDebug :app:testDebugUnitTest` with `ANDROID_HOME` set to the SDK path. The build uses Gradle 9.6, Android Gradle Plugin 8.7.3, Kotlin 2.3.21, Compose BOM 2024.10.01, Room 2.8.5, and WorkManager 2.9.0. Dependency download is needed on the first sync. See [testing notes](docs/TESTING.md) for verification status.

For transcription and conversation, run `git submodule update --init --recursive`, then install Android NDK 27.0.12077973 and CMake 3.22.1 with Android Studio’s SDK Manager. Obtain the [English tiny whisper.cpp model](https://huggingface.co/ggerganov/whisper.cpp/blob/main/ggml-tiny.en.bin) (`ggml-tiny.en.bin`, SHA-256 `921e4cf8686fdd993dcd081a5da5b6c365bfde1162e72b08d75ac75289920b1f`) and import it through Settings. The app does not download models in the background. NDK and CMake belong on the development computer; the compiled native libraries are packaged in the APK for the phone.

The conversation model is the existing local Ollama `lfm3:latest` download, a 1.1 GB `LFM3-1.7B-Q4_K_M.gguf` blob. Its manifest points to the GGUF file in `%USERPROFILE%\.ollama\models\blobs\sha256-3793e1e6e2266919d6e015287221b41100175a98eddaba92e111aed3b05c93c4`. Copy that file to the phone and import it through Settings, or use `adb` with a debug build to put it in the app's private `files/models/lfm3-1.7b-q4_k_m.gguf`. The model is not committed to GitHub or bundled in the APK. Deterministic retrieval works without it. AI responses are labeled drafts and checked for citations to recorded ideas.

## Privacy and use

Grant microphone permission and tap **Record an idea**. Stop and save using the app or its recording notification. The library allows playback, title/transcript edits, and deletion. Audio and the database are stored under the app's private data directory. Uninstalling the app removes that data. Android cloud backup and device transfer extraction are excluded.

The widget opens the visible app for deliberate recording. Conversation voice sessions use temporary cache audio and delete it after recognition. Unique WorkManager jobs handle transcription; long recordings may exceed its execution window. Quiet-audio rejection uses a simple RMS threshold that needs broader testing.

See [architecture](docs/ARCHITECTURE.md) for the remaining implementation plan and feature status.
