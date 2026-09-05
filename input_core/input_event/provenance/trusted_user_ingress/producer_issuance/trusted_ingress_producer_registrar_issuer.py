#20260905_kpopmodder: Issues registrars after separate producer authorization.
from __future__ import annotations

from ..trusted_ingress_invocation_issuer import TrustedIngressInvocationIssuer


class TrustedIngressProducerRegistrarIssuer:
    def __init__(
        self,
        *,
        registry,
        registry_token: object,
        factory_token: object,
    ) -> None:
        self.issuer = TrustedIngressInvocationIssuer(
            registry=registry,
            registry_token=registry_token,
            factory_token=factory_token,
        )

    def issue(self, *, input_event_adapter, source_policy):
        return self.issuer.issue(
            input_event_adapter=input_event_adapter,
            source_policy=source_policy,
        )


__all__ = ("TrustedIngressProducerRegistrarIssuer",)
