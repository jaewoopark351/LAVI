#20260713_kpopmodder: Move local-match orchestration to command/result DTO contracts.
from __future__ import annotations

import json
import subprocess
from typing import Any, Callable, Dict, Optional, Union

from app_core.extensions import (
    GameResultDTO,
    GameStartResultDTO,
    GameStatusDTO,
    GameStopResultDTO,
)
from core.logger import log_print
from .starcraft2_contracts import (
    LadderProxyExitEventDTO,
    LadderProxyResultDTO,
    LadderProxyStatusDTO,
    LocalMatchLaunchConfigDTO,
    LocalMatchRuntimeStatusDTO,
    StarCraft2Event,
    StartResultDTO,
)

from .starcraft2_dto import (
    StarCraft2CommandResult,
    StarCraft2LocalMatchCommand,
    StarCraft2LocalMatchStatus,
)
from .starcraft2_event_bus import StarCraft2EventBus
from .starcraft2_runtime_context import SC2RuntimeContext


LineCallback = Optional[Callable[[str, str], None]]


LOCAL_MATCH_AI_BY_RACE = {
    "Terran": "BenBotBC",
    "Protoss": "sharkbot",
    "Zerg": "changeling",
}


class StarCraft2LocalMatchService:
    #20260715_kpopmodder: Public domain service boundary for Local Human vs AI flow.
    def __init__(
        self,
        arg_utils,
        config_service,
        command_template,
        ladder_proxy,
        line_callback: LineCallback = None,
        event_bus: Optional[StarCraft2EventBus] = None,
        runtime_context: Optional[SC2RuntimeContext] = None,
    ):
        self.arg_utils = arg_utils
        self.config_service = config_service
        self.command_template = command_template
        self.ladder_proxy = ladder_proxy
        self.line_callback = line_callback
        self.event_bus = event_bus
        self.runtime_context = runtime_context
        self._last_game_result_dto: Optional[GameResultDTO] = None
        self._last_game_stop_result_dto: Optional[GameStopResultDTO] = None
        self._last_game_status_dto: Optional[GameStatusDTO] = None

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
            return result

        start_result = self._launch_local_match_process(config, command.capture_output)
        return start_result

    def on_local_match_stop_click(self):
        status = self.stop_local_match()
        return self._local_match_status_json(result=status)

    def _stop_local_match(self) -> StarCraft2CommandResult:
        proxy_result = LadderProxyResultDTO.from_mapping(self.ladder_proxy.stop())
        return StarCraft2CommandResult.from_mapping(
            proxy_result.to_dict(),
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
        game_result = self._build_game_result_dto(normalized_result)
        proxy_command = LocalMatchLaunchConfigDTO.from_mapping(proxy_config)
        ladder_proxy_status = LadderProxyStatusDTO.from_mapping(
            self.ladder_proxy.get_status(proxy_command)
        )
        ladder_proxy_status_payload = ladder_proxy_status.to_dict()
        game_status = self._build_game_status_dto(
            result=game_result,
            ladder_proxy_status=ladder_proxy_status_payload,
        )
        return StarCraft2LocalMatchStatus(
            result=normalized_result,
            ladder_proxy=ladder_proxy_status_payload,
            game_result=game_result,
            game_status=game_status,
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
        proxy_command = LocalMatchLaunchConfigDTO.from_mapping(
            {**dict(config or {}), "capture_output": bool(capture_output)}
        )
        proxy_result = LadderProxyResultDTO.from_mapping(
            self.ladder_proxy.start(
                proxy_command,
                line_callback=self.line_callback,
                exit_callback=self._on_ladder_proxy_exit,
            )
        )
        return StarCraft2CommandResult.from_mapping(
            proxy_result.to_dict(),
            action="local_human_vs_changeling",
        )

    def _build_game_result_dto(self, result: StartResultDTO) -> GameResultDTO:
        payload = result.to_dict() if isinstance(result, StartResultDTO) else {}
        action = str(payload.get("action") or "")
        if action in {"local_match_stop", "stop"}:
            stop_dto = GameStopResultDTO.from_mapping(payload, action="stop")
            self._last_game_stop_result_dto = stop_dto
            self._last_game_result_dto = stop_dto
            return stop_dto
        game_result = GameStartResultDTO.from_mapping(
            payload,
            action=action or "local_human_vs_changeling",
        )
        self._last_game_result_dto = game_result
        return game_result

    def _build_game_status_dto(
        self,
        result: GameResultDTO,
        ladder_proxy_status: Dict[str, Any],
    ) -> GameStatusDTO:
        status_payload = {
            "name": "starcraft2",
            "initialized": True,
            "started": bool(ladder_proxy_status.get("running")),
            "runtime": (
                self.runtime_context.snapshot()
                if self.runtime_context is not None
                and callable(getattr(self.runtime_context, "snapshot", None))
                else {}
            ),
            "details": {
                "mode": "local_human_vs_changeling",
                "result": result.to_dict(),
                "ladder_proxy": dict(ladder_proxy_status or {}),
            },
            "error": result.error,
        }
        dto = GameStatusDTO.from_mapping(status_payload, name="starcraft2")
        self._last_game_status_dto = dto
        return dto

    def _on_ladder_proxy_exit(self, result: LadderProxyExitEventDTO) -> None:
        details = LadderProxyExitEventDTO.from_mapping(result)
        event = StarCraft2Event(
            event_type="proxy_stopped",
            details={
                "source": "ladder_proxy",
                "pid": details.pid,
                "returncode": details.returncode,
                "launch_diagnostics": dict(details.launch_diagnostics),
            },
        )
        if self.event_bus is not None:
            self.event_bus.emit(event)

_StarCraft2LocalMatchService = StarCraft2LocalMatchService
