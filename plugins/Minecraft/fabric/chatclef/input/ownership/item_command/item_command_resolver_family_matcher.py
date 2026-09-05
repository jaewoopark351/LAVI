#20260905_kpopmodder: Identify exact resolver-owned item families without treating acquisition verbs as ownership.
from __future__ import annotations

import re

from plugins.Minecraft.fabric.chatclef.intent.chatclef_alias_repository import (
    ChatClefKoreanAliasRepository,
)
from plugins.Minecraft.fabric.chatclef.intent.korean_text_normalizer import (
    KoreanTextNormalizer,
)


class ItemCommandResolverFamilyMatcher:
    _AMBIGUOUS_FAMILIES = frozenset({"갑옷", "도구", "장비"})

    def __init__(
        self,
        aliases: ChatClefKoreanAliasRepository | None = None,
        normalizer: KoreanTextNormalizer | None = None,
    ):
        self._aliases = aliases or ChatClefKoreanAliasRepository()
        self._normalizer = normalizer or KoreanTextNormalizer()

    def match(self, item_phrase: object) -> str:
        normalized = self._normalizer.normalize(item_phrase)
        normalized = re.sub(r"\s+", "", normalized)
        if not normalized:
            return ""
        for family in self._AMBIGUOUS_FAMILIES:
            if normalized == family or normalized.endswith(family):
                return f"ambiguous:{family}"
        for alias, target in self._aliases.sorted_aliases(
            self._aliases.equipment_aliases
        ):
            compact_alias = self._normalizer.normalize(alias).replace(" ", "")
            if normalized == compact_alias or normalized.endswith(compact_alias):
                return f"equipment:{target}"
        return ""


__all__ = ("ItemCommandResolverFamilyMatcher",)
