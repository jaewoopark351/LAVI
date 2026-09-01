# 20260901_kpopmodder: Assemble only directly bound production P1 StoreHome evidence.
from __future__ import annotations

from ....scenario.matrix_row import AutomaticDepositMatrixRow
from ...artifact_identity_collection import AutomaticDepositArtifactIdentityCollection
from ...carry_on_identity_collection import AutomaticDepositCarryOnIdentityCollection
from ...fixture_manifest import AutomaticDepositFixtureManifest
from ...gameplay_observation_manifest import (
    AutomaticDepositGameplayObservationManifest,
)
from ...latest_log_delta_result import AutomaticDepositLatestLogDeltaResult
from ...live_fixture.p1_live_fixture_binding_contract import (
    verify_p1_live_fixture_binding,
)
from ...live_fixture.p1_live_fixture_observation import P1LiveFixtureObservation
from ...operator_action.manual_operator_action_contract import (
    verify_p1_manual_operator_action,
)
from ...operator_action.manual_operator_action_observation import (
    ManualOperatorActionObservation,
)
from ...preflight.evidence_preflight import (
    verify_automatic_deposit_evidence_preflight,
)
from ...run_manifest import AutomaticDepositRunManifest
from ...runtime_log.log_delta_contract import (
    verify_automatic_deposit_runtime_log_delta,
)
from ...verified_evidence import _create_verified_evidence
from ...verified_evidence_result import AutomaticDepositVerifiedEvidenceResult
from ....oracle.runtime_log.store_home.p1_runtime_log_adapter import (
    adapt_p1_store_home_runtime_log,
)
from .runtime_observation.p1_runtime_observation_bundle import (
    P1RuntimeObservationBundle,
)
from .runtime_observation.p1_runtime_observation_bundle_binding_contract import (
    verify_p1_runtime_observation_bundle_binding,
)


def build_p1_store_home_verified_evidence(
    row: AutomaticDepositMatrixRow,
    run_manifest: AutomaticDepositRunManifest,
    fixture_manifest: AutomaticDepositFixtureManifest,
    artifact_collection: AutomaticDepositArtifactIdentityCollection,
    log_delta: AutomaticDepositLatestLogDeltaResult,
    gameplay_observation: AutomaticDepositGameplayObservationManifest | None,
    *,
    carry_on_collection: AutomaticDepositCarryOnIdentityCollection | None,
    p1_runtime_observation_bundle: P1RuntimeObservationBundle | None,
    p1_live_fixture_observation: P1LiveFixtureObservation | None,
    operator_action_observation: ManualOperatorActionObservation | None,
) -> AutomaticDepositVerifiedEvidenceResult:
    if not isinstance(row, AutomaticDepositMatrixRow) or row.row_id != "P1":
        return _failure(
            "P1_STORE_HOME_ROW_REQUIRED",
            ("P1_STORE_HOME_ROW_REQUIRED",),
        )
    preflight_reason, preflight_errors = verify_automatic_deposit_evidence_preflight(
        row,
        run_manifest,
        fixture_manifest,
        artifact_collection,
        gameplay_observation=gameplay_observation,
        carry_on_collection=carry_on_collection,
    )
    if preflight_errors:
        return _failure(preflight_reason, preflight_errors)

    fixture_binding = verify_p1_live_fixture_binding(
        p1_live_fixture_observation,
        run_manifest,
        fixture_manifest,
    )
    if not fixture_binding.ok:
        return _failure(
            "P1_STORE_HOME_LIVE_FIXTURE_BINDING_FAILED",
            fixture_binding.errors,
        )
    if not isinstance(p1_live_fixture_observation, P1LiveFixtureObservation):
        return _failure(
            "P1_STORE_HOME_LIVE_FIXTURE_BINDING_FAILED",
            ("P1_LIVE_FIXTURE_OBSERVATION_NOT_TYPED",),
        )

    log_errors = verify_automatic_deposit_runtime_log_delta(
        run_manifest.latest_log_cursor,
        log_delta,
    )
    if log_errors:
        return _failure("P1_STORE_HOME_LOG_DELTA_FAILED", log_errors)

    runtime_bundle_errors = verify_p1_runtime_observation_bundle_binding(
        p1_runtime_observation_bundle,
        run_manifest,
    )
    if runtime_bundle_errors:
        return _failure(
            "P1_STORE_HOME_RUNTIME_OBSERVATION_BUNDLE_FAILED",
            runtime_bundle_errors,
        )
    if not isinstance(
        p1_runtime_observation_bundle,
        P1RuntimeObservationBundle,
    ):
        return _failure(
            "P1_STORE_HOME_RUNTIME_OBSERVATION_BUNDLE_FAILED",
            ("P1_RUNTIME_OBSERVATION_BUNDLE_NOT_TYPED",),
        )
    raw_artifact = (
        p1_runtime_observation_bundle.jvm_log_prefix.runtime_artifact_observation
    )

    action_errors = verify_p1_manual_operator_action(
        operator_action_observation,
        row,
        run_manifest,
    )
    if action_errors:
        return _failure("P1_STORE_HOME_OPERATOR_ACTION_FAILED", action_errors)

    candidate_position = fixture_manifest.as_evidence_mapping().get(
        "trusted_container_position"
    )
    projection = adapt_p1_store_home_runtime_log(
        log_delta,
        expected_candidate_position=candidate_position,
        expected_run_manifest_id=raw_artifact.run_manifest_id,
    )
    if not projection.ok:
        return _failure(
            "P1_STORE_HOME_RUNTIME_LOG_NOT_VERIFIED",
            (projection.reason,),
        )

    values: dict[str, object] = {
        "artifact_identity_verified": True,
        "fixture_identity_verified": True,
        "runtime_log_complete": True,
        "operator_action_observed": True,
        "java_store_home_operation_id": projection.operation_id,
        "jvm_run_manifest_id": raw_artifact.run_manifest_id,
        "jvm_runtime_code_source_sha256": raw_artifact.runtime_code_source_sha256,
        "production_status_fingerprint": (
            p1_runtime_observation_bundle.status_fingerprint
        ),
        "jvm_log_prefix_fingerprint": (
            p1_runtime_observation_bundle.log_prefix_fingerprint
        ),
        "jvm_runtime_artifact_observation_fingerprint": (
            p1_runtime_observation_bundle.raw_artifact_fingerprint
        ),
        "p1_static_manifest_binding_fingerprint": (
            p1_runtime_observation_bundle.binding_fingerprint
        ),
        "p1_static_manifest_binding_classification": (
            p1_runtime_observation_bundle.static_binding_classification
        ),
        "p1_static_manifest_binding_reason": (
            p1_runtime_observation_bundle.static_binding_reason
        ),
        "p1_operation_correlation_claimed": (
            p1_runtime_observation_bundle.p1_operation_correlation_claimed
        ),
        "p1_runtime_observation_bundle_fingerprint": (
            p1_runtime_observation_bundle.bundle_fingerprint
        ),
        "p1_harness_run_binding_fingerprint": (
            p1_runtime_observation_bundle.harness_run_binding_fingerprint
        ),
        "p1_live_fixture_observation_fingerprint": (
            p1_live_fixture_observation.observation_fingerprint
        ),
    }
    values.update(projection.evidence_mapping())
    values.update(gameplay_observation.as_mapping())
    return AutomaticDepositVerifiedEvidenceResult(
        ok=True,
        reason="P1_STORE_HOME_VERIFIED_EVIDENCE_BOUND_TO_RUN",
        evidence=_create_verified_evidence(
            run_id=run_manifest.run_id,
            row_id=row.row_id,
            operation_id=run_manifest.operation_id,
            artifact_sha256=(
                run_manifest.artifact_identity.deployed_jar_sha256.casefold()
            ),
            fixture_fingerprint=fixture_manifest.fixture_fingerprint.casefold(),
            values=values,
        ),
    )


def _failure(
    reason: str,
    errors: tuple[str, ...],
) -> AutomaticDepositVerifiedEvidenceResult:
    return AutomaticDepositVerifiedEvidenceResult(
        ok=False,
        reason=reason,
        errors=errors,
    )
