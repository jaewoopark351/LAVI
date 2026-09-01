#20260901_kpopmodder: Assemble supervised submit, terminal, production-log, and gameplay evidence.
from __future__ import annotations

from minecraft_chatclef.runtime.automatic_deposit.evidence.gameplay_observation_manifest import (
    AutomaticDepositGameplayObservationManifest,
)
from minecraft_chatclef.runtime.automatic_deposit.evidence.live_fixture.p1_live_fixture_observation import (
    P1LiveFixtureObservation,
)
from minecraft_chatclef.runtime.automatic_deposit.evidence.runtime_observer.status_artifact.p1_supervised_runtime_artifact_observation import (
    P1SupervisedRuntimeArtifactObservation,
)
from minecraft_chatclef.runtime.automatic_deposit.oracle.runtime_log.store_home.supervised.adapter.p1_supervised_runtime_log_adapter import (
    adapt_p1_supervised_store_home_runtime_log,
)
from minecraft_chatclef.runtime.automatic_deposit.orchestration.live.p1_supervised.execution.p1_supervised_execution_result import (
    P1SupervisedExecutionResult,
)

from .p1_supervised_verified_evidence_result import (
    P1SupervisedVerifiedEvidenceResult,
    inconclusive_p1_supervised_evidence,
)
from .p1_supervised_execution_result_contract import (
    p1_supervised_execution_result_error,
)
from .p1_supervised_gameplay_observation_contract import (
    p1_supervised_gameplay_observation_error,
    p1_supervised_gameplay_receipt_error,
)


def build_p1_supervised_verified_evidence(
    execution: object,
    log_delta: object,
    *,
    runtime_artifact: object,
    live_fixture: object,
    gameplay_observation: object,
    expected_run_manifest_id: object,
) -> P1SupervisedVerifiedEvidenceResult:
    if not isinstance(execution, P1SupervisedExecutionResult):
        return inconclusive_p1_supervised_evidence(
            "P1_SUPERVISED_EXECUTION_RESULT_NOT_TYPED"
        )
    failure = p1_supervised_execution_result_error(execution)
    if failure:
        return _inconclusive(execution, failure)
    if not isinstance(runtime_artifact, P1SupervisedRuntimeArtifactObservation):
        return _inconclusive(
            execution,
            "P1_SUPERVISED_RUNTIME_ARTIFACT_NOT_TYPED",
        )
    if not isinstance(live_fixture, P1LiveFixtureObservation):
        return _inconclusive(execution, "P1_SUPERVISED_LIVE_FIXTURE_NOT_TYPED")
    if (
        live_fixture.schema_version != "p1-live-fixture-observation/v1"
        or live_fixture.verdict != "PASS"
        or live_fixture.reason != "P1_LIVE_FIXTURE_READY"
        or live_fixture.limitations
        or live_fixture.container_has_capacity is not True
    ):
        return _inconclusive(execution, "P1_SUPERVISED_FIXTURE_NOT_READY")
    if not isinstance(
        gameplay_observation,
        AutomaticDepositGameplayObservationManifest,
    ):
        return _inconclusive(
            execution,
            "P1_SUPERVISED_GAMEPLAY_OBSERVATION_NOT_TYPED",
        )
    candidate_position = ", ".join(
        str(value) for value in live_fixture.trusted_position
    )
    destination_canonical_key = "|".join(
        (
            live_fixture.world_key,
            live_fixture.dimension,
            *(str(value) for value in live_fixture.trusted_position),
        )
    )
    projection = adapt_p1_supervised_store_home_runtime_log(
        log_delta,
        expected_candidate_position=candidate_position,
        expected_run_manifest_id=expected_run_manifest_id,
        expected_command_request_id=execution.submitted_request_id,
        expected_command_session_id=runtime_artifact.session_id,
        expected_world_key=live_fixture.world_key,
        expected_dimension=live_fixture.dimension,
        expected_destination_canonical_key=destination_canonical_key,
        expected_destination_id=live_fixture.trusted_destination_id,
    )
    if not projection.ok:
        return _inconclusive(
            execution,
            projection.reason,
            operation_id=projection.operation_id,
        )
    gameplay_error = p1_supervised_gameplay_observation_error(
        gameplay_observation,
        expected_run_manifest_id=str(expected_run_manifest_id),
        expected_operation_id=projection.operation_id,
        runtime_artifact=runtime_artifact,
        live_fixture=live_fixture,
    )
    if gameplay_error:
        return _inconclusive(
            execution,
            gameplay_error,
            operation_id=projection.operation_id,
        )
    receipt_error = p1_supervised_gameplay_receipt_error(
        execution,
        gameplay_observation,
    )
    if receipt_error:
        return _inconclusive(
            execution,
            receipt_error,
            operation_id=projection.operation_id,
        )
    values: dict[str, object] = {
        "helper_submit_call_count": execution.submit_call_count,
        "automatic_resubmit_count": execution.automatic_resubmit_count,
        "actual_submitted_request_id": execution.submitted_request_id,
        "external_run_manifest_id": expected_run_manifest_id,
        "runtime_artifact_observation_fingerprint": (
            runtime_artifact.observation_fingerprint
        ),
        "jvm_loaded_jar_sha256": runtime_artifact.loaded_jar_sha256,
        "jvm_loaded_jar_path": runtime_artifact.loaded_jar_path,
        "jvm_loaded_jar_size": runtime_artifact.loaded_jar_size,
        "minecraft_process_id": runtime_artifact.minecraft_process_id,
        "command_session_id": runtime_artifact.session_id,
        "live_fixture_observation_fingerprint": (
            live_fixture.observation_fingerprint
        ),
        "trusted_destination_id": live_fixture.trusted_destination_id,
        "trusted_destination_fingerprint": (
            live_fixture.trusted_destination_fingerprint
        ),
        "trusted_destination_canonical_key": destination_canonical_key,
        "trusted_registry_sha256": live_fixture.trusted_registry_sha256,
        "before_world_snapshot_id": live_fixture.world_snapshot_fingerprint,
        "before_inventory_snapshot_id": (
            live_fixture.player_inventory_fingerprint
        ),
        "gameplay_observer_identity_fingerprint": (
            gameplay_observation.observer_identity_fingerprint
        ),
        "runtime_log_complete": True,
        "terminal_lifecycle_observed": True,
        "runtime_reported_completion": True,
    }
    values.update(projection.evidence_mapping())
    values.update(gameplay_observation.as_mapping())
    return P1SupervisedVerifiedEvidenceResult(
        ok=True,
        verdict="VERIFIED_PENDING_RECONCILIATION",
        reason="P1_SUPERVISED_EVIDENCE_VERIFIED_PENDING_RECONCILIATION",
        submit_call_count=execution.submit_call_count,
        automatic_resubmit_count=execution.automatic_resubmit_count,
        submitted_request_id=execution.submitted_request_id,
        operation_id=projection.operation_id,
        evidence=tuple(sorted(values.items())),
    )


def _inconclusive(
    execution: P1SupervisedExecutionResult,
    reason: str,
    *,
    operation_id: str = "",
) -> P1SupervisedVerifiedEvidenceResult:
    return inconclusive_p1_supervised_evidence(
        reason,
        submit_call_count=max(execution.submit_call_count, 0),
        automatic_resubmit_count=max(execution.automatic_resubmit_count, 0),
        submitted_request_id=execution.submitted_request_id,
        operation_id=operation_id,
    )
