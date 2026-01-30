# Instagram Integration - Refactored Architecture

## Overview

The app has been refactored to support both TikTok and Instagram downloads with a clean, modular architecture.

## Python Architecture

### File Structure
```
app/src/main/python/
├── base_downloader.py      # Base class with common logic
├── tiktok_downloader.py    # TikTok-specific implementation
└── instagram_downloader.py # Instagram-specific implementation
```

### Class Hierarchy

**BaseDownloader** (base_downloader.py)
- Common functionality for all downloaders
- Methods:
  - `__init__(output_dir)` - Initialize with output directory
  - `_normalize_url(url)` - Basic URL normalization
  - `_get_ydl_opts()` - Common yt-dlp options
  - `_clear_proxy_env()` - Clear proxy environment variables
  - `_restore_proxy_env()` - Restore proxy environment variables
  - `download(url)` - Main download method

**TikTokDownloader** (tiktok_downloader.py)
- Extends `BaseDownloader`
- Overrides:
  - `_normalize_url()` - TikTok-specific URL handling (vm.tiktok.com, short URLs, etc.)
  - `_get_ydl_opts()` - Adds TikTok extractor configuration

**InstagramDownloader** (instagram_downloader.py)
- Extends `BaseDownloader`
- Overrides:
  - `_normalize_url()` - Instagram-specific URL handling (removes tracking params)
  - `_get_ydl_opts()` - Adds Instagram extractor configuration

### Error Handling

Simplified error handling (no detailed traceback dumps):
- `DownloadError` exception for all download failures
- Clean error messages without verbose debugging info
- Standard Python exception messages from yt-dlp

## Android Architecture

### UI Components

**MainActivity** - Tab navigation
- Manages ViewPager2 with platform tabs
- Handles permissions
- No download logic

**DownloadFragment** - Download UI (reusable for both platforms)
- Takes platform parameter ("tiktok" or "instagram")
- Dynamically loads correct Python downloader:
  - TikTok: `tiktok_downloader.TikTokDownloader`
  - Instagram: `instagram_downloader.InstagramDownloader`
- Platform-specific URL validation
- Shows recent downloads filtered by platform

**HistoryActivity** - Download history
- Filters by platform if specified
- Uses correct downloader for reload functionality
- Shows platform badges

### Database Schema

**DownloadHistory** entity:
```kotlin
@Entity(tableName = "download_history")
data class DownloadHistory(
    @PrimaryKey val taskId: String,
    val url: String,
    val fileName: String?,
    val filePath: String?,
    val platform: String,  // "tiktok" or "instagram"
    val status: String,
    val createdAt: Long,
    val completedAt: Long?,
    val error: String?
)
```

**DAO Queries**:
- `getAllHistory()` - All downloads
- `getHistoryByPlatform(platform)` - Filter by platform
- `getRecentHistory(limit)` - Recent downloads (all platforms)
- `getRecentHistoryByPlatform(platform, limit)` - Recent downloads for one platform

## Usage

### Using Python Downloaders in Android

```kotlin
// TikTok Download
val python = Python.getInstance()
val module = python.getModule("tiktok_downloader")
val downloader = module.callAttr("TikTokDownloader")
downloader["output_dir"] = outputDir.absolutePath
val filePath = downloader.callAttr("download", url).toString()

// Instagram Download
val python = Python.getInstance()
val module = python.getModule("instagram_downloader")
val downloader = module.callAttr("InstagramDownloader")
downloader["output_dir"] = outputDir.absolutePath
val filePath = downloader.callAttr("download", url).toString()
```

### Using Python Downloaders Standalone

```python
# TikTok
from tiktok_downloader import TikTokDownloader

downloader = TikTokDownloader(output_dir="/path/to/save")
file_path = downloader.download("https://www.tiktok.com/@user/video/123")

# Instagram
from instagram_downloader import InstagramDownloader

downloader = InstagramDownloader(output_dir="/path/to/save")
file_path = downloader.download("https://www.instagram.com/reel/ABC123/")
```

## Key Improvements

### 1. **Separation of Concerns**
- Base class handles common logic
- Platform-specific classes only override what's different
- Easy to add new platforms (YouTube, Twitter, etc.)

### 2. **Code Reuse**
- No duplicate code between TikTok and Instagram
- Common proxy management, error handling, file handling

### 3. **Maintainability**
- Each class has a single responsibility
- Easy to update platform-specific logic
- Clear inheritance hierarchy

### 4. **Simplified Error Handling**
- Removed verbose exception formatting
- Clean, user-friendly error messages
- Standard yt-dlp error information

### 5. **Extensibility**
To add a new platform:
1. Create `new_platform_downloader.py`
2. Extend `BaseDownloader`
3. Override `_normalize_url()` and `_get_ydl_opts()` as needed
4. Update UI to include new tab

## Build Instructions

1. Sync Gradle files
2. Clean and rebuild project
3. Build APK
4. Test both TikTok and Instagram downloads

## Testing Checklist

- [ ] TikTok video download
- [ ] Instagram Reel download
- [ ] Instagram post download
- [ ] Recent downloads show correct platform badge
- [ ] History filtering by platform
- [ ] Reload from history (both platforms)
- [ ] Error messages are clean and readable
- [ ] Tab navigation works smoothly
- [ ] URL validation for each platform
