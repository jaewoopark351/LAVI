#20260905_kpopmodder: Isolate ordinary submitted-result route decisions.
from __future__ import annotations


class OrdinarySubmissionDecisionBuilder:
    def __init__(self, decision_factory):
        self._decision_factory = decision_factory

    def build(self, translation: object, result: object):
        return self._decision_factory.submitted(translation, result)


__all__ = ("OrdinarySubmissionDecisionBuilder",)
