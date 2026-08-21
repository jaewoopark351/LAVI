#20260820_kpopmodder: Generate Korean ChatClef item artifacts from the 1.20.1 catalog and official ko_kr asset.
from __future__ import annotations

import collections
import hashlib
import json
import re
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[3]
RESOURCE_DIR = (
    REPO_ROOT / "plugins" / "Minecraft" / "fabric" / "chatclef" / "intent" / "resources"
)
CATALOG_PATH = (
    REPO_ROOT
    / "plugins"
    / "Minecraft"
    / "runtime"
    / "chatclef_fabric_1.20.1"
    / "CataloguedResources.txt"
)
ASSET_INDEX_CANDIDATES = (
    Path(r"C:\Users\jaewo\curseforge\minecraft\Install\assets\indexes\5.json"),
    Path(r"C:\Users\jaewo\AppData\Roaming\.minecraft\assets\indexes\5.json"),
)
CATALOG_TARGET_COUNT = 591


CURATED_ALIASES = {
    "구운 소고기": "cooked_beef",
    "금": "gold_ingot",
    "골드 주괴": "gold_ingot",
    "금 주괴": "gold_ingot",
    "금괴": "gold_ingot",
    "나무": "log",
    "다이아": "diamond",
    "다이아몬드": "diamond",
    "돌": "stone",
    "에메랄드": "emerald",
    "레드스톤": "redstone",
    "스테이크": "cooked_beef",
    "석탄": "coal",
    "원목": "log",
    "익힌 소고기": "cooked_beef",
    "조약돌": "cobblestone",
    "횃불": "torch",
    "철": "iron_ingot",
    "철 주괴": "iron_ingot",
    "철괴": "iron_ingot",
    "통나무": "log",
}

LEGACY_LANG_TARGETS = {
    "book_and_quill": ("item.minecraft.writable_book", "writable_book"),
    "diamond_pick": ("item.minecraft.diamond_pickaxe", "diamond_pickaxe"),
    "eye_of_ender": ("item.minecraft.ender_eye", "ender_eye"),
    "gold_pick": ("item.minecraft.golden_pickaxe", "golden_pickaxe"),
    "iron_pick": ("item.minecraft.iron_pickaxe", "iron_pickaxe"),
    "lapis": ("item.minecraft.lapis_lazuli", "lapis_lazuli"),
    "milk": ("item.minecraft.milk_bucket", None),
    "minecart_with_chest": ("item.minecraft.chest_minecart", "chest_minecart"),
    "minecart_with_furnace": ("item.minecraft.furnace_minecart", "furnace_minecart"),
    "minecart_with_hopper": ("item.minecraft.hopper_minecart", "hopper_minecart"),
    "minecart_with_tnt": ("item.minecraft.tnt_minecart", "tnt_minecart"),
    "netherite_pick": ("item.minecraft.netherite_pickaxe", "netherite_pickaxe"),
    "oxeye_dasiy": ("block.minecraft.oxeye_daisy", None),
    "stone_pick": ("item.minecraft.stone_pickaxe", "stone_pickaxe"),
    "wooden_pick": ("item.minecraft.wooden_pickaxe", "wooden_pickaxe"),
}

GENERIC_DISPLAYS = {
    "bed": "침대",
    "boat": "보트",
    "door": "문",
    "fence": "울타리",
    "fence_gate": "울타리 문",
    "flower": "꽃",
    "leaves": "잎",
    "log": "원목",
    "mushroom": "버섯",
    "planks": "판자",
    "trapdoor": "다락문",
    "wooden_button": "나무 버튼",
    "wooden_door": "나무 문",
    "wooden_fence": "나무 울타리",
    "wooden_fence_gate": "나무 울타리 문",
    "wooden_pressure_plate": "나무 압력판",
    "wooden_slab": "나무 반 블록",
    "wooden_stairs": "나무 계단",
    "wooden_trapdoor": "나무 다락문",
    "wool": "양털",
}

UNSUPPORTED_DISPLAYS = {
    "crimson_boat": "진홍빛 보트",
    "crimson_leaves": "진홍빛 잎",
    "crimson_log": "진홍빛 원목",
    "warped_boat": "뒤틀린 보트",
    "warped_leaves": "뒤틀린 잎",
    "warped_log": "뒤틀린 원목",
}

SAFE_DISAMBIGUATING_ALIASES = {
    "벽돌 아이템": "brick",
    "벽돌 블록": "bricks",
    "네더 벽돌 아이템": "nether_brick",
    "네더 벽돌 블록": "nether_bricks",
    "우유": "milk",
    "옥스아이 데이지 오타": "oxeye_dasiy",
}

FORBIDDEN_ALIASES = {
    "잡템",
    "갑옷",
    "Steve에게",
    "deposit",
    "bare_deposit",
}


def main() -> None:
    asset_index_path, lang_object, lang_payload = _load_official_ko_kr()
    catalog_targets = _load_catalog_targets()
    catalog_set = set(catalog_targets)
    canonicalization = {
        target: canonical
        for target, (_lang_key, canonical) in LEGACY_LANG_TARGETS.items()
        if canonical and canonical in catalog_set
    }

    policy_targets: dict[str, dict[str, Any]] = {}
    display_names: dict[str, str] = {}
    for target in catalog_targets:
        policy_row, display_name = _classify_target(
            target,
            lang_payload,
            canonicalization,
        )
        policy_targets[target] = policy_row
        display_names[target] = display_name

    aliases = _build_aliases(catalog_targets, policy_targets, display_names)
    policy = {
        "schema_version": 1,
        "minecraft_version": "1.20.1",
        "catalog_target_count": len(catalog_targets),
        "source": {
            "catalog_path": "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/CataloguedResources.txt",
            "catalog_sha256": _sha256(CATALOG_PATH.read_bytes()),
            "minecraft_asset_index": asset_index_path.name,
            "official_korean_lang_object_sha1": lang_object["hash"],
            "official_korean_lang_sha256": _sha256(_lang_object_bytes(asset_index_path, lang_object)),
        },
        "classification_counts": dict(
            sorted(collections.Counter(
                row["classification"] for row in policy_targets.values()
            ).items())
        ),
        "targets": dict(sorted(policy_targets.items())),
    }
    canonicalization_payload = {
        "schema_version": 1,
        "minecraft_version": "1.20.1",
        "canonicalization": dict(sorted(canonicalization.items())),
    }

    _write_json("korean_item_aliases.json", dict(sorted(aliases.items())))
    _write_json("korean_item_display_names.json", dict(sorted(display_names.items())))
    _write_json("chatclef_item_command_target_policy.json", policy)
    _write_json("chatclef_target_canonicalization.json", canonicalization_payload)


def _load_official_ko_kr() -> tuple[Path, dict[str, Any], dict[str, str]]:
    for asset_index_path in ASSET_INDEX_CANDIDATES:
        if not asset_index_path.exists():
            continue
        asset_index = json.loads(asset_index_path.read_text(encoding="utf-8"))
        lang_object = asset_index["objects"].get("minecraft/lang/ko_kr.json")
        if not isinstance(lang_object, dict):
            continue
        payload = json.loads(
            _lang_object_bytes(asset_index_path, lang_object).decode("utf-8")
        )
        return asset_index_path, lang_object, {str(k): str(v) for k, v in payload.items()}
    raise FileNotFoundError("minecraft/lang/ko_kr.json was not found in local assets")


def _lang_object_bytes(asset_index_path: Path, lang_object: dict[str, Any]) -> bytes:
    lang_hash = str(lang_object["hash"])
    lang_path = asset_index_path.parents[1] / "objects" / lang_hash[:2] / lang_hash
    return lang_path.read_bytes()


def _load_catalog_targets() -> list[str]:
    targets = [
        line.strip()
        for line in CATALOG_PATH.read_text(encoding="utf-8").splitlines()
        if line.strip() and re.fullmatch(r"[a-z0-9_]+", line.strip())
    ]
    if len(targets) != CATALOG_TARGET_COUNT:
        raise ValueError(f"unexpected ChatClef catalog target count: {len(targets)}")
    if len(targets) != len(set(targets)):
        raise ValueError("ChatClef catalog target duplicate detected")
    return targets


def _classify_target(
    target: str,
    lang_payload: dict[str, str],
    canonicalization: dict[str, str],
) -> tuple[dict[str, Any], str]:
    item_key = f"item.minecraft.{target}"
    block_key = f"block.minecraft.{target}"
    item_display = lang_payload.get(item_key)
    block_display = lang_payload.get(block_key)
    if item_display or block_display:
        lang_key = item_key if item_display else block_key
        display_name = item_display or block_display or target
        row = {
            "classification": "DIRECT_ITEM" if item_display else "DIRECT_BLOCK",
            "display_name": display_name,
            "input_alias_policy": "official_unique_alias_when_unambiguous",
            "lang_key": lang_key,
        }
        return row, display_name
    if target in LEGACY_LANG_TARGETS:
        lang_key, canonical_target = LEGACY_LANG_TARGETS[target]
        display_name = lang_payload[lang_key]
        row = {
            "classification": "CANONICALIZED_LEGACY",
            "display_name": display_name,
            "input_alias_policy": (
                "display_only_when_canonical_alias_exists"
                if canonical_target
                else "display_only"
            ),
            "lang_key": lang_key,
        }
        if target in canonicalization:
            row["canonical_target"] = canonicalization[target]
        return row, display_name
    if target in GENERIC_DISPLAYS:
        return {
            "classification": "GENERIC_GROUP",
            "display_name": GENERIC_DISPLAYS[target],
            "input_alias_policy": "curated_only",
            "lang_key": None,
        }, GENERIC_DISPLAYS[target]
    if target in UNSUPPORTED_DISPLAYS:
        return {
            "classification": "UNSUPPORTED",
            "display_name": UNSUPPORTED_DISPLAYS[target],
            "input_alias_policy": "no_public_alias",
            "lang_key": None,
        }, UNSUPPORTED_DISPLAYS[target]
    raise ValueError(f"unclassified ChatClef target: {target}")


def _build_aliases(
    catalog_targets: list[str],
    policy_targets: dict[str, dict[str, Any]],
    display_names: dict[str, str],
) -> dict[str, str]:
    aliases = dict(CURATED_ALIASES)
    display_to_targets: dict[str, list[str]] = collections.defaultdict(list)
    for target in catalog_targets:
        if policy_targets[target]["classification"] in {"DIRECT_ITEM", "DIRECT_BLOCK"}:
            display_to_targets[display_names[target]].append(target)
    for display_name, targets in sorted(display_to_targets.items()):
        if len(targets) == 1:
            aliases.setdefault(display_name, targets[0])
    aliases.update(SAFE_DISAMBIGUATING_ALIASES)
    for forbidden in FORBIDDEN_ALIASES:
        aliases.pop(forbidden, None)
    _validate_aliases(aliases, set(catalog_targets))
    return aliases


def _validate_aliases(aliases: dict[str, str], catalog_targets: set[str]) -> None:
    compact_to_target: dict[str, str] = {}
    for alias, target in aliases.items():
        if target not in catalog_targets:
            raise ValueError(f"alias target outside ChatClef catalog: {alias} -> {target}")
        compact = re.sub(r"\s+", "", alias).lower()
        previous = compact_to_target.setdefault(compact, target)
        if previous != target:
            raise ValueError(
                f"compact alias conflict for {alias}: {previous} vs {target}"
            )


def _write_json(file_name: str, payload: Any) -> None:
    path = RESOURCE_DIR / file_name
    path.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )


def _sha256(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest().upper()


if __name__ == "__main__":
    main()
