#20260907_kpopmodder: Hold the optional output callback for crafting terminal responses.
from __future__ import annotations

import threading
from typing import Callable

from plugins.Minecraft.fabric.chatclef.response.crafting_lifecycle import (
    CraftingLifecycleTerminalResponse,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleTerminalResponse,
)


class CraftingFeedbackTerminalListener:
    def __init__(self) -> None:
        self._lock = threading.RLock()
        self._callback: Callable[[object], object] | None = (
            None
        )

    def set_callback(
        self,
        callback: Callable[[object], object] | None,
    ) -> None:
        if callback is not None and not callable(callback):
            raise TypeError("crafting terminal callback must be callable or None")
        with self._lock:
            self._callback = callback

    def publish(self, response: object) -> None:
        if type(response) not in {
            CraftingLifecycleTerminalResponse,
            CommandLifecycleTerminalResponse,
        }:
            raise TypeError("command terminal response must be exact")
        with self._lock:
            callback = self._callback
        if callback is not None:
            callback(response)


__all__ = ("CraftingFeedbackTerminalListener",)
