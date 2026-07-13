#20260713_kpopmodder: Move local-match orchestration to command/result DTO contracts.
from __future__ import annotations

import json
import subprocess
from typing import Any, Callable, Dict, Optional

from core.logger import log_print

from .starcraft2_dto import (
    StarCraft2CommandResult,
    StarCraft2LocalMatchCommand,
    StarCraft2LocalMatchStatus,
)


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
    ):
        self.arg_utils = arg_utils
        self.config_service = config_service
        self.command_template = command_template
        self.ladder_proxy = ladder_proxy
        self.line_callback = line_callback

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

    def on_local_human_vs_changeling_click(
        self,
        executable_path,
        working_directory,
        args,
        proxy_ports,
        ai_race=None,
    ):
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
            return self._local_match_status_json(
                result=result,
            )

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
        return self._start_local_human_vs_changeling(command)

    def _start_local_human_vs_changeling(
        self,
        command: StarCraft2LocalMatchCommand,
    ):
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
            return self._local_match_status_json(config, result)

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
            return self._local_match_status_json(config, result)

        start_result = self._launch_local_match_process(config, command.capture_output)
        return self._local_match_status_json(config, start_result)

    def on_local_match_stop_click(self):
        result = self._stop_local_match()
        return self._local_match_status_json(result=result)

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
        config = self.config_service.local_match_config(
            executable_path=executable_path,
            working_directory=working_directory,
            args=args,
            proxy_ports=proxy_ports,
        )
        return self._local_match_status_json(config)

    def _local_match_status_json(self, ladder_proxy_config=None, result: Optional[StarCraft2CommandResult] = None):
        proxy_config = (
            ladder_proxy_config
            if isinstance(ladder_proxy_config, dict)
            else self.config_service.local_match_config()
        )
        status = StarCraft2LocalMatchStatus(
            result=result,
            ladder_proxy=self.ladder_proxy.get_status(proxy_config),
        )
        return json.dumps(
            status.to_dict(),
            ensure_ascii=False,
            indent=2,
            default=str,
        )

    def local_match_status_json(
        self,
        ladder_proxy_config=None,
        result: Optional[StarCraft2CommandResult] = None,
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
        )
        return StarCraft2CommandResult.from_mapping(raw_result, action="local_human_vs_changeling")
