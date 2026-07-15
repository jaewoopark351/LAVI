#20260713_kpopmodder: Extract facade orchestration for start/stop/status from UI surface.
from __future__ import annotations

import json
import time
from typing import Any, Dict, Optional

from app_core.extensions import GameStartResultDTO, GameStatusDTO, GameStopResultDTO
from core.logger import log_print
from .starcraft2_contracts import (
    EngineResultDTO,
    EngineStartCommandDTO,
    EngineStatusDTO,
    LadderProxyStatusDTO,
    LocalMatchLaunchConfigDTO,
    LocalMatchRuntimeStatusDTO,
    StartResultDTO,
    StopResultDTO,
    StarCraft2Event,
)
from .starcraft2_engine_interface import adapt_starcraft2_engine
from .starcraft2_runtime_context import SC2RuntimeContext
from .starcraft2_event_bus import StarCraft2EventBus
from .starcraft2_local_match_service import StarCraft2LocalMatchService


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
        local_match_service: Optional[StarCraft2LocalMatchService] = None,
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
        self._runtime_event_subscription = (
            self.event_bus.subscribe_typed(self._handle_runtime_event)
            if self.event_bus is not None
            else None
        )
        self.current_engine = None
        self.status_event_callback = None
        self.tts = None
        self.last_start_result: Dict[str, Any] = {}
        self.last_stop_result: Dict[str, Any] = {}
        self._last_start_result_dto: Optional[StartResultDTO] = None
        self._last_stop_result_dto: Optional[StopResultDTO] = None
        self._last_game_start_result_dto: Optional[GameStartResultDTO] = None
        self._last_game_stop_result_dto: Optional[GameStopResultDTO] = None
        self._last_game_status_dto: Optional[GameStatusDTO] = None
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
            self.current_engine = adapt_starcraft2_engine(
                self.engine_registry.create(engine_name)
            )
        result = self.current_engine.start(
            EngineStartCommandDTO.from_mapping(runtime_config),
            event_callback=self.handle_engine_event,
        )
        result_payload = (
            result.to_dict() if isinstance(result, EngineResultDTO) else result
        )
        facade_result = StartResultDTO.from_mapping(result_payload, action="start")
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

        engine_result = self.current_engine.stop()
        result = StopResultDTO.from_mapping(
            engine_result.to_dict()
            if isinstance(engine_result, EngineResultDTO)
            else engine_result,
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
        result = self.local_match_service.start_local_match(
            executable_path,
            working_directory,
            args,
            proxy_ports,
            ai_race=ai_race,
        )
        self._sync_local_match_runtime_context(
            result=result,
            config=self.match_config_service.local_match_config(
                executable_path=executable_path,
                working_directory=working_directory,
                args=args,
                proxy_ports=proxy_ports,
            ),
        )
        return result

    def stop_local_match(self):
        if self.local_match_service is None:
            return self._local_match_missing_status()
        result = self.local_match_service.stop_local_match()
        self._sync_local_match_runtime_context(result=result)
        return result

    def get_local_match_status(
        self,
        executable_path=None,
        working_directory=None,
        args=None,
        proxy_ports=None,
    ):
        if self.local_match_service is None:
            return self._local_match_missing_status()
        result = self.local_match_service.get_local_match_status(
            executable_path=executable_path,
            working_directory=working_directory,
            args=args,
            proxy_ports=proxy_ports,
        )
        self._sync_local_match_runtime_context(
            result=result,
            config=self.match_config_service.local_match_config(
                executable_path=executable_path,
                working_directory=working_directory,
                args=args,
                proxy_ports=proxy_ports,
            ),
        )
        return result

    def local_match_status(self):
        return self.get_local_match_status()

    def shutdown(self):
        if self._shutdown:
            return
        self._shutdown = True
        #20260715_kpopmodder: Keep the proxy stop result inside Facade-owned runtime state.
        proxy_stop_result = self.ladder_proxy.stop()
        self._sync_local_match_runtime_context(result=proxy_stop_result)
        subscription = self._runtime_event_subscription
        if subscription is not None:
            subscription.unsubscribe()
            self._runtime_event_subscription = None
        try:
            self.stop()
        except Exception as e:
            log_print(f"[StarCraft2] shutdown failed: {e}")

    def get_status(self) -> Dict[str, Any]:
        self._sync_state_from_engine()
        ladder_proxy_status = LadderProxyStatusDTO.from_mapping(
            self.ladder_proxy.get_status(
                LocalMatchLaunchConfigDTO.from_mapping(
                    self.match_config_service.ladder_proxy_config()
                )
            )
        ).to_dict()
        status = {
            "enabled": self.config_manager.get_bool("enabled", False),
            "engine": str(self.config_manager.get("engine", "internal_lav_bot")),
            "config": self.config_manager.config_message(),
            "state": self.state.to_dict(),
            "engine_status": (
                self._engine_status_payload()
                if self.current_engine is not None
                else {}
            ),
            "last_start_result": dict(self.last_start_result or {}),
            "last_stop_result": dict(self.last_stop_result or {}),
            "ladder_proxy": ladder_proxy_status,
        }
        game_status = self._build_game_status_dto(status)
        status["game_status"] = game_status.to_dict()
        return status

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

    def _handle_runtime_event(self, event: StarCraft2Event) -> None:
        normalized = StarCraft2Event.from_mapping(event)
        event_type = normalized.event_type.strip().lower()
        if event_type != "proxy_stopped":
            return
        self._sync_local_match_runtime_context(exit_details=normalized.details)

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

    #20260715_kpopmodder: Keep legacy UI helper names delegated through the facade boundary.
    def ladder_proxy_config(
        self,
        executable_path=None,
        working_directory=None,
        args=None,
        proxy_host=None,
        proxy_ports=None,
    ) -> Dict[str, Any]:
        return self.match_config_service.ladder_proxy_config(
            executable_path=executable_path,
            working_directory=working_directory,
            args=args,
            proxy_host=proxy_host,
            proxy_ports=proxy_ports,
        )

    def local_match_config(
        self,
        executable_path=None,
        working_directory=None,
        args=None,
        proxy_ports=None,
        keep_local_match_identity_args: bool = False,
    ) -> Dict[str, Any]:
        return self.match_config_service.local_match_config(
            executable_path=executable_path,
            working_directory=working_directory,
            args=args,
            proxy_ports=proxy_ports,
            keep_local_match_identity_args=keep_local_match_identity_args,
        )

    def ensure_local_match_runtime(self, config: Dict[str, Any]) -> Dict[str, Any]:
        return self.match_config_service.ensure_local_match_runtime(config)

    def same_path(self, left: str, right: str) -> bool:
        return self._arg_utils().same_path(left, right)

    def config_bool(self, value: Any, default: bool = False) -> bool:
        return self._arg_utils().config_bool(value, default=default)

    def float_config_value(self, value: Any, default: float) -> float:
        return self._arg_utils().float_config_value(value, default)

    def normalize_ladder_args(self, value: Any) -> list[str]:
        return self._arg_utils().normalize_ladder_args(value)

    def has_arg(self, args: list[str], name: str) -> bool:
        return self._arg_utils().has_arg(args, name)

    def ladder_arg_value(self, args: list[str], name: str, fallback: str = "") -> str:
        return self._arg_utils().ladder_arg_value(args, name, fallback=fallback)

    def normalize_sc2_race(self, value: Any, fallback: str = "Random") -> str:
        return self._arg_utils().normalize_sc2_race(value, fallback=fallback)

    def strip_local_match_args(self, args: list[str]) -> list[str]:
        return self._arg_utils().strip_local_match_args(args)

    def strip_ladder_args(self, args: list[str], names: set[str]) -> list[str]:
        return self._arg_utils().strip_ladder_args(args, names)

    def _arg_utils(self):
        arg_utils = getattr(self.match_config_service, "arg_utils", None)
        if arg_utils is None:
            raise RuntimeError("starcraft2_arg_utils_missing")
        return arg_utils

    def _local_match_missing_status(self) -> LocalMatchRuntimeStatusDTO:
        result = StartResultDTO(
            ok=False,
            action="local_human_vs_changeling",
            error="local_match_service_missing",
            message="local_match_service_missing",
        )
        game_result = GameStartResultDTO.from_mapping(
            result.to_dict(),
            action="local_human_vs_changeling",
        )
        game_status = GameStatusDTO.from_mapping(
            {
                "name": "starcraft2",
                "initialized": True,
                "started": False,
                "details": {"mode": "local_human_vs_changeling"},
                "error": "local_match_service_missing",
            },
            name="starcraft2",
        )
        return LocalMatchRuntimeStatusDTO(
            mode="local_human_vs_changeling",
            result=result,
            ladder_proxy={
                "ok": False,
                "running": False,
                "status": {"error": "local_match_service_missing"},
            },
            game_result=game_result,
            game_status=game_status,
        )

    def sync_state_from_engine(self):
        self._sync_state_from_engine()

    def _sync_state_from_engine(self):
        if self.current_engine is None:
            return
        try:
            status = self._engine_status_payload()
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

    def _engine_status_payload(self) -> Dict[str, Any]:
        if self.current_engine is None:
            return {}
        status = self.current_engine.get_status()
        if isinstance(status, EngineStatusDTO):
            return status.to_dict()
        return dict(status) if isinstance(status, dict) else {}

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

    #20260715_kpopmodder: Keep Facade as the sole writer of local-match runtime state.
    def _sync_local_match_runtime_context(
        self,
        result: Any = None,
        config: Optional[Dict[str, Any]] = None,
        exit_details: Optional[Dict[str, Any]] = None,
    ) -> None:
        if self.runtime_context is None:
            return
        runtime_config = (
            config
            if isinstance(config, dict)
            else self.match_config_service.local_match_config()
        )
        runtime_status = LadderProxyStatusDTO.from_mapping(
            self.ladder_proxy.get_status(
                LocalMatchLaunchConfigDTO.from_mapping(runtime_config)
            )
        ).to_dict()
        self.runtime_context.set_status(runtime_status)
        self.runtime_context.set_tails(
            runtime_status.get("stdout_tail", []),
            runtime_status.get("stderr_tail", []),
        )
        validation = runtime_status.get("validation")
        validation = validation if isinstance(validation, dict) else {}
        timeout = validation.get("connect_timeout_sec")
        if timeout is not None:
            try:
                self.runtime_context.timeout_sec = float(timeout)
            except (TypeError, ValueError):
                pass
        self.runtime_context.check_hosts = self._normalize_runtime_hosts(runtime_config)
        ports = self._normalize_runtime_ports(runtime_status, runtime_config)
        if ports:
            self.runtime_context.ports = ports

        if bool(runtime_status.get("running")):
            self.runtime_context.set_process(
                getattr(self.ladder_proxy, "process", None),
                "local_match_proxy",
            )
            self.runtime_context.started_at = (
                getattr(self.ladder_proxy, "started_at", None)
                or self.runtime_context.started_at
                or time.time()
            )
            self.runtime_context.stopped_at = None
            self.runtime_context.runtime_error = None
            return

        self.runtime_context.clear_process()
        if self.runtime_context.stopped_at is None:
            self.runtime_context.stopped_at = time.time()
        result_payload = result.to_dict() if hasattr(result, "to_dict") else result
        result_payload = result_payload if isinstance(result_payload, dict) else {}
        details = exit_details if isinstance(exit_details, dict) else {}
        returncode = details.get("returncode", runtime_status.get("returncode"))
        error = result_payload.get("error") or runtime_status.get("last_error")
        if error:
            self.runtime_context.runtime_error = str(error)
        elif returncode not in (0, None):
            self.runtime_context.runtime_error = f"proxy_exit_{returncode}"
        else:
            self.runtime_context.runtime_error = None

    @staticmethod
    def _normalize_runtime_hosts(config: Dict[str, Any]) -> list[str]:
        raw_hosts = config.get("check_hosts", ["127.0.0.1"])
        if isinstance(raw_hosts, str):
            raw_hosts = [part.strip() for part in raw_hosts.split(",")]
        if not isinstance(raw_hosts, (list, tuple)):
            return ["127.0.0.1"]
        hosts = [str(item).strip() for item in raw_hosts if str(item).strip()]
        return hosts or ["127.0.0.1"]

    @staticmethod
    def _normalize_runtime_ports(
        runtime_status: Dict[str, Any],
        config: Dict[str, Any],
    ) -> list[int]:
        candidate = config.get("ports")
        if candidate is None:
            candidate = runtime_status.get("ports")
        if candidate is None:
            validation = runtime_status.get("validation")
            candidate = validation.get("ports") if isinstance(validation, dict) else None
        if isinstance(candidate, dict):
            candidate = candidate.get("ports")
        if isinstance(candidate, str):
            candidate = [part.strip() for part in candidate.split(",")]
        if not isinstance(candidate, (list, tuple)):
            return []
        ports = []
        for item in candidate:
            try:
                ports.append(int(item))
            except (TypeError, ValueError):
                continue
        return ports

    def _remember_start_result(self, result: StartResultDTO) -> Dict[str, Any]:
        self._last_start_result_dto = result
        self._last_game_start_result_dto = GameStartResultDTO.from_mapping(
            result.to_dict(),
            action=result.action,
        )
        self.last_start_result = result.to_dict()
        self.last_start_result["game_result"] = (
            self._last_game_start_result_dto.to_dict()
        )
        return self.last_start_result

    def _remember_stop_result(self, result: StopResultDTO) -> Dict[str, Any]:
        self._last_stop_result_dto = result
        self._last_game_stop_result_dto = GameStopResultDTO.from_mapping(
            result.to_dict(),
            action=result.action,
        )
        self.last_stop_result = result.to_dict()
        self.last_stop_result["game_result"] = self._last_game_stop_result_dto.to_dict()
        return self.last_stop_result

    def _build_game_status_dto(self, status: Dict[str, Any]) -> GameStatusDTO:
        state = status.get("state") if isinstance(status, dict) else {}
        state = state if isinstance(state, dict) else {}
        engine_status = status.get("engine_status") if isinstance(status, dict) else {}
        engine_status = engine_status if isinstance(engine_status, dict) else {}
        runtime_snapshot = (
            self.runtime_context.snapshot()
            if self.runtime_context is not None
            and callable(getattr(self.runtime_context, "snapshot", None))
            else {}
        )
        payload = {
            "name": "starcraft2",
            "initialized": True,
            "started": bool(state.get("running") or engine_status.get("running")),
            "runtime": runtime_snapshot,
            "details": dict(status or {}),
            "error": state.get("last_error") or engine_status.get("last_error"),
        }
        dto = GameStatusDTO.from_mapping(payload, name="starcraft2")
        self._last_game_status_dto = dto
        return dto

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
