#20260905_kpopmodder: Reject multiple or non-ASCII-decimal quantities before Feature-B parsing.
from __future__ import annotations

import re
import unicodedata

from plugins.Minecraft.fabric.chatclef.intent.korean_text_normalizer import (
    KoreanTextNormalizer,
)

from .generic_crafting_quantity_shape import GenericCraftingQuantityShape


class GenericCraftingQuantityShapeGuard:
    _DIGIT_TOKEN_RE = re.compile(
        r"(?<![A-Za-z0-9_])[+-]?[0-9]+\s*(?:개|만큼)"
    )
    _KOREAN_TOKEN_RE = re.compile(
        r"(?<![가-힣])(?:하나|한|둘|두|셋|세|넷|네)(?:\s*(?:개|만큼))?(?![가-힣])"
    )

    def __init__(self, normalizer: KoreanTextNormalizer | None = None):
        self._normalizer = normalizer or KoreanTextNormalizer()

    def inspect(self, text: object) -> GenericCraftingQuantityShape:
        normalized = self._normalizer.normalize(text)
        spans = self._non_overlapping_spans(normalized)
        without_tokens = self._remove_spans(normalized, spans)
        if self._has_non_ascii_decimal(normalized):
            return GenericCraftingQuantityShape(
                False,
                len(spans),
                normalized,
                without_tokens,
                "generic_crafting_non_ascii_decimal_quantity",
            )
        if len(spans) > 1:
            return GenericCraftingQuantityShape(
                False,
                len(spans),
                normalized,
                without_tokens,
                "generic_crafting_multiple_quantity_tokens",
            )
        return GenericCraftingQuantityShape(
            True,
            len(spans),
            normalized,
            without_tokens,
        )

    def _non_overlapping_spans(self, text: str) -> list[tuple[int, int]]:
        matches = list(self._DIGIT_TOKEN_RE.finditer(text))
        matches.extend(self._KOREAN_TOKEN_RE.finditer(text))
        spans: list[tuple[int, int]] = []
        for match in sorted(matches, key=lambda item: (item.start(), item.end())):
            span = match.span()
            if spans and span[0] < spans[-1][1]:
                continue
            spans.append(span)
        return spans

    def _remove_spans(self, text: str, spans: list[tuple[int, int]]) -> str:
        pieces: list[str] = []
        cursor = 0
        for start, end in spans:
            pieces.extend((text[cursor:start], " "))
            cursor = end
        pieces.append(text[cursor:])
        return re.sub(r"\s+", " ", "".join(pieces)).strip()

    def _has_non_ascii_decimal(self, text: str) -> bool:
        return any(
            unicodedata.category(character) == "Nd"
            and character not in "0123456789"
            for character in text
        )
