#20260905_kpopmodder: Isolate generic non-validated translation rejection.
from __future__ import annotations


class OrdinaryGenericTranslationRejection:
    def __init__(self, decision_factory):
        self._decision_factory = decision_factory

    def decide(self, translation_status: str, translation: object):
        if translation_status == "validated":
            return None
        return self._decision_factory.translation_rejection(translation)


__all__ = ("OrdinaryGenericTranslationRejection",)
