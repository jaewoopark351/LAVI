#20260905_kpopmodder: Own the post-acceptance queue display update.
from __future__ import annotations


class LlmInputQueueAcceptanceDisplayEffect:
    def __init__(self, *, worker, diagnostics):
        if not callable(getattr(worker, "queue_updated_callback", None)):
            raise TypeError("worker.queue_updated_callback must be callable")
        self._worker = worker
        self._diagnostics = diagnostics

    def run(self) -> None:
        try:
            self._worker.queue_updated_callback()
        except Exception:
            self._diagnostics.log_failure("queue_display_update")


__all__ = ("LlmInputQueueAcceptanceDisplayEffect",)
