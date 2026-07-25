#20260725_kpopmodder: Added focused normalization for English get-item phrases.
from __future__ import annotations

import re

from .minecraft_item_phrase_normalizer import MinecraftItemPhraseNormalizer


class MinecraftGetItemCandidateNormalizer:
    NUMBER_WORDS = {
        "one": "1",
        "two": "2",
        "three": "3",
        "four": "4",
        "five": "5",
        "six": "6",
        "seven": "7",
        "eight": "8",
        "nine": "9",
        "ten": "10",
    }

    def __init__(
        self,
        item_phrase_normalizer: MinecraftItemPhraseNormalizer | None = None,
    ):
        self.item_phrase_normalizer = (
            item_phrase_normalizer or MinecraftItemPhraseNormalizer()
        )

    def normalize(self, candidate: str) -> str:
        normalized = str(candidate or "").strip()
        if not normalized:
            return ""

        normalized = self._replace_number_words(normalized)
        return self.item_phrase_normalizer.normalize(normalized)

    def _replace_number_words(self, value: str) -> str:
        normalized = value
        for word, number in self.NUMBER_WORDS.items():
            normalized = re.sub(
                rf"\b{re.escape(word)}\b",
                number,
                normalized,
                flags=re.IGNORECASE,
            )
        return normalized
