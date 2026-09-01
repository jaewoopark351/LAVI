#20260901_kpopmodder: Adapt one sealed production log delta into fail-closed P1 StoreHome evidence.
from __future__ import annotations

from ..production_log_delta_scanner import scan_production_diagnostic_delta
from .p1_runtime_evidence_projector import project_p1_store_home_runtime_evidence
from .projection_result import (
    P1_STORE_HOME_FALSE_EVIDENCE,
    P1StoreHomeRuntimeEvidenceProjection,
)


def adapt_p1_store_home_runtime_log(
    delta: object,
    *,
    expected_candidate_position: object,
    expected_run_manifest_id: object,
) -> P1StoreHomeRuntimeEvidenceProjection:
    scan = scan_production_diagnostic_delta(delta)
    if not scan.ok:
        reason_parts = (
            "P1_STORE_HOME_PRODUCTION_LOG_SCAN_FAILED",
            scan.reason,
            scan.failure_detail,
        )
        return P1StoreHomeRuntimeEvidenceProjection(
            ok=False,
            reason=":".join(part for part in reason_parts if part),
            operation_id="",
            command_request_id="",
            evidence=P1_STORE_HOME_FALSE_EVIDENCE,
        )
    return project_p1_store_home_runtime_evidence(
        scan.records,
        expected_candidate_position=expected_candidate_position,
        expected_run_manifest_id=expected_run_manifest_id,
    )
