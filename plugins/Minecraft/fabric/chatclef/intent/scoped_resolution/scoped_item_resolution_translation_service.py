#20260905_kpopmodder: Isolate the request-local item-resolution translation pipeline.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import (
    ChatClefIntentStatus,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import (
    ChatClefTranslationResultDTO,
)

from .no_scoped_item_resolution_profile import NO_PROFILE
from .request_local_korean_item_phrase_resolver import (
    RequestLocalKoreanItemPhraseResolver,
)
from .scoped_item_resolution_profile import ScopedItemResolutionProfile


class ScopedItemResolutionTranslationService:
    def __init__(
        self,
        *,
        base_resolver,
        compiler,
        rule_parser,
        translate_intent_callback,
    ):
        self._base_resolver = base_resolver
        self._compiler = compiler
        self._rule_parser = rule_parser
        self._translate_intent_callback = translate_intent_callback

    def translate(
        self,
        text: object,
        item_resolution_profile: ScopedItemResolutionProfile = NO_PROFILE,
    ) -> ChatClefTranslationResultDTO:
        raw_text = str(text or "").strip()
        if not raw_text:
            return self._reject(
                ChatClefIntentStatus.INVALID,
                "empty_input",
                "Korean command is empty.",
            )
        if self._compiler.has_dangerous_text(raw_text):
            return self._reject(
                ChatClefIntentStatus.INVALID,
                "dangerous_command_slot",
                "Korean command contains characters that cannot enter ChatClef slots.",
            )
        try:
            intent = self._rule_parser.parse(raw_text)
            resolver = RequestLocalKoreanItemPhraseResolver(
                self._base_resolver,
                item_resolution_profile,
            )
            return self._translate_intent_callback(intent, resolver)
        except Exception as error:
            return self._reject(
                ChatClefIntentStatus.INTERNAL_ERROR,
                "translation_internal_error",
                f"{type(error).__name__}: {error}",
            )

    def _reject(
        self,
        status: ChatClefIntentStatus,
        reason_code: str,
        message: str,
    ) -> ChatClefTranslationResultDTO:
        return ChatClefTranslationResultDTO.rejected(
            status=status,
            reason_code=reason_code,
            message=message,
            data={},
        )


__all__ = ("ScopedItemResolutionTranslationService",)
