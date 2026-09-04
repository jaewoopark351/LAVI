#20260827_kpopmodder: Keep Korean command rollout policy outside the extension facade.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.command_registry.admission.korean_command_submission_admission_decision import (
    KoreanCommandSubmissionAdmissionDecision,
)
from plugins.Minecraft.fabric.chatclef.command_registry.admission.store_home_command_admission import (
    StoreHomeCommandAdmission,
)
from plugins.Minecraft.fabric.chatclef.command_registry.admission.auto_deposit_trust import (
    AutoDepositTrustCommandAdmission,
)


class KoreanCommandSubmissionAdmission:
    _NOT_PUBLIC_MESSAGE = (
        "Korean command parsed successfully but is not public-enabled for "
        "automatic Minecraft submission."
    )

    def __init__(
        self,
        store_home_admission: StoreHomeCommandAdmission | None = None,
        auto_deposit_trust_admission: AutoDepositTrustCommandAdmission | None = None,
    ):
        self._store_home_admission = (
            store_home_admission or StoreHomeCommandAdmission()
        )
        self._auto_deposit_trust_admission = auto_deposit_trust_admission

    def inspect(
        self,
        command_name: str,
        source: str,
        registry: object,
        route_claim: object = None,
    ) -> KoreanCommandSubmissionAdmissionDecision:
        command = str(command_name or "").strip().lower()
        request_source = source if type(source) is str else ""
        try:
            spec = registry.spec(command)
        except KeyError:
            return self._not_public(command, request_source)

        if command == AutoDepositTrustCommandAdmission.COMMAND_NAME:
            if self._auto_deposit_trust_admission is None:
                return self._decision(
                    command,
                    request_source,
                    False,
                    "auto_deposit_trust_input_claim_required",
                    "AUTO_DEPOSIT_TRUST requires router-owned admission.",
                    True,
                )
            h5 = self._auto_deposit_trust_admission.inspect(
                command,
                request_source,
                spec,
                route_claim,
            )
            if not h5.allowed:
                return self._decision(
                    command,
                    request_source,
                    False,
                    h5.reason_code,
                    h5.message,
                    True,
                )

        store_home = self._store_home_admission.inspect(
            command,
            request_source,
            spec,
        )
        if not store_home.allowed:
            return KoreanCommandSubmissionAdmissionDecision(
                allowed=False,
                command_name=command,
                source=request_source,
                reason_code=store_home.reason_code,
                message=store_home.message,
                expose_admission_reason=True,
            )
        if spec.readiness_axes.public_korean_enabled:
            if request_source not in spec.allowed_input_sources:
                return self._decision(
                    command,
                    request_source,
                    False,
                    "korean_command_source_not_allowed",
                    "This input source is not admitted for Korean command submission.",
                    True,
                )
            return KoreanCommandSubmissionAdmissionDecision(
                allowed=True,
                command_name=command,
                source=request_source,
            )
        return self._not_public(command, request_source)

    def commit(
        self,
        inspection: KoreanCommandSubmissionAdmissionDecision,
        route_claim: object,
        request: object,
    ) -> KoreanCommandSubmissionAdmissionDecision:
        if not inspection.allowed:
            return inspection
        if inspection.command_name != AutoDepositTrustCommandAdmission.COMMAND_NAME:
            return inspection
        if self._auto_deposit_trust_admission is None:
            return self._decision(
                inspection.command_name,
                inspection.source,
                False,
                "auto_deposit_trust_input_claim_invalid",
                "AUTO_DEPOSIT_TRUST claim owner is unavailable.",
                True,
            )
        decision = self._auto_deposit_trust_admission.commit(
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
            not decision.allowed,
        )

    def abandon_if_issued(self, route_claim: object) -> None:
        if self._auto_deposit_trust_admission is not None:
            self._auto_deposit_trust_admission.abandon_if_issued(route_claim)

    def _decision(
        self,
        command_name: str,
        source: str,
        allowed: bool,
        reason_code: str = "",
        message: str = "",
        expose: bool = False,
    ) -> KoreanCommandSubmissionAdmissionDecision:
        return KoreanCommandSubmissionAdmissionDecision(
            allowed=allowed,
            command_name=command_name,
            source=source,
            reason_code=reason_code,
            message=message,
            expose_admission_reason=expose,
        )

    def _not_public(
        self,
        command_name: str,
        source: str,
    ) -> KoreanCommandSubmissionAdmissionDecision:
        return KoreanCommandSubmissionAdmissionDecision(
            allowed=False,
            command_name=command_name,
            source=source,
            reason_code="korean_command_not_public",
            message=self._NOT_PUBLIC_MESSAGE,
        )
