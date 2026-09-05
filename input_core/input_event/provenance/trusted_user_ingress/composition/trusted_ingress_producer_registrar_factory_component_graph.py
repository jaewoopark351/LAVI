#20260905_kpopmodder: Composes trusted producer binding and registrar issuance.
from __future__ import annotations

from ..producer_binding import TrustedIngressProducerBindingAuthority
from ..producer_issuance import TrustedIngressProducerRegistrarIssuer


class TrustedIngressProducerRegistrarFactoryComponentGraph:
    def __init__(self, *, registry) -> None:
        self.factory_token = object()
        self.binding_authority = TrustedIngressProducerBindingAuthority()
        self.registry_token = registry._bind_producer_factory(self.factory_token)
        self.registrar_issuer = TrustedIngressProducerRegistrarIssuer(
            registry=registry,
            registry_token=self.registry_token,
            factory_token=self.factory_token,
        )


__all__ = ("TrustedIngressProducerRegistrarFactoryComponentGraph",)
