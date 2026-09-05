#20260905_kpopmodder: Own legacy prediction fallback after registration refusal.
from __future__ import annotations


class TrustedLocalChatFallbackDispatcher:
    def __init__(self, predict_callback):
        if not callable(predict_callback):
            raise TypeError("fallback_predict_callback must be callable")
        self._predict_callback = predict_callback

    def dispatch(self, registrar, history, system_prompt):
        event = registrar.created_event
        if event is None:
            return
        yield from self._predict_callback(
            event,
            history,
            system_prompt,
        )


__all__ = ("TrustedLocalChatFallbackDispatcher",)
