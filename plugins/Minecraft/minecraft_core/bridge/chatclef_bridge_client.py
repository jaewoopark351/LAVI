#20260725_kpopmodder: Added a dependency-free HTTP client for the local ChatClef bridge.
from __future__ import annotations

from typing import Any, Dict

from .chatclef_http_transport import ChatClefHttpTransport
from .minecraft_action_endpoint_client import MinecraftActionEndpointClient
from .minecraft_read_endpoint_client import MinecraftReadEndpointClient


class ChatClefBridgeClient:
    def __init__(
        self,
        base_url: str = "http://127.0.0.1:4316",
        timeout_sec: float = 3.0,
        opener=None,
    ):
        self.base_url = str(base_url or "http://127.0.0.1:4316").rstrip("/")
        self.timeout_sec = float(timeout_sec or 3.0)
        self.transport = ChatClefHttpTransport(
            base_url=self.base_url,
            timeout_sec=self.timeout_sec,
            opener=opener,
        )
        self.read_endpoints = MinecraftReadEndpointClient(self.transport)
        self.action_endpoints = MinecraftActionEndpointClient(self.transport)

    def health(self) -> Dict[str, Any]:
        return self.read_endpoints.health()

    def status(self) -> Dict[str, Any]:
        return self.read_endpoints.status()

    def inventory(self) -> Dict[str, Any]:
        return self.read_endpoints.inventory()

    def current_action(self) -> Dict[str, Any]:
        return self.read_endpoints.current_action()

    def get_item(self, item: str, count: int = 1) -> Dict[str, Any]:
        return self.action_endpoints.get_item(item, count)

    def get_and_equip(self, item: str, count: int = 1) -> Dict[str, Any]:
        return self.action_endpoints.get_and_equip(item, count)

    def craft(self, item: str, count: int = 1) -> Dict[str, Any]:
        return self.action_endpoints.craft(item, count)

    def equip(self, item: str) -> Dict[str, Any]:
        return self.action_endpoints.equip(item)

    def goto(
        self,
        target: Any = None,
        *,
        x: Any = None,
        y: Any = None,
        z: Any = None,
        dimension: Any = None,
    ) -> Dict[str, Any]:
        return self.action_endpoints.goto(
            target,
            x=x,
            y=y,
            z=z,
            dimension=dimension,
        )

    def stop(self) -> Dict[str, Any]:
        return self.action_endpoints.stop()

    def request_json(
        self,
        method: str,
        path: str,
        payload: Dict[str, Any] | None = None,
    ) -> Dict[str, Any]:
        return self.transport.request_json(method, path, payload)
