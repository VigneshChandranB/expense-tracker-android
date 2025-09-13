@echo off
REM Portable Android Development Setup - No Admin Required

echo ========================================
echo Portable Android SDK Setup
echo No Administrator Rights Required
echo ========================================
echo.

set TOOLS_DIR=D:\PortableTools
set PROJECT_DIR=%CD%

echo Creating portable tools directory...
if not exist "%TOOLS_DIR%" mkdir "%TOOLS_DIR%"

echo.
echo ========================================
echo Step 1: Download Required Tools
echo ========================================
echo.

echo Please download these files manually (no admin required):
echo.
echo 1. Portable JDK 11:
echo    URL: https://github.com/adoptium/temurin11-binaries/releases
echo    File: OpenJDK11U-jdk_x64_windows_hotspot_11.0.21_9.zip
echo    Extract to: %TOOLS_DIR%\jdk-11
echo.
echo 2. Android Command Line Tools:
echo    URL: https://developer.android.com/studio#command-tools
echo    File: commandlinetools-win-9477386_latest.zip  
echo    Extract to: %TOOLS_DIR%\android-sdk\cmdline-tools\latest
echo.

pause

echo.
echo ========================================
echo Step 2: Verify Downloads
echo ========================================
echo.

if exist "%TOOLS_DIR%\jdk-11\bin\java.exe" (
    echo ✅ JDK 11 found
) else (
    echo ❌ JDK 11 not found at %TOOLS_DIR%\jdk-11
    echo Please extract JDK zip file to the correct location
    pause
    exit /b 1
)

if exist "%TOOLS_DIR%\android-sdk\cmdline-tools\latest\bin\sdkmanager.bat" (
    echo ✅ Android SDK Command Line Tools found
) else (
    echo ❌ Android SDK tools not found
    echo Please extract to: %TOOLS_DIR%\android-sdk\cmdline-tools\latest
    pause
    exit /b 1
)

echo.
echo ========================================
echo Step 3: Setup Environment Variables
echo ========================================
echo.

set JAVA_HOME=%TOOLS_DIR%\jdk-11
set ANDROID_HOME=%TOOLS_DIR%\android-sdk
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\cmdline-tools\latest\bin;%ANDROID_HOME%\platform-tools;%PATH%

echo JAVA_HOME=%JAVA_HOME%
echo ANDROID_HOME=%ANDROID_HOME%

echo.
echo Testing Java installation...
"%JAVA_HOME%\bin\java.exe" -version
if %ERRORLEVEL% neq 0 (
    echo ❌ Java test failed
    pause
    exit /b 1
)

echo ✅ Java working correctly

echo.
echo ========================================
echo Step 4: Install Android SDK Components
echo ========================================
echo.

echo Installing required Android SDK components...
echo This may take a few minutes...

REM Accept licenses first
echo y | "%ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager.bat" --licenses

REM Install required components
"%ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager.bat" "platform-tools" "platforms;android-30" "platforms;android-34" "build-tools;34.0.0"

if %ERRORLEVEL% neq 0 (
    echo ❌ SDK installation failed
    pause
    exit /b 1
)

echo ✅ Android SDK components installed

echo.
echo ========================================
echo Step 5: Create Environment Script
echo ========================================
echo.

echo Creating portable environment script...

echo @echo off > "%PROJECT_DIR%\setup-portable-env.bat"
echo REM Portable Android Development Environment >> "%PROJECT_DIR%\setup-portable-env.bat"
echo set JAVA_HOME=%JAVA_HOME% >> "%PROJECT_DIR%\setup-portable-env.bat"
echo set ANDROID_HOME=%ANDROID_HOME% >> "%PROJECT_DIR%\setup-portable-env.bat"
echo set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\cmdline-tools\latest\bin;%ANDROID_HOME%\platform-tools;%%PATH%% >> "%PROJECT_DIR%\setup-portable-env.bat"
echo echo ✅ Portable Android environment loaded >> "%PROJECT_DIR%\setup-portable-env.bat"
echo echo Java: %%JAVA_HOME%% >> "%PROJECT_DIR%\setup-portable-env.bat"
echo echo Android SDK: %%ANDROID_HOME%% >> "%PROJECT_DIR%\setup-portable-env.bat"

echo ✅ Environment script created: setup-portable-env.bat

echo.
echo ========================================
echo Step 6: Test Gradle Build
echo ========================================
echo.

echo Testing Gradle wrapper...
if exist "gradlew.bat" (
    echo ✅ Gradle wrapper found
    
    echo Testing Gradle build...
    call gradlew.bat --version
    
    if %ERRORLEVEL% equ 0 (
        echo ✅ Gradle working correctly
    ) else (
        echo ❌ Gradle test failed
    )
) else (
    echo ❌ Gradle wrapper not found
    echo Make sure you're in the correct project directory
)

echo.
echo ========================================
echo Setup Complete!
echo ========================================
echo.

echo ✅ Portable Android development environment ready!
echo.
echo Next steps:
echo 1. Run: setup-portable-env.bat (loads environment)
echo 2. Create keystore: keytool -genkey -v -keystore keystore\release.keystore -alias expense_tracker_key -keyalg RSA -keysize 2048 -validity 10000
echo 3. Update gradle.properties with keystore details
echo 4. Build APK: gradlew.bat assembleRelease
echo.
echo APK will be created at: app\build\outputs\apk\release\app-release.apk
echo.

pause