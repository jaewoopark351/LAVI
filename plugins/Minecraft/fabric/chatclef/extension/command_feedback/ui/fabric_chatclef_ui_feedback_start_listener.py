#20260907_kpopmodder: Own only the optional direct-GUI START presentation callback.
from __future__ import annotations

import threading

from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleCoalescedResponse,
    CommandLifecycleStartResponse,
)


class FabricChatClefUiFeedbackStartListener:
    def __init__(self) -> None:
        self._callback = None
        self._lock = threading.Lock()

    def set_callback(self, callback) -> None:
        if callback is not None and not callable(callback):
            raise TypeError("command lifecycle START callback must be callable or None")
        with self._lock:
            self._callback = callback

    def publish(self, response: object):
        if type(response) not in {
            CommandLifecycleCoalescedResponse,
            CommandLifecycleStartResponse,
        }:
            return None
        with self._lock:
            callback = self._callback
        if not callable(callback):
            return None
        try:
            return callback(response)
        except Exception:
            return None


__all__ = ("FabricChatClefUiFeedbackStartListener",)
