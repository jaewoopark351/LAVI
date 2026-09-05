#20260905_kpopmodder: Reset only Fabric ChatClef transport-owned state at server shutdown.
from __future__ import annotations


class FabricChatClefServerShutdownStateReset:
    def __init__(
        self,
        *,
        connection_ownership,
        command_lock,
        session_registry,
        stop_control_claim_registry,
        stop_control_tracker_registry,
        stop_control_admission_barrier,
    ) -> None:
        self._connection_ownership = connection_ownership
        self._command_lock = command_lock
        self._session_registry = session_registry
        self._stop_control_claim_registry = stop_control_claim_registry
        self._stop_control_tracker_registry = stop_control_tracker_registry
        self._stop_control_admission_barrier = stop_control_admission_barrier

    def clear_connection(self) -> None:
        self._session_registry.clear()
        with self._command_lock:
            self._connection_ownership.clear()

    def reset_for_shutdown(self) -> None:
        self._session_registry.clear()
        with self._command_lock:
            self._connection_ownership.clear()
            self._stop_control_claim_registry.reset_for_shutdown()
            self._stop_control_tracker_registry.reset_for_shutdown()
            self._stop_control_admission_barrier.reset_for_shutdown()
