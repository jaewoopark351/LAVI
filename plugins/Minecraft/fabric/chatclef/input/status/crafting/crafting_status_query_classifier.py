#20260907_kpopmodder: Classify only the approved Korean crafting-status grammar.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.korean_text_normalizer import (
    KoreanTextNormalizer,
)

from .crafting_status_query import CraftingStatusQuery


class CraftingStatusQueryClassifier:
    _GENERIC = frozenset(
        {
            "마크 지금 뭐 만들고 있어",
            "마크 뭐 만들고 있어",
            "마크 지금 뭐 만드는 중이야",
            "마크 뭐 만드는 중이야",
        }
    )
    _DIAMOND_PICKAXE = frozenset(
        {
            "다이아 곡괭이 만들고 있어",
            "다이아 곡괭이 만드는 중이야",
            "지금 다이아 곡괭이 만들고 있어",
            "지금 다이아 곡괭이 만드는 중이야",
            "마크 다이아 곡괭이 만들고 있어",
            "마크 다이아 곡괭이 만드는 중이야",
        }
    )

    def __init__(self, normalizer=None) -> None:
        self._normalizer = normalizer or KoreanTextNormalizer()

    def classify(self, text: object) -> CraftingStatusQuery | None:
        normalized = self._normalizer.normalize(text)
        if normalized in self._GENERIC:
            return CraftingStatusQuery(target_item=None)
        if normalized in self._DIAMOND_PICKAXE:
            return CraftingStatusQuery(target_item="diamond_pickaxe")
        return None


__all__ = ("CraftingStatusQueryClassifier",)
