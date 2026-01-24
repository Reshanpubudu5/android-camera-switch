# Build script for Android Camera APK
# This script builds the debug APK for the Android Camera app

Write-Host "Building Android Camera APK..." -ForegroundColor Green

# Check if gradlew.bat exists
if (-Not (Test-Path "gradlew.bat")) {
    Write-Host "Error: gradlew.bat not found. Please ensure you're in the project root directory." -ForegroundColor Red
    exit 1
}

# Check if local.properties exists
if (-Not (Test-Path "local.properties")) {
    Write-Host "Warning: local.properties not found." -ForegroundColor Yellow
    Write-Host "Please create local.properties with your Android SDK path:" -ForegroundColor Yellow
    Write-Host "sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk" -ForegroundColor Yellow
    Write-Host ""
    $continue = Read-Host "Continue anyway? (y/n)"
    if ($continue -ne "y" -and $continue -ne "Y") {
        exit 1
    }
}

# Build the APK
Write-Host "Running Gradle build..." -ForegroundColor Cyan
& .\gradlew.bat assembleDebug

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "Build successful!" -ForegroundColor Green
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        Write-Host "APK location: $((Resolve-Path $apkPath).Path)" -ForegroundColor Green
        Write-Host ""
        Write-Host "To install on your device:" -ForegroundColor Cyan
        Write-Host "  adb install $apkPath" -ForegroundColor White
    }
} else {
    Write-Host ""
    Write-Host "Build failed. Please check the error messages above." -ForegroundColor Red
    exit 1
}
