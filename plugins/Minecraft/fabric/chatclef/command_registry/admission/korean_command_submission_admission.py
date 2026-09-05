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
        return self._public_policy.inspect(command, request_source, spec)

    def commit(
        self,
        inspection: KoreanCommandSubmissionAdmissionDecision,
        route_claim: object,
        request: object,
    ) -> KoreanCommandSubmissionAdmissionDecision:
        return self._auto_deposit_trust_policy.commit(
            inspection,
            route_claim,
            request,
        )

    def abandon_if_issued(self, route_claim: object) -> None:
        self._auto_deposit_trust_policy.abandon_if_issued(route_claim)
