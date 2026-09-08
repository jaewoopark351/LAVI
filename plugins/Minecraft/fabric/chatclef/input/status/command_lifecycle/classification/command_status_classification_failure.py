#20260908_kpopmodder: Carry one bounded STATUS classification-stage failure without raw input or exception text.
from __future__ import annotations

import re


class CommandStatusClassificationFailure(RuntimeError):
    _STAGES = frozenset(
        {
            "input_validation",
            "addressee_parsing",
            "generic_matching",
            "family_matching",
            "target_matching",
        }
    )
    _EXCEPTION_CLASS = re.compile(r"[A-Za-z_][A-Za-z0-9_]{0,95}\Z", re.ASCII)

    def __init__(self, *, stage: str, exception_class: str) -> None:
        if stage not in self._STAGES:
            raise ValueError("command status classification stage is invalid")
        safe_exception_class = (
            exception_class
            if type(exception_class) is str
            and self._EXCEPTION_CLASS.fullmatch(exception_class) is not None
            else "invalid"
        )
        self.stage = stage
        self.exception_class = safe_exception_class
        super().__init__(stage)


__all__ = ("CommandStatusClassificationFailure",)
