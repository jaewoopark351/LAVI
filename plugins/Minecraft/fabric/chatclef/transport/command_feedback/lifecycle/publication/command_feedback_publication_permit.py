#20260907_kpopmodder: Gate one lifecycle publication turn without owning command state.
from __future__ import annotations

import threading
from typing import ClassVar


class CommandFeedbackPublicationPermit:
    START: ClassVar[str] = "start"
    STATUS: ClassVar[str] = "status"
    _WAITING: ClassVar[str] = "waiting"
    _READY: ClassVar[str] = "ready"
    _CANCELLED: ClassVar[str] = "cancelled"

    __slots__ = ("lifecycle_token", "sequence", "kind", "_condition", "_state")

    def __init__(self, *, lifecycle_token: object, sequence: int, kind: str) -> None:
        if type(sequence) is not int or sequence < 1:
            raise ValueError("command feedback publication sequence must be positive")
        if kind not in (self.START, self.STATUS):
            raise ValueError("command feedback publication kind is invalid")
        self.lifecycle_token = lifecycle_token
        self.sequence = sequence
        self.kind = kind
        self._condition = threading.Condition()
        self._state = self._WAITING

    def wait_until_ready(self, timeout_seconds: float) -> bool:
        if type(timeout_seconds) not in (int, float) or timeout_seconds <= 0:
            raise ValueError("command feedback publication timeout must be positive")
        with self._condition:
            self._condition.wait_for(
                lambda: self._state != self._WAITING,
                timeout=float(timeout_seconds),
            )
            return self._state == self._READY

    def _activate_turn(self) -> bool:
        with self._condition:
            if self._state != self._WAITING:
                return False
            self._state = self._READY
            self._condition.notify_all()
            return True

    def _cancel_turn(self) -> None:
        with self._condition:
            self._state = self._CANCELLED
            self._condition.notify_all()

    def _is_ready(self) -> bool:
        with self._condition:
            return self._state == self._READY


__all__ = ("CommandFeedbackPublicationPermit",)
