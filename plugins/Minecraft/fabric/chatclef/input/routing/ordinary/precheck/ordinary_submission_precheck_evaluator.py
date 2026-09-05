#20260905_kpopmodder: Isolate ordinary submission readiness and rejection result.
from __future__ import annotations


class OrdinarySubmissionPrecheckEvaluator:
    def __init__(self, *, extension, submission_precheck, decision_factory):
        self._extension = extension
        self._submission_precheck = submission_precheck
        self._decision_factory = decision_factory

    def inspect(self):
        return self._submission_precheck.inspect(self._extension)

    def rejection(self, readiness):
        return self._decision_factory.precheck_rejection(readiness)


__all__ = ("OrdinarySubmissionPrecheckEvaluator",)
