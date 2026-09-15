#20260915_kpopmodder: Simulated native capabilities separate name recognition from command support.
from ..all_commands.catalogue_fixture import catalogue


def item_catalogue(*, equip=True):
    snapshot = catalogue()
    snapshot["entries"].extend([
        {"kind": "item", "id": "minecraft:diamond_leggings", "translation_key": "item.minecraft.diamond_leggings",
         "korean_name": "다이아몬드 레깅스", "tokens": {"equip": "diamond_leggings"} if equip else {},
         "capabilities": ["equip"] if equip else []},
        {"kind": "item", "id": "minecraft:elytra", "translation_key": "item.minecraft.elytra",
         "korean_name": "겉날개", "tokens": {"give": "elytra"}, "capabilities": ["give"]},
    ])
    return snapshot
