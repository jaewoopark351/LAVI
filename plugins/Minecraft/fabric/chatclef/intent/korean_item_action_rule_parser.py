#20260820_kpopmodder: Parse Korean item-action ChatClef commands without changing Java command grammar.
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
from plugins.Minecraft.fabric.chatclef.intent.korean_text_normalizer import (
    KoreanTextNormalizer,
)


class KoreanItemActionRuleParser:
    _PLAYER_RE = r"[A-Za-z0-9_]{3,16}"
    _GIVE_RE = re.compile(
        rf"(?P<player>{_PLAYER_RE})\s*(?:에게|한테)\s*"
        r"(?P<body>.+?)\s*(?:줘|주기|전달해\s*줘|전달해|넘겨\s*줘|넘겨)$"
    )
    _EQUIP_ACTION_RE = re.compile(
        r"(?:입어\s*줘|입어|착용해\s*줘|착용해|착용|장착해\s*줘|장착해|장착)$"
    )
    _DEPOSIT_CONTAINER_RE = re.compile(r"(?:상자|창고|보관함)\s*(?:안에|에|로)?")
    _DEPOSIT_ACTION_RE = re.compile(
        r"(?:넣어\s*줘|넣어|저장해\s*줘|저장해|저장|보관해\s*줘|보관해|보관)"
    )

    def __init__(
        self,
        normalizer: KoreanTextNormalizer | None = None,
        quantity_parser: KoreanQuantityParser | None = None,
    ):
        self._normalizer = normalizer or KoreanTextNormalizer()
        self._quantity_parser = quantity_parser or KoreanQuantityParser()

    def parse(self, original: str, normalized: str) -> ChatClefIntentDTO | None:
        give_intent = self._parse_give(original)
        if give_intent is not None:
            return give_intent
        if self._looks_like_deposit(normalized):
            return self._intent(
                ChatClefIntentType.DEPOSIT_ITEM,
                original,
                quantity=self._quantity_parser.parse_optional(normalized),
                item_phrase=self._deposit_item_phrase(normalized),
            )
        if self._looks_like_equip(normalized):
            return self._intent(
                ChatClefIntentType.EQUIP_ITEM,
                original,
                quantity=1,
                item_phrase=self._equip_item_phrase(normalized),
            )
        return None

    def _parse_give(self, original: str) -> ChatClefIntentDTO | None:
        original_normalized = self._normalizer.normalize(
            original,
            lowercase_english=False,
        )
        match = self._GIVE_RE.search(original_normalized)
        if match is None:
            return None
        body = self._normalizer.normalize(match.group("body"))
        return self._intent(
            ChatClefIntentType.GIVE_ITEM,
            original_normalized,
            player_name=match.group("player"),
            quantity=self._quantity_parser.parse(body),
            item_phrase=self._quantity_parser.strip_quantity(body),
        )

    def _looks_like_deposit(self, normalized: str) -> bool:
        return (
            self._DEPOSIT_CONTAINER_RE.search(normalized) is not None
            and self._DEPOSIT_ACTION_RE.search(normalized) is not None
        )

    def _deposit_item_phrase(self, normalized: str) -> str:
        phrase = self._quantity_parser.strip_quantity(normalized)
        phrase = self._DEPOSIT_CONTAINER_RE.sub(" ", phrase)
        phrase = self._DEPOSIT_ACTION_RE.sub(" ", phrase)
        phrase = re.sub(r"(?:해\s*줘|해|줘|주세요)$", " ", phrase)
        phrase = re.sub(r"[\s]*(?:을|를)$", " ", phrase)
        return re.sub(r"\s+", " ", phrase).strip()

    def _looks_like_equip(self, normalized: str) -> bool:
        return self._EQUIP_ACTION_RE.search(normalized) is not None

    def _equip_item_phrase(self, normalized: str) -> str:
        phrase = self._quantity_parser.strip_quantity(normalized)
        phrase = self._EQUIP_ACTION_RE.sub(" ", phrase)
        phrase = re.sub(r"[\s]*(?:을|를)$", " ", phrase)
        return re.sub(r"\s+", " ", phrase).strip()

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
