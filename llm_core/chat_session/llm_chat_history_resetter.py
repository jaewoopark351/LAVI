#20260905_kpopmodder: Owns explicit clearing of the active LLM chat history.
from __future__ import annotations


class LlmChatHistoryResetter:
    def __init__(self, history_callback) -> None:
        if not callable(history_callback):
            raise TypeError("history_callback must be callable")
        self._history_callback = history_callback

    def reset(self) -> None:
        self._history_callback().clear()


__all__ = ("LlmChatHistoryResetter",)
