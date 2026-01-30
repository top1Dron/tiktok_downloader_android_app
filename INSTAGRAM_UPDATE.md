# Instagram Support Added

## Overview

The app now supports downloading videos from both TikTok and Instagram with a tabbed interface.

## Changes Made

### 1. Python Downloader - Refactored Architecture

**Files**:
- `app/src/main/python/base_downloader.py` - Base downloader class with common logic
- `app/src/main/python/tiktok_downloader.py` - TikTok-specific downloader
- `app/src/main/python/instagram_downloader.py` - Instagram-specific downloader

**Architecture**:
- `BaseDownloader` class handles:
  - Common yt-dlp options
  - Proxy management
  - File handling
  - Error handling
- `TikTokDownloader` extends `BaseDownloader`:
  - TikTok-specific URL normalization
  - TikTok extractor configuration
- `InstagramDownloader` extends `BaseDownloader`:
  - Instagram-specific URL normalization
  - Instagram extractor configuration
  - Removes tracking parameters from URLs

### 2. Database Updates

**File**: `app/src/main/java/com/tiktokdownloader/app/data/DownloadHistory.kt`

- Added `platform` field (String): "tiktok" or "instagram"
- Database version updated to 2
- Fallback to destructive migration for schema changes

**File**: `app/src/main/java/com/tiktokdownloader/app/data/DownloadHistoryDao.kt`

- Added `getHistoryByPlatform()` to filter by platform
- Added `getRecentHistoryByPlatform()` to get recent downloads by platform

### 3. UI Updates

**File**: `app/src/main/res/layout/activity_main.xml`

- Tab-based layout with TikTok and Instagram tabs
- ViewPager2 for swipe navigation
- Material Design toolbar with tabs

**File**: `app/src/main/res/layout/fragment_download.xml`

- Reusable fragment layout for download functionality
- Works for both TikTok and Instagram

**File**: `app/src/main/res/layout/item_history.xml` & `item_recent_download.xml`

- Added platform badge (TikTok = black, Instagram = pink)
- Shows platform type for each download

### 4. New Components

**DownloadFragment.kt** - Fragment for download functionality
- Takes platform parameter ("tiktok" or "instagram")
- Updates UI text based on platform
- Validates URLs for correct platform
- Loads recent downloads filtered by platform

**ViewPagerAdapter.kt** - Adapter for tabs
- Creates fragments for each platform

**MainActivity.kt** - Simplified main activity
- Manages tabs and ViewPager
- Handles permissions

### 5. Build Configuration

**File**: `app/build.gradle.kts`

- Added ViewPager2 dependency
- Updated yt-dlp to support Instagram (version 2025.12.8 already includes Instagram support)

## Features

### Tab Navigation
- **TikTok Tab** (default): Download TikTok videos
- **Instagram Tab**: Download Instagram Reels and posts

### Shared Features (Both Tabs)
- URL input and download
- Recent downloads (last 5)
- Full history page
- Reload functionality
- Platform-specific validation

### Visual Indicators
- Platform badges with colors:
  - TikTok: Black (#000000)
  - Instagram: Pink (#E4405F)

### History Management
- Separate history for each platform
- Filter by platform in history view
- Shared database for all downloads

## Usage

1. **Download from TikTok**:
   - Open app (TikTok tab is default)
   - Enter TikTok URL
   - Tap "Download Video"

2. **Download from Instagram**:
   - Switch to Instagram tab
   - Enter Instagram Reel/Post URL (e.g., https://www.instagram.com/reel/DUF5XD-CN5w/)
   - Tap "Download Video"

3. **View History**:
   - Tap "View Full History" to see all downloads
   - Platform badges show source (TikTok or Instagram)
   - Tap "Reload" to re-download or access file

## Rebuild Instructions

1. **Sync Gradle files** in Android Studio
2. **Clean and rebuild** the project
3. **Build APK**: Build > Build Bundle(s) / APK(s) > Build APK(s)
4. **Install on device** or emulator

## Error Handling

The app now includes full exception details in error messages to help debug issues like:
- "Unable to extract webpage video data"
- Network connectivity problems
- Private or deleted videos

Error messages include:
- Exception type and message
- Exception attributes (may include response data from yt-dlp)
- Full traceback

## Supported URL Formats

### TikTok
- `https://www.tiktok.com/@username/video/1234567890`
- `https://vm.tiktok.com/xxxxx/`
- `https://www.tiktok.com/t/xxxxx/`

### Instagram
- `https://www.instagram.com/reel/xxxxx/`
- `https://www.instagram.com/p/xxxxx/` (posts)
- `https://www.instagram.com/tv/xxxxx/` (IGTV)

## Notes

- yt-dlp 2025.12.8 already supports Instagram natively
- No additional libraries needed
- Same download engine for both platforms
- Platform detection is automatic if using the correct tab
