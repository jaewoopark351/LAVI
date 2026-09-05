#20260905_kpopmodder: Isolate translated-command exception result conversion.
from __future__ import annotations


class TranslatedCommandSubmissionFailureHandler:
    _INTERNAL_REASON = "auto_deposit_trust_input_internal_error"

    def __init__(self, *, result_factory, result_recorder):
        self._result_factory = result_factory
        self._result_recorder = result_recorder

    def operation_failure(
        self,
        command: object,
        error: Exception,
        action: str,
    ):
        payload = self._result_factory.operation_failure(
            command,
            self._INTERNAL_REASON,
            error,
        )
        self._result_recorder(payload, action)
        return payload

    def malformed_translation(self, error: Exception, action: str):
        payload = self._result_factory.malformed_translation(error)
        self._result_recorder(payload, action)
        return payload


__all__ = ("TranslatedCommandSubmissionFailureHandler",)
