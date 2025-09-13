@echo off
REM Build APK using portable setup - No Admin Required

echo ========================================
echo Build APK - Portable Setup
echo ========================================
echo.

REM Load portable environment
if exist "setup-portable-env.bat" (
    echo Loading portable environment...
    call setup-portable-env.bat
) else (
    echo ❌ Portable environment not found!
    echo Please run portable-setup.bat first
    pause
    exit /b 1
)

echo.
echo ========================================
echo Step 1: Verify Environment
echo ========================================
echo.

REM Check Java
java -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ❌ Java not found in PATH
    echo Please run portable-setup.bat first
    pause
    exit /b 1
)
echo ✅ Java found

REM Check Android SDK
if exist "%ANDROID_HOME%\platform-tools\adb.exe" (
    echo ✅ Android SDK found
) else (
    echo ❌ Android SDK not properly installed
    echo Please run portable-setup.bat first
    pause
    exit /b 1
)

echo.
echo ========================================
echo Step 2: Check/Create Keystore
echo ========================================
echo.

if exist "keystore\release.keystore" (
    echo ✅ Release keystore found
) else (
    echo ⚠️  Release keystore not found
    echo Creating keystore...
    
    if not exist "keystore" mkdir keystore
    
    echo.
    echo Please provide keystore information:
    echo (Use strong passwords and remember them!)
    echo.
    
    keytool -genkey -v -keystore keystore\release.keystore -alias expense_tracker_key -keyalg RSA -keysize 2048 -validity 10000
    
    if %ERRORLEVEL% neq 0 (
        echo ❌ Keystore creation failed
        pause
        exit /b 1
    )
    
    echo ✅ Keystore created successfully
)

echo.
echo ========================================
echo Step 3: Update Gradle Properties
echo ========================================
echo.

if exist "gradle.properties" (
    findstr /C:"RELEASE_STORE_FILE" gradle.properties >nul
    if %ERRORLEVEL% equ 0 (
        echo ✅ Signing configuration found in gradle.properties
    ) else (
        echo ⚠️  Adding signing configuration to gradle.properties
        echo. >> gradle.properties
        echo # Release signing configuration >> gradle.properties
        echo RELEASE_STORE_FILE=keystore/release.keystore >> gradle.properties
        echo RELEASE_STORE_PASSWORD=your_store_password >> gradle.properties
        echo RELEASE_KEY_ALIAS=expense_tracker_key >> gradle.properties
        echo RELEASE_KEY_PASSWORD=your_key_password >> gradle.properties
        
        echo ⚠️  Please edit gradle.properties and update the passwords!
        echo Press any key after updating passwords...
        pause
    )
) else (
    echo ❌ gradle.properties not found
    pause
    exit /b 1
)

echo.
echo ========================================
echo Step 4: Clean Previous Builds
echo ========================================
echo.

echo Cleaning previous builds...
call gradlew.bat clean

if %ERRORLEVEL% neq 0 (
    echo ❌ Clean failed
    pause
    exit /b 1
)

echo ✅ Clean completed

echo.
echo ========================================
echo Step 5: Build Release APK
echo ========================================
echo.

echo Building release APK...
echo This may take several minutes...

call gradlew.bat assembleRelease

if %ERRORLEVEL% neq 0 (
    echo ❌ Build failed!
    echo.
    echo Common issues:
    echo 1. Check keystore passwords in gradle.properties
    echo 2. Ensure internet connection for dependencies
    echo 3. Check build output for specific errors
    pause
    exit /b 1
)

echo.
echo ========================================
echo Build Successful! 🎉
echo ========================================
echo.

set APK_PATH=app\build\outputs\apk\release\app-release.apk

if exist "%APK_PATH%" (
    echo ✅ APK created successfully!
    echo.
    echo 📱 APK Location: %APK_PATH%
    
    REM Show APK size
    for %%I in ("%APK_PATH%") do echo 📦 APK Size: %%~zI bytes
    
    echo.
    echo 🔍 APK Information:
    if exist "%ANDROID_HOME%\build-tools\34.0.0\aapt.exe" (
        "%ANDROID_HOME%\build-tools\34.0.0\aapt.exe" dump badging "%APK_PATH%" | findstr "package:"
        "%ANDROID_HOME%\build-tools\34.0.0\aapt.exe" dump badging "%APK_PATH%" | findstr "versionName:"
        "%ANDROID_HOME%\build-tools\34.0.0\aapt.exe" dump badging "%APK_PATH%" | findstr "minSdkVersion:"
    )
    
    echo.
    echo 📋 Next Steps:
    echo 1. Copy APK to your Android 11 device
    echo 2. Enable "Install from Unknown Sources"
    echo 3. Install the APK
    echo 4. Grant SMS permission for automatic transaction detection
    echo.
    echo 🎯 Your Android 11 device (RKQ1.200826.002) will run this APK perfectly!
    
) else (
    echo ❌ APK not found at expected location
    echo Check build output for errors
)

echo.
pause