#20260707_kpopmodder: Added StarCraft2 GameExtension wiring for the extension registry.
from __future__ import annotations

from typing import Any, Dict, Optional

from app_core.extensions import GameExtensionInterface
from app_core.extensions.game_extension_context import GameExtensionContext
from core.logger import log_print

from .starcraft2_bridge import StarCraft2Bridge
from .starcraft2_worker import StarCraft2Worker


STARCRAFT2_STATUS_EVENT_CALLBACK_RESOURCE = "starcraft2_status_event_callback"
STARCRAFT2_LOG_EVENT_ORIGIN = "starcraft2_log_observer"
STARCRAFT2_TERMINAL_EVENT_TYPES = {
    "game_ended", "game_won", "game_lost", "engine_error", "error",
}


class StarCraft2GameExtension(GameExtensionInterface):
    #20260707_kpopmodder: Adapter around new StarCraft2 plugin facade and worker.
    def __init__(self, plugin=None):
        self.plugin = plugin
        self.bridge = StarCraft2Bridge(plugin)
        self.worker = StarCraft2Worker(self.bridge)
        self._context: Optional[GameExtensionContext] = None
        self._is_initialized = False
        self._is_started = False
        self._game_active = False
        self._game_end_cancelled = False
        self._suppress_post_game_tts = False
        self._reaction_callback = None

    @property
    def name(self) -> str:
        return "starcraft2"

    def initialize(self, context: GameExtensionContext) -> None:
        self._context = context
        if self.plugin is None:
            self._maybe_build_plugin()
        tts_setter = getattr(self.plugin, "set_tts", None)
        if callable(tts_setter):
            tts_setter(getattr(context, "tts", None))
        callback = self._build_status_callback(context)
        if callback is not None:
            self._reaction_callback = callback
            setter = getattr(self.plugin, "set_status_event_callback", None)
            if callable(setter):
                setter(self._on_status_event)
        #20260711_kpopmodder: Publish the common SC2 status callback so the
        # passive log observer can reuse the main event/memory/TTS path.
        set_shared = getattr(context, "set_shared", None)
        if callable(set_shared):
            set_shared(
                STARCRAFT2_STATUS_EVENT_CALLBACK_RESOURCE,
                self._on_status_event,
            )
        self._is_initialized = True

    def start(self) -> None:
        if self._is_started:
            return
        if not self._is_initialized:
            log_print("[StarCraft2GameExtension] initialize must be called before start")
        self._ensure_bridge_ready()
        self.worker.start()
        self._is_started = True
        #20260710_kpopmodder: SC2 commentary is sourced from Ladder Proxy
        # ResponseObservation telemetry; do not use noisy ScreenVision frames.
        if self._auto_launch_enabled():
            self.worker.handle_command({"action": "start"})

    def stop(self) -> None:
        self._is_started = False
        self._game_active = False
        try:
            self.bridge.stop_game()
        finally:
            self.worker.stop()

    def shutdown(self) -> None:
        self.stop()

    def handle_command(self, command: Any) -> Dict[str, Any]:
        self._ensure_bridge_ready()
        return self.worker.handle_command(command)

    def get_status(self) -> Dict[str, Any]:
        plugin_status = {}
        if self.plugin is not None:
            try:
                status = self.plugin.get_status()
                if isinstance(status, dict):
                    plugin_status = status
            except Exception as e:
                plugin_status = {"error": str(e)}
        plugin_status["extension"] = {
            "name": self.name,
            "initialized": self._is_initialized,
            "started": self._is_started,
        }
        plugin_status["worker"] = self.worker.get_status()
        return plugin_status

    def _auto_launch_enabled(self) -> bool:
        config_manager = getattr(self.plugin, "config_manager", None)
        getter = getattr(config_manager, "get_bool", None)
        if not callable(getter):
            return False
        return bool(getter("auto_launch", False))

    def _on_status_event(self, event: Any) -> None:
        event = dict(event or {}) if isinstance(event, dict) else {}
        event_type = str(event.get("event_type") or "").strip().lower()
        if event_type == "game_started":
            self._game_active = True
            self._game_end_cancelled = False
            self._suppress_post_game_tts = False
        elif event_type in STARCRAFT2_TERMINAL_EVENT_TYPES:
            self._game_active = False
            self._suppress_post_game_tts = True
            if not self._game_end_cancelled:
                self._cancel_pending_tts(event_type)
                self._game_end_cancelled = True
        elif self._suppress_post_game_tts:
            #20260711_kpopmodder: Preserve late post-game telemetry in logs and
            # raw memory while preventing it from refilling the spoken queue.
            raw_details = event.get("details")
            details = dict(raw_details) if isinstance(raw_details, dict) else {}
            details["speak"] = False
            event["details"] = details
            log_print(
                "[StarCraft2GameExtension] post-game TTS suppressed: "
                f"event={event_type}"
            )
        if callable(self._reaction_callback):
            # The shared callback owns both policy admission and TTS. A false
            # result may mean the event was intentionally rate-limited; never
            # bypass that decision with a second speech path.
            self._reaction_callback(event)
        else:
            self._speak_structured_log_event(event)

    def _cancel_pending_tts(self, event_type: str) -> bool:
        tts = getattr(self._context, "tts", None) if self._context is not None else None
        cancel_pending = getattr(tts, "cancel_pending", None)
        try:
            if callable(cancel_pending):
                cancel_pending(reason=f"starcraft2_{event_type}")
                log_print(
                    "[StarCraft2GameExtension] pending TTS cancelled: "
                    f"event={event_type}"
                )
                return True

            handle_interrupt = getattr(tts, "handle_interrupt", None)
            if callable(handle_interrupt):
                handle_interrupt()
                log_print(
                    "[StarCraft2GameExtension] pending TTS interrupted via fallback: "
                    f"event={event_type}"
                )
                return True
        except Exception as e:
            log_print(
                "[StarCraft2GameExtension] pending TTS cancellation failed: "
                f"event={event_type} error={e}"
            )
        return False

    def _speak_structured_log_event(self, event: Any) -> bool:
        event = event if isinstance(event, dict) else {}
        event_type = str(event.get("event_type") or "").strip().lower()
        details = event.get("details") if isinstance(event.get("details"), dict) else {}
        if event_type not in {"upgrade", "strategy"}:
            return False
        if str(details.get("origin") or "") != STARCRAFT2_LOG_EVENT_ORIGIN:
            return False
        if details.get("speak") is False:
            return False
        message = str(details.get("message") or "").strip()
        tts = getattr(self._context, "tts", None) if self._context is not None else None
        receive_input = getattr(tts, "receive_input", None)
        if not message or not callable(receive_input):
            return False
        #20260711_kpopmodder: The current reaction policy does not yet render
        # upgrade/strategy events, so keep their structured main-path fallback
        # here without bypassing the shared callback or duplicating telemetry.
        receive_input(message)
        return True

    def _build_status_callback(self, context: Optional[GameExtensionContext]):
        if context is None:
            return None
        llm = getattr(context, "llm", None)
        tts = getattr(context, "tts", None)
        if llm is None or tts is None:
            return None
        try:
            from plugins.StarCraft2.starcraft2_core.starcraft2_reaction_runtime import (
                build_starcraft2_status_event_callback,
            )
        except Exception as e:
            log_print(f"[StarCraft2GameExtension] callback builder import failed: {e}")
            return None
        try:
            return build_starcraft2_status_event_callback(
                llm,
                tts,
                memory_store=getattr(context, "memory_store", None),
            )
        except Exception as e:
            log_print(f"[StarCraft2GameExtension] build callback failed: {e}")
            return None

    def _ensure_bridge_ready(self):
        if self.plugin is None:
            self._maybe_build_plugin()
        if self.bridge.plugin is None:
            self.bridge.plugin = self.plugin
        if self.worker.bridge is None:
            self.worker.bridge = self.bridge

    def _maybe_build_plugin(self):
        try:
            from plugins.StarCraft2.starcraft2 import StarCraft2
        except Exception as e:
            raise RuntimeError("StarCraft2 plugin could not be imported") from e
        self.plugin = StarCraft2()
        self.bridge.plugin = self.plugin
        self.worker.bridge = self.bridge
