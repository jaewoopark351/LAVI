#20260905_kpopmodder: Inspect, commit, and abandon one opaque H5 route claim.
from __future__ import annotations

from .auto_deposit_trust_command_admission_decision import (
    AutoDepositTrustCommandAdmissionDecision,
)


class AutoDepositTrustClaimedSubmissionAuthorizer:
    COMMAND_NAME = "auto_deposit_trust"

    def __init__(self, claim_registry: object):
        self.claim_registry = claim_registry

    def inspect(
        self,
        *,
        command_name: str,
        source: str,
        route_claim: object = None,
    ) -> AutoDepositTrustCommandAdmissionDecision:
        if command_name != self.COMMAND_NAME:
            return AutoDepositTrustCommandAdmissionDecision(True)
        if route_claim is None:
            return self._reject(
                "auto_deposit_trust_input_claim_required",
                "AUTO_DEPOSIT_TRUST requires a router-issued input claim.",
            )
        inspector = getattr(self.claim_registry, "inspect", None)
        if not callable(inspector) or not inspector(
            route_claim,
            source=source,
            command_name=command_name,
        ):
            return self._reject(
                "auto_deposit_trust_input_claim_invalid",
                "AUTO_DEPOSIT_TRUST input claim is invalid or already spent.",
            )
        return AutoDepositTrustCommandAdmissionDecision(True)

    def commit(
        self,
        *,
        command_name: str,
        source: str,
        route_claim: object,
        request: object,
    ) -> AutoDepositTrustCommandAdmissionDecision:
        if command_name != self.COMMAND_NAME:
            return AutoDepositTrustCommandAdmissionDecision(True)
        try:
            committed = self.claim_registry.commit(
                route_claim,
                source=source,
                command_name=command_name,
                request=request,
            )
        except Exception:
            committed = False
        if not committed:
            return self._reject(
                "auto_deposit_trust_input_claim_invalid",
                "AUTO_DEPOSIT_TRUST input claim could not be committed.",
            )
        return AutoDepositTrustCommandAdmissionDecision(True)

    def abandon_if_issued(self, route_claim: object) -> None:
        abandon = getattr(self.claim_registry, "abandon_if_issued", None)
        if callable(abandon):
            try:
                abandon(route_claim)
            except Exception:
                return

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
