#20260818_kpopmodder: Keep Korean translation invocation and DTO validation in one single-pass boundary.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import (
    ChatClefTranslationResultDTO,
)


class MinecraftChatClefTranslationBoundary:
    def is_available(self, extension: Any) -> bool:
        return callable(
            getattr(extension, "translate_natural_language_command", None)
        )

    def translate_once(self, extension: Any, text: str) -> Any:
        translator = getattr(extension, "translate_natural_language_command")
        return translator(text)

    def validate(self, payload: Any) -> dict[str, Any]:
        translation = ChatClefTranslationResultDTO.from_mapping(
            self._mapping_payload(payload)
        )
        return translation.to_dict()

    def status(self, translation: Mapping[str, Any]) -> str:
        return str(translation.get("status") or "").strip().lower()

    def _mapping_payload(self, payload: Any) -> dict[str, Any]:
        if isinstance(payload, Mapping):
            return dict(payload)
        to_dict = getattr(payload, "to_dict", None)
        if callable(to_dict):
            mapped = to_dict()
            if isinstance(mapped, Mapping):
                return dict(mapped)
        raise TypeError("translation result must be an object")
