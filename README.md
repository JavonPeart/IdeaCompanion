# Idea Companion

An early Android prototype for manual, private voice capture. It records AAC audio in app-private storage and stores metadata in Room. There is no account, cloud service, analytics, or INTERNET permission. Android backup is disabled.

This is **not yet the complete offline voice companion** described in the project specification. whisper.cpp transcription builds and ran on a connected Android phone with an imported tiny English model. Spoken command input is not implemented. Saved ideas can also be titled and transcribed manually. A typed command screen supports deterministic retrieval and optional local Android TTS.

## Build

Install Android Studio, JDK 17, and Android SDK platform 35. Open this directory in Android Studio and sync Gradle, or run `gradlew.bat :app:assembleDebug :app:testDebugUnitTest` with `ANDROID_HOME` set to the SDK path. The build uses Gradle 9.6, Android Gradle Plugin 8.7.3, Kotlin 2.0.21, Compose BOM 2024.10.01, and Room 2.6.1. Dependency download is needed on the first sync. See [testing notes](docs/TESTING.md) for verification status.

For transcription, run `git submodule update --init --recursive`, then install Android NDK 27.0.12077973 and CMake 3.22.1 with Android Studio’s SDK Manager. Obtain the [English tiny whisper.cpp model](https://huggingface.co/ggerganov/whisper.cpp/blob/main/ggml-tiny.en.bin) (`ggml-tiny.en.bin`, SHA-256 `921e4cf8686fdd993dcd081a5da5b6c365bfde1162e72b08d75ac75289920b1f`) and import it through Settings. The app does not download models in the background. NDK and CMake belong on the development computer; the compiled native library is packaged in the APK for the phone.

## Privacy and use

Grant microphone permission and tap **Record an idea**. Stop and save using the app or its recording notification. The library allows playback, title/transcript edits, and deletion. Audio and the database are stored under the app's private data directory. Uninstalling the app removes that data. Android cloud backup and device transfer extraction are excluded.

See [architecture](docs/ARCHITECTURE.md) for the remaining implementation plan and feature status.
