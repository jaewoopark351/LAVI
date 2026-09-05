#20260905_kpopmodder: Isolate Fabric ChatClef handshake diagnostics.
from __future__ import annotations


class FabricChatClefHandshakeDiagnostics:
    def __init__(self, diagnostics):
        self._diagnostics = diagnostics

    def rejected(self, *, session_id: str, reason: str) -> None:
        self._diagnostics.warning(
            "rejected duplicate client handshake "
            f"session={session_id} reason={reason}"
        )

    def accepted(self, *, session_id: str, generation: int) -> None:
        self._diagnostics.info(
            "client connected "
            f"session={session_id} generation={generation}"
        )


__all__ = ("FabricChatClefHandshakeDiagnostics",)
