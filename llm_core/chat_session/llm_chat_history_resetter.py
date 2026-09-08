#20260905_kpopmodder: Owns explicit clearing of the active LLM chat history.
from __future__ import annotations


class LlmChatHistoryResetter:
    def __init__(
        self,
        history_callback,
        clear_pending_presentations_callback=None,
    ) -> None:
        if not callable(history_callback):
            raise TypeError("history_callback must be callable")
        if (
            clear_pending_presentations_callback is not None
            and not callable(clear_pending_presentations_callback)
        ):
            raise TypeError(
                "clear_pending_presentations_callback must be callable"
            )
        self._history_callback = history_callback
        self._clear_pending_presentations_callback = (
            clear_pending_presentations_callback
        )

    def reset(self) -> None:
        if self._clear_pending_presentations_callback is not None:
            self._clear_pending_presentations_callback()
        self._history_callback().clear()


__all__ = ("LlmChatHistoryResetter",)
