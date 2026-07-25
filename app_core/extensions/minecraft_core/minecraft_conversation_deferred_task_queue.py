#20260725_kpopmodder: Added deferred task queue so Minecraft work starts after the immediate chat reply is emitted.
from __future__ import annotations

import threading
from typing import Callable

from core.logger import log_print


class MinecraftConversationDeferredTaskQueue:
    def __init__(self):
        self._tasks: list[Callable[[], object]] = []
        self._lock = threading.Lock()

    def add(self, task: Callable[[], object]) -> None:
        if not callable(task):
            return
        with self._lock:
            self._tasks.append(task)

    def run_all(self) -> None:
        tasks = self._pop_all()
        for task in tasks:
            try:
                task()
            except Exception as error:
                log_print(f"[MinecraftConversation] deferred task failed: {error}")

    def _pop_all(self) -> list[Callable[[], object]]:
        with self._lock:
            tasks = list(self._tasks)
            self._tasks.clear()
        return tasks
