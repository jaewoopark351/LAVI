#20260905_kpopmodder: Own the post-acceptance queue worker start request.
from __future__ import annotations


class LlmInputQueueAcceptanceWorkerStartEffect:
    def __init__(self, *, worker, diagnostics):
        if not callable(getattr(worker, "process_input_queue", None)):
            raise TypeError("worker.process_input_queue must be callable")
        self._worker = worker
        self._diagnostics = diagnostics

    def run(self) -> None:
        try:
            self._worker.process_input_queue()
        except Exception:
            self._diagnostics.log_failure("worker_start")


__all__ = ("LlmInputQueueAcceptanceWorkerStartEffect",)
