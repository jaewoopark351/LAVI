#20260905_kpopmodder: Construct typed STOP submission results for send outcomes.
from __future__ import annotations


class StopControlSendResultStage:
    def __init__(self, result_factory: object):
        self._result_factory = result_factory

    def accepted(self, tracker: object):
        return self._result_factory.create(True, "accepted", tracker)

    def rejected(self):
        return self._result_factory.create(False, "control_send_rejected")

    def unknown(self):
        return self._result_factory.create(False, "control_send_unknown")


__all__ = ("StopControlSendResultStage",)
