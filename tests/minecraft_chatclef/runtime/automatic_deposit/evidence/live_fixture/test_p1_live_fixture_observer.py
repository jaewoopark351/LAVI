#20260901_kpopmodder: Verify one sealed read-only P1 fixture observation stays inconclusive without a save barrier.
from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from ._fixture_builders import (
    chunk_nbt,
    container_block_entity,
    trusted_destination,
    write_level_dat,
    write_player_dat,
    write_region_file,
    write_trusted_destinations,
)
from .p1_live_fixture_observation import P1LiveFixtureObservation
from .p1_live_fixture_observer import observe_p1_live_fixture


class P1LiveFixtureObserverTests(unittest.TestCase):
    def test_offline_save_evidence_is_sealed_and_explicitly_inconclusive(self):
        with tempfile.TemporaryDirectory() as directory:
            paths = self._fixture(Path(directory))

            observation, reason = observe_p1_live_fixture(
                world_directory=paths["world"],
                trusted_destinations_path=paths["trusted"],
                player_data_path=paths["player"],
                expected_world_key="singleplayer:P1 fixture",
                expected_dimension="OVERWORLD",
                running_world_key="singleplayer:P1 fixture",
            )

            self.assertEqual("P1_LIVE_FIXTURE_OBSERVED_INCONCLUSIVE", reason)
            self.assertIsNotNone(observation)
            assert observation is not None
            self.assertEqual("INCONCLUSIVE", observation.verdict)
            self.assertEqual((12, 64, -9), observation.trusted_position)
            self.assertEqual("singleplayer:P1 fixture", observation.world_key)
            self.assertEqual("minecraft:barrel", observation.container_type)
            self.assertTrue(observation.container_has_capacity)
            self.assertEqual(
                ("RUNNING_JVM_SAVE_BARRIER_NOT_AVAILABLE",),
                observation.limitations,
            )
            for value in (
                observation.level_dat_sha256,
                observation.player_data_sha256,
                observation.trusted_registry_sha256,
                observation.region_sha256,
                observation.observation_fingerprint,
            ):
                self.assertEqual(64, len(value))
            with self.assertRaises(ValueError):
                P1LiveFixtureObservation(
                    **{
                        field: getattr(observation, field)
                        for field in observation.__dataclass_fields__
                        if not field.startswith("_")
                    }
                )

    def test_runtime_world_mismatch_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            paths = self._fixture(Path(directory))

            observation, reason = observe_p1_live_fixture(
                world_directory=paths["world"],
                trusted_destinations_path=paths["trusted"],
                player_data_path=paths["player"],
                expected_world_key="singleplayer:P1 fixture",
                expected_dimension="OVERWORLD",
                running_world_key="singleplayer:other",
            )

            self.assertIsNone(observation)
            self.assertEqual("P1_RUNNING_WORLD_IDENTITY_MISMATCH", reason)

    def test_missing_runtime_link_and_stale_save_remain_inconclusive(self):
        with tempfile.TemporaryDirectory() as directory:
            paths = self._fixture(Path(directory))
            observation, reason = observe_p1_live_fixture(
                world_directory=paths["world"],
                trusted_destinations_path=paths["trusted"],
                player_data_path=paths["player"],
                expected_world_key="singleplayer:P1 fixture",
                expected_dimension="OVERWORLD",
                running_world_key=None,
            )
            self.assertEqual("P1_LIVE_FIXTURE_OBSERVED_INCONCLUSIVE", reason)
            assert observation is not None
            self.assertEqual(
                (
                    "RUNNING_WORLD_IDENTITY_NOT_OBSERVED",
                    "RUNNING_JVM_SAVE_BARRIER_NOT_AVAILABLE",
                ),
                observation.limitations,
            )

            newest = max(paths["player"].stat().st_mtime_ns, paths["region"].stat().st_mtime_ns)
            observation, reason = observe_p1_live_fixture(
                world_directory=paths["world"],
                trusted_destinations_path=paths["trusted"],
                player_data_path=paths["player"],
                expected_world_key="singleplayer:P1 fixture",
                expected_dimension="OVERWORLD",
                running_world_key="singleplayer:P1 fixture",
                minimum_saved_state_mtime_ns=newest + 1,
            )
            self.assertIsNone(observation)
            self.assertIn(reason, {"P1_PLAYER_DATA_STALE", "P1_ANVIL_REGION_STALE"})

    def _fixture(self, root: Path) -> dict[str, Path]:
        world = root / "saves" / "P1 fixture"
        trusted = root / "config" / "lavi" / "automatic-deposit-trusted-destinations.json"
        player = world / "playerdata" / "fixture-player.dat"
        region = world / "region" / "r.0.-1.mca"
        write_level_dat(world / "level.dat", "P1 fixture")
        write_player_dat(
            player,
            (
                (0, "minecraft:diamond_pickaxe", 1),
                (9, "minecraft:cobblestone", 32),
            ),
        )
        write_trusted_destinations(trusted, [trusted_destination()])
        write_region_file(
            region,
            chunk_x=0,
            chunk_z=-1,
            nbt_payload=chunk_nbt(
                chunk_x=0,
                chunk_z=-1,
                block_entities=(container_block_entity(),),
            ),
        )
        return {
            "world": world.resolve(),
            "trusted": trusted.resolve(),
            "player": player.resolve(),
            "region": region.resolve(),
        }
