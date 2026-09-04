#20260803_kpopmodder: Added deterministic-first intent extraction with optional LLM fallback.
#20260827_kpopmodder: Keep deterministic STORE_HOME refusals out of the LLM fallback.
#20260905_kpopmodder: Preserve every H5 guard marker before optional LLM fallback.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.auto_deposit_trust import (
    AutoDepositTrustGuardMarkerDetector,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_llm_intent_extractor import (
    ChatClefLLMIntentExtractor,
)
from plugins.Minecraft.fabric.chatclef.intent.korean_chatclef_rule_parser import (
    KoreanChatClefRuleParser,
)
from plugins.Minecraft.fabric.chatclef.intent.store_home import (
    StoreHomeIntentClassification,
)


class CompositeChatClefIntentExtractor:
    def __init__(
        self,
        rule_parser: KoreanChatClefRuleParser | None = None,
        llm_extractor: ChatClefLLMIntentExtractor | None = None,
        auto_deposit_trust_guard_marker: AutoDepositTrustGuardMarkerDetector
        | None = None,
    ):
        self._rule_parser = rule_parser or KoreanChatClefRuleParser()
        self._llm_extractor = llm_extractor
        self._auto_deposit_trust_guard_marker = (
            auto_deposit_trust_guard_marker
            or AutoDepositTrustGuardMarkerDetector()
        )

    def extract(self, text: str) -> ChatClefIntentDTO:
        intent = self._rule_parser.parse(text)
        if intent.intent_type is not ChatClefIntentType.UNKNOWN:
            return intent
        if self._auto_deposit_trust_guard_marker.has_marker(intent):
            return intent
        if StoreHomeIntentClassification.guarded_from_intent(intent) is not None:
            return intent
        if self._llm_extractor is None:
            return intent
        return self._llm_extractor.extract(text)
