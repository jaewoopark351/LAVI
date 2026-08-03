#20260803_kpopmodder: Added status presentation outside the Fabric ChatClef panel.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.ui.fabric_chatclef_json_formatter import (
    FabricChatClefJsonFormatter,
)


class FabricChatClefStatusPresenter:
    def __init__(
        self,
        plugin: Any,
        extension: Any = None,
        formatter: FabricChatClefJsonFormatter | None = None,
    ):
        self.plugin = plugin
        self.extension = extension
        self.formatter = formatter or FabricChatClefJsonFormatter()

    def status_payload(self) -> dict[str, Any]:
        provider = None
        if self.extension is not None:
            provider = getattr(self.extension, "get_status", None)
        if not callable(provider):
            provider = getattr(self.plugin, "get_status", None)
        if not callable(provider):
            return {
                "error": "status_provider_missing",
                "details": "Fabric ChatClef status provider is unavailable.",
            }
        try:
            return self.formatter.mapping_payload(provider())
        except Exception as error:
            return {
                "error": "status_provider_failed",
                "details": f"{type(error).__name__}: {error}",
            }

    def status_values(self, status: Mapping[str, Any]) -> tuple[str, str, str, str]:
        bridge = self.bridge_status(status)
        endpoint = self.endpoint_text(status, bridge)
        lifecycle = str(bridge.get("lifecycle_state") or "unknown")
        connected = "true" if bool(bridge.get("connected")) else "false"
        return endpoint, lifecycle, connected, self.formatter.to_json(status)

    def bridge_status(self, status: Mapping[str, Any]) -> dict[str, Any]:
        payload = self.formatter.mapping_payload(status)
        details = self.formatter.mapping_payload(payload.get("details"))
        if "lifecycle_state" in details:
            return details
        bridge = self.formatter.mapping_payload(payload.get("bridge"))
        if "lifecycle_state" in bridge:
            return bridge
        plugin = self.formatter.mapping_payload(payload.get("plugin"))
        plugin_bridge = self.formatter.mapping_payload(plugin.get("bridge"))
        if "lifecycle_state" in plugin_bridge:
            return plugin_bridge
        return payload

    def endpoint_text(
        self,
        status: Mapping[str, Any],
        bridge: Mapping[str, Any],
    ) -> str:
        bridge_details = self.formatter.mapping_payload(bridge.get("details"))
        endpoint = bridge_details.get("endpoint") or status.get("endpoint")
        if endpoint:
            return str(endpoint)
        config = getattr(self.plugin, "config", None)
        return str(getattr(config, "endpoint", ""))
