#20260905_kpopmodder: Isolate Korean STOP admission from ordinary command policy.
from __future__ import annotations

from ..korean_command_submission_admission_decision import (
    KoreanCommandSubmissionAdmissionDecision,
)


class KoreanStopCommandSubmissionPolicy:
    COMMAND_NAME = "stop"

    def inspect(
        self,
        command_name: str,
        source: str,
    ) -> KoreanCommandSubmissionAdmissionDecision | None:
        if command_name != self.COMMAND_NAME:
            return None
        return KoreanCommandSubmissionAdmissionDecision(
            allowed=False,
            command_name=command_name,
            source=source,
            reason_code="stop_control_claim_required",
            message=(
                "STOP requires trusted Chat or final-microphone control admission."
            ),
            expose_admission_reason=True,
        )


__all__ = ("KoreanStopCommandSubmissionPolicy",)
