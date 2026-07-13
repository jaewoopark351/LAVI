#20260713_kpopmodder: Extract argument and lightweight config helpers from plugin facade.

from __future__ import annotations

import os
import shlex
from typing import Any, Iterable, List, Optional, Set


class _StarCraft2ArgUtils:
    def __init__(self, sc2_race_choices: Optional[Iterable[str]] = None):
        self.sc2_race_choices = list(sc2_race_choices or ["Terran", "Zerg", "Protoss", "Random"])

    def normalize_ladder_args(self, value: Any) -> List[str]:
        if isinstance(value, str):
            try:
                parts = shlex.split(value, posix=False)
            except ValueError:
                parts = value.split()
            return [str(part).strip().strip("\"'") for part in parts if str(part).strip()]
        if isinstance(value, (list, tuple)):
            return [str(item) for item in value if str(item)]
        return []

    def has_arg(self, args: List[str], name: str) -> bool:
        prefix = name + "="
        return any(
            str(arg or "").strip() == name or str(arg or "").strip().startswith(prefix)
            for arg in args
        )

    def ladder_arg_value(self, args: List[str], name: str, fallback: str = "") -> str:
        prefix = name + "="
        for index, arg in enumerate(args):
            text = str(arg or "").strip()
            if text == name and index + 1 < len(args):
                return str(args[index + 1] or "").strip()
            if text.startswith(prefix):
                return text[len(prefix):].strip()
        return str(fallback or "")

    def normalize_sc2_race(self, value: Any, fallback: str = "Random") -> str:
        races = {race.lower(): race for race in self.sc2_race_choices}
        text = str(value or "").strip().lower()
        if text in races:
            return races[text]
        fallback_text = str(fallback or "").strip().lower()
        return races.get(fallback_text, "Random")

    def local_match_race_from_args(
        self,
        args: Any,
        fallback: str = "Terran",
    ) -> str:
        normalized_args = self.normalize_ladder_args(args)
        prefix = "--race="
        for index, arg in enumerate(normalized_args):
            text = str(arg or "").strip()
            if text == "--race" and index + 1 < len(normalized_args):
                return self.normalize_sc2_race(
                    normalized_args[index + 1],
                    fallback=fallback,
                )
            if text.startswith(prefix):
                return self.normalize_sc2_race(
                    text[len(prefix):],
                    fallback=fallback,
                )
        return self.normalize_sc2_race(fallback, fallback="Terran")

    def local_match_ai_race_from_args(
        self,
        args: Any,
        bot_name_to_race: Optional[dict[str, str]] = None,
        fallback: str = "Zerg",
    ) -> str:
        normalized_args = self.normalize_ladder_args(args)
        if bot_name_to_race is None:
            return self.normalize_sc2_race(fallback, fallback="Zerg")
        for index, arg in enumerate(normalized_args):
            text = str(arg or "").strip()
            if text == "--bot" and index + 1 < len(normalized_args):
                bot_name = normalized_args[index + 1].lower()
                for race, mapped_bot in bot_name_to_race.items():
                    if bot_name == mapped_bot.lower():
                        return race
        return self.normalize_sc2_race(fallback, fallback="Zerg")

    def strip_ladder_args(self, args: List[str], names: Set[str]) -> List[str]:
        stripped = []
        skip_next = False
        for arg in args:
            text = str(arg or "").strip()
            if skip_next:
                skip_next = False
                continue
            if text in names:
                skip_next = True
                continue
            if any(text.startswith(name + "=") for name in names):
                continue
            stripped.append(arg)
        return stripped

    def strip_local_match_args(self, args: List[str]) -> List[str]:
        return self.strip_ladder_args(
            args,
            {
                "--remote-human-host",
                "--remote-human-client-port",
                "--lan-game-host-ip",
                "--bot-race",
                #20260710_kpopmodder: LavHumanVsBot uses its built-in
                # human name in the current binary; passing this legacy
                # option prevents all local bot types from progressing.
                "--bot",
                "--human-name",
            },
        )

    def same_path(self, left: str, right: str) -> bool:
        left_path = os.path.normcase(os.path.abspath(os.path.normpath(str(left or ""))))
        right_path = os.path.normcase(os.path.abspath(os.path.normpath(str(right or ""))))
        return bool(left_path and right_path and left_path == right_path)

    def config_bool(self, value: Any, default: bool = False) -> bool:
        if isinstance(value, bool):
            return value
        text = str(value if value is not None else default).strip().lower()
        return text in {"1", "true", "yes", "on"}

    def float_config_value(self, value: Any, default: float) -> float:
        try:
            parsed = float(value)
        except (TypeError, ValueError):
            return default
        return parsed if parsed > 0 else default
