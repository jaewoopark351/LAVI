#20260717_kpopmodder: Isolates the StarCraft2 GameExtension implementation.
from typing import Any, Dict, Optional

from app_core.extensions import GameExtensionInterface
from app_core.extensions.game_extension_context import GameExtensionContext
from core.logger import log_print

from .starcraft2_bridge import StarCraft2Bridge
from .starcraft2_constants import (
    STARCRAFT2_STATUS_EVENT_CALLBACK_RESOURCE,
    STARCRAFT2_TERMINAL_EVENT_OBSERVER_RESOURCE,
)
from .starcraft2_status_event_subscription import _StarCraft2StatusEventSubscription
from .starcraft2_worker import StarCraft2Worker


class StarCraft2GameExtension(GameExtensionInterface):
    #20260707_kpopmodder: Adapter around new StarCraft2 plugin facade and worker.
    def __init__(self, plugin=None):
        self.plugin = plugin
        self.bridge = StarCraft2Bridge(plugin)
        self.worker = StarCraft2Worker(self.bridge)
        self._context: Optional[GameExtensionContext] = None
        self._is_initialized = False
        self._is_started = False
        self._reaction_callback = None
        self._status_event_subscription = None

    @property
    def name(self) -> str:
        return "starcraft2"

    def initialize(self, context: GameExtensionContext) -> None:
        super().initialize(context)
        self._context = context
        if self.plugin is None:
            self._maybe_build_plugin()
        tts_setter = getattr(self.plugin, "set_tts", None)
        if callable(tts_setter):
            tts_setter(getattr(context, "tts", None))
        self._bind_common_event_bus(context)
        callback = self._build_status_callback(context)
        if callback is not None:
            self._reaction_callback = callback
            self._status_event_subscription = self._bind_status_events(self._on_status_event)
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
        self.mark_started(True)
        self.publish_event("extension_started")
        #20260710_kpopmodder: SC2 commentary is sourced from Ladder Proxy
        # ResponseObservation telemetry; do not use noisy ScreenVision frames.
        if self._auto_launch_enabled():
            self.worker.handle_command({"action": "start"})

    def stop(self) -> None:
        self._is_started = False
        self.mark_started(False)
        try:
            self.bridge.stop_game()
        finally:
            self.worker.stop()

    def shutdown(self) -> None:
        if self._status_event_subscription is not None:
            unsubscribe = getattr(self._status_event_subscription, "unsubscribe", None)
            if callable(unsubscribe):
                unsubscribe()
            self._status_event_subscription = None
        self.stop()

    def _bind_status_events(self, callback):
        plugin = self.plugin
        if plugin is None or not callable(callback):
            return None
        subscribe = getattr(plugin, "subscribe_events", None)
        if callable(subscribe):
            try:
                return subscribe(callback)
            except Exception:
                log_print("[StarCraft2GameExtension] subscribe_events failed; trying alternate subscribe_status_events.")
        subscribe = getattr(plugin, "subscribe_status_events", None)
        if callable(subscribe):
            try:
                return subscribe(callback)
            except Exception:
                log_print("[StarCraft2GameExtension] subscribe_status_events failed.")
        set_status_event_callback = getattr(plugin, "set_status_event_callback", None)
        if callable(set_status_event_callback):
            try:
                set_status_event_callback(callback)
                return _StarCraft2StatusEventSubscription(
                    lambda: set_status_event_callback(None)
                )
            except Exception:
                log_print(
                    "[StarCraft2GameExtension] set_status_event_callback failed."
                )
        log_print("[StarCraft2GameExtension] failed to bind status event path: no callback API on plugin")
        return None

    def _bind_common_event_bus(self, context: Optional[GameExtensionContext]) -> None:
        if context is None or self.plugin is None:
            return
        event_bus = getattr(context, "event_bus", None)
        attach = getattr(self.plugin, "attach_game_event_bus", None)
        if callable(attach):
            attach(event_bus)

    def handle_command(self, command: Any) -> Dict[str, Any]:
        command_dto = self.record_command(command)
        self._ensure_bridge_ready()
        result = self.worker.handle_command(command_dto.to_legacy_dict())
        self.record_result(result, action=command_dto.action)
        return result

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
        runtime_context = getattr(self, "runtime_context", None)
        snapshot = getattr(runtime_context, "snapshot", None)
        if callable(snapshot):
            plugin_status["runtime_context"] = snapshot()
        return self.apply_status_contract(plugin_status)

    def _auto_launch_enabled(self) -> bool:
        config_manager = getattr(self.plugin, "config_manager", None)
        getter = getattr(config_manager, "get_bool", None)
        if not callable(getter):
            return False
        return bool(getter("auto_launch", False))

    def _on_status_event(self, event: Any) -> None:
        event = dict(event or {}) if isinstance(event, dict) else {}
        self._notify_terminal_event_observer(event)
        if callable(self._reaction_callback):
            self._reaction_callback(event)

    def _notify_terminal_event_observer(self, event: Dict[str, Any]) -> None:
        #20260717_kpopmodder: Let the passive SC2 log observer learn ladder
        # proxy terminal events before it tails delayed stderr cleanup lines.
        context = self._context
        get_shared = getattr(context, "get_shared", None)
        observer = (
            get_shared(STARCRAFT2_TERMINAL_EVENT_OBSERVER_RESOURCE)
            if callable(get_shared)
            else None
        )
        if not callable(observer):
            return
        try:
            observer(event)
        except Exception as e:
            log_print(
                "[StarCraft2GameExtension] terminal observer notification failed: "
                f"{e}"
            )

    def _build_status_callback(self, context: Optional[GameExtensionContext]):
        if context is None:
            return None
        llm = getattr(context, "llm", None)
        tts = getattr(context, "tts", None)
        if tts is None:
            log_print("[StarCraft2GameExtension] status callback skipped: tts is required")
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
