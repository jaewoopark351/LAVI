#20260905_kpopmodder: Keep at most one live Python STOP tracker.
from __future__ import annotations

import threading

from .stop_control_identity import StopControlIdentity
from .stop_control_tracker import StopControlTracker


class StopControlTrackerRegistry:
    def __init__(self):
        self._lock = threading.RLock()
        self._tracker: StopControlTracker | None = None

    def register(self, tracker: StopControlTracker) -> bool:
        with self._lock:
            if self._tracker is not None:
                return False
            self._tracker = tracker
            return True

    def current(self) -> StopControlTracker | None:
        with self._lock:
            return self._tracker

    def matching(self, identity: StopControlIdentity) -> StopControlTracker | None:
        with self._lock:
            tracker = self._tracker
            if tracker is None or tracker.identity != identity:
                return None
            return tracker

    def retire(self, tracker: StopControlTracker) -> bool:
        with self._lock:
            if self._tracker is not tracker:
                return False
            self._tracker = None
            return True

    def reset_for_shutdown(self) -> None:
        with self._lock:
            self._tracker = None


__all__ = ("StopControlTrackerRegistry",)
