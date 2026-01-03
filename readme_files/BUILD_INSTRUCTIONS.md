# Building the Android APK

## Prerequisites

1. **Android Studio** - Download from https://developer.android.com/studio
2. **JDK 17+** - Android Studio includes this automatically
3. **Android SDK** - Installed via Android Studio SDK Manager (usually done automatically)
4. **Python 3.12** - Required for building (Chaquopy uses it to compile Python bytecode)
   - On macOS with Homebrew: `brew install python@3.12`
   - Update `buildPython` path in `app/build.gradle.kts` if Python 3.12 is in a different location

## Building the APK in Android Studio

### Step 1: Open the Project

1. **If you haven't opened the project yet:**
   - Open Android Studio
   - Click **"Open"** or **"Open an Existing Project"**
   - Navigate to and select the `tiktok_downloader_android_app` folder
   - Click **"OK"**

2. **If you've already opened the project:**
   - The project should be visible in Android Studio
   - Wait for Gradle sync to complete (you'll see "Gradle sync" in the status bar at the bottom)

### Step 2: Wait for Gradle Sync

- Android Studio will automatically start syncing Gradle files
- You'll see a progress indicator in the bottom status bar
- Wait until you see **"Gradle sync finished"** or **"Gradle build finished"**
- If there are errors, see the Troubleshooting section below

**Note:** The first sync may take 10-15 minutes as it downloads dependencies and sets up Chaquopy (Python runtime for Android).

### Step 3: Build the APK

**Option A: Build APK directly (Recommended for testing)**

1. In the top menu, go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**
2. Wait for the build to complete (check the Build output at the bottom)
3. When finished, you'll see a notification: **"APK(s) generated successfully"**
4. Click **"locate"** in the notification to open the folder, or find the APK at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

**Option B: Build and Run on Device/Emulator**

1. Connect your Android device via USB (with USB debugging enabled) OR start an Android emulator
2. Click the green **Run** button (▶️) in the toolbar, or press **Shift+F10** (Windows/Linux) or **Ctrl+R** (Mac)
3. Select your device/emulator from the list
4. The app will build, install, and launch automatically

### Step 4: Install the APK

**On Physical Device:**

1. Transfer the APK file (`app-debug.apk`) to your Android device
2. On your device, enable **"Install from Unknown Sources"** in Settings
3. Open the APK file on your device and tap **"Install"**

**On Emulator:**

1. Drag and drop the APK file onto the emulator window, OR
2. Use ADB from command line:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## Alternative: Building from Command Line

If you prefer using the command line:

1. Open Terminal/Command Prompt and navigate to the project directory:
   ```bash
   cd /path/to/tiktok_downloader_android_app
   ```

2. Make gradlew executable (Linux/Mac only):
   ```bash
   chmod +x gradlew
   ```

3. Build the APK:
   ```bash
   # On Mac/Linux:
   ./gradlew assembleDebug
   
   # On Windows:
   gradlew.bat assembleDebug
   ```

4. The APK will be at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

## Troubleshooting

### Build Issues

**Gradle Sync Failed / "Unable to find method" Error:**

This is usually caused by Gradle version incompatibility or corrupt cache. Follow these steps **in order**:

**Quick Fix (Recommended):**

1. **Close Android Studio completely**

2. **Run the cleanup script:**
   - **On Mac/Linux:** Open Terminal in the project folder and run:
     ```bash
     ./clean_gradle.sh
     ```
   - **On Windows:** Double-click `clean_gradle.bat` or run it from Command Prompt

3. **Restart Android Studio**

4. **Invalidate caches:**
   - Go to **File > Invalidate Caches / Restart**
   - Select **"Invalidate and Restart"**
   - Wait for Android Studio to restart completely

5. **Sync Gradle:**
   - Go to **File > Sync Project with Gradle Files**
   - Wait for sync to complete (this may take 5-10 minutes as it downloads dependencies)

**Manual Fix (if script doesn't work):**

1. **Close Android Studio completely**

2. **Stop Gradle daemons:**
   - Open Terminal/Command Prompt in the project directory
   - Run: `./gradlew --stop` (Mac/Linux) or `gradlew.bat --stop` (Windows)

3. **Delete cache folders:**
   - Delete `.gradle` folder in your project directory
   - Delete `app/build` folder (if exists)
   - Delete `build` folder (if exists)
   - Delete `~/.gradle/caches` (Mac/Linux) or `C:\Users\YourName\.gradle\caches` (Windows)
   - Delete `~/.gradle/daemon` (Mac/Linux) or `C:\Users\YourName\.gradle\daemon` (Windows)

4. **Kill Java processes (if needed):**
   - **Mac/Linux:** `pkill -f java` or `killall java`
   - **Windows:** Open Task Manager, end all Java processes

5. **Restart Android Studio**

6. **Invalidate caches:**
   - **File > Invalidate Caches / Restart > Invalidate and Restart**

7. **Sync Gradle:**
   - **File > Sync Project with Gradle Files**
   - Wait for completion

**If still failing:**

- The project uses Gradle 8.2 (compatible with Android Gradle Plugin 8.2.0)
- Verify `gradle/wrapper/gradle-wrapper.properties` has: `gradle-8.2-bin.zip`
- Make sure you have internet connection (Gradle needs to download dependencies)
- Check Android Studio's Gradle settings: **File > Settings > Build Tools > Gradle**
  - Use Gradle from: **'gradle-wrapper.properties' file**
  - Gradle JDK: Should be set to a JDK 17+

**SDK Not Found:**
- Go to **Tools > SDK Manager**
- Install the latest Android SDK Platform
- Install Android SDK Build-Tools
- Click **Apply** and wait for installation

**JDK Not Found:**
- Go to **File > Project Structure > SDK Location**
- Set the JDK location (usually Android Studio includes it)
- Or download JDK 17 from https://adoptium.net/

**Python Not Found (Chaquopy build errors):**
- Make sure Python 3.12 is installed on your system
- On macOS with Homebrew: `brew install python@3.12`
- Verify Python 3.12 is in your PATH: `python3.12 --version`
- Update the `buildPython` path in `app/build.gradle.kts` if Python 3.12 is installed in a different location
- The path should point to the Python 3.12 executable (e.g., `/opt/homebrew/bin/python3.12` on macOS)

**Build Errors:**
- Clean the project: **Build > Clean Project**
- Rebuild: **Build > Rebuild Project**
- Check the Build output panel for specific error messages

**Dependency Conflicts:**
- If you see errors about incompatible dependencies, try:
  - **File > Sync Project with Gradle Files**
  - **Build > Clean Project**
  - **Build > Rebuild Project**

**"Python 3.12 is not available for the ABI 'armeabi-v7a'":**
- This is expected - Python 3.12 only supports `arm64-v8a` and `x86_64` ABIs
- The app is configured to only build for these architectures
- If you need support for older devices, you may need to use an older Python version (but yt-dlp requires Python 3.10+)

### Runtime Issues

**Permission Denied:**
- The app will request storage permissions when needed
- Grant permissions when prompted
- If denied, go to **Settings > Apps > TikTok Downloader > Permissions** and enable Storage

**Download Fails:**
- Make sure you have an internet connection
- Verify the TikTok URL is valid and the video is publicly accessible
- Some videos may be region-locked or require login
- Check Logcat in Android Studio for detailed error messages

**Files Not Saving:**
- Check storage permissions are granted
- Verify device has available storage
- Check Logcat in Android Studio for error messages
- Files are saved to Downloads/TikTokDownloads folder (not app's private storage)
- On some Android 10+ devices, if subfolder creation fails, files may be saved directly to Downloads/

**App Crashes:**
- Check Logcat in Android Studio (bottom panel) for error messages
- Make sure all required permissions are granted
- Try uninstalling and reinstalling the app

### Common Error Messages

**"Failed to resolve: androidx..."**
- Sync Gradle: **File > Sync Project with Gradle Files**
- Make sure you have internet connection

**"Execution failed for task ':app:mergeDebugResources'"**
- Clean project: **Build > Clean Project**
- Delete `.gradle` folder and rebuild

**"Could not find or load main class"**
- Invalidate caches: **File > Invalidate Caches / Restart**

**"IllegalAccessError: superclass access check failed" / KAPT errors with JDK 17+:**

**✅ FIXED: Project migrated to KSP (Kotlin Symbol Processing)**

The project has been migrated from KAPT to KSP, which eliminates JDK module access issues. KSP is the modern replacement for KAPT and is faster and more reliable.

If you're seeing this error, it means the migration hasn't been applied yet:

This error occurs when KAPT (Kotlin Annotation Processing Tool) tries to access internal JDK classes. The fix has been applied, but **you MUST restart the Gradle daemon** for it to take effect.

**CRITICAL: Restart Gradle Daemon (Required!)**

The JVM arguments in `gradle.properties` only take effect when the Gradle daemon restarts. Follow these steps:

1. **Stop Gradle daemons:**
   - **Option A (Recommended):** In Android Studio, go to **File > Settings** (or **Android Studio > Preferences** on Mac)
     - Navigate to **Build, Execution, Deployment > Build Tools > Gradle**
     - Click **"Stop Gradle daemons"** button
   - **Option B:** Run the script:
     - Mac/Linux: `./restart_gradle_daemon.sh`
     - Windows: Double-click `restart_gradle_daemon.bat`
   - **Option C:** Close Android Studio completely

2. **Restart Android Studio** (if you closed it)

3. **Sync Gradle:**
   - **File > Sync Project with Gradle Files**
   - Wait for sync to complete (the daemon will restart with new settings)

4. **If error persists:**
   - **File > Invalidate Caches / Restart > Invalidate and Restart**
   - After restart, **File > Sync Project with Gradle Files**
   - **Build > Clean Project**
   - **Build > Rebuild Project**

**Note:** The project is configured for JDK 17+, but Android Studio may be using JDK 21. The JVM arguments work for both versions.

## Project Structure

```
tiktok_downloader_android_app/
├── app/
│   ├── build.gradle.kts          # App-level build configuration
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/tiktokdownloader/app/
│           │   ├── MainActivity.kt       # Main download screen
│           │   ├── HistoryActivity.kt    # Download history
│           │   ├── PythonTikTokDownloader.kt  # Python wrapper (Chaquopy)
│           │   └── python/
│           │       └── tiktok_downloader.py  # Python downloader (yt-dlp)
│           └── res/                      # Resources (layouts, strings, etc.)
├── build.gradle.kts              # Project-level build configuration
└── settings.gradle.kts           # Project settings
```

## Next Steps

After successfully building and installing the app:

1. **Open the app** on your device/emulator
2. **Enter a TikTok URL** (or use the paste button to paste from clipboard)
3. **Tap Download** - the video will be downloaded directly on your device!
4. **Check download history** to see completed downloads
5. **Videos are saved** to Downloads/TikTokDownloads folder

**Note:** The app is completely self-contained and doesn't require any backend server. All downloads happen directly on your device using Python (via Chaquopy) and yt-dlp.
