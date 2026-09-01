#20260831_kpopmodder: Orchestrate only verified evidence and an audited submit boundary.
from __future__ import annotations

from collections.abc import Sequence

from ..evidence.artifact_identity_collection import (
    AutomaticDepositArtifactIdentityCollection,
)
from ..evidence.carry_on_identity_collection import (
    AutomaticDepositCarryOnIdentityCollection,
)
from ..evidence.fixture_manifest import AutomaticDepositFixtureManifest
from ..evidence.gameplay_observation_manifest import (
    AutomaticDepositGameplayObservationManifest,
)
from ..evidence.java_contract_manifest import AutomaticDepositJavaContractManifest
from ..evidence.latest_log_delta_result import AutomaticDepositLatestLogDeltaResult
from ..evidence.live_fixture.p1_live_fixture_observation import (
    P1LiveFixtureObservation,
)
from ..evidence.operator_action.manual_operator_action_observation import (
    ManualOperatorActionObservation,
)
from ..evidence.run_manifest import AutomaticDepositRunManifest
from ..evidence.assembly.store_home.runtime_observation.p1_runtime_observation_bundle import (
    P1RuntimeObservationBundle,
)
from ..evidence.verified_evidence_builder import (
    build_automatic_deposit_verified_evidence,
    build_p1_store_home_verified_evidence,
    verify_automatic_deposit_evidence_preflight,
)
from ..oracle.matrix_verdict import AutomaticDepositVerdict
from ..oracle.row_verdict import evaluate_automatic_deposit_row
from ..scenario.matrix_row import AutomaticDepositMatrixRow
from ..scenario.transport_mode import AutomaticDepositTransportMode
from .matrix_run_result import AutomaticDepositMatrixRunResult

def run_automatic_deposit_matrix_row(
    row: object,
    run_manifest: AutomaticDepositRunManifest,
    fixture_manifest: AutomaticDepositFixtureManifest,
    artifact_collection: AutomaticDepositArtifactIdentityCollection,
    *,
    selected_modes: Sequence[AutomaticDepositTransportMode],
    log_delta: AutomaticDepositLatestLogDeltaResult | None = None,
    gameplay_observation: AutomaticDepositGameplayObservationManifest | None = None,
    carry_on_collection: AutomaticDepositCarryOnIdentityCollection | None = None,
    java_contract_manifest: AutomaticDepositJavaContractManifest | None = None,
    p1_runtime_observation_bundle: P1RuntimeObservationBundle | None = None,
    p1_live_fixture_observation: P1LiveFixtureObservation | None = None,
    operator_action_observation: ManualOperatorActionObservation | None = None,
) -> AutomaticDepositMatrixRunResult:
    submit_call_count = 0
    if not isinstance(row, AutomaticDepositMatrixRow):
        return AutomaticDepositMatrixRunResult(
            row_id="",
            transport_mode=None,
            verdict=AutomaticDepositVerdict.INCONCLUSIVE,
            reason="MATRIX_ROW_NOT_TYPED",
            submit_call_count=submit_call_count,
        )
    if not isinstance(run_manifest, AutomaticDepositRunManifest):
        return _inconclusive(
            row,
            "RUN_MANIFEST_NOT_TYPED",
            submit_call_count,
            "",
        )
    if not isinstance(fixture_manifest, AutomaticDepositFixtureManifest):
        return _inconclusive(
            row,
            "FIXTURE_MANIFEST_NOT_TYPED",
            submit_call_count,
            run_manifest.operator_action_fingerprint,
            run_manifest.submission_invocation_fingerprint,
        )
    if not isinstance(
        artifact_collection,
        AutomaticDepositArtifactIdentityCollection,
    ):
        return _inconclusive(
            row,
            "ARTIFACT_RECHECK_NOT_TYPED",
            submit_call_count,
            run_manifest.operator_action_fingerprint,
            run_manifest.submission_invocation_fingerprint,
        )
    operator_fingerprint = run_manifest.operator_action_fingerprint
    submission_fingerprint = run_manifest.submission_invocation_fingerprint

    normalized_modes, mode_error = _normalize_modes(selected_modes)
    if mode_error:
        return _inconclusive(
            row,
            mode_error,
            submit_call_count,
            operator_fingerprint,
            submission_fingerprint,
        )
    selected_mode = normalized_modes[0]
    if selected_mode is not row.transport_mode:
        return _inconclusive(
            row,
            "TRANSPORT_MODE_DOES_NOT_MATCH_ROW",
            submit_call_count,
            operator_fingerprint,
            submission_fingerprint,
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
        return _inconclusive(
            row,
            preflight_reason,
            submit_call_count,
            operator_fingerprint,
            submission_fingerprint,
            violated_contracts=preflight_errors,
        )
    if log_delta is None:
        return _inconclusive(
            row,
            "LATEST_LOG_DELTA_NOT_CONFIGURED",
            submit_call_count,
            operator_fingerprint,
            submission_fingerprint,
        )

    if selected_mode is AutomaticDepositTransportMode.SUPERVISED_LAVI_SUBMIT:
        return _inconclusive(
            row,
            "SUPERVISED_REQUIRES_COMMON_LIVE_PIPELINE",
            submit_call_count,
            operator_fingerprint,
            submission_fingerprint,
        )

    if row.row_id == "P1":
        verified = build_p1_store_home_verified_evidence(
            row,
            run_manifest,
            fixture_manifest,
            artifact_collection,
            log_delta,
            gameplay_observation,
            carry_on_collection=carry_on_collection,
            p1_runtime_observation_bundle=p1_runtime_observation_bundle,
            p1_live_fixture_observation=p1_live_fixture_observation,
            operator_action_observation=operator_action_observation,
        )
    else:
        verified = build_automatic_deposit_verified_evidence(
            row,
            run_manifest,
            fixture_manifest,
            artifact_collection,
            log_delta,
            gameplay_observation,
            carry_on_collection=carry_on_collection,
            java_contract_manifest=java_contract_manifest,
        )
    if not verified.ok or verified.evidence is None:
        return _inconclusive(
            row,
            verified.reason,
            submit_call_count,
            operator_fingerprint,
            submission_fingerprint,
            violated_contracts=verified.errors,
        )

    verdict = evaluate_automatic_deposit_row(
        row,
        fixture_manifest,
        verified.evidence,
        helper_submit_call_count=submit_call_count,
    )
    return AutomaticDepositMatrixRunResult(
        row_id=row.row_id,
        transport_mode=row.transport_mode,
        verdict=verdict.verdict,
        reason=verdict.reason,
        submit_call_count=submit_call_count,
        operator_action_fingerprint=operator_fingerprint,
        submission_invocation_fingerprint=submission_fingerprint,
        missing_evidence=verdict.missing_evidence,
        violated_contracts=verdict.violated_contracts,
    )


def _normalize_modes(
    selected_modes: Sequence[AutomaticDepositTransportMode],
) -> tuple[tuple[AutomaticDepositTransportMode, ...], str]:
    try:
        normalized = tuple(selected_modes)
    except TypeError:
        return (), "TRANSPORT_MODE_SELECTION_NOT_ITERABLE"
    if len(normalized) != 1 or not isinstance(
        normalized[0], AutomaticDepositTransportMode
    ):
        return (), "EXACTLY_ONE_TYPED_TRANSPORT_MODE_REQUIRED"
    return normalized, ""


def _inconclusive(
    row: AutomaticDepositMatrixRow,
    reason: str,
    submit_call_count: int,
    operator_action_fingerprint: str,
    submission_invocation_fingerprint: str = "",
    *,
    violated_contracts: tuple[str, ...] = (),
) -> AutomaticDepositMatrixRunResult:
    return AutomaticDepositMatrixRunResult(
        row_id=row.row_id,
        transport_mode=row.transport_mode,
        verdict=AutomaticDepositVerdict.INCONCLUSIVE,
        reason=reason,
        submit_call_count=submit_call_count,
        operator_action_fingerprint=operator_action_fingerprint,
        submission_invocation_fingerprint=submission_invocation_fingerprint,
        violated_contracts=violated_contracts,
    )
