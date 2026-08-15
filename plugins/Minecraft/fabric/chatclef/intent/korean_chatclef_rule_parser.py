#20260803_kpopmodder: Added deterministic Korean rule parsing for ChatClef intents.
from __future__ import annotations

import re

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import (
    ChatClefIntentDTO,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import (
    ChatClefIntentType,
)
from plugins.Minecraft.fabric.chatclef.intent.korean_quantity_parser import (
    KoreanQuantityParser,
)
from plugins.Minecraft.fabric.chatclef.intent.korean_acquisition_verb_matcher import (
    KoreanAcquisitionVerbMatcher,
)
from plugins.Minecraft.fabric.chatclef.intent.korean_text_normalizer import (
    KoreanTextNormalizer,
)


class KoreanChatClefRuleParser:
    _GOTO_RE = re.compile(
        r"(?P<x>-?\d+)\s+(?P<y>-?\d+)\s+(?P<z>-?\d+)"
        r"(?:\s*(?:으로|로))?\s*(?:이동|이동해|가|가줘|가자)"
    )
    _FOLLOW_RE = re.compile(
        r"(?P<player>[A-Za-z0-9_]{3,16})\s*(?:따라가|따라가줘|팔로우|쫓아가)"
    )

    def __init__(
        self,
        normalizer: KoreanTextNormalizer | None = None,
        quantity_parser: KoreanQuantityParser | None = None,
        acquisition_verbs: KoreanAcquisitionVerbMatcher | None = None,
    ):
        self._normalizer = normalizer or KoreanTextNormalizer()
        self._quantity_parser = quantity_parser or KoreanQuantityParser()
        self._acquisition_verbs = acquisition_verbs or KoreanAcquisitionVerbMatcher(
            self._normalizer
        )

    def parse(self, text: object) -> ChatClefIntentDTO:
        original = self._normalizer.normalize(text, lowercase_english=False)
        normalized = self._normalizer.normalize(text)
        if self._is_stop(normalized):
            return self._intent(ChatClefIntentType.STOP, original)
        if self._is_idle(normalized):
            return self._intent(ChatClefIntentType.IDLE, original)
        goto_match = self._GOTO_RE.search(normalized)
        if goto_match is not None:
            return self._intent(
                ChatClefIntentType.GOTO,
                original,
                x=int(goto_match.group("x")),
                y=int(goto_match.group("y")),
                z=int(goto_match.group("z")),
            )
        follow_match = self._FOLLOW_RE.search(original)
        if follow_match is not None:
            return self._intent(
                ChatClefIntentType.FOLLOW,
                original,
                player_name=follow_match.group("player"),
            )
        food_intent = self._food_or_meat_intent(original, normalized)
        if food_intent is not None:
            return food_intent
        if self._looks_like_get_item(normalized):
            quantity = self._quantity_parser.parse(normalized)
            item_phrase = self._item_phrase(normalized)
            return self._intent(
                ChatClefIntentType.GET_ITEM,
                original,
                quantity=quantity,
                item_phrase=item_phrase,
            )
        return self._intent(ChatClefIntentType.UNKNOWN, original)

    def _food_or_meat_intent(
        self,
        original: str,
        normalized: str,
    ) -> ChatClefIntentDTO | None:
        if "음식" in normalized and ("만큼" in normalized or "모아" in normalized):
            return self._intent(
                ChatClefIntentType.FOOD,
                original,
                food_units=self._quantity_parser.parse(normalized),
            )
        if (
            "고기" in normalized
            and "소고기" not in normalized
            and ("만큼" in normalized or "모아" in normalized)
        ):
            return self._intent(
                ChatClefIntentType.MEAT,
                original,
                food_units=self._quantity_parser.parse(normalized),
            )
        return None

    def _looks_like_get_item(self, normalized: str) -> bool:
        if self._acquisition_verbs.matches(normalized):
            return True
        return self._quantity_parser.strip_quantity(normalized) != normalized

    def _item_phrase(self, normalized: str) -> str:
        without_quantity = self._quantity_parser.strip_quantity(normalized)
        without_verbs = self._acquisition_verbs.strip(without_quantity)
        without_soft_words = re.sub(r"\b(?:좀|제발|주세요|줘)\b", " ", without_verbs)
        compacted = re.sub(r"\s+", " ", without_soft_words).strip()
        without_particles = re.sub(r"[을를]$", " ", compacted)
        return re.sub(r"\s+", " ", without_particles).strip()

    def _is_stop(self, normalized: str) -> bool:
        return normalized in {"멈춰", "멈춰줘", "중지", "정지", "스톱", "그만"}

    def _is_idle(self, normalized: str) -> bool:
        return normalized in {"가만히 있어", "대기", "대기해", "쉬어", "idle"}

    def _intent(
        self,
        intent_type: ChatClefIntentType,
        original: str,
        **slots: object,
    ) -> ChatClefIntentDTO:
        return ChatClefIntentDTO(
            intent_type=intent_type,
            original_text=original,
            source="rule",
            **slots,
        )
