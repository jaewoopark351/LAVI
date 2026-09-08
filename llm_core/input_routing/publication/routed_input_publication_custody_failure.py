#20260905_kpopmodder: Preserve routed-input publication responsibilities in focused modules.
#20260908_kpopmodder: Carry only bounded backend-neutral resolver failure facts.
from __future__ import annotations

from dataclasses import dataclass
import re


_RESOLUTION_STAGES = frozenset(
    {
        "publication_acknowledgement_identity",
        "publication_route_identity",
        "dispatcher_initial_decision_resolution",
        "dispatcher_ready_decision_resolution",
        "dispatcher_external_response_publication",
        "dispatcher_publication_commit_inspection",
    }
)
_EXCEPTION_CLASS = re.compile(r"[A-Za-z_][A-Za-z0-9_]{0,95}\Z", re.ASCII)


@dataclass(frozen=True, slots=True)
class RoutedInputPublicationCustodyFailure:
    stage: str
    exception_class: str

    def __post_init__(self) -> None:
        stage = (
            self.stage
            if type(self.stage) is str and self.stage in _RESOLUTION_STAGES
            else "invalid"
        )
        exception_class = (
            self.exception_class
            if type(self.exception_class) is str
            and _EXCEPTION_CLASS.fullmatch(self.exception_class) is not None
            else "invalid"
        )
        object.__setattr__(self, "stage", stage)
        object.__setattr__(self, "exception_class", exception_class)


__all__ = ("RoutedInputPublicationCustodyFailure",)
