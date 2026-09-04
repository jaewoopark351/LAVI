#20260905_kpopmodder: Own H5 request-source and staged-readiness policy only.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.command_registry.contracts import (
    ChatClefCommandSpec,
)

from .auto_deposit_trust_command_admission_decision import (
    AutoDepositTrustCommandAdmissionDecision,
)


class AutoDepositTrustSourceReadinessAdmission:
    COMMAND_NAME = "auto_deposit_trust"

    def inspect(
        self,
        command_name: str,
        source: str,
        spec: ChatClefCommandSpec,
    ) -> AutoDepositTrustCommandAdmissionDecision:
        if command_name != self.COMMAND_NAME:
            return AutoDepositTrustCommandAdmissionDecision(True)
        if source not in spec.allowed_input_sources:
            return self._reject(
                "auto_deposit_trust_source_not_allowed",
                "AUTO_DEPOSIT_TRUST input source is not admitted.",
            )
        readiness = spec.readiness_axes
        if not (
            readiness.source_registered
            and readiness.korean_parse_compile_ready
            and readiness.python_admission_ready
        ):
            return self._reject(
                "auto_deposit_trust_python_admission_not_ready",
                "AUTO_DEPOSIT_TRUST Python admission is not ready.",
            )
        if readiness.public_korean_enabled and not readiness.bridge_lifecycle_ready:
            return self._reject(
                "auto_deposit_trust_public_readiness_incomplete",
                "AUTO_DEPOSIT_TRUST cannot be public-enabled before bridge lifecycle verification.",
            )
        return AutoDepositTrustCommandAdmissionDecision(True)

    def _reject(
        self,
        reason_code: str,
        message: str,
    ) -> AutoDepositTrustCommandAdmissionDecision:
        return AutoDepositTrustCommandAdmissionDecision(
            False,
            reason_code,
            message,
        )
