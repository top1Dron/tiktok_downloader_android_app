"""
TikTok video downloader using yt-dlp.
Adapted for Android using Chaquopy.
"""

from base_downloader import BaseDownloader, DownloadError


class TikTokDownloader(BaseDownloader):
    """Class for downloading TikTok videos."""

    def _normalize_url(self, url: str) -> str:
        """
        Normalize TikTok URL to standard format.
        Handles various TikTok URL formats.
        """
        url = url.strip()
        
        # Handle short URLs (vm.tiktok.com, tiktok.com/t/)
        if "vm.tiktok.com" in url or "tiktok.com/t/" in url:
            if not url.startswith("http"):
                url = "https://" + url
            return url
        
        # Handle full URLs
        if "tiktok.com" in url:
            if url.startswith("http://"):
                url = url.replace("http://", "https://")
            return url
        
        return url

    def _get_ydl_opts(self) -> dict:
        """Get TikTok-specific yt-dlp options."""
        opts = super()._get_ydl_opts()
        opts["extractor_args"] = {
            "tiktok": {
                "webpage_download_timeout": 60,
                "api_hostname": "api.tiktok.com",
            }
        }
        opts["http_headers"]["Referer"] = "https://www.tiktok.com/"
        return opts
