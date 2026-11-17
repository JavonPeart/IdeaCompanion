# Verification

The Android SDK and Java 17 are installed locally. After fetching the required development dependencies, `gradlew.bat :app:assembleDebug :app:testDebugUnitTest` completed successfully. The two `SearchQueryTest` unit tests passed. The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.

After adding typed conversation and DataStore settings, the same build and unit test command passed again, including three command router tests. The newer APK installed on the connected device, launched, displayed the Conversation tab, and retained the existing saved idea. Conversation commands and TTS were not exercised on device.

## Connected device smoke test

On November 16, 2026, the debug APK installed through ADB on a Samsung SM-G991U1. The app launched and remained the resumed activity. The microphone permission prompt appeared, permission was granted, recording started, and the UI displayed its active state. Stopping and saving produced one idea in the Home count and Library. Its detail screen opened, and an app-private `.m4a` file existed under `files/recordings` (17,135 bytes). The playback button was triggered and the app process remained alive with no app crash in the inspected AndroidRuntime log. Audio output was not independently heard or measured. The test idea remains on the device.

Background recording, notification stop, deletion, permission denial, interruption handling, and spoken audio quality remain unverified. The full transcription and conversation journey cannot be validated until those features are implemented.
