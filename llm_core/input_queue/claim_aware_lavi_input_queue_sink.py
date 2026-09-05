#20260905_kpopmodder: Keeps the claim-aware queue sink API as a focused facade.
from __future__ import annotations

from core.logger import log_print
from input_core.input_event.provenance.trusted_user_ingress import (
    QueueAcceptanceReceipt,
    RegisteredIngressDelivery,
)

from .claim_aware_lavi_input_queue_committer import (
    ClaimAwareLlmInputQueueCommitter,
)
from .llm_input_queue_acceptance_effects import LlmInputQueueAcceptanceEffects


class ClaimAwareLlmInputQueueSink:
    def __init__(self, worker, *, log_callback=log_print):
        self._committer = ClaimAwareLlmInputQueueCommitter(
            worker,
            log_callback=log_callback,
        )
        self._acceptance_effects = LlmInputQueueAcceptanceEffects(
            worker,
            log_callback=log_callback,
        )

    def offer_registered(
        self,
        delivery: RegisteredIngressDelivery,
    ) -> QueueAcceptanceReceipt | None:
        receipt = self._committer.commit(delivery)
        if receipt is None:
            return None
        self._acceptance_effects.run()
        return receipt


__all__ = ("ClaimAwareLlmInputQueueSink",)
