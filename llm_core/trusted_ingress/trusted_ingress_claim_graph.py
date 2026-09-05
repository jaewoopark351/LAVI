#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from input_core.input_event.provenance.trusted_user_ingress import (
    TrustedIngressProducerRegistrarFactory,
    TrustedUserInputIngressClaimRegistry,
)


class TrustedIngressClaimGraph:
    def __init__(
        self,
        *,
        registry=None,
        producer_registrar_factory=None,
    ) -> None:
        if registry is None and producer_registrar_factory is not None:
            registry = getattr(producer_registrar_factory, "registry", None)
        if registry is None:
            registry = TrustedUserInputIngressClaimRegistry()
        if type(registry) is not TrustedUserInputIngressClaimRegistry:
            raise TypeError(
                "registry must be TrustedUserInputIngressClaimRegistry"
            )
        if producer_registrar_factory is None:
            producer_registrar_factory = TrustedIngressProducerRegistrarFactory(
                registry
            )
        if (
            type(producer_registrar_factory)
            is not TrustedIngressProducerRegistrarFactory
        ):
            raise TypeError(
                "producer_registrar_factory must be "
                "TrustedIngressProducerRegistrarFactory"
            )
        if producer_registrar_factory.registry is not registry:
            raise ValueError("producer registrar factory registry must match")
        self.registry = registry
        self.producer_registrar_factory = producer_registrar_factory

    def validate_consumed_evidence(self, event, evidence):
        return self.registry.validate_consumed_evidence(event, evidence)


__all__ = ("TrustedIngressClaimGraph",)
