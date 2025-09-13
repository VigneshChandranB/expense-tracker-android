# Quick Start: Generate APK in 15 Minutes

## Fastest Method: Android Studio

### Step 1: Install Android Studio (5 minutes)
1. Download: https://developer.android.com/studio
2. Run installer with default settings
3. Let it download SDK components

### Step 2: Open Project (2 minutes)
1. Launch Android Studio
2. Choose "Open an Existing Project"
3. Select this folder: `D:\Expences Tracker`
4. Wait for Gradle sync to complete

### Step 3: Generate Signed APK (5 minutes)
1. **Build → Generate Signed Bundle/APK**
2. **Select APK**
3. **Create New Keystore**:
   - Key store path: `D:\Expences Tracker\keystore\release.keystore`
   - Password: Choose strong password
   - Key alias: `expense_tracker_key`
   - Key password: Same as keystore password
   - Validity: 25 years
   - Certificate info: Fill your details
4. **Select Release build variant**
5. **Click Finish**

### Step 4: Find Your APK (1 minute)
Location: `app\build\outputs\apk\release\app-release.apk`

## Alternative: Use Existing Android Studio

If you already have Android Studio installed:

1. **Update to latest version** (Help → Check for Updates)
2. **Open this project**
3. **Follow Step 3 above**

## Test Your APK

### On Physical Device:
1. Enable Developer Options
2. Enable USB Debugging  
3. Allow Unknown Sources
4. Install APK: `adb install app-release.apk`

### On Emulator:
1. Create Android 11+ emulator in Android Studio
2. Drag APK to emulator window
3. Test all features

## Expected APK Details

- **File**: `app-release.apk`
- **Size**: ~25MB
- **Compatible**: Android 11+ devices
- **Features**: All expense tracking functionality
- **Security**: Signed and obfuscated

## Troubleshooting

### Build Fails?
- Check Android Studio's Build Output
- Ensure internet connection for dependencies
- Try Build → Clean Project, then rebuild

### Keystore Issues?
- Use absolute paths
- Ensure passwords match
- Don't use special characters in passwords

### APK Won't Install?
- Check device has Android 11+
- Enable Unknown Sources
- Ensure sufficient storage space

---

**Total Time**: ~15 minutes with good internet
**Result**: Production-ready APK for your Android 11 device