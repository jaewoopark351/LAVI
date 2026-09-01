#20260831_kpopmodder: Bind post-run gameplay evidence to the exact run and fixture.
from __future__ import annotations

import re

from ...batch.gameplay_checkpoint import CHECKPOINT_FIELDS
from .artifact_identity import AutomaticDepositArtifactIdentity
from .fixture_manifest import AutomaticDepositFixtureManifest
from .gameplay_observation_manifest import (
    AutomaticDepositGameplayObservationManifest,
)
from .run_manifest import AutomaticDepositRunManifest


def verify_automatic_deposit_gameplay_observation(
    manifest: object,
    run_manifest: AutomaticDepositRunManifest,
    fixture_manifest: AutomaticDepositFixtureManifest,
) -> tuple[str, ...]:
    if not isinstance(manifest, AutomaticDepositGameplayObservationManifest):
        return ("GAMEPLAY_OBSERVATION_NOT_TYPED",)
    errors: list[str] = []
    if manifest.schema_version != "automatic-deposit-gameplay-observation/v1":
        errors.append("GAMEPLAY_OBSERVATION_SCHEMA_INVALID")
    identity_pairs = [
        (manifest.run_id, run_manifest.run_id, "GAMEPLAY_RUN_ID_MISMATCH"),
        (manifest.row_id, run_manifest.row_id, "GAMEPLAY_ROW_ID_MISMATCH"),
        (
            manifest.operation_id,
            run_manifest.operation_id,
            "GAMEPLAY_OPERATION_ID_MISMATCH",
        ),
        (
            manifest.fixture_fingerprint.casefold(),
            fixture_manifest.fixture_fingerprint.casefold(),
            "GAMEPLAY_FIXTURE_FINGERPRINT_MISMATCH",
        ),
        (
            manifest.before_world_snapshot_id,
            fixture_manifest.world_snapshot_id,
            "GAMEPLAY_BEFORE_WORLD_SNAPSHOT_MISMATCH",
        ),
        (
            manifest.before_inventory_snapshot_id,
            fixture_manifest.inventory_snapshot_id,
            "GAMEPLAY_BEFORE_INVENTORY_SNAPSHOT_MISMATCH",
        ),
    ]
    if isinstance(run_manifest.artifact_identity, AutomaticDepositArtifactIdentity):
        identity_pairs.append(
            (
                manifest.artifact_sha256.casefold(),
                run_manifest.artifact_identity.deployed_jar_sha256.casefold(),
                "GAMEPLAY_ARTIFACT_SHA256_MISMATCH",
            )
        )
    else:
        errors.append("GAMEPLAY_RUN_ARTIFACT_IDENTITY_NOT_TYPED")
    for actual, expected, reason in identity_pairs:
        if actual != expected:
            errors.append(reason)
    if manifest.operator_confirmed is not True:
        errors.append("GAMEPLAY_OBSERVATION_NOT_OPERATOR_CONFIRMED")
    if manifest.observation_source not in (
        "OPERATOR_OBSERVATION",
        "AUTOMATED_WORLD_SNAPSHOT",
    ):
        errors.append("GAMEPLAY_OBSERVATION_SOURCE_INVALID")
    if not re.fullmatch(
        r"[0-9a-fA-F]{64}", manifest.observer_identity_fingerprint
    ):
        errors.append("GAMEPLAY_OBSERVER_IDENTITY_INVALID")
    if (
        manifest.before_world_snapshot_id == manifest.after_world_snapshot_id
        or manifest.before_inventory_snapshot_id
        == manifest.after_inventory_snapshot_id
    ):
        errors.append("GAMEPLAY_BEFORE_AFTER_SNAPSHOT_NOT_DISTINCT")
    keys = tuple(key for key, _value in manifest.values)
    if set(keys) != set(CHECKPOINT_FIELDS) or len(keys) != len(set(keys)):
        errors.append("GAMEPLAY_CHECKPOINT_FIELDS_INVALID")
    if any(type(value) is not bool for _key, value in manifest.values):
        errors.append("GAMEPLAY_CHECKPOINT_VALUE_NOT_BOOLEAN")
    return tuple(errors)
