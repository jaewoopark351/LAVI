#20260713_kpopmodder: Extract facade orchestration for start/stop/status from UI surface.
from __future__ import annotations

import json
import time
from typing import Any, Dict, Optional

from core.logger import log_print
from .starcraft2_contracts import (
    LocalMatchRuntimeStatusDTO,
    StartResultDTO,
    StopResultDTO,
)
from .starcraft2_runtime_context import SC2RuntimeContext
from .starcraft2_event_bus import StarCraft2EventBus
from .starcraft2_local_match_service import _StarCraft2LocalMatchService


class StarCraft2FacadeService:
    #20260713_kpopmodder: Public orchestration boundary between UI callbacks and SC2 domain services.
    def __init__(
        self,
        config_manager,
        engine_registry,
        state,
        ladder_proxy,
        match_config_service,
        engine_event_service,
        local_match_service: Optional[_StarCraft2LocalMatchService] = None,
        event_bus: StarCraft2EventBus | None = None,
        runtime_context: Optional[SC2RuntimeContext] = None,
    ):
        self.config_manager = config_manager
        self.engine_registry = engine_registry
        self.state = state
        self.ladder_proxy = ladder_proxy
        self.match_config_service = match_config_service
        self.engine_event_service = engine_event_service
        self.local_match_service = local_match_service
        self.runtime_context = runtime_context or SC2RuntimeContext()
        self.event_bus = event_bus
        if self.event_bus is None and engine_event_service is not None:
            self.event_bus = getattr(engine_event_service, "event_bus", None)
        if self.runtime_context is not None and self.event_bus is not None:
            self.runtime_context.event_bus = self.event_bus
        self.current_engine = None
        self.status_event_callback = None
        self.tts = None
        self.last_start_result: Dict[str, Any] = {}
        self.last_stop_result: Dict[str, Any] = {}
        self._last_start_result_dto: Optional[StartResultDTO] = None
        self._last_stop_result_dto: Optional[StopResultDTO] = None
        self._shutdown = False

    def start(
        self,
        config_overrides: Optional[Dict[str, Any]] = None,
        launch_source: str = "manual",
    ):
        runtime_config = self.config_manager.build_runtime_config(config_overrides or {})
        if not bool(runtime_config.get("enabled", False)):
            result = self._facade_result(False, "enabled_false")
            return self._remember_start_result(result)
        if launch_source == "startup" and not bool(
            runtime_config.get("auto_launch", False)
        ):
            result = self._facade_result(True, None, {"skipped": "auto_launch_false"})
            return self._remember_start_result(result)

        engine_name = str(runtime_config.get("engine") or "internal_lav_bot")
        if self.current_engine is None or self.current_engine.engine_name != engine_name:
            self.current_engine = self.engine_registry.create(engine_name)
        result = self.current_engine.start(
            runtime_config,
            event_callback=self.handle_engine_event,
        )
        facade_result = StartResultDTO.from_mapping(result, action="start")
        self._sync_runtime_context(facade_result.to_dict())
        self._remember_start_result(facade_result)
        self._sync_state_from_engine()
        return self.last_start_result

    def stop(self):
        if self.current_engine is None:
            self.state.mark_stopped("not_running")
            result = self._facade_result(
                True,
                None,
                {"stopped": "not_running"},
                stopped=True,
                action="stop",
            )
            return self._remember_stop_result(result)

        result = StopResultDTO.from_mapping(
            self.current_engine.stop(),
            action="stop",
        )
        result_payload = result.to_dict()
        self._sync_runtime_context(result_payload)
        self._remember_stop_result(result)
        self._sync_state_from_engine()
        return self.last_stop_result

    def status(self) -> Dict[str, Any]:
        return self.get_status()

    def start_local_match(
        self,
        executable_path,
        working_directory,
        args,
        proxy_ports,
        ai_race=None,
    ):
        if self.local_match_service is None:
            return self._local_match_missing_status()
        return self.local_match_service.start_local_match(
            executable_path,
            working_directory,
            args,
            proxy_ports,
            ai_race=ai_race,
        )

    def stop_local_match(self):
        if self.local_match_service is None:
            return self._local_match_missing_status()
        return self.local_match_service.stop_local_match()

    def get_local_match_status(
        self,
        executable_path=None,
        working_directory=None,
        args=None,
        proxy_ports=None,
    ):
        if self.local_match_service is None:
            return self._local_match_missing_status()
        return self.local_match_service.get_local_match_status(
            executable_path=executable_path,
            working_directory=working_directory,
            args=args,
            proxy_ports=proxy_ports,
        )

    def local_match_status(self):
        return self.get_local_match_status()

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

    def ui_values(self, result=None):
        status = self.get_status()
        last_event = status.get("state", {}).get("last_event") or {}
        last_error = (
            (result or {}).get("error")
            if isinstance(result, dict)
            else None
        ) or status.get("state", {}).get("last_error") or ""
        return (
            self.config_manager.config_message(),
            self.status_json(status),
            json.dumps(last_event, ensure_ascii=False, indent=2, default=str),
            str(last_error or ""),
        )

    def set_status_event_callback(self, callback):
        self.status_event_callback = callback
        if self.engine_event_service is not None:
            self.engine_event_service.set_status_event_callback(callback)

    def subscribe_status_events(self, callback):
        if self.event_bus is None:
            return None
        return self.event_bus.subscribe(callback)

    def set_tts(self, tts):
        self.tts = tts

    def is_running(self) -> bool:
        return bool(self.current_engine and self.current_engine.is_running())

    def handle_engine_event(self, event):
        self.engine_event_service.update_state(event)

    def on_local_match_race_change(self, race, args):
        if self.local_match_service is None:
            return ""
        return self.local_match_service.on_local_match_race_change(race, args)

    def on_local_match_ai_race_change(self, ai_race, args):
        if self.local_match_service is None:
            return ""
        return self.local_match_service.on_local_match_ai_race_change(ai_race, args)

    def on_local_human_vs_changeling_click(
        self,
        executable_path,
        working_directory,
        args,
        proxy_ports,
        ai_race=None,
    ):
        return self.local_match_status_json(
            result=self.start_local_match(
                executable_path=executable_path,
                working_directory=working_directory,
                args=args,
                proxy_ports=proxy_ports,
                ai_race=ai_race,
            )
        )

    def on_local_match_stop_click(self):
        return self.local_match_status_json(
            result=self.stop_local_match()
        )

    def on_local_match_status_click(
        self,
        executable_path,
        working_directory,
        args,
        proxy_ports,
    ):
        status = self.get_local_match_status(
            executable_path=executable_path,
            working_directory=working_directory,
            args=args,
            proxy_ports=proxy_ports,
        )
        return self.local_match_status_json(
            ladder_proxy_config=None,
            result=status,
        )

    def local_match_status_json(self, ladder_proxy_config=None, result=None):
        if self.local_match_service is None:
            return json.dumps(
                {"ok": False, "error": "local_match_service_missing"},
                ensure_ascii=False,
                indent=2,
                default=str,
            )
        return self.local_match_service.local_match_status_json(
            ladder_proxy_config=ladder_proxy_config,
            result=result,
        )

    def local_match_race_from_args(self, args, fallback: str = "Terran") -> str:
        if self.local_match_service is None:
            return str(fallback or "Terran")
        return self.local_match_service.local_match_race_from_args(args, fallback=fallback)

    def local_match_ai_race_from_args(self, args, fallback: str = "Zerg") -> str:
        if self.local_match_service is None:
            return str(fallback or "Zerg")
        return self.local_match_service.local_match_ai_race_from_args(
            args, fallback=fallback
        )

    def _local_match_missing_status(self) -> LocalMatchRuntimeStatusDTO:
        return LocalMatchRuntimeStatusDTO(
            mode="local_human_vs_changeling",
            result=StartResultDTO(
                ok=False,
                action="local_human_vs_changeling",
                error="local_match_service_missing",
                message="local_match_service_missing",
            ),
            ladder_proxy={
                "ok": False,
                "running": False,
                "status": {"error": "local_match_service_missing"},
            },
        )

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
            self._sync_runtime_context(status=status)

    def _sync_runtime_context(self, status: Dict[str, Any] | None = None):
        if self.runtime_context is None:
            return
        if isinstance(status, dict):
            if "status" in status and isinstance(status.get("status"), dict):
                status_payload = dict(status.get("status") or {})
                running = bool(
                    status_payload.get("running")
                    or status.get("running")
                    or self.current_engine.is_running()
                )
            else:
                status_payload = dict(status)
                running = bool(status_payload.get("running", False))
        else:
            status_payload = {}
            running = bool(self.current_engine.is_running() if self.current_engine else False)
        self.runtime_context.set_status(status_payload)
        if self.current_engine is not None and running:
            if self.runtime_context.started_at is None:
                self.runtime_context.started_at = time.time()
            self.runtime_context.set_process(
                getattr(self.current_engine, "process", None),
                f"engine:{self.current_engine.engine_name}",
            )
            self.runtime_context.set_tails(
                status_payload.get("stdout_tail", []),
                status_payload.get("stderr_tail", []),
            )
            self.runtime_context.runtime_error = None
            return

        if self.runtime_context.stopped_at is None:
            self.runtime_context.stopped_at = time.time()
        self.runtime_context.clear_process()
        self.runtime_context.runtime_error = None if status_payload.get("error") is None else str(status_payload.get("error"))

    def _remember_start_result(self, result: StartResultDTO) -> Dict[str, Any]:
        self._last_start_result_dto = result
        self.last_start_result = result.to_dict()
        return self.last_start_result

    def _remember_stop_result(self, result: StopResultDTO) -> Dict[str, Any]:
        self._last_stop_result_dto = result
        self.last_stop_result = result.to_dict()
        return self.last_stop_result

    def _facade_result(
        self,
        ok: bool,
        error=None,
        status=None,
        stopped: bool = False,
        action: str = "start",
    ):
        if action == "stop" or stopped:
            return StopResultDTO(
                ok=bool(ok),
                running=self.is_running(),
                action=action,
                stopped=bool(stopped),
                status=dict(status or {}),
                error=None if error is None else str(error),
                details={},
            )
        return StartResultDTO(
            ok=bool(ok),
            running=self.is_running(),
            action=action,
            status=dict(status or {}),
            error=None if error is None else str(error),
            details={},
        )


_StarCraft2FacadeService = StarCraft2FacadeService
