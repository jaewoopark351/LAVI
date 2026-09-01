#20260901_kpopmodder: Coordinate separated P1 matrix evaluation and result projection.
from __future__ import annotations

from ....evidence.fixture_manifest import AutomaticDepositFixtureManifest
from ....evidence.run_manifest import AutomaticDepositRunManifest
from ....scenario.matrix_row import AutomaticDepositMatrixRow
from .p1_manual_final_observations import P1ManualFinalObservations
from .p1_manual_prepared_observations import P1ManualPreparedObservations
from .p1_matrix_evaluator import evaluate_p1_matrix_evidence
from .p1_matrix_result_projector import project_p1_matrix_result


def finalize_p1_matrix_evidence(
    base: dict[str, object],
    row: AutomaticDepositMatrixRow,
    run_manifest: AutomaticDepositRunManifest,
    fixture_manifest: AutomaticDepositFixtureManifest,
    prepared: P1ManualPreparedObservations,
    final: P1ManualFinalObservations,
) -> dict[str, object]:
    matrix_result = evaluate_p1_matrix_evidence(
        row,
        run_manifest,
        fixture_manifest,
        prepared,
        final,
    )
    return project_p1_matrix_result(base, matrix_result, prepared)
