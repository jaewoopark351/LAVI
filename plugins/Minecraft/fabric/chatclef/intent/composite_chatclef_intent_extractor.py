#20260803_kpopmodder: Added deterministic-first intent extraction with optional LLM fallback.
from __future__ import annotations

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


class CompositeChatClefIntentExtractor:
    def __init__(
        self,
        rule_parser: KoreanChatClefRuleParser | None = None,
        llm_extractor: ChatClefLLMIntentExtractor | None = None,
    ):
        self._rule_parser = rule_parser or KoreanChatClefRuleParser()
        self._llm_extractor = llm_extractor

    def extract(self, text: str) -> ChatClefIntentDTO:
        intent = self._rule_parser.parse(text)
        if intent.intent_type is not ChatClefIntentType.UNKNOWN:
            return intent
        if self._llm_extractor is None:
            return intent
        return self._llm_extractor.extract(text)
