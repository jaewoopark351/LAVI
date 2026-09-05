#20260905_kpopmodder: Preserve H5 submission admission as a composition-only facade.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.command_registry.admission.auto_deposit_trust import (
    AutoDepositTrustCommandAdmission,
)

from plugins.Minecraft.fabric.chatclef.command_registry.admission.korean_command.auto_deposit_trust.korean_auto_deposit_trust_inspector import (
    KoreanAutoDepositTrustInspector,
)
from plugins.Minecraft.fabric.chatclef.command_registry.admission.korean_command.auto_deposit_trust.korean_auto_deposit_trust_claim_lifecycle import (
    KoreanAutoDepositTrustClaimLifecycle,
)


class KoreanAutoDepositTrustSubmissionPolicy:
    def __init__(
        self,
        admission: AutoDepositTrustCommandAdmission | None,
    ):
        self._admission = admission
        self._inspector = KoreanAutoDepositTrustInspector(admission)
        self._claim_lifecycle = KoreanAutoDepositTrustClaimLifecycle(admission)

    def inspect(
        self,
        command_name: str,
        source: str,
        spec: object,
        route_claim: object,
    ) -> object | None:
        return self._inspector.inspect(
            command_name,
            source,
            spec,
            route_claim,
        )

    def commit(
        self,
        inspection: object,
        route_claim: object,
        request: object,
    ) -> object:
        return self._claim_lifecycle.commit(
            inspection,
            route_claim,
            request,
        )

    def abandon_if_issued(self, route_claim: object) -> None:
        self._claim_lifecycle.abandon_if_issued(route_claim)


__all__ = ("KoreanAutoDepositTrustSubmissionPolicy",)
