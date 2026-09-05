#20260905_kpopmodder: Dispatch and always abandon one registered local-Chat delivery.
from __future__ import annotations


class TrustedLocalChatRegisteredDeliveryDispatcher:
    def __init__(self, *, dispatch_callback, abandoner):
        if not callable(dispatch_callback):
            raise TypeError("registered_dispatch_callback must be callable")
        self._dispatch_callback = dispatch_callback
        self._abandoner = abandoner

    def dispatch(self, delivery, history, system_prompt):
        try:
            yield from self._dispatch_callback(
                delivery,
                history,
                system_prompt,
            )
        finally:
            self._abandoner.abandon(delivery)


__all__ = ("TrustedLocalChatRegisteredDeliveryDispatcher",)
