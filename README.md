# Music Remote — Native Android APK

Native Kotlin + Jetpack Compose Android application for controlling YouTube Music on Windows over TLS WebSockets (`wss://`).

## Building the Installable APK

### Option 1: GitHub Actions (No Android Studio Needed)
1. Push this `android-app` folder to a GitHub repository.
2. Open the **Actions** tab and run **Build Android APK**.
3. Download the **`MusicRemote-debug-apk`** artifact and install `app-debug.apk` on your Android phone.

### Option 2: Android Studio
1. Open this folder in **Android Studio**.
2. Wait for Gradle sync to complete.
3. Click **Build -> Build Bundle(s) / APK(s) -> Build APK(s)** (or run `./gradlew assembleDebug`).
4. Install `app/build/outputs/apk/debug/app-debug.apk` on your phone.
