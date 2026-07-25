#20260725_kpopmodder: Added read-only endpoint methods for ChatClef bridge status APIs.
from __future__ import annotations

from typing import Any, Dict

from .chatclef_http_transport import ChatClefHttpTransport


class MinecraftReadEndpointClient:
    def __init__(self, transport: ChatClefHttpTransport):
        self.transport = transport

    def health(self) -> Dict[str, Any]:
        return self.transport.request_json("GET", "/v1/health")

    def status(self) -> Dict[str, Any]:
        return self.transport.request_json("GET", "/v1/status")

    def inventory(self) -> Dict[str, Any]:
        return self.transport.request_json("GET", "/v1/inventory")

    def current_action(self) -> Dict[str, Any]:
        return self.transport.request_json("GET", "/v1/actions/current")
