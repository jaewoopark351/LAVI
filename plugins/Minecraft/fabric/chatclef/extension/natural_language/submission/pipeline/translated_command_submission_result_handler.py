#20260905_kpopmodder: Isolate translated-command rejection result recording.
from __future__ import annotations


class TranslatedCommandSubmissionResultHandler:
    def __init__(self, *, result_factory, result_recorder):
        self._result_factory = result_factory
        self._result_recorder = result_recorder

    def translation_rejection(self, translation: object, action: str):
        payload = self._result_factory.translation_rejection(translation)
        self._result_recorder(payload, action)
        return payload

    def admission_rejection(
        self,
        command: object,
        translation: object,
        admission: object,
        action: str,
    ):
        payload = self._result_factory.admission_rejection(
            command,
            translation,
            admission,
        )
        self._result_recorder(payload, action)
        return payload

    def guarded(self, payload: object, action: str):
        self._result_recorder(payload, action)
        return payload


__all__ = ("TranslatedCommandSubmissionResultHandler",)
