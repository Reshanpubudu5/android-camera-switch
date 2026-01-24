# Installation script for Android Camera APK
# This script installs the APK on a connected Android device via ADB

Write-Host "Installing Android Camera APK..." -ForegroundColor Green

# Check if adb is available
$adbPath = Get-Command adb -ErrorAction SilentlyContinue
if (-Not $adbPath) {
    Write-Host "Error: ADB (Android Debug Bridge) not found in PATH." -ForegroundColor Red
    Write-Host "Please install Android SDK Platform Tools or add it to your PATH." -ForegroundColor Yellow
    Write-Host "Download from: https://developer.android.com/studio/releases/platform-tools" -ForegroundColor Yellow
    exit 1
}

# Check if device is connected
Write-Host "Checking for connected devices..." -ForegroundColor Cyan
$devices = & adb devices
if ($devices -match "device$") {
    Write-Host "Device found!" -ForegroundColor Green
} else {
    Write-Host "Error: No Android device found." -ForegroundColor Red
    Write-Host "Please:" -ForegroundColor Yellow
    Write-Host "  1. Connect your device via USB" -ForegroundColor Yellow
    Write-Host "  2. Enable USB Debugging in Developer Options" -ForegroundColor Yellow
    Write-Host "  3. Allow USB debugging when prompted on your device" -ForegroundColor Yellow
    exit 1
}

# Find the APK
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
if (-Not (Test-Path $apkPath)) {
    Write-Host "Error: APK not found at $apkPath" -ForegroundColor Red
    Write-Host "Please build the APK first using: .\build-apk.ps1" -ForegroundColor Yellow
    exit 1
}

# Install the APK
Write-Host "Installing APK on device..." -ForegroundColor Cyan
& adb install -r $apkPath

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "Installation successful!" -ForegroundColor Green
    Write-Host "You can now launch the app from your device." -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "Installation failed. Please check the error messages above." -ForegroundColor Red
    exit 1
}
