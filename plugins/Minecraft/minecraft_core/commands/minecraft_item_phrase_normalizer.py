#20260725_kpopmodder: Added shared item phrase normalization for Minecraft text commands.
from __future__ import annotations

import re


class MinecraftItemPhraseNormalizer:
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

    def normalize(self, value: str) -> str:
        normalized = str(value or "").strip()
        for phrase, item_id in self.ITEM_PHRASES:
            normalized = re.sub(
                rf"\b{re.escape(phrase)}\b",
                item_id,
                normalized,
                flags=re.IGNORECASE,
            )
        return normalized
