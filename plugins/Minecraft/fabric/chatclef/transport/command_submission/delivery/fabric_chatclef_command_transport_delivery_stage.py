#20260905_kpopmodder: Deliver one admitted ordinary-command envelope over its bound websocket.
from __future__ import annotations


class FabricChatClefCommandTransportDeliveryStage:
    def __init__(self, *, envelope_transport, delivery) -> None:
        self._envelope_transport = envelope_transport
        self._delivery = delivery

    def deliver(self, *, command_context, envelope, loop):
        return self._delivery.deliver(
            self._envelope_transport.send(command_context.websocket, envelope),
            loop,
        )
