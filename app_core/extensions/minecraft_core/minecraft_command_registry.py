#20260725_kpopmodder: Added Minecraft extension command registry and action normalization.
from __future__ import annotations

from typing import Any


class MinecraftCommandRegistry:
    SUPPORTED_ACTIONS = {
        "health",
        "ping",
        "status",
        "get_status",
        "inventory",
        "get_inventory",
        "current_action",
        "actions_current",
        "get_current_action",
        "get_item",
        "get-item",
        "getitem",
        "get_and_equip",
        "get-and-equip",
        "getandequip",
        "craft",
        "craft_item",
        "craft-item",
        "craftitem",
        "make",
        "make_item",
        "make-item",
        "equip",
        "equip_item",
        "equip-item",
        "hold",
        "hold_item",
        "select",
        "select_item",
        "goto",
        "go_to",
        "move_to",
        "travel_to",
        "stop",
        "cancel",
        "reload",
    }

    def normalize_action(self, value: Any) -> str:
        return str(value or "").strip().lower().replace("-", "_")

    def supports(self, action: Any) -> bool:
        return self.normalize_action(action) in self.normalized_actions()

    def normalized_actions(self) -> set[str]:
        return {self.normalize_action(action) for action in self.SUPPORTED_ACTIONS}
