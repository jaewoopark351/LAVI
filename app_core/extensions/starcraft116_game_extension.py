#20260706_kpopmodder: Added StarCraft116 adapter wiring for the new extension architecture.
from __future__ import annotations

from typing import Any, Dict, Optional

from core.event_manager import EventType, event_manager
from core.logger import log_print
from app_core.extensions import GameExtensionInterface
from app_core.extensions.game_extension_context import GameExtensionContext

from .starcraft116_bridge import StarCraft116Bridge
from .starcraft116_worker import StarCraft116Worker


class StarCraft116GameExtension(GameExtensionInterface):
    #20260706_kpopmodder: Adapter around existing StarCraft116 plugin without changing legacy behavior.
    def __init__(self, plugin=None):
        self.plugin = plugin
        self.bridge = StarCraft116Bridge(plugin)
        self.worker = StarCraft116Worker(self.bridge)
        self._context: Optional[GameExtensionContext] = None
        self._is_initialized = False
        self._is_started = False
        self._screen_observation_subscription = None
        self._screen_observation_warning_emitted = False

    @property
    def name(self) -> str:
        return "starcraft116"

    def initialize(self, context: GameExtensionContext) -> None:
        self._context = context
        if self.plugin is None:
            self._maybe_build_plugin()

        callback = self._build_status_callback(context)
        if callback is not None and getattr(self.plugin, "status_event_callback", None) is None:
            self.bridge.start_status_listener(callback)
        self._subscribe_screen_observation_events()

        self._is_initialized = True

    def start(self) -> None:
        if self._is_started:
            return
        if not self._is_initialized:
            log_print(
                "[StarCraft116GameExtension] initialize must be called before start"
            )
        self._ensure_bridge_ready()
        if self._context is not None:
            callback = self._build_status_callback(self._context)
            if callback is not None and not self._is_status_listener_set():
                self.bridge.start_status_listener(callback)
            self._subscribe_screen_observation_events()
        try:
            self.bridge.start_status_listener()
            self.worker.start()
            self._is_started = True
        except Exception as e:
            log_print(f"[StarCraft116GameExtension] start failed: {e}")
            raise

    def stop(self) -> None:
        self._is_started = False
        try:
            self.worker.stop()
        finally:
            self.bridge.stop_status_listener()
            self._unsubscribe_screen_observation_events()

    def shutdown(self) -> None:
        self.stop()

    def handle_command(self, command: Any) -> Any:
        return self.worker.handle_command(command)

    #20260706_kpopmodder: Shim path for ScreenVision event->command wiring.
    def _on_screen_observation_event(self, payload: Any = None, **kwargs):
        if not self._is_started:
            return

        event_payload = payload if isinstance(payload, dict) else {}
        if not isinstance(event_payload, dict):
            event_payload = {}
        if kwargs:
            merged = dict(kwargs)
            merged.update(event_payload)
            event_payload = merged

        normalized = self._normalize_screen_observation_payload(event_payload)
        if not normalized:
            return

        result = self.handle_command(
            {
                "action": "screen_observation",
                "source": "screen_vision",
                "payload": normalized,
                "event_type": "screen_observation",
            }
        )
        if not result.get("ok"):
            error = str(result.get("error", ""))
            if (
                error == "screen_observation_handler_missing"
                and not self._screen_observation_warning_emitted
            ):
                self._screen_observation_warning_emitted = True
                log_print(
                    "[StarCraft116GameExtension] screen observation handler not"
                    " implemented in plugin yet; keeping event route active"
                )

    #20260706_kpopmodder: Normalize ScreenVision event payload before enqueueing bridge command.
    def _normalize_screen_observation_payload(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        if not payload:
            return {}

        nested = payload.get("payload")
        merged = {}
        if isinstance(nested, dict):
            merged.update(nested)
        merged.update(payload)

        source = str(
            merged.get("source")
            or merged.get("observer")
            or "screen_vision"
        ).strip()
        question = str(merged.get("question") or "").strip()
        observation = str(merged.get("observation") or "").strip()
        text = str(merged.get("text") or "").strip()

        if not observation and text:
            lines = [line.strip() for line in text.splitlines() if line.strip()]
            if lines:
                observation = lines[0]

        return {
            "source": source,
            "question": question,
            "observation": observation,
            "text": text,
            "display_text": str(merged.get("display_text") or "").strip(),
            "remember_history": bool(merged.get("remember_history", False)),
            "metadata": merged.get("metadata", {}),
            "event_name": "SCREEN_OBSERVATION",
            "event_type": "screen_observation",
        }

    def get_status(self) -> Dict[str, Any]:
        base = {}
        if self.plugin is not None and hasattr(self.plugin, "get_status"):
            try:
                status = self.plugin.get_status()
                if isinstance(status, dict):
                    base = status
            except Exception as e:
                log_print(f"[StarCraft116GameExtension] get_status failed: {e}")
        base["extension"] = {
            "name": self.name,
            "initialized": self._is_initialized,
            "started": self._is_started,
            "bridge_watcher": self.bridge.get_watcher_state(),
        }
        base["worker"] = self.worker.get_status()
        return base

    def _is_status_listener_set(self) -> bool:
        return bool(
            getattr(self.plugin, "status_event_callback", None) is not None
        )

    #20260706_kpopmodder: Keep event route explicit and unsubscribed on stop.
    def _subscribe_screen_observation_events(self):
        if self._screen_observation_subscription is not None:
            return

        self._screen_observation_subscription = event_manager.subscribe(
            EventType.SCREEN_OBSERVATION,
            self._on_screen_observation_event,
        )

    #20260706_kpopmodder: Explicit unsubscribe helps avoid stale callbacks in runtime reboots.
    def _unsubscribe_screen_observation_events(self):
        subscription = self._screen_observation_subscription
        if subscription is None:
            return

        try:
            subscription.unsubscribe()
        except Exception as e:
            log_print(
                f"[StarCraft116GameExtension] unsubscribe screen observation failed: {e}"
            )
        finally:
            self._screen_observation_subscription = None

    def _build_status_callback(self, context: Optional[GameExtensionContext]):
        if context is None:
            return None
        llm = getattr(context, "llm", None)
        tts = getattr(context, "tts", None)
        if llm is None or tts is None:
            return None
        try:
            from plugins.StarCraft116.starcraft116_core.starcraft116_reaction_runtime import (
                build_starcraft116_status_event_callback,
            )
        except Exception as e:
            log_print(f"[StarCraft116GameExtension] callback builder import failed: {e}")
            return None
        try:
            return build_starcraft116_status_event_callback(llm, tts)
        except Exception as e:
            log_print(
                f"[StarCraft116GameExtension] build callback failed: {e}"
            )
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
            from plugins.StarCraft116.starcraft116 import StarCraft116
        except Exception as e:
            raise RuntimeError("StarCraft116 plugin could not be imported") from e
        self.plugin = StarCraft116()
        self.bridge.plugin = self.plugin
        self.worker.bridge = self.bridge
