#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from core.logger import log_print
from input_core.input_event.provenance.trusted_user_ingress import (
    QueueAcceptanceReceipt,
    RegisteredIngressDelivery,
)


class ClaimAwareLlmInputQueueCommitter:
    def __init__(self, worker, *, log_callback=log_print) -> None:
        if not callable(getattr(worker.input_queue, "put", None)):
            raise TypeError("worker.input_queue.put must be callable")
        if not hasattr(worker.input_queue_lock, "__enter__"):
            raise TypeError("worker.input_queue_lock must be a context manager")
        if not callable(log_callback):
            raise TypeError("log_callback must be callable")
        self._worker = worker
        self._log_callback = log_callback

    def commit(
        self,
        delivery: RegisteredIngressDelivery,
    ) -> QueueAcceptanceReceipt | None:
        if type(delivery) is not RegisteredIngressDelivery:
            return None
        queue_entry_token = object()
        with self._worker.input_queue_lock:
            queue_delivery = delivery._acquire_queue_ownership(queue_entry_token)
            if queue_delivery is None:
                return None
            try:
                self._worker.input_queue.put(queue_delivery)
            except Exception:
                queue_delivery.abandon()
                self._log_bounded_failure()
                return None
            return QueueAcceptanceReceipt._issue(
                queue_delivery=queue_delivery,
                queue_entry_token=queue_entry_token,
            )

    def _log_bounded_failure(self) -> None:
        try:
            self._log_callback(
                "[ClaimAwareLlmInputQueueSink] queue insertion failed"
            )
        except Exception:
            return


__all__ = ("ClaimAwareLlmInputQueueCommitter",)
