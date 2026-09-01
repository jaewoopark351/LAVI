#20260901_kpopmodder: Verify run, fixture, artifact, and deterministic evidence before assembly.
from __future__ import annotations

from ..artifact_identity_collection import AutomaticDepositArtifactIdentityCollection
from ..carry_on_identity_collection import AutomaticDepositCarryOnIdentityCollection
from ..fixture_manifest import AutomaticDepositFixtureManifest
from ..fixture_manifest_contract import verify_automatic_deposit_fixture_manifest
from ..gameplay_observation_contract import (
    verify_automatic_deposit_gameplay_observation,
)
from ..gameplay_observation_manifest import (
    AutomaticDepositGameplayObservationManifest,
)
from ..java_contract_manifest import AutomaticDepositJavaContractManifest
from ..java_contract_manifest_contract import (
    verify_automatic_deposit_java_contract_manifest,
)
from ..run_manifest import AutomaticDepositRunManifest
from ..run_manifest_contract import verify_automatic_deposit_run_manifest
from ...scenario.matrix_row import AutomaticDepositMatrixRow
from .artifact_recheck import verify_artifact_recheck
from .carry_on_recheck import verify_carry_on_recheck
from .requirement_ownership import verify_requirement_ownership


def verify_automatic_deposit_evidence_preflight(
    row: AutomaticDepositMatrixRow,
    run_manifest: AutomaticDepositRunManifest,
    fixture_manifest: AutomaticDepositFixtureManifest,
    artifact_collection: AutomaticDepositArtifactIdentityCollection,
    *,
    gameplay_observation: AutomaticDepositGameplayObservationManifest | None,
    carry_on_collection: AutomaticDepositCarryOnIdentityCollection | None,
    java_contract_manifest: AutomaticDepositJavaContractManifest | None = None,
) -> tuple[str, tuple[str, ...]]:
    if row.row_id in ("P4", "P5"):
        return (
            "PAIRED_RUN_EVIDENCE_NOT_IMPLEMENTED",
            ("PAIRED_RUN_EVIDENCE_NOT_IMPLEMENTED",),
        )

    type_errors: list[str] = []
    if not isinstance(run_manifest, AutomaticDepositRunManifest):
        type_errors.append("RUN_MANIFEST_NOT_TYPED")
    if not isinstance(fixture_manifest, AutomaticDepositFixtureManifest):
        type_errors.append("FIXTURE_MANIFEST_NOT_TYPED")
    if not isinstance(
        artifact_collection,
        AutomaticDepositArtifactIdentityCollection,
    ):
        type_errors.append("ARTIFACT_RECHECK_NOT_TYPED")
    if java_contract_manifest is not None and not isinstance(
        java_contract_manifest,
        AutomaticDepositJavaContractManifest,
    ):
        type_errors.append("JAVA_CONTRACT_MANIFEST_NOT_TYPED")
    if not isinstance(
        gameplay_observation,
        AutomaticDepositGameplayObservationManifest,
    ):
        type_errors.append("GAMEPLAY_OBSERVATION_NOT_TYPED")
    if not isinstance(
        carry_on_collection,
        AutomaticDepositCarryOnIdentityCollection,
    ):
        type_errors.append("CARRY_ON_IDENTITY_COLLECTION_NOT_TYPED")
    if type_errors:
        return "VERIFIED_EVIDENCE_PREFLIGHT_FAILED", tuple(type_errors)

    errors: list[str] = []
    errors.extend(verify_requirement_ownership(row))
    run_verification = verify_automatic_deposit_run_manifest(run_manifest, row)
    errors.extend(run_verification.errors)
    fixture_verification = verify_automatic_deposit_fixture_manifest(
        fixture_manifest,
        row,
    )
    errors.extend(fixture_verification.errors)
    if run_manifest.fixture_fingerprint != fixture_manifest.fixture_fingerprint:
        errors.append("RUN_AND_FIXTURE_FINGERPRINT_MISMATCH")
    if run_manifest.diagnostics_mode != fixture_manifest.diagnostics_mode:
        errors.append("RUN_AND_FIXTURE_DIAGNOSTICS_MODE_MISMATCH")
    errors.extend(
        verify_automatic_deposit_gameplay_observation(
            gameplay_observation,
            run_manifest,
            fixture_manifest,
        )
    )
    errors.extend(verify_artifact_recheck(run_manifest, artifact_collection))
    errors.extend(
        verify_carry_on_recheck(
            run_manifest,
            fixture_manifest,
            carry_on_collection,
        )
    )
    errors.extend(
        verify_automatic_deposit_java_contract_manifest(
            java_contract_manifest,
            run_manifest,
            row,
        )
    )
    if errors:
        return "VERIFIED_EVIDENCE_PREFLIGHT_FAILED", tuple(errors)
    return "VERIFIED_EVIDENCE_PREFLIGHT_VERIFIED", ()
