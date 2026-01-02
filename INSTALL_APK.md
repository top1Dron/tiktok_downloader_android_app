# Installing the APK on Your Device

## APK Location

The built APK is located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

Full path:
```
/Users/apple/Desktop/work/test/tiktok_downloader/tiktok_downloader_android_app/app/build/outputs/apk/debug/app-debug.apk
```

## Installation Methods

### Method 1: Install via ADB (Recommended)

**For Physical Device:**
1. Enable **Developer Options** on your Android device:
   - Go to **Settings > About Phone**
   - Tap **"Build Number"** 7 times
   
2. Enable **USB Debugging**:
   - Go to **Settings > Developer Options**
   - Enable **"USB Debugging"**

3. Connect your device via USB

4. Install the APK:
   ```bash
   cd /Users/apple/Desktop/work/test/tiktok_downloader/tiktok_downloader_android_app
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

**For Emulator:**
```bash
cd /Users/apple/Desktop/work/test/tiktok_downloader/tiktok_downloader_android_app
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Method 2: Transfer and Install Manually

1. **Transfer APK to device:**
   - Email it to yourself
   - Use cloud storage (Google Drive, Dropbox, etc.)
   - Use USB file transfer
   - Use ADB push:
     ```bash
     adb push app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/
     ```

2. **On your device:**
   - Open **Files** or **Downloads** app
   - Find `app-debug.apk`
   - Tap it to install
   - If prompted, allow **"Install from Unknown Sources"**

### Method 3: Drag and Drop (Emulator Only)

1. Start the Android emulator in Android Studio
2. Drag the APK file (`app-debug.apk`) onto the emulator window
3. The installation will start automatically

## Setting Up Run/Debug in Android Studio

### Enable Run/Debug for Physical Device

1. **Enable Developer Options** on your device (see Method 1 above)

2. **Enable USB Debugging** (see Method 1 above)

3. **Connect device via USB**

4. **In Android Studio:**
   - Look at the top toolbar
   - You should see a device dropdown (may show "No devices")
   - Click it and select your connected device
   - If device doesn't appear:
     - Go to **Tools > Device Manager**
     - Or check **Tools > Connection Assistant**

5. **Run the app:**
   - Click the green **Run** button (▶️) in the toolbar
   - Or press **Shift+F10** (Windows/Linux) or **Ctrl+R** (Mac)
   - The app will build, install, and launch automatically

### Enable Run/Debug for Emulator

1. **Start an emulator:**
   - Go to **Tools > Device Manager**
   - Click **Create Device** if you don't have one
   - Select a device and click **Play** button (▶️) to start it

2. **Select the emulator:**
   - In the device dropdown (top toolbar), select your running emulator

3. **Run the app:**
   - Click the green **Run** button (▶️)
   - Or press **Shift+F10** (Windows/Linux) or **Ctrl+R** (Mac)

## Troubleshooting

**"No devices found":**
- Make sure USB debugging is enabled
- Try a different USB cable/port
- Restart ADB: **Tools > Connection Assistant > Restart ADB Server**
- Check if device appears: Run `adb devices` in terminal

**"Installation failed":**
- Make sure "Install from Unknown Sources" is enabled
- Uninstall any previous version first
- Check device storage space

**Run button is grayed out:**
- Make sure a device/emulator is selected
- Wait for Gradle sync to complete
- Check for build errors in the Build panel

## Quick Commands

**Check connected devices:**
```bash
adb devices
```

**Install APK:**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Uninstall app (if needed):**
```bash
adb uninstall com.tiktokdownloader.app
```

**Reinstall (uninstall + install):**
```bash
adb uninstall com.tiktokdownloader.app && adb install app/build/outputs/apk/debug/app-debug.apk
```

