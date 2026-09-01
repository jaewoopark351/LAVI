#20260901_kpopmodder: Own only opt-in, row, and typed manifest admission for live rows.
from __future__ import annotations

from collections.abc import Mapping

from ....preflight.runtime_opt_in import live_mutating_run_selected
from ...evidence.fixture_manifest import AutomaticDepositFixtureManifest
from ...evidence.fixture_manifest_contract import (
    verify_automatic_deposit_fixture_manifest,
)
from ...evidence.run_manifest import AutomaticDepositRunManifest
from ...evidence.run_manifest_contract import verify_automatic_deposit_run_manifest
from ...oracle.matrix_verdict import AutomaticDepositVerdict
from ...scenario.matrix_catalog import automatic_deposit_matrix_catalog
from .application_admission_result import (
    AutomaticDepositLiveApplicationAdmissionResult,
)


def admit_automatic_deposit_live_application(
    environment: Mapping[str, object],
    *,
    row_id: object,
    run_manifest: AutomaticDepositRunManifest | None,
    fixture_manifest: AutomaticDepositFixtureManifest | None,
) -> AutomaticDepositLiveApplicationAdmissionResult:
    normalized_row_id = str(row_id or "").strip()
    base = {
        "schema_version": "automatic-deposit-live-result/v1",
        "row_id": normalized_row_id,
        "verdict": AutomaticDepositVerdict.INCONCLUSIVE.value,
        "submit_call_count": 0,
    }
    if not live_mutating_run_selected(environment):
        return _failure(base, "LIVE_RUNTIME_OPT_IN_NOT_SELECTED")
    rows = tuple(
        row
        for row in automatic_deposit_matrix_catalog()
        if row.row_id == normalized_row_id
    )
    if len(rows) != 1:
        return _failure(base, "EXACT_MATRIX_ROW_NOT_SELECTED")
    if run_manifest is None or fixture_manifest is None:
        return _failure(base, "RUN_OR_FIXTURE_MANIFEST_MISSING")
    if not isinstance(run_manifest, AutomaticDepositRunManifest):
        return _failure(base, "RUN_MANIFEST_NOT_TYPED")
    if not isinstance(fixture_manifest, AutomaticDepositFixtureManifest):
        return _failure(base, "FIXTURE_MANIFEST_NOT_TYPED")
    row = rows[0]
    run_verification = verify_automatic_deposit_run_manifest(run_manifest, row)
    if not run_verification.ok:
        return _failure(
            base,
            "RUN_MANIFEST_NOT_VERIFIED",
            errors=list(run_verification.errors),
        )
    fixture_verification = verify_automatic_deposit_fixture_manifest(
        fixture_manifest,
        row,
    )
    if not fixture_verification.ok:
        return _failure(
            base,
            "FIXTURE_MANIFEST_NOT_VERIFIED",
            errors=list(fixture_verification.errors),
        )
    if run_manifest.fixture_fingerprint != fixture_manifest.fixture_fingerprint:
        return _failure(base, "RUN_AND_FIXTURE_FINGERPRINT_MISMATCH")
    if run_manifest.diagnostics_mode != fixture_manifest.diagnostics_mode:
        return _failure(base, "RUN_AND_FIXTURE_DIAGNOSTICS_MODE_MISMATCH")
    return AutomaticDepositLiveApplicationAdmissionResult(
        ok=True,
        row=row,
        result=base,
    )


def _failure(
    base: dict[str, object],
    reason: str,
    **details: object,
) -> AutomaticDepositLiveApplicationAdmissionResult:
    return AutomaticDepositLiveApplicationAdmissionResult(
        ok=False,
        row=None,
        result={**base, "reason": reason, **details},
    )
