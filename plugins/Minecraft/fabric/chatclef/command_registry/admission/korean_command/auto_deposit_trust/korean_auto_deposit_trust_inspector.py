#20260905_kpopmodder: Own read-only H5 Korean submission admission decisions.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.command_registry.admission.auto_deposit_trust import (
    AutoDepositTrustCommandAdmission,
)
from plugins.Minecraft.fabric.chatclef.command_registry.admission.korean_command_submission_admission_decision import (
    KoreanCommandSubmissionAdmissionDecision,
)


class KoreanAutoDepositTrustInspector:
    def __init__(self, admission: AutoDepositTrustCommandAdmission | None):
        self._admission = admission

    def inspect(
        self,
        command_name: str,
        source: str,
        spec: object,
        route_claim: object,
    ) -> KoreanCommandSubmissionAdmissionDecision | None:
        if command_name != AutoDepositTrustCommandAdmission.COMMAND_NAME:
            return None
        if self._admission is None:
            return self._decision(
                command_name,
                source,
                "auto_deposit_trust_input_claim_required",
                "AUTO_DEPOSIT_TRUST requires router-owned admission.",
            )
        decision = self._admission.inspect(
            command_name,
            source,
            spec,
            route_claim,
        )
        if decision.allowed:
            return None
        return self._decision(
            command_name,
            source,
            decision.reason_code,
            decision.message,
        )

    @staticmethod
    def _decision(
        command_name: str,
        source: str,
        reason_code: str,
        message: str,
    ) -> KoreanCommandSubmissionAdmissionDecision:
        return KoreanCommandSubmissionAdmissionDecision(
            allowed=False,
            command_name=command_name,
            source=source,
            reason_code=reason_code,
            message=message,
            expose_admission_reason=True,
        )


__all__ = ("KoreanAutoDepositTrustInspector",)
