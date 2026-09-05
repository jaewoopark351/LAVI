#20260905_kpopmodder: Own the current LLM input worker thread reference.
from __future__ import annotations


class LlmInputQueueWorkerThreadState:
    def __init__(self):
        self.thread = None


__all__ = ("LlmInputQueueWorkerThreadState",)
