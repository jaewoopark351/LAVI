#20260905_kpopmodder: Keeps the legacy queue-runtime path as a focused facade.
from __future__ import annotations

from llm_core.input_queue import (
    LlmInputQueueDrainer,
    LlmInputQueueThreadStarter,
)


class LLMInputQueueRuntime:
    def __init__(self, worker):
        self.worker = worker
        self._thread_starter = LlmInputQueueThreadStarter(worker)
        self._drainer = LlmInputQueueDrainer(worker)

    def start_once(self, target):
        return self._thread_starter.start_once(target)

    def drain_queue(self):
        return self._drainer.drain()


__all__ = ("LLMInputQueueRuntime",)
