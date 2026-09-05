#20260905_kpopmodder: Isolate stateful translated-command admission commit.
from __future__ import annotations


class TranslatedCommandAdmissionCommitter:
    def __init__(self, admission):
        self._admission = admission

    def commit(self, inspection: object, route_claim: object, request: object):
        return self._admission.commit(inspection, route_claim, request)


__all__ = ("TranslatedCommandAdmissionCommitter",)
