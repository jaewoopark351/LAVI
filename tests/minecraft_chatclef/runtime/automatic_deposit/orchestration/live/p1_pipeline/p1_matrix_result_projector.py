#20260901_kpopmodder: Project one matrix result while enforcing zero submission.
from __future__ import annotations

from ...matrix_run_result import AutomaticDepositMatrixRunResult
from ..result.p1_live_result_builder import p1_simple_failure_result
from .p1_manual_prepared_observations import P1ManualPreparedObservations


def project_p1_matrix_result(
    base: dict[str, object],
    matrix_result: AutomaticDepositMatrixRunResult,
    prepared: P1ManualPreparedObservations,
) -> dict[str, object]:
    if matrix_result.submit_call_count != 0:
        return p1_simple_failure_result(
            base,
            "P1_LIVE_MATRIX_SUBMIT_COUNT_INVALID",
        )
    return {
        **base,
        **matrix_result.as_mapping(),
        "submit_call_count": 0,
        "runtime_observation_bundle_fingerprint": (
            prepared.runtime_observation_bundle.bundle_fingerprint
        ),
        "fixture_observation_fingerprint": (
            prepared.fixture_observation.observation_fingerprint
        ),
    }
