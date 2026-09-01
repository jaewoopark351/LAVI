#20260901_kpopmodder: Compose read-only P1 save evidence without claiming live JVM state.
from __future__ import annotations

from pathlib import Path
from types import MappingProxyType

from .anvil_chunk_reader import read_stable_anvil_chunk
from .container_inventory_snapshot_collector import (
    collect_trusted_container_inventory_snapshot,
)
from .p1_live_fixture_observation import (
    P1LiveFixtureObservation,
    _create_p1_live_fixture_observation,
    p1_world_snapshot_fingerprint,
)
from .player_inventory_observer import observe_saved_player_inventory
from .trusted_destination_file_reader import read_exact_trusted_destination
from .world_identity_reader import read_saved_world_identity


_DIMENSION_REGION_DIRECTORIES = MappingProxyType(
    {
        "OVERWORLD": ("region",),
        "NETHER": ("DIM-1", "region"),
        "END": ("DIM1", "region"),
    }
)


def observe_p1_live_fixture(
    *,
    world_directory: Path,
    trusted_destinations_path: Path,
    player_data_path: Path,
    expected_world_key: str,
    expected_dimension: str,
    running_world_key: str | None,
    minimum_saved_state_mtime_ns: int | None = None,
) -> tuple[P1LiveFixtureObservation | None, str]:
    path_problem = _path_problem(
        world_directory, trusted_destinations_path, player_data_path
    )
    if path_problem:
        return None, path_problem
    if expected_dimension not in _DIMENSION_REGION_DIRECTORIES:
        return None, "P1_EXPECTED_DIMENSION_INVALID"
    if running_world_key is not None:
        if not isinstance(running_world_key, str) or not running_world_key.strip():
            return None, "P1_RUNNING_WORLD_IDENTITY_INVALID"
        if running_world_key != expected_world_key:
            return None, "P1_RUNNING_WORLD_IDENTITY_MISMATCH"
    if minimum_saved_state_mtime_ns is not None and (
        isinstance(minimum_saved_state_mtime_ns, bool)
        or not isinstance(minimum_saved_state_mtime_ns, int)
        or minimum_saved_state_mtime_ns < 0
    ):
        return None, "P1_MINIMUM_SAVED_STATE_MTIME_INVALID"

    world = world_directory.resolve(strict=False)
    player_path = player_data_path.resolve(strict=False)
    if not _is_direct_player_data_path(world, player_path):
        return None, "P1_PLAYER_DATA_PATH_OUTSIDE_WORLD"

    world_identity, world_reason = read_saved_world_identity(
        world / "level.dat",
        expected_world_key=expected_world_key,
    )
    if world_identity is None:
        return None, f"P1_{world_reason}"
    destination, destination_reason = read_exact_trusted_destination(
        trusted_destinations_path,
        expected_world_key=expected_world_key,
        expected_dimension=expected_dimension,
    )
    if destination is None:
        return None, f"P1_{destination_reason}"
    player, player_reason = observe_saved_player_inventory(
        player_path,
        minimum_mtime_ns=minimum_saved_state_mtime_ns,
    )
    if player is None:
        return None, f"P1_{player_reason}"

    block_x, _block_y, block_z = destination.position
    chunk_x = block_x >> 4
    chunk_z = block_z >> 4
    region_x = chunk_x >> 5
    region_z = chunk_z >> 5
    region_directory = world.joinpath(
        *_DIMENSION_REGION_DIRECTORIES[expected_dimension]
    )
    region_path = region_directory / f"r.{region_x}.{region_z}.mca"
    chunk, chunk_reason = read_stable_anvil_chunk(
        region_path,
        block_position=destination.position,
        minimum_mtime_ns=minimum_saved_state_mtime_ns,
    )
    if chunk is None:
        return None, f"P1_{chunk_reason}"
    container, container_reason = collect_trusted_container_inventory_snapshot(
        chunk,
        expected_position=destination.position,
    )
    if container is None:
        return None, f"P1_{container_reason}"

    limitations: list[str] = []
    if running_world_key is None:
        limitations.append("RUNNING_WORLD_IDENTITY_NOT_OBSERVED")
    limitations.append("RUNNING_JVM_SAVE_BARRIER_NOT_AVAILABLE")
    values: dict[str, object] = {
        "schema_version": "p1-live-fixture-observation/v1",
        "verdict": "INCONCLUSIVE",
        "reason": "RUNNING_JVM_SAVE_BARRIER_NOT_AVAILABLE",
        "world_directory": str(world),
        "world_key": world_identity.world_key,
        "dimension": expected_dimension,
        "trusted_position": destination.position,
        "trusted_destination_id": destination.destination_id,
        "trusted_destination_fingerprint": destination.destination_fingerprint,
        "container_type": container.block_entity_id,
        "container_capacity_slots": container.capacity_slots,
        "container_occupied_slots": container.occupied_slots,
        "container_empty_slots": container.empty_slots,
        "container_has_capacity": container.has_capacity,
        "container_inventory_fingerprint": container.inventory_fingerprint,
        "player_inventory_counts": player.inventory_counts,
        "player_inventory_fingerprint": player.inventory_fingerprint,
        "level_dat_path": world_identity.level_dat_path,
        "level_dat_sha256": world_identity.level_dat_sha256,
        "level_dat_size": world_identity.level_dat_size,
        "level_dat_mtime_ns": world_identity.level_dat_mtime_ns,
        "player_data_path": player.player_data_path,
        "player_data_sha256": player.player_data_sha256,
        "player_data_size": player.player_data_size,
        "player_data_mtime_ns": player.player_data_mtime_ns,
        "trusted_registry_path": destination.registry_path,
        "trusted_registry_sha256": destination.registry_sha256,
        "trusted_registry_size": destination.registry_size,
        "trusted_registry_mtime_ns": destination.registry_mtime_ns,
        "region_path": chunk.region_path,
        "region_sha256": chunk.region_sha256,
        "region_size": chunk.region_size,
        "region_mtime_ns": chunk.region_mtime_ns,
        "region_chunk_timestamp": chunk.chunk_timestamp,
        "world_snapshot_fingerprint": p1_world_snapshot_fingerprint(
            world_key=world_identity.world_key,
            dimension=expected_dimension,
            level_dat_sha256=world_identity.level_dat_sha256,
            region_sha256=chunk.region_sha256,
            region_chunk_timestamp=chunk.chunk_timestamp,
        ),
        "limitations": tuple(limitations),
    }
    return (
        _create_p1_live_fixture_observation(**values),
        "P1_LIVE_FIXTURE_OBSERVED_INCONCLUSIVE",
    )


def _path_problem(
    world_directory: object,
    trusted_destinations_path: object,
    player_data_path: object,
) -> str:
    for label, value in (
        ("WORLD_DIRECTORY", world_directory),
        ("TRUSTED_DESTINATIONS", trusted_destinations_path),
        ("PLAYER_DATA", player_data_path),
    ):
        if not isinstance(value, Path) or not value.is_absolute():
            return f"P1_{label}_PATH_NOT_ABSOLUTE"
    return ""


def _is_direct_player_data_path(world: Path, player_path: Path) -> bool:
    try:
        relative = player_path.relative_to(world / "playerdata")
    except ValueError:
        return False
    return len(relative.parts) == 1 and player_path.suffix.casefold() == ".dat"
