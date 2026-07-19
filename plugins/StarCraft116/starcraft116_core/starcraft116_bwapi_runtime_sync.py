#20260719_kpopmodder: Added this module to keep profile-specific BWAPI DLLs in sync before launch.
import hashlib
import os
import shutil
from pathlib import Path

from core.logger import log_print


class StarCraft116BWAPIRuntimeSync:
    #20260719_kpopmodder: Monster needs BWAPI 4.2.0/proxy, while Stardust-family DLL bots need BWAPI 4.4.0.
    BASE_DLL_NAMES = ("BWAPI.dll", "BWAPId.dll")
    MONSTER_EXTRA_FILE_NAMES = ("BWAPI_real.dll", "bwapi.ini")
    BACKUP_SUFFIX = ".lav_runtime_sync_backup"

    def __init__(self, config_manager):
        self.config_manager = config_manager

    def sync(self, profile_name=None):
        if not self._get_bool("bwapi_runtime_dll_sync_enabled", True):
            return self._result(True, "bwapi_runtime_dll_sync_enabled disabled.", skipped=True)

        source_dir = self._source_bwapi_data_dir(profile_name)
        target_dir = self._target_bwapi_data_dir(profile_name)
        if not source_dir or not target_dir:
            return self._result(True, "BWAPI runtime DLL sync skipped: source or target is not configured.", skipped=True)

        if self._same_path(source_dir, target_dir):
            return self._result(True, f"BWAPI runtime DLLs already use profile source: {target_dir}", skipped=True)

        if not source_dir.is_dir():
            return self._result(False, f"BWAPI runtime DLL source does not exist: {source_dir}")
        if not target_dir.is_dir():
            return self._result(False, f"BWAPI runtime DLL target does not exist: {target_dir}")

        missing_required = [
            name for name in self._required_file_names(profile_name)
            if not (source_dir / name).is_file()
        ]
        if missing_required:
            return self._result(
                False,
                "BWAPI runtime source is missing required files: "
                + ", ".join(missing_required)
                + f" in {source_dir}",
            )

        copied = []
        skipped = []
        try:
            for name in self._file_names(profile_name):
                source = source_dir / name
                if not source.is_file():
                    continue
                target = target_dir / name
                if target.is_file() and self._same_file_hash(source, target):
                    skipped.append(name)
                    continue
                self._backup_once(target)
                shutil.copy2(source, target)
                copied.append(name)
            if self._is_monster_profile(profile_name):
                if self._normalize_monster_bwapi_ini(target_dir / "bwapi.ini"):
                    if "bwapi.ini" in skipped:
                        skipped.remove("bwapi.ini")
                    if "bwapi.ini" not in copied:
                        copied.append("bwapi.ini")
        except Exception as e:
            message = f"Failed to sync BWAPI runtime DLLs: {e}"
            log_print(f"[StarCraft116BWAPIRuntimeSync] {message}")
            return self._result(False, message)

        if copied:
            return self._result(
                True,
                (
                    "Synced BWAPI runtime files for "
                    f"{profile_name or self._active_profile_name()}: "
                    + ", ".join(copied)
                    + f" -> {target_dir}"
                ),
                synced=True,
            )
        return self._result(
            True,
            (
                "BWAPI runtime files already match "
                f"{profile_name or self._active_profile_name()}: "
                + ", ".join(skipped)
            ),
            skipped=True,
        )

    def _source_bwapi_data_dir(self, profile_name):
        profile_name = profile_name or self._active_profile_name()

        starcraft_dir = self._call_resolver("resolve_profile_bwapi_starcraft_dir", profile_name)
        if starcraft_dir:
            candidate = Path(starcraft_dir) / "bwapi-data"
            if (candidate / "BWAPI.dll").is_file():
                return candidate

        bundle_dir = self._call_resolver("resolve_profile_bwapi_bundle_dir", profile_name)
        if bundle_dir:
            candidate = Path(bundle_dir) / "Starcraft" / "bwapi-data"
            if (candidate / "BWAPI.dll").is_file():
                return candidate

        bwapi_data_dir = self._call_resolver("resolve_profile_bwapi_data_dir", profile_name)
        if bwapi_data_dir:
            return Path(bwapi_data_dir)
        return None

    def _target_bwapi_data_dir(self, profile_name):
        profile_name = profile_name or self._active_profile_name()
        runtime_dir = self._call_resolver("resolve_profile_runtime_bwapi_data_dir", profile_name)
        if runtime_dir:
            return Path(runtime_dir)
        bwapi_data_dir = self._call_resolver("resolve_profile_bwapi_data_dir", profile_name)
        if bwapi_data_dir:
            return Path(bwapi_data_dir)
        return None

    def _file_names(self, profile_name):
        names = list(self.BASE_DLL_NAMES)
        if self._is_monster_profile(profile_name):
            names.extend(self.MONSTER_EXTRA_FILE_NAMES)
        return tuple(names)

    def _required_file_names(self, profile_name):
        if self._is_monster_profile(profile_name):
            return ("BWAPI.dll", "BWAPI_real.dll", "bwapi.ini")
        return ("BWAPI.dll",)

    def _is_monster_profile(self, profile_name):
        return str(profile_name or self._active_profile_name() or "").strip().lower() == "monster"

    def _active_profile_name(self):
        getter = getattr(self.config_manager, "get_active_profile_name", None)
        if callable(getter):
            return getter()
        return ""

    def _call_resolver(self, name, profile_name):
        resolver = getattr(self.config_manager, name, None)
        if not callable(resolver):
            return ""
        return str(resolver(profile_name) or "").strip()

    def _get_bool(self, key, default=False):
        getter = getattr(self.config_manager, "get_bool", None)
        if callable(getter):
            return getter(key, default)
        return default

    def _backup_once(self, target):
        if not target.is_file():
            return
        if not self._get_bool("bwapi_runtime_dll_backup_enabled", True):
            return
        backup = target.with_name(target.name + self.BACKUP_SUFFIX)
        if backup.exists():
            return
        shutil.copy2(target, backup)

    def _normalize_monster_bwapi_ini(self, path):
        if not path.is_file():
            return False
        original_text = path.read_text(encoding="utf-8", errors="ignore")
        lines = original_text.splitlines()
        changed = False
        normalized_lines = []
        for line in lines:
            stripped = line.lstrip()
            prefix = line[:len(line) - len(stripped)]
            key = stripped.split("=", 1)[0].strip().lower()
            if key == "ai":
                normalized_lines.append(f"{prefix}ai     =")
                changed = changed or stripped.strip() != "ai     ="
            elif key == "ai_dbg":
                normalized_lines.append(f"{prefix}ai_dbg =")
                changed = changed or stripped.strip() != "ai_dbg ="
            else:
                normalized_lines.append(line)
        if not changed:
            return False
        path.write_text("\n".join(normalized_lines) + "\n", encoding="utf-8")
        return True

    @classmethod
    def _same_path(cls, first, second):
        try:
            return first.resolve() == second.resolve()
        except Exception:
            return os.path.normcase(os.path.normpath(str(first))) == os.path.normcase(os.path.normpath(str(second)))

    @classmethod
    def _same_file_hash(cls, first, second):
        return cls._sha256(first) == cls._sha256(second)

    @staticmethod
    def _sha256(path):
        digest = hashlib.sha256()
        with open(path, "rb") as file:
            for chunk in iter(lambda: file.read(1024 * 1024), b""):
                digest.update(chunk)
        return digest.hexdigest()

    @staticmethod
    def _result(ok, message, *, synced=False, skipped=False):
        return {
            "ok": bool(ok),
            "message": str(message or ""),
            "synced": bool(synced),
            "skipped": bool(skipped),
        }
