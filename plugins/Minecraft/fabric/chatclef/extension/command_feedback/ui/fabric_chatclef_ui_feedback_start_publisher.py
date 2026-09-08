#20260907_kpopmodder: Resolve one START publication permit outside command ownership locks.
from __future__ import annotations


class FabricChatClefUiFeedbackStartPublisher:
    TURN_TIMEOUT_SECONDS = 2.0

    def __init__(
        self,
        *,
        response_factory,
        coalesced_response_factory,
        listener,
    ) -> None:
        self._response_factory = response_factory
        self._coalesced_response_factory = coalesced_response_factory
        self._listener = listener

    def publish(self, descriptor: object, acknowledgement: object) -> bool:
        if acknowledgement is True:
            return False
        waiter = getattr(acknowledgement, "wait_until_ready", None)
        resolver = getattr(acknowledgement, "acknowledge", None)
        if not callable(waiter) or not callable(resolver):
            return False
        try:
            ready = waiter(timeout_seconds=self.TURN_TIMEOUT_SECONDS) is True
        except Exception:
            ready = False
        if not ready:
            self._acknowledge(resolver, published=False)
            return False
        try:
            selector = getattr(
                acknowledgement,
                "select_coalesced_terminal",
                None,
            )
            terminal = selector() if callable(selector) else None
            response = (
                self._response_factory.build(descriptor)
                if terminal is None
                else self._coalesced_response_factory.build(terminal)
            )
            outcome = self._listener.publish(response)
            published = bool(
                outcome is True
                or getattr(outcome, "output_delivered", False) is True
            )
        except Exception:
            published = False
        accepted = self._acknowledge(resolver, published=published)
        return published and accepted

    @staticmethod
    def _acknowledge(callback, *, published: bool) -> bool:
        try:
            return callback(published=published) is True
        except Exception:
            return False


__all__ = ("FabricChatClefUiFeedbackStartPublisher",)
