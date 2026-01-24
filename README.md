# Android Camera App

A simple Android camera application with quick switching between multiple cameras (Front, Macro, Wide, and USB cameras).

## Features

- Quick camera switching with dedicated buttons
- Support for multiple camera types:
  - Front camera
  - Rear cameras (Macro, Wide, Main)
  - USB cameras (when connected)
- Simple and intuitive UI
- Real-time camera preview

## Prerequisites

Before building the app, ensure you have:

1. **Java Development Kit (JDK) 8 or higher**
   - Download from: https://www.oracle.com/java/technologies/downloads/
   - Or use OpenJDK: https://adoptium.net/

2. **Android Studio** (recommended) or **Android SDK Command Line Tools**
   - Android Studio: https://developer.android.com/studio
   - Command Line Tools: https://developer.android.com/studio#command-tools
   
3. **Android SDK** with:
   - Android SDK Platform 34
   - Android SDK Build-Tools
   - Android Support Repository

## Project Setup

1. **Set Android SDK Path**
   
   Create a file named `local.properties` in the project root with your Android SDK path:
   
   ```
   sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
   ```
   
   Or on Windows:
   ```
   sdk.dir=C:/Users/YourUsername/AppData/Local/Android/Sdk
   ```

2. **Download Gradle Wrapper** (if not present)
   
   The project uses Gradle wrapper. If `gradle/wrapper/gradle-wrapper.jar` is missing, download it or use Android Studio to sync the project.

## Building the APK

### Method 1: Using Gradle Wrapper (Command Line)

Open PowerShell or Command Prompt in the project directory and run:

```powershell
# For debug APK
.\gradlew.bat assembleDebug

# For release APK (requires signing configuration)
.\gradlew.bat assembleRelease
```

The APK will be generated at:
- Debug: `app\build\outputs\apk\debug\app-debug.apk`
- Release: `app\build\outputs\apk\release\app-release.apk`

### Method 2: Using Android Studio

1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to `D:\work\android-camera` and select it
4. Wait for Gradle sync to complete
5. Go to **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
6. Wait for the build to complete
7. Click on the notification to locate the APK file

### Method 3: Using Build Script

Run the provided build script:

```powershell
.\build-apk.ps1
```

## Installation Instructions

### Option 1: Install via ADB (Android Debug Bridge)

1. **Enable Developer Options on your Android device:**
   - Go to **Settings** → **About Phone**
   - Tap **Build Number** 7 times
   - Go back to **Settings** → **Developer Options**
   - Enable **USB Debugging**

2. **Connect your device via USB** and allow USB debugging when prompted

3. **Install the APK:**
   ```powershell
   adb install app\build\outputs\apk\debug\app-debug.apk
   ```

### Option 2: Transfer and Install Manually

1. **Transfer the APK to your device:**
   - Copy `app-debug.apk` to your phone via USB, email, or cloud storage

2. **Enable Unknown Sources:**
   - Go to **Settings** → **Security** (or **Apps** → **Special Access**)
   - Enable **Install Unknown Apps** or **Unknown Sources**

3. **Install the APK:**
   - Open the APK file on your device using a file manager
   - Tap **Install** and follow the prompts

### Option 3: Install via Android Studio

1. Connect your device via USB with USB debugging enabled
2. In Android Studio, click the **Run** button (green play icon)
3. Select your device from the list
4. The app will be built and installed automatically

## Usage

1. **Launch the app** from your app drawer
2. **Grant camera permission** when prompted
3. **Use the buttons at the bottom** to switch between cameras:
   - **◀ Prev / Next ▶**: Cycle through all available cameras
   - **Front**: Switch to front camera
   - **Macro**: Switch to macro camera (if available)
   - **Wide**: Switch to wide camera (if available)
   - **USB**: Switch to USB camera (if connected)

## Troubleshooting

### Build Issues

- **"SDK location not found"**: Create `local.properties` file with correct SDK path
- **"Gradle sync failed"**: Ensure you have internet connection for downloading dependencies
- **"Java version incompatible"**: Update to JDK 8 or higher

### Camera Issues

- **"No cameras found"**: Ensure your device has camera hardware
- **"Camera permission denied"**: Grant camera permission in device settings
- **USB camera not detected**: USB cameras may require additional drivers or specific USB OTG support

### Installation Issues

- **"App not installed"**: Uninstall any previous version first, or check if device has enough storage
- **"Parse error"**: Ensure the APK file is not corrupted, try downloading/building again

## Project Structure

```
android-camera/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/androidcamera/app/
│   │       │   └── MainActivity.java
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   └── activity_main.xml
│   │       │   └── values/
│   │       │       └── strings.xml
│   │       └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
├── gradle.properties
└── README.md
```

## Requirements

- **Minimum Android Version**: Android 7.0 (API 24)
- **Target Android Version**: Android 14 (API 34)
- **Camera Permission**: Required

## License

This project is provided as-is for educational and personal use.
