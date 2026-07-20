#20260713_kpopmodder: Extract match config builders and runtime-ensure logic into helper class.

from __future__ import annotations

import os
from typing import Any, Dict, Optional

from plugins.StarCraft2.bot_launch_profiles import get_bot_launch_profile
from .starcraft2_runtime_downloader import (
    DEFAULT_RUNTIME_REPO_ID,
    DEFAULT_RUNTIME_REPO_TYPE,
    DEFAULT_RUNTIME_REVISION,
)


class _StarCraft2MatchConfigService:
    def __init__(self, config_manager, plugin_root: str, runtime_downloader, arg_utils):
        self.config_manager = config_manager
        self.plugin_root = plugin_root
        self.runtime_downloader = runtime_downloader
        self.arg_utils = arg_utils

    def ladder_proxy_config(
        self,
        executable_path=None,
        working_directory=None,
        args=None,
        proxy_host=None,
        proxy_ports=None,
    ):
        ladder_config = self.config_manager.get_section("ladder_proxy")
        config = dict(ladder_config)
        if executable_path is not None:
            config["executable_path"] = str(
                executable_path or ladder_config.get("executable_path", "")
            )
        if working_directory is not None:
            config["working_directory"] = str(
                working_directory or ladder_config.get("working_directory", "")
            )
        if args is not None:
            config["args"] = args if isinstance(args, list) else str(args or "")
        if proxy_host is not None:
            config["proxy_host"] = str(proxy_host or "")
        if proxy_ports is not None and str(proxy_ports or "").strip():
            config["ports"] = proxy_ports
        config["executable_path"] = self.config_manager.resolve_path_value(
            config.get("executable_path", "")
        )
        config["working_directory"] = self.config_manager.resolve_path_value(
            config.get("working_directory", "")
        )
        for field in ("starcraft2_exe_path", "starcraft2_support64_path", "starcraft2_base_path"):
            config[field] = self.config_manager.resolve_path_value(config.get(field, ""))
        self._refresh_starcraft2_runtime_paths(config)
        return config

    def local_match_config(
        self,
        executable_path=None,
        working_directory=None,
        args=None,
        proxy_ports=None,
        bot_name: Optional[str] = None,
        bot_display_name: Optional[str] = None,
        keep_local_match_identity_args: bool = False,
    ) -> Dict[str, Any]:
        #20260711_kpopmodder: Local Match intentionally no longer reads the LAN
        # Lobby launcher defaults; this protects local play while remote-human
        # native code was archived in the LAN-only native copy.
        ladder_config = self.config_manager.get_section("ladder_proxy")
        local_config = self.config_manager.get_section("local_match")
        config = dict(ladder_config)
        config.update(local_config)
        if executable_path is not None:
            config["executable_path"] = str(
                executable_path
                or local_config.get("executable_path", "")
                or ladder_config.get("executable_path", "")
            )
        config["executable_path"] = self.config_manager.resolve_path_value(
            config.get("executable_path", "")
        )
        if working_directory is not None:
            config["working_directory"] = str(
                working_directory
                or local_config.get("working_directory", "")
                or ladder_config.get("working_directory", "")
            )
        config["working_directory"] = self.config_manager.resolve_path_value(
            config.get("working_directory", "")
        )
        if args is not None:
            config["args"] = args if isinstance(args, list) else str(args or "")
        if proxy_ports is not None and str(proxy_ports or "").strip():
            config["ports"] = proxy_ports
        if bot_display_name is not None:
            config["bot_display_name"] = str(bot_display_name or "").strip()
        normalized_args = self.arg_utils.normalize_ladder_args(config.get("args", []))
        config["args"] = (
            normalized_args
            if keep_local_match_identity_args
            else self.arg_utils.strip_local_match_args(normalized_args)
        )
        config["proxy_host"] = ""
        config["check_hosts"] = ["127.0.0.1"]
        config["remote_human_enabled"] = False
        config["mode"] = "local_human_vs_changeling"
        resolved_bot_name = ""
        preferred_bot_name = str(bot_name or "").strip()
        for index, arg in enumerate(normalized_args):
            if preferred_bot_name:
                break
            if str(arg).strip() == "--bot" and index + 1 < len(normalized_args):
                resolved_bot_name = str(normalized_args[index + 1]).strip()
                break
        if preferred_bot_name:
            resolved_bot_name = preferred_bot_name
        config["bot_name"] = resolved_bot_name
        profile = get_bot_launch_profile(resolved_bot_name)
        config["bot_profile"] = {
            "name": profile.name,
            "type": profile.bot_type,
            "file_name": profile.file_name,
            "required_runtime": profile.required_runtime,
        } if profile else {"name": resolved_bot_name, "error": "unknown_bot_profile"}
        if profile:
            bot_root = os.path.join(config.get("working_directory", ""), "Bots")
            config["bot_profile_validation"] = profile.validate(bot_root)
        for field in ("starcraft2_exe_path", "starcraft2_support64_path", "starcraft2_base_path"):
            config[field] = self.config_manager.resolve_path_value(config.get(field, ""))
        self._refresh_starcraft2_runtime_paths(config)
        return config

    def _refresh_starcraft2_runtime_paths(self, config: Dict[str, Any]) -> None:
        #20260717_kpopmodder: Keep local-match launchers resilient to SC2 Base
        # folder changes after Blizzard updates.
        resolver = getattr(self.config_manager, "resolve_starcraft2_runtime_paths", None)
        if not callable(resolver):
            return
        resolved = resolver(config)
        if not isinstance(resolved, dict):
            return
        for field in (
            "starcraft2_exe_path",
            "starcraft2_support64_path",
            "starcraft2_base_path",
        ):
            if resolved.get(field):
                config[field] = resolved[field]

    def ensure_local_match_runtime(self, config: Dict[str, Any]) -> Dict[str, Any]:
        runtime_dir = self.config_manager.resolve_path_value(
            str(config.get("working_directory", "") or "")
        )
        repo_runtime_dir = os.path.normpath(os.path.join(self.plugin_root, "runtime"))
        if not self.arg_utils.same_path(runtime_dir, repo_runtime_dir):
            return {
                "ok": True,
                "downloaded": False,
                "skipped": "non_repo_runtime",
                "runtime_dir": runtime_dir,
            }
        download_config = self.config_manager.get_section("runtime_download")
        return self.runtime_downloader.ensure_runtime(
            repo_runtime_dir,
            enabled=self.arg_utils.config_bool(download_config.get("enabled", True), True),
            repo_id=str(download_config.get("repo_id") or DEFAULT_RUNTIME_REPO_ID),
            repo_type=str(download_config.get("repo_type") or DEFAULT_RUNTIME_REPO_TYPE),
            revision=str(download_config.get("revision") or DEFAULT_RUNTIME_REVISION),
            local_archive_path=os.path.join(self.plugin_root, "runtime.Zip"),
        )
