# isFake

<p align="center">
  <img src="assets/app-icon.svg" alt="isFake app icon" width="112" />
</p>

isFake is an Android app for choosing a location and publishing it as a mock location through Android's test location provider. Choose coordinates on a map or enter them manually, save favorite locations, and control the simulation from a persistent notification.

## Features

- Pick a location on an OpenStreetMap-based map or enter latitude and longitude manually.
- Start and stop simulation with a foreground service.
- Configure the update interval, accuracy, and altitude.
- Save and manage favorite locations locally.
- Use a persistent notification with an action to stop the service.
- Switch between Spanish and English using Android's per-app language settings.

## Requirements

- Android 12 or later (API 31+).
- JDK 17 to build the project.
- Android SDK with platform 36 installed.
- Internet access to load map tiles.

## Device Setup

Android requires you to explicitly select an app for mock locations:

1. Enable Developer options in your device settings. On most devices, tap the build number seven times in the phone's About section.
2. In Developer options, open **Select mock location app** and choose **isFake**. The setting's name and location can vary by manufacturer.
3. Open isFake, choose a location on the map or enter its coordinates, then start the simulation.
4. Grant the location and notification permissions requested by Android.

## Build

From the project root, run:

```bash
./gradlew :app:assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. To install it on a connected device using ADB, run:

```bash
./gradlew :app:installDebug
```

## Tech Stack

- Kotlin 2.1.21 and Java 17.
- Jetpack Compose and Material 3.
- OSMDroid 6.1.20 for map display.
- Hilt for dependency injection.
- AndroidX DataStore Preferences and Kotlin Coroutines/Flow for persistence and state.
- Android `LocationManager` and a foreground service for mock location updates.

The app has `minSdk 31`, `targetSdk 35`, and compiles against Android SDK 36. Plugin and dependency versions are maintained in `gradle/libs.versions.toml`.

## Project Structure

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/isfake/app/
│   ├── data/
│   │   ├── datastore/PreferencesDataStore.kt
│   │   ├── model/FakeLocation.kt
│   │   └── repository/LocationRepository.kt
│   ├── di/AppModule.kt
│   ├── service/MockLocationService.kt
│   ├── ui/
│   │   ├── components/
│   │   │   ├── BatteryOptimizationDialog.kt
│   │   │   ├── MapView.kt
│   │   │   └── SettingsDialog.kt
│   │   ├── screens/home/
│   │   │   ├── HomeScreen.kt
│   │   │   └── HomeViewModel.kt
│   │   └── theme/
│   │       ├── Color.kt
│   │       ├── Theme.kt
│   │       └── Type.kt
│   ├── IsFakeApp.kt
│   └── MainActivity.kt
└── res/
    ├── values/strings.xml
    ├── values-en/strings.xml
    └── xml/locales_config.xml
```