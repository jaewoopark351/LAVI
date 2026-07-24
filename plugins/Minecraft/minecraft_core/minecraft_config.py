#20260725_kpopmodder: Added Minecraft bridge config loader with repository-root path handling.
from __future__ import annotations

import copy
import json
import os
from pathlib import Path
from typing import Any, Dict

from core.paths import get_lavi_paths


DEFAULT_CONFIG: Dict[str, Any] = {
    "enabled": True,
    "allow_actions": True,
    "bridge": {
        "base_url": "http://127.0.0.1:4316",
        "timeout_sec": 3.0,
    },
}


class MinecraftConfig:
    def __init__(self, plugin_root: str | None = None, config_path: str | None = None):
        self.plugin_root = plugin_root or os.path.dirname(
            os.path.dirname(os.path.abspath(__file__))
        )
        self.project_root = os.path.dirname(os.path.dirname(self.plugin_root))
        self.paths = get_lavi_paths(Path(self.project_root))
        self.config_path = str(
            config_path or self.paths.config_path("minecraft_config.json")
        )
        self.example_config_path = str(
            self.paths.config_path("minecraft_config.example.json")
        )
        self.config: Dict[str, Any] = self._default_config()
        self.config_exists = False
        self.load_error = ""
        self.load()

    def load(self) -> Dict[str, Any]:
        self.config = self._default_config()
        self.config_exists = os.path.exists(self.config_path)
        self.load_error = ""
        if not self.config_exists:
            return self.config

        try:
            with open(self.config_path, "r", encoding="utf-8") as file:
                loaded = json.load(file)
        except Exception as error:
            self.load_error = str(error)
            return self.config

        if not isinstance(loaded, dict):
            self.load_error = "config root must be a JSON object"
            return self.config

        self._merge_dict(self.config, loaded)
        return self.config

    def reload(self) -> Dict[str, Any]:
        return self.load()

    def load_example_config(self) -> Dict[str, Any]:
        with open(self.example_config_path, "r", encoding="utf-8") as file:
            data = json.load(file)
        if not isinstance(data, dict):
            raise ValueError("Minecraft example config must be a JSON object")
        return data

    def snapshot(self) -> Dict[str, Any]:
        return copy.deepcopy(self.config)

    def get_bool(self, key: str, default: bool = False) -> bool:
        value = self.config.get(key, default)
        if isinstance(value, bool):
            return value
        return str(value).strip().lower() in {"1", "true", "yes", "on"}

    def bridge_base_url(self) -> str:
        bridge = self._bridge_section()
        return str(bridge.get("base_url") or "http://127.0.0.1:4316").strip()

    def request_timeout_sec(self) -> float:
        bridge = self._bridge_section()
        try:
            return max(0.1, float(bridge.get("timeout_sec", 3.0)))
        except (TypeError, ValueError):
            return 3.0

    def config_message(self) -> str:
        if self.load_error:
            return f"Minecraft config load failed: {self.load_error}"
        if not self.config_exists:
            return (
                "Minecraft config missing. Using safe defaults from "
                f"{self.example_config_path}."
            )
        if not self.get_bool("enabled", True):
            return "Minecraft config loaded. enabled=false in plugin config."
        return f"Minecraft bridge configured: {self.bridge_base_url()}"

    def _bridge_section(self) -> Dict[str, Any]:
        bridge = self.config.get("bridge", {})
        return copy.deepcopy(bridge) if isinstance(bridge, dict) else {}

    def _default_config(self) -> Dict[str, Any]:
        return copy.deepcopy(DEFAULT_CONFIG)

    def _merge_dict(self, target: Dict[str, Any], source: Dict[str, Any]) -> None:
        for key, value in source.items():
            if isinstance(value, dict) and isinstance(target.get(key), dict):
                self._merge_dict(target[key], value)
            else:
                target[key] = copy.deepcopy(value)
