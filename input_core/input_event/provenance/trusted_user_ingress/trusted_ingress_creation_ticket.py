#20260905_kpopmodder: Carries one opaque callback-invocation creation ticket.
from __future__ import annotations

import threading


class TrustedIngressCreationTicket:
    __slots__ = (
        "_adapter",
        "_factory_token",
        "_invocation_token",
        "_lock",
        "_outcome",
        "_policy",
        "_registry_token",
    )

    def __init__(
        self,
        *,
        adapter: object,
        policy: object,
        registry_token: object,
        factory_token: object,
        invocation_token: object,
    ):
        self._adapter = adapter
        self._policy = policy
        self._registry_token = registry_token
        self._factory_token = factory_token
        self._invocation_token = invocation_token
        self._lock = threading.Lock()
        self._outcome = "OPEN"

    @property
    def outcome(self) -> str:
        with self._lock:
            return self._outcome

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
        return "TrustedIngressCreationTicket(<opaque>)"

    def __copy__(self):
        raise TypeError("trusted_ingress_creation_ticket_is_not_copyable")

    def __deepcopy__(self, _memo):
        raise TypeError("trusted_ingress_creation_ticket_is_not_copyable")

    def __reduce__(self):
        raise TypeError("trusted_ingress_creation_ticket_is_not_serializable")

    def __reduce_ex__(self, _protocol):
        raise TypeError("trusted_ingress_creation_ticket_is_not_serializable")


__all__ = ("TrustedIngressCreationTicket",)
