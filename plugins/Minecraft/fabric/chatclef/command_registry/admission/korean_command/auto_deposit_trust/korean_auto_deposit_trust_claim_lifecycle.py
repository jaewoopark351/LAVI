#20260905_kpopmodder: Own mutable H5 Korean claim commit and abandonment.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.command_registry.admission.auto_deposit_trust import (
    AutoDepositTrustCommandAdmission,
)
from plugins.Minecraft.fabric.chatclef.command_registry.admission.korean_command_submission_admission_decision import (
    KoreanCommandSubmissionAdmissionDecision,
)


class KoreanAutoDepositTrustClaimLifecycle:
    def __init__(self, admission: AutoDepositTrustCommandAdmission | None):
        self._admission = admission

    def commit(
        self,
        inspection: object,
        route_claim: object,
        request: object,
    ) -> object:
        if (
            not inspection.allowed
            or inspection.command_name
            != AutoDepositTrustCommandAdmission.COMMAND_NAME
        ):
            return inspection
        if self._admission is None:
            return self._decision(
                inspection.command_name,
                inspection.source,
                False,
                "auto_deposit_trust_input_claim_invalid",
                "AUTO_DEPOSIT_TRUST claim owner is unavailable.",
            )
        decision = self._admission.commit(
            command_name=inspection.command_name,
            source=inspection.source,
            route_claim=route_claim,
            request=request,
        )
        return self._decision(
            inspection.command_name,
            inspection.source,
            decision.allowed,
            decision.reason_code,
            decision.message,
        )

    def abandon_if_issued(self, route_claim: object) -> None:
        if self._admission is not None:
            self._admission.abandon_if_issued(route_claim)

    @staticmethod
    def _decision(
        command_name: str,
        source: str,
        allowed: bool,
        reason_code: str,
        message: str,
    ) -> KoreanCommandSubmissionAdmissionDecision:
        return KoreanCommandSubmissionAdmissionDecision(
            allowed=allowed,
            command_name=command_name,
            source=source,
            reason_code=reason_code,
            message=message,
            expose_admission_reason=not allowed,
        )


__all__ = ("KoreanAutoDepositTrustClaimLifecycle",)
