#20260905_kpopmodder: Own validated non-item command compilation.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import (
    ChatClefTranslationResultDTO,
)


class ChatClefNonItemTranslationStage:
    def translate(self, intent: object, compiler: object) -> ChatClefTranslationResultDTO:
        command = compiler.compile(intent)
        return ChatClefTranslationResultDTO.validated(command=command, intent=intent)


__all__ = ("ChatClefNonItemTranslationStage",)
