#20260905_kpopmodder: Consumes routed-response authority for one ingress registry.
from __future__ import annotations

from ..routed_response_emission_capability_consumer import (
    RoutedResponseEmissionCapabilityConsumer,
)


class TrustedRoutedResponseCapabilityConsumer:
    def __init__(self, *, registry_token: object) -> None:
        self.consumer = RoutedResponseEmissionCapabilityConsumer(
            registry_token=registry_token
        )

    def consume(
        self,
        capability: object,
        *,
        event: object,
        text: str,
        source: str,
        response_kind: str = "immediate",
    ) -> bool:
        return self.consumer.consume(
            capability,
            event=event,
            text=text,
            source=source,
            response_kind=response_kind,
        )


__all__ = ("TrustedRoutedResponseCapabilityConsumer",)
