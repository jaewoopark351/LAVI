#20260713_kpopmodder: Move local-match orchestration to command/result DTO contracts.
from __future__ import annotations

import json
import subprocess
import time
from typing import Any, Callable, Dict, Optional, Union

from core.logger import log_print
from .starcraft2_contracts import LocalMatchRuntimeStatusDTO, StartResultDTO

from .starcraft2_dto import (
    StarCraft2CommandResult,
    StarCraft2LocalMatchCommand,
    StarCraft2LocalMatchStatus,
)
from .starcraft2_event_bus import _StarCraft2EventBus
from .starcraft2_runtime_context import SC2RuntimeContext


LineCallback = Optional[Callable[[str, str], None]]


LOCAL_MATCH_AI_BY_RACE = {
    "Terran": "BenBotBC",
    "Protoss": "sharkbot",
    "Zerg": "changeling",
}


class _StarCraft2LocalMatchService:
    def __init__(
        self,
        arg_utils,
        config_service,
        command_template,
        ladder_proxy,
        line_callback: LineCallback = None,
        event_bus: Optional[_StarCraft2EventBus] = None,
        runtime_context: Optional[SC2RuntimeContext] = None,
    ):
        self.arg_utils = arg_utils
        self.config_service = config_service
        self.command_template = command_template
        self.ladder_proxy = ladder_proxy
        self.line_callback = line_callback
        self.event_bus = event_bus
        self.runtime_context = runtime_context

    def local_match_race_from_args(self, args, fallback: str = "Terran") -> str:
        return self.arg_utils.local_match_race_from_args(args, fallback=fallback)

    def local_match_ai_race_from_args(self, args, fallback: str = "Zerg") -> str:
        return self.arg_utils.local_match_ai_race_from_args(
            args,
            bot_name_to_race=LOCAL_MATCH_AI_BY_RACE,
            fallback=fallback,
        )

    def on_local_match_race_change(self, race, args):
        selected_race = self.arg_utils.normalize_sc2_race(
            race,
            fallback=self.arg_utils.local_match_race_from_args(args),
        )
        normalized_args = self.arg_utils.strip_local_match_args(
            self.arg_utils.normalize_ladder_args(args)
        )
        normalized_args = self.arg_utils.strip_ladder_args(normalized_args, {"--race"})
        normalized_args.extend(["--race", selected_race])
        return subprocess.list2cmdline(normalized_args)

    def on_local_match_ai_race_change(self, ai_race, args):
        normalized_args = self.arg_utils.strip_local_match_args(
            self.arg_utils.normalize_ladder_args(args)
        )
        return subprocess.list2cmdline(normalized_args)

    def start_local_match(
        self,
        executable_path,
        working_directory,
        args,
        proxy_ports,
        ai_race=None,
    ) -> LocalMatchRuntimeStatusDTO:
        command = self._build_local_match_command(
            executable_path=executable_path,
            working_directory=working_directory,
            args=args,
            proxy_ports=proxy_ports,
            ai_race=ai_race,
        )
        if isinstance(command, StarCraft2CommandResult):
            return self._make_local_match_status(result=command)
        start_result = self._start_local_human_vs_changeling(command)
        return self._make_local_match_status(
            result=start_result,
            ladder_proxy_config=self.config_service.local_match_config(
                executable_path=str(executable_path or ""),
                working_directory=str(working_directory or ""),
                args=args,
                proxy_ports=proxy_ports,
                bot_name=command.bot_name,
                keep_local_match_identity_args=command.keep_local_match_identity_args,
            ),
        )

    def stop_local_match(self) -> LocalMatchRuntimeStatusDTO:
        result = self._stop_local_match()
        return self._make_local_match_status(
            result=result,
            ladder_proxy_config=self.config_service.local_match_config(),
        )

    def get_local_match_status(
        self,
        executable_path=None,
        working_directory=None,
        args=None,
        proxy_ports=None,
    ) -> LocalMatchRuntimeStatusDTO:
        return self._make_local_match_status(
            ladder_proxy_config=self.config_service.local_match_config(
                executable_path=executable_path,
                working_directory=working_directory,
                args=args,
                proxy_ports=proxy_ports,
            ),
        )

    def on_local_human_vs_changeling_click(
        self,
        executable_path,
        working_directory,
        args,
        proxy_ports,
        ai_race=None,
    ):
        status = self.start_local_match(
            executable_path=executable_path,
            working_directory=working_directory,
            args=args,
            proxy_ports=proxy_ports,
            ai_race=ai_race,
        )
        return self._local_match_status_json(result=status)

    def _build_local_match_command(
        self,
        executable_path,
        working_directory,
        args,
        proxy_ports,
        ai_race=None,
    ) -> Union[StarCraft2LocalMatchCommand, StarCraft2CommandResult]:
        selected_ai_race = self.arg_utils.normalize_sc2_race(
            ai_race
            or self.arg_utils.local_match_ai_race_from_args(
                args,
                bot_name_to_race=LOCAL_MATCH_AI_BY_RACE,
                fallback="Zerg",
            ),
            fallback="Zerg",
        )
        bot_name = LOCAL_MATCH_AI_BY_RACE.get(selected_ai_race)
        if not bot_name:
            result = StarCraft2CommandResult(
                ok=False,
                running=False,
                action="local_human_vs_changeling",
                error="local_match_random_ai_not_supported",
                message="Random AI is disabled until deterministic bot selection is implemented.",
            )
            return result

        args = self.on_local_match_ai_race_change(selected_ai_race, args)
        selected_human_race = self.arg_utils.local_match_race_from_args(args)

        command = StarCraft2LocalMatchCommand(
            executable_path=str(executable_path or ""),
            working_directory=str(working_directory or ""),
            args=self.command_template.build_launch_args(
                args,
                bot_name=bot_name,
                human_name="LAVHuman",
                human_race=selected_human_race,
                bot_race=selected_ai_race,
            ).as_dict()["args"],
            proxy_ports=str(proxy_ports or ""),
            bot_name=bot_name,
            ai_race=selected_ai_race,
            human_race=selected_human_race,
            capture_output=True,
            keep_local_match_identity_args=False,
        )
        return command

    def _start_local_human_vs_changeling(
        self,
        command: StarCraft2LocalMatchCommand,
    ) -> StarCraft2CommandResult:
        launch_template = self.command_template.build_launch_args(
            command.args,
            bot_name=command.bot_name,
            human_name="LAVHuman",
            human_race=command.human_race,
            bot_race=command.ai_race,
        )
        config = self.config_service.local_match_config(
            executable_path=command.executable_path,
            working_directory=command.working_directory,
            args=launch_template.as_dict()["args"],
            proxy_ports=command.proxy_ports,
            bot_name=command.bot_name,
            keep_local_match_identity_args=command.keep_local_match_identity_args,
        )
        config["launch_template"] = launch_template.as_dict()
        runtime_download = self.config_service.ensure_local_match_runtime(config)
        config["runtime_download"] = runtime_download
        if not runtime_download.get("ok", False):
            result = StarCraft2CommandResult(
                ok=False,
                running=False,
                action="local_human_vs_changeling",
                error=runtime_download.get("error", "starcraft2_runtime_download_failed"),
                details={"runtime_download": runtime_download},
            )
            log_print(
                f"[StarCraft2LocalMatchService] runtime restore failed: {result.to_dict()}"
            )
            self._sync_runtime_context(result, config)
            return result

        if runtime_download.get("downloaded"):
            #20260712_kpopmodder: Rebuild config after restore so bot profile
            # checks evaluate the restored filesystem tree.
            config = self.config_service.local_match_config(
                executable_path=command.executable_path,
                working_directory=command.working_directory,
                args=launch_template.as_dict()["args"],
                proxy_ports=command.proxy_ports,
                bot_name=command.bot_name,
                keep_local_match_identity_args=command.keep_local_match_identity_args,
            )
            config["runtime_download"] = runtime_download
            config["launch_template"] = launch_template.as_dict()

        bot_profile_validation = config.get("bot_profile_validation", {})
        if (
            isinstance(bot_profile_validation, dict)
            and bot_profile_validation
            and not bot_profile_validation.get("ok", False)
        ):
            #20260712_kpopmodder: Do not launch SC2 when selected bot runtime
            # is incomplete; partial runtime leads to early JoinGame failures.
            result = StarCraft2CommandResult(
                ok=False,
                running=False,
                action="local_human_vs_changeling",
                error=bot_profile_validation.get("error", "bot_runtime_invalid"),
                details={"bot_profile_validation": bot_profile_validation},
            )
            log_print(
                f"[StarCraft2LocalMatchService] preflight failed: {result.to_dict()}"
            )
            self._sync_runtime_context(result, config)
            return result

        start_result = self._launch_local_match_process(config, command.capture_output)
        self._sync_runtime_context(start_result, config)
        return start_result

    def on_local_match_stop_click(self):
        status = self.stop_local_match()
        return self._local_match_status_json(result=status)

    def _stop_local_match(self) -> StarCraft2CommandResult:
        return StarCraft2CommandResult.from_mapping(
            self.ladder_proxy.stop(),
            action="local_match_stop",
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
        return self._local_match_status_json(result=status)

    def _make_local_match_status(
        self,
        ladder_proxy_config=None,
        result: Optional[StarCraft2CommandResult] = None,
    ) -> LocalMatchRuntimeStatusDTO:
        proxy_config = (
            ladder_proxy_config
            if isinstance(ladder_proxy_config, dict)
            else self.config_service.local_match_config()
        )
        if isinstance(result, LocalMatchRuntimeStatusDTO):
            return result
        normalized_result = (
            result
            if isinstance(result, StarCraft2CommandResult)
            else StartResultDTO.from_mapping(result, action="local_human_vs_changeling")
        )
        return StarCraft2LocalMatchStatus(
            result=normalized_result,
            ladder_proxy=self.ladder_proxy.get_status(proxy_config),
        )

    def _local_match_status_json(
        self,
        ladder_proxy_config=None,
        result: Optional[Union[LocalMatchRuntimeStatusDTO, StarCraft2CommandResult, Dict[str, Any]]] = None,
    ):
        proxy_config = (
            ladder_proxy_config
            if isinstance(ladder_proxy_config, dict)
            else self.config_service.local_match_config()
        )
        status = (
            result.to_dict()
            if isinstance(result, LocalMatchRuntimeStatusDTO)
            else self._make_local_match_status(proxy_config, result).to_dict()
        )
        return json.dumps(
            status,
            ensure_ascii=False,
            indent=2,
            default=str,
        )

    def local_match_status_json(
        self,
        ladder_proxy_config=None,
        result: Optional[Union[LocalMatchRuntimeStatusDTO, StarCraft2CommandResult, Dict[str, Any]]] = None,
    ):
        return self._local_match_status_json(
            ladder_proxy_config=ladder_proxy_config,
            result=result,
        )

    def _launch_local_match_process(
        self,
        config: Dict[str, Any],
        capture_output: bool,
    ) -> StarCraft2CommandResult:
        raw_result = self.ladder_proxy.start(
            config,
            capture_output=bool(capture_output),
            line_callback=self.line_callback,
            exit_callback=self._on_ladder_proxy_exit,
        )
        return StarCraft2CommandResult.from_mapping(raw_result, action="local_human_vs_changeling")

    def _on_ladder_proxy_exit(self, result: Dict[str, Any]) -> None:
        details = result if isinstance(result, dict) else {}
        event = {
            "event_type": "proxy_stopped",
            "details": {
                "source": "ladder_proxy",
                "pid": details.get("pid"),
                "returncode": details.get("returncode"),
                "launch_diagnostics": details.get("launch_diagnostics"),
            },
        }
        if self.event_bus is not None:
            self.event_bus.emit(event)
        if self.runtime_context is None:
            return
        runtime_status = self.ladder_proxy.get_status(
            self.config_service.local_match_config()
        )
        self.runtime_context.set_status(runtime_status)
        self.runtime_context.set_tails(
            runtime_status.get("stdout_tail", []),
            runtime_status.get("stderr_tail", []),
        )
        self.runtime_context.clear_process()
        self.runtime_context.stopped_at = time.time()
        returncode = details.get("returncode")
        self.runtime_context.runtime_error = (
            None if returncode in (0, None) else f"proxy_exit_{returncode}"
        )

    def _sync_runtime_context(
        self,
        result: StarCraft2CommandResult,
        config: Optional[Dict[str, Any]] = None,
    ) -> None:
        if self.runtime_context is None:
            return
        if not isinstance(result, StarCraft2CommandResult):
            return
        runtime_status = self.ladder_proxy.get_status(config or {})
        self.runtime_context.set_status(runtime_status)
        validation = runtime_status.get("validation")
        validation_data = validation if isinstance(validation, dict) else {}
        self.runtime_context.timeout_sec = float(validation_data.get("connect_timeout_sec") or self.runtime_context.timeout_sec)
        configured_hosts = self._normalize_config_check_hosts(config)
        self.runtime_context.check_hosts = list(configured_hosts or ["127.0.0.1"])
        runtime_ports = self._normalize_runtime_ports(runtime_status, config)
        if runtime_ports:
            self.runtime_context.ports = runtime_ports
        if result.ok and runtime_status.get("running"):
            if self.runtime_context.started_at is None:
                self.runtime_context.started_at = time.time()
            self.runtime_context.set_process(getattr(self.ladder_proxy, "process", None), "local_match_proxy")
            self.runtime_context.set_tails(
                runtime_status.get("stdout_tail", []),
                runtime_status.get("stderr_tail", []),
            )
            self.runtime_context.runtime_error = None
            return
        if result.ok and not runtime_status.get("running"):
            self.runtime_context.stopped_at = runtime_status.get("stopped_at")
            self.runtime_context.clear_process()
            self.runtime_context.runtime_error = None if result.error is None else str(result.error)
            return
        self.runtime_context.stopped_at = runtime_status.get("stopped_at")
        self.runtime_context.runtime_error = (
            None if result.error is None else str(result.error)
        )
        self.runtime_context.clear_process()

    def _normalize_config_check_hosts(self, config: Optional[Dict[str, Any]]) -> list[str]:
        config = config if isinstance(config, dict) else {}
        hosts = []
        raw_hosts = config.get("check_hosts", []) if isinstance(config, dict) else []
        if isinstance(raw_hosts, str):
            raw_hosts = [part.strip() for part in raw_hosts.split(",")]
        if isinstance(raw_hosts, list):
            hosts.extend(str(item).strip() for item in raw_hosts if str(item).strip())
        return hosts if hosts else ["127.0.0.1"]

    def _normalize_runtime_ports(
        self,
        runtime_status: Dict[str, Any],
        config: Optional[Dict[str, Any]],
    ) -> list[int]:
        candidate = None
        for source in (config, runtime_status.get("ports"), runtime_status.get("validation")):
            if isinstance(source, dict) and source:
                value = source.get("ports")
                if value is not None:
                    candidate = value
                    break
            elif isinstance(source, (list, tuple)) or isinstance(source, str):
                candidate = source
                break
        if candidate is None:
            return []
        if isinstance(candidate, str):
            raw_values = [part.strip() for part in candidate.split(",")]
        else:
            raw_values = candidate
        ports: list[int] = []
        for item in raw_values:
            try:
                ports.append(int(item))
            except (TypeError, ValueError):
                continue
        return ports if ports else []
