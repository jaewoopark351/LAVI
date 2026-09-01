#20260901_kpopmodder: Finalize already-collected P1 evidence behind a second guard read.
from __future__ import annotations

from collections.abc import Callable

from ....evidence.fixture_manifest import AutomaticDepositFixtureManifest
from ....evidence.run_manifest import AutomaticDepositRunManifest
from ....scenario.matrix_row import AutomaticDepositMatrixRow
from ..p1_guard_gate import evaluate_p1_guard_gate
from ..result.p1_live_result_builder import p1_guard_failure_result
from ..p1_manual_observer_dependencies import P1ManualObserverDependencies
from .p1_manual_prepared_observations import P1ManualPreparedObservations
from .p1_matrix_finalizer import finalize_p1_matrix_evidence
from .p1_post_action_observer import observe_p1_post_action_evidence


def finalize_p1_manual_live_observations(
    base: dict[str, object],
    row: AutomaticDepositMatrixRow,
    run_manifest: AutomaticDepositRunManifest,
    fixture_manifest: AutomaticDepositFixtureManifest,
    dependencies: P1ManualObserverDependencies,
    prepared: P1ManualPreparedObservations,
    guard_state_reader: Callable[[str], object],
) -> dict[str, object]:
    final, failure = observe_p1_post_action_evidence(base, dependencies)
    if final is None:
        return failure

    final_guard = evaluate_p1_guard_gate(
        run_manifest.repository_root,
        guard_state_reader,
    )
    if not final_guard.clear:
        return p1_guard_failure_result(base, final_guard)

    return finalize_p1_matrix_evidence(
        base,
        row,
        run_manifest,
        fixture_manifest,
        prepared,
        final,
    )
