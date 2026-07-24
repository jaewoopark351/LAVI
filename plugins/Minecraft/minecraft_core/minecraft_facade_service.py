#20260725_kpopmodder: Added a small facade so UI and GameExtension share bridge behavior.
from __future__ import annotations

import json
from typing import Any, Dict

from .chatclef_bridge_client import ChatClefBridgeClient
from .minecraft_config import MinecraftConfig


class MinecraftFacadeService:
    def __init__(
        self,
        config_manager: MinecraftConfig | None = None,
        client_factory=None,
    ):
        self.config_manager = config_manager or MinecraftConfig()
        self.client_factory = client_factory or ChatClefBridgeClient
        self.client = self._build_client()

    def reload(self) -> Dict[str, Any]:
        self.config_manager.reload()
        self.client = self._build_client()
        return {
            "ok": True,
            "action": "reload",
            "config": self._public_config(),
        }

    def health(self) -> Dict[str, Any]:
        return self.client.health()

    def status(self) -> Dict[str, Any]:
        return self.client.status()

    def inventory(self) -> Dict[str, Any]:
        return self.client.inventory()

    def current_action(self) -> Dict[str, Any]:
        return self.client.current_action()

    def get_item(self, item: Any, count: Any = 1) -> Dict[str, Any]:
        if not self._actions_allowed():
            return {
                "ok": False,
                "action": "get_item",
                "error": "actions_disabled",
                "message": "Minecraft actions are disabled in config.",
            }
        item_name = str(item or "").strip()
        if not item_name:
            return {
                "ok": False,
                "action": "get_item",
                "error": "missing_item",
                "message": "item is required.",
            }
        return self.client.get_item(item_name, self._coerce_count(count))

    def stop(self) -> Dict[str, Any]:
        return self.client.stop()

    def handle_command(self, command: Any) -> Dict[str, Any]:
        payload = dict(command) if isinstance(command, dict) else {"action": command}
        nested = payload.get("payload")
        if isinstance(nested, dict):
            merged = dict(nested)
            merged.update({key: value for key, value in payload.items() if key != "payload"})
            payload = merged
        action = self._normalize_action(
            payload.get("action")
            or payload.get("type")
            or payload.get("event")
            or payload.get("event_type")
        )

        if action in {"health", "ping"}:
            return self._with_action(self.health(), action)
        if action in {"status", "get_status"}:
            return self._with_action(self.status(), action)
        if action in {"inventory", "get_inventory"}:
            return self._with_action(self.inventory(), action)
        if action in {"current_action", "actions_current", "get_current_action"}:
            return self._with_action(self.current_action(), action)
        if action in {"get_item", "getitem"}:
            return self._with_action(
                self.get_item(payload.get("item"), payload.get("count", 1)),
                "get_item",
            )
        if action in {"stop", "cancel"}:
            return self._with_action(self.stop(), action)
        if action == "reload":
            return self.reload()
        return {
            "ok": False,
            "action": action or "",
            "error": "unknown_action",
        }

    def get_status(self) -> Dict[str, Any]:
        bridge_status = self.status()
        return {
            "ok": bool(bridge_status.get("ok", False)),
            "name": "minecraft",
            "enabled": self.config_manager.get_bool("enabled", True),
            "allow_actions": self.config_manager.get_bool("allow_actions", True),
            "config_message": self.config_manager.config_message(),
            "config": self._public_config(),
            "bridge": bridge_status,
        }

    def status_json(self, payload: Dict[str, Any] | None = None) -> str:
        return json.dumps(
            payload if isinstance(payload, dict) else self.get_status(),
            ensure_ascii=False,
            indent=2,
            default=str,
        )

    def public_config(self) -> Dict[str, Any]:
        return self._public_config()

    def _build_client(self):
        return self.client_factory(
            base_url=self.config_manager.bridge_base_url(),
            timeout_sec=self.config_manager.request_timeout_sec(),
        )

    def _public_config(self) -> Dict[str, Any]:
        return {
            "enabled": self.config_manager.get_bool("enabled", True),
            "allow_actions": self.config_manager.get_bool("allow_actions", True),
            "bridge_base_url": self.config_manager.bridge_base_url(),
            "timeout_sec": self.config_manager.request_timeout_sec(),
            "config_path": self.config_manager.config_path,
        }

    def _actions_allowed(self) -> bool:
        return (
            self.config_manager.get_bool("enabled", True)
            and self.config_manager.get_bool("allow_actions", True)
        )

    def _coerce_count(self, value: Any) -> int:
        try:
            count = int(float(value))
        except (TypeError, ValueError):
            count = 1
        return max(1, count)

    def _normalize_action(self, value: Any) -> str:
        return str(value or "").strip().lower().replace("-", "_")

    def _with_action(self, payload: Dict[str, Any], action: str) -> Dict[str, Any]:
        result = dict(payload or {})
        result.setdefault("action", action)
        return result
