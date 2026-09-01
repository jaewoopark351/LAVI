#20260901_kpopmodder: Assemble already verified evidence without owning preflight or log parsing.
from __future__ import annotations

from ..artifact_identity_collection import AutomaticDepositArtifactIdentityCollection
from ..carry_on_identity_collection import AutomaticDepositCarryOnIdentityCollection
from ..fixture_manifest import AutomaticDepositFixtureManifest
from ..gameplay_observation_manifest import AutomaticDepositGameplayObservationManifest
from ..java_contract_manifest import AutomaticDepositJavaContractManifest
from ..java_contract_manifest_contract import java_contract_evidence_mapping
from ..latest_log_delta_result import AutomaticDepositLatestLogDeltaResult
from ..preflight.evidence_preflight import (
    verify_automatic_deposit_evidence_preflight,
)
from ..run_manifest import AutomaticDepositRunManifest
from ..runtime_log.synthetic_evidence_correlation import (
    parse_correlated_synthetic_runtime_events,
    verify_synthetic_runtime_log_delta,
)
from ..verified_evidence import _create_verified_evidence
from ..verified_evidence_result import AutomaticDepositVerifiedEvidenceResult
from ...scenario.matrix_row import AutomaticDepositMatrixRow


def build_automatic_deposit_verified_evidence(
    row: AutomaticDepositMatrixRow,
    run_manifest: AutomaticDepositRunManifest,
    fixture_manifest: AutomaticDepositFixtureManifest,
    artifact_collection: AutomaticDepositArtifactIdentityCollection,
    log_delta: AutomaticDepositLatestLogDeltaResult,
    gameplay_observation: AutomaticDepositGameplayObservationManifest | None,
    *,
    carry_on_collection: AutomaticDepositCarryOnIdentityCollection | None,
    java_contract_manifest: AutomaticDepositJavaContractManifest | None = None,
) -> AutomaticDepositVerifiedEvidenceResult:
    if isinstance(row, AutomaticDepositMatrixRow) and row.row_id == "P1":
        return _failure(
            "P1_STORE_HOME_REQUIRES_PRODUCTION_ASSEMBLY",
            ("P1_STORE_HOME_REQUIRES_PRODUCTION_ASSEMBLY",),
        )
    preflight_reason, preflight_errors = verify_automatic_deposit_evidence_preflight(
        row,
        run_manifest,
        fixture_manifest,
        artifact_collection,
        gameplay_observation=gameplay_observation,
        carry_on_collection=carry_on_collection,
        java_contract_manifest=java_contract_manifest,
    )
    if preflight_errors:
        return _failure(preflight_reason, preflight_errors)

    errors = list(verify_synthetic_runtime_log_delta(run_manifest, log_delta))
    if errors:
        return _failure("VERIFIED_EVIDENCE_LOG_DELTA_FAILED", errors)

    events, event_errors = parse_correlated_synthetic_runtime_events(
        row,
        run_manifest,
        fixture_manifest,
        log_delta,
    )
    if event_errors:
        return _failure("RUNTIME_EVIDENCE_NOT_VERIFIED", event_errors)

    values: dict[str, object] = {
        "artifact_identity_verified": True,
        "fixture_identity_verified": True,
        "runtime_log_complete": True,
    }
    for event in events:
        values[event.evidence_key] = _canonical_value(event.evidence_value)
    if gameplay_observation is not None:
        values.update(gameplay_observation.as_mapping())
    values.update(java_contract_evidence_mapping(java_contract_manifest))

    unsupported = tuple(
        requirement.key
        for requirement in row.evidence_requirements
        if requirement.owner not in (
            "RUNTIME_LOG",
            "JAVA_DETERMINISTIC",
            "ARTIFACT_PREFLIGHT",
            "OPERATOR_FIXTURE",
            "HARNESS_CONTROL",
            "GAMEPLAY_OBSERVATION",
        )
    )
    if unsupported:
        return _failure("EVIDENCE_OWNER_UNSUPPORTED", unsupported)
    unsupported_derived = tuple(
        requirement.key
        for requirement in row.evidence_requirements
        if requirement.owner in ("ARTIFACT_PREFLIGHT", "OPERATOR_FIXTURE")
        and requirement.key not in values
    )
    if unsupported_derived:
        return _failure("DERIVED_EVIDENCE_NOT_IMPLEMENTED", unsupported_derived)

    return AutomaticDepositVerifiedEvidenceResult(
        ok=True,
        reason="VERIFIED_EVIDENCE_BOUND_TO_RUN",
        evidence=_create_verified_evidence(
            run_id=run_manifest.run_id,
            row_id=row.row_id,
            operation_id=run_manifest.operation_id,
            artifact_sha256=(
                run_manifest.artifact_identity.deployed_jar_sha256.lower()
            ),
            fixture_fingerprint=fixture_manifest.fixture_fingerprint.lower(),
            values=values,
        ),
    )


def _canonical_value(value: str) -> object:
    if value == "true":
        return True
    if value == "false":
        return False
    return value


def _failure(
    reason: str,
    errors: tuple[str, ...] | list[str] = (),
) -> AutomaticDepositVerifiedEvidenceResult:
    return AutomaticDepositVerifiedEvidenceResult(
        ok=False,
        reason=reason,
        errors=tuple(errors),
    )
