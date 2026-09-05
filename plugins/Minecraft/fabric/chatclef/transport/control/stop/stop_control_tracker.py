#20260905_kpopmodder: Own one Python STOP lifecycle and its one terminal publication CAS.
from __future__ import annotations

import threading
from dataclasses import dataclass, field

from .stop_control_identity import StopControlIdentity
from .stop_control_target_snapshot import StopControlTargetSnapshot


@dataclass
class StopControlTracker:
    identity: StopControlIdentity
    barrier_token: object
    transport_websocket: object = field(repr=False, compare=False)
    request: object
    event_id: str
    source: str
    target_scope: str
    target: StopControlTargetSnapshot | None
    _state_lock: threading.Lock = field(default_factory=threading.Lock, repr=False)
    _delivery_lock: threading.Lock = field(default_factory=threading.Lock, repr=False)
    _terminal_delivered: bool = field(default=False, repr=False)
    _quarantined: bool = field(default=False, repr=False)
    _released: bool = field(default=False, repr=False)

    @property
    def quarantined(self) -> bool:
        with self._state_lock:
            return self._quarantined

    def enter_quarantine(self) -> bool:
        with self._state_lock:
            if self._quarantined or self._released:
                return False
            self._quarantined = True
            return True

    @property
    def released(self) -> bool:
        with self._state_lock:
            return self._released

    def mark_released(self) -> bool:
        with self._state_lock:
            if self._quarantined or self._released:
                return False
            self._released = True
            return True

    def claim_terminal_delivery(self) -> bool:
        with self._delivery_lock:
            if self._terminal_delivered:
                return False
            self._terminal_delivered = True
            return True


__all__ = ("StopControlTracker",)
