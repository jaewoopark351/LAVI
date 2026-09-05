#20260905_kpopmodder: Isolate the unchanged legacy natural-language route.
from __future__ import annotations

from typing import Any

from .natural_language_command_input import natural_language_text


class LegacyNaturalLanguageCommandCoordinator:
    def __init__(self, *, natural_language_service, translated_submission):
        self._natural_language_service = natural_language_service
        self._translated_submission = translated_submission

    def translate(self, command: Any) -> dict[str, Any]:
        text = natural_language_text(command)
        return self._natural_language_service.translate(text).to_dict()

    def handle(self, command: Any) -> dict[str, Any]:
        text = natural_language_text(command)
        translation = self._natural_language_service.translate(text)
        return self._translated_submission.submit_translation(
            command,
            translation,
            text,
            action="translate_natural_language_command",
        )


__all__ = ("LegacyNaturalLanguageCommandCoordinator",)
