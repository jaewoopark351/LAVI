#20260819_kpopmodder: Own a real asyncio event loop's test-thread lifecycle.
from __future__ import annotations

import asyncio
import threading


class AsyncioEventLoopThread:
    def __init__(self):
        self._loop = asyncio.new_event_loop()
        self._ready = threading.Event()
        self._thread = threading.Thread(
            target=self._run,
            name="chatclef-test-event-loop",
            daemon=True,
        )
        self._started = False

    @property
    def loop(self) -> asyncio.AbstractEventLoop:
        return self._loop

    def start(self) -> None:
        if self._started:
            return
        self._thread.start()
        if not self._ready.wait(timeout=2.0):
            raise AssertionError("test event loop did not start")
        self._started = True

    def pending_task_count(self) -> int:
        future = asyncio.run_coroutine_threadsafe(
            _count_other_pending_tasks(),
            self._loop,
        )
        return future.result(timeout=2.0)

    def close(self) -> None:
        if not self._started:
            self._loop.close()
            return
        self._loop.call_soon_threadsafe(self._loop.stop)
        self._thread.join(timeout=2.0)
        if self._thread.is_alive():
            raise AssertionError("test event loop did not stop")
        self._loop.close()
        self._started = False

    def _run(self) -> None:
        asyncio.set_event_loop(self._loop)
        self._loop.call_soon(self._ready.set)
        self._loop.run_forever()


async def _count_other_pending_tasks() -> int:
    current = asyncio.current_task()
    return sum(
        1
        for task in asyncio.all_tasks()
        if task is not current and not task.done()
    )
