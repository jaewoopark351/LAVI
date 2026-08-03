#20260803_kpopmodder: Added deterministic Korean equipment target composition.
from __future__ import annotations


class ChatClefEquipmentTargetComposer:
    EQUIPMENT_BY_MATERIAL = {
        "wooden": {"pickaxe", "shovel", "sword", "axe", "hoe"},
        "stone": {"pickaxe", "shovel", "sword", "axe", "hoe"},
        "iron": {
            "pickaxe",
            "shovel",
            "sword",
            "axe",
            "hoe",
            "helmet",
            "chestplate",
            "leggings",
            "boots",
        },
        "golden": {
            "pickaxe",
            "shovel",
            "sword",
            "axe",
            "hoe",
            "helmet",
            "chestplate",
            "leggings",
            "boots",
        },
        "diamond": {
            "pickaxe",
            "shovel",
            "sword",
            "axe",
            "hoe",
            "helmet",
            "chestplate",
            "leggings",
            "boots",
        },
        "netherite": {
            "pickaxe",
            "shovel",
            "sword",
            "axe",
            "hoe",
            "helmet",
            "chestplate",
            "leggings",
            "boots",
        },
        "leather": {"helmet", "chestplate", "leggings", "boots"},
    }

    def compose(self, material: str, equipment: str) -> str | None:
        material_key = str(material or "")
        equipment_key = str(equipment or "")
        if equipment_key not in self.EQUIPMENT_BY_MATERIAL.get(material_key, set()):
            return None
        return f"{material_key}_{equipment_key}"

    def all_targets(self) -> set[str]:
        return {
            f"{material}_{equipment}"
            for material, equipment_set in self.EQUIPMENT_BY_MATERIAL.items()
            for equipment in equipment_set
        }
