#20260901_kpopmodder: Seal one combined read-only P1 world/inventory/container observation.
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field


_P1_LIVE_FIXTURE_OBSERVATION_SEAL = object()


@dataclass(frozen=True, slots=True)
class P1LiveFixtureObservation:
    schema_version: str
    verdict: str
    reason: str
    world_directory: str
    world_key: str
    dimension: str
    trusted_position: tuple[int, int, int]
    trusted_destination_id: str
    trusted_destination_fingerprint: str
    container_type: str
    container_capacity_slots: int
    container_occupied_slots: int
    container_empty_slots: int
    container_has_capacity: bool
    container_inventory_fingerprint: str
    player_inventory_counts: tuple[tuple[str, int], ...]
    player_inventory_fingerprint: str
    level_dat_path: str
    level_dat_sha256: str
    level_dat_size: int
    level_dat_mtime_ns: int
    player_data_path: str
    player_data_sha256: str
    player_data_size: int
    player_data_mtime_ns: int
    trusted_registry_path: str
    trusted_registry_sha256: str
    trusted_registry_size: int
    trusted_registry_mtime_ns: int
    region_path: str
    region_sha256: str
    region_size: int
    region_mtime_ns: int
    region_chunk_timestamp: int
    world_snapshot_fingerprint: str
    limitations: tuple[str, ...]
    observation_fingerprint: str
    _seal: object = field(default=None, repr=False, compare=False)
    _integrity: str = field(default="", repr=False, compare=False)

    def __post_init__(self) -> None:
        expected = _observation_fingerprint(_public_values(self))
        if self._seal is not _P1_LIVE_FIXTURE_OBSERVATION_SEAL:
            raise ValueError("P1 live fixture observation must be observer-created")
        if self.observation_fingerprint != expected or self._integrity != expected:
            raise ValueError("P1 live fixture observation integrity mismatch")

    def as_fixture_evidence_mapping(self) -> dict[str, object]:
        x, y, z = self.trusted_position
        return {
            "fixture_observation_verdict": self.verdict,
            "world_snapshot_id": self.world_snapshot_fingerprint,
            "inventory_snapshot_id": self.player_inventory_fingerprint,
            "container_type": self.container_type.removeprefix("minecraft:").upper(),
            "trusted_destination_fingerprint": self.trusted_destination_fingerprint,
            "trusted_container_position": f"{x}, {y}, {z}",
            "trusted_binding_count": "1",
            "container_inventory_fingerprint": self.container_inventory_fingerprint,
            "container_has_capacity": self.container_has_capacity,
            "limitations": self.limitations,
        }


def _create_p1_live_fixture_observation(
    **values: object,
) -> P1LiveFixtureObservation:
    fingerprint = _observation_fingerprint(values)
    return P1LiveFixtureObservation(
        **values,
        observation_fingerprint=fingerprint,
        _seal=_P1_LIVE_FIXTURE_OBSERVATION_SEAL,
        _integrity=fingerprint,
    )


def _public_values(observation: P1LiveFixtureObservation) -> dict[str, object]:
    return {
        field_name: getattr(observation, field_name)
        for field_name in observation.__dataclass_fields__
        if field_name not in {"observation_fingerprint", "_seal", "_integrity"}
    }


def _observation_fingerprint(values: object) -> str:
    if not isinstance(values, dict):
        raise ValueError("P1 fixture fingerprint values are invalid")
    payload = json.dumps(
        values,
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def p1_world_snapshot_fingerprint(
    *,
    world_key: str,
    dimension: str,
    level_dat_sha256: str,
    region_sha256: str,
    region_chunk_timestamp: int,
) -> str:
    payload = json.dumps(
        {
            "dimension": dimension,
            "level_dat_sha256": level_dat_sha256,
            "region_chunk_timestamp": region_chunk_timestamp,
            "region_sha256": region_sha256,
            "world_key": world_key,
        },
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()
