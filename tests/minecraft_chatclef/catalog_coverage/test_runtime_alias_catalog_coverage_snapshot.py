#20260815_kpopmodder: Expose runtime Korean alias coverage against ChatClef catalog.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.intent.chatclef_alias_repository import (
    ChatClefKoreanAliasRepository,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_target_catalog import (
    ChatClefTargetCatalog,
)


PSEUDO_TARGETS = {
    "all",
    "armor_set",
    "bare_deposit",
    "deposit_all_non_gear",
    "full_armor_set_shortcut",
    "player_name",
}
CONTEXTUAL_ALIAS_PHRASES = {
    "잡템",
    "갑옷",
    "철 갑옷",
    "금 갑옷",
    "다이아몬드 갑옷",
    "Steve에게",
}


class RuntimeAliasCatalogCoverageSnapshotTests(unittest.TestCase):
    def test_catalog_count_matches_current_chatclef_baseline(self):
        catalog = ChatClefTargetCatalog()

        self.assertEqual(591, len(catalog.targets))

    def test_runtime_alias_coverage_is_visible_and_not_full_catalog_claim(self):
        catalog_targets = set(ChatClefTargetCatalog().targets)
        aliases = ChatClefKoreanAliasRepository()
        alias_targets = set(aliases.fixed_item_aliases.values())
        covered_targets = alias_targets & catalog_targets
        missing_targets = catalog_targets - alias_targets

        self.assertGreater(len(aliases.fixed_item_aliases), 0)
        self.assertGreater(len(catalog_targets), len(aliases.fixed_item_aliases))
        self.assertGreater(len(missing_targets), 0)
        self.assertLess(len(covered_targets), len(catalog_targets))

    def test_every_runtime_alias_target_exists_in_catalog(self):
        catalog = ChatClefTargetCatalog()
        aliases = ChatClefKoreanAliasRepository()

        missing = sorted(
            target
            for target in set(aliases.fixed_item_aliases.values())
            if not catalog.contains(target)
        )

        self.assertEqual([], missing)

    def test_runtime_alias_file_does_not_include_command_policy_pseudo_targets(self):
        aliases = ChatClefKoreanAliasRepository()

        self.assertTrue(
            PSEUDO_TARGETS.isdisjoint(set(aliases.fixed_item_aliases.values()))
        )

    def test_runtime_alias_file_does_not_flatten_contextual_command_phrases(self):
        aliases = ChatClefKoreanAliasRepository()

        self.assertTrue(CONTEXTUAL_ALIAS_PHRASES.isdisjoint(aliases.fixed_item_aliases))


if __name__ == "__main__":
    unittest.main()
