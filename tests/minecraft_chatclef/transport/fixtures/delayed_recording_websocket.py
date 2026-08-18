#20260819_kpopmodder: Delay one websocket send and record only released wire data.
from __future__ import annotations

import asyncio
import threading


class DelayedRecordingWebSocket:
    def __init__(self):
        self._lock = threading.Lock()
        self._send_started = threading.Event()
        self._send_completed = threading.Event()
        self._release_future: asyncio.Future[None] | None = None
        self._release_loop: asyncio.AbstractEventLoop | None = None
        self._wire_messages: list[str] = []

    @property
    def wire_messages(self) -> list[str]:
        with self._lock:
            return list(self._wire_messages)

    @property
    def send_started(self) -> bool:
        return self._send_started.is_set()

    @property
    def send_completed(self) -> bool:
        return self._send_completed.is_set()

    async def send(self, payload: str) -> None:
        loop = asyncio.get_running_loop()
        release_future = loop.create_future()
        with self._lock:
            self._release_loop = loop
            self._release_future = release_future
        self._send_started.set()

        await release_future

        with self._lock:
            self._wire_messages.append(payload)
        self._send_completed.set()

    def wait_until_started(self, *, timeout: float = 2.0) -> None:
        if not self._send_started.wait(timeout=timeout):
            raise AssertionError("delayed websocket send did not start")

    def release(self) -> None:
        self.wait_until_started()
        with self._lock:
            loop = self._release_loop
            release_future = self._release_future
        if loop is None or release_future is None:
            raise AssertionError("delayed websocket has no release future")
        loop.call_soon_threadsafe(_release_if_pending, release_future)


def _release_if_pending(release_future: asyncio.Future[None]) -> None:
    if not release_future.done():
        release_future.set_result(None)
