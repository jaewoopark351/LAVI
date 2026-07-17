#20260717_kpopmodder: Split from legacy multi-class module for AGENTS 29.1 file/type separation.

#20260713_kpopmodder: Add deterministic local-match launch argument template builder and launch diagnostics.
from __future__ import annotations

from collections import deque
from typing import Any, Dict, List, Optional, Set

import time


_KNOWN_RACES = {"terran", "zerg", "protoss", "random"}
_KNOWN_BOOLEAN_ARGS = {"--realtime", "--no-realtime"}
_KNOWN_VALUE_ARGS = {
    "--map",
    "--race",
    "--bot-race",
    "--bot-dir",
    "--config",
    "--bot",
    "--human-name",
}


def _normalize_race(value: str, fallback: str = "Terran") -> str:
    text = str(value or "").strip().lower()
    if text in _KNOWN_RACES:
        return text.title() if text != "random" else "Random"
    fallback_text = str(fallback or "").strip().lower()
    return fallback_text.title() if fallback_text in _KNOWN_RACES else "Terran"

from ._local_match_template_build_result import _LocalMatchTemplateBuildResult

class _LocalMatchCommandTemplate:
    #20260713_kpopmodder: Keep local-match arg normalization deterministic and
    # predictable for runtime replay and easier diagnostics.
    def __init__(
        self,
        default_human_name: str = "LAVHuman",
        valid_boolean_args: Optional[Set[str]] = None,
        known_value_args: Optional[Set[str]] = None,
    ):
        self.default_human_name = str(default_human_name or "").strip() or "LAVHuman"
        self.known_boolean_args = valid_boolean_args or set(_KNOWN_BOOLEAN_ARGS)
        self.known_value_args = known_value_args or set(_KNOWN_VALUE_ARGS)

    def build_launch_args(
        self,
        args: List[str] | str | None,
        bot_name: str,
        human_name: Optional[str] = None,
        human_race: str = "Terran",
        bot_race: Optional[str] = None,
    ) -> _LocalMatchTemplateBuildResult:
        normalized_args = self._parse_args(args)
        parsed = normalized_args["ordered"]
        warnings: List[str] = []

        dropped = list(normalized_args["dropped"])
        if not dropped:
            warnings = []
        else:
            warnings.append("Dropped unsupported local-match args and normalized to strict template.")

        forced_human_name = str(human_name or self.default_human_name or "LAVHuman")
        forced_bot_name = str(bot_name or "").strip()
        selected_human_race = _normalize_race(human_race, fallback="Terran")
        selected_bot_race = _normalize_race(bot_race or parsed.get("--bot-race", ""), fallback="Random")

        ordered_args: List[str] = []
        canonical = [
            ("--human-name", forced_human_name),
            ("--bot", forced_bot_name),
            ("--map", parsed.get("--map", "")),
            ("--race", parsed.get("--race", selected_human_race)),
            ("--bot-race", parsed.get("--bot-race", selected_bot_race)),
            ("--bot-dir", parsed.get("--bot-dir", "")),
            ("--config", parsed.get("--config", "")),
        ]
        for key, value in canonical:
            if key == "--bot-race" and not value:
                continue
            if key in {"--map", "--bot-dir", "--config"} and not value:
                continue
            if key in {"--bot", "--human-name"} and not value:
                continue
            ordered_args.append(key)
            if value is not None and value != "":
                ordered_args.append(str(value))

        for bool_key in ("--realtime", "--no-realtime"):
            if bool_key in normalized_args["flags"]:
                ordered_args.append(bool_key)

        normalized = {
            "human_name": forced_human_name,
            "bot_name": forced_bot_name,
            "human_race": selected_human_race,
            "bot_race": selected_bot_race,
        }
        return _LocalMatchTemplateBuildResult(
            ordered_args,
            [str(item) for item in dropped],
            warnings,
            normalized,
        )

    def _parse_args(self, args: List[str] | str | None) -> Dict[str, Any]:
        tokens = self._normalize_args(args)
        dropped: List[str] = []
        recognized_flags = set()
        recognized_values: Dict[str, str] = {}
        i = 0
        while i < len(tokens):
            token = str(tokens[i] or "").strip()
            i += 1
            if not token:
                continue

            # Boolean flags can stay as canonical tokens.
            if token in self.known_boolean_args:
                recognized_flags.add(token)
                continue

            if token in self.known_value_args:
                if i >= len(tokens):
                    dropped.append(token)
                    continue
                value = str(tokens[i] or "").strip()
                i += 1
                recognized_values[token] = value
                continue

            if "=" in token:
                key, value = token.split("=", 1)
                key = key.strip()
                if key in self.known_value_args:
                    recognized_values[key] = value.strip()
                    continue

            dropped.append(token)

        ordered = {}
        for key in ("--map", "--race", "--bot-race", "--bot-dir", "--config", "--bot", "--human-name"):
            if key in recognized_values:
                value = str(recognized_values[key]).strip()
                if value:
                    ordered[key] = value
        return {"ordered": ordered, "flags": recognized_flags, "dropped": dropped}

    def _normalize_args(self, value: Any) -> List[str]:
        from shlex import split

        if isinstance(value, str):
            try:
                return split(value, posix=False)
            except ValueError:
                return [part.strip() for part in value.split() if part.strip()]
        if isinstance(value, (list, tuple)):
            return [str(item).strip().strip("\"'") for item in value if str(item).strip()]
        return []
