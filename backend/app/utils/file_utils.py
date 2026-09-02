import os
from pathlib import Path
from PIL import Image

def get_file_extension(filename: str) -> str:
    """Get lowercase file extension (e.g. 'jpg')"""
    return os.path.splitext(filename)[1].lower().replace(".", "")

def resize_image(source_path: Path, dest_path: Path, max_width: int, max_height: int) -> None:
    """Resize image preserving aspect ratio and save it as JPEG"""
    with Image.open(source_path) as img:
        img.thumbnail((max_width, max_height))
        if img.mode in ("RGBA", "P"):
            img = img.convert("RGB")
        dest_path.parent.mkdir(parents=True, exist_ok=True)
        img.save(dest_path, "JPEG")
