#20260725_kpopmodder: Added GameExtension adapter for Minecraft ChatClef bridge commands.
from __future__ import annotations

from typing import Any, Dict, Optional

from app_core.extensions.game_extension_context import GameExtensionContext
from app_core.extensions.game_extension_interface import GameExtensionInterface
from core.logger import log_print


class MinecraftGameExtension(GameExtensionInterface):
    def __init__(self, plugin=None):
        self.plugin = plugin
        self._context: Optional[GameExtensionContext] = None
        self._is_initialized = False
        self._is_started = False
        self._command_handlers = {
            "health",
            "ping",
            "status",
            "get_status",
            "inventory",
            "get_inventory",
            "current_action",
            "actions_current",
            "get_current_action",
            "get_item",
            "get-item",
            "getitem",
            "stop",
            "cancel",
            "reload",
        }

    @property
    def name(self) -> str:
        return "minecraft"

    def initialize(self, context: GameExtensionContext) -> None:
        super().initialize(context)
        self._context = context
        if self.plugin is None:
            self._maybe_build_plugin()
        self._sync_runtime_context_resources()
        self._is_initialized = True

    def start(self) -> None:
        if self._is_started:
            return
        if not self._is_initialized:
            log_print("[MinecraftGameExtension] initialize must be called before start")
        self._ensure_plugin_ready()
        self._is_started = True
        self.mark_started(True)
        self.publish_event("extension_started")

    def stop(self) -> None:
        #20260725_kpopmodder: Lifecycle shutdown must not send an in-game stop command implicitly.
        self._is_started = False
        self.mark_started(False)

    def shutdown(self) -> None:
        self.stop()

    def handle_command(self, command: Any) -> Dict[str, Any]:
        command_dto = self.record_command(command)
        self._ensure_plugin_ready()
        payload = command_dto.to_legacy_dict()
        action = self._normalize_action(
            command_dto.action or self._payload_action(payload)
        )
        if action not in self._command_handlers:
            result = {"ok": False, "action": action or "", "error": "unknown_action"}
            return self._finalize_command_result(result, action=action)

        plugin = self.plugin
        handler = getattr(plugin, "handle_command", None)
        if not callable(handler):
            result = {"ok": False, "action": action, "error": "missing_plugin_handler"}
            return self._finalize_command_result(result, action=action)

        payload["action"] = action
        result = handler(payload)
        return self._finalize_command_result(result, action=action)

    def get_status(self) -> Dict[str, Any]:
        plugin_status = {}
        if self.plugin is not None:
            try:
                status = self.plugin.get_status()
                if isinstance(status, dict):
                    plugin_status = status
            except Exception as error:
                plugin_status = {"ok": False, "error": str(error)}
        plugin_status["extension"] = {
            "name": self.name,
            "initialized": self._is_initialized,
            "started": self._is_started,
        }
        self._sync_runtime_context_resources()
        runtime_context = getattr(self, "runtime_context", None)
        snapshot = getattr(runtime_context, "snapshot", None)
        if callable(snapshot):
            plugin_status["runtime_context"] = snapshot()
        return self.apply_status_contract(plugin_status)

    def _ensure_plugin_ready(self) -> None:
        if self.plugin is None:
            self._maybe_build_plugin()
        self._sync_runtime_context_resources()

    def _maybe_build_plugin(self) -> None:
        try:
            from plugins.Minecraft.minecraft import Minecraft
        except Exception as error:
            raise RuntimeError("Minecraft plugin could not be imported") from error
        self.plugin = Minecraft()

    def _finalize_command_result(
        self,
        result: Any,
        action: str = "",
    ) -> Dict[str, Any]:
        result_dto = self.record_result(result, action=action)
        return result_dto.to_legacy_dict()

    def _sync_runtime_context_resources(self) -> None:
        runtime_context = getattr(self, "runtime_context", None)
        set_resource = getattr(runtime_context, "set_resource", None)
        if not callable(set_resource):
            return
        plugin = self.plugin
        set_resource("plugin", plugin)
        if plugin is None:
            return
        set_resource("config_manager", getattr(plugin, "config_manager", None))
        set_resource("facade_service", getattr(plugin, "facade_service", None))

    def _normalize_action(self, value: Any) -> str:
        return str(value or "").strip().lower().replace("-", "_")

    def _payload_action(self, payload: Dict[str, Any]) -> Any:
        nested = payload.get("payload")
        if isinstance(nested, dict):
            return (
                nested.get("action")
                or nested.get("type")
                or nested.get("event")
                or nested.get("event_type")
            )
        return None
