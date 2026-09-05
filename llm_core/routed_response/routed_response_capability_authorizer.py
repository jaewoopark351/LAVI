#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from input_core.input_event.provenance.trusted_user_ingress import (
    RoutedResponseEmissionCapability,
)

from .routed_response_request import RoutedResponseRequest


class RoutedResponseCapabilityAuthorizer:
    def __init__(self, capability_consumer=None) -> None:
        if capability_consumer is not None and not callable(capability_consumer):
            raise TypeError("emission_capability_consumer must be callable")
        self._capability_consumer = capability_consumer

    def authorize(
        self,
        request: RoutedResponseRequest,
        *,
        emission_capability,
        event,
    ) -> tuple[bool, str | None]:
        if type(request) is not RoutedResponseRequest:
            raise TypeError("routed response request must be exact")
        if (
            type(emission_capability) is not RoutedResponseEmissionCapability
            or self._capability_consumer is None
        ):
            return False, "authorization_unavailable"
        try:
            accepted = self._capability_consumer(
                emission_capability,
                event=event,
                text=request.text,
                source=request.source,
                response_kind=request.response_kind,
            )
        except Exception:
            return False, "authorization_failed"
        if accepted is not True:
            return False, "authorization_rejected"
        return True, None


__all__ = ("RoutedResponseCapabilityAuthorizer",)
