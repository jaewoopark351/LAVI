#20260905_kpopmodder: Isolate translated-command transport submission.
from __future__ import annotations


class TranslatedCommandTransportInvoker:
    def __init__(self, command_submitter):
        self._command_submitter = command_submitter

    def submit(self, request: object):
        return self._command_submitter(request)


__all__ = ("TranslatedCommandTransportInvoker",)
