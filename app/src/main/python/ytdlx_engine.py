"""Small bridge between the Android app and yt-dlp.

The Android side passes plain URL/format arguments. Downloads are written to an
app-private staging directory, then Kotlin publishes the completed file through
MediaStore.
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

import yt_dlp


def _duration(value: Any) -> int | None:
    try:
        return int(value) if value is not None else None
    except (TypeError, ValueError):
        return None


def _summary(info: dict[str, Any]) -> dict[str, Any]:
    return {
        "id": info.get("id"),
        "title": info.get("title") or "Tanpa judul",
        "channel": info.get("channel") or info.get("uploader"),
        "uploader": info.get("uploader"),
        "duration": _duration(info.get("duration")),
        "view_count": info.get("view_count"),
        "upload_date": info.get("upload_date"),
        "thumbnail": info.get("thumbnail"),
        "webpage_url": info.get("webpage_url") or info.get("original_url"),
        "description": info.get("description"),
    }


def extract_metadata(url: str) -> str:
    options = {
        "quiet": True,
        "no_warnings": True,
        "ignoreerrors": False,
        "skip_download": True,
        "extract_flat": False,
    }
    with yt_dlp.YoutubeDL(options) as ydl:
        info = ydl.extract_info(url, download=False)
    if not info:
        raise RuntimeError("Metadata tidak tersedia untuk URL ini")
    if info.get("_type") == "playlist":
        entries = [entry for entry in (info.get("entries") or []) if entry]
        return json.dumps({
            "playlist": True,
            "title": info.get("title") or "Playlist",
            "entries": [_summary(entry) for entry in entries],
        }, ensure_ascii=False)
    return json.dumps(_summary(info), ensure_ascii=False)


def download(url: str, format_selector: str, output_dir: str) -> str:
    destination = Path(output_dir)
    destination.mkdir(parents=True, exist_ok=True)
    before = {path.resolve() for path in destination.iterdir() if path.is_file()}
    options = {
        "format": format_selector,
        "outtmpl": str(destination / "%(title)s [%(id)s].%(ext)s"),
        "noplaylist": True,
        "quiet": False,
        "no_warnings": False,
        "continuedl": True,
        "retries": 10,
        "fragment_retries": 10,
        "merge_output_format": "mp4/mkv",
    }
    with yt_dlp.YoutubeDL(options) as ydl:
        result = ydl.download([url])
    if result not in (None, 0):
        raise RuntimeError(f"yt-dlp mengembalikan kode {result}")
    candidates = [path for path in destination.iterdir() if path.is_file() and path.resolve() not in before]
    if not candidates:
        raise RuntimeError("Download selesai tetapi file hasil tidak ditemukan")
    return str(max(candidates, key=lambda path: path.stat().st_mtime))
