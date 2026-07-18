#20260718_kpopmodder: Added this module to keep VAD model downloads separate from recording.
from __future__ import annotations

import hashlib
import os
from pathlib import Path
from typing import Any, Callable, Optional
import urllib.request

from core.logger import log_print


DOWNLOAD_CHUNK_SIZE_BYTES = 1024 * 1024


class VadModelDownloader:
    #20260718_kpopmodder: Repairs the repo-local Silero VAD model file on demand and verifies SHA-256.
    def __init__(
        self,
        download_open_fn: Optional[Callable[..., Any]] = None,
    ):
        self._download_open_fn = download_open_fn or urllib.request.urlopen

    def ensure_model(
        self,
        *,
        model_path: str | os.PathLike[str],
        url: str,
        expected_sha256: str,
        timeout_sec: float,
        project_root: str | os.PathLike[str],
        enabled: bool = True,
    ) -> dict[str, Any]:
        target = Path(model_path).resolve()
        root = Path(project_root).resolve()
        self._ensure_inside_project(target, root)

        if target.is_file() and target.stat().st_size > 0:
            actual_sha256 = self.sha256_file(target)
            return {
                "ok": self._sha_matches(actual_sha256, expected_sha256),
                "downloaded": False,
                "model_path": str(target),
                "sha256": actual_sha256,
                "expected_sha256": expected_sha256,
                "error": ""
                if self._sha_matches(actual_sha256, expected_sha256)
                else "vad_model_sha256_mismatch",
            }

        if not enabled:
            return {
                "ok": False,
                "downloaded": False,
                "model_path": str(target),
                "error": "vad_model_download_disabled",
            }

        try:
            target.parent.mkdir(parents=True, exist_ok=True)
            self._download(
                url=url,
                destination=target,
                expected_sha256=expected_sha256,
                timeout_sec=timeout_sec,
            )
        except Exception as e:
            message = str(e)
            log_print(
                "[VoiceInputVAD] model download failed: "
                f"path={target}, error={message}"
            )
            return {
                "ok": False,
                "downloaded": False,
                "model_path": str(target),
                "url": url,
                "error": "vad_model_download_failed",
                "message": message,
            }

        actual_sha256 = self.sha256_file(target)
        return {
            "ok": True,
            "downloaded": True,
            "model_path": str(target),
            "url": url,
            "sha256": actual_sha256,
            "expected_sha256": expected_sha256,
            "error": "",
        }

    def sha256_file(self, path: Path) -> str:
        digest = hashlib.sha256()
        with open(path, "rb") as file:
            while True:
                chunk = file.read(DOWNLOAD_CHUNK_SIZE_BYTES)
                if not chunk:
                    break
                digest.update(chunk)
        return digest.hexdigest()

    def _download(
        self,
        *,
        url: str,
        destination: Path,
        expected_sha256: str,
        timeout_sec: float,
    ) -> None:
        temp_path = destination.with_name(destination.name + ".part")
        request = urllib.request.Request(
            str(url),
            headers={"User-Agent": "LAVI-VoiceInput-SileroVAD/1.0"},
        )
        log_print(f"[VoiceInputVAD] downloading model: {url} -> {destination}")
        with (
            self._download_open_fn(request, timeout=float(timeout_sec)) as response,
            open(temp_path, "wb") as output,
        ):
            while True:
                chunk = response.read(DOWNLOAD_CHUNK_SIZE_BYTES)
                if not chunk:
                    break
                output.write(chunk)

        if not temp_path.is_file() or temp_path.stat().st_size <= 0:
            self._remove_temp(temp_path)
            raise RuntimeError(f"downloaded_empty_vad_model: {destination.name}")

        actual_sha256 = self.sha256_file(temp_path)
        if not self._sha_matches(actual_sha256, expected_sha256):
            self._remove_temp(temp_path)
            raise RuntimeError(
                "vad_model_sha256_mismatch: "
                f"expected={expected_sha256}, actual={actual_sha256}"
            )

        os.replace(temp_path, destination)
        log_print(
            "[VoiceInputVAD] model download complete: "
            f"path={destination}, sha256={actual_sha256}"
        )

    def _ensure_inside_project(self, target: Path, root: Path) -> None:
        if target == root:
            return
        try:
            target.relative_to(root)
        except ValueError as e:
            raise RuntimeError(f"vad_model_path_outside_project: {target}") from e

    def _sha_matches(self, actual: str, expected: str) -> bool:
        return str(actual or "").lower() == str(expected or "").lower()

    def _remove_temp(self, path: Path) -> None:
        try:
            if path.exists():
                path.unlink()
        except OSError:
            pass
