#20260718_kpopmodder: Added repo-local LC0 runtime bootstrap for the Chess plugin.
from __future__ import annotations

import os
from pathlib import Path
from typing import Any, Callable, Dict, Iterable, Optional
from urllib.parse import quote
import urllib.request

from core.logger import log_print


DEFAULT_LC0_RUNTIME_DIR = Path(
    "plugins",
    "Chess",
    "lc0-v0.32.1-windows-gpu-nvidia-cuda12",
)
DEFAULT_LC0_DOWNLOAD_REPO_ID = (
    "jaewoopark96/lc0-v0.32.1-windows-gpu-nvidia-cuda12"
)
DEFAULT_LC0_DOWNLOAD_REVISION = "main"
DEFAULT_LC0_DOWNLOAD_SUBDIR = "lc0-v0.32.1-windows-gpu-nvidia-cuda12"
DEFAULT_LC0_DOWNLOAD_FILES = (
    "791556.pb.gz",
    "BT4-1024x15x32h-swa-6147500-policytune-332.pb.gz",
    "COPYING",
    "CUDA.txt",
    "README.txt",
    "cublas64_12.dll",
    "cublasLt64_12.dll",
    "cudart64_12.dll",
    "lc0-training-client.exe",
    "lc0.exe",
    "mimalloc-LICENSE",
    "mimalloc-override.dll",
    "mimalloc-readme.md",
    "mimalloc-redirect.dll",
)
DEFAULT_LC0_REQUIRED_FILES = (
    "BT4-1024x15x32h-swa-6147500-policytune-332.pb.gz",
    "cublas64_12.dll",
    "cublasLt64_12.dll",
    "cudart64_12.dll",
    "lc0.exe",
    "mimalloc-override.dll",
    "mimalloc-redirect.dll",
)


class LC0RuntimeDownloader:
    #20260718_kpopmodder: Keeps LC0 binaries out of git while repairing the repo-local runtime on demand.
    def __init__(
        self,
        download_file_fn: Optional[Callable[..., Any]] = None,
    ):
        self._download_file_fn = download_file_fn or self._download_file

    def ensure_runtime(
        self,
        runtime_dir: str,
        *,
        enabled: bool = True,
        repo_id: str = DEFAULT_LC0_DOWNLOAD_REPO_ID,
        revision: str = DEFAULT_LC0_DOWNLOAD_REVISION,
        subdir: str = DEFAULT_LC0_DOWNLOAD_SUBDIR,
        files: Iterable[str] = DEFAULT_LC0_DOWNLOAD_FILES,
        required_files: Iterable[str] = DEFAULT_LC0_REQUIRED_FILES,
        timeout_sec: float = 120.0,
    ) -> Dict[str, Any]:
        runtime_text = str(runtime_dir or "").strip().strip("\"'")
        if not runtime_text:
            return {
                "ok": False,
                "downloaded": False,
                "error": "lc0_runtime_dir_missing",
                "runtime_dir": "",
            }
        target_dir = Path(runtime_text)
        target_dir = target_dir.resolve()
        required_files = tuple(required_files)
        existing_validation = self.validate_runtime_ready(target_dir, required_files)
        if existing_validation.get("ok"):
            return {
                "ok": True,
                "downloaded": False,
                "skipped": "runtime_present",
                "runtime_dir": str(target_dir),
                "validation": existing_validation,
            }
        if not enabled:
            return {
                "ok": False,
                "downloaded": False,
                "error": "lc0_runtime_download_disabled",
                "runtime_dir": str(target_dir),
                "validation": existing_validation,
            }

        downloaded_files = []
        try:
            target_dir.mkdir(parents=True, exist_ok=True)
            for relative_path in tuple(files):
                destination = self._safe_destination(target_dir, relative_path)
                if self._file_ready(destination):
                    continue
                url = self.build_huggingface_resolve_url(
                    repo_id=repo_id,
                    revision=revision,
                    subdir=subdir,
                    relative_path=relative_path,
                )
                log_print(
                    "[ChessLC0RuntimeDownloader] downloading "
                    f"{relative_path} from {url}"
                )
                self._download_file_fn(
                    url=url,
                    destination=str(destination),
                    timeout_sec=float(timeout_sec),
                )
                downloaded_files.append(str(relative_path))
        except Exception as e:
            message = str(e)
            log_print(f"[ChessLC0RuntimeDownloader] download failed: {message}")
            return {
                "ok": False,
                "downloaded": bool(downloaded_files),
                "error": "lc0_runtime_download_failed",
                "message": message,
                "runtime_dir": str(target_dir),
                "repo_id": repo_id,
                "revision": revision,
                "subdir": subdir,
                "downloaded_files": downloaded_files,
            }

        validation = self.validate_runtime_ready(target_dir, required_files)
        if not validation.get("ok"):
            return {
                "ok": False,
                "downloaded": bool(downloaded_files),
                "error": "lc0_runtime_incomplete",
                "runtime_dir": str(target_dir),
                "repo_id": repo_id,
                "revision": revision,
                "subdir": subdir,
                "downloaded_files": downloaded_files,
                "validation": validation,
            }

        result = {
            "ok": True,
            "downloaded": bool(downloaded_files),
            "runtime_dir": str(target_dir),
            "repo_id": repo_id,
            "revision": revision,
            "subdir": subdir,
            "downloaded_files": downloaded_files,
            "validation": validation,
        }
        if downloaded_files:
            log_print(f"[ChessLC0RuntimeDownloader] runtime restored: {result}")
        return result

    def validate_runtime_ready(
        self,
        runtime_dir: str | os.PathLike[str],
        required_files: Iterable[str] = DEFAULT_LC0_REQUIRED_FILES,
    ) -> Dict[str, Any]:
        target_dir = Path(runtime_dir).resolve()
        missing_files = []
        for relative_path in tuple(required_files):
            try:
                destination = self._safe_destination(target_dir, relative_path)
            except Exception:
                missing_files.append(str(relative_path))
                continue
            if not self._file_ready(destination):
                missing_files.append(os.path.normpath(str(relative_path)))
        return {
            "ok": target_dir.is_dir() and not missing_files,
            "runtime_dir": str(target_dir),
            "required_files": list(required_files),
            "missing_files": missing_files,
            "error": "lc0_runtime_incomplete" if missing_files else "",
        }

    def build_huggingface_resolve_url(
        self,
        *,
        repo_id: str,
        revision: str,
        subdir: str,
        relative_path: str,
    ) -> str:
        repo = quote(str(repo_id or DEFAULT_LC0_DOWNLOAD_REPO_ID).strip(), safe="/")
        rev = quote(str(revision or DEFAULT_LC0_DOWNLOAD_REVISION).strip(), safe="")
        remote_path = "/".join(
            segment
            for segment in (
                str(subdir or "").strip().replace("\\", "/").strip("/"),
                str(relative_path or "").strip().replace("\\", "/").strip("/"),
            )
            if segment
        )
        remote_path = "/".join(
            quote(segment, safe="")
            for segment in remote_path.split("/")
            if segment
        )
        return f"https://huggingface.co/{repo}/resolve/{rev}/{remote_path}?download=true"

    def _download_file(
        self,
        *,
        url: str,
        destination: str,
        timeout_sec: float,
    ) -> None:
        destination_path = Path(destination)
        destination_path.parent.mkdir(parents=True, exist_ok=True)
        temp_path = destination_path.with_name(destination_path.name + ".part")
        request = urllib.request.Request(
            url,
            headers={"User-Agent": "LAVI-Chess-LC0RuntimeDownloader/1.0"},
        )
        with (
            urllib.request.urlopen(request, timeout=float(timeout_sec)) as response,
            open(temp_path, "wb") as output,
        ):
            while True:
                chunk = response.read(1024 * 1024)
                if not chunk:
                    break
                output.write(chunk)
        if not self._file_ready(temp_path):
            raise RuntimeError(f"downloaded_empty_file: {destination_path.name}")
        os.replace(temp_path, destination_path)

    def _safe_destination(
        self,
        target_dir: Path,
        relative_path: str | os.PathLike[str],
    ) -> Path:
        text = str(relative_path or "").strip().strip("\"'")
        candidate = Path(text)
        if not text or candidate.is_absolute() or ".." in candidate.parts:
            raise RuntimeError(f"unsafe_lc0_runtime_path: {relative_path}")
        target_root = target_dir.resolve()
        destination = (target_root / candidate).resolve()
        if destination != target_root and not str(destination).startswith(
            str(target_root) + os.sep
        ):
            raise RuntimeError(f"unsafe_lc0_runtime_path: {relative_path}")
        return destination

    def _file_ready(self, path: Path) -> bool:
        try:
            return path.is_file() and path.stat().st_size > 0
        except OSError:
            return False
