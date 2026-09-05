#20260905_kpopmodder: Classify one ordinary-command coroutine schedule without retry.
from __future__ import annotations

from typing import Any, Callable

from .fabric_chatclef_command_delivery_outcome import (
    FabricChatClefCommandDeliveryOutcome,
)


class FabricChatClefCommandDelivery:
    def __init__(
        self,
        *,
        future_scheduler: Callable[[Any, Any], Any],
        send_timeout_sec: float,
    ) -> None:
        self._future_scheduler = future_scheduler
        self._send_timeout_sec = send_timeout_sec

    def deliver(self, coroutine: Any, loop: Any) -> FabricChatClefCommandDeliveryOutcome:
        try:
            future = self._future_scheduler(coroutine, loop)
        except Exception as error:
            close = getattr(coroutine, "close", None)
            if callable(close):
                close()
            return FabricChatClefCommandDeliveryOutcome(
                status="not_scheduled",
                error=error,
            )
        try:
            future.result(timeout=self._send_timeout_sec)
        except Exception as error:
            return FabricChatClefCommandDeliveryOutcome(
                status="outcome_unknown",
                error=error,
            )
        return FabricChatClefCommandDeliveryOutcome(status="sent")
