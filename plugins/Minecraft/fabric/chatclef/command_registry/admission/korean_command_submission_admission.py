#20260827_kpopmodder: Keep Korean command rollout policy outside the extension facade.
#20260905_kpopmodder: Route STOP only through its trusted control claim instead of ordinary admission.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.command_registry.admission.auto_deposit_trust import (
    AutoDepositTrustCommandAdmission,
)
from plugins.Minecraft.fabric.chatclef.command_registry.admission.korean_command import (
    KoreanAutoDepositTrustSubmissionPolicy,
    KoreanPublicCommandSubmissionPolicy,
    KoreanStopCommandSubmissionPolicy,
)
from plugins.Minecraft.fabric.chatclef.command_registry.admission.korean_command_submission_admission_decision import (
    KoreanCommandSubmissionAdmissionDecision,
)
from plugins.Minecraft.fabric.chatclef.command_registry.admission.store_home_command_admission import (
    StoreHomeCommandAdmission,
)
from plugins.Minecraft.fabric.chatclef.input.confirmation.confirmation_receipt import KoreanCommandConfirmationReceipt


class KoreanCommandSubmissionAdmission:
    def __init__(
        self,
        store_home_admission: StoreHomeCommandAdmission | None = None,
        auto_deposit_trust_admission: AutoDepositTrustCommandAdmission | None = None,
    ):
        self._stop_policy = KoreanStopCommandSubmissionPolicy()
        self._auto_deposit_trust_policy = KoreanAutoDepositTrustSubmissionPolicy(
            auto_deposit_trust_admission
        )
        self._public_policy = KoreanPublicCommandSubmissionPolicy(
            store_home_admission
        )

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
            return self._public_policy.unknown(command, request_source)

        stop_decision = self._stop_policy.inspect(command, request_source)
        if stop_decision is not None:
            return stop_decision

        h5_decision = self._auto_deposit_trust_policy.inspect(
            command,
            request_source,
            spec,
            route_claim,
        )
        if h5_decision is not None:
            return h5_decision
        public = self._public_policy.inspect(command, request_source, spec)
        if not public.allowed:
            return public
        if spec.safety_tier in {"R3", "R4"}:
            if (type(route_claim) is not KoreanCommandConfirmationReceipt
                    or not route_claim.matches_admission(command, request_source)):
                return KoreanCommandSubmissionAdmissionDecision(
                    allowed=False, command_name=command, source=request_source,
                    reason_code="korean_confirmation_required",
                    message="이 명령은 신뢰할 수 있는 한국어 입력에서 요청을 확인한 뒤 실행할 수 있어.",
                    expose_admission_reason=True,
                )
        return public

    def commit(
        self,
        inspection: KoreanCommandSubmissionAdmissionDecision,
        route_claim: object,
        request: object,
    ) -> KoreanCommandSubmissionAdmissionDecision:
        if type(route_claim) is KoreanCommandConfirmationReceipt:
            if not inspection.allowed or not route_claim.commit(request):
                return KoreanCommandSubmissionAdmissionDecision(
                    allowed=False, command_name=inspection.command_name, source=inspection.source,
                    reason_code="korean_confirmation_binding_rejected",
                    message="확인한 요청의 연결이나 대상 정보가 달라져 실행하지 않았어.",
                    expose_admission_reason=True,
                )
            return inspection
        return self._auto_deposit_trust_policy.commit(
            inspection,
            route_claim,
            request,
        )

    def abandon_if_issued(self, route_claim: object) -> None:
        if type(route_claim) is KoreanCommandConfirmationReceipt:
            route_claim.abandon()
            return
        self._auto_deposit_trust_policy.abandon_if_issued(route_claim)
