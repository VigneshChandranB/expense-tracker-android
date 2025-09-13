# 🚀 Build APK with Codemagic - Step by Step

## ⏱️ Total Time: 15 minutes to APK download

### Step 1: Sign Up for Codemagic (2 minutes)
1. **Go to**: https://codemagic.io
2. **Click "Sign up for free"**
3. **Choose "Continue with GitHub"**
4. **Authorize Codemagic** to access your repositories
5. **Complete profile setup** (name, email verification)

### Step 2: Add Your Repository (1 minute)
1. **Click "Add application"** on dashboard
2. **Select "GitHub"** as source
3. **Find your repository**: `expense-tracker-android` (or whatever you named it)
4. **Click "Select"** to add the repository

### Step 3: Configure Build Settings (3 minutes)
1. **Project type**: Select "Android"
2. **Build configuration**: Choose "Custom workflow"
3. **Branch**: Select "main" or "master" (whichever you used)

### Step 4: Create Build Configuration (5 minutes)

#### Option A: Use Codemagic UI
1. **Build triggers**: Set to "Manual" (for now)
2. **Environment variables**: Add these:
   ```
   RELEASE_STORE_PASSWORD=codemagic_build
   RELEASE_KEY_PASSWORD=codemagic_build
   ```
3. **Build script**: Replace default with:
   ```bash
   # Create keystore
   mkdir -p keystore
   keytool -genkey -v -keystore keystore/release.keystore \
     -alias expense_tracker_key -keyalg RSA -keysize 2048 -validity 10000 \
     -dname "CN=ExpenseTracker, OU=Development, O=ExpenseTracker, L=City, S=State, C=US" \
     -storepass codemagic_build -keypass codemagic_build
   
   # Configure signing
   echo "RELEASE_STORE_FILE=keystore/release.keystore" >> gradle.properties
   echo "RELEASE_STORE_PASSWORD=codemagic_build" >> gradle.properties
   echo "RELEASE_KEY_ALIAS=expense_tracker_key" >> gradle.properties
   echo "RELEASE_KEY_PASSWORD=codemagic_build" >> gradle.properties
   
   # Build APK
   ./gradlew assembleRelease
   ```

#### Option B: Use YAML Configuration (Advanced)
Create `codemagic.yaml` in your repository root:
```yaml
workflows:
  android-workflow:
    name: Android Workflow
    max_build_duration: 60
    environment:
      android_signing:
        - keystore_reference
      groups:
        - google_play
      java: 17
    scripts:
      - name: Set up local.properties
        script: |
          echo "sdk.dir=$ANDROID_SDK_ROOT" > "$CM_BUILD_DIR/local.properties"
      - name: Create keystore
        script: |
          mkdir -p keystore
          keytool -genkey -v -keystore keystore/release.keystore \
            -alias expense_tracker_key -keyalg RSA -keysize 2048 -validity 10000 \
            -dname "CN=ExpenseTracker, OU=Development, O=ExpenseTracker, L=City, S=State, C=US" \
            -storepass codemagic_build -keypass codemagic_build
      - name: Configure signing
        script: |
          echo "RELEASE_STORE_FILE=keystore/release.keystore" >> gradle.properties
          echo "RELEASE_STORE_PASSWORD=codemagic_build" >> gradle.properties
          echo "RELEASE_KEY_ALIAS=expense_tracker_key" >> gradle.properties
          echo "RELEASE_KEY_PASSWORD=codemagic_build" >> gradle.properties
      - name: Build Android release
        script: |
          ./gradlew assembleRelease
    artifacts:
      - app/build/outputs/**/*.apk
      - app/build/outputs/**/*.aab
    publishing:
      email:
        recipients:
          - your-email@example.com
        notify:
          success: true
          failure: false
```

### Step 5: Start Build (1 minute)
1. **Click "Start new build"**
2. **Select branch**: main/master
3. **Click "Start build"**
4. **Monitor progress** in real-time

### Step 6: Download APK (3 minutes)
1. **Wait for build completion** (green checkmark)
2. **Go to "Artifacts" section**
3. **Download APK file**: `app-release.apk`
4. **APK is ready** for installation!

## 🎯 Expected Build Process

### Build Steps You'll See:
1. ✅ **Repository checkout** (30 seconds)
2. ✅ **Environment setup** (2 minutes)
3. ✅ **Dependency download** (3 minutes)
4. ✅ **Keystore creation** (30 seconds)
5. ✅ **APK compilation** (5-8 minutes)
6. ✅ **Artifact upload** (1 minute)

### Total Build Time: 12-15 minutes

## 📱 Install APK on Your Device

### Transfer Methods:
- **Email**: Email APK to yourself
- **Cloud Storage**: Upload to Google Drive/Dropbox
- **USB**: Connect phone and copy APK
- **Direct Download**: Use phone browser to download from Codemagic

### Installation:
1. **Enable Unknown Sources** in Android settings
2. **Tap APK file** to install
3. **Grant SMS permission** for automatic transaction detection
4. **Start using** the expense tracker!

## 🆘 Troubleshooting

### Build Fails?
1. **Check build logs** in Codemagic dashboard
2. **Common issues**:
   - Missing `gradlew` file → Codemagic handles this automatically
   - Dependency issues → Usually auto-resolved
   - Signing issues → Check keystore creation step

### Can't Download APK?
1. **Check artifacts section** after successful build
2. **Try different browser** if download fails
3. **Use direct link** provided in build results

### APK Won't Install?
1. **Check Android version** (need Android 11+)
2. **Enable Unknown Sources** in security settings
3. **Clear storage space** (need 50MB+)

## ✅ Success Checklist

- [ ] Codemagic account created and GitHub connected
- [ ] Repository added to Codemagic
- [ ] Build configuration set up (UI or YAML)
- [ ] Build started and completed successfully
- [ ] APK downloaded from artifacts
- [ ] APK installed on Android 11 device
- [ ] App launches and SMS permission granted
- [ ] Expense tracking features working

## 🎉 Expected Results

### APK Details:
- **File**: `app-release.apk`
- **Size**: ~25MB
- **Version**: 1.0.0
- **Compatible**: Android 11+ ✅
- **Your Device**: RKQ1.200826.002 ✅ Perfect match!

### Features Working:
- ✅ SMS transaction detection
- ✅ Multi-bank support (HDFC, ICICI, SBI, etc.)
- ✅ Manual transaction entry
- ✅ Categories and analytics
- ✅ Export to CSV/PDF
- ✅ Secure encrypted storage

---

**Codemagic is much more reliable than GitHub Actions for Android builds. You should have your working APK within 15 minutes!**