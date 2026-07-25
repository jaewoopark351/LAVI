#20260725_kpopmodder: Added Minecraft item names for conversational replies.
from __future__ import annotations


class MinecraftConversationItemNameFormatter:
    ITEM_NAMES = {
        "birch_log": "\uc790\uc791\ub098\ubb34 \uc6d0\ubaa9",
        "birch_planks": "\uc790\uc791\ub098\ubb34 \ud310\uc790",
        "cobblestone": "\uc870\uc57d\ub3cc",
        "crafting_table": "\uc791\uc5c5\ub300",
        "diamond": "\ub2e4\uc774\uc544\ubaac\ub4dc",
        "diamond_ore": "\ub2e4\uc774\uc544\ubaac\ub4dc \uad11\uc11d",
        "diamond_pickaxe": "\ub2e4\uc774\uc544\ubaac\ub4dc \uace1\uad2d\uc774",
        "furnace": "\ud654\ub85c",
        "iron_ore": "\ucca0\uad11\uc11d",
        "iron_pickaxe": "\ucca0 \uace1\uad2d\uc774",
        "oak_log": "\ucc38\ub098\ubb34 \uc6d0\ubaa9",
        "oak_planks": "\ucc38\ub098\ubb34 \ud310\uc790",
        "raw_iron": "\ucca0 \uc6d0\uc11d",
        "spruce_log": "\uac00\ubb38\ube44\ub098\ubb34 \uc6d0\ubaa9",
        "spruce_planks": "\uac00\ubb38\ube44\ub098\ubb34 \ud310\uc790",
        "stick": "\ub9c9\ub300\uae30",
        "stone_pickaxe": "\ub3cc \uace1\uad2d\uc774",
        "wooden_pickaxe": "\ub098\ubb34 \uace1\uad2d\uc774",
    }

    def format(self, item: object) -> str:
        item_id = str(item or "").strip()
        if not item_id:
            return ""
        return self.ITEM_NAMES.get(item_id, item_id.replace("_", " "))
