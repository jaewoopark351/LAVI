#20260908_kpopmodder: Preserve one bounded claim-evaluation exception identity without exception text.
from __future__ import annotations

import re


class ContextualCommandStatusClaimFailure(RuntimeError):
    stage = "claim_evaluation"
    _EXCEPTION_CLASS = re.compile(r"[A-Za-z_][A-Za-z0-9_]{0,95}\Z", re.ASCII)

    def __init__(self, *, exception_class: str) -> None:
        self.exception_class = (
            exception_class
            if type(exception_class) is str
            and self._EXCEPTION_CLASS.fullmatch(exception_class) is not None
            else "invalid"
        )
        super().__init__(self.stage)


__all__ = ("ContextualCommandStatusClaimFailure",)
