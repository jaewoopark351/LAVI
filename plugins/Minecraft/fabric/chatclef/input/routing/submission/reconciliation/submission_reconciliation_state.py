#20260819_kpopmodder: Store one unresolved submission request identity without policy decisions.
from __future__ import annotations


class MinecraftChatClefSubmissionReconciliationState:
    def __init__(self):
        self._pending_request_id: str | None = None

    @property
    def pending_request_id(self) -> str | None:
        return self._pending_request_id

    def retain(self, request_id: str) -> None:
        self._pending_request_id = request_id

    def clear(self) -> None:
        self._pending_request_id = None
