#20260907_kpopmodder: Deliver projected terminal responses through the existing publisher.
from __future__ import annotations

from typing import Any


class FabricChatClefCommandResultTerminalDelivery:
    def __init__(self, terminal_delivery: Any) -> None:
        self._terminal_delivery = terminal_delivery

    def publish(self, terminal_response: Any) -> None:
        if terminal_response is None or self._terminal_delivery is None:
            return
        self._terminal_delivery.publish(terminal_response)
