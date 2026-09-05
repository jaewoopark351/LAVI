#20260905_kpopmodder: Own the mutable LLM input queue and its lock.
from __future__ import annotations

import threading
from queue import Queue


class LlmInputQueueStore:
    def __init__(self):
        self.queue = Queue()
        self.lock = threading.Lock()


__all__ = ("LlmInputQueueStore",)
