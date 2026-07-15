#20260707_kpopmodder: Added StarCraft2 config loader with SC2PATH fallback and disabled-by-default defaults.
from __future__ import annotations

import copy
import json
import os
import shutil
from pathlib import Path
from typing import Any, Dict, List

from core.logger import log_print
from core.paths import get_lavi_paths


LEGACY_PROJECT_ROOTS_ENV = "LAVI_LEGACY_PROJECT_ROOTS"

LOCAL_MATCH_EXECUTABLE_RELATIVE = os.path.join(
    "plugins",
    "StarCraft2",
    "native",
    "Sc2LadderServer",
    "bin",
    "LavHumanVsBot.exe",
)
LOCAL_MATCH_WORKDIR_RELATIVE = os.path.join("plugins", "StarCraft2", "runtime")

DEFAULT_STARCRAFT2_PATH = "C:\\Program Files (x86)\\StarCraft II"


DEFAULT_CONFIG: Dict[str, Any] = {
    "enabled": False,
    "engine": "internal_lav_bot",
    "starcraft2_path": DEFAULT_STARCRAFT2_PATH,
    "map_name": "AbyssalReefLE",
    "race": "Terran",
    "enemy_race": "Zerg",
    "enemy_difficulty": "Easy",
    "realtime": False,
    "auto_launch": False,
    "step_time": 0.015,
    "max_game_seconds": 1800,
    "target_workers": 18,
    "target_barracks": 4,
    "attack_count": 8,
    "reinforce_interval_sec": 5,
    "enemy_seen_event_interval_sec": 20,
    "enable_lav_coach": True,
    "coach_event_interval_sec": 8,
    "external_exe": {
        "path": "",
        "working_directory": "",
        "args": [],
    },
    "micromachine": {
        "path": "",
        "working_directory": "",
        "args": [],
        "starcraft2_executable": "",
        "auto_add_executable_arg": True,
        "bot_config_name": "BotConfig.txt",
        "require_bot_config": True,
    },
    "ares_sc2": {
        "script_path": "",
        "working_directory": "",
        "python_path": "",
        "use_poetry": False,
        "poetry_path": "poetry",
        "python_command": "python",
        "args": [],
    },
    "external_jar": {
        "java_path": "java",
        "jar_path": "",
        "working_directory": "",
        "args": [],
    },
    "human_vs_bot": {
        "launcher_path": "",
        "working_directory": "",
        "args": [],
    },
    "runtime_download": {
        "enabled": True,
        "repo_id": "jaewoopark96/plugins_StarCraft2_runtime",
        "repo_type": "model",
        "revision": "main",
    },
#     #20260712_kpopmodder: LAN Lobby default config is commented out for
#     # maintenance safety. JSON config files cannot use # comments, so this
#     # Python default is the authoritative commented reference.
#     "lan_lobby": {
#         "enabled": False,
#         "archived": True,
#         "notes": "20260712_kpopmodder: LAN Lobby remote-human is archived/disabled; keep as reference only unless explicitly revived.",
#         "room_name": "LAV StarCraft II",
#         "player_name": "LAV",
#         "preferred_bot": "Changeling",
#         "mode": "observer",
#         "discovery_port": 47624,
#         "join_port": 47625,
#         "broadcast_addresses": ["255.255.255.255"],
#         "announce_interval_sec": 2.0,
#         "room_ttl_sec": 10.0,
#         "proxy_host": "",
#         "proxy_ports": [5677, 5678],
#         "start_port": 5690,
#         "human_client_port": 5679,
#         "remote_start_port": 47626,
#         "lan_connect_mode": "relay",
#         "lan_port_layout": "s2client-api-shared",
#         "multiplayer_relay_enabled": True,
#         "multiplayer_relay_bind_host": "",
#         "multiplayer_relay_ports": [],
#         "map_download_port": 47627,
#         "auto_serve_map": True,
#         "auto_start_scan": False,
#         "auto_host_room": False,
#     },
    #20260711_kpopmodder: Keep Local Match launcher settings separate from LAN Lobby remote-human launcher settings.
    "local_match": {
        "enabled": False,
        "executable_path": LOCAL_MATCH_EXECUTABLE_RELATIVE,
        "working_directory": LOCAL_MATCH_WORKDIR_RELATIVE,
        "args": [
            "--human-name",
            "IdleProbe",
            "--map",
            "PersephoneLE.SC2Map",
            "--race",
            "Protoss",
            "--bot-dir",
            "Bots/",
            "--config",
            "HumanLadder.json",
        ],
        "ports": [5677, 5678],
        "check_hosts": ["127.0.0.1"],
        "connect_timeout_sec": 0.5,
        "capture_output": True,
        "restart_unhealthy": True,
        "restart_unhealthy_after_sec": 20.0,
        "remote_human_enabled": False,
        "starcraft2_exe_path": "",
        "starcraft2_support64_path": "",
        "starcraft2_base_path": "",
    },
    "ladder_proxy": {
        "enabled": False,
        "archived": True,
        "notes": "20260712_kpopmodder: LAN Lobby ladder proxy is archived/disabled.",
        "executable_path": "",
        "working_directory": "",
        "args": [],
        "ports": [5677, 5678],
        "check_hosts": ["127.0.0.1"],
        "connect_timeout_sec": 0.5,
        "capture_output": True,
        "auto_start_with_lan_host": False,
        "remote_human_enabled": False,
        "remote_human_client_port": 5679,
        "remote_start_timeout_sec": 60.0,
        "starcraft2_exe_path": "",
        "starcraft2_support64_path": "",
        "starcraft2_base_path": "",
    },
}


class StarCraft2Config:
    #20260707_kpopmodder: Keep StarCraft II optional so missing game files never break LAV startup.
    def __init__(self, plugin_root: str | None = None, config_path: str | None = None):
        self.plugin_root = plugin_root or os.path.dirname(
            os.path.dirname(os.path.abspath(__file__))
        )
        self.project_root = os.path.dirname(os.path.dirname(self.plugin_root))
        self.paths = get_lavi_paths(Path(self.project_root))
        self.config_path = str(config_path or self.paths.config_path("starcraft2_config.json"))
        self.config_dir = os.path.dirname(self.config_path)
        self.example_config_path = str(self.paths.config_path("starcraft2_config.example.json"))
        self.legacy_example_config_path = os.path.join(
            self.plugin_root,
            "config",
            "starcraft2_config.example.json",
        )
        self.config: Dict[str, Any] = self._default_config()
        self.config_exists = False
        self.load_error = ""
        self.migrated_from = ""
        self.load()

    def load(self) -> Dict[str, Any]:
        self.config = self._default_config()
        self._migrate_legacy_config_if_missing()
        self.config_exists = os.path.exists(self.config_path)
        self.load_error = ""

        if not self.config_exists:
            return self.config

        try:
            with open(self.config_path, "r", encoding="utf-8") as file:
                loaded = json.load(file)
        except Exception as e:
            self.load_error = str(e)
            return self.config

        if not isinstance(loaded, dict):
            self.load_error = "config root must be a JSON object"
            return self.config

        self._merge_dict(self.config, loaded)
        return self.config

    def reload(self) -> Dict[str, Any]:
        return self.load()

    def load_example_config(self) -> Dict[str, Any]:
        example_path = (
            self.example_config_path
            if os.path.exists(self.example_config_path)
            else self.legacy_example_config_path
        )
        with open(example_path, "r", encoding="utf-8") as file:
            data = json.load(file)
        if not isinstance(data, dict):
            raise ValueError("StarCraft2 example config must be a JSON object")
        return data

    def snapshot(self) -> Dict[str, Any]:
        return copy.deepcopy(self.config)

    def get(self, key: str, default: Any = None) -> Any:
        return self.config.get(key, default)

    def set_runtime_value(self, key: str, value: Any) -> None:
        self.config[key] = value

    def get_bool(self, key: str, default: bool = False) -> bool:
        value = self.config.get(key, default)
        if isinstance(value, bool):
            return value
        return str(value).strip().lower() in {"1", "true", "yes", "on"}

    def get_int(self, key: str, default: int = 0) -> int:
        try:
            return int(self.config.get(key, default))
        except (TypeError, ValueError):
            return int(default)

    def get_float(self, key: str, default: float = 0.0) -> float:
        try:
            return float(self.config.get(key, default))
        except (TypeError, ValueError):
            return float(default)

    def get_section(self, key: str) -> Dict[str, Any]:
        value = self.config.get(key, {})
        return copy.deepcopy(value) if isinstance(value, dict) else {}

    def sc2path_env(self) -> str:
        return str(os.environ.get("SC2PATH", "") or "").strip().strip("\"'")

    def resolve_starcraft2_path(self) -> str:
        configured = str(self.config.get("starcraft2_path", "") or "").strip().strip("\"'")
        if configured:
            return self.resolve_path_value(configured)
        env_path = self.sc2path_env()
        if env_path:
            return self.resolve_path_value(env_path)
        return self.resolve_path_value(DEFAULT_STARCRAFT2_PATH)

    def starcraft2_path_candidates(self) -> List[str]:
        candidates = []
        for value in (
            self.config.get("starcraft2_path", ""),
            self.sc2path_env(),
            DEFAULT_STARCRAFT2_PATH,
        ):
            resolved = self.resolve_path_value(str(value or ""))
            if resolved and resolved not in candidates:
                candidates.append(resolved)
        return candidates

    def config_message(self) -> str:
        if self.load_error:
            return f"StarCraft2 config load failed: {self.load_error}"
        if not self.config_exists:
            return (
                "StarCraft2 config missing. Using safe defaults from "
                f"{self.example_config_path}; module remains disabled unless modules.json enables it."
            )
        if not self.get_bool("enabled", False):
            return "StarCraft2 config loaded. enabled=false in plugin config."
        return (
            "StarCraft2 config loaded. engine="
            f"{self.config.get('engine', 'internal_lav_bot')}"
        )

    def build_runtime_config(self, overrides: Dict[str, Any] | None = None) -> Dict[str, Any]:
        runtime_config = self.snapshot()
        if isinstance(overrides, dict):
            self._merge_dict(runtime_config, overrides)
        runtime_config["starcraft2_path"] = self.resolve_path_value(
            str(runtime_config.get("starcraft2_path", "") or "")
        )
        if not runtime_config.get("starcraft2_path"):
            runtime_config["starcraft2_path"] = self.resolve_starcraft2_path()
        return runtime_config

    def resolve_path_value(self, value: str) -> str:
        value = str(value or "").strip().strip("\"'")
        if not value:
            return ""
        value = os.path.expandvars(os.path.expanduser(value))
        if os.path.isabs(value):
            value = self._migrate_old_repo_root(value)
            return os.path.normpath(value)
        return os.path.normpath(str(self.paths.root_path(value)))

    def _legacy_config_paths(self) -> List[str]:
        return [
            os.path.join(self.plugin_root, "config_starcraft2.json"),
            os.path.join(self.plugin_root, "config", "starcraft2_config.json"),
        ]

    def _migrate_legacy_config_if_missing(self) -> None:
        if os.path.exists(self.config_path):
            return
        for legacy_path in self._legacy_config_paths():
            if not os.path.exists(legacy_path):
                continue
            os.makedirs(os.path.dirname(self.config_path), exist_ok=True)
            shutil.copy2(legacy_path, self.config_path)
            self.migrated_from = legacy_path
            log_print(
                "[StarCraft2Config] migrated legacy config without overwrite: "
                f"{legacy_path} -> {self.config_path}"
            )#20260716_kpopmodder
            return

    def _migrate_old_repo_root(self, value: str) -> str:
        legacy_value = os.path.normpath(os.path.normcase(os.path.expandvars(os.path.expanduser(value))))
        for marker in self._legacy_project_root_markers():
            legacy_root = os.path.normpath(os.path.normcase(marker))
            if legacy_value == legacy_root or legacy_value.startswith(
                f"{legacy_root}{os.sep}"
            ):
                suffix = legacy_value[len(legacy_root) :].lstrip("/\\")
                return os.path.join(self.project_root, suffix)
        return value

    def _legacy_project_root_markers(self) -> List[str]:
        #20260716_kpopmodder: Keep legacy migration configurable without committing a private PC path.
        value = os.environ.get(LEGACY_PROJECT_ROOTS_ENV, "")
        return [
            item.strip().strip("\"'")
            for item in value.split(os.pathsep)
            if item.strip()
        ]

    def _default_config(self) -> Dict[str, Any]:
        return copy.deepcopy(DEFAULT_CONFIG)

    def _merge_dict(self, target: Dict[str, Any], source: Dict[str, Any]) -> None:
        for key, value in source.items():
            if isinstance(value, dict) and isinstance(target.get(key), dict):
                self._merge_dict(target[key], value)
            else:
                target[key] = copy.deepcopy(value)


def load_starcraft2_example_config(plugin_root: str | None = None) -> Dict[str, Any]:
    return StarCraft2Config(plugin_root).load_example_config()
