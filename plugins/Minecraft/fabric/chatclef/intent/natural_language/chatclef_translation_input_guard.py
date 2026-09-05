#20260905_kpopmodder: Isolate normalization and command-slot safety validation.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import (
    ChatClefIntentStatus,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import (
    ChatClefTranslationResultDTO,
)

from .chatclef_translation_rejection_factory import (
    ChatClefTranslationRejectionFactory,
)


class ChatClefTranslationInputGuard:
    def __init__(
        self,
        *,
        compiler: object,
        rejection_factory: ChatClefTranslationRejectionFactory,
    ):
        self._compiler = compiler
        self._rejection_factory = rejection_factory

    def inspect(
        self,
        text: object,
    ) -> tuple[str, ChatClefTranslationResultDTO | None]:
        raw_text = str(text or "").strip()
        if not raw_text:
            return raw_text, self._rejection_factory.create(
                ChatClefIntentStatus.INVALID,
                "empty_input",
                "Korean command is empty.",
            )
        if self._compiler.has_dangerous_text(raw_text):
            return raw_text, self._rejection_factory.create(
                ChatClefIntentStatus.INVALID,
                "dangerous_command_slot",
                "Korean command contains characters that cannot enter ChatClef slots.",
            )
        return raw_text, None


__all__ = ("ChatClefTranslationInputGuard",)
