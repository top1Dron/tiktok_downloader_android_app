"""
Instagram video downloader using yt-dlp.
Adapted for Android using Chaquopy.
"""

from base_downloader import BaseDownloader, DownloadError


class InstagramDownloader(BaseDownloader):
    """Class for downloading Instagram videos and reels."""

    def _normalize_url(self, url: str) -> str:
        """
        Normalize Instagram URL to standard format.
        Handles various Instagram URL formats.
        """
        url = url.strip()
        
        if "instagram.com" in url:
            if not url.startswith("http"):
                url = "https://" + url
            elif url.startswith("http://"):
                url = url.replace("http://", "https://")
            
            # Remove tracking parameters
            if "?" in url:
                url = url.split("?")[0]
            
            return url
        
        return url

    def _get_ydl_opts(self) -> dict:
        """Get Instagram-specific yt-dlp options."""
        opts = super()._get_ydl_opts()
        opts["extractor_args"] = {
            "instagram": {
                "api_hostname": "i.instagram.com",
            }
        }
        return opts
