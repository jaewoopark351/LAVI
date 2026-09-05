#20260905_kpopmodder: Preserve translated-command input APIs as a thin facade.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent import ChatClefTranslationResultDTO

from plugins.Minecraft.fabric.chatclef.extension.natural_language.submission.stages.translated_command_dto_policy import TranslatedCommandDtoPolicy
from plugins.Minecraft.fabric.chatclef.extension.natural_language.submission.stages.translated_command_input_reader import TranslatedCommandInputReader


class TranslatedCommandInputStage:
    def __init__(self) -> None:
        self._input_reader = TranslatedCommandInputReader()
        self._dto_policy = TranslatedCommandDtoPolicy()

    def natural_language_text(self, command: object) -> str:
        return self._input_reader.natural_language_text(command)

    def translation(self, value: object) -> ChatClefTranslationResultDTO:
        return self._dto_policy.translation(value)

    def command_name(self, command: object) -> str:
        return self._dto_policy.command_name(command)

    def source(self, command: object) -> str:
        return self._input_reader.source(command)


__all__ = ("TranslatedCommandInputStage",)
