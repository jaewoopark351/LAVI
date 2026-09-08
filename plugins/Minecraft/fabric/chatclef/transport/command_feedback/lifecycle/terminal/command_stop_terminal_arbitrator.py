#20260907_kpopmodder: Delegate original cancellation only to its exact live trusted STOP owner.
from __future__ import annotations


class CommandStopTerminalArbitrator:
    USER_STOP_REQUESTED = "user_stop_requested"

    def __init__(self, stop_tracker_registry=None) -> None:
        self._stop_trackers = stop_tracker_registry

    def specialized_stop_owns_terminal(
        self,
        *,
        status: object,
        result_reason: object,
        expected_active: object,
    ) -> bool:
        status_text = getattr(status, "value", status)
        if (
            str(status_text or "").strip().lower() != "cancelled"
            or str(result_reason or "").strip()
            != self.USER_STOP_REQUESTED
            or expected_active is None
        ):
            return False
        registry = self._stop_trackers
        current = getattr(registry, "current", None)
        if not callable(current):
            return False
        try:
            tracker = current()
            if tracker is None or getattr(tracker, "quarantined", True) is True:
                return False
            target = getattr(tracker, "target", None)
            return bool(
                target is not None
                and getattr(target, "complete", False) is True
                and getattr(target, "owner_token", None) is expected_active
                and getattr(target, "request_id", None)
                == getattr(expected_active, "request_id", None)
                and getattr(target, "command_message_id", None)
                == getattr(expected_active, "command_message_id", None)
                and getattr(target, "session_id", None)
                == getattr(expected_active, "session_id", None)
                and getattr(target, "server_connection_generation", None)
                == getattr(expected_active, "generation", None)
            )
        except Exception:
            return False


__all__ = ("CommandStopTerminalArbitrator",)
