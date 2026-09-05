#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations


class StopControlResultDiagnosticReporter:
    def __init__(self, *, diagnostics: object, transition_logger: object):
        self._diagnostics = diagnostics
        self._transitions = transition_logger

    def warn(self, message: str) -> None:
        try:
            self._diagnostics.warning(message)
        except Exception:
            pass

    def log_terminal(
        self,
        *,
        tracker: object,
        data: dict,
        decision: object | None,
        diagnostic_disposition: str | None,
        barrier_state: str,
        frozen_owner_match: object,
        retirement_evidence: str,
    ) -> None:
        self._transitions.log(
            tracker=tracker,
            wire_data=(data if decision is not None and decision.valid else None),
            control_status=(
                decision.status if decision is not None else "unknown"
            ),
            control_outcome=(
                decision.control_outcome
                if decision is not None
                else "unknown"
            ),
            control_reason=(
                decision.reason
                if decision is not None and decision.valid
                else None
            ),
            diagnostic_disposition=diagnostic_disposition,
            control_result_delivery="received",
            python_stop_barrier_state=barrier_state,
            ordinary_owner_gate_state=(
                "closed" if barrier_state == "closed" else "open"
            ),
            frozen_python_owner_exact_match=frozen_owner_match,
            quarantine_active=tracker.quarantined,
            retirement_evidence_kind=retirement_evidence,
        )


__all__ = ("StopControlResultDiagnosticReporter",)
