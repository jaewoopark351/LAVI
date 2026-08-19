#20260815_kpopmodder: Expose runtime Korean alias coverage against ChatClef catalog.
from __future__ import annotations

import hashlib
import json
import unittest
from pathlib import Path

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
REPO_ROOT = Path(__file__).resolve().parents[3]
ARTIFACT_ROOT = Path(__file__).resolve().parent
SNAPSHOT_ARTIFACT = ARTIFACT_ROOT / "runtime_alias_catalog_coverage.snapshot.json"
FLOOR_ARTIFACT = ARTIFACT_ROOT / "runtime_alias_catalog_coverage.floor.json"


class RuntimeAliasCatalogCoverageSnapshotTests(unittest.TestCase):
    def test_catalog_count_matches_current_chatclef_baseline(self):
        catalog = ChatClefTargetCatalog()

        self.assertEqual(591, len(catalog.targets))

    def test_runtime_alias_coverage_is_visible_and_not_full_catalog_claim(self):
        snapshot = _coverage_snapshot()
        actual = _actual_coverage()

        self.assertEqual(snapshot["catalog_target_count"], actual["catalog_target_count"])
        self.assertEqual(snapshot["alias_count"], actual["alias_count"])
        self.assertEqual(snapshot["covered_target_count"], actual["covered_target_count"])
        self.assertEqual(snapshot["missing_target_count"], actual["missing_target_count"])
        self.assertEqual(snapshot["coverage_percent"], actual["coverage_percent"])
        self.assertGreater(actual["missing_target_count"], 0)
        self.assertLess(
            actual["covered_target_count"],
            actual["catalog_target_count"],
        )

    def test_runtime_alias_coverage_floor_prevents_silent_alias_loss(self):
        floor = _coverage_floor()
        actual = _actual_coverage()

        self.assertGreaterEqual(actual["alias_count"], floor["minimum_alias_count"])
        self.assertGreaterEqual(
            actual["covered_target_count"],
            floor["minimum_covered_target_count"],
        )
        self.assertGreaterEqual(
            actual["coverage_percent"],
            floor["minimum_coverage_percent"],
        )

    def test_coverage_snapshot_source_hashes_match_working_tree_sources(self):
        snapshot = _coverage_snapshot()

        for path_key, hash_key in [
            ("catalog_path", "catalog_sha256"),
            ("alias_source_path", "alias_source_sha256"),
            ("catalog_parser_source_path", "catalog_parser_source_sha256"),
        ]:
            with self.subTest(path=snapshot[path_key]):
                actual_hash = hashlib.sha256(
                    (REPO_ROOT / str(snapshot[path_key])).read_bytes()
                ).hexdigest()

                self.assertEqual(snapshot[hash_key], actual_hash)

    def test_baseline_required_targets_are_present(self):
        catalog = ChatClefTargetCatalog()
        snapshot = _coverage_snapshot()

        missing = sorted(
            target
            for target in snapshot["baseline_required_targets"]
            if not catalog.contains(target)
        )

        self.assertEqual([], missing)

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


def _actual_coverage() -> dict[str, int | float]:
    catalog_targets = set(ChatClefTargetCatalog().targets)
    aliases = ChatClefKoreanAliasRepository()
    alias_targets = set(aliases.fixed_item_aliases.values())
    covered_targets = alias_targets & catalog_targets
    missing_targets = catalog_targets - alias_targets
    coverage_percent = round(len(covered_targets) * 100 / len(catalog_targets), 2)
    return {
        "catalog_target_count": len(catalog_targets),
        "alias_count": len(aliases.fixed_item_aliases),
        "covered_target_count": len(covered_targets),
        "missing_target_count": len(missing_targets),
        "coverage_percent": coverage_percent,
    }


def _coverage_snapshot() -> dict[str, object]:
    return json.loads(SNAPSHOT_ARTIFACT.read_text(encoding="utf-8"))


def _coverage_floor() -> dict[str, object]:
    return json.loads(FLOOR_ARTIFACT.read_text(encoding="utf-8"))

if __name__ == "__main__":
    unittest.main()
