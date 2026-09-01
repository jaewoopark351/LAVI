#20260901_kpopmodder: Bind typed gameplay snapshots to one supervised runtime, fixture, and Java operation.
from __future__ import annotations

import re

from minecraft_chatclef.runtime.automatic_deposit.evidence.gameplay_observation_manifest import (
    AutomaticDepositGameplayObservationManifest,
)
from minecraft_chatclef.runtime.automatic_deposit.evidence.live_fixture.p1_live_fixture_observation import (
    P1LiveFixtureObservation,
)
from minecraft_chatclef.runtime.automatic_deposit.evidence.runtime_observer.status_artifact.p1_supervised_runtime_artifact_observation import (
    P1SupervisedRuntimeArtifactObservation,
)
from minecraft_chatclef.runtime.batch.gameplay_checkpoint import CHECKPOINT_FIELDS
from minecraft_chatclef.runtime.automatic_deposit.orchestration.live.p1_supervised.execution.p1_supervised_execution_result import (
    P1SupervisedExecutionResult,
)


_EXPECTED_CHECKPOINT = {
    "gameplay_observation_complete": True,
    "gameplay_effect_observed": True,
    "expected_gameplay_effect_verified": True,
    "partial_gameplay_effect_observed": False,
    "unexpected_effect_observed": False,
    "prohibited_effect_absence_verified": True,
}


def p1_supervised_gameplay_observation_error(
    observation: AutomaticDepositGameplayObservationManifest,
    *,
    expected_run_manifest_id: str,
    expected_operation_id: str,
    runtime_artifact: P1SupervisedRuntimeArtifactObservation,
    live_fixture: P1LiveFixtureObservation,
) -> str:
    if observation.schema_version != "automatic-deposit-gameplay-observation/v1":
        return "P1_SUPERVISED_GAMEPLAY_SCHEMA_INVALID"
    for actual, expected, reason in (
        (
            observation.run_id,
            expected_run_manifest_id,
            "P1_SUPERVISED_GAMEPLAY_RUN_ID_MISMATCH",
        ),
        (observation.row_id, "P1", "P1_SUPERVISED_GAMEPLAY_ROW_ID_MISMATCH"),
        (
            observation.operation_id,
            expected_operation_id,
            "P1_SUPERVISED_GAMEPLAY_OPERATION_ID_MISMATCH",
        ),
        (
            observation.artifact_sha256.casefold(),
            runtime_artifact.loaded_jar_sha256.casefold(),
            "P1_SUPERVISED_GAMEPLAY_ARTIFACT_SHA256_MISMATCH",
        ),
        (
            observation.fixture_fingerprint.casefold(),
            live_fixture.observation_fingerprint.casefold(),
            "P1_SUPERVISED_GAMEPLAY_FIXTURE_FINGERPRINT_MISMATCH",
        ),
        (
            observation.before_world_snapshot_id,
            live_fixture.world_snapshot_fingerprint,
            "P1_SUPERVISED_GAMEPLAY_BEFORE_WORLD_SNAPSHOT_MISMATCH",
        ),
        (
            observation.before_inventory_snapshot_id,
            live_fixture.player_inventory_fingerprint,
            "P1_SUPERVISED_GAMEPLAY_BEFORE_INVENTORY_SNAPSHOT_MISMATCH",
        ),
    ):
        if actual != expected:
            return reason
    if observation.operator_confirmed is not True:
        return "P1_SUPERVISED_GAMEPLAY_NOT_OPERATOR_CONFIRMED"
    if observation.observation_source not in {
        "OPERATOR_OBSERVATION",
        "AUTOMATED_WORLD_SNAPSHOT",
    }:
        return "P1_SUPERVISED_GAMEPLAY_SOURCE_INVALID"
    if re.fullmatch(
        r"[0-9a-fA-F]{64}", observation.observer_identity_fingerprint
    ) is None:
        return "P1_SUPERVISED_GAMEPLAY_OBSERVER_IDENTITY_INVALID"
    if (
        observation.before_world_snapshot_id == observation.after_world_snapshot_id
        or observation.before_inventory_snapshot_id
        == observation.after_inventory_snapshot_id
    ):
        return "P1_SUPERVISED_GAMEPLAY_BEFORE_AFTER_SNAPSHOT_NOT_DISTINCT"
    values = dict(observation.values)
    keys = tuple(key for key, _value in observation.values)
    if set(keys) != set(CHECKPOINT_FIELDS) or len(keys) != len(set(keys)):
        return "P1_SUPERVISED_GAMEPLAY_CHECKPOINT_FIELDS_INVALID"
    for key in CHECKPOINT_FIELDS:
        if values.get(key) is not _EXPECTED_CHECKPOINT[key]:
            return f"P1_SUPERVISED_GAMEPLAY_CHECKPOINT_INVALID:{key}"
    return ""


def p1_supervised_gameplay_receipt_error(
    execution: P1SupervisedExecutionResult,
    observation: AutomaticDepositGameplayObservationManifest,
) -> str:
    values = observation.as_mapping()
    for field in CHECKPOINT_FIELDS:
        receipt_value = getattr(execution, field)
        if receipt_value is not None and receipt_value is not values[field]:
            return f"P1_SUPERVISED_GAMEPLAY_RECEIPT_CONFLICT:{field}"
    if execution.end_to_end_success is False:
        return "P1_SUPERVISED_GAMEPLAY_RECEIPT_CONFLICT:end_to_end_success"
    return ""
