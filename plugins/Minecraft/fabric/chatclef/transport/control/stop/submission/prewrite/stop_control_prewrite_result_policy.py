#20260905_kpopmodder: Map STOP prewrite status to a typed submission result only.
from __future__ import annotations


class StopControlPrewriteResultPolicy:
    def __init__(self, *, result_factory: object):
        self._result_factory = result_factory

    def resolve(self, *, tracker: object, status: str) -> object | None:
        if status == "ready":
            return None
        if status == "terminal":
            return self._result_factory.create(True, "accepted", tracker)
        if status == "unknown":
            return self._result_factory.create(False, "control_send_unknown")
        if status == "rejected":
            return self._result_factory.create(False, "control_send_rejected")
        raise ValueError("unknown STOP prewrite status")


__all__ = ("StopControlPrewriteResultPolicy",)
