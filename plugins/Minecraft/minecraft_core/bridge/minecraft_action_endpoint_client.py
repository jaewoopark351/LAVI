#20260725_kpopmodder: Added write/action endpoint methods for ChatClef bridge APIs.
from __future__ import annotations

from typing import Any, Dict

from .chatclef_http_transport import ChatClefHttpTransport
from .minecraft_action_request_builder import MinecraftActionRequestBuilder


class MinecraftActionEndpointClient:
    def __init__(
        self,
        transport: ChatClefHttpTransport,
        request_builder: MinecraftActionRequestBuilder | None = None,
    ):
        self.transport = transport
        self.request_builder = request_builder or MinecraftActionRequestBuilder()

    def get_item(self, item: str, count: int = 1) -> Dict[str, Any]:
        return self.transport.request_json(
            "POST",
            "/v1/actions/get-item",
            self.request_builder.get_item(item, count),
        )

    def equip(self, item: str) -> Dict[str, Any]:
        return self.transport.request_json(
            "POST",
            "/v1/actions/equip",
            self.request_builder.equip(item),
        )

    def goto(
        self,
        target: Any = None,
        *,
        x: Any = None,
        y: Any = None,
        z: Any = None,
        dimension: Any = None,
    ) -> Dict[str, Any]:
        return self.transport.request_json(
            "POST",
            "/v1/actions/goto",
            self.request_builder.goto(
                target,
                x=x,
                y=y,
                z=z,
                dimension=dimension,
            ),
        )

    def stop(self) -> Dict[str, Any]:
        return self.transport.request_json("POST", "/v1/actions/stop", {})
