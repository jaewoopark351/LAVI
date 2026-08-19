#20260819_kpopmodder: Read matching terminal evidence from trusted Fabric bridge status.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.input.routing.bridge_status_selector import (
    MinecraftChatClefBridgeStatusSelector,
)


class MinecraftChatClefBridgeReconciliationObserver:
    EXPECTED_BACKEND = "fabric_chatclef"

    def __init__(
        self,
        bridge_status_selector: MinecraftChatClefBridgeStatusSelector | None = None,
    ):
        self._bridge_status_selector = (
            bridge_status_selector or MinecraftChatClefBridgeStatusSelector()
        )

    def observe(
        self,
        extension: Any,
        expected_request_id: str,
    ) -> dict[str, Any] | None:
        status_method = getattr(extension, "get_status", None)
        if not callable(status_method):
            return None
        try:
            raw_status = status_method()
        except Exception:
            return None
        if not isinstance(raw_status, Mapping):
            return None
        try:
            bridge, _selection_error = self._bridge_status_selector.select(raw_status)
        except Exception:
            return None
        if bridge is None:
            return None
        if bridge.get("backend_id") != self.EXPECTED_BACKEND:
            return None
        if bridge.get("enabled") is not True:
            return None
        if bridge.get("connected") is not True:
            return None
        if bridge.get("lifecycle_state") != "connected":
            return None
        details = bridge.get("details")
        if not isinstance(details, Mapping):
            return None
        commands = details.get("commands")
        if not isinstance(commands, Mapping):
            return None
        if "active_request_id" not in commands:
            return None
        if commands.get("active_request_id") is not None:
            return None
        last_result = commands.get("last_result")
        if not isinstance(last_result, Mapping):
            return None
        request_id = last_result.get("request_id")
        if (
            type(request_id) is not str
            or request_id != expected_request_id
            or request_id != request_id.strip()
        ):
            return None
        try:
            return dict(last_result)
        except Exception:
            return None
