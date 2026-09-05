#20260905_kpopmodder: Block ordinary Python command admission while one STOP is unresolved.
from __future__ import annotations

import threading

from .stop_control_barrier_token import StopControlBarrierToken


class StopControlAdmissionBarrier:
    def __init__(self):
        self._lock = threading.RLock()
        self._token: StopControlBarrierToken | None = None

    def close(self, identity: tuple[object, ...]) -> StopControlBarrierToken | None:
        with self._lock:
            if self._token is not None:
                return None
            token = StopControlBarrierToken(self, tuple(identity), object())
            self._token = token
            return token

    def release(self, token: object) -> bool:
        with self._lock:
            if type(token) is not StopControlBarrierToken:
                return False
            if token._owner is not self or self._token is not token:
                return False
            self._token = None
            return True

    @property
    def closed(self) -> bool:
        with self._lock:
            return self._token is not None

    def reset_for_shutdown(self) -> None:
        with self._lock:
            self._token = None


__all__ = ("StopControlAdmissionBarrier",)
