#20260905_kpopmodder: Isolate translated-command request text and source reading.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.extension.natural_language.natural_language_command_input import (
    natural_language_text,
    request_source,
)


class TranslatedCommandInputReader:
    def natural_language_text(self, command: object) -> str:
        return natural_language_text(command)

    def source(self, command: object) -> str:
        return request_source(command)


__all__ = ("TranslatedCommandInputReader",)
