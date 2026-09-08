#20260907_kpopmodder: Isolate bounded zero/one-token registered raw grammars.
from __future__ import annotations

import re

from .command_feedback_raw_java_number_parser import (
    CommandFeedbackRawJavaNumberParser,
)


class CommandFeedbackRawScalarGrammar:
    GRAMMAR_IDS = frozenset(
        {
            "attack_target_optional_count",
            "no_arguments",
            "on_off_state",
            "optional_block",
            "optional_finite_double",
            "optional_player",
            "positive_integer",
            "structure",
        }
    )
    _SAFE_TARGET = re.compile(r"[A-Za-z0-9_]{1,64}\Z", re.ASCII)
    _PLAYER = re.compile(r"[A-Za-z0-9_]{1,16}\Z", re.ASCII)
    _BLOCK = re.compile(r"[A-Za-z0-9_]+\Z", re.ASCII)
    _STRUCTURES = frozenset({"stronghold", "desert_temple"})

    def __init__(self, *, number_parser=None) -> None:
        self._numbers = number_parser or CommandFeedbackRawJavaNumberParser()

    def decode(
        self,
        grammar_id: str,
        command_name: str,
        arguments: tuple[str, ...],
    ) -> str | None:
        if grammar_id == "no_arguments":
            return "no_arguments" if not arguments else None
        if grammar_id == "on_off_state":
            if len(arguments) != 1 or arguments[0].lower() not in {"on", "off"}:
                return None
            return f"state_{arguments[0].lower()}"
        if grammar_id == "positive_integer":
            if len(arguments) != 1:
                return None
            parsed = self._numbers.integer(arguments[0], positive=False)
            if parsed is None:
                return None
            if parsed < 1:
                return (
                    "food_units_unprojected"
                    if command_name == "food"
                    else "meat_units_unprojected"
                )
            return "food_units" if command_name == "food" else "meat_units"
        if grammar_id == "attack_target_optional_count":
            if len(arguments) not in {1, 2}:
                return None
            parsed_count = 1
            if len(arguments) == 2:
                parsed_count = self._numbers.integer(arguments[1], positive=False)
            if parsed_count is None:
                return None
            if (
                self._SAFE_TARGET.fullmatch(arguments[0]) is None
                or parsed_count < 1
            ):
                return "target_count_unprojected"
            return "target_count" if len(arguments) == 2 else "target_default_count"
        if grammar_id == "optional_player":
            if not arguments:
                return "butler_player"
            if len(arguments) == 1 and self._PLAYER.fullmatch(arguments[0]):
                return "explicit_player"
            if len(arguments) == 1:
                return "explicit_player_unprojected"
            return None
        if grammar_id == "optional_block":
            if not arguments:
                return "default_target"
            if len(arguments) == 1 and self._BLOCK.fullmatch(arguments[0]):
                return "explicit_target"
            if len(arguments) == 1:
                return "explicit_target_unprojected"
            return None
        if grammar_id == "optional_finite_double":
            if not arguments:
                return "default_value"
            if len(arguments) == 1 and self._numbers.is_finite_double(arguments[0]):
                return "explicit_value"
            return None
        if grammar_id == "structure":
            if len(arguments) == 1 and arguments[0].lower() in self._STRUCTURES:
                return "structure"
        return None


__all__ = ("CommandFeedbackRawScalarGrammar",)
