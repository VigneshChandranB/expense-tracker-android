# Build Android APK Without Admin Access

## 🚫 Problem: No Admin Rights for Android Studio Installation

## ✅ Solution: Portable Development Setup

### Option 1: Portable Android SDK + Command Line (Recommended)

#### Step 1: Download Portable Tools
1. **Portable JDK 11** (No installation required):
   - Download: https://github.com/adoptium/temurin11-binaries/releases
   - Choose: `OpenJDK11U-jdk_x64_windows_hotspot_11.0.21_9.zip`
   - Extract to: `D:\PortableTools\jdk-11`

2. **Android Command Line Tools** (Portable):
   - Download: https://developer.android.com/studio#command-tools
   - Choose: `commandlinetools-win-9477386_latest.zip`
   - Extract to: `D:\PortableTools\android-sdk\cmdline-tools\latest`

#### Step 2: Setup Environment (No Admin Required)
Create `setup-env.bat`:
```batch
@echo off
set JAVA_HOME=D:\PortableTools\jdk-11
set ANDROID_HOME=D:\PortableTools\android-sdk
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\cmdline-tools\latest\bin;%ANDROID_HOME%\platform-tools;%PATH%

echo Environment setup complete!
echo Java: %JAVA_HOME%
echo Android SDK: %ANDROID_HOME%
```

#### Step 3: Install Required SDK Components
```batch
# Run setup-env.bat first
setup-env.bat

# Accept licenses
sdkmanager --licenses

# Install required components
sdkmanager "platform-tools" "platforms;android-30" "platforms;android-34" "build-tools;34.0.0"
```

#### Step 4: Build APK
```batch
# Navigate to project
cd "D:\Expences Tracker"

# Run environment setup
setup-env.bat

# Create keystore (one time)
keytool -genkey -v -keystore keystore\release.keystore -alias expense_tracker_key -keyalg RSA -keysize 2048 -validity 10000

# Build APK
gradlew.bat assembleRelease
```

### Option 2: Online Build Services (Zero Installation)

#### GitHub Actions (Free)
1. **Push code to GitHub**
2. **Create workflow file**: `.github/workflows/build.yml`
3. **GitHub builds APK automatically**
4. **Download APK from Actions tab**

#### Codemagic (Free tier)
1. **Connect GitHub repository**
2. **Configure build settings**
3. **Download built APK**

### Option 3: VS Code Portable + Extensions

#### Setup Portable VS Code:
1. **Download VS Code Portable**: https://code.visualstudio.com/docs/editor/portable
2. **Extract to**: `D:\PortableTools\VSCode`
3. **Install Extensions** (no admin needed):
   - Android iOS Emulator
   - Gradle for Java
   - Extension Pack for Java

### Option 4: Docker Desktop Alternative (If Docker Available)

#### Using Docker without Admin:
```dockerfile
# Dockerfile for Android build
FROM openjdk:11-jdk

# Install Android SDK
RUN apt-get update && apt-get install -y wget unzip
RUN wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
RUN unzip commandlinetools-linux-9477386_latest.zip -d /opt/android-sdk

# Build APK
COPY . /app
WORKDIR /app
RUN ./gradlew assembleRelease
```

### Option 5: Cloud Development Environment

#### GitHub Codespaces (Free tier):
1. **Open repository in Codespaces**
2. **Pre-configured Android environment**
3. **Build APK in cloud**
4. **Download result**

#### Gitpod (Free tier):
1. **Open project in Gitpod**
2. **Automated Android SDK setup**
3. **Build and download APK**

## 🎯 Recommended Approach: Portable SDK Setup

### Complete Portable Setup Script: