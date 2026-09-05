#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from .prewrite import (
    StopControlPrewriteConnectionValidator,
    StopControlPrewriteStateCoordinator,
)


class StopControlPrewriteGuard:
    def __init__(
        self,
        *,
        command_lock: object,
        connection_ownership: object,
        tracker_registry: object,
        admission_barrier: object,
    ):
        self._command_lock = command_lock
        self._connection_validator = StopControlPrewriteConnectionValidator(
            connection_ownership
        )
        self._state_coordinator = StopControlPrewriteStateCoordinator(
            command_lock=command_lock,
            tracker_registry=tracker_registry,
            admission_barrier=admission_barrier,
        )

    def evaluate(self, tracker: object) -> tuple[str, bool, bool]:
        try:
            with self._command_lock:
                self._state_coordinator.ensure_sendable_while_locked(tracker)
                if not self._connection_validator.matches(tracker):
                    return self._state_coordinator.reject_connection_change_while_locked(
                        tracker
                    )
        except Exception:
            return self._state_coordinator.resolve_failure(tracker)
        return "ready", False, False


__all__ = ("StopControlPrewriteGuard",)
