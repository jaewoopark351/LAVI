#20260905_kpopmodder: Decode generic-crafting submission input text only.
from __future__ import annotations

from typing import Any

from plugins.Minecraft.fabric.chatclef.extension.natural_language.natural_language_command_input import (
    natural_language_text,
)


class GenericCraftingSubmissionInputDecoder:
    def __init__(self, result_factory):
        self._result_factory = result_factory

    def decode(self, command: Any):
        try:
            return natural_language_text(command), None
        except Exception as error:
            return None, self._result_factory.operation_failure(
                command,
                "generic_crafting_input_internal_error",
                error,
            )


__all__ = ("GenericCraftingSubmissionInputDecoder",)
