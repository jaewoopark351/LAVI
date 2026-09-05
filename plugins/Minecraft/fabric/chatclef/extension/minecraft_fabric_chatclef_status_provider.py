#20260905_kpopmodder: Isolate Fabric ChatClef extension status projection.
from __future__ import annotations


class MinecraftFabricChatClefStatusProvider:
    def __init__(
        self,
        *,
        extension_name: str,
        plugin,
        adapter,
        apply_status_contract,
    ):
        self._extension_name = extension_name
        self._plugin = plugin
        self._adapter = adapter
        self._apply_status_contract = apply_status_contract

    def get_status(self) -> dict[str, object]:
        status = self._adapter.get_status().to_dict()
        return self._apply_status_contract(
            {
                "name": self._extension_name,
                "plugin": self.plugin_status(),
                "runtime": {"backend_id": self._adapter.backend_id},
                "details": status,
                "error": status.get("last_error_message"),
            }
        )

    def plugin_status(self) -> dict[str, object]:
        status = getattr(self._plugin, "get_status", None)
        if callable(status):
            return dict(status())
        return {"present": self._plugin is not None}


__all__ = ("MinecraftFabricChatClefStatusProvider",)
