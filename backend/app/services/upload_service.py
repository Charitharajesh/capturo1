import os
from typing import Optional
from pathlib import Path
from uuid import uuid4
import aiofiles
from fastapi import UploadFile
from PIL import Image
from app.core.config import settings
from app.core.exceptions import MediaUploadError

class UploadService:
    def validate_file(self, file: UploadFile, is_video: bool = False) -> None:
        """Validate file size and header signatures"""
        max_size = settings.MAX_VIDEO_SIZE_MB if is_video else settings.MAX_PHOTO_SIZE_MB
        
        # Check size by reading chunks
        size = 0
        for chunk in iter(lambda: file.file.read(1024 * 1024), b""):
            size += len(chunk)
            if size > max_size * 1024 * 1024:
                file.file.seek(0)
                raise MediaUploadError(f"File size exceeds the maximum limit of {max_size}MB")
        file.file.seek(0)

        # Sniff magic bytes
        header = file.file.read(16)
        file.file.seek(0)
        
        if not is_video:
            # Check image headers (JPEG: FF D8 FF, PNG: 89 50 4E 47, WEBP: RIFF + WEBP)
            if header.startswith(b"\xff\xd8\xff"):
                return
            if header.startswith(b"\x89PNG\r\n\x1a\n"):
                return
            if header.startswith(b"RIFF") and b"WEBP" in header:
                return
            raise MediaUploadError("Invalid image signature. Only JPEG, PNG and WEBP are supported.")
        else:
            # Check video headers (MP4 / MOV: typically contains ftyp or starts with standard containers)
            if b"ftyp" in header or header.startswith(b"\x1a\x45\xdf\xa3"):
                return
            raise MediaUploadError("Invalid video signature. Only standard video types are supported.")

    async def save_uploaded_file(self, file: UploadFile, creator_id: str, booking_id: Optional[str] = None) -> tuple[str, str]:
        """Save file on disk and return paths. Generate thumbnail if it's an image."""
        is_video = file.content_type.startswith("video/")
        self.validate_file(file, is_video=is_video)

        ext = file.filename.split(".")[-1].lower() if "." in file.filename else ("mp4" if is_video else "jpg")
        filename = f"{uuid4()}.{ext}"
        
        # Build path structure: uploads/{creator_id}/{booking_id or 'portfolio'}/{filename}
        sub_folder = booking_id if booking_id else "portfolio"
        relative_dir = Path(creator_id) / sub_folder
        dest_dir = Path(settings.UPLOAD_DIR) / relative_dir
        dest_dir.mkdir(parents=True, exist_ok=True)
        
        dest_path = dest_dir / filename
        
        # Save file asynchronously
        async with aiofiles.open(dest_path, "wb") as out_file:
            file.file.seek(0)
            while content := file.file.read(1024 * 1024):
                await out_file.write(content)
        
        file_url = f"/uploads/{creator_id}/{sub_folder}/{filename}"
        thumbnail_url = None
        
        # Generate thumbnail for images
        if not is_video:
            try:
                thumb_filename = f"thumb_{filename}.jpg"
                thumb_dir = dest_dir / "thumbs"
                thumb_dir.mkdir(parents=True, exist_ok=True)
                thumb_path = thumb_dir / thumb_filename
                
                with Image.open(dest_path) as img:
                    img.thumbnail((400, 400))
                    # Convert to RGB if PNG/WEBP has alpha transparency
                    if img.mode in ("RGBA", "P"):
                        img = img.convert("RGB")
                    img.save(thumb_path, "JPEG")
                thumbnail_url = f"/uploads/{creator_id}/{sub_folder}/thumbs/{thumb_filename}"
            except Exception:
                # Fallback to file_url if thumbnail generation fails
                thumbnail_url = file_url

        return file_url, thumbnail_url

upload_service = UploadService()
