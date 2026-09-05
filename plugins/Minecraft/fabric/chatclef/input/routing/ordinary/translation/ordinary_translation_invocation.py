#20260905_kpopmodder: Isolate ordinary-command translation invocation failures.
from __future__ import annotations


class OrdinaryTranslationInvocation:
    def __init__(self, *, extension, translation_boundary, failure_handler):
        self._extension = extension
        self._translation_boundary = translation_boundary
        self._failure_handler = failure_handler

    def invoke(self, command_text: str):
        try:
            return (
                self._translation_boundary.translate_once(
                    self._extension,
                    command_text,
                ),
                None,
            )
        except Exception as error:
            return None, self._failure_handler.decision(
                "translation_failed",
                error,
            )


__all__ = ("OrdinaryTranslationInvocation",)
