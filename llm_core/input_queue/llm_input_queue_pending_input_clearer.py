#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from queue import Empty

from input_core.input_event.provenance.trusted_user_ingress import (
    QueueOwnedIngressDelivery,
)


class LlmInputQueuePendingInputClearer:
    def __init__(self, worker) -> None:
        self._worker = worker

    def clear(self) -> None:
        with self._worker.input_queue_lock:
            while True:
                try:
                    pending_input = self._worker.input_queue.get_nowait()
                    if type(pending_input) is QueueOwnedIngressDelivery:
                        pending_input.abandon()
                except Empty:
                    break
                except Exception:
                    break
            self._worker.queue_updated_callback()


__all__ = ("LlmInputQueuePendingInputClearer",)
