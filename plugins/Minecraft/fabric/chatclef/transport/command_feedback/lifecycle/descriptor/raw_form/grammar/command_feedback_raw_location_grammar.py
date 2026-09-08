#20260907_kpopmodder: Isolate the registered GOTO target grammar.
from __future__ import annotations

from .command_feedback_raw_java_number_parser import (
    CommandFeedbackRawJavaNumberParser,
)


class CommandFeedbackRawLocationGrammar:
    GRAMMAR_IDS = frozenset({"goto_target"})
    _DIMENSIONS = frozenset({"overworld", "nether", "end"})

    def __init__(self, *, number_parser=None) -> None:
        self._numbers = number_parser or CommandFeedbackRawJavaNumberParser()

    def decode(
        self,
        grammar_id: str,
        _command_name: str,
        arguments: tuple[str, ...],
    ) -> str | None:
        if grammar_id != "goto_target" or not arguments:
            return None
        values = list(arguments)
        parenthesized = values[0].startswith("(") or values[-1].endswith(")")
        if parenthesized:
            if not (values[0].startswith("(") and values[-1].endswith(")")):
                return None
            values[0] = values[0][1:]
            values[-1] = values[-1][:-1]
            if not values[0] or not values[-1]:
                return None
        dimension = None
        if values and values[-1].lower() in self._DIMENSIONS:
            dimension = values.pop().lower()
        if any(value.lower() in self._DIMENSIONS for value in values):
            return None
        coordinates = [self._numbers.integer(value, positive=False) for value in values]
        if any(value is None for value in coordinates) or len(coordinates) > 3:
            return None
        if not coordinates and dimension is None:
            return None
        coordinate_kind = {0: "dimension", 1: "y", 2: "xz", 3: "xyz"}[
            len(coordinates)
        ]
        if dimension is not None and coordinates:
            coordinate_kind = f"{coordinate_kind}_dimension"
        return f"parenthesized_{coordinate_kind}" if parenthesized else coordinate_kind


__all__ = ("CommandFeedbackRawLocationGrammar",)
