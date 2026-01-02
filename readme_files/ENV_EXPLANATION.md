# How Environment Variables Work in Android App

## 📁 File Location

The `.env` file is located in:
```
app/src/main/assets/.env
```

**Important:** 
- The `.env` file is **NOT** committed to git (it's in `.gitignore`)
- You need to create it manually from `.env.example`
- Files in `assets/` folder are bundled into the APK at build time

## 🔄 Loading Flow

### Step 1: App Starts
When the app launches, `MainActivity.onCreate()` is called:

```kotlin
// MainActivity.kt, line 49-53
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize configuration from .env file
    AppConfig.initialize(this)  // ← This loads the .env file
    ...
}
```

### Step 2: AppConfig.initialize()
This method loads the `.env` file from assets:

```kotlin
// AppConfig.kt, line 21-42
fun initialize(context: Context) {
    try {
        // 1. Read .env file from assets folder (bundled in APK)
        val envContent = context.assets.open(".env").bufferedReader().use { it.readText() }
        
        // 2. Write to cache directory (because dotenv library needs a file path)
        val cacheDir = context.cacheDir
        val envFile = File(cacheDir, ".env")
        envFile.writeText(envContent)
        
        // 3. Load using dotenv library
        dotenv = dotenv {
            directory = cacheDir.absolutePath
            filename = ".env"
            ignoreIfMissing = false
        }
    } catch (e: Exception) {
        // If .env file doesn't exist, use default values
        dotenv = null
    }
}
```

**Why copy to cache?**
- The `dotenv-kotlin` library needs a file path to read from
- Assets are read-only, so we copy to cache directory first
- Cache directory is writable and accessible

### Step 3: Accessing Values
When code needs configuration values, it accesses `AppConfig`:

```kotlin
// ApiService.kt, line 88
val retrofit = Retrofit.Builder()
    .baseUrl(AppConfig.BASE_URL)  // ← Reads from .env or uses default
    ...
```

```kotlin
// ApiService.kt, line 69
if (AppConfig.API_KEY.isNotEmpty()) {  // ← Reads from .env or uses default
    ...
}
```

## 📋 Complete Flow Diagram

```
1. Build Time:
   app/src/main/assets/.env → Bundled into APK

2. Runtime (App Start):
   MainActivity.onCreate()
   ↓
   AppConfig.initialize(context)
   ↓
   Read from assets: context.assets.open(".env")
   ↓
   Copy to cache: cacheDir/.env
   ↓
   Load with dotenv library
   ↓
   Store in AppConfig.dotenv

3. Runtime (When Needed):
   ApiService.create()
   ↓
   AppConfig.BASE_URL  → Reads from dotenv or default
   AppConfig.API_KEY   → Reads from dotenv or default
```

## 🔍 Where Values Are Used

### 1. ApiService.kt
- **BASE_URL**: Used in Retrofit builder (line 88)
- **API_KEY**: Used in API key interceptor (line 73)

### 2. WebSocketService.kt (if used)
- **BASE_URL**: Converted to WebSocket URL via `AppConfig.getWebSocketUrl()`

## 📝 Default Values

If `.env` file is missing or a value is not set, defaults are used:

- **BASE_URL**: `"http://10.0.2.2:8000/"` (Android emulator default)
- **API_KEY**: `""` (empty string)

## 🛠️ How to Set Up

1. **Copy example file:**
   ```bash
   cp app/src/main/assets/.env.example app/src/main/assets/.env
   ```

2. **Edit `.env` file:**
   ```env
   BASE_URL=http://192.168.0.105:8000/
   API_KEY=your-api-key-here
   ```

3. **Rebuild the app** - The `.env` file is bundled into the APK

## ⚠️ Important Notes

1. **Assets are read-only**: The `.env` file in assets is bundled into the APK and cannot be changed at runtime
2. **Cache copy**: The file is copied to cache directory for the dotenv library to read
3. **No runtime changes**: To change values, you must edit `.env` and rebuild the app
4. **Git ignored**: `.env` is in `.gitignore`, so it won't be committed (use `.env.example` as template)

## 🔍 Debugging

To check if `.env` is loaded correctly, add logging:

```kotlin
// In AppConfig.initialize()
android.util.Log.d("AppConfig", "BASE_URL: ${AppConfig.BASE_URL}")
android.util.Log.d("AppConfig", "API_KEY set: ${AppConfig.API_KEY.isNotEmpty()}")
```

Check Logcat in Android Studio to see the values.

