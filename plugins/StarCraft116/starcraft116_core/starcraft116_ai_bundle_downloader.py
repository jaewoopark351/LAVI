#20260718_kpopmodder: Added repo-local StarCraft 1.16 AI bundle bootstrap.
from __future__ import annotations

import os
import shutil
from pathlib import Path
from typing import Any, Callable, Dict, Iterable, Optional

from core.logger import log_print


DEFAULT_STARCRAFT116_AI_BUNDLE_DIR = Path(
    "plugins",
    "StarCraft116",
    "StarCraft_1_16_Bots",
)
DEFAULT_STARCRAFT116_AI_BUNDLE_REPO_ID = "jaewoopark96/StarCraft_1_16_Bots"
DEFAULT_STARCRAFT116_AI_BUNDLE_REPO_TYPE = "model"
DEFAULT_STARCRAFT116_AI_BUNDLE_REVISION = "main"
DEFAULT_STARCRAFT116_AI_BUNDLE_REMOTE_SUBDIR = ""
DEFAULT_STARCRAFT116_AI_REQUIRED_FILES = (
    "Crona/BananaBrain.dll",
    "Monster/Monster.exe",
    "Monster/run_monster_robust_log.bat",
    "Stardust/bwapi-data/AI/Stardust.dll",
    "Terminus/BananaBrain.dll",
)
DEFAULT_STARCRAFT116_MONSTER_LAUNCHER_PATH = "Monster/run_monster_robust_log.bat"
DEFAULT_STARCRAFT116_MONSTER_LAUNCHER_TEMPLATE = Path(
    "tools",
    "run_monster_robust_log.example.bat",
)
DEFAULT_STARCRAFT116_MONSTER_LAUNCHER_MARKER = (
    "bwapi_Code\\bwapi-4.2.0\\Release_Binary\\Starcraft"
)


class StarCraft116AIBundleDownloader:
    #20260718_kpopmodder: Keeps StarCraft 1.16 bot binaries out of git while restoring them on demand.
    def __init__(self, download_fn: Optional[Callable[..., Any]] = None):
        self._download_fn = download_fn or self._snapshot_download

    def ensure_bundle(
        self,
        bundle_dir: str | os.PathLike[str],
        *,
        enabled: bool = True,
        repo_id: str = DEFAULT_STARCRAFT116_AI_BUNDLE_REPO_ID,
        repo_type: str = DEFAULT_STARCRAFT116_AI_BUNDLE_REPO_TYPE,
        revision: str = DEFAULT_STARCRAFT116_AI_BUNDLE_REVISION,
        remote_subdir: str = DEFAULT_STARCRAFT116_AI_BUNDLE_REMOTE_SUBDIR,
        required_files: Iterable[str] = DEFAULT_STARCRAFT116_AI_REQUIRED_FILES,
    ) -> Dict[str, Any]:
        target_dir = self._target_dir(bundle_dir)
        if target_dir is None:
            return {
                "ok": False,
                "downloaded": False,
                "error": "starcraft116_ai_bundle_dir_missing",
                "bundle_dir": "",
            }

        required_files = tuple(required_files)
        existing_validation = self.validate_bundle_ready(target_dir, required_files)
        if existing_validation.get("ok"):
            repair_result = self._repair_monster_launcher(target_dir)
            if not repair_result.get("ok"):
                return self._launcher_repair_failed(target_dir, repair_result)
            existing_validation = self.validate_bundle_ready(target_dir, required_files)
            return {
                "ok": True,
                "downloaded": False,
                "skipped": "ai_bundle_present",
                "bundle_dir": str(target_dir),
                "validation": existing_validation,
                "repair": repair_result,
            }

        if not enabled:
            return {
                "ok": False,
                "downloaded": False,
                "error": "starcraft116_ai_bundle_download_disabled",
                "bundle_dir": str(target_dir),
                "validation": existing_validation,
            }

        try:
            download_remote_subdir = str(
                DEFAULT_STARCRAFT116_AI_BUNDLE_REMOTE_SUBDIR
                if remote_subdir is None
                else remote_subdir
            )
            download_remote_subdir = (
                download_remote_subdir.strip().replace("\\", "/").strip("/")
            )
            download_local_dir = target_dir.parent if download_remote_subdir else target_dir
            target_dir.parent.mkdir(parents=True, exist_ok=True)
            snapshot_path = self._download(
                repo_id=str(repo_id or DEFAULT_STARCRAFT116_AI_BUNDLE_REPO_ID),
                repo_type=str(repo_type or DEFAULT_STARCRAFT116_AI_BUNDLE_REPO_TYPE),
                revision=str(revision or DEFAULT_STARCRAFT116_AI_BUNDLE_REVISION),
                remote_subdir=download_remote_subdir,
                local_dir=str(download_local_dir),
            )
        except Exception as e:
            message = str(e)
            log_print(f"[StarCraft116AIBundleDownloader] download failed: {message}")
            return {
                "ok": False,
                "downloaded": False,
                "error": "starcraft116_ai_bundle_download_failed",
                "message": message,
                "bundle_dir": str(target_dir),
                "repo_id": repo_id,
                "revision": revision,
                "remote_subdir": remote_subdir,
            }

        repair_result = self._repair_monster_launcher(target_dir)
        if not repair_result.get("ok"):
            return self._launcher_repair_failed(target_dir, repair_result)

        validation = self.validate_bundle_ready(target_dir, required_files)
        if not validation.get("ok"):
            return {
                "ok": False,
                "downloaded": True,
                "error": "starcraft116_ai_bundle_incomplete",
                "bundle_dir": str(target_dir),
                "repo_id": repo_id,
                "revision": revision,
                "remote_subdir": remote_subdir,
                "snapshot_path": str(snapshot_path or ""),
                "validation": validation,
                "repair": repair_result,
            }

        result = {
            "ok": True,
            "downloaded": True,
            "bundle_dir": str(target_dir),
            "repo_id": repo_id,
            "revision": revision,
            "remote_subdir": remote_subdir,
            "snapshot_path": str(snapshot_path or ""),
            "validation": validation,
            "repair": repair_result,
        }
        log_print(f"[StarCraft116AIBundleDownloader] AI bundle restored: {result}")
        return result

    def validate_bundle_ready(
        self,
        bundle_dir: str | os.PathLike[str],
        required_files: Iterable[str] = DEFAULT_STARCRAFT116_AI_REQUIRED_FILES,
    ) -> Dict[str, Any]:
        target_dir = Path(bundle_dir).resolve()
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
            "bundle_dir": str(target_dir),
            "required_files": list(required_files),
            "missing_files": missing_files,
            "error": "starcraft116_ai_bundle_incomplete" if missing_files else "",
        }

    def _download(
        self,
        *,
        repo_id: str,
        repo_type: str,
        revision: str,
        remote_subdir: str,
        local_dir: str,
    ) -> Any:
        remote = str(remote_subdir or "").strip().replace("\\", "/").strip("/")
        allow_patterns = [f"{remote}/**"] if remote else None
        return self._download_fn(
            repo_id=repo_id,
            repo_type=repo_type,
            revision=revision,
            local_dir=local_dir,
            local_files_only=False,
            allow_patterns=allow_patterns,
            ignore_patterns=[".git", ".git/*"],
            max_workers=4,
        )

    def _snapshot_download(self, **kwargs: Any) -> Any:
        try:
            from huggingface_hub import snapshot_download
        except Exception as e:
            raise RuntimeError(f"huggingface_hub_unavailable: {e}") from e
        return snapshot_download(**kwargs)

    def _target_dir(
        self,
        bundle_dir: str | os.PathLike[str],
    ) -> Optional[Path]:
        text = str(bundle_dir or "").strip().strip("\"'")
        if not text:
            return None
        return Path(text).resolve()

    def _safe_destination(
        self,
        target_dir: Path,
        relative_path: str | os.PathLike[str],
    ) -> Path:
        text = str(relative_path or "").strip().strip("\"'")
        candidate = Path(text)
        if not text or candidate.is_absolute() or ".." in candidate.parts:
            raise RuntimeError(f"unsafe_starcraft116_ai_bundle_path: {relative_path}")
        target_root = target_dir.resolve()
        destination = (target_root / candidate).resolve()
        if destination != target_root and not str(destination).startswith(
            str(target_root) + os.sep
        ):
            raise RuntimeError(f"unsafe_starcraft116_ai_bundle_path: {relative_path}")
        return destination

    def _file_ready(self, path: Path) -> bool:
        try:
            return path.is_file() and path.stat().st_size > 0
        except OSError:
            return False

    def _repair_monster_launcher(self, target_dir: Path) -> Dict[str, Any]:
        #20260718_kpopmodder: Keep downloaded Monster launcher aligned with project-local BWAPI code release.
        destination = self._safe_destination(
            target_dir,
            DEFAULT_STARCRAFT116_MONSTER_LAUNCHER_PATH,
        )
        template = target_dir.parent / DEFAULT_STARCRAFT116_MONSTER_LAUNCHER_TEMPLATE
        if not template.is_file():
            return {
                "ok": True,
                "repaired": False,
                "skipped": "monster_launcher_template_missing",
                "template": str(template),
                "destination": str(destination),
            }

        try:
            if destination.is_file():
                current = destination.read_text(encoding="utf-8", errors="ignore")
                if DEFAULT_STARCRAFT116_MONSTER_LAUNCHER_MARKER in current:
                    return {
                        "ok": True,
                        "repaired": False,
                        "skipped": "monster_launcher_already_current",
                        "template": str(template),
                        "destination": str(destination),
                    }
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(template, destination)
        except Exception as e:
            message = str(e)
            log_print(
                "[StarCraft116AIBundleDownloader] Monster launcher repair failed: "
                f"{message}"
            )
            return {
                "ok": False,
                "repaired": False,
                "error": "starcraft116_monster_launcher_repair_failed",
                "message": message,
                "template": str(template),
                "destination": str(destination),
            }

        return {
            "ok": True,
            "repaired": True,
            "template": str(template),
            "destination": str(destination),
        }

    def _launcher_repair_failed(
        self,
        target_dir: Path,
        repair_result: Dict[str, Any],
    ) -> Dict[str, Any]:
        return {
            "ok": False,
            "downloaded": False,
            "error": repair_result.get(
                "error",
                "starcraft116_monster_launcher_repair_failed",
            ),
            "bundle_dir": str(target_dir),
            "repair": repair_result,
        }
