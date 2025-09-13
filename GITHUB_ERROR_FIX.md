# 🔧 Fix GitHub Actions Android SDK Error

## ❌ Current Error:
The `android-actions/setup-android@v3` action is failing during SDK setup.

## ✅ Quick Fix Options:

### Option 1: Replace Entire Workflow File (Recommended)

**Replace your `.github/workflows/build-apk.yml` with this content:**

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
        # Create Android SDK directory
        mkdir -p $HOME/android-sdk
        
        # Download Android command line tools
        cd $HOME
        wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
        unzip -q commandlinetools-linux-9477386_latest.zip
        
        # Move to proper location
        mkdir -p $HOME/android-sdk/cmdline-tools
        mv cmdline-tools $HOME/android-sdk/cmdline-tools/latest
        
        # Set up environment
        export ANDROID_HOME=$HOME/android-sdk
        export ANDROID_SDK_ROOT=$HOME/android-sdk
        export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
        
        # Add to GitHub environment
        echo "ANDROID_HOME=$HOME/android-sdk" >> $GITHUB_ENV
        echo "ANDROID_SDK_ROOT=$HOME/android-sdk" >> $GITHUB_ENV
        echo "$HOME/android-sdk/cmdline-tools/latest/bin" >> $GITHUB_PATH
        echo "$HOME/android-sdk/platform-tools" >> $GITHUB_PATH
        echo "$HOME/android-sdk/build-tools/34.0.0" >> $GITHUB_PATH
        
        # Accept licenses
        yes | $HOME/android-sdk/cmdline-tools/latest/bin/sdkmanager --licenses || true
        
        # Install required SDK components
        $HOME/android-sdk/cmdline-tools/latest/bin/sdkmanager "platform-tools"
        $HOME/android-sdk/cmdline-tools/latest/bin/sdkmanager "platforms;android-30"
        $HOME/android-sdk/cmdline-tools/latest/bin/sdkmanager "platforms;android-34"
        $HOME/android-sdk/cmdline-tools/latest/bin/sdkmanager "build-tools;34.0.0"
        
    - name: Cache Gradle packages
      uses: actions/cache@v4
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
          
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
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
      run: ./gradlew assembleRelease --stacktrace
      
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

### Option 2: Alternative Simpler Workflow

If the above still fails, use this minimal version:

```yaml
name: Build Android APK

on:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Setup Android SDK
      uses: android-actions/setup-android@v2
      
    - name: Build APK
      run: |
        # Create keystore
        mkdir -p keystore
        keytool -genkey -v -keystore keystore/release.keystore \
          -alias expense_tracker_key -keyalg RSA -keysize 2048 -validity 10000 \
          -dname "CN=ExpenseTracker, OU=Development, O=ExpenseTracker, L=City, S=State, C=US" \
          -storepass github_build -keypass github_build
          
        # Configure signing
        echo "RELEASE_STORE_FILE=keystore/release.keystore" >> gradle.properties
        echo "RELEASE_STORE_PASSWORD=github_build" >> gradle.properties
        echo "RELEASE_KEY_ALIAS=expense_tracker_key" >> gradle.properties
        echo "RELEASE_KEY_PASSWORD=github_build" >> gradle.properties
        
        # Build APK
        chmod +x gradlew
        ./gradlew assembleRelease
        
    - name: Upload APK
      uses: actions/upload-artifact@v4
      with:
        name: expense-tracker-apk
        path: app/build/outputs/apk/release/app-release.apk
```

## 🔄 What Changed:

1. **JDK Version**: Changed from JDK 11 to JDK 17 (more compatible)
2. **Android SDK Setup**: Manual setup instead of problematic action
3. **Error Handling**: Added `--stacktrace` for better error reporting
4. **Simplified Process**: More reliable step-by-step approach

## 📋 Steps to Fix:

1. **Go to your GitHub repository**
2. **Edit `.github/workflows/build-apk.yml`**
3. **Replace entire content** with Option 1 above
4. **Commit changes**
5. **Run workflow again**

## ⏱️ Expected Results:

- ✅ Android SDK setup should complete successfully
- ✅ APK build should work
- ✅ Download should be available in ~8-10 minutes

---

**This should resolve the Android SDK setup error you're experiencing!**