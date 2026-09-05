#20260905_kpopmodder: Project STOP send outcomes onto canonical transition diagnostics.
from __future__ import annotations


class StopControlSendDiagnosticStage:
    def __init__(self, transition_reporter: object):
        self._transition_reporter = transition_reporter

    def report_terminal_release(self, tracker: object) -> None:
        self._transition_reporter.terminal_release_won_send_race(tracker)

    def report_unknown(
        self,
        tracker: object,
        *,
        disposition: str,
        quarantined: bool,
    ) -> None:
        self._transition_reporter.transition(
            tracker,
            diagnostic_disposition=disposition,
            control_result_delivery="unknown",
            python_stop_barrier_state="closed",
            quarantine_active=quarantined,
            retirement_evidence_kind="unknown_quarantine",
        )

    def report_not_scheduled(
        self,
        tracker: object,
        *,
        released: bool,
    ) -> None:
        self._transition_reporter.transition(
            tracker,
            diagnostic_disposition="control_send_rejected_before_write",
            control_result_delivery="not_scheduled",
            python_stop_barrier_state=("released" if released else "closed"),
            quarantine_active=False,
            retirement_evidence_kind="rejected_before_write",
        )

    def report_accepted(
        self,
        tracker: object,
        *,
        terminal_released: bool,
        quarantined: bool,
    ) -> None:
        self._transition_reporter.transition(
            tracker,
            diagnostic_disposition="control_send_accepted",
            control_result_delivery="pending",
            python_stop_barrier_state=(
                "released" if terminal_released else "closed"
            ),
            quarantine_active=quarantined,
            retirement_evidence_kind=(
                "terminal_result"
                if terminal_released
                else "unknown_quarantine" if quarantined else None
            ),
        )


__all__ = ("StopControlSendDiagnosticStage",)
