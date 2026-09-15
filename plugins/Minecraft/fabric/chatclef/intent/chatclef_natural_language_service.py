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
        runtime_catalog_provider=None,
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
        #20260915_kpopmodder: Session ownership stays outside translation; read a fresh snapshot per request.
        self._runtime_catalog_provider = runtime_catalog_provider
        self._name_vocabulary = None

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
        from dataclasses import replace
        from .names.command_target_resolver import CommandTargetResolver
        from .names.runtime_command_name_catalog import RuntimeCommandNameCatalog
        from .chatclef_intent_type import ChatClefIntentType as I
        snapshot = self._runtime_catalog_provider() if callable(self._runtime_catalog_provider) else None
        catalog = None
        if snapshot is not None:
            if self._name_vocabulary is None:
                from .navigation.find.find_name_repository import FindNameRepository
                self._name_vocabulary = FindNameRepository()
            catalog = RuntimeCommandNameCatalog(snapshot, self._name_vocabulary)
        command = {I.GET_ITEM: "get", I.EQUIP_ITEM: "equip", I.DEPOSIT_ITEM: "deposit", I.GIVE_ITEM: "give"}.get(intent.intent_type, intent.intent_type.value)
        scoped = CommandTargetResolver(resolver, command, catalog)
        if intent.slots.get("butler_user") is True:
            import re
            if catalog is None or type(catalog.butler_user) is not str or not re.fullmatch(r"[A-Za-z0-9_]{3,16}", catalog.butler_user):
                from .chatclef_intent_status import ChatClefIntentStatus
                return ChatClefTranslationResultDTO.rejected(ChatClefIntentStatus.INVALID, "verified_butler_user_required",
                    "연결된 게임 사용자 정보가 없어. 플레이어 이름을 직접 알려줘.", intent)
        result = self._intent_policy.translate(intent, scoped)
        if catalog is not None:
            data = {**result.data, "runtime_catalogue": dict(catalog.identity)}
            resolutions = result.data.get("resolutions", [result.data.get("resolution", {})])
            labels = {}
            for resolution in resolutions:
                detail = resolution.get("data", {})
                label, token = detail.get("label"), resolution.get("target")
                if detail.get("native_capability") is True and type(label) is str and type(token) is str:
                    labels[token] = label
            if labels:
                data["target_labels"] = labels
            if intent.slots.get("butler_user") is True:
                data["butler_user_bound"] = catalog.butler_user
            result = replace(result, data=data)
        return result
