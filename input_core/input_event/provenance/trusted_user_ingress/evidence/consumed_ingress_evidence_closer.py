#20260905_kpopmodder: Atomically closes evidence and clears its capability keys.
from __future__ import annotations

from .consumed_ingress_evidence_lifecycle import (
    ConsumedIngressEvidenceLifecycle,
)
from .routed_response_capability_key_registry import (
    RoutedResponseCapabilityKeyRegistry,
)


class ConsumedIngressEvidenceCloser:
    def __init__(
        self,
        *,
        lifecycle: ConsumedIngressEvidenceLifecycle,
        key_registry: RoutedResponseCapabilityKeyRegistry,
    ) -> None:
        self._lifecycle = lifecycle
        self._key_registry = key_registry

    def close(self) -> bool:
        with self._lifecycle.lock:
            if not self._lifecycle.close_locked():
                return False
            self._key_registry.clear_locked()
            return True


__all__ = ("ConsumedIngressEvidenceCloser",)
