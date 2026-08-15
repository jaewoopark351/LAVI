#20260815_kpopmodder: Lock Korean alias generation and runtime alias contracts.
from __future__ import annotations

import re
import unittest

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


if __name__ == "__main__":
    unittest.main()
