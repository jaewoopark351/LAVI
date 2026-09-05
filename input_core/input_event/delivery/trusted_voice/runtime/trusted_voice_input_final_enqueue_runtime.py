#20260905_kpopmodder: Sequence one registered VoiceInput-final queue handoff.
from __future__ import annotations

from input_core.input_event.provenance.trusted_user_ingress import (
    QueueAcceptanceReceipt,
)


class TrustedVoiceInputFinalEnqueueRuntime:
    def __init__(
        self,
        *,
        delivery_factory,
        queue_acceptance,
        pre_accept_notifier,
        post_accept_notifier,
        abandoner,
    ):
        self._delivery_factory = delivery_factory
        self._queue_acceptance = queue_acceptance
        self._pre_accept_notifier = pre_accept_notifier
        self._post_accept_notifier = post_accept_notifier
        self._abandoner = abandoner

    def enqueue(self, payload) -> QueueAcceptanceReceipt | None:
        delivery = self._delivery_factory.create(payload)
        if delivery is None:
            return None
        if not self._pre_accept_notifier.notify(delivery.event):
            self._abandoner.abandon(delivery)
            return None
        receipt = self._queue_acceptance.offer(delivery)
        if receipt is None:
            self._abandoner.abandon(delivery)
            return None
        self._post_accept_notifier.notify(delivery.event)
        return receipt


__all__ = ("TrustedVoiceInputFinalEnqueueRuntime",)
