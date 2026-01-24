# Quick Start Guide

## Quick Build Commands

### Build APK
```powershell
.\gradlew.bat assembleDebug
```

### Build and Install (if device connected)
```powershell
.\build-apk.ps1
.\install-apk.ps1
```

### Or use Android Studio
1. Open project in Android Studio
2. Click **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
3. Click **Run** button to install on connected device

## APK Location
After building, find your APK at:
```
app\build\outputs\apk\debug\app-debug.apk
```

## First Time Setup

1. **Create `local.properties` file** in project root:
   ```
   sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
   ```
   Replace `YourUsername` with your Windows username.

2. **Install Android SDK** if not already installed:
   - Download Android Studio: https://developer.android.com/studio
   - Or install SDK Command Line Tools

3. **Build the project:**
   ```powershell
   .\gradlew.bat assembleDebug
   ```

## Install on Device

### Via ADB (recommended)
```powershell
adb install app\build\outputs\apk\debug\app-debug.apk
```

### Manual
1. Copy APK to phone
2. Enable "Install Unknown Apps" in settings
3. Open APK file and install

## Troubleshooting

- **"SDK not found"**: Create `local.properties` with correct SDK path
- **"Gradle sync failed"**: Check internet connection, dependencies need to be downloaded
- **"No device found"**: Enable USB debugging on your phone
