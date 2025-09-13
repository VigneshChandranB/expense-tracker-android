@echo off
REM Build script for generating production APK

echo Building Expense Tracker Release APK...
echo.

REM Clean previous builds
echo Cleaning previous builds...
call gradlew clean

REM Run tests before building
echo Running tests...
call gradlew test
if %ERRORLEVEL% neq 0 (
    echo Tests failed! Aborting build.
    exit /b 1
)

REM Build release APK
echo Building release APK...
call gradlew assembleRelease
if %ERRORLEVEL% neq 0 (
    echo Build failed!
    exit /b 1
)

REM Build release AAB (Android App Bundle)
echo Building release AAB...
call gradlew bundleRelease
if %ERRORLEVEL% neq 0 (
    echo AAB build failed!
    exit /b 1
)

echo.
echo Build completed successfully!
echo APK location: app\build\outputs\apk\release\
echo AAB location: app\build\outputs\bundle\release\
echo.

REM Show APK info
if exist "app\build\outputs\apk\release\app-release.apk" (
    echo APK file: app-release.apk
    for %%I in ("app\build\outputs\apk\release\app-release.apk") do echo APK size: %%~zI bytes
)

if exist "app\build\outputs\bundle\release\app-release.aab" (
    echo AAB file: app-release.aab
    for %%I in ("app\build\outputs\bundle\release\app-release.aab") do echo AAB size: %%~zI bytes
)

pause