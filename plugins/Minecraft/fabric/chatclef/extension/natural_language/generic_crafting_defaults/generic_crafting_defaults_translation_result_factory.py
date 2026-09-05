#20260905_kpopmodder: Build fail-closed Feature-B translation results in one boundary.
from __future__ import annotations

from typing import Any

from plugins.Minecraft.fabric.chatclef.intent import ChatClefTranslationResultDTO
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import (
    ChatClefIntentStatus,
)


class GenericCraftingDefaultsTranslationResultFactory:
    def rejected(
        self,
        reason_code: str,
        message: str,
    ) -> dict[str, Any]:
        return ChatClefTranslationResultDTO.rejected(
            status=ChatClefIntentStatus.INVALID,
            reason_code=reason_code,
            message=message,
            data={"generic_crafting_defaults": True},
        ).to_dict()
