#20260905_kpopmodder: Preserve trusted-route publication failures in a focused module.
#20260908_kpopmodder: Carry only a closed STATUS pipeline stage and exception class.
from __future__ import annotations

from dataclasses import dataclass
import re


_TRUSTED_STAGES = frozenset(
    {
        "trusted_feedback_rendering",
        "response_capability_authorization",
        "feature_admission_finalization",
        "proof_lifecycle_close",
        "publication_acknowledgement_identity",
        "publication_route_identity",
    }
)
_EXCEPTION_CLASS = re.compile(r"[A-Za-z_][A-Za-z0-9_]{0,95}\Z", re.ASCII)


@dataclass(frozen=True, slots=True)
class CommandStatusPublicationPipelineFailure:
    stage: str
    exception_class: str

    def __post_init__(self) -> None:
        safe_stage = (
            self.stage
            if type(self.stage) is str and self.stage in _TRUSTED_STAGES
            else "invalid"
        )
        safe_exception = (
            self.exception_class
            if type(self.exception_class) is str
            and (
                self.exception_class == "none"
                or _EXCEPTION_CLASS.fullmatch(self.exception_class) is not None
            )
            else "invalid"
        )
        object.__setattr__(self, "stage", safe_stage)
        object.__setattr__(self, "exception_class", safe_exception)


__all__ = ("CommandStatusPublicationPipelineFailure",)
