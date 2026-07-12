#202600707_kpopmodder
#20260706_kpopmodder: Added Chess adapter wrapper to run existing Chess plugin through extension registry.
from __future__ import annotations

from typing import Any, Dict, Optional

from core.logger import log_print
from app_core.extensions import GameExtensionInterface
from app_core.extensions.game_extension_context import GameExtensionContext


class ChessGameExtension(GameExtensionInterface):
    #20260706_kpopmodder: Keep legacy Chess wiring intact while exposing a stable extension interface.
    def __init__(self, plugin=None):
        self.plugin = plugin
        self._context: Optional[GameExtensionContext] = None
        self._is_initialized = False
        self._is_started = False
        self._command_handlers = {
            "status",
            "get_status",
            "state",
            "new_game",
            "ai_move",
            "start_ai",
            "start_engine",
            "stop_engine",
            "start_server",
            "start_web_server",
            "stop",
            "shutdown",
            "resign",
            "reset",
            "reset_or_resign",
        }

    @property
    def name(self) -> str:
        return "chess"

    def initialize(self, context: GameExtensionContext) -> None:
        self._context = context
        if self.plugin is None:
            self._maybe_build_plugin()

        if self.plugin is None:
            return
        if self._context is None:
            return
        self._bind_ai_reaction_callback(self._context)
        self._is_initialized = True

    def start(self) -> None:
        if self._is_started:
            return
        if not self._is_initialized:
            log_print("[ChessGameExtension] initialize must be called before start")
        self._ensure_plugin_ready()
        plugin = self.plugin
        if plugin is None:
            raise RuntimeError("Chess plugin is not available")
        self._ensure_server_started(plugin)
        self._is_started = True

    def stop(self) -> None:
        self._is_started = False
        if self.plugin is None:
            return
        try:
            plugin_shutdown = getattr(self.plugin, "shutdown", None)
            if callable(plugin_shutdown):
                plugin_shutdown()
        except Exception as e:
            log_print(f"[ChessGameExtension] stop failed: {e}")

    def shutdown(self) -> None:
        self.stop()

    def handle_command(self, command: Any) -> Dict[str, Any]:
        if self.plugin is None:
            return {"ok": False, "error": "missing_plugin"}

        action = None
        payload = {}
        if isinstance(command, dict):
            payload = dict(command)
            action = str(
                payload.get("action")
                or payload.get("type")
                or payload.get("event")
                or ""
            ).strip().lower()
        elif isinstance(command, str):
            action = command.strip().lower()

        if action not in self._command_handlers:
            return {"ok": False, "action": action or "", "error": "unknown_action"}

        return self._run_command(action, payload)

    def get_status(self) -> Dict[str, Any]:
        status = {
            "extension": {
                "name": self.name,
                "initialized": self._is_initialized,
                "started": self._is_started,
            }
        }
        plugin = self.plugin
        if plugin is None:
            status["plugin"] = {"ready": False}
            return status
        try:
            controller = getattr(plugin, "controller", None)
            web_server = getattr(plugin, "web_server", None)
            if controller is not None and hasattr(controller, "state"):
                state = controller.state()
                if isinstance(state, dict):
                    status["controller"] = state
            status["plugin"] = {
                "ready": True,
                "server_url": getattr(plugin, "server_url", None),
                "server_message": getattr(plugin, "server_message", ""),
            }
            if web_server is not None:
                status["plugin"]["web_server_host"] = getattr(
                    web_server, "host", None
                )
                status["plugin"]["web_server_port"] = getattr(
                    web_server, "port", None
                )
        except Exception as e:
            status["error"] = str(e)
        return status

    def _run_command(self, action: str, payload: Dict[str, Any]) -> Dict[str, Any]:
        plugin = self.plugin
        if plugin is None:
            return {"ok": False, "action": action, "error": "missing_plugin"}
        controller = getattr(plugin, "controller", None)

        try:
            if action in {"status", "get_status", "state"}:
                return {"ok": True, "action": action, "status": self.get_status()}
            if action in {"start_server", "start_web_server"}:
                return {
                    "ok": True,
                    "action": action,
                    "server_url": self._ensure_server_started(plugin),
                }
            if action in {"stop", "shutdown"}:
                self.stop()
                return {"ok": True, "action": action}
            if action == "new_game":
                return {
                    "ok": True,
                    "action": action,
                    "state": self._state_or_default(
                        controller.new_game() if controller is not None else None
                    ),
                }
            if action in {"ai_move", "start_ai"}:
                return {
                    "ok": True,
                    "action": action,
                    "state": self._state_or_default(
                        controller.apply_ai_move() if controller is not None else None
                    ),
                }
            if action == "start_engine":
                return {
                    "ok": True,
                    "action": action,
                    "state": self._state_or_default(
                        controller.start_engine() if controller is not None else None
                    ),
                }
            if action == "stop_engine":
                return {
                    "ok": True,
                    "action": action,
                    "state": self._state_or_default(
                        controller.stop_engine() if controller is not None else None
                    ),
                }
            if action in {"reset", "resign", "reset_or_resign"}:
                return {
                    "ok": True,
                    "action": action,
                    "state": self._state_or_default(
                        controller.reset_or_resign()
                        if controller is not None
                        else None
                    ),
                }
            return {
                "ok": False,
                "action": action,
                "error": "action_not_supported",
            }
        except Exception as e:
            return {"ok": False, "action": action, "error": str(e)}

    def _ensure_server_started(self, plugin) -> Optional[str]:
        starter = getattr(plugin, "_ensure_server_started", None)
        if callable(starter):
            try:
                starter()
                return getattr(plugin, "server_url", None)
            except Exception:
                return getattr(plugin, "server_url", None)

        web_server = getattr(plugin, "web_server", None)
        if web_server is not None and getattr(web_server, "url", None) is None:
            start_server = getattr(web_server, "start", None)
            if callable(start_server):
                return str(start_server())
        return getattr(plugin, "server_url", None)

    def _ensure_plugin_ready(self):
        if self.plugin is None:
            self._maybe_build_plugin()

    def _maybe_build_plugin(self):
        try:
            from plugins.Chess.Chess import Chess
        except Exception as e:
            raise RuntimeError("Chess plugin could not be imported") from e
        self.plugin = Chess()

    def _state_or_default(self, state: Any) -> Dict[str, Any]:
        if isinstance(state, dict):
            return state
        return {"ok": False}

    def _bind_ai_reaction_callback(self, context: GameExtensionContext) -> None:
        if self.plugin is None:
            return
        callback = getattr(self.plugin, "set_ai_move_applied_callback", None)
        if not callable(callback):
            return
        llm = getattr(context, "llm", None)
        tts = getattr(context, "tts", None)
        if llm is None or tts is None:
            return
        try:
            from plugins.Chess.chess_core.chess_reaction_runtime import (
                handle_chess_ai_move_applied,
            )
        except Exception as e:
            log_print(f"[ChessGameExtension] callback builder import failed: {e}")
            return
        try:
            callback(
                lambda event: handle_chess_ai_move_applied(
                    llm,
                    self.plugin,
                    tts,
                    event,
                )
            )
        except Exception as e:
            log_print(f"[ChessGameExtension] callback registration failed: {e}")
