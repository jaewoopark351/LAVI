#20260901_kpopmodder: Verify exact trusted block-entity inventory and capacity evidence.
from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from ._fixture_builders import (
    chunk_nbt,
    container_block_entity,
    named_string,
    write_region_file,
)
from .anvil_chunk_reader import read_stable_anvil_chunk
from .container_inventory_snapshot_collector import (
    collect_trusted_container_inventory_snapshot,
)


class ContainerInventorySnapshotTests(unittest.TestCase):
    def test_barrel_inventory_and_capacity_are_normalized(self):
        chunk = self._chunk(
            (
                container_block_entity(
                    items=(
                        (0, "minecraft:cobblestone", 32),
                        (4, "minecraft:diamond", 3),
                    )
                ),
            )
        )

        snapshot, reason = collect_trusted_container_inventory_snapshot(
            chunk, expected_position=(12, 64, -9)
        )

        self.assertEqual("TRUSTED_CONTAINER_INVENTORY_OBSERVED", reason)
        self.assertIsNotNone(snapshot)
        assert snapshot is not None
        self.assertEqual("minecraft:barrel", snapshot.block_entity_id)
        self.assertEqual(27, snapshot.capacity_slots)
        self.assertEqual(2, snapshot.occupied_slots)
        self.assertEqual(25, snapshot.empty_slots)
        self.assertTrue(snapshot.has_capacity)
        self.assertEqual(
            (("minecraft:cobblestone", 32), ("minecraft:diamond", 3)),
            snapshot.item_counts,
        )
        self.assertEqual(64, len(snapshot.inventory_fingerprint))

    def test_full_barrel_reports_no_saved_slot_capacity(self):
        chunk = self._chunk(
            (
                container_block_entity(
                    items=tuple(
                        (slot, "minecraft:cobblestone", 64)
                        for slot in range(27)
                    )
                ),
            )
        )

        snapshot, reason = collect_trusted_container_inventory_snapshot(
            chunk, expected_position=(12, 64, -9)
        )

        self.assertEqual("TRUSTED_CONTAINER_INVENTORY_OBSERVED", reason)
        assert snapshot is not None
        self.assertEqual(27, snapshot.occupied_slots)
        self.assertEqual(0, snapshot.empty_slots)
        self.assertFalse(snapshot.has_capacity)

    def test_wrong_or_duplicate_block_entity_is_rejected(self):
        wrong = self._chunk(
            (container_block_entity(block_entity_id="minecraft:furnace"),)
        )
        snapshot, reason = collect_trusted_container_inventory_snapshot(
            wrong, expected_position=(12, 64, -9)
        )
        self.assertIsNone(snapshot)
        self.assertEqual("TRUSTED_CONTAINER_BLOCK_ENTITY_UNSUPPORTED", reason)

        duplicate = self._chunk(
            (container_block_entity(), container_block_entity())
        )
        snapshot, reason = collect_trusted_container_inventory_snapshot(
            duplicate, expected_position=(12, 64, -9)
        )
        self.assertIsNone(snapshot)
        self.assertEqual("TRUSTED_CONTAINER_BLOCK_ENTITY_NOT_EXCLUSIVE", reason)

    def test_malformed_inventory_and_unresolved_loot_are_rejected(self):
        for entity, expected in (
            (
                container_block_entity(
                    items=(
                        (0, "minecraft:stone", 1),
                        (0, "minecraft:diamond", 1),
                    )
                ),
                "TRUSTED_CONTAINER_SLOT_DUPLICATE",
            ),
            (
                container_block_entity(
                    items=(),
                    extra_named_tags=(
                        named_string("LootTable", "minecraft:chests/simple_dungeon"),
                    ),
                ),
                "TRUSTED_CONTAINER_LOOT_TABLE_UNRESOLVED",
            ),
        ):
            with self.subTest(expected=expected):
                snapshot, reason = collect_trusted_container_inventory_snapshot(
                    self._chunk((entity,)), expected_position=(12, 64, -9)
                )
                self.assertIsNone(snapshot)
                self.assertEqual(expected, reason)

    def _chunk(self, entities: tuple[bytes, ...]):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        path = Path(temporary.name) / "r.0.-1.mca"
        write_region_file(
            path,
            chunk_x=0,
            chunk_z=-1,
            nbt_payload=chunk_nbt(
                chunk_x=0,
                chunk_z=-1,
                block_entities=entities,
            ),
        )
        chunk, reason = read_stable_anvil_chunk(
            path.resolve(), block_position=(12, 64, -9)
        )
        self.assertEqual("ANVIL_CHUNK_OBSERVED", reason)
        assert chunk is not None
        return chunk
