#20260905_kpopmodder: Preserve the trusted VoiceInput-final enqueue facade.
from __future__ import annotations

from core.logger import log_print
from input_core.input_event.provenance.trusted_user_ingress import (
    QueueAcceptanceReceipt,
)

from .trusted_voice import TrustedVoiceInputFinalEnqueueComponentGraph


class TrustedVoiceInputFinalEnqueueCoordinator:
    def __init__(
        self,
        *,
        input_event_adapter,
        producer_registrar_factory,
        claim_aware_queue_sinks,
        pre_accept_observers=(),
        post_accept_observers=(),
        log_callback=log_print,
    ):
        self._components = TrustedVoiceInputFinalEnqueueComponentGraph(
            input_event_adapter=input_event_adapter,
            producer_registrar_factory=producer_registrar_factory,
            claim_aware_queue_sinks=claim_aware_queue_sinks,
            pre_accept_observers=pre_accept_observers,
            post_accept_observers=post_accept_observers,
            log_callback=log_callback,
        )

    def enqueue(self, payload) -> QueueAcceptanceReceipt | None:
        return self._components.runtime.enqueue(payload)

    __call__ = enqueue


__all__ = ("TrustedVoiceInputFinalEnqueueCoordinator",)
