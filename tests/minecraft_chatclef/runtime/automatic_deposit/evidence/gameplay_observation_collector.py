#20260831_kpopmodder: Collect explicit post-run gameplay evidence without auto-confirmation.
from __future__ import annotations

import re
from collections.abc import Mapping

from ...batch.gameplay_checkpoint import CHECKPOINT_FIELDS
from ...observation.gameplay_outcome import apply_gameplay_evidence
from .gameplay_observation_manifest import (
    AutomaticDepositGameplayObservationManifest,
    _create_gameplay_observation_manifest,
)


def collect_automatic_deposit_gameplay_observation(
    *,
    run_id: object,
    row_id: object,
    operation_id: object,
    artifact_sha256: object,
    fixture_fingerprint: object,
    before_world_snapshot_id: object,
    after_world_snapshot_id: object,
    before_inventory_snapshot_id: object,
    after_inventory_snapshot_id: object,
    observation_source: object,
    observer_identity_fingerprint: object,
    operator_confirmed: object,
    checkpoint: object,
) -> tuple[AutomaticDepositGameplayObservationManifest | None, str]:
    if not isinstance(checkpoint, Mapping):
        return None, "GAMEPLAY_CHECKPOINT_NOT_MAPPING"
    if set(checkpoint) != set(CHECKPOINT_FIELDS):
        return None, "GAMEPLAY_CHECKPOINT_FIELDS_INVALID"
    if any(type(checkpoint[field]) is not bool for field in CHECKPOINT_FIELDS):
        return None, "GAMEPLAY_CHECKPOINT_VALUE_NOT_BOOLEAN"
    text_fields = {
        "run_id": _text(run_id),
        "row_id": _text(row_id),
        "operation_id": _text(operation_id),
        "artifact_sha256": _text(artifact_sha256).lower(),
        "fixture_fingerprint": _text(fixture_fingerprint).lower(),
        "before_world_snapshot_id": _text(before_world_snapshot_id),
        "after_world_snapshot_id": _text(after_world_snapshot_id),
        "before_inventory_snapshot_id": _text(before_inventory_snapshot_id),
        "after_inventory_snapshot_id": _text(after_inventory_snapshot_id),
        "observation_source": _text(observation_source),
        "observer_identity_fingerprint": _text(
            observer_identity_fingerprint
        ).lower(),
    }
    if any(not value for value in text_fields.values()):
        return None, "GAMEPLAY_OBSERVATION_IDENTITY_MISSING"
    if not _is_sha256(text_fields["artifact_sha256"]):
        return None, "GAMEPLAY_ARTIFACT_SHA256_INVALID"
    if not _is_sha256(text_fields["fixture_fingerprint"]):
        return None, "GAMEPLAY_FIXTURE_FINGERPRINT_INVALID"
    if not _is_sha256(text_fields["observer_identity_fingerprint"]):
        return None, "GAMEPLAY_OBSERVER_IDENTITY_INVALID"
    if text_fields["observation_source"] not in (
        "OPERATOR_OBSERVATION",
        "AUTOMATED_WORLD_SNAPSHOT",
    ):
        return None, "GAMEPLAY_OBSERVATION_SOURCE_INVALID"
    if operator_confirmed is not True:
        return None, "GAMEPLAY_OBSERVATION_NOT_OPERATOR_CONFIRMED"
    if (
        text_fields["before_world_snapshot_id"]
        == text_fields["after_world_snapshot_id"]
        or text_fields["before_inventory_snapshot_id"]
        == text_fields["after_inventory_snapshot_id"]
    ):
        return None, "GAMEPLAY_BEFORE_AFTER_SNAPSHOT_NOT_DISTINCT"

    normalized: dict[str, object] = {}
    apply_gameplay_evidence(
        normalized,
        observation_complete=checkpoint["gameplay_observation_complete"],
        effect_observed=checkpoint["gameplay_effect_observed"],
        expected_effect_verified=checkpoint[
            "expected_gameplay_effect_verified"
        ],
        partial_effect_observed=checkpoint[
            "partial_gameplay_effect_observed"
        ],
        unexpected_effect_observed=checkpoint["unexpected_effect_observed"],
        prohibited_effect_absence_verified=checkpoint[
            "prohibited_effect_absence_verified"
        ],
    )
    values = tuple(
        (field, bool(normalized[field])) for field in CHECKPOINT_FIELDS
    )
    return (
        _create_gameplay_observation_manifest(
            schema_version="automatic-deposit-gameplay-observation/v1",
            values=values,
            operator_confirmed=True,
            **text_fields,
        ),
        "GAMEPLAY_OBSERVATION_COLLECTED",
    )


def _text(value: object) -> str:
    return value.strip() if type(value) is str else ""


def _is_sha256(value: object) -> bool:
    return bool(re.fullmatch(r"[0-9a-f]{64}", str(value or "")))
