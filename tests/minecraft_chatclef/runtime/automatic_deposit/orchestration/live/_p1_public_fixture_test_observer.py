#20260901_kpopmodder: Exercise the public P1 fixture observer's save-barrier limit.
from __future__ import annotations

from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch

from ...evidence.live_fixture import p1_live_fixture_observer
from ...evidence.live_fixture.p1_live_fixture_observation import (
    P1LiveFixtureObservation,
)


def observe_public_inconclusive_fixture() -> tuple[
    P1LiveFixtureObservation | None,
    str,
]:
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
        return p1_live_fixture_observer.observe_p1_live_fixture(
            world_directory=world,
            trusted_destinations_path=Path(
                "C:/minecraft/instance/config/lavi/trusted.json"
            ),
            player_data_path=world / "playerdata" / "fixture-player.dat",
            expected_world_key="singleplayer:P1 fixture",
            expected_dimension="OVERWORLD",
            running_world_key="singleplayer:P1 fixture",
        )


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
