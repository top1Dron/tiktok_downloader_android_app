# How Android Assets Folder Works

## The Magic: `context.assets`

In Android, the `app/src/main/assets/` folder is a **special directory** that gets bundled into the APK at build time.

## How It Works

### 1. Build Time
```
app/src/main/assets/.env  →  Bundled into APK  →  Available at runtime
```

When you build the APK, Gradle automatically:
- Takes all files from `app/src/main/assets/`
- Bundles them into the APK
- Makes them accessible via `AssetManager`

### 2. Runtime Access

In your code:
```kotlin
context.assets.open(".env")
```

**What happens:**
- `context.assets` is an `AssetManager` instance
- It automatically points to the bundled assets folder
- `.open(".env")` opens the `.env` file from the bundled assets
- **No path needed** - Android knows where assets are!

## The Code Path

```kotlin
// AppConfig.kt, line 24
val envContent = context.assets.open(".env").bufferedReader().use { it.readText() }
```

**Step by step:**
1. `context.assets` → Gets the `AssetManager` (points to bundled assets)
2. `.open(".env")` → Opens `.env` file from assets folder
3. `.bufferedReader()` → Reads the file as text
4. `.use { it.readText() }` → Reads all content

## Why No Explicit Path?

Android's `AssetManager` is **hardcoded** to look in the assets folder that was bundled into the APK. You don't need to specify:
- ❌ `app/src/main/assets/.env` (source path - not available at runtime)
- ❌ `/assets/.env` (not a real filesystem path)
- ✅ Just `.env` (AssetManager knows where to look)

## File Location Mapping

| Development Path | Runtime Access |
|-----------------|----------------|
| `app/src/main/assets/.env` | `context.assets.open(".env")` |
| `app/src/main/assets/folder/file.txt` | `context.assets.open("folder/file.txt")` |

## Important Notes

1. **Assets are read-only** - You can't modify files in assets at runtime
2. **Must rebuild APK** - Changes to assets require rebuilding
3. **No subdirectories in path** - Use `"folder/file.txt"` not `"/folder/file.txt"`
4. **Case-sensitive** - File names are case-sensitive

## Verification

To verify the `.env` file is bundled:
1. Build the APK
2. Extract the APK (it's a ZIP file)
3. Look in `assets/` folder inside the APK
4. You should see `.env` file there

## Alternative: Why We Copy to Cache

Notice in the code:
```kotlin
// Read from assets (read-only)
val envContent = context.assets.open(".env")...

// Copy to cache directory (writable)
val envFile = File(cacheDir, ".env")
envFile.writeText(envContent)

// Then use dotenv library to read from cache
dotenv = dotenv {
    directory = cacheDir.absolutePath  // ← Uses cache, not assets
    filename = ".env"
}
```

**Why?**
- The `dotenv-kotlin` library needs a **file path** to read from
- Assets are accessed via `AssetManager`, not file paths
- So we copy from assets (read-only) to cache (writable) first
- Then the dotenv library can read it as a normal file

## Summary

- **Source:** `app/src/main/assets/.env` (development)
- **Runtime:** Bundled into APK, accessed via `context.assets.open(".env")`
- **No path needed** - Android's `AssetManager` handles it automatically
- **Must rebuild** - Changes to assets require rebuilding the APK

