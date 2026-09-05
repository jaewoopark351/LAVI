#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from .routed_response_emission_capability import RoutedResponseEmissionCapability


class RoutedResponseEmissionCapabilityConsumer:
    def __init__(self, *, registry_token: object) -> None:
        if registry_token is None:
            raise ValueError("registry_token is required")
        self._registry_token = registry_token

    def consume(
        self,
        capability: object,
        *,
        event: object,
        text: str,
        source: str,
        response_kind: str = "immediate",
    ) -> bool:
        if type(capability) is not RoutedResponseEmissionCapability:
            return False
        try:
            return capability._consume(
                registry_token=self._registry_token,
                event=event,
                text=text,
                source=source,
                response_kind=response_kind,
            ) is True
        except Exception:
            return False


__all__ = ("RoutedResponseEmissionCapabilityConsumer",)
