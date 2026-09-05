#20260905_kpopmodder: Isolate translated-command DTO decoding and command naming.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.extension.natural_language.natural_language_command_input import (
    translated_command_name,
)
from plugins.Minecraft.fabric.chatclef.intent import ChatClefTranslationResultDTO


class TranslatedCommandDtoPolicy:
    def translation(self, value: object) -> ChatClefTranslationResultDTO:
        return ChatClefTranslationResultDTO.from_mapping(value)

    def command_name(self, command: object) -> str:
        return translated_command_name(command)


__all__ = ("TranslatedCommandDtoPolicy",)
