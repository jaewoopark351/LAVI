#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from .terminal_response import (
    StopControlTerminalDeliveryAuthorizer,
    StopControlTerminalResponseDelivery,
    StopControlTerminalResponseFactory,
)


class StopControlTerminalResponsePublisher:
    def __init__(
        self,
        *,
        terminal_listener: object,
        renderer: object,
        diagnostic_reporter: object,
    ):
        self._authorizer = StopControlTerminalDeliveryAuthorizer()
        self._response_factory = StopControlTerminalResponseFactory(renderer)
        self._delivery = StopControlTerminalResponseDelivery(
            terminal_listener=terminal_listener,
            diagnostic_reporter=diagnostic_reporter,
        )

    def publish(self, *, tracker: object, decision: object | None) -> None:
        if not self._authorizer.claim(tracker):
            return
        response = self._response_factory.create(
            tracker=tracker,
            decision=decision,
        )
        self._delivery.deliver(response)


__all__ = ("StopControlTerminalResponsePublisher",)
