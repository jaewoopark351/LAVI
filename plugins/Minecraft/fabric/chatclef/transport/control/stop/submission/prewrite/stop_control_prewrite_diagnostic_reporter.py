#20260905_kpopmodder: Report STOP prewrite state without deciding submission results.
from __future__ import annotations


class StopControlPrewriteDiagnosticReporter:
    def __init__(self, *, transition_reporter: object):
        self._transition_reporter = transition_reporter

    def admission_committed(self, tracker: object) -> None:
        self._transition_reporter.transition(
            tracker,
            diagnostic_disposition="python_admission_committed",
            control_result_delivery="not_attempted",
            python_stop_barrier_state="closed",
            quarantine_active=False,
        )

    def report(
        self,
        *,
        tracker: object,
        status: str,
        released: bool,
        quarantined: bool,
    ) -> None:
        if status == "ready":
            return
        if status == "terminal":
            self._transition_reporter.terminal_release_won_send_race(tracker)
            return
        if status == "unknown":
            self._transition_reporter.transition(
                tracker,
                diagnostic_disposition="control_send_prewrite_validation_unknown",
                control_result_delivery="unknown",
                python_stop_barrier_state="closed",
                quarantine_active=quarantined,
                retirement_evidence_kind="unknown_quarantine",
            )
            return
        if status == "rejected":
            self._transition_reporter.transition(
                tracker,
                diagnostic_disposition="connection_changed_before_write",
                control_result_delivery="not_scheduled",
                python_stop_barrier_state=("released" if released else "closed"),
                quarantine_active=False,
                retirement_evidence_kind="rejected_before_write",
            )
            return
        raise ValueError("unknown STOP prewrite status")


__all__ = ("StopControlPrewriteDiagnosticReporter",)
