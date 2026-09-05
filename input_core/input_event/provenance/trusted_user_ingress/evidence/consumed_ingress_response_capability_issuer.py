#20260905_kpopmodder: Issues one response capability from live consumed evidence.
from __future__ import annotations

from ..routed_response_emission_capability import (
    RoutedResponseEmissionCapability,
)
from .consumed_ingress_evidence_lifecycle import (
    ConsumedIngressEvidenceLifecycle,
)
from .routed_response_capability_key_registry import (
    RoutedResponseCapabilityKeyRegistry,
)


class ConsumedIngressResponseCapabilityIssuer:
    def __init__(
        self,
        *,
        lifecycle: ConsumedIngressEvidenceLifecycle,
        key_registry: RoutedResponseCapabilityKeyRegistry,
        registry_token: object,
    ) -> None:
        self._lifecycle = lifecycle
        self._key_registry = key_registry
        self._registry_token = registry_token

    def issue(
        self,
        event: object,
        owner: object,
        *,
        text: str,
        source: str,
        response_kind: str = "immediate",
    ) -> RoutedResponseEmissionCapability | None:
        if (
            not self._lifecycle.matches_event(event)
            or type(text) is not str
            or not text
            or type(source) is not str
            or not source
            or type(response_kind) is not str
            or not response_kind
        ):
            return None
        key = (response_kind, source)
        with self._lifecycle.lock:
            if not self._lifecycle.is_live_for_locked(owner):
                return None
            if owner is None or not self._key_registry.reserve_locked(key):
                return None
            return RoutedResponseEmissionCapability._issue(
                registry_token=self._registry_token,
                event=self._lifecycle.event,
                event_signature=self._lifecycle.event_signature,
                text=text,
                source=source,
                response_kind=response_kind,
            )


__all__ = ("ConsumedIngressResponseCapabilityIssuer",)
