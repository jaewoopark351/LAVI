#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.transport.control.stop.stop_control_submission_result import (
    StopControlSubmissionResult,
)

from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.prewrite.stop_control_prewrite_diagnostic_reporter import StopControlPrewriteDiagnosticReporter
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.prewrite.stop_control_prewrite_result_policy import StopControlPrewriteResultPolicy


class StopControlSubmissionCoordinator:
    def __init__(
        self,
        *,
        admission_coordinator: object,
        prewrite_guard: object,
        send_outcome_coordinator: object,
        transition_reporter: object,
        result_factory: object,
    ):
        self._admission_coordinator = admission_coordinator
        self._prewrite_guard = prewrite_guard
        self._send_outcome_coordinator = send_outcome_coordinator
        self._prewrite_diagnostic_reporter = StopControlPrewriteDiagnosticReporter(
            transition_reporter=transition_reporter
        )
        self._prewrite_result_policy = StopControlPrewriteResultPolicy(
            result_factory=result_factory
        )

    def submit(
        self,
        *,
        event: object,
        eligibility_proof: object,
        receipt: object,
    ) -> StopControlSubmissionResult:
        immediate, loop, tracker, envelope = self._admission_coordinator.admit(
            event=event,
            eligibility_proof=eligibility_proof,
            receipt=receipt,
        )
        if immediate is not None:
            return immediate

        self._prewrite_diagnostic_reporter.admission_committed(tracker)
        prewrite_status, released, quarantined = self._prewrite_guard.evaluate(
            tracker
        )
        self._prewrite_diagnostic_reporter.report(
            tracker=tracker,
            status=prewrite_status,
            released=released,
            quarantined=quarantined,
        )
        immediate = self._prewrite_result_policy.resolve(
            tracker=tracker,
            status=prewrite_status,
        )
        if immediate is not None:
            return immediate
        return self._send_outcome_coordinator.deliver(
            tracker=tracker,
            envelope=envelope,
            loop=loop,
        )


__all__ = ("StopControlSubmissionCoordinator",)
