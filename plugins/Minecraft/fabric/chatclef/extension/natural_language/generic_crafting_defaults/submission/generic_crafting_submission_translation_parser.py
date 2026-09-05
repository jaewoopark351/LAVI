#20260905_kpopmodder: Parse generic-crafting translation DTOs only.
from __future__ import annotations

from typing import Any

from plugins.Minecraft.fabric.chatclef.intent import ChatClefTranslationResultDTO


class GenericCraftingSubmissionTranslationParser:
    def __init__(self, result_factory):
        self._result_factory = result_factory

    def parse(self, translation: Any):
        try:
            return ChatClefTranslationResultDTO.from_mapping(translation), None
        except Exception as error:
            return None, self._result_factory.malformed_translation(error)


__all__ = ("GenericCraftingSubmissionTranslationParser",)
