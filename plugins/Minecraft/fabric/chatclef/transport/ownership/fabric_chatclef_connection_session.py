#20260905_kpopmodder: Own only the active Fabric ChatClef websocket session identity.
from __future__ import annotations

from typing import Any

from ..fabric_chatclef_connection_admission import FabricChatClefConnectionAdmission


class FabricChatClefConnectionSession:
    def __init__(self) -> None:
        self._generation = 0
        self._active_websocket: Any = None
        self._active_session_id: str | None = None

    @property
    def active_websocket(self) -> Any:
        return self._active_websocket

    @property
    def active_session_id(self) -> str | None:
        return self._active_session_id

    @property
    def active_generation(self) -> int:
        return self._generation if self._active_websocket is not None else 0

    def is_connected(self) -> bool:
        return self._active_websocket is not None and self._active_session_id is not None

    def is_active_websocket(self, websocket: Any) -> bool:
        return self._active_websocket is websocket

    def try_activate(
        self,
        *,
        websocket: Any,
        session_id: str,
    ) -> FabricChatClefConnectionAdmission:
        if self._active_websocket is not None:
            if (
                self._active_websocket is websocket
                and self._active_session_id == session_id
            ):
                return FabricChatClefConnectionAdmission(
                    accepted=True,
                    session_id=session_id,
                    generation=self._generation,
                )
            return FabricChatClefConnectionAdmission(
                accepted=False,
                session_id=session_id,
                generation=self.active_generation,
                reason=(
                    "Fabric ChatClef bridge already has an active Java websocket "
                    f"session: {self._active_session_id}"
                ),
            )

        self._generation += 1
        self._active_websocket = websocket
        self._active_session_id = session_id
        return FabricChatClefConnectionAdmission(
            accepted=True,
            session_id=session_id,
            generation=self._generation,
        )

    def clear_if_active(self, *, websocket: Any, session_id: str | None) -> bool:
        if self._active_websocket is not websocket:
            return False
        if self._active_session_id != session_id:
            return False
        self.clear()
        return True

    def clear(self) -> None:
        self._active_websocket = None
        self._active_session_id = None
