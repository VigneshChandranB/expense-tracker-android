# 🔧 Fix Gradle Wrapper JAR Error

## ❌ Current Error:
```
Error: Invalid or corrupt jarfile /home/runner/work/.../gradle/wrapper/gradle-wrapper.jar
```

## 🎯 Root Cause:
The `gradle-wrapper.jar` file is corrupted, missing, or wasn't uploaded properly to GitHub.

## ✅ Complete Fix - Clean Workflow

**Replace your entire `.github/workflows/build-apk.yml` with this clean version:**

```yaml
name: Build Android APK

on:
  workflow_dispatch:  # Manual trigger only for now

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
      
    - name: Download and Setup Gradle
      run: |
        # Download Gradle 8.4
        wget -q https://services.gradle.org/distributions/gradle-8.4-bin.zip
        unzip -q gradle-8.4-bin.zip
        export GRADLE_HOME=$PWD/gradle-8.4
        export PATH=$GRADLE_HOME/bin:$PATH
        echo "$PWD/gradle-8.4/bin" >> $GITHUB_PATH
        
        # Verify Gradle
        gradle --version
        
    - name: Initialize Gradle Wrapper
      run: |
        # Remove any existing gradle wrapper files
        rm -rf gradle/wrapper
        rm -f gradlew gradlew.bat
        
        # Initialize new gradle wrapper
        gradle wrapper --gradle-version 8.4
        
        # Make gradlew executable
        chmod +x gradlew
        
        # Test the wrapper
        ./gradlew --version
        
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
        # Build with detailed logging
        ./gradlew assembleRelease --stacktrace --info
        
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

## 🔄 What This Fix Does:

1. **Downloads fresh Gradle** (8.4) from official source
2. **Removes corrupted wrapper files** completely
3. **Creates new gradle wrapper** from scratch
4. **Uses JDK 17** (more compatible with modern Android builds)
5. **Adds detailed logging** for better error tracking

## 📋 Steps to Apply Fix:

1. **Go to your GitHub repository**
2. **Edit `.github/workflows/build-apk.yml`**
3. **Replace ENTIRE content** with the code above
4. **Commit changes**
5. **Go to Actions tab**
6. **Run "Build Android APK" workflow manually**

## ⏱️ Expected Results:

- ✅ **Fresh Gradle setup**: No more JAR corruption
- ✅ **Clean wrapper**: New gradle-wrapper.jar created
- ✅ **Successful build**: APK generated in ~10-12 minutes
- ✅ **Download ready**: APK available in artifacts

## 🆘 If This Still Fails:

The issue might be with your project files. Check that these exist in your repository:
- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts`
- `app/src/main/AndroidManifest.xml`

## 🎯 Why This Works:

- **No dependency** on existing corrupted files
- **Fresh download** of all build tools
- **Clean initialization** of gradle wrapper
- **Modern versions** (JDK 17, Gradle 8.4)
- **Better error handling** with stacktrace

---

**This should completely resolve the gradle-wrapper.jar corruption error!**