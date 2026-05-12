# Oasis Android

Native Android client for Oasis, a manga reading app with account login, MangaDex browsing, saved library titles, reading history, and profile management.

## Tech Stack

- Kotlin
- Android Gradle Plugin 8.3.2
- Gradle wrapper
- AndroidX AppCompat, Activity, ConstraintLayout
- Material Components
- Min SDK 24, target SDK 34, compile SDK 36

## Features

- Login and registration
- Home dashboard
- MangaDex browse and series details
- Chapter reader
- Saved library
- Reading history
- Profile updates, profile photo upload, password change, and account deletion

## Project Structure

```text
app/src/main/java/com/oasis/mobile
+-- app          # App entry setup
+-- data         # API client, models, local stores, config
+-- screens      # Android activities grouped by screen
+-- utils        # Shared UI and helper utilities
```

Key resource folders:

- `app/src/main/res/layout` - screen layouts
- `app/src/main/res/drawable` - reusable drawables and navigation icons
- `app/src/main/res/drawable-nodpi` - Oasis logo assets

## Getting Started

1. Open Android Studio.
2. Choose **Open**.
3. Select this folder: `C:\Users\MY PC\OasisProject\oasis-android`.
4. Let Android Studio sync Gradle.
5. Run the `app` configuration on an emulator or Android device.

The first sync may download Gradle and Android dependencies.

## Backend Configuration

The app reads backend URLs from:

```text
app/src/main/java/com/oasis/mobile/data/ApiConfig.kt
```

Current defaults:

- `http://192.168.1.5:8080/api` for a real device on the same Wi-Fi network
- `http://10.0.2.2:8080/api` for an Android emulator calling a backend running on the host computer

Update `ApiConfig.BASE_URLS` if your backend runs on a different host or port.

## Useful Commands

Run these from the project root:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
.\gradlew.bat connectedAndroidTest
```

## Git Notes

The repository ignores local Android Studio files, Gradle caches, build outputs, and `local.properties`. Do not commit machine-specific SDK paths, generated build folders, or signing keys.
