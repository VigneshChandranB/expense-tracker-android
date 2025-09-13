# Complete APK Generation Guide

## Current Status: ❌ APK NOT GENERATED YET

The build configuration is ready, but you need to generate the actual APK file.

## Step-by-Step Generation Process

### Step 1: Install Required Tools

#### Option A: Using Android Studio (Recommended)
1. **Download Android Studio**: https://developer.android.com/studio
2. **Install Android Studio** with default settings
3. **Open Project**: File → Open → Select this project folder
4. **Wait for Gradle Sync** to complete

#### Option B: Command Line Only
1. **Install Java JDK 11**: https://adoptium.net/
2. **Download Android SDK Command Line Tools**: https://developer.android.com/studio#command-tools
3. **Set Environment Variables**:
   ```
   JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-11.0.x
   ANDROID_HOME=C:\Android\Sdk
   ```

### Step 2: Create Release Keystore

#### Using Command Line (keytool):
```bash
# Navigate to project directory
cd "D:\Expences Tracker"

# Create keystore directory if it doesn't exist
mkdir keystore

# Generate release keystore
keytool -genkey -v -keystore keystore\release.keystore -alias expense_tracker_key -keyalg RSA -keysize 2048 -validity 10000
```

**When prompted, enter:**
- **Keystore password**: Choose a strong password (remember this!)
- **Key password**: Same as keystore password (or different)
- **First and last name**: Your name or app name
- **Organizational unit**: Your organization
- **Organization**: Your company/personal
- **City**: Your city
- **State**: Your state
- **Country code**: Your country (e.g., IN for India)

#### Using Android Studio:
1. **Build → Generate Signed Bundle/APK**
2. **Choose APK**
3. **Create New Keystore**
4. **Fill in keystore details**
5. **Save keystore file in `keystore/` folder**

### Step 3: Configure Signing Credentials

Edit `gradle.properties` file and add your keystore details:

```properties
# Signing configuration for release builds
RELEASE_STORE_FILE=keystore/release.keystore
RELEASE_STORE_PASSWORD=your_actual_password
RELEASE_KEY_ALIAS=expense_tracker_key
RELEASE_KEY_PASSWORD=your_actual_key_password
```

**⚠️ SECURITY NOTE**: Never commit these passwords to version control!

### Step 4: Generate APK

#### Method A: Using Build Script (Easiest)
```bash
# Run the automated build script
scripts\build-release.bat
```

#### Method B: Using Gradle Commands
```bash
# Clean previous builds
.\gradlew clean

# Generate release APK
.\gradlew assembleRelease

# Generate AAB (for Play Store)
.\gradlew bundleRelease
```

#### Method C: Using Android Studio
1. **Build → Generate Signed Bundle/APK**
2. **Choose APK**
3. **Select existing keystore** (created in Step 2)
4. **Enter keystore passwords**
5. **Choose release build variant**
6. **Click Finish**

### Step 5: Verify Generated APK

#### Check APK Location:
```
app\build\outputs\apk\release\app-release.apk
```

#### Verify APK using script:
```bash
scripts\verify-apk.bat
```

#### Manual verification:
```bash
# Check APK info
aapt dump badging app\build\outputs\apk\release\app-release.apk

# Check APK size
dir app\build\outputs\apk\release\app-release.apk
```

## Expected Output Files

After successful build, you should have:

### APK Files:
- `app\build\outputs\apk\release\app-release.apk` (~25MB)
- `app\build\outputs\apk\release\output-metadata.json`

### AAB Files (if generated):
- `app\build\outputs\bundle\release\app-release.aab` (~20MB)

### Mapping Files:
- `app\build\outputs\mapping\release\mapping.txt` (for crash analysis)

## Troubleshooting Common Issues

### Issue 1: Gradle Wrapper Not Found
```
Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain
```
**Solution**: Download and extract Gradle wrapper:
```bash
# Download gradle wrapper
curl -o gradle-wrapper.jar https://services.gradle.org/distributions/gradle-8.4-bin.zip
```

### Issue 2: Keystore Not Found
```
Error: Keystore file not found
```
**Solution**: 
1. Ensure keystore file exists in `keystore/release.keystore`
2. Check `gradle.properties` has correct path
3. Use absolute path if needed

### Issue 3: Build Tools Not Found
```
Error: Android SDK Build-tools not found
```
**Solution**:
1. Install Android SDK Build-tools via Android Studio
2. Or download command line tools and install build-tools

### Issue 4: Out of Memory
```
Error: OutOfMemoryError
```
**Solution**: Increase heap size in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
```

## Testing the Generated APK

### Install on Device:
1. **Enable Developer Options** on your Android device
2. **Enable USB Debugging**
3. **Allow installation from unknown sources**
4. **Copy APK to device** and install

### Test Installation:
```bash
# Using ADB (if device connected)
adb install app\build\outputs\apk\release\app-release.apk
```

## APK Information

### Expected APK Details:
- **Package Name**: com.expensetracker
- **Version Code**: 1
- **Version Name**: 1.0.0
- **Min SDK**: 30 (Android 11)
- **Target SDK**: 34 (Android 14)
- **Size**: ~25MB (optimized)
- **Architecture**: Universal (ARM64, ARM32)

### Security Features:
- **Signed**: Production certificate
- **Obfuscated**: ProGuard enabled
- **Encrypted**: Database encryption
- **Optimized**: Resource shrinking enabled

## Next Steps After APK Generation

1. **Test APK** on multiple devices
2. **Run final testing checklist** (docs/FINAL_TESTING_CHECKLIST.md)
3. **Complete deployment checklist** (docs/DEPLOYMENT_CHECKLIST.md)
4. **Distribute APK** to users or upload to Play Store

## Support

If you encounter issues:
1. Check this guide first
2. Review error messages carefully
3. Ensure all prerequisites are installed
4. Check Android Studio's Build Output for detailed errors

---

**Status**: Ready for APK generation
**Next Action**: Follow Step 1 to install required tools