# TikTok Downloader Android App

A native Android application for downloading TikTok videos using the TikTok Downloader backend API.

## Features

- 📥 Download TikTok videos by URL
- 🔄 Real-time download progress via WebSocket
- 📚 Download history with local SQLite storage
- 🔁 View and re-download previous downloads
- 💾 Save videos to Downloads/TikTokDownloads folder
- 🎨 Modern Material Design UI
- 🔐 Environment-based configuration (.env file)

## Prerequisites

- **Android Studio** - Download from [developer.android.com/studio](https://developer.android.com/studio)
- **JDK 17+** - Included with Android Studio
- **Android SDK** - Installed via Android Studio SDK Manager
- **Backend Server** - The Python backend must be running (see [backend README](../tiktok_downloader_backend/README.md))

## Quick Start

### 1. Configure Environment Variables

Before building, configure the backend server URL and API key using a `.env` file:

1. **Copy the example file:**
   ```bash
   cp app/src/main/assets/.env.example app/src/main/assets/.env
   ```

2. **Edit `app/src/main/assets/.env` and set your values:**
   ```env
   BASE_URL=http://YOUR_SERVER_IP:8000/
   API_KEY=your-api-key-here
   ```
   
   - **For Android Emulator:** `BASE_URL=http://10.0.2.2:8000/`
   - **For Physical Device:** `BASE_URL=http://YOUR_COMPUTER_IP:8000/`
     - Find your IP: `ifconfig` (Mac/Linux) or `ipconfig` (Windows)
   - **API_KEY:** Leave empty if your backend doesn't require an API key

**Note:** The `.env` file is automatically loaded when the app starts. No code changes needed!

### 2. Build the APK

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

### 3. Install and Run

1. Transfer the APK to your Android device
2. Enable "Install from Unknown Sources" in device settings
3. Install the APK
4. Start the backend server (see [backend README](../tiktok_downloader_backend/README.md))
5. Open the app and enter a TikTok URL

## Project Structure

```
tiktok_downloader_android_app/
├── app/
│   ├── build.gradle.kts          # App-level build configuration
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   └── .env.example      # Environment variables template
│       ├── java/com/tiktokdownloader/app/
│       │   ├── AppConfig.kt          # Environment configuration loader
│       │   ├── ApiService.kt         # API client
│       │   ├── MainActivity.kt       # Main download screen
│       │   ├── HistoryActivity.kt    # Download history
│       │   ├── WebSocketService.kt   # WebSocket client for real-time updates
│       │   └── data/                 # Room database for local storage
│       │       ├── AppDatabase.kt
│       │       ├── DownloadHistory.kt
│       │       └── DownloadHistoryDao.kt
│       └── res/                      # Resources (layouts, strings, etc.)
├── build.gradle.kts              # Project-level build configuration
├── settings.gradle.kts           # Project settings
└── README.md                      # This file
```

## Configuration

### Environment Variables (.env file)

All configuration is done via a `.env` file in `app/src/main/assets/`:

1. **Copy the example file:**
   ```bash
   cp app/src/main/assets/.env.example app/src/main/assets/.env
   ```

2. **Edit `.env` file:**
   ```env
   BASE_URL=http://your-server:8000/
   API_KEY=your-api-key-here
   ```

3. **Rebuild the app** - The configuration is loaded at runtime.

**How it works:**
- The `.env` file is bundled into the APK at build time
- When the app starts, `AppConfig.initialize()` loads values from the `.env` file
- Values are accessed via `AppConfig.BASE_URL` and `AppConfig.API_KEY`
- If `.env` is missing, default values are used

**Note:** The `.env` file is gitignored and won't be committed to version control. Use `.env.example` as a template.

## Features in Detail

### Download Videos

1. Enter a TikTok URL in the main screen
2. Tap "Download"
3. The app connects to the backend via WebSocket for real-time updates
4. Download progress is shown in real-time
5. Completed videos are saved to Downloads/TikTokDownloads folder

### Download History

- All downloads are stored locally in SQLite (Room database)
- View download history in the History screen
- Re-download videos from history
- History persists even if the backend is offline

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

## Dependencies

- **Room** - Local SQLite database
- **Retrofit** - HTTP client for API calls
- **OkHttp** - HTTP client with WebSocket support
- **Coroutines** - Asynchronous programming
- **Material Components** - UI components
- **dotenv-kotlin** - Environment variable loader

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

### Step 3: Configure Environment Variables

1. Copy `.env.example` to `.env`:
   ```bash
   cp app/src/main/assets/.env.example app/src/main/assets/.env
   ```

2. Edit `app/src/main/assets/.env` with your backend URL and API key

### Step 4: Build the APK

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

**Build Errors:**
- Clean the project: **Build > Clean Project**
- Rebuild: **Build > Rebuild Project**

### Runtime Issues

**Connection Error / Can't Connect to Server:**
- Make sure the Python backend server is running
- Check that `BASE_URL` in `.env` file is correct:
  - For emulator: `http://10.0.2.2:8000/`
  - For physical device: `http://YOUR_COMPUTER_IP:8000/` (not `localhost`)
- Make sure your computer and device are on the same network
- Check firewall settings (port 8000 should be accessible)

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

**App Crashes:**
- Check Logcat in Android Studio (bottom panel) for error messages
- Make sure all required permissions are granted
- Verify the backend server is running and accessible
- Check that `.env` file exists and is properly formatted

### Common Error Messages

**"Failed to resolve: androidx..."**
- Sync Gradle: **File > Sync Project with Gradle Files**
- Make sure you have internet connection

**"Execution failed for task ':app:mergeDebugResources'"**
- Clean project: **Build > Clean Project**
- Delete `.gradle` folder and rebuild

**"Could not find or load main class"**
- Invalidate caches: **File > Invalidate Caches / Restart**

## Development

### Running in Android Studio

1. Open project in Android Studio
2. Wait for Gradle sync
3. Connect device or start emulator
4. Click Run (▶️) or press `Shift+F10`

### Debugging

- Use Android Studio's **Logcat** to view logs
- Set breakpoints in Kotlin code
- Use **Network Inspector** to debug API calls
- Check **AppConfig** logs to verify `.env` loading

### Environment Variables Debugging

To verify `.env` is loaded correctly, check Logcat for:
```
AppConfig: Could not load .env file: ...
```

If you see this warning, the `.env` file is missing or has errors. Check:
1. File exists: `app/src/main/assets/.env`
2. File format is correct (no spaces around `=`)
3. Rebuild the app after creating/editing `.env`

## API Integration

The app communicates with the backend via:

- **REST API** - For download requests and status checks
- **WebSocket** - For real-time download progress updates

### API Endpoints Used

- `POST /download` - Start video download
- `GET /download/status/{task_id}` - Get download status
- `GET /download/file/{file_name}` - Download video file
- `WS /ws/{client_id}` - WebSocket for real-time updates

See [backend API documentation](../tiktok_downloader_backend/readme_files/API_USAGE.md) for details.

## Related Documentation

- **Backend README**: See `../tiktok_downloader_backend/README.md`
- **Backend API Usage**: See `../tiktok_downloader_backend/readme_files/API_USAGE.md`
- **Backend WebSocket Usage**: See `../tiktok_downloader_backend/readme_files/WEBSOCKET_USAGE.md`

## License

[Add your license here]
