#20260905_kpopmodder: Recognize only explicit leading Minecraft ownership markers.
from __future__ import annotations

import re

from plugins.Minecraft.fabric.chatclef.intent.korean_text_normalizer import (
    KoreanTextNormalizer,
)


class ItemCommandMinecraftMarkerMatcher:
    _LEADING_MARKER = re.compile(
        r"^(?:마크|마인크래프트)(?: +ai)? +",
        re.ASCII,
    )

    def __init__(self, normalizer: KoreanTextNormalizer | None = None):
        self._normalizer = normalizer or KoreanTextNormalizer()

    def matches(self, text: object) -> bool:
        if type(text) is not str:
            return False
        normalized = self._normalizer.normalize(text)
        return self._LEADING_MARKER.match(normalized) is not None


__all__ = ("ItemCommandMinecraftMarkerMatcher",)
