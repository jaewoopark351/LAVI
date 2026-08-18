#20260819_kpopmodder: Track futures scheduled through the real thread-safe API.
from __future__ import annotations

import asyncio
from concurrent.futures import Future
from typing import Any, Coroutine


class ScheduledFutureTracker:
    def __init__(self):
        self._futures: list[Future[Any]] = []

    @property
    def count(self) -> int:
        return len(self._futures)

    @property
    def has_pending(self) -> bool:
        return any(not future.done() for future in self._futures)

    def schedule(
        self,
        coroutine: Coroutine[Any, Any, Any],
        loop: asyncio.AbstractEventLoop,
    ) -> Future[Any]:
        future = asyncio.run_coroutine_threadsafe(coroutine, loop)
        self._futures.append(future)
        return future

    def wait_for_all(self, *, timeout: float = 2.0) -> None:
        for future in self._futures:
            future.result(timeout=timeout)
