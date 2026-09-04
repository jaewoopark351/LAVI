#20260905_kpopmodder: Compose H5 source/readiness and opaque-claim authorization.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.command_registry.contracts import (
    ChatClefCommandSpec,
)

from .auto_deposit_trust_claimed_submission_authorizer import (
    AutoDepositTrustClaimedSubmissionAuthorizer,
)
from .auto_deposit_trust_command_admission_decision import (
    AutoDepositTrustCommandAdmissionDecision,
)
from .auto_deposit_trust_source_readiness_admission import (
    AutoDepositTrustSourceReadinessAdmission,
)


class AutoDepositTrustCommandAdmission:
    COMMAND_NAME = "auto_deposit_trust"

    def __init__(
        self,
        *,
        source_readiness: AutoDepositTrustSourceReadinessAdmission | None = None,
        authorizer: AutoDepositTrustClaimedSubmissionAuthorizer,
    ):
        self._source_readiness = (
            source_readiness or AutoDepositTrustSourceReadinessAdmission()
        )
        self.authorizer = authorizer

    def inspect(
        self,
        command_name: str,
        source: str,
        spec: ChatClefCommandSpec,
        route_claim: object = None,
    ) -> AutoDepositTrustCommandAdmissionDecision:
        source_decision = self._source_readiness.inspect(
            command_name,
            source,
            spec,
        )
        if not source_decision.allowed:
            return source_decision
        return self.authorizer.inspect(
            command_name=command_name,
            source=source,
            route_claim=route_claim,
        )

    def commit(
        self,
        *,
        command_name: str,
        source: str,
        route_claim: object,
        request: object,
    ) -> AutoDepositTrustCommandAdmissionDecision:
        return self.authorizer.commit(
            command_name=command_name,
            source=source,
            route_claim=route_claim,
            request=request,
        )

    def abandon_if_issued(self, route_claim: object) -> None:
        self.authorizer.abandon_if_issued(route_claim)
