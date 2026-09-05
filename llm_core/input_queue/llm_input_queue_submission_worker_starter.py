#20260905_kpopmodder: Start queue processing after a plain-input commit.
from __future__ import annotations


class LlmInputQueueSubmissionWorkerStarter:
    def __init__(self, worker):
        self._worker = worker

    def start(self) -> None:
        self._worker.process_input_queue()


__all__ = ("LlmInputQueueSubmissionWorkerStarter",)
