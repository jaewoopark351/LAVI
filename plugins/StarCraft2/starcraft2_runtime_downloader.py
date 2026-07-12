#20260712_kpopmodder: Added repo-local runtime bootstrap for StarCraft2 Local Match.
from __future__ import annotations

import os
from typing import Any, Callable, Dict, Optional
import zipfile

from core.logger import log_print


DEFAULT_RUNTIME_REPO_ID = "jaewoopark96/plugins_StarCraft2_runtime"
DEFAULT_RUNTIME_REPO_TYPE = "model"
DEFAULT_RUNTIME_REVISION = "main"
DEFAULT_RUNTIME_REQUIRED_FILES = (
    "HumanLadder.json",
    "PlayerIds",
    os.path.join("Bots", "BenBotBC", "BenBotBC.jar"),
    os.path.join("Bots", "changeling", "changeling.exe"),
    os.path.join("Bots", "changeling", "config.yml"),
    os.path.join("Bots", "changeling", "ladderbots.json"),
    os.path.join("Bots", "sharkbot", "sharkbot.exe"),
    os.path.join("Bots", "sharkbot", "ladderbots.json"),
    os.path.join("jre", "bin", "java.exe"),
)

_PLACEHOLDER_NAMES = {
    ".cache",
    ".gitattributes",
    ".gitignore",
    ".gitkeep",
    "README.md",
    "README.txt",
}


class StarCraft2RuntimeDownloader:
    #20260712_kpopmodder: Keeps large bot/JRE runtime files out of git while
    # allowing a fresh checkout to repair the repo-local runtime folder on demand.
    def __init__(self, download_fn: Optional[Callable[..., Any]] = None):
        self._download_fn = download_fn

    def ensure_runtime(
        self,
        runtime_dir: str,
        *,
        enabled: bool = True,
        repo_id: str = DEFAULT_RUNTIME_REPO_ID,
        repo_type: str = DEFAULT_RUNTIME_REPO_TYPE,
        revision: str = DEFAULT_RUNTIME_REVISION,
        local_archive_path: str = "",
    ) -> Dict[str, Any]:
        target_dir = os.path.normpath(str(runtime_dir or "").strip().strip("\"'"))
        if not target_dir:
            return {
                "ok": False,
                "downloaded": False,
                "error": "starcraft2_runtime_dir_missing",
                "runtime_dir": "",
            }
        if not enabled:
            return {
                "ok": True,
                "downloaded": False,
                "skipped": "runtime_download_disabled",
                "runtime_dir": target_dir,
            }
        existing_validation = self.validate_runtime_ready(target_dir)
        if existing_validation.get("ok"):
            return {
                "ok": True,
                "downloaded": False,
                "skipped": "runtime_present",
                "runtime_dir": target_dir,
                "validation": existing_validation,
            }

        archive_path = os.path.normpath(
            str(local_archive_path or "").strip().strip("\"'")
        )
        if archive_path and os.path.isfile(archive_path):
            try:
                os.makedirs(target_dir, exist_ok=True)
                self._extract_archive(archive_path, target_dir)
                validation = self.validate_runtime_ready(target_dir)
                if validation.get("ok"):
                    result = {
                        "ok": True,
                        "downloaded": True,
                        "source": "local_archive",
                        "runtime_dir": target_dir,
                        "archive_path": archive_path,
                        "validation": validation,
                    }
                    log_print(
                        f"[StarCraft2RuntimeDownloader] runtime restored from archive: {result}"
                    )
                    return result
                log_print(
                    "[StarCraft2RuntimeDownloader] local archive incomplete: "
                    f"{validation}"
                )
            except Exception as e:
                log_print(f"[StarCraft2RuntimeDownloader] archive restore failed: {e}")

        try:
            os.makedirs(target_dir, exist_ok=True)
            snapshot_path = self._download(
                repo_id=str(repo_id or DEFAULT_RUNTIME_REPO_ID),
                repo_type=str(repo_type or DEFAULT_RUNTIME_REPO_TYPE),
                revision=str(revision or DEFAULT_RUNTIME_REVISION),
                local_dir=target_dir,
            )
        except Exception as e:
            message = str(e)
            log_print(f"[StarCraft2RuntimeDownloader] download failed: {message}")
            return {
                "ok": False,
                "downloaded": False,
                "error": "starcraft2_runtime_download_failed",
                "message": message,
                "runtime_dir": target_dir,
                "repo_id": repo_id,
                "revision": revision,
            }

        validation = self.validate_runtime_ready(target_dir)
        if not validation.get("ok"):
            return {
                "ok": False,
                "downloaded": True,
                "error": "starcraft2_runtime_incomplete",
                "runtime_dir": target_dir,
                "repo_id": repo_id,
                "revision": revision,
                "snapshot_path": str(snapshot_path or ""),
                "validation": validation,
            }

        result = {
            "ok": True,
            "downloaded": True,
            "runtime_dir": target_dir,
            "repo_id": repo_id,
            "revision": revision,
            "snapshot_path": str(snapshot_path or ""),
            "validation": validation,
        }
        log_print(f"[StarCraft2RuntimeDownloader] runtime restored: {result}")
        return result

    def validate_runtime_ready(
        self,
        runtime_dir: str,
        required_files: tuple[str, ...] = DEFAULT_RUNTIME_REQUIRED_FILES,
    ) -> Dict[str, Any]:
        target_dir = os.path.normpath(str(runtime_dir or "").strip().strip("\"'"))
        missing_files = []
        for relative_path in required_files:
            normalized_relative = os.path.normpath(str(relative_path or "").strip())
            if not normalized_relative:
                continue
            path = os.path.join(target_dir, normalized_relative)
            if not os.path.isfile(path):
                missing_files.append(normalized_relative)
        return {
            "ok": bool(target_dir) and os.path.isdir(target_dir) and not missing_files,
            "runtime_dir": target_dir,
            "required_files": list(required_files),
            "missing_files": missing_files,
            "error": "starcraft2_runtime_incomplete" if missing_files else "",
        }

    def _extract_archive(self, archive_path: str, target_dir: str) -> None:
        target_root = os.path.abspath(os.path.normpath(target_dir))
        with zipfile.ZipFile(archive_path) as archive:
            for member in archive.infolist():
                relative_name = os.path.normpath(member.filename)
                if not relative_name or relative_name.startswith(".."):
                    raise RuntimeError(f"unsafe_runtime_archive_member: {member.filename}")
                destination = os.path.abspath(os.path.join(target_root, relative_name))
                if destination != target_root and not destination.startswith(target_root + os.sep):
                    raise RuntimeError(f"unsafe_runtime_archive_member: {member.filename}")
                if member.is_dir():
                    os.makedirs(destination, exist_ok=True)
                    continue
                os.makedirs(os.path.dirname(destination), exist_ok=True)
                with archive.open(member, "r") as source, open(destination, "wb") as target:
                    while True:
                        chunk = source.read(1024 * 1024)
                        if not chunk:
                            break
                        target.write(chunk)

    def is_runtime_empty(self, runtime_dir: str) -> bool:
        target_dir = os.path.normpath(str(runtime_dir or "").strip().strip("\"'"))
        if not target_dir or not os.path.isdir(target_dir):
            return True
        try:
            entries = os.listdir(target_dir)
        except OSError:
            return False
        meaningful_entries = [
            name
            for name in entries
            if str(name or "").strip() and name not in _PLACEHOLDER_NAMES
        ]
        return not meaningful_entries

    def _download(
        self,
        *,
        repo_id: str,
        repo_type: str,
        revision: str,
        local_dir: str,
    ) -> Any:
        download_fn = self._download_fn or self._snapshot_download
        return download_fn(
            repo_id=repo_id,
            repo_type=repo_type,
            revision=revision,
            local_dir=local_dir,
            local_files_only=False,
            ignore_patterns=[".git", ".git/*"],
            max_workers=4,
        )

    def _snapshot_download(self, **kwargs: Any) -> Any:
        try:
            from huggingface_hub import snapshot_download
        except Exception as e:
            raise RuntimeError(f"huggingface_hub_unavailable: {e}") from e
        return snapshot_download(**kwargs)
