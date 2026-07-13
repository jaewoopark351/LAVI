#20260713_kpopmodder: Extract local-match UI handlers and local-match status builders.

from __future__ import annotations

import json
from typing import Any, Dict

import subprocess

from core.logger import log_print


LOCAL_MATCH_AI_BY_RACE = {
    "Terran": "BenBotBC",
    "Protoss": "sharkbot",
    "Zerg": "changeling",
}


class _StarCraft2LocalMatchService:
    def __init__(self, owner, arg_utils, config_service):
        self.owner = owner
        self.arg_utils = arg_utils
        self.config_service = config_service

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
        selected_race = self.arg_utils.normalize_sc2_race(ai_race, fallback="Zerg")
        normalized_args = self.arg_utils.strip_local_match_args(
            self.arg_utils.normalize_ladder_args(args)
        )
        bot_name = LOCAL_MATCH_AI_BY_RACE.get(selected_race, "")
        replaced = False
        rewritten = []
        skip_next = False
        for arg in normalized_args:
            text = str(arg or "").strip()
            if skip_next:
                skip_next = False
                continue
            if text == "--bot":
                rewritten.extend(["--bot", bot_name] if bot_name else [])
                skip_next = True
                replaced = True
                continue
            if text.startswith("--bot="):
                if bot_name:
                    rewritten.append("--bot=" + bot_name)
                replaced = True
                continue
            rewritten.append(arg)
        if bot_name and not replaced:
            rewritten = ["--bot", bot_name] + rewritten
        normalized_args = rewritten
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
            result = {
                "ok": False,
                "error": "local_match_random_ai_not_supported",
                "message": "Random AI is disabled until deterministic bot selection is implemented.",
            }
            return self.local_match_status_json(result=result)
        args = self.on_local_match_ai_race_change(selected_ai_race, args)
        config = self.config_service.local_match_config(
            executable_path=executable_path,
            working_directory=working_directory,
            args=args,
            proxy_ports=proxy_ports,
        )
        runtime_download = self.config_service.ensure_local_match_runtime(config)
        config["runtime_download"] = runtime_download
        if not runtime_download.get("ok", False):
            result = {
                "ok": False,
                "running": False,
                "error": runtime_download.get("error", "starcraft2_runtime_download_failed"),
                "runtime_download": runtime_download,
            }
            log_print(f"[StarCraft2] Local Match runtime restore failed: {result}")
            return self.local_match_status_json(config, result)
        if runtime_download.get("downloaded"):
            #20260712_kpopmodder: Rebuild validation after restoring ignored
            # runtime files so bot profile checks see the freshly downloaded tree.
            config = self.config_service.local_match_config(
                executable_path=executable_path,
                working_directory=working_directory,
                args=args,
                proxy_ports=proxy_ports,
            )
            config["runtime_download"] = runtime_download
        bot_profile_validation = config.get("bot_profile_validation", {})
        if (
            isinstance(bot_profile_validation, dict)
            and bot_profile_validation
            and not bot_profile_validation.get("ok", False)
        ):
            #20260712_kpopmodder: Do not open SC2 when the selected bot runtime
            # is incomplete; a half-restored runtime otherwise reaches JoinGame.
            result = {
                "ok": False,
                "running": False,
                "error": bot_profile_validation.get("error", "bot_runtime_invalid"),
                "bot_profile_validation": bot_profile_validation,
            }
            log_print(f"[StarCraft2] Local Human vs AI preflight failed: {result}")
            return self.local_match_status_json(config, result)
        result = self.owner.ladder_proxy.start(
            config,
            capture_output=bool(config.get("capture_output", True)),
            line_callback=self.owner._on_ladder_proxy_line,
        )
        log_print(f"[StarCraft2] Start Local Human vs Changeling result: {result}")
        return self.local_match_status_json(config, result)

    def on_local_match_stop_click(self):
        result = self.owner.ladder_proxy.stop()
        log_print(f"[StarCraft2] Stop Local Match result: {result}")
        return self.local_match_status_json(result=result)

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
        return self.local_match_status_json(config)

    def local_match_status_json(self, ladder_proxy_config=None, result=None):
        proxy_config = (
            ladder_proxy_config
            if isinstance(ladder_proxy_config, dict)
            else self.config_service.local_match_config()
        )
        return json.dumps(
            {
                "mode": "local_human_vs_changeling",
                "result": result or {},
                "ladder_proxy": self.owner.ladder_proxy.get_status(proxy_config),
            },
            ensure_ascii=False,
            indent=2,
            default=str,
        )
