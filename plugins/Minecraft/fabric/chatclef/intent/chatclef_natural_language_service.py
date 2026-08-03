#20260803_kpopmodder: Added Korean natural-language orchestration before ChatClef command submission.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.chatclef_command_compiler import (
    ChatClefCommandCompiler,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_schema_validator import (
    ChatClefIntentSchemaValidator,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import (
    ChatClefIntentStatus,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_target_catalog import (
    ChatClefTargetCatalog,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import (
    ChatClefTranslationResultDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.composite_chatclef_intent_extractor import (
    CompositeChatClefIntentExtractor,
)
from plugins.Minecraft.fabric.chatclef.intent.korean_item_phrase_resolver import (
    KoreanItemPhraseResolver,
)


class ChatClefNaturalLanguageService:
    def __init__(
        self,
        extractor: CompositeChatClefIntentExtractor | None = None,
        schema_validator: ChatClefIntentSchemaValidator | None = None,
        resolver: KoreanItemPhraseResolver | None = None,
        target_catalog: ChatClefTargetCatalog | None = None,
        compiler: ChatClefCommandCompiler | None = None,
    ):
        self._extractor = extractor or CompositeChatClefIntentExtractor()
        self._schema_validator = schema_validator or ChatClefIntentSchemaValidator()
        self._resolver = resolver or KoreanItemPhraseResolver()
        self._target_catalog = target_catalog
        self._compiler = compiler or ChatClefCommandCompiler()

    def translate(self, text: object) -> ChatClefTranslationResultDTO:
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
            intent = self._extractor.extract(raw_text)
            valid, reason_code, message = self._schema_validator.validate(intent)
            if not valid:
                return self._reject(
                    ChatClefIntentStatus.INVALID,
                    reason_code,
                    message,
                    intent,
                )
            if intent.intent_type is ChatClefIntentType.UNKNOWN:
                return self._reject(
                    ChatClefIntentStatus.UNKNOWN,
                    "unknown_intent",
                    "Korean command did not match a supported ChatClef intent.",
                    intent,
                )
            if intent.intent_type is ChatClefIntentType.GET_ITEM:
                return self._translate_get_item(intent)
            command = self._compiler.compile(intent)
            return ChatClefTranslationResultDTO.validated(command=command, intent=intent)
        except Exception as error:
            return self._reject(
                ChatClefIntentStatus.INTERNAL_ERROR,
                "translation_internal_error",
                f"{type(error).__name__}: {error}",
            )

    def _translate_get_item(
        self,
        intent: ChatClefIntentDTO,
    ) -> ChatClefTranslationResultDTO:
        resolution = self._resolver.resolve(intent.item_phrase)
        status = ChatClefIntentStatus(resolution["status"])
        if status is not ChatClefIntentStatus.VALIDATED:
            return self._reject(
                status,
                str(resolution["reason_code"]),
                "Korean item phrase could not be resolved to one ChatClef target.",
                intent,
                {"resolution": resolution},
            )
        target = str(resolution["target"])
        catalog = self._catalog()
        if not catalog.contains(target):
            return self._reject(
                ChatClefIntentStatus.UNSUPPORTED,
                "target_not_in_chatclef_catalog",
                f"Resolved target is not obtainable by ChatClef: {target}",
                intent,
                {"resolution": resolution},
            )
        command = self._compiler.compile(intent, target=target)
        return ChatClefTranslationResultDTO.validated(
            command=command,
            intent=intent,
            resolved_target=target,
            data={"resolution": resolution},
        )

    def _catalog(self) -> ChatClefTargetCatalog:
        if self._target_catalog is None:
            self._target_catalog = ChatClefTargetCatalog()
        return self._target_catalog

    def _reject(
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
