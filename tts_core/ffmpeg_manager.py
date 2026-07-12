import os
import shutil
import zipfile

import requests
from tqdm import tqdm

from core.logger import log_print


FFMPEG_FILE_NAME = "ffmpeg-release-essentials.zip"
FFMPEG_DOWNLOAD_URL = (
    "https://www.gyan.dev/ffmpeg/builds/packages/"
    "ffmpeg-6.1.1-essentials_build.zip"
)
FFMPEG_EXTRACTED_DIR_NAME = "ffmpeg-6.1.1-essentials_build"


def ensure_ffmpeg_exists(base_dir=None):#20260616_kpopmodder
    if base_dir is None:
        base_dir = os.path.dirname(os.path.dirname(__file__))#20260617_kpopmodder

    ffmpeg_path = os.path.join(base_dir, "ffmpeg.exe")
    ffprobe_path = os.path.join(base_dir, "ffprobe.exe")

    if os.path.exists(ffmpeg_path) and os.path.exists(ffprobe_path):
        return True

    zip_path = os.path.join(base_dir, FFMPEG_FILE_NAME)

    if not download_ffmpeg(zip_path):
        return False

    if not extract_ffmpeg(zip_path, base_dir):
        return False

    return move_ffmpeg_binaries(base_dir, zip_path)


def download_ffmpeg(zip_path):
    log_print(f"Downloading {FFMPEG_FILE_NAME} from {FFMPEG_DOWNLOAD_URL}...")

    try:
        response = requests.get(
            FFMPEG_DOWNLOAD_URL,
            stream=True,
            timeout=(5, 60),
        )

    except Exception as e:
        log_print(f"ffmpeg download request failed: {e}")
        return False

    if response.status_code != 200:
        log_print(f"ffmpeg download failed. status_code={response.status_code}")
        return False

    total_size_in_bytes = int(response.headers.get("content-length", 0))
    block_size = 1024

    progress_bar = tqdm(
        total=total_size_in_bytes,
        unit="iB",
        unit_scale=True,
    )

    try:
        with open(zip_path, "wb") as file:
            for data in response.iter_content(block_size):
                if not data:
                    continue

                progress_bar.update(len(data))
                file.write(data)

    except Exception as e:
        log_print(f"ffmpeg download write failed: {e}")
        return False

    finally:
        progress_bar.close()

    if total_size_in_bytes != 0 and progress_bar.n != total_size_in_bytes:
        log_print("ERROR, something went wrong during ffmpeg download")
        return False

    log_print(f"{FFMPEG_FILE_NAME} downloaded successfully.")
    return True


def extract_ffmpeg(zip_path, base_dir):
    log_print(f"Extracting {FFMPEG_FILE_NAME}...")

    try:
        with zipfile.ZipFile(zip_path, "r") as zip_ref:
            zip_ref.extractall(base_dir)

    except Exception as e:
        log_print(f"ffmpeg extract failed: {e}")
        return False

    log_print(f"{FFMPEG_FILE_NAME} extracted successfully.")
    return True


def move_ffmpeg_binaries(base_dir, zip_path):
    extracted_dir = os.path.join(base_dir, FFMPEG_EXTRACTED_DIR_NAME)

    ffmpeg_exe_path = os.path.join(
        extracted_dir,
        "bin",
        "ffmpeg.exe",
    )

    ffprobe_exe_path = os.path.join(
        extracted_dir,
        "bin",
        "ffprobe.exe",
    )

    target_ffmpeg_path = os.path.join(base_dir, "ffmpeg.exe")
    target_ffprobe_path = os.path.join(base_dir, "ffprobe.exe")

    try:
        if not os.path.exists(ffmpeg_exe_path):
            log_print(f"ffmpeg.exe not found in extracted folder: {ffmpeg_exe_path}")
            return False

        if not os.path.exists(ffprobe_exe_path):
            log_print(f"ffprobe.exe not found in extracted folder: {ffprobe_exe_path}")
            return False

        if os.path.exists(target_ffmpeg_path):
            os.remove(target_ffmpeg_path)

        if os.path.exists(target_ffprobe_path):
            os.remove(target_ffprobe_path)

        shutil.move(ffmpeg_exe_path, target_ffmpeg_path)
        shutil.move(ffprobe_exe_path, target_ffprobe_path)

        shutil.rmtree(extracted_dir)

        if os.path.exists(zip_path):
            os.remove(zip_path)

    except Exception as e:
        log_print(f"ffmpeg binary move failed: {e}")
        return False

    log_print("ffmpeg.exe and ffprobe.exe are ready.")
    return True