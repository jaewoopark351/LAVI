#20260905_kpopmodder: Composes focused consumed-ingress evidence collaborators.
from __future__ import annotations

from .consumed_ingress_evidence_lifecycle import (
    ConsumedIngressEvidenceLifecycle,
)
from .consumed_ingress_evidence_closer import ConsumedIngressEvidenceCloser
from .consumed_ingress_response_capability_issuer import (
    ConsumedIngressResponseCapabilityIssuer,
)
from .routed_response_capability_key_registry import (
    RoutedResponseCapabilityKeyRegistry,
)


class ConsumedIngressEvidenceComponentGraph:
    def __init__(
        self,
        *,
        event: object,
        event_signature: tuple[object, ...],
        registry_token: object,
    ) -> None:
        self.lifecycle = ConsumedIngressEvidenceLifecycle(
            event=event,
            event_signature=event_signature,
        )
        self.capability_key_registry = RoutedResponseCapabilityKeyRegistry()
        self.response_capability_issuer = ConsumedIngressResponseCapabilityIssuer(
            lifecycle=self.lifecycle,
            key_registry=self.capability_key_registry,
            registry_token=registry_token,
        )
        self.closer = ConsumedIngressEvidenceCloser(
            lifecycle=self.lifecycle,
            key_registry=self.capability_key_registry,
        )


__all__ = ("ConsumedIngressEvidenceComponentGraph",)
