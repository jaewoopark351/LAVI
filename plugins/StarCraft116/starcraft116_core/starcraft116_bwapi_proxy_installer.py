#20260718_kpopmodder: Added this module to keep BWAPI DLL proxy installation isolated.
import hashlib
import os
import shutil
from pathlib import Path

from core.logger import log_print


class StarCraft116BWAPIProxyInstaller:
    #20260718_kpopmodder: Installs the Monster BWAPI proxy beside the original BWAPI DLL.
    PROXY_DLL_NAME = "BWAPI.dll"
    REAL_DLL_NAME = "BWAPI_real.dll"
    DEFAULT_SOURCE_PATH = "plugins\\StarCraft116\\BWAPI.dll"

    def __init__(self, config_manager):
        self.config_manager = config_manager

    def ensure_installed(self, profile_name=None):
        if not self.config_manager.get_bool("bwapi_proxy_dll_auto_install", True):
            return self._result(True, "bwapi_proxy_dll_auto_install disabled.")

        source_path = self._source_path()
        if not source_path or not source_path.is_file():
            return self._result(
                False,
                f"BWAPI proxy DLL source does not exist: {source_path or ''}",
            )

        bwapi_data_dir = self._bwapi_data_dir(profile_name)
        if not bwapi_data_dir:
            return self._result(False, "BWAPI data directory is not configured.")
        if not bwapi_data_dir.is_dir():
            return self._result(
                False,
                f"BWAPI data directory does not exist: {bwapi_data_dir}",
            )

        if self.config_manager.get_bool("bwapi_proxy_dll_project_only", True):
            if not self._is_inside_project(bwapi_data_dir):
                return self._result(
                    True,
                    f"Skipped BWAPI proxy DLL install outside project: {bwapi_data_dir}",
                    skipped=True,
                )

        proxy_target = bwapi_data_dir / self.PROXY_DLL_NAME
        real_target = bwapi_data_dir / self.REAL_DLL_NAME
        if not proxy_target.is_file():
            return self._result(
                False,
                f"BWAPI.dll target does not exist: {proxy_target}",
            )

        try:
            source_hash = self._sha256(source_path)
            target_hash = self._sha256(proxy_target)
            if source_hash == target_hash:
                if real_target.is_file():
                    return self._result(
                        True,
                        f"BWAPI proxy DLL already installed: {proxy_target}",
                        skipped=True,
                    )
                return self._result(
                    False,
                    f"BWAPI proxy DLL is installed but BWAPI_real.dll is missing: {real_target}",
                )

            if not real_target.is_file() or self._sha256(real_target) == source_hash:
                shutil.copy2(proxy_target, real_target)

            shutil.copy2(source_path, proxy_target)
        except Exception as e:
            message = f"Failed to install BWAPI proxy DLL: {e}"
            log_print(f"[StarCraft116BWAPIProxyInstaller] {message}")
            return self._result(False, message)

        return self._result(
            True,
            f"Installed BWAPI proxy DLL: {proxy_target}",
            installed=True,
        )

    def _source_path(self):
        configured = str(
            self.config_manager.get(
                "bwapi_proxy_dll_source_path",
                self.DEFAULT_SOURCE_PATH,
            )
            or self.DEFAULT_SOURCE_PATH
        )
        return Path(self.config_manager.resolve_path_value(configured))

    def _bwapi_data_dir(self, profile_name):
        directory = self.config_manager.resolve_profile_bwapi_data_dir(profile_name)
        if not directory:
            return None
        return Path(directory)

    def _is_inside_project(self, path):
        try:
            project_root = Path(self.config_manager.project_root).resolve()
            resolved = path.resolve()
            return resolved == project_root or project_root in resolved.parents
        except Exception:
            return False

    @staticmethod
    def _sha256(path):
        digest = hashlib.sha256()
        with open(path, "rb") as file:
            for chunk in iter(lambda: file.read(1024 * 1024), b""):
                digest.update(chunk)
        return digest.hexdigest()

    @staticmethod
    def _result(ok, message, *, installed=False, skipped=False):
        return {
            "ok": bool(ok),
            "message": str(message or ""),
            "installed": bool(installed),
            "skipped": bool(skipped),
        }
