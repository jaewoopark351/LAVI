#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations


class StopControlSubmissionTransitionReporter:
    def __init__(self, transition_logger: object):
        self._transitions = transition_logger

    def transition(
        self,
        tracker: object,
        *,
        diagnostic_disposition: str,
        control_result_delivery: str,
        python_stop_barrier_state: str,
        quarantine_active: bool,
        retirement_evidence_kind: str | None = None,
    ) -> None:
        self._transitions.log(
            tracker=tracker,
            diagnostic_disposition=diagnostic_disposition,
            control_result_delivery=control_result_delivery,
            python_stop_barrier_state=python_stop_barrier_state,
            ordinary_owner_gate_state=(
                "closed" if python_stop_barrier_state == "closed" else "open"
            ),
            quarantine_active=quarantine_active,
            retirement_evidence_kind=retirement_evidence_kind,
        )

    def terminal_release_won_send_race(self, tracker: object) -> None:
        self.transition(
            tracker,
            diagnostic_disposition=(
                "terminal_release_preceded_send_observation"
            ),
            control_result_delivery="received",
            python_stop_barrier_state="released",
            quarantine_active=False,
            retirement_evidence_kind="terminal_result",
        )


__all__ = ("StopControlSubmissionTransitionReporter",)
