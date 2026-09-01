#20260901_kpopmodder: Evaluate one sealed P1 evidence set without submit capability.
from __future__ import annotations

from ....evidence.fixture_manifest import AutomaticDepositFixtureManifest
from ....evidence.run_manifest import AutomaticDepositRunManifest
from ....scenario.matrix_row import AutomaticDepositMatrixRow
from ...matrix_run_result import AutomaticDepositMatrixRunResult
from ...matrix_runner import run_automatic_deposit_matrix_row
from .p1_manual_final_observations import P1ManualFinalObservations
from .p1_manual_prepared_observations import P1ManualPreparedObservations


def evaluate_p1_matrix_evidence(
    row: AutomaticDepositMatrixRow,
    run_manifest: AutomaticDepositRunManifest,
    fixture_manifest: AutomaticDepositFixtureManifest,
    prepared: P1ManualPreparedObservations,
    final: P1ManualFinalObservations,
) -> AutomaticDepositMatrixRunResult:
    return run_automatic_deposit_matrix_row(
        row,
        run_manifest,
        fixture_manifest,
        prepared.artifact_collection,
        selected_modes=(row.transport_mode,),
        log_delta=final.log_delta,
        gameplay_observation=final.gameplay_observation,
        carry_on_collection=prepared.carry_on_collection,
        p1_runtime_observation_bundle=prepared.runtime_observation_bundle,
        p1_live_fixture_observation=prepared.fixture_observation,
        operator_action_observation=final.operator_action,
    )
