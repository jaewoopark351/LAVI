#20260803_kpopmodder: Added Korean natural-language orchestration before ChatClef command submission.
#20260827_kpopmodder: Consume guarded STORE_HOME candidates without LLM fallback or submission.
#20260905_kpopmodder: Decode H5 guards before generic UNKNOWN handling and never recover malformed guards.
#20260905_kpopmodder: Add an explicit deterministic request-local item-resolution seam for Feature B.
#20260905_kpopmodder: Delegate translation responsibilities to folderized collaborators.
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
from plugins.Minecraft.fabric.chatclef.intent.chatclef_target_catalog import (
    ChatClefTargetCatalog,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import (
    ChatClefTranslationResultDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.composite_chatclef_intent_extractor import (
    CompositeChatClefIntentExtractor,
)
from plugins.Minecraft.fabric.chatclef.intent.korean_chatclef_rule_parser import (
    KoreanChatClefRuleParser,
)
from plugins.Minecraft.fabric.chatclef.intent.korean_item_phrase_resolver import (
    KoreanItemPhraseResolver,
)
from plugins.Minecraft.fabric.chatclef.intent.natural_language import (
    ChatClefItemActionTranslator,
    ChatClefNaturalLanguageComponentGraph,
)
from plugins.Minecraft.fabric.chatclef.intent.scoped_resolution import (
    NO_PROFILE,
    ScopedItemResolutionProfile,
)


class ChatClefNaturalLanguageService:
    _ITEM_ACTION_INTENTS = ChatClefItemActionTranslator.ITEM_ACTION_INTENTS

    def __init__(
        self,
        extractor: CompositeChatClefIntentExtractor | None = None,
        schema_validator: ChatClefIntentSchemaValidator | None = None,
        resolver: KoreanItemPhraseResolver | None = None,
        target_catalog: ChatClefTargetCatalog | None = None,
        compiler: ChatClefCommandCompiler | None = None,
        auto_deposit_trust_guard_decoder: AutoDepositTrustGuardIntentDecoder
        | None = None,
        scoped_rule_parser: KoreanChatClefRuleParser | None = None,
    ):
        graph = ChatClefNaturalLanguageComponentGraph(
            extractor=extractor,
            schema_validator=schema_validator,
            resolver=resolver,
            target_catalog=target_catalog,
            compiler=compiler,
            guard_decoder=auto_deposit_trust_guard_decoder,
            scoped_rule_parser=scoped_rule_parser,
        )
        self._extractor = graph.extractor
        self._resolver = graph.resolver
        self._input_guard = graph.input_guard
        self._intent_policy = graph.intent_policy
        self._rejection_factory = graph.rejection_factory
        self._scoped_translation_service = graph.scoped_translation_service

    def translate(self, text: object) -> ChatClefTranslationResultDTO:
        raw_text, rejection = self._input_guard.inspect(text)
        if rejection is not None:
            return rejection
        try:
            intent = self._extractor.extract(raw_text)
            return self._translate_intent(intent, self._resolver)
        except Exception as error:
            return self._rejection_factory.internal_error(error)

    def translate_with_item_resolution_profile(
        self,
        text: object,
        item_resolution_profile: ScopedItemResolutionProfile = NO_PROFILE,
    ) -> ChatClefTranslationResultDTO:
        return self._scoped_translation_service.translate(
            text,
            item_resolution_profile,
        )

    def _translate_intent(
        self,
        intent: ChatClefIntentDTO,
        resolver: object,
    ) -> ChatClefTranslationResultDTO:
        return self._intent_policy.translate(intent, resolver)
