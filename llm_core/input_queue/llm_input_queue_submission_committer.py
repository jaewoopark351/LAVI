#20260905_kpopmodder: Commit one plain input and publish its queue update.
from __future__ import annotations


class LlmInputQueueSubmissionCommitter:
    def __init__(self, worker):
        self._worker = worker

    def commit(self, value) -> None:
        with self._worker.input_queue_lock:
            self._worker.input_queue.put(value)
            self._worker.queue_updated_callback()


__all__ = ("LlmInputQueueSubmissionCommitter",)
