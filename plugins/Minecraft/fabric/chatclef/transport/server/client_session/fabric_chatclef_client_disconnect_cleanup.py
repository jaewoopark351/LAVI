#20260905_kpopmodder: Release only the exact disconnected Fabric ChatClef client session.
from __future__ import annotations

import json
from typing import Any


class FabricChatClefClientDisconnectCleanup:
    def __init__(
        self,
        *,
        connection_ownership,
        command_lock,
        session_registry,
        diagnostics,
    ) -> None:
        self._connection_ownership = connection_ownership
        self._command_lock = command_lock
        self._session_registry = session_registry
        self._diagnostics = diagnostics

    def release(
        self,
        *,
        websocket: Any,
        session_id: str,
        connection_generation: int,
    ) -> None:
        with self._command_lock:
            before_status = self._connection_ownership.snapshot()
            cleared = self._connection_ownership.clear_if_active(
                websocket=websocket,
                session_id=session_id,
            )
            after_status = self._connection_ownership.snapshot()
        if not cleared:
            return
        self._session_registry.remove(session_id)
        self._diagnostics.info(
            "client disconnected "
            f"session={session_id} "
            f"generation={connection_generation} "
            f"before={self._compact_json(before_status)} "
            f"after={self._compact_json(after_status)}"
        )

    @staticmethod
    def _compact_json(payload: Any) -> str:
        try:
            return json.dumps(
                payload,
                ensure_ascii=False,
                sort_keys=True,
                separators=(",", ":"),
            )
        except Exception as error:
            return f"<json failed {type(error).__name__}: {error}>"
