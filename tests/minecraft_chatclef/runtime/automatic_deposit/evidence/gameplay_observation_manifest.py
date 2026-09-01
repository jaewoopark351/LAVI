#20260831_kpopmodder: Seal post-run gameplay evidence to one run and before/after snapshots.
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field


_GAMEPLAY_OBSERVATION_SEAL = object()


@dataclass(frozen=True, slots=True)
class AutomaticDepositGameplayObservationManifest:
    schema_version: str
    run_id: str
    row_id: str
    operation_id: str
    artifact_sha256: str
    fixture_fingerprint: str
    before_world_snapshot_id: str
    after_world_snapshot_id: str
    before_inventory_snapshot_id: str
    after_inventory_snapshot_id: str
    observation_source: str
    observer_identity_fingerprint: str
    operator_confirmed: bool
    values: tuple[tuple[str, bool], ...]
    _seal: object = field(repr=False, compare=False)
    _integrity: str = field(repr=False, compare=False)

    def __post_init__(self) -> None:
        if self._seal is not _GAMEPLAY_OBSERVATION_SEAL:
            raise ValueError(
                "gameplay observation must be created by its collector"
            )
        if self._integrity != _gameplay_observation_integrity(
            self.schema_version,
            self.run_id,
            self.row_id,
            self.operation_id,
            self.artifact_sha256,
            self.fixture_fingerprint,
            self.before_world_snapshot_id,
            self.after_world_snapshot_id,
            self.before_inventory_snapshot_id,
            self.after_inventory_snapshot_id,
            self.observation_source,
            self.observer_identity_fingerprint,
            self.operator_confirmed,
            self.values,
        ):
            raise ValueError("gameplay observation integrity mismatch")

    def as_mapping(self) -> dict[str, bool]:
        return dict(self.values)


def _create_gameplay_observation_manifest(
    *,
    schema_version: str,
    run_id: str,
    row_id: str,
    operation_id: str,
    artifact_sha256: str,
    fixture_fingerprint: str,
    before_world_snapshot_id: str,
    after_world_snapshot_id: str,
    before_inventory_snapshot_id: str,
    after_inventory_snapshot_id: str,
    observation_source: str,
    observer_identity_fingerprint: str,
    operator_confirmed: bool,
    values: tuple[tuple[str, bool], ...],
) -> AutomaticDepositGameplayObservationManifest:
    normalized_values = tuple(sorted(values))
    integrity = _gameplay_observation_integrity(
        schema_version,
        run_id,
        row_id,
        operation_id,
        artifact_sha256,
        fixture_fingerprint,
        before_world_snapshot_id,
        after_world_snapshot_id,
        before_inventory_snapshot_id,
        after_inventory_snapshot_id,
        observation_source,
        observer_identity_fingerprint,
        operator_confirmed,
        normalized_values,
    )
    return AutomaticDepositGameplayObservationManifest(
        schema_version=schema_version,
        run_id=run_id,
        row_id=row_id,
        operation_id=operation_id,
        artifact_sha256=artifact_sha256,
        fixture_fingerprint=fixture_fingerprint,
        before_world_snapshot_id=before_world_snapshot_id,
        after_world_snapshot_id=after_world_snapshot_id,
        before_inventory_snapshot_id=before_inventory_snapshot_id,
        after_inventory_snapshot_id=after_inventory_snapshot_id,
        observation_source=observation_source,
        observer_identity_fingerprint=observer_identity_fingerprint,
        operator_confirmed=operator_confirmed,
        values=normalized_values,
        _seal=_GAMEPLAY_OBSERVATION_SEAL,
        _integrity=integrity,
    )


def _gameplay_observation_integrity(*values: object) -> str:
    raw = json.dumps(
        values,
        ensure_ascii=False,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(raw).hexdigest()
