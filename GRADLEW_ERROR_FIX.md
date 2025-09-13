# 🔧 Fix Gradlew Permission Error

## ❌ Current Error:
```
chmod: cannot access 'gradlew': No such file or directory
Error: Process completed with exit code 1.
```

## 🎯 Root Cause:
The `gradlew` file is either missing or lost executable permissions when uploaded to GitHub.

## ✅ Quick Fixes:

### Option 1: Check Your Upload (Recommended First)

**Verify these files exist in your GitHub repository:**
- [ ] `gradlew` (Unix executable)
- [ ] `gradlew.bat` (Windows batch file)
- [ ] `gradle/wrapper/gradle-wrapper.jar`
- [ ] `gradle/wrapper/gradle-wrapper.properties`

**If missing, re-upload them:**
1. Go to your local project folder: `D:\Expences Tracker`
2. Find these files and upload them to GitHub
3. Make sure to upload the `gradle/` folder completely

### Option 2: Use Fixed Workflow (No Gradlew Dependency)

**Replace your entire `.github/workflows/build-apk.yml` with:**

```yaml
name: Build Android APK

on:
  push:
    branches: [ main, master ]
  pull_request:
    branches: [ main, master ]
  workflow_dispatch:  # Allow manual trigger

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Setup Android SDK
      run: |
        # Download and setup Android SDK
        wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
        unzip -q commandlinetools-linux-9477386_latest.zip
        mkdir -p $HOME/android-sdk/cmdline-tools
        mv cmdline-tools $HOME/android-sdk/cmdline-tools/latest
        
        # Set environment variables
        echo "ANDROID_HOME=$HOME/android-sdk" >> $GITHUB_ENV
        echo "ANDROID_SDK_ROOT=$HOME/android-sdk" >> $GITHUB_ENV
        echo "$HOME/android-sdk/cmdline-tools/latest/bin" >> $GITHUB_PATH
        echo "$HOME/android-sdk/platform-tools" >> $GITHUB_PATH
        echo "$HOME/android-sdk/build-tools/34.0.0" >> $GITHUB_PATH
        
        # Accept licenses and install required components
        yes | $HOME/android-sdk/cmdline-tools/latest/bin/sdkmanager --licenses || true
        $HOME/android-sdk/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-30" "platforms;android-34" "build-tools;34.0.0"
      
    - name: Setup Gradle
      run: |
        # Install Gradle directly
        wget -q https://services.gradle.org/distributions/gradle-8.4-bin.zip
        unzip -q gradle-8.4-bin.zip
        export PATH=$PATH:$PWD/gradle-8.4/bin
        echo "$PWD/gradle-8.4/bin" >> $GITHUB_PATH
        
        # Verify Gradle installation
        gradle --version
        
    - name: Create keystore
      run: |
        mkdir -p keystore
        keytool -genkey -v -keystore keystore/release.keystore \
          -alias expense_tracker_key -keyalg RSA -keysize 2048 -validity 10000 \
          -dname "CN=ExpenseTracker, OU=Development, O=ExpenseTracker, L=City, S=State, C=US" \
          -storepass github_build -keypass github_build
          
    - name: Create gradle.properties
      run: |
        echo "RELEASE_STORE_FILE=keystore/release.keystore" >> gradle.properties
        echo "RELEASE_STORE_PASSWORD=github_build" >> gradle.properties
        echo "RELEASE_KEY_ALIAS=expense_tracker_key" >> gradle.properties
        echo "RELEASE_KEY_PASSWORD=github_build" >> gradle.properties
        
    - name: Build Release APK
      run: |
        # Use gradle directly instead of gradlew
        gradle assembleRelease --stacktrace
        
    - name: Upload APK
      uses: actions/upload-artifact@v4
      with:
        name: expense-tracker-apk
        path: app/build/outputs/apk/release/app-release.apk
        
    - name: Upload AAB
      uses: actions/upload-artifact@v4
      with:
        name: expense-tracker-aab
        path: app/build/outputs/bundle/release/app-release.aab
      if: success()
      
    - name: APK Info
      run: |
        echo "APK built successfully!"
        ls -la app/build/outputs/apk/release/
        if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
          echo "APK size: $(du -h app/build/outputs/apk/release/app-release.apk | cut -f1)"
        fi
```

### Option 3: Simple Gradlew Fix

**If you want to keep using gradlew, replace the "Grant execute permission" step with:**

```yaml
    - name: Setup Gradle Wrapper
      run: |
        # List files to debug
        echo "Files in repository:"
        ls -la
        
        # Check if gradlew exists
        if [ -f "gradlew" ]; then
          echo "gradlew found, making executable"
          chmod +x gradlew
        else
          echo "gradlew not found, using gradle wrapper from gradle folder"
          # Create gradlew if missing
          echo '#!/bin/bash' > gradlew
          echo 'java -jar gradle/wrapper/gradle-wrapper.jar "$@"' >> gradlew
          chmod +x gradlew
        fi
        
        # Test gradlew
        ./gradlew --version
```

## 🎯 Recommended Solution:

**Use Option 2** - it bypasses the gradlew issue entirely by installing Gradle directly.

## 📋 Steps:

1. **Replace your workflow file** with Option 2 content
2. **Commit changes**
3. **Run workflow again**
4. **Should build successfully** in ~8-10 minutes

## ⚠️ If Still Failing:

Check that these essential files are in your GitHub repository:
- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`

---

**This should resolve the gradlew permission error!**