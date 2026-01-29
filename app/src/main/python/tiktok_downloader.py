"""
TikTok video downloader using yt-dlp.
Adapted for Android using Chaquopy.
"""

import os
import tempfile
from pathlib import Path
import yt_dlp



class DownloadError(Exception):
    """Custom exception for download errors."""
    pass


class TikTokDownloader:
    """Class for downloading a TikTok video from a given URL."""

    def __init__(self, output_dir: str = None):
        """
        Initialize TikTokDownloader.

        Args:
            output_dir: Directory to save downloaded videos. If None, uses temp directory.
        """
        if output_dir is None:
            self.output_dir = tempfile.gettempdir()
        else:
            self.output_dir = output_dir
            Path(self.output_dir).mkdir(parents=True, exist_ok=True)

    def _normalize_url(self, url: str) -> str:
        """
        Normalize TikTok URL to standard format.
        Handles various TikTok URL formats.
        """
        url = url.strip()
        
        # Handle short URLs (vm.tiktok.com, tiktok.com/t/)
        if "vm.tiktok.com" in url or "tiktok.com/t/" in url:
            # These should work as-is, but ensure they're complete
            if not url.startswith("http"):
                url = "https://" + url
            return url
        
        # Handle full URLs
        if "tiktok.com" in url:
            # Ensure it's https
            if url.startswith("http://"):
                url = url.replace("http://", "https://")
            return url
        
        # If it doesn't look like a TikTok URL, return as-is
        # (yt-dlp will handle the error)
        return url

    def download(self, url: str) -> str:
        """
        Download a TikTok video from a given URL.

        Args:
            url: TikTok video URL

        Returns:
            Path to the downloaded video file

        Raises:
            DownloadError: If download fails
        """
        # Temporarily unset proxy environment variables for this download
        original_proxy_vars = {}
        proxy_vars = [
            "HTTP_PROXY",
            "HTTPS_PROXY",
            "http_proxy",
            "https_proxy",
            "ALL_PROXY",
            "all_proxy",
        ]
        for var in proxy_vars:
            if var in os.environ:
                original_proxy_vars[var] = os.environ[var]
                del os.environ[var]

        try:
            # Normalize URL - handle different TikTok URL formats
            url = self._normalize_url(url)
            
            ydl_opts = {
                "outtmpl": os.path.join(self.output_dir, "%(title)s.%(ext)s"),
                "format": "best[ext=mp4]/best",
                "quiet": False,
                "no_warnings": False,
                # Explicitly disable proxy
                "proxy": "",
                # Add headers to make requests look more like a browser
                "http_headers": {
                    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                    "Accept-Language": "en-US,en;q=0.5",
                    "Accept-Encoding": "gzip, deflate",
                    "Connection": "keep-alive",
                    "Upgrade-Insecure-Requests": "1",
                },
            }

            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                try:
                    # Extract info to get the filename
                    # Use process=True to get full video info
                    info = ydl.extract_info(url, download=False)
                    # if not info:
                    #     raise DownloadError("Failed to extract video information")
                    filename = ydl.prepare_filename(info)
                except yt_dlp.DownloadError as e:
                    # Video not available, invalid URL, or extraction failed
                    raise DownloadError(f"Video not available or invalid URL: {str(e)}")
                except Exception as e:
                    # Other extraction errors (connection issues, etc.)
                    raise DownloadError(f"Failed to extract video info: {str(e)}")

                try:
                    # Download the video
                    ydl.download([url])
                except yt_dlp.DownloadError as e:
                    # Download failed (video unavailable, connection issues, etc.)
                    raise DownloadError(f"Failed to download video: {str(e)}")
                except Exception as e:
                    # Other download errors
                    raise DownloadError(f"Download error: {str(e)}")

                # Return the path to the downloaded file
                if os.path.exists(filename):
                    return filename
                else:
                    # Sometimes the extension might differ, try to find the file
                    base_name = os.path.splitext(filename)[0]
                    for ext in [".mp4", ".webm", ".mkv"]:
                        potential_file = base_name + ext
                        if os.path.exists(potential_file):
                            return potential_file
                    raise DownloadError(f"Downloaded file not found: {filename}")
        except DownloadError:
            # Re-raise DownloadError as-is
            raise
        except Exception as e:
            # Unexpected errors - still return as DownloadError for 400 status
            raise DownloadError(f"Failed to download TikTok video: {str(e)}")
        finally:
            # Restore original proxy environment variables
            for var, value in original_proxy_vars.items():
                os.environ[var] = value
