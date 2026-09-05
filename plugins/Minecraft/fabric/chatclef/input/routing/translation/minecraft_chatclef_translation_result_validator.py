#20260905_kpopmodder: Isolate translation DTO shape and status validation.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import (
    ChatClefTranslationResultDTO,
)


class MinecraftChatClefTranslationResultValidator:
    def validate(self, payload: Any) -> dict[str, Any]:
        translation = ChatClefTranslationResultDTO.from_mapping(
            self.mapping_payload(payload)
        )
        return translation.to_dict()

    def status(self, translation: Mapping[str, Any]) -> str:
        return str(translation.get("status") or "").strip().lower()

    def mapping_payload(self, payload: Any) -> dict[str, Any]:
        if isinstance(payload, Mapping):
            return dict(payload)
        to_dict = getattr(payload, "to_dict", None)
        if callable(to_dict):
            mapped = to_dict()
            if isinstance(mapped, Mapping):
                return dict(mapped)
        raise TypeError("translation result must be an object")


__all__ = ("MinecraftChatClefTranslationResultValidator",)
