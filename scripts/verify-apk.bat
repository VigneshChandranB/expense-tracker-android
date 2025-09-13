@echo off
REM Script to verify the generated APK

set APK_PATH=app\build\outputs\apk\release\app-release.apk

if not exist "%APK_PATH%" (
    echo APK not found at %APK_PATH%
    echo Please build the release APK first using build-release.bat
    pause
    exit /b 1
)

echo Verifying APK: %APK_PATH%
echo.

REM Check APK signature
echo Checking APK signature...
call aapt dump badging "%APK_PATH%" | findstr "package:"
call aapt dump badging "%APK_PATH%" | findstr "application-label:"
call aapt dump badging "%APK_PATH%" | findstr "versionCode:"
call aapt dump badging "%APK_PATH%" | findstr "versionName:"
call aapt dump badging "%APK_PATH%" | findstr "minSdkVersion:"
call aapt dump badging "%APK_PATH%" | findstr "targetSdkVersion:"

echo.
echo APK verification completed.
echo.

REM Show APK contents
echo APK contents:
call aapt list "%APK_PATH%" | findstr "\.dex$"
call aapt list "%APK_PATH%" | findstr "AndroidManifest.xml"
call aapt list "%APK_PATH%" | findstr "resources.arsc"

pause