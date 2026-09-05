#20260905_kpopmodder: Isolate STOP prewrite retirement, release, and quarantine state.
from __future__ import annotations


class StopControlPrewriteStateCoordinator:
    def __init__(
        self,
        *,
        command_lock: object,
        tracker_registry: object,
        admission_barrier: object,
    ):
        self._command_lock = command_lock
        self._tracker_registry = tracker_registry
        self._admission_barrier = admission_barrier

    def ensure_sendable_while_locked(self, tracker: object) -> None:
        if self._tracker_registry.current() is not tracker:
            raise RuntimeError("STOP tracker changed before control send")
        if tracker.quarantined:
            raise RuntimeError("STOP tracker quarantined before control send")

    def reject_connection_change_while_locked(
        self,
        tracker: object,
    ) -> tuple[str, bool, bool]:
        retired = self._tracker_registry.retire(tracker)
        released = (
            self._admission_barrier.release(tracker.barrier_token)
            if retired
            else False
        )
        return "rejected", released, False

    def resolve_failure(self, tracker: object) -> tuple[str, bool, bool]:
        with self._command_lock:
            terminal_released = tracker.released
            if (
                not terminal_released
                and self._tracker_registry.current() is tracker
            ):
                tracker.enter_quarantine()
        if terminal_released:
            return "terminal", True, False
        return "unknown", False, tracker.quarantined


__all__ = ("StopControlPrewriteStateCoordinator",)
