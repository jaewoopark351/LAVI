#20260915_kpopmodder: Explicit simulated Java catalogue; these rows do not establish installed-game coverage.
def catalogue():
    return {
        "schema_version": 1,
        "minecraft_version": "1.20.1",
        "session_id": "confirmation-session",
        "catalogue_sha256": "a" * 64,
        "butler_user": "Alex",
        "entries": [
            {
                "kind": "entity",
                "id": "minecraft:zombie",
                "translation_key": "entity.minecraft.zombie",
                "korean_name": "좀비",
                "tokens": {"attack": "zombie"},
                "capabilities": ["attack", "find"],
            },
            {
                "kind": "block",
                "id": "minecraft:diamond_ore",
                "translation_key": "block.minecraft.diamond_ore",
                "korean_name": "다이아몬드 원석",
                "tokens": {"scan": "DIAMOND_ORE"},
                "capabilities": ["scan", "find"],
            },
        ],
    }
