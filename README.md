# TikTok Downloader Android App

A self-contained native Android application for downloading TikTok videos directly on your device. No backend server required!

## Features

- 📥 Download TikTok videos by URL directly on your device
- 🐍 Uses Python 3.12 (via Chaquopy) with yt-dlp for reliable downloads
- 📚 Download history with local SQLite storage
- 🔁 View and re-download previous downloads
- 💾 Save videos to Downloads/TikTokDownloads folder
- 🎨 Modern Material Design UI
- 📋 One-tap paste from clipboard (clears and pastes in one action)

## Prerequisites

- **Android Studio** - Download from [developer.android.com/studio](https://developer.android.com/studio)
- **JDK 17+** - Included with Android Studio
- **Android SDK** - Installed via Android Studio SDK Manager
- **Python 3.12** - Required for building (used by Chaquopy to compile Python bytecode)

## Quick Start

### 1. Build the APK

**Using Android Studio:**
1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**
4. Find the APK at: `app/build/outputs/apk/debug/app-debug.apk`

**Using Command Line:**
```bash
cd tiktok_downloader_android_app
./gradlew assembleDebug  # Mac/Linux
gradlew.bat assembleDebug  # Windows
```

### 2. Install and Run

1. Transfer the APK to your Android device
2. Enable "Install from Unknown Sources" in device settings
3. Install the APK
4. Open the app and enter a TikTok URL
5. Tap "Download" - the video will be downloaded directly on your device!

## Project Structure

```
tiktok_downloader_android_app/
├── app/
│   ├── build.gradle.kts          # App-level build configuration
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       ├── java/com/tiktokdownloader/app/
│       │   ├── MainActivity.kt       # Main download screen
│       │   ├── HistoryActivity.kt    # Download history
│       │   ├── PythonTikTokDownloader.kt  # Python wrapper (Chaquopy)
│       │   ├── python/
│       │   │   └── tiktok_downloader.py  # Python downloader (yt-dlp)
│       │   └── data/                 # Room database for local storage
│       │       ├── AppDatabase.kt
│       │       ├── DownloadHistory.kt
│       │       └── DownloadHistoryDao.kt
│       └── res/                      # Resources (layouts, strings, etc.)
├── build.gradle.kts              # Project-level build configuration
├── settings.gradle.kts           # Project settings
└── README.md                      # This file
```

## How It Works

The app is **completely self-contained** and doesn't require any backend server:

1. **Python Integration**: Uses [Chaquopy](https://chaquo.com/chaquopy/) to embed Python 3.12 runtime in the Android app
2. **TikTok Downloader**: Uses `yt-dlp` (a Python library) to download videos directly from TikTok
3. **Local Processing**: All video downloads happen on your device - no data is sent to external servers
4. **Offline Capable**: Once built, the app works completely offline (except for downloading videos from TikTok)

## Features in Detail

### Download Videos

1. Enter a TikTok URL in the main screen (or use the paste button to paste from clipboard)
2. Tap "Download"
3. The app uses Python/yt-dlp to download the video directly on your device
4. Download progress is shown in real-time
5. Completed videos are saved to Downloads/TikTokDownloads folder

### Paste Button

- The paste button (📋 icon) in the URL input field:
  - **Always clears** the input field first
  - **Then pastes** the clipboard content
  - This ensures a clean paste every time

### Download History

- All downloads are stored locally in SQLite (Room database)
- View download history in the History screen
- Re-download videos from history
- Clear all history with the "Clear All History" button
- History persists across app restarts

### File Storage

- Videos are saved to the public Downloads folder in a dedicated subfolder
- All Android versions: Saved to `Downloads/TikTokDownloads/`
- Files are named with timestamp: `YYYY-MM-DD_HHMMSS.mp4`
- Files are playable by any video player
- The TikTokDownloads folder is automatically created if it doesn't exist

## Requirements

- **Minimum SDK:** Android 7.0 (API 24)
- **Target SDK:** Android 14 (API 34)
- **Kotlin:** 1.9.20
- **Gradle:** 8.2
- **Android Gradle Plugin:** 8.2.0
- **JDK:** 17+
- **Python:** 3.12 (for building - Chaquopy uses it to compile Python bytecode)

## Dependencies

- **Room** - Local SQLite database
- **Coroutines** - Asynchronous programming
- **Material Components** - UI components
- **Chaquopy** - Python runtime for Android
- **yt-dlp** - Python library for downloading videos (embedded via Chaquopy)

## Building in Android Studio

### Step 1: Open the Project

1. Open Android Studio
2. Click **"Open"** or **"Open an Existing Project"**
3. Navigate to and select the `tiktok_downloader_android_app` folder
4. Click **"OK"**

### Step 2: Wait for Gradle Sync

- Android Studio will automatically start syncing Gradle files
- Wait until you see **"Gradle sync finished"** in the status bar
- If there are errors, see Troubleshooting section below

### Step 3: Build the APK

**Option A: Build APK directly (Recommended for testing)**
1. In the top menu, go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**
2. Wait for the build to complete
3. When finished, you'll see a notification: **"APK(s) generated successfully"**
4. Click **"locate"** in the notification, or find the APK at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

**Option B: Build and Run on Device/Emulator**
1. Connect your Android device via USB (with USB debugging enabled) OR start an Android emulator
2. Click the green **Run** button (▶️) in the toolbar, or press **Shift+F10** (Windows/Linux) or **Ctrl+R** (Mac)
3. Select your device/emulator from the list
4. The app will build, install, and launch automatically

## Troubleshooting

### Build Issues

**Gradle Sync Failed / "Unable to find method" Error:**

1. **Close Android Studio completely**
2. **Run the cleanup script:**
   - **On Mac/Linux:** `./clean_gradle.sh`
   - **On Windows:** Double-click `clean_gradle.bat`
3. **Restart Android Studio**
4. **Invalidate caches:**
   - Go to **File > Invalidate Caches / Restart**
   - Select **"Invalidate and Restart"**
5. **Sync Gradle:**
   - Go to **File > Sync Project with Gradle Files**

**SDK Not Found:**
- Go to **Tools > SDK Manager**
- Install the latest Android SDK Platform
- Install Android SDK Build-Tools

**JDK Not Found:**
- Go to **File > Project Structure > SDK Location**
- Set the JDK location (usually Android Studio includes it)
- Or download JDK 17 from https://adoptium.net/

**Python Not Found (Chaquopy build errors):**
- Make sure Python 3.12 is installed on your system
- On macOS with Homebrew: `brew install python@3.12`
- Update the `buildPython` path in `app/build.gradle.kts` if Python 3.12 is installed in a different location
- The path should point to the Python 3.12 executable (e.g., `/opt/homebrew/bin/python3.12` on macOS)

**Build Errors:**
- Clean the project: **Build > Clean Project**
- Rebuild: **Build > Rebuild Project**

### Runtime Issues

**Permission Denied:**
- The app will request storage permissions when needed
- Grant permissions when prompted
- If denied, go to **Settings > Apps > TikTok Downloader > Permissions** and enable Storage

**Files Not Saving:**
- Check storage permissions are granted
- Verify device has available storage
- Check Logcat in Android Studio for error messages
- Files are saved to Downloads/TikTokDownloads folder (not app's private storage)
- On some Android 10+ devices, if subfolder creation fails, files may be saved directly to Downloads/

**Download Fails:**
- Make sure you have an internet connection
- Verify the TikTok URL is valid and the video is publicly accessible
- Some videos may be region-locked or require login
- Check Logcat in Android Studio for detailed error messages

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

**"Python 3.12 is not available for the ABI 'armeabi-v7a'"**
- This is expected - Python 3.12 only supports `arm64-v8a` and `x86_64` ABIs
- The app is configured to only build for these architectures
- If you need support for older devices, you may need to use an older Python version (but yt-dlp requires Python 3.10+)

## Development

### Running in Android Studio

1. Open project in Android Studio
2. Wait for Gradle sync
3. Connect device or start emulator
4. Click Run (▶️) or press `Shift+F10`

### Debugging

- Use Android Studio's **Logcat** to view logs
- Set breakpoints in Kotlin code
- Python errors from yt-dlp will appear in Logcat as well
- Look for tags: `MainActivity`, `PythonTikTokDownloader`, `Python`

### Python Code

The Python downloader is located at:
- `app/src/main/python/tiktok_downloader.py`

This file is embedded in the APK and runs via Chaquopy's Python runtime. To modify the downloader:
1. Edit `tiktok_downloader.py`
2. Rebuild the app
3. The changes will be included in the new APK

## Technical Details

### Chaquopy Integration

- **Python Version:** 3.12
- **Python Library:** yt-dlp 2025.12.8
- **ABI Support:** arm64-v8a, x86_64 (Python 3.12 limitation)
- **Build Python:** Uses system Python 3.12 to compile bytecode during build

### Architecture

- **MainActivity**: UI and user interaction
- **PythonTikTokDownloader**: Kotlin wrapper that calls Python code via Chaquopy
- **tiktok_downloader.py**: Python class that uses yt-dlp to download videos
- **AppDatabase**: Room database for storing download history locally

## License

[Add your license here]
