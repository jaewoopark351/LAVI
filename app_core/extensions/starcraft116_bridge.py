#20260706_kpopmodder: Added StarCraft 1.16 bridge adapter around existing plugin internals.
from __future__ import annotations

from typing import Any, Dict, Mapping, Optional, Protocol

from core.logger import log_print


class StarCraft116BridgeProtocol(Protocol):
    #20260706_kpopmodder: Keep the protocol explicit while adapter code stays concrete and small.
    def start_status_listener(self, callback=None): ...
    def stop_status_listener(self): ...
    def start_game(self, profile_name: Optional[str] = None): ...
    def stop_game(self): ...
    def send_command(self, command: Any) -> Dict[str, Any]: ...
    def get_status(self) -> Dict[str, Any]: ...
    def poll_once(self) -> int: ...


class StarCraft116Bridge:
    #20260706_kpopmodder: Keep legacy StarCraft116 behavior and expose a narrow bridge API.
    def __init__(self, plugin):
        self.plugin = plugin
        self.status_listener = None

    def start_status_listener(self, callback=None):
        if callback is not None and callable(callback):
            self.status_listener = callback
            setter = getattr(self.plugin, "set_status_event_callback", None)
            if callable(setter):
                setter(callback)
            else:
                return False
        return bool(getattr(self.plugin, "start_game_event_watcher", None) and self.plugin.start_game_event_watcher())

    def stop_status_listener(self):
        stopper = getattr(self.plugin, "stop_game_event_watcher", None)
        if not callable(stopper):
            return False
        stopper()
        return True

    def start_game(self, profile_name: Optional[str] = None):
        launcher = getattr(self.plugin, "start", None)
        if not callable(launcher):
            return False
        launcher(profile_name=profile_name, launch_source="extension")
        return True

    def stop_game(self):
        stopper = getattr(self.plugin, "stop", None)
        if not callable(stopper):
            return False
        stopper()
        return True

    def get_status(self) -> Dict[str, Any]:
        getter = getattr(self.plugin, "get_status", None)
        if callable(getter):
            try:
                status = getter()
                if isinstance(status, dict):
                    return status
            except Exception as e:
                log_print(f"[StarCraft116Bridge] get_status failed: {e}")
        return {}

    def get_watcher_state(self) -> Dict[str, Any]:
        thread = getattr(self.plugin, "game_event_thread", None)
        stop_event = getattr(self.plugin, "game_event_stop_event", None)
        return {
            "thread_alive": bool(thread and getattr(thread, "is_alive", lambda: False)()),
            "stop_signaled": bool(stop_event and getattr(stop_event, "is_set", lambda: True)()),
            "status_callback_set": bool(
                getattr(self.plugin, "status_event_callback", None) is not None
            ),
        }

    def send_command(self, command: Any) -> Dict[str, Any]:
        if self.plugin is None:
            return {"ok": False, "error": "missing_plugin"}

        if isinstance(command, Mapping):
            payload = dict(command)
            action = str(payload.get("action") or payload.get("type") or "").strip().lower()
            profile_name = payload.get("profile_name") or payload.get("profile")
            launch_source = payload.get("launch_source", "extension")
        elif isinstance(command, str):
            action = command.strip().lower()
            profile_name = None
            launch_source = "extension"
        else:
            return {"ok": False, "error": "unsupported_command"}

        if action in {"launch", "start"}:
            return {
                "ok": self.start_game(
                    profile_name=profile_name,
                ),
                "action": action,
                "profile_name": profile_name,
                "launch_source": launch_source,
            }
        if action in {"stop", "shutdown"}:
            return {"ok": self.stop_game(), "action": action}
        if action in {"start_watcher", "watch"}:
            callback = payload.get("callback") if isinstance(command, Mapping) else None
            return {
                "ok": self.start_status_listener(callback),
                "action": action,
            }
        if action in {"stop_watcher", "watch_stop"}:
            return {"ok": self.stop_status_listener(), "action": action}
        if action in {"status", "get_status"}:
            return {"ok": True, "status": self.get_status(), "action": action}
        if action in {"status_listener", "get_watcher_status"}:
            return {"ok": True, "status": self.get_watcher_state(), "action": action}
        #20260706_kpopmodder: Add screen observation passthrough action for event/command shim.
        if action in {"screen_observation", "screen_vision", "screen_vision_observation"}:
            observation_payload = self._extract_observation_payload(
                payload if isinstance(command, Mapping) else {},
            )
            handler = self._resolve_screen_observation_handler()
            if handler is None:
                return {
                    "ok": False,
                    "action": action,
                    "error": "screen_observation_handler_missing",
                }

            try:
                handler(observation_payload)
            except Exception as e:
                return {
                    "ok": False,
                    "action": action,
                    "error": str(e),
                }

            return {
                "ok": True,
                "action": action,
                "observation_payload": observation_payload,
            }

        return {"ok": False, "action": action, "error": "unknown_action"}

    #20260706_kpopmodder: Keep ScreenVision observation bridge tolerant to text-only and nested payloads.
    def _extract_observation_payload(self, payload: Mapping) -> Dict[str, Any]:
        data = dict(payload) if isinstance(payload, Mapping) else {}
        nested_payload = data.get("payload")
        if isinstance(nested_payload, Mapping):
            data.update(dict(nested_payload))

        observation = data.get("observation")
        text = data.get("text")
        if observation is None and isinstance(text, str):
            observation = self._extract_observation_from_text(text)
            data["observation"] = observation

        if data.get("source") is None:
            data["source"] = "screen_vision"
        if data.get("source_event") is None:
            data["source_event"] = "screen_observation"

        return {
            "source": data.get("source") or "screen_vision",
            "observation": data.get("observation") or "",
            "text": data.get("text") or "",
            "question": data.get("question") or "",
            "display_text": data.get("display_text") or "",
            "remember_history": bool(data.get("remember_history", False)),
            "metadata": data.get("metadata", {}),
            "source_event": data.get("source_event") or "screen_observation",
            "raw_payload": data,
        }

    #20260706_kpopmodder: First non-empty line is used when only LLM input text exists.
    def _extract_observation_from_text(self, text: str) -> str:
        cleaned = str(text or "").strip()
        if not cleaned:
            return ""

        lines = cleaned.splitlines()
        for line in lines:
            candidate = str(line or "").strip()
            if candidate:
                return candidate
        return ""

    #20260706_kpopmodder: Support multiple future handler method names without touching caller code.
    def _resolve_screen_observation_handler(self):
        for name in (
            "handle_screen_observation",
            "handle_screen_observation_payload",
            "on_screen_observation",
            "receive_screen_observation",
            "process_screen_observation",
        ):
            handler = getattr(self.plugin, name, None)
            if callable(handler):
                return handler
        return None

    def poll_once(self) -> int:
        poller = getattr(self.plugin, "_poll_game_events", None)
        if not callable(poller):
            return 0
        try:
            return int(poller())
        except Exception as e:
            log_print(f"[StarCraft116Bridge] poll_once failed: {e}")
            return 0
