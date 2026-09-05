#20260905_kpopmodder: Own one exact STOP transport delivery attempt.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.transport.command_submission.fabric_chatclef_command_delivery import (
    FabricChatClefCommandDelivery,
)


class StopControlTransportDeliveryStage:
    def __init__(
        self,
        *,
        envelope_transport: object,
        future_scheduler: object,
        send_timeout_sec: float,
    ):
        self._envelope_transport = envelope_transport
        self._delivery = FabricChatClefCommandDelivery(
            future_scheduler=future_scheduler,
            send_timeout_sec=send_timeout_sec,
        )

    def deliver(
        self,
        *,
        tracker: object,
        envelope: object,
        loop: object,
    ) -> str:
        outcome = self._delivery.deliver(
            self._envelope_transport.send(
                tracker.transport_websocket,
                envelope,
            ),
            loop,
        )
        return outcome.status


__all__ = ("StopControlTransportDeliveryStage",)
