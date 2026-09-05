#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from .trusted_ingress_creation_ticket import TrustedIngressCreationTicket
from .trusted_ingress_producer_capability import (
    TrustedIngressProducerCapability,
)
from .trusted_ingress_producer_registrar import TrustedIngressProducerRegistrar


class TrustedIngressInvocationIssuer:
    def __init__(
        self,
        *,
        registry,
        registry_token: object,
        factory_token: object,
    ) -> None:
        if registry_token is None or factory_token is None:
            raise ValueError("registry and factory tokens are required")
        self._registry = registry
        self._registry_token = registry_token
        self._factory_token = factory_token

    def issue(self, *, input_event_adapter, source_policy):
        invocation_token = object()
        ticket = TrustedIngressCreationTicket(
            adapter=input_event_adapter,
            policy=source_policy,
            registry_token=self._registry_token,
            factory_token=self._factory_token,
            invocation_token=invocation_token,
        )
        capability = TrustedIngressProducerCapability(
            ticket=ticket,
            registry_token=self._registry_token,
            factory_token=self._factory_token,
            invocation_token=invocation_token,
        )
        return TrustedIngressProducerRegistrar._issue(
            adapter=input_event_adapter,
            registry=self._registry,
            ticket=ticket,
            capability=capability,
        )


__all__ = ("TrustedIngressInvocationIssuer",)
