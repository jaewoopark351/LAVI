#20260905_kpopmodder: Own facade current system-prompt state synchronization.
from __future__ import annotations


class LlmSystemPromptCurrentStateSynchronizer:
    def __init__(self, update_current_callback):
        if not callable(update_current_callback):
            raise TypeError("update_current_callback must be callable")
        self._update_current_callback = update_current_callback

    def synchronize(self, content) -> None:
        self._update_current_callback(content)


__all__ = ("LlmSystemPromptCurrentStateSynchronizer",)
