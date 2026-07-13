#20260713_kpopmodder: Extract facade orchestration for start/stop/status from UI surface.
from __future__ import annotations

import json
from typing import Any, Dict, Optional

from core.logger import log_print
from .starcraft2_event_bus import _StarCraft2EventBus


class _StarCraft2FacadeService:
    def __init__(
        self,
        config_manager,
        engine_registry,
        state,
        ladder_proxy,
        match_config_service,
        engine_event_service,
        event_bus: _StarCraft2EventBus | None = None,
    ):
        self.config_manager = config_manager
        self.engine_registry = engine_registry
        self.state = state
        self.ladder_proxy = ladder_proxy
        self.match_config_service = match_config_service
        self.engine_event_service = engine_event_service
        self.event_bus = event_bus
        if self.event_bus is None and engine_event_service is not None:
            self.event_bus = getattr(engine_event_service, "event_bus", None)
        self.current_engine = None
        self.status_event_callback = None
        self.tts = None
        self.last_start_result: Dict[str, Any] = {}
        self.last_stop_result: Dict[str, Any] = {}
        self._shutdown = False

    def start(
        self,
        config_overrides: Optional[Dict[str, Any]] = None,
        launch_source: str = "manual",
    ):
        runtime_config = self.config_manager.build_runtime_config(config_overrides or {})
        if not bool(runtime_config.get("enabled", False)):
            result = self._facade_result(False, "enabled_false")
            self.last_start_result = result
            return result
        if launch_source == "startup" and not bool(
            runtime_config.get("auto_launch", False)
        ):
            result = self._facade_result(True, None, {"skipped": "auto_launch_false"})
            self.last_start_result = result
            return result

        engine_name = str(runtime_config.get("engine") or "internal_lav_bot")
        if self.current_engine is None or self.current_engine.engine_name != engine_name:
            self.current_engine = self.engine_registry.create(engine_name)
        result = self.current_engine.start(
            runtime_config,
            event_callback=self.handle_engine_event,
        )
        self.last_start_result = result
        self._sync_state_from_engine()
        return result

    def stop(self):
        if self.current_engine is None:
            self.state.mark_stopped("not_running")
            result = self._facade_result(True, None, {"stopped": "not_running"})
            self.last_stop_result = result
            return result

        result = self.current_engine.stop()
        self.last_stop_result = result
        self._sync_state_from_engine()
        return result

    def shutdown(self):
        if self._shutdown:
            return
        self._shutdown = True
        self.ladder_proxy.stop()
        try:
            self.stop()
        except Exception as e:
            log_print(f"[StarCraft2] shutdown failed: {e}")

    def get_status(self) -> Dict[str, Any]:
        self._sync_state_from_engine()
        return {
            "enabled": self.config_manager.get_bool("enabled", False),
            "engine": str(self.config_manager.get("engine", "internal_lav_bot")),
            "config": self.config_manager.config_message(),
            "state": self.state.to_dict(),
            "engine_status": (
                self.current_engine.get_status()
                if self.current_engine is not None
                else {}
            ),
            "last_start_result": dict(self.last_start_result or {}),
            "last_stop_result": dict(self.last_stop_result or {}),
            "ladder_proxy": self.ladder_proxy.get_status(
                self.match_config_service.ladder_proxy_config()
            ),
        }

    def status_json(self, status=None) -> str:
        return json.dumps(
            status or self.get_status(),
            ensure_ascii=False,
            indent=2,
            default=str,
        )

    def set_status_event_callback(self, callback):
        self.status_event_callback = callback
        if self.engine_event_service is not None:
            self.engine_event_service.set_status_event_callback(callback)
        if self.event_bus is not None:
            self.event_bus.set_status_event_callback(callback)

    def set_tts(self, tts):
        self.tts = tts
        if self.event_bus is not None:
            self.event_bus.set_tts(tts)

    def is_running(self) -> bool:
        return bool(self.current_engine and self.current_engine.is_running())

    def handle_engine_event(self, event):
        self.engine_event_service.update_state(event)

    def sync_state_from_engine(self):
        self._sync_state_from_engine()

    def _sync_state_from_engine(self):
        if self.current_engine is None:
            return
        try:
            status = self.current_engine.get_status()
        except Exception as e:
            self.state.mark_error(e)
            return
        if isinstance(status, dict):
            self.state.running = bool(status.get("running", self.current_engine.is_running()))
            self.state.last_error = status.get("last_error") or self.state.last_error
            self.state.last_event = status.get("last_event") or self.state.last_event
            self.state.process_pid = status.get("process_pid")
            self.state.stdout_tail = list(status.get("stdout_tail") or [])[-20:]
            self.state.stderr_tail = list(status.get("stderr_tail") or [])[-20:]

    def _facade_result(self, ok: bool, error=None, status=None):
        return {
            "ok": bool(ok),
            "engine": str(self.config_manager.get("engine", "internal_lav_bot")),
            "running": self.is_running(),
            "status": status or {},
            "error": None if error is None else str(error),
        }
