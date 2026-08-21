#20260815_kpopmodder: Lock Korean alias generation and runtime alias contracts.
from __future__ import annotations

import json
import re
import unittest
from pathlib import Path

from plugins.Minecraft.fabric.chatclef.intent.chatclef_alias_repository import (
    ChatClefKoreanAliasRepository,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_target_catalog import (
    ChatClefTargetCatalog,
)
from plugins.Minecraft.fabric.chatclef.intent.korean_text_normalizer import (
    KoreanTextNormalizer,
)


PSEUDO_TARGETS = {
    "all",
    "armor_set",
    "player_name",
}
TARGET_RE = re.compile(r"^[a-z0-9_]+$")
DANGEROUS_SLOT_RE = re.compile(r"[;#\r\n\"'@]|[\x00-\x1f\x7f]")
RESOURCE_DIR = (
    Path(__file__).resolve().parents[1]
    / "plugins"
    / "Minecraft"
    / "fabric"
    / "chatclef"
    / "intent"
    / "resources"
)


class MinecraftChatClefKoreanAliasGenerationContractTests(unittest.TestCase):
    def test_fixed_item_alias_targets_exist_in_catalog(self):
        aliases = ChatClefKoreanAliasRepository()
        catalog = ChatClefTargetCatalog()

        missing = sorted(
            target
            for target in set(aliases.fixed_item_aliases.values())
            if not catalog.contains(target)
        )

        self.assertEqual([], missing)

    def test_fixed_item_aliases_do_not_contain_contextual_pseudo_targets(self):
        aliases = ChatClefKoreanAliasRepository()

        self.assertTrue(
            PSEUDO_TARGETS.isdisjoint(set(aliases.fixed_item_aliases.values()))
        )
        self.assertNotIn("잡템", aliases.fixed_item_aliases)
        self.assertNotIn("갑옷", aliases.fixed_item_aliases)
        self.assertNotIn("Steve에게", aliases.fixed_item_aliases)

    def test_fixed_item_aliases_have_no_compact_target_conflicts(self):
        aliases = ChatClefKoreanAliasRepository()
        normalizer = KoreanTextNormalizer()
        seen: dict[str, str] = {}
        conflicts: list[tuple[str, str, str]] = []

        for alias, target in aliases.fixed_item_aliases.items():
            compact = normalizer.normalize(alias).replace(" ", "")
            previous = seen.setdefault(compact, target)
            if previous != target:
                conflicts.append((compact, previous, target))

        self.assertEqual([], conflicts)

    def test_alias_and_target_slots_are_safe_for_compilation(self):
        aliases = ChatClefKoreanAliasRepository()

        for alias, target in aliases.fixed_item_aliases.items():
            with self.subTest(alias=alias, target=target):
                self.assertIsNone(DANGEROUS_SLOT_RE.search(alias))
                self.assertIsNone(DANGEROUS_SLOT_RE.search(target))
                self.assertRegex(target, TARGET_RE)

    def test_generator_and_runtime_normalization_golden_vectors_match(self):
        normalizer = KoreanTextNormalizer()

        vectors = {
            "철 주괴": "철주괴",
            "철주괴": "철주괴",
            "철  주괴": "철주괴",
            "철 주괴!": "철주괴",
        }

        for value, expected_compact in vectors.items():
            with self.subTest(value=value):
                self.assertEqual(
                    expected_compact,
                    normalizer.normalize(value).replace(" ", ""),
                )

    def test_wood_policy_keeps_ambiguous_lumber_out_of_fixed_item_aliases(self):
        aliases = ChatClefKoreanAliasRepository()

        self.assertNotIn("목재", aliases.fixed_item_aliases)
        self.assertEqual("log", aliases.fixed_item_aliases["나무"])
        self.assertEqual("log", aliases.fixed_item_aliases["원목"])
        self.assertEqual("log", aliases.fixed_item_aliases["통나무"])

    def test_official_korean_item_aliases_cover_direct_catalog_targets(self):
        aliases = ChatClefKoreanAliasRepository()

        self.assertEqual("oak_log", aliases.fixed_item_aliases["참나무 원목"])
        self.assertEqual("emerald", aliases.fixed_item_aliases["에메랄드"])
        self.assertEqual("torch", aliases.fixed_item_aliases["횃불"])
        self.assertEqual("brick", aliases.fixed_item_aliases["벽돌 아이템"])
        self.assertEqual("bricks", aliases.fixed_item_aliases["벽돌 블록"])
        self.assertNotIn("벽돌", aliases.fixed_item_aliases)

    def test_target_policy_classifies_every_chatclef_catalog_target(self):
        catalog = ChatClefTargetCatalog()
        policy = _load_json("chatclef_item_command_target_policy.json")
        target_policy = policy["targets"]

        self.assertEqual(591, policy["catalog_target_count"])
        self.assertEqual(591, len(target_policy))
        self.assertEqual(set(catalog.targets), set(target_policy))
        self.assertNotIn("UNRESOLVED", policy["classification_counts"])
        self.assertEqual(
            591,
            sum(int(value) for value in policy["classification_counts"].values()),
        )
        self.assertEqual(
            "60664E7CCFE4588950CD65CBA961CCCD2FE0B575086759DB4AAF689224FDFAAE",
            policy["source"]["official_korean_lang_sha256"],
        )

    def test_display_name_resource_has_one_entry_per_catalog_target(self):
        catalog = ChatClefTargetCatalog()
        display_names = _load_json("korean_item_display_names.json")

        self.assertEqual(set(catalog.targets), set(display_names))
        self.assertEqual("참나무 원목", display_names["oak_log"])
        self.assertEqual("철 흉갑", display_names["iron_chestplate"])


def _load_json(file_name: str) -> dict[str, object]:
    with (RESOURCE_DIR / file_name).open("r", encoding="utf-8") as handle:
        payload = json.load(handle)
    if not isinstance(payload, dict):
        raise TypeError(f"{file_name} must contain a JSON object")
    return payload


if __name__ == "__main__":
    unittest.main()
