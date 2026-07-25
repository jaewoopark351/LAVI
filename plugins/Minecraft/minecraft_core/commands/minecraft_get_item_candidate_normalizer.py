#20260725_kpopmodder: Added focused normalization for English get-item phrases.
from __future__ import annotations

import re


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
    ITEM_PHRASES = (
        ("iron pickaxes", "iron_pickaxe"),
        ("iron pickaxe", "iron_pickaxe"),
        ("stone pickaxes", "stone_pickaxe"),
        ("stone pickaxe", "stone_pickaxe"),
        ("wooden pickaxes", "wooden_pickaxe"),
        ("wooden pickaxe", "wooden_pickaxe"),
        ("diamond pickaxes", "diamond_pickaxe"),
        ("diamond pickaxe", "diamond_pickaxe"),
        ("crafting tables", "crafting_table"),
        ("crafting table", "crafting_table"),
        ("oak logs", "oak_log"),
        ("oak log", "oak_log"),
        ("birch logs", "birch_log"),
        ("birch log", "birch_log"),
        ("spruce logs", "spruce_log"),
        ("spruce log", "spruce_log"),
        ("oak planks", "oak_planks"),
        ("oak plank", "oak_planks"),
        ("birch planks", "birch_planks"),
        ("birch plank", "birch_planks"),
        ("sticks", "stick"),
    )

    def normalize(self, candidate: str) -> str:
        normalized = str(candidate or "").strip()
        if not normalized:
            return ""

        normalized = self._replace_number_words(normalized)
        return self._replace_item_phrases(normalized)

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

    def _replace_item_phrases(self, value: str) -> str:
        normalized = value
        for phrase, item_id in self.ITEM_PHRASES:
            normalized = re.sub(
                rf"\b{re.escape(phrase)}\b",
                item_id,
                normalized,
                flags=re.IGNORECASE,
            )
        return normalized
