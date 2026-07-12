#20260707_kpopmodder: Added bridge adapter for StarCraft2 plugin facade commands.
from __future__ import annotations

from typing import Any, Dict, Mapping, Optional, Protocol

from core.logger import log_print


class StarCraft2BridgeProtocol(Protocol):
    #20260707_kpopmodder: Narrow command bridge for StarCraft2 worker and extension callers.
    def send_command(self, command: Any) -> Dict[str, Any]: ...
    def start_game(self, overrides: Optional[Dict[str, Any]] = None) -> Dict[str, Any]: ...
    def stop_game(self) -> Dict[str, Any]: ...
    def get_status(self) -> Dict[str, Any]: ...


class StarCraft2Bridge:
    #20260707_kpopmodder: Keep StarCraft2 plugin facade behind a stable command API.
    def __init__(self, plugin=None):
        self.plugin = plugin

    def start_game(self, overrides: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        if self.plugin is None:
            return self._error("missing_plugin", "start")
        starter = getattr(self.plugin, "start", None)
        if not callable(starter):
            return self._error("missing_start_method", "start")
        try:
            result = starter(overrides or {}, launch_source="extension")
            return self._normalize_result(result, "start")
        except Exception as e:
            return self._error(str(e), "start")

    def stop_game(self) -> Dict[str, Any]:
        if self.plugin is None:
            return self._error("missing_plugin", "stop")
        stopper = getattr(self.plugin, "stop", None)
        if not callable(stopper):
            return self._error("missing_stop_method", "stop")
        try:
            return self._normalize_result(stopper(), "stop")
        except Exception as e:
            return self._error(str(e), "stop")

    def shutdown(self) -> Dict[str, Any]:
        if self.plugin is None:
            return self._error("missing_plugin", "shutdown")
        shutdown = getattr(self.plugin, "shutdown", None)
        if callable(shutdown):
            try:
                shutdown()
                return {"ok": True, "action": "shutdown"}
            except Exception as e:
                return self._error(str(e), "shutdown")
        return self.stop_game()

    def get_status(self) -> Dict[str, Any]:
        if self.plugin is None:
            return {"ok": False, "error": "missing_plugin", "status": {}}
        getter = getattr(self.plugin, "get_status", None)
        if not callable(getter):
            return {"ok": False, "error": "missing_get_status_method", "status": {}}
        try:
            status = getter()
            return {
                "ok": True,
                "action": "status",
                "status": status if isinstance(status, dict) else {},
            }
        except Exception as e:
            log_print(f"[StarCraft2Bridge] get_status failed: {e}")
            return {"ok": False, "error": str(e), "status": {}}

    def set_engine(self, engine_name: str) -> Dict[str, Any]:
        if self.plugin is None:
            return self._error("missing_plugin", "set_engine")
        config_manager = getattr(self.plugin, "config_manager", None)
        setter = getattr(config_manager, "set_runtime_value", None)
        if not callable(setter):
            return self._error("missing_config_setter", "set_engine")
        setter("engine", str(engine_name or "internal_lav_bot"))
        return {
            "ok": True,
            "action": "set_engine",
            "engine": str(engine_name or "internal_lav_bot"),
        }

    def send_command(self, command: Any) -> Dict[str, Any]:
        payload = self._normalize_command(command)
        action = payload.get("action", "")
        if action in {"start", "launch"}:
            return self.start_game(payload.get("config") or payload.get("overrides") or payload)
        if action in {"stop"}:
            return self.stop_game()
        if action in {"shutdown"}:
            return self.shutdown()
        if action in {"status", "get_status"}:
            return self.get_status()
        if action == "set_engine":
            return self.set_engine(payload.get("engine") or payload.get("engine_name"))
        return {"ok": False, "action": action, "error": "unknown_action"}

    def _normalize_command(self, command: Any) -> Dict[str, Any]:
        if isinstance(command, Mapping):
            payload = dict(command)
            action = str(payload.get("action") or payload.get("type") or "").strip().lower()
            payload["action"] = action
            return payload
        if isinstance(command, str):
            return {"action": command.strip().lower()}
        return {"action": "", "raw": str(command)}

    def _normalize_result(self, result: Any, action: str) -> Dict[str, Any]:
        if isinstance(result, dict):
            normalized = dict(result)
            normalized.setdefault("action", action)
            normalized.setdefault("ok", False)
            return normalized
        return {"ok": bool(result), "action": action}

    def _error(self, error: str, action: str) -> Dict[str, Any]:
        return {"ok": False, "action": action, "error": str(error)}

