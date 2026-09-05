#20260905_kpopmodder: Composes claim and routed-response ingress authorities.
from __future__ import annotations

from ..claim_lifecycle import TrustedIngressClaimLifecycle
from ..response_capability import (
    TrustedConsumedIngressEvidenceAuthority,
    TrustedRoutedResponseCapabilityConsumer,
)


class TrustedUserInputIngressClaimRegistryComponentGraph:
    def __init__(self, *, capacity: int) -> None:
        self.claim_lifecycle = TrustedIngressClaimLifecycle(capacity=capacity)
        registry_token = self.claim_lifecycle.state_store.registry_token
        self.consumed_evidence_authority = (
            TrustedConsumedIngressEvidenceAuthority(
                registry_token=registry_token
            )
        )
        self.response_capability_consumer = (
            TrustedRoutedResponseCapabilityConsumer(
                registry_token=registry_token
            )
        )


__all__ = ("TrustedUserInputIngressClaimRegistryComponentGraph",)
