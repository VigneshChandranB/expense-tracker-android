# 🚀 Build APK with GitHub Actions - Step by Step

## ⏱️ Total Time: 10 minutes to APK download

### Step 1: Create GitHub Account (2 minutes)
1. **Go to**: https://github.com
2. **Click "Sign up"**
3. **Choose username**: (e.g., `yourname-expense-tracker`)
4. **Enter email and password**
5. **Verify email** (check your inbox)

### Step 2: Create New Repository (1 minute)
1. **Click green "New" button** (or go to https://github.com/new)
2. **Repository name**: `expense-tracker-android`
3. **Description**: `Android Expense Tracker with SMS Integration`
4. **Set to Public** (required for free GitHub Actions)
5. **Check "Add a README file"**
6. **Click "Create repository"**

### Step 3: Upload Project Files (3 minutes)

#### Method A: Drag & Drop (Easiest)
1. **Click "uploading an existing file"** link on your new repo page
2. **Open Windows Explorer** to your project folder: `D:\Expences Tracker`
3. **Select ALL files and folders** (Ctrl+A)
4. **Drag and drop** into GitHub upload area
5. **Wait for upload** to complete
6. **Scroll down** and click "Commit changes"

#### Method B: Individual Upload
1. **Click "Add file" → "Upload files"**
2. **Upload these key files/folders**:
   - `app/` folder (entire folder)
   - `gradle/` folder
   - `build.gradle.kts`
   - `gradle.properties`
   - `gradlew` and `gradlew.bat`
   - `settings.gradle.kts`
   - `.github/` folder (contains the workflow)
3. **Commit changes**

### Step 4: Verify Workflow File (30 seconds)
1. **Check that `.github/workflows/build-apk.yml` exists** in your repo
2. **If missing**: Create it manually
   - Click "Add file" → "Create new file"
   - Name: `.github/workflows/build-apk.yml`
   - Copy content from the workflow file I created
   - Commit the file

### Step 5: Trigger the Build (1 minute)
1. **Go to "Actions" tab** in your repository
2. **You should see "Build Android APK" workflow**
3. **Click on the workflow name**
4. **Click "Run workflow" button** (top right)
5. **Click green "Run workflow" button** in the dropdown
6. **Build starts automatically!**

### Step 6: Monitor Build Progress (2-5 minutes)
1. **Click on the running workflow** (yellow circle icon)
2. **Click on "build" job** to see detailed progress
3. **Watch the steps execute**:
   - ✅ Checkout code
   - ✅ Set up JDK 11
   - ✅ Setup Android SDK
   - ✅ Create keystore
   - ✅ Build Release APK
   - ✅ Upload APK

### Step 7: Download Your APK (1 minute)
1. **Wait for green checkmark** (build successful)
2. **Scroll down to "Artifacts" section**
3. **Click "expense-tracker-apk"** to download
4. **Extract the ZIP file** on your computer
5. **You now have `app-release.apk`!**

## 📱 Install on Your Android 11 Device

### Transfer APK to Phone:
- **USB Cable**: Copy APK to phone storage
- **Email**: Email APK to yourself
- **Cloud Storage**: Upload to Google Drive/Dropbox
- **Bluetooth**: Send via Bluetooth

### Install APK:
1. **Enable Unknown Sources**:
   - Settings → Security → Unknown Sources (Android 11)
   - Or Settings → Apps → Special app access → Install unknown apps
2. **Tap the APK file** in your file manager
3. **Click "Install"**
4. **Wait for installation** to complete
5. **Open "Expense Tracker"** from app drawer

### First Launch Setup:
1. **Grant SMS permission** (recommended for auto-detection)
2. **Add your bank accounts** (HDFC, ICICI, SBI, etc.)
3. **Start tracking expenses!**

## 🎯 Expected Results

### APK Details:
- **File**: `app-release.apk`
- **Size**: ~25MB
- **Version**: 1.0.0
- **Compatible**: Android 11+ ✅
- **Your Device**: RKQ1.200826.002 ✅ Perfect match!

### Features Working:
- ✅ SMS transaction detection
- ✅ Multi-bank support
- ✅ Manual transaction entry
- ✅ Categories and analytics
- ✅ Export to CSV/PDF
- ✅ Secure encrypted storage

## 🔄 Future Updates

### To Build New APK:
1. **Make code changes** (if needed)
2. **Upload changed files** to GitHub
3. **APK builds automatically** on every push
4. **Download new APK** from Actions tab

### Automatic Builds:
- **Every code push** triggers new build
- **Pull requests** also trigger builds
- **Manual trigger** anytime via Actions tab

## 🆘 Troubleshooting

### Build Fails?
1. **Check Actions tab** for error details
2. **Common issues**:
   - Missing `gradlew` file
   - Incorrect folder structure
   - Missing workflow file

### Upload Issues?
1. **File too large**: Upload in smaller batches
2. **Slow internet**: Try uploading key files first
3. **Browser issues**: Try different browser

### APK Won't Install?
1. **Check Android version**: Need Android 11+
2. **Enable Unknown Sources**: Check security settings
3. **Storage space**: Ensure 50MB+ free space

## ✅ Success Checklist

- [ ] GitHub account created
- [ ] Repository created and set to public
- [ ] All project files uploaded
- [ ] Workflow file exists in `.github/workflows/`
- [ ] Build triggered and completed successfully
- [ ] APK downloaded and extracted
- [ ] APK installed on Android 11 device
- [ ] App launches and works correctly

---

**🎉 Congratulations!** You've successfully built your Android APK using GitHub Actions without needing admin rights or installing any software!

**Next**: Install the APK on your Android 11 device and start tracking your expenses with automatic SMS detection from your bank accounts.