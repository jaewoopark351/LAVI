#20260725_kpopmodder: Added Korean item aliases for Minecraft natural command parsing.
from __future__ import annotations

from .minecraft_korean_alias_text_normalizer import MinecraftKoreanAliasTextNormalizer


class MinecraftKoreanItemDictionary:
    ITEM_ALIASES = (
        ("\ub2e4\uc774\uc544\ubaac\ub4dc \uad11\uc11d", "diamond_ore"),
        ("\ub2e4\uc774\uc544\ubaac\ub4dc \uace1\uad2d\uc774", "diamond_pickaxe"),
        ("\ucc38\ub098\ubb34 \ud1b5\ub098\ubb34", "oak_log"),
        ("\ucc38\ub098\ubb34 \uc6d0\ubaa9", "oak_log"),
        ("\uc624\ud06c \ud1b5\ub098\ubb34", "oak_log"),
        ("\uc624\ud06c \uc6d0\ubaa9", "oak_log"),
        ("\uc790\uc791\ub098\ubb34 \uc6d0\ubaa9", "birch_log"),
        ("\uac00\ubb38\ube44\ub098\ubb34 \uc6d0\ubaa9", "spruce_log"),
        ("\uac00\ubb38\ube44 \uc6d0\ubaa9", "spruce_log"),
        ("\ucc38\ub098\ubb34 \ud310\uc790", "oak_planks"),
        ("\uc624\ud06c \ud310\uc790", "oak_planks"),
        ("\uc790\uc791\ub098\ubb34 \ud310\uc790", "birch_planks"),
        ("\uac00\ubb38\ube44\ub098\ubb34 \ud310\uc790", "spruce_planks"),
        ("\uac00\ubb38\ube44 \ud310\uc790", "spruce_planks"),
        ("\ucca0\uc81c \uace1\uad2d\uc774", "iron_pickaxe"),
        ("\ucca0 \uace1\uad2d\uc774", "iron_pickaxe"),
        ("\ub3cc \uace1\uad2d\uc774", "stone_pickaxe"),
        ("\ub098\ubb34 \uace1\uad2d\uc774", "wooden_pickaxe"),
        ("\uc791\uc5c5\ub300", "crafting_table"),
        ("\uc81c\uc791\ub300", "crafting_table"),
        ("\ub2e4\uc774\uc544\ubaac\ub4dc", "diamond"),
        ("\ub098\ubb34", "oak_log"),
        ("\ucc38\ub098\ubb34", "oak_log"),
        ("\uc624\ud06c", "oak_log"),
        ("\uc790\uc791\ub098\ubb34", "birch_log"),
        ("\uac00\ubb38\ube44\ub098\ubb34", "spruce_log"),
        ("\ub9c9\ub300\uae30", "stick"),
        ("\uc2a4\ud2f1", "stick"),
        ("\uc870\uc57d\ub3cc", "cobblestone"),
        ("\ucf54\ube14\uc2a4\ud1a4", "cobblestone"),
        ("\ub3cc", "cobblestone"),
        ("\ud654\ub85c", "furnace"),
        ("\uc11d\ud0c4", "coal"),
        ("\ucca0\uad11\uc11d", "iron_ore"),
        ("\ucca0 \uc6d0\uc11d", "raw_iron"),
    )

    def __init__(
        self,
        text_normalizer: MinecraftKoreanAliasTextNormalizer | None = None,
    ):
        self.text_normalizer = text_normalizer or MinecraftKoreanAliasTextNormalizer()

    def find_item(self, text: object) -> str:
        lowered = str(text or "").strip().lower()
        if not lowered:
            return ""

        normalized = self.text_normalizer.normalize(lowered)
        for phrase, item_id in self.ITEM_ALIASES:
            if phrase in lowered or self.text_normalizer.normalize(phrase) in normalized:
                return item_id
        return ""
