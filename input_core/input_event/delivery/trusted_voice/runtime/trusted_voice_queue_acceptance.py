#20260905_kpopmodder: Own exactly-one queue sink and receipt validation.
from __future__ import annotations

from input_core.input_event.provenance.trusted_user_ingress import (
    QueueAcceptanceReceipt,
)


class TrustedVoiceQueueAcceptance:
    def __init__(self, sinks, diagnostics):
        self._sinks = tuple(sinks)
        self._diagnostics = diagnostics

    def offer(self, delivery) -> QueueAcceptanceReceipt | None:
        if len(self._sinks) != 1:
            return None
        offer_registered = getattr(self._sinks[0], "offer_registered", None)
        if not callable(offer_registered):
            return None
        try:
            receipt = offer_registered(delivery)
        except Exception:
            self._diagnostics.log_failure("queue_offer")
            return None
        if type(receipt) is not QueueAcceptanceReceipt:
            return None
        try:
            if not receipt.matches_registered_delivery(delivery):
                return None
        except Exception:
            self._diagnostics.log_failure("receipt_validation")
            return None
        return receipt


__all__ = ("TrustedVoiceQueueAcceptance",)
