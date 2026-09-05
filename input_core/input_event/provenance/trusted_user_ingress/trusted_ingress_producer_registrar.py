#20260905_kpopmodder: Adapts and registers one factory-bound callback invocation exactly once.
from __future__ import annotations

import threading


_ISSUANCE_TOKEN = object()


class TrustedIngressProducerRegistrar:
    __slots__ = (
        "_adapter",
        "_capability",
        "_created_event",
        "_last_rejection_reason",
        "_lock",
        "_registry",
        "_started",
        "_ticket",
    )

    def __init__(
        self,
        *,
        adapter: object,
        registry: object,
        ticket: object,
        capability: object,
        _issuance_token: object = None,
    ):
        if _issuance_token is not _ISSUANCE_TOKEN:
            raise TypeError(
                "TrustedIngressProducerRegistrar is issued only by its factory"
            )
        self._adapter = adapter
        self._registry = registry
        self._ticket = ticket
        self._capability = capability
        self._lock = threading.Lock()
        self._started = False
        self._created_event = None
        self._last_rejection_reason = ""

    @classmethod
    def _issue(
        cls,
        *,
        adapter: object,
        registry: object,
        ticket: object,
        capability: object,
    ) -> "TrustedIngressProducerRegistrar":
        return cls(
            adapter=adapter,
            registry=registry,
            ticket=ticket,
            capability=capability,
            _issuance_token=_ISSUANCE_TOKEN,
        )

    @property
    def creation_ticket(self):
        return self._ticket

    @property
    def created_event(self):
        return self._created_event

    @property
    def last_rejection_reason(self) -> str:
        return self._last_rejection_reason

    def create_registered_delivery(self, payload: object):
        with self._lock:
            if self._started:
                self._last_rejection_reason = (
                    "trusted_ingress_producer_capability_spent"
                )
                return None
            self._started = True

        try:
            event = self._adapter.adapt(payload)
            self._created_event = event
            delivery, reason = self._registry._register_from_producer(
                ticket=self._ticket,
                capability=self._capability,
                event=event,
            )
        except Exception:
            self._consume_capabilities("REJECTED")
            raise

        outcome = "REGISTERED" if delivery is not None else "REJECTED"
        self._consume_capabilities(outcome)
        self._last_rejection_reason = reason
        return delivery

    def abandon_before_registration(self) -> bool:
        with self._lock:
            if self._started:
                return False
            self._started = True
        return self._consume_capabilities("ABANDONED")

    def _consume_capabilities(self, outcome: str) -> bool:
        ticket_consumed = self._ticket._consume(outcome)
        capability_consumed = self._capability._consume(outcome)
        return ticket_consumed and capability_consumed

    def __repr__(self) -> str:
        return "TrustedIngressProducerRegistrar(<opaque>)"

    def __copy__(self):
        raise TypeError("trusted_ingress_producer_registrar_is_not_copyable")

    def __deepcopy__(self, _memo):
        raise TypeError("trusted_ingress_producer_registrar_is_not_copyable")

    def __reduce__(self):
        raise TypeError("trusted_ingress_producer_registrar_is_not_serializable")

    def __reduce_ex__(self, _protocol):
        raise TypeError("trusted_ingress_producer_registrar_is_not_serializable")


__all__ = ("TrustedIngressProducerRegistrar",)
