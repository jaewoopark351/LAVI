#20260905_kpopmodder: Centralize deterministic rejection construction in the feature package.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import (
    ChatClefIntentStatus,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import (
    ChatClefTranslationResultDTO,
)


class ChatClefTranslationRejectionFactory:
    def create(
        self,
        status: ChatClefIntentStatus,
        reason_code: str,
        message: str,
        intent: ChatClefIntentDTO | None = None,
        data: dict[str, object] | None = None,
    ) -> ChatClefTranslationResultDTO:
        return ChatClefTranslationResultDTO.rejected(
            status=status,
            reason_code=reason_code,
            message=message,
            intent=intent,
            data=data or {},
        )

    def internal_error(self, error: Exception) -> ChatClefTranslationResultDTO:
        return self.create(
            ChatClefIntentStatus.INTERNAL_ERROR,
            "translation_internal_error",
            f"{type(error).__name__}: {error}",
        )


__all__ = ("ChatClefTranslationRejectionFactory",)
