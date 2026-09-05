#20260905_kpopmodder: Own validated item-command compilation result construction.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import (
    ChatClefTranslationResultDTO,
)


class ChatClefItemCommandCompilationStage:
    def compile(
        self,
        *,
        intent: object,
        target: str,
        resolution: dict[str, object],
        compiler: object,
    ) -> ChatClefTranslationResultDTO:
        command = compiler.compile(intent, target=target)
        return ChatClefTranslationResultDTO.validated(
            command=command,
            intent=intent,
            resolved_target=target,
            data={"resolution": resolution},
        )


__all__ = ("ChatClefItemCommandCompilationStage",)
