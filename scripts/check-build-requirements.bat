@echo off
REM Check if all build requirements are met

echo ========================================
echo Build Requirements Check
echo ========================================
echo.

set REQUIREMENTS_MET=1

echo [1/6] Checking Java JDK...
java -version >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo ✅ Java JDK found
    java -version 2>&1 | findstr "version"
) else (
    echo ❌ Java JDK not found or not in PATH
    echo    Download from: https://adoptium.net/
    set REQUIREMENTS_MET=0
)
echo.

echo [2/6] Checking Android SDK...
if defined ANDROID_HOME (
    echo ✅ ANDROID_HOME set to: %ANDROID_HOME%
    if exist "%ANDROID_HOME%\platform-tools\adb.exe" (
        echo ✅ Android SDK platform-tools found
    ) else (
        echo ⚠️  Android SDK platform-tools not found
    )
) else (
    echo ❌ ANDROID_HOME not set
    echo    Install Android Studio or SDK command line tools
    set REQUIREMENTS_MET=0
)
echo.

echo [3/6] Checking Gradle Wrapper...
if exist "gradlew.bat" (
    echo ✅ Gradle wrapper found
) else (
    echo ❌ Gradle wrapper missing
    echo    This should be included in the project
    set REQUIREMENTS_MET=0
)
echo.

echo [4/6] Checking Keystore Setup...
if exist "keystore\release.keystore" (
    echo ✅ Release keystore found
) else (
    echo ⚠️  Release keystore not found
    echo    You need to create this before building release APK
    echo    See: keystore\README.md for instructions
)

if exist "gradle.properties" (
    findstr /C:"RELEASE_STORE_FILE" gradle.properties >nul
    if %ERRORLEVEL% equ 0 (
        echo ✅ Signing configuration template found
    ) else (
        echo ⚠️  Signing configuration needs setup
    )
) else (
    echo ❌ gradle.properties missing
    set REQUIREMENTS_MET=0
)
echo.

echo [5/6] Checking Build Configuration...
if exist "app\build.gradle.kts" (
    echo ✅ App build configuration found
    findstr /C:"minSdk = 30" app\build.gradle.kts >nul
    if %ERRORLEVEL% equ 0 (
        echo ✅ Android 11+ target confirmed
    )
) else (
    echo ❌ App build configuration missing
    set REQUIREMENTS_MET=0
)
echo.

echo [6/6] Checking Project Structure...
if exist "app\src\main\java\com\expensetracker" (
    echo ✅ Source code structure found
) else (
    echo ❌ Source code missing
    set REQUIREMENTS_MET=0
)

if exist "app\src\main\AndroidManifest.xml" (
    echo ✅ Android manifest found
) else (
    echo ❌ Android manifest missing
    set REQUIREMENTS_MET=0
)
echo.

echo ========================================
echo Requirements Summary
echo ========================================
echo.

if %REQUIREMENTS_MET% equ 1 (
    echo ✅ ALL CORE REQUIREMENTS MET!
    echo.
    echo You can proceed with APK generation:
    echo 1. Create release keystore if not done: keytool -genkey -v -keystore keystore\release.keystore -alias expense_tracker_key -keyalg RSA -keysize 2048 -validity 10000
    echo 2. Update gradle.properties with keystore credentials
    echo 3. Run: scripts\build-release.bat
) else (
    echo ❌ SOME REQUIREMENTS MISSING!
    echo.
    echo Please install missing components:
    echo - Java JDK 11+: https://adoptium.net/
    echo - Android Studio: https://developer.android.com/studio
    echo - Or Android SDK Command Line Tools
    echo.
    echo Then run this script again to verify.
)

echo.
echo Detailed setup guide: GENERATE_APK_GUIDE.md
echo.

pause