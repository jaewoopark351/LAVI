# 20260901_kpopmodder: Bind sealed P1 fixture observations to exact run and fixture manifests.
from __future__ import annotations

import unittest
from dataclasses import replace
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch

from ...scenario.matrix_catalog import automatic_deposit_matrix_catalog
from ...testing.hermetic_evidence import hermetic_fixture, hermetic_run_manifest
from ..fixture_manifest_fingerprint import automatic_deposit_fixture_fingerprint
from . import p1_live_fixture_observer
from .p1_live_fixture_binding_contract import verify_p1_live_fixture_binding
from .p1_live_fixture_observation import (
    P1LiveFixtureObservation,
    _create_p1_live_fixture_observation,
)


class P1LiveFixtureBindingContractTests(unittest.TestCase):
    def test_private_factory_ready_observation_binds_exactly(self):
        observation = _ready_observation()
        run_manifest, fixture_manifest = _matching_manifests(observation)

        result = verify_p1_live_fixture_binding(
            observation,
            run_manifest,
            fixture_manifest,
        )

        self.assertTrue(result.ok, result.errors)
        self.assertTrue(result.identity_bound)
        self.assertTrue(result.ready)
        self.assertEqual("P1_LIVE_FIXTURE_BOUND_AND_READY", result.reason)
        self.assertIs(observation, result.observation)
        self.assertEqual((), result.identity_errors)
        self.assertEqual((), result.readiness_errors)

    def test_public_observer_identity_is_preserved_but_save_barrier_is_not_ready(self):
        world = Path("C:/minecraft/instance/saves/P1 fixture")
        with (
            patch.object(
                p1_live_fixture_observer,
                "read_saved_world_identity",
                return_value=(_world_identity(), "SAVED_WORLD_IDENTITY_OBSERVED"),
            ),
            patch.object(
                p1_live_fixture_observer,
                "read_exact_trusted_destination",
                return_value=(_trusted_destination(), "TRUSTED_DESTINATION_OBSERVED"),
            ),
            patch.object(
                p1_live_fixture_observer,
                "observe_saved_player_inventory",
                return_value=(_player_inventory(), "PLAYER_INVENTORY_OBSERVED"),
            ),
            patch.object(
                p1_live_fixture_observer,
                "read_stable_anvil_chunk",
                return_value=(_chunk(), "ANVIL_CHUNK_OBSERVED"),
            ),
            patch.object(
                p1_live_fixture_observer,
                "collect_trusted_container_inventory_snapshot",
                return_value=(_container(), "TRUSTED_CONTAINER_INVENTORY_OBSERVED"),
            ),
        ):
            observation, observer_reason = (
                p1_live_fixture_observer.observe_p1_live_fixture(
                    world_directory=world,
                    trusted_destinations_path=Path(
                        "C:/minecraft/instance/config/lavi/trusted.json"
                    ),
                    player_data_path=world / "playerdata" / "fixture-player.dat",
                    expected_world_key="singleplayer:P1 fixture",
                    expected_dimension="OVERWORLD",
                    running_world_key="singleplayer:P1 fixture",
                )
            )
        self.assertEqual("P1_LIVE_FIXTURE_OBSERVED_INCONCLUSIVE", observer_reason)
        self.assertIsInstance(observation, P1LiveFixtureObservation)
        assert observation is not None
        run_manifest, fixture_manifest = _matching_manifests(observation)

        result = verify_p1_live_fixture_binding(
            observation,
            run_manifest,
            fixture_manifest,
        )

        self.assertFalse(result.ok)
        self.assertTrue(result.identity_bound)
        self.assertFalse(result.ready)
        self.assertEqual("P1_LIVE_FIXTURE_NOT_READY", result.reason)
        self.assertIs(observation, result.observation)
        self.assertEqual(
            observation.observation_fingerprint,
            result.observation_fingerprint,
        )
        self.assertEqual((), result.identity_errors)
        self.assertEqual(
            (
                "P1_LIVE_FIXTURE_VERDICT_INCONCLUSIVE",
                "P1_LIVE_FIXTURE_LIMITATIONS_PRESENT",
            ),
            result.readiness_errors,
        )
        self.assertEqual(
            ("RUNNING_JVM_SAVE_BARRIER_NOT_AVAILABLE",),
            result.limitations,
        )

    def test_rejects_untyped_observation_and_manifest_inputs(self):
        observation = _ready_observation()
        run_manifest, fixture_manifest = _matching_manifests(observation)
        cases = (
            (
                None,
                run_manifest,
                fixture_manifest,
                "P1_LIVE_FIXTURE_OBSERVATION_NOT_TYPED",
            ),
            (
                observation,
                None,
                fixture_manifest,
                "P1_LIVE_FIXTURE_RUN_MANIFEST_NOT_TYPED",
            ),
            (
                observation,
                run_manifest,
                None,
                "P1_LIVE_FIXTURE_MANIFEST_NOT_TYPED",
            ),
        )
        for observed, run, fixture, expected_error in cases:
            with self.subTest(expected_error=expected_error):
                result = verify_p1_live_fixture_binding(observed, run, fixture)

                self.assertFalse(result.ok)
                self.assertFalse(result.identity_bound)
                self.assertIn(expected_error, result.identity_errors)

    def test_rejects_every_exact_identity_mismatch(self):
        observation = _ready_observation()
        run_manifest, fixture_manifest = _matching_manifests(observation)
        cases = (
            (
                _ready_observation(schema_version="p1-live-fixture-observation/v2"),
                run_manifest,
                fixture_manifest,
                "P1_LIVE_FIXTURE_SCHEMA_VERSION_INVALID",
            ),
            (
                observation,
                replace(run_manifest, world="singleplayer:other"),
                fixture_manifest,
                "P1_LIVE_FIXTURE_RUN_WORLD_MISMATCH",
            ),
            (
                observation,
                replace(run_manifest, fixture_fingerprint="f" * 64),
                fixture_manifest,
                "P1_LIVE_FIXTURE_RUN_FIXTURE_FINGERPRINT_MISMATCH",
            ),
            (
                observation,
                run_manifest,
                replace(fixture_manifest, world_snapshot_id="f" * 64),
                "P1_LIVE_FIXTURE_WORLD_SNAPSHOT_MISMATCH",
            ),
            (
                observation,
                run_manifest,
                replace(fixture_manifest, inventory_snapshot_id="f" * 64),
                "P1_LIVE_FIXTURE_INVENTORY_SNAPSHOT_MISMATCH",
            ),
            (
                observation,
                run_manifest,
                replace(fixture_manifest, trusted_destination_fingerprint="f" * 64),
                "P1_LIVE_FIXTURE_TRUSTED_DESTINATION_FINGERPRINT_MISMATCH",
            ),
            (
                observation,
                run_manifest,
                _replace_attribute(
                    fixture_manifest,
                    "trusted_container_position",
                    "13, 64, -9",
                ),
                "P1_LIVE_FIXTURE_TRUSTED_POSITION_MISMATCH",
            ),
            (
                observation,
                run_manifest,
                replace(fixture_manifest, container_type="BARREL"),
                "P1_LIVE_FIXTURE_CONTAINER_TYPE_MISMATCH",
            ),
            (
                observation,
                run_manifest,
                _replace_attribute(fixture_manifest, "trusted_binding_count", "2"),
                "P1_LIVE_FIXTURE_TRUSTED_BINDING_COUNT_MISMATCH",
            ),
        )
        for observed, run, fixture, expected_error in cases:
            with self.subTest(expected_error=expected_error):
                result = verify_p1_live_fixture_binding(observed, run, fixture)

                self.assertFalse(result.ok)
                self.assertFalse(result.identity_bound)
                self.assertIn(expected_error, result.identity_errors)

    def test_capacity_and_each_readiness_signal_fail_closed_after_identity_binding(
        self,
    ):
        baseline = _ready_observation()
        run_manifest, fixture_manifest = _matching_manifests(baseline)
        cases = (
            (
                _ready_observation(container_has_capacity=False),
                "P1_LIVE_FIXTURE_CONTAINER_HAS_NO_CAPACITY",
            ),
            (
                _ready_observation(verdict="INCONCLUSIVE", reason="not-ready"),
                "P1_LIVE_FIXTURE_VERDICT_INCONCLUSIVE",
            ),
            (
                _ready_observation(limitations=("UNVERIFIED_SAVE_BARRIER",)),
                "P1_LIVE_FIXTURE_LIMITATIONS_PRESENT",
            ),
        )
        for observation, expected_error in cases:
            with self.subTest(expected_error=expected_error):
                result = verify_p1_live_fixture_binding(
                    observation,
                    run_manifest,
                    fixture_manifest,
                )

                self.assertFalse(result.ok)
                self.assertTrue(result.identity_bound)
                self.assertFalse(result.ready)
                self.assertIs(observation, result.observation)
                self.assertIn(expected_error, result.readiness_errors)


def _matching_manifests(observation: P1LiveFixtureObservation):
    row = next(row for row in automatic_deposit_matrix_catalog() if row.row_id == "P1")
    base = hermetic_fixture(row)
    observed = observation.as_fixture_evidence_mapping()
    attributes = tuple(
        (
            key,
            str(observed[key]) if key in observed else value,
        )
        for key, value in base.attributes
    )
    provisional = replace(
        base,
        fixture_fingerprint="0" * 64,
        world_snapshot_id=observation.world_snapshot_fingerprint,
        inventory_snapshot_id=observation.player_inventory_fingerprint,
        container_type=str(observed["container_type"]),
        trusted_destination_fingerprint=observation.trusted_destination_fingerprint,
        attributes=attributes,
    )
    fixture = replace(
        provisional,
        fixture_fingerprint=automatic_deposit_fixture_fingerprint(provisional),
    )
    run = replace(
        hermetic_run_manifest(row, fixture),
        world=observation.world_key,
    )
    return run, fixture


def _replace_attribute(fixture, key: str, value: str):
    return replace(
        fixture,
        attributes=tuple(
            (name, value if name == key else existing)
            for name, existing in fixture.attributes
        ),
    )


def _ready_observation(**overrides: object) -> P1LiveFixtureObservation:
    values: dict[str, object] = {
        "schema_version": "p1-live-fixture-observation/v1",
        "verdict": "PASS",
        "reason": "P1_LIVE_FIXTURE_READY",
        "world_directory": "C:/minecraft/instance/saves/P1 fixture",
        "world_key": "singleplayer:P1 fixture",
        "dimension": "OVERWORLD",
        "trusted_position": (12, 64, -9),
        "trusted_destination_id": "td_ready",
        "trusted_destination_fingerprint": "e" * 64,
        "container_type": "minecraft:chest",
        "container_capacity_slots": 27,
        "container_occupied_slots": 1,
        "container_empty_slots": 26,
        "container_has_capacity": True,
        "container_inventory_fingerprint": "c" * 64,
        "player_inventory_counts": (("minecraft:cobblestone", 32),),
        "player_inventory_fingerprint": "b" * 64,
        "level_dat_path": "C:/minecraft/instance/saves/P1 fixture/level.dat",
        "level_dat_sha256": "1" * 64,
        "level_dat_size": 128,
        "level_dat_mtime_ns": 10,
        "player_data_path": "C:/minecraft/instance/saves/P1 fixture/playerdata/player.dat",
        "player_data_sha256": "2" * 64,
        "player_data_size": 256,
        "player_data_mtime_ns": 11,
        "trusted_registry_path": "C:/minecraft/instance/config/lavi/trusted.json",
        "trusted_registry_sha256": "3" * 64,
        "trusted_registry_size": 512,
        "trusted_registry_mtime_ns": 12,
        "region_path": "C:/minecraft/instance/saves/P1 fixture/region/r.0.-1.mca",
        "region_sha256": "4" * 64,
        "region_size": 8192,
        "region_mtime_ns": 13,
        "region_chunk_timestamp": 1_777_777_777,
        "world_snapshot_fingerprint": "a" * 64,
        "limitations": (),
    }
    values.update(overrides)
    return _create_p1_live_fixture_observation(**values)


def _world_identity() -> SimpleNamespace:
    return SimpleNamespace(
        world_key="singleplayer:P1 fixture",
        level_dat_path="C:/minecraft/instance/saves/P1 fixture/level.dat",
        level_dat_sha256="1" * 64,
        level_dat_size=128,
        level_dat_mtime_ns=10,
    )


def _trusted_destination() -> SimpleNamespace:
    return SimpleNamespace(
        world_key="singleplayer:P1 fixture",
        dimension="OVERWORLD",
        position=(12, 64, -9),
        destination_id="td_public",
        destination_fingerprint="e" * 64,
        registry_path="C:/minecraft/instance/config/lavi/trusted.json",
        registry_sha256="3" * 64,
        registry_size=512,
        registry_mtime_ns=12,
    )


def _player_inventory() -> SimpleNamespace:
    return SimpleNamespace(
        player_data_path=(
            "C:/minecraft/instance/saves/P1 fixture/playerdata/fixture-player.dat"
        ),
        player_data_sha256="2" * 64,
        player_data_size=256,
        player_data_mtime_ns=11,
        inventory_counts=(("minecraft:cobblestone", 32),),
        inventory_fingerprint="b" * 64,
    )


def _chunk() -> SimpleNamespace:
    return SimpleNamespace(
        region_path="C:/minecraft/instance/saves/P1 fixture/region/r.0.-1.mca",
        region_sha256="4" * 64,
        region_size=8192,
        region_mtime_ns=13,
        chunk_timestamp=1_777_777_777,
    )


def _container() -> SimpleNamespace:
    return SimpleNamespace(
        block_entity_id="minecraft:chest",
        capacity_slots=27,
        occupied_slots=1,
        empty_slots=26,
        has_capacity=True,
        inventory_fingerprint="c" * 64,
    )


if __name__ == "__main__":
    unittest.main()
