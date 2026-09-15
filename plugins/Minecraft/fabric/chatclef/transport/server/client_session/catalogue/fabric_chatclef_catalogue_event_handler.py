#20260915_kpopmodder: Validate catalogue event custody before replacing session metadata.
from __future__ import annotations

from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType


class FabricChatClefCatalogueEventHandler:
    def __init__(self, *, connection_ownership, command_lock, session_registry, diagnostics):
        self._ownership = connection_ownership
        self._lock = command_lock
        self._sessions = session_registry
        self._diagnostics = diagnostics

    def handle(self, websocket, envelope) -> bool:
        if (envelope.message_type != BridgeMessageType.EVENT
                or envelope.payload.get("event_type") != "korean_command_catalogue_v1"):
            return False
        with self._lock:
            active = (self._ownership.is_active_websocket(websocket)
                      and envelope.session_id == self._ownership.active_session_id)
            generation = self._ownership.active_generation
            if active:
                summary = self._sessions.update_command_catalogue(
                    envelope.session_id, envelope.payload.get("korean_command_catalogue_v1"))
            else:
                summary = {"available": False, "reason": "catalogue_foreign_session"}
        # Snapshot only the values decided above; logging failure cannot select admission.
        try:
            self._diagnostics.info(
                "command_catalogue boundary=refresh "
                f"generation={generation} active={active} "
                f"available={summary.get('available', False)} "
                f"entries={summary.get('entry_count', 0)} "
                f"reason={summary.get('reason', 'validated')}")
        except Exception:
            pass
        return True
