#20260908_kpopmodder: Preserve one bounded STATUS failure diagnostic across custody transfers.
from __future__ import annotations

from dataclasses import dataclass, field, replace
import threading

from ..publication.command_status_publication_handoff_failure import (
    _sanitize_status_exception_class,
)


_STATUS_FAILURE_STAGES = frozenset(
    {
        "publication_handoff_diagnostic_custody",
        "publication_handoff_acknowledgement",
        "publication_handoff_snapshot",
        "status_rendering",
        "presentation_detail_projection",
        "primary_decision_assembly",
        "fallback_decision_assembly",
        "optional_route_result_validation",
        "optional_route_result_acknowledgement_identity",
        "trusted_feedback_rendering",
        "response_capability_authorization",
        "feature_admission_finalization",
        "proof_lifecycle_close",
        "publication_acknowledgement_identity",
        "publication_route_identity",
        "dispatcher_initial_decision_resolution",
        "dispatcher_ready_decision_resolution",
        "dispatcher_external_response_publication",
        "dispatcher_publication_commit_inspection",
    }
)


@dataclass(frozen=True, slots=True)
class CommandStatusFailureDiagnosticCustody:
    _base_record: object = field(repr=False)
    _record_callback: object = field(repr=False)
    _lock: object = field(default_factory=threading.Lock, repr=False, compare=False)
    _recorded: list[bool] = field(
        default_factory=lambda: [False],
        repr=False,
        compare=False,
    )

    def __post_init__(self) -> None:
        if not callable(self._record_callback):
            raise TypeError("STATUS failure diagnostic callback must be callable")

    def record_once(self, stage: object, exception_class: object) -> bool:
        with self._lock:
            if self._recorded[0]:
                return False
            self._recorded[0] = True
        safe_stage = (
            stage
            if type(stage) is str and stage in _STATUS_FAILURE_STAGES
            else "invalid"
        )
        safe_exception_class = _sanitize_status_exception_class(exception_class)
        try:
            record = replace(
                self._base_record,
                stage=safe_stage,
                exception_class=safe_exception_class,
            )
            self._record_callback(record)
        except Exception:
            return False
        return True


__all__ = ("CommandStatusFailureDiagnosticCustody",)
