#20260905_kpopmodder: Hold the optional routed-output callback for verified STOP terminals.
from __future__ import annotations

import threading
from typing import Callable

from plugins.Minecraft.fabric.chatclef.response.stop import (
    StopControlTerminalResponse,
)


class StopControlTerminalListener:
    def __init__(self):
        self._lock = threading.RLock()
        self._callback: Callable[[StopControlTerminalResponse], object] | None = None

    def set_callback(
        self,
        callback: Callable[[StopControlTerminalResponse], object] | None,
    ) -> None:
        if callback is not None and not callable(callback):
            raise TypeError("STOP terminal callback must be callable or None")
        with self._lock:
            self._callback = callback

    def publish(self, response: StopControlTerminalResponse) -> None:
        if type(response) is not StopControlTerminalResponse:
            raise TypeError("STOP terminal response must be exact")
        with self._lock:
            callback = self._callback
        if callback is not None:
            callback(response)


__all__ = ("StopControlTerminalListener",)
