#20260905_kpopmodder: Own STOP tracker and barrier transitions after a send attempt.
from __future__ import annotations


class StopControlSendStateTransitionStage:
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

    def terminal_was_released(self, tracker: object) -> bool:
        with self._command_lock:
            return tracker.released

    def resolve_not_scheduled(
        self,
        tracker: object,
    ) -> tuple[bool, bool]:
        with self._command_lock:
            terminal_released = tracker.released
            if terminal_released:
                released = True
            else:
                retired = self._tracker_registry.retire(tracker)
                released = (
                    self._admission_barrier.release(tracker.barrier_token)
                    if retired
                    else False
                )
        return terminal_released, released

    def quarantine_if_unreleased(
        self,
        tracker: object,
    ) -> tuple[bool, bool]:
        with self._command_lock:
            terminal_released = tracker.released
            if (
                not terminal_released
                and self._tracker_registry.current() is tracker
            ):
                tracker.enter_quarantine()
        return terminal_released, tracker.quarantined

    def snapshot(self, tracker: object) -> tuple[bool, bool]:
        with self._command_lock:
            return tracker.released, tracker.quarantined


__all__ = ("StopControlSendStateTransitionStage",)
