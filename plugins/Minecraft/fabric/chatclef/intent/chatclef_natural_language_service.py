#20260803_kpopmodder: Added Korean natural-language orchestration before ChatClef command submission.
#20260827_kpopmodder: Consume guarded STORE_HOME candidates without LLM fallback or submission.
#20260905_kpopmodder: Decode H5 guards before generic UNKNOWN handling and never recover malformed guards.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust import (
    AutoDepositTrustGuardIntentDecoder,
)
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
from plugins.Minecraft.fabric.chatclef.intent.store_home import (
    StoreHomeIntentClassification,
)


class ChatClefNaturalLanguageService:
    _ITEM_ACTION_INTENTS = {
        ChatClefIntentType.GET_ITEM,
        ChatClefIntentType.EQUIP_ITEM,
        ChatClefIntentType.DEPOSIT_ITEM,
        ChatClefIntentType.GIVE_ITEM,
    }

    def __init__(
        self,
        extractor: CompositeChatClefIntentExtractor | None = None,
        schema_validator: ChatClefIntentSchemaValidator | None = None,
        resolver: KoreanItemPhraseResolver | None = None,
        target_catalog: ChatClefTargetCatalog | None = None,
        compiler: ChatClefCommandCompiler | None = None,
        auto_deposit_trust_guard_decoder: AutoDepositTrustGuardIntentDecoder
        | None = None,
    ):
        self._extractor = extractor or CompositeChatClefIntentExtractor()
        self._schema_validator = schema_validator or ChatClefIntentSchemaValidator()
        self._resolver = resolver or KoreanItemPhraseResolver()
        self._target_catalog = target_catalog
        self._compiler = compiler or ChatClefCommandCompiler()
        self._auto_deposit_trust_guard_decoder = (
            auto_deposit_trust_guard_decoder or AutoDepositTrustGuardIntentDecoder()
        )

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
            auto_deposit_trust_guard = (
                self._auto_deposit_trust_guard_decoder.decode(intent)
            )
            if auto_deposit_trust_guard.is_valid:
                classification = auto_deposit_trust_guard.classification
                if classification is None:
                    raise RuntimeError("valid_auto_deposit_trust_guard_missing_classification")
                return self._reject(
                    ChatClefIntentStatus.INVALID,
                    auto_deposit_trust_guard.reason_code,
                    auto_deposit_trust_guard.message,
                    intent,
                    {
                        "auto_deposit_trust_decision": (
                            classification.decision.value
                        )
                    },
                )
            if auto_deposit_trust_guard.is_invalid:
                return self._reject(
                    ChatClefIntentStatus.INVALID,
                    auto_deposit_trust_guard.reason_code,
                    auto_deposit_trust_guard.message,
                    intent,
                )
            valid, reason_code, message = self._schema_validator.validate(intent)
            if not valid:
                return self._reject(
                    ChatClefIntentStatus.INVALID,
                    reason_code,
                    message,
                    intent,
                )
            store_home_rejection = StoreHomeIntentClassification.guarded_from_intent(
                intent
            )
            if store_home_rejection is not None:
                return self._reject(
                    ChatClefIntentStatus.INVALID,
                    store_home_rejection.reason_code,
                    store_home_rejection.message,
                    intent,
                    {"store_home_decision": store_home_rejection.decision.value},
                )
            if intent.intent_type is ChatClefIntentType.UNKNOWN:
                return self._reject(
                    ChatClefIntentStatus.UNKNOWN,
                    "unknown_intent",
                    "Korean command did not match a supported ChatClef intent.",
                    intent,
                )
            if intent.intent_type in self._ITEM_ACTION_INTENTS:
                return self._translate_item_action(intent)
            command = self._compiler.compile(intent)
            return ChatClefTranslationResultDTO.validated(command=command, intent=intent)
        except Exception as error:
            return self._reject(
                ChatClefIntentStatus.INTERNAL_ERROR,
                "translation_internal_error",
                f"{type(error).__name__}: {error}",
            )

    def _translate_item_action(
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
        if intent.intent_type is ChatClefIntentType.EQUIP_ITEM:
            if not self._resolver.supports_equipment_target(target):
                return self._reject(
                    ChatClefIntentStatus.UNSUPPORTED,
                    "target_not_equippable",
                    f"Resolved target cannot be equipped by ChatClef: {target}",
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
