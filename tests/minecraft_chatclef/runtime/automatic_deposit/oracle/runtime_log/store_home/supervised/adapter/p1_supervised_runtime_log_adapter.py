#20260901_kpopmodder: Apply the shared production scanner before supervised StoreHome projection.
from __future__ import annotations

from ....production_log_delta_scanner import scan_production_diagnostic_delta
from ...projection_result import (
    P1_STORE_HOME_FALSE_EVIDENCE,
    P1StoreHomeRuntimeEvidenceProjection,
)
from ..projection.p1_supervised_runtime_evidence_projector import (
    project_p1_supervised_store_home_runtime_evidence,
)


def adapt_p1_supervised_store_home_runtime_log(
    delta: object,
    *,
    expected_candidate_position: object,
    expected_run_manifest_id: object,
    expected_command_request_id: object,
    expected_command_session_id: object,
    expected_world_key: object,
    expected_dimension: object,
    expected_destination_canonical_key: object,
    expected_destination_id: object,
) -> P1StoreHomeRuntimeEvidenceProjection:
    scan = scan_production_diagnostic_delta(delta)
    if not scan.ok:
        parts = (
            "P1_SUPERVISED_PRODUCTION_LOG_SCAN_FAILED",
            scan.reason,
            scan.failure_detail,
        )
        return P1StoreHomeRuntimeEvidenceProjection(
            ok=False,
            reason=":".join(part for part in parts if part),
            operation_id="",
            command_request_id="",
            evidence=P1_STORE_HOME_FALSE_EVIDENCE,
        )
    return project_p1_supervised_store_home_runtime_evidence(
        scan.records,
        expected_candidate_position=expected_candidate_position,
        expected_run_manifest_id=expected_run_manifest_id,
        expected_command_request_id=expected_command_request_id,
        expected_command_session_id=expected_command_session_id,
        expected_world_key=expected_world_key,
        expected_dimension=expected_dimension,
        expected_destination_canonical_key=(
            expected_destination_canonical_key
        ),
        expected_destination_id=expected_destination_id,
    )
