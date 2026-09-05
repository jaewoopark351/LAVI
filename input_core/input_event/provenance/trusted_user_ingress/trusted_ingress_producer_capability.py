#20260905_kpopmodder: Keeps one factory-issued producer capability process-local and one-shot.
from __future__ import annotations

import threading


class TrustedIngressProducerCapability:
    __slots__ = (
        "_factory_token",
        "_invocation_token",
        "_lock",
        "_outcome",
        "_registry_token",
        "_ticket",
    )

    def __init__(
        self,
        *,
        ticket: object,
        registry_token: object,
        factory_token: object,
        invocation_token: object,
    ):
        self._ticket = ticket
        self._registry_token = registry_token
        self._factory_token = factory_token
        self._invocation_token = invocation_token
        self._lock = threading.Lock()
        self._outcome = "OPEN"

    def _is_open(self) -> bool:
        with self._lock:
            return self._outcome == "OPEN"

    def _consume(self, outcome: str) -> bool:
        with self._lock:
            if self._outcome != "OPEN":
                return False
            self._outcome = outcome
            return True

    def __repr__(self) -> str:
        return "TrustedIngressProducerCapability(<opaque>)"

    def __copy__(self):
        raise TypeError("trusted_ingress_producer_capability_is_not_copyable")

    def __deepcopy__(self, _memo):
        raise TypeError("trusted_ingress_producer_capability_is_not_copyable")

    def __reduce__(self):
        raise TypeError("trusted_ingress_producer_capability_is_not_serializable")

    def __reduce_ex__(self, _protocol):
        raise TypeError("trusted_ingress_producer_capability_is_not_serializable")


__all__ = ("TrustedIngressProducerCapability",)
