#20260819_kpopmodder: Serialize route submission and reconciliation ownership changes.
from __future__ import annotations

import threading
from types import TracebackType


class MinecraftChatClefSubmissionRouteLock:
    def __init__(self):
        self._lock = threading.RLock()

    def __enter__(self) -> "MinecraftChatClefSubmissionRouteLock":
        self._lock.acquire()
        return self

    def __exit__(
        self,
        _exception_type: type[BaseException] | None,
        _exception: BaseException | None,
        _traceback: TracebackType | None,
    ) -> None:
        self._lock.release()
