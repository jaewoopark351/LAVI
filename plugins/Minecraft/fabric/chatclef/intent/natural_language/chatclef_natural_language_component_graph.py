#20260905_kpopmodder: Compose natural-language translation collaborators in one boundary.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust import (
    AutoDepositTrustGuardIntentDecoder,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_command_compiler import (
    ChatClefCommandCompiler,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_schema_validator import (
    ChatClefIntentSchemaValidator,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_target_catalog import (
    ChatClefTargetCatalog,
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
from plugins.Minecraft.fabric.chatclef.intent.scoped_resolution import (
    ScopedItemResolutionTranslationService,
)

from .chatclef_translation_input_guard import ChatClefTranslationInputGuard
from .chatclef_translation_rejection_factory import (
    ChatClefTranslationRejectionFactory,
)
from .item import ChatClefItemActionTranslator
from .policy import ChatClefIntentTranslationPolicy


class ChatClefNaturalLanguageComponentGraph:
    def __init__(
        self,
        *,
        extractor: CompositeChatClefIntentExtractor | None = None,
        schema_validator: ChatClefIntentSchemaValidator | None = None,
        resolver: KoreanItemPhraseResolver | None = None,
        target_catalog: ChatClefTargetCatalog | None = None,
        compiler: ChatClefCommandCompiler | None = None,
        guard_decoder: AutoDepositTrustGuardIntentDecoder | None = None,
        scoped_rule_parser: KoreanChatClefRuleParser | None = None,
    ):
        self.extractor = extractor or CompositeChatClefIntentExtractor()
        self.resolver = resolver or KoreanItemPhraseResolver()
        active_compiler = compiler or ChatClefCommandCompiler()
        rejection_factory = ChatClefTranslationRejectionFactory()
        item_translator = ChatClefItemActionTranslator(
            resolver=self.resolver,
            compiler=active_compiler,
            rejection_factory=rejection_factory,
            target_catalog=target_catalog,
        )
        self.input_guard = ChatClefTranslationInputGuard(
            compiler=active_compiler,
            rejection_factory=rejection_factory,
        )
        self.intent_policy = ChatClefIntentTranslationPolicy(
            schema_validator=schema_validator or ChatClefIntentSchemaValidator(),
            guard_decoder=guard_decoder or AutoDepositTrustGuardIntentDecoder(),
            compiler=active_compiler,
            item_translator=item_translator,
            rejection_factory=rejection_factory,
        )
        self.rejection_factory = rejection_factory
        self.scoped_translation_service = ScopedItemResolutionTranslationService(
            base_resolver=self.resolver,
            compiler=active_compiler,
            rule_parser=scoped_rule_parser or KoreanChatClefRuleParser(),
            translate_intent_callback=self.intent_policy.translate,
        )


__all__ = ("ChatClefNaturalLanguageComponentGraph",)
