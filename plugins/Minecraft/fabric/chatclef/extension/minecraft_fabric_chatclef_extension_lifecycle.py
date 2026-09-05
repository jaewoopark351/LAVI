#20260905_kpopmodder: Isolate Fabric ChatClef extension start/stop lifecycle.
from __future__ import annotations


class MinecraftFabricChatClefExtensionLifecycle:
    def __init__(self, *, adapter, mark_started, publish_event):
        self._adapter = adapter
        self._mark_started = mark_started
        self._publish_event = publish_event

    def start(self) -> None:
        self._adapter.start()
        status = self._adapter.get_status()
        self._mark_started(status.enabled and not bool(status.last_error_message))
        self._publish_event(
            "minecraft_fabric_chatclef_started",
            {"status": status.to_dict()},
        )

    def stop(self) -> None:
        self._adapter.stop()
        self._mark_started(False)
        self._publish_event("minecraft_fabric_chatclef_stopped", {})


__all__ = ("MinecraftFabricChatClefExtensionLifecycle",)
