#20260801_kpopmodder: Isolate Fabric ChatClef config coercion from plugin startup.
from __future__ import annotations

from collections.abc import Mapping
from typing import Any

from .fabric_chatclef_config import FabricChatClefConfig


class FabricChatClefConfigLoader:
    SECTION_NAME = "MinecraftFabricChatClef"

    def from_mapping(self, value: Any) -> FabricChatClefConfig:
        payload = self._section_payload(value)
        return FabricChatClefConfig(
            enabled=self._bool(payload.get("enabled"), False),
            host=self._host(payload.get("host")),
            port=self._port(payload.get("port")),
            startup_timeout_sec=self._positive_float(
                payload.get("startup_timeout_sec"),
                3.0,
            ),
            reconcile_stale_deposit_to_unknown_enabled=self._feature_bool(
                payload.get("reconcile_stale_deposit_to_unknown_enabled"),
                False,
            ),
        )

    def _section_payload(self, value: Any) -> dict[str, Any]:
        if not isinstance(value, Mapping):
            return {}
        section = value.get(self.SECTION_NAME)
        if isinstance(section, Mapping):
            return dict(section)
        return dict(value)

    def _host(self, value: Any) -> str:
        host = str(value or "").strip()
        return host or "127.0.0.1"

    def _port(self, value: Any) -> int:
        port = 4316 if value in (None, "") else int(value)
        if port < 0 or port > 65535:
            raise ValueError("Fabric ChatClef bridge port must be 0..65535")
        return port

    def _positive_float(self, value: Any, default: float) -> float:
        number = float(value or default)
        if number <= 0:
            return default
        return number

    def _bool(self, value: Any, default: bool) -> bool:
        if isinstance(value, bool):
            return value
        if value is None:
            return default
        if isinstance(value, str):
            return value.strip().lower() in {"1", "true", "yes", "on"}
        return bool(value)

    def _feature_bool(self, value: Any, default: bool) -> bool:
        if isinstance(value, bool):
            return value
        if value is None:
            return default
        if isinstance(value, str):
            text = value.strip().lower()
            if text == "true":
                return True
            if text == "false":
                return False
        raise ValueError(
            "reconcile_stale_deposit_to_unknown_enabled must be true or false"
        )
