#20260908_kpopmodder: Carry only a bounded post-permit STATUS handoff failure signal.
from __future__ import annotations

from dataclasses import dataclass
import re


_HANDOFF_STAGES = frozenset(
    {
        "publication_handoff_diagnostic_custody",
        "publication_handoff_acknowledgement",
        "publication_handoff_snapshot",
    }
)
_EXCEPTION_CLASS = re.compile(r"[A-Za-z_][A-Za-z0-9_]{0,95}\Z", re.ASCII)


def _sanitize_status_failure_stage(value: object) -> str:
    return value if type(value) is str and value in _HANDOFF_STAGES else "invalid"


def _sanitize_status_exception_class(value: object) -> str:
    if value == "none":
        return "none"
    if type(value) is str and _EXCEPTION_CLASS.fullmatch(value) is not None:
        return value
    return "invalid"


@dataclass(frozen=True, slots=True)
class CommandStatusPublicationHandoffFailure:
    stage: str
    exception_class: str

    def __post_init__(self) -> None:
        object.__setattr__(self, "stage", _sanitize_status_failure_stage(self.stage))
        object.__setattr__(
            self,
            "exception_class",
            _sanitize_status_exception_class(self.exception_class),
        )


__all__ = ("CommandStatusPublicationHandoffFailure",)
