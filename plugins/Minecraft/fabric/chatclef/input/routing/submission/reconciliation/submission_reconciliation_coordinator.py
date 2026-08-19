#20260819_kpopmodder: Coordinate UNKNOWN retention with trusted terminal reconciliation.
from __future__ import annotations

from typing import Any, Mapping

from ..submission_result_normalizer import (
    MinecraftChatClefSubmissionResultNormalizer,
)
from .bridge_reconciliation_observer import (
    MinecraftChatClefBridgeReconciliationObserver,
)
from .bridge_result_adapter import MinecraftChatClefBridgeResultAdapter
from .submission_reconciliation_policy import (
    MinecraftChatClefSubmissionReconciliationPolicy,
)
from .submission_reconciliation_state import (
    MinecraftChatClefSubmissionReconciliationState,
)
from .submission_route_lock import MinecraftChatClefSubmissionRouteLock


class MinecraftChatClefSubmissionReconciliationCoordinator:
    def __init__(
        self,
        extension: Any,
        *,
        state: MinecraftChatClefSubmissionReconciliationState | None = None,
        policy: MinecraftChatClefSubmissionReconciliationPolicy | None = None,
        observer: MinecraftChatClefBridgeReconciliationObserver | None = None,
        result_adapter: MinecraftChatClefBridgeResultAdapter | None = None,
        result_normalizer: MinecraftChatClefSubmissionResultNormalizer | None = None,
        route_lock: MinecraftChatClefSubmissionRouteLock | None = None,
    ):
        self._extension = extension
        self._state = state or MinecraftChatClefSubmissionReconciliationState()
        self._policy = policy or MinecraftChatClefSubmissionReconciliationPolicy()
        self._observer = observer or MinecraftChatClefBridgeReconciliationObserver()
        self._result_adapter = result_adapter or MinecraftChatClefBridgeResultAdapter()
        self._result_normalizer = (
            result_normalizer or MinecraftChatClefSubmissionResultNormalizer()
        )
        self._route_lock = route_lock or MinecraftChatClefSubmissionRouteLock()

    @property
    def pending_request_id(self) -> str | None:
        with self._route_lock:
            return self._state.pending_request_id

    def observe_submission_result(self, result: Mapping[str, Any]) -> None:
        with self._route_lock:
            if self._state.pending_request_id is not None:
                return
            if not self._policy.requires_reconciliation(result):
                return
            request_id = self._policy.result_request_id(result)
            if request_id is not None:
                self._state.retain(request_id)

    def blocking_result(self) -> dict[str, Any] | None:
        with self._route_lock:
            request_id = self._state.pending_request_id
            if request_id is None:
                return None
            return self._result_normalizer.unknown(
                request_id,
                "A previous Fabric ChatClef submission outcome is unresolved; "
                "matching terminal reconciliation is required before another "
                "command.",
            )

    def reconcile(self) -> bool:
        with self._route_lock:
            request_id = self._state.pending_request_id
            if request_id is None:
                return False
            observed = self._observer.observe(self._extension, request_id)
            if observed is None:
                return False
            try:
                payload = self._result_adapter.adapt(observed)
                result = self._result_normalizer.normalize(
                    payload,
                    expected_request_id=request_id,
                )
            except Exception:
                return False
            if not self._policy.is_matching_terminal(result, request_id):
                return False
            self._state.clear()
            return True
