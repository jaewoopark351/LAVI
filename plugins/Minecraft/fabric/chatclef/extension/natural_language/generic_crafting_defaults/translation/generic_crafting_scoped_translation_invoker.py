#20260905_kpopmodder: Invoke and parse one scoped crafting translation only.
from __future__ import annotations

from typing import Any

from plugins.Minecraft.fabric.chatclef.extension.natural_language.natural_language_command_input import (
    natural_language_text,
)
from plugins.Minecraft.fabric.chatclef.intent import ChatClefTranslationResultDTO


class GenericCraftingScopedTranslationInvoker:
    def __init__(self, *, natural_language_service, result_factory):
        self._natural_language_service = natural_language_service
        self._result_factory = result_factory

    def invoke(self, command: Any, item_resolution_profile: object):
        translator = getattr(
            self._natural_language_service,
            "translate_with_item_resolution_profile",
            None,
        )
        if not callable(translator):
            return None, self._result_factory.rejected(
                "generic_crafting_scoped_translation_unavailable",
                "요청별 제작 해석 기능을 사용할 수 없어요.",
            )
        try:
            text = natural_language_text(command)
            translation = translator(text, item_resolution_profile)
            translated = ChatClefTranslationResultDTO.from_mapping(translation)
        except Exception as error:
            return None, self._result_factory.rejected(
                "generic_crafting_scoped_translation_failed",
                f"{type(error).__name__}: {error}",
            )
        return translated, None


__all__ = ("GenericCraftingScopedTranslationInvoker",)
