#20260901_kpopmodder: Route manual P1 through separated read-only evidence stages.
from __future__ import annotations

from collections.abc import Callable

from minecraft_chatclef.runtime.submission.guard_state import observe_one_shot_guard_state

from ...evidence.fixture_manifest import AutomaticDepositFixtureManifest
from ...evidence.run_manifest import AutomaticDepositRunManifest
from .p1_guard_gate import evaluate_p1_guard_gate
from .result.p1_live_result_builder import (
    p1_guard_failure_result,
    p1_simple_failure_result,
)
from .p1_manual_observer_dependencies import P1ManualObserverDependencies
from .p1_manual_observer_dependencies_contract import (
    missing_p1_manual_observer_dependencies,
)
from .p1_pipeline.p1_manual_finalization import (
    finalize_p1_manual_live_observations,
)
from .p1_pipeline.p1_manual_preparation import prepare_p1_manual_observations
from .p1_pipeline.p1_matrix_row_selector import select_p1_matrix_row


def run_p1_manual_live_application(
    base: dict[str, object],
    run_manifest: AutomaticDepositRunManifest,
    fixture_manifest: AutomaticDepositFixtureManifest,
    *,
    guard_state_reader: Callable[[str], object] | None,
    observer_dependencies: P1ManualObserverDependencies | None,
) -> dict[str, object]:
    """Finalize already-collected evidence without initiating a game action."""
    reader = (
        observe_one_shot_guard_state
        if guard_state_reader is None
        else guard_state_reader
    )
    initial_guard = evaluate_p1_guard_gate(run_manifest.repository_root, reader)
    if not initial_guard.clear:
        return p1_guard_failure_result(base, initial_guard)

    dependencies = (
        P1ManualObserverDependencies()
        if observer_dependencies is None
        else observer_dependencies
    )
    if not isinstance(dependencies, P1ManualObserverDependencies):
        return p1_simple_failure_result(
            base,
            "P1_LIVE_OBSERVER_DEPENDENCIES_NOT_TYPED",
        )
    missing = missing_p1_manual_observer_dependencies(dependencies)
    if missing:
        return p1_simple_failure_result(
            base,
            "P1_LIVE_OBSERVER_DEPENDENCIES_MISSING",
            missing_dependencies=list(missing),
        )

    row = select_p1_matrix_row()
    if row is None:
        return p1_simple_failure_result(base, "P1_MATRIX_ROW_NOT_EXACT")
    prepared, failure = prepare_p1_manual_observations(
        base,
        run_manifest,
        fixture_manifest,
        dependencies,
    )
    if prepared is None:
        return failure
    return finalize_p1_manual_live_observations(
        base,
        row,
        run_manifest,
        fixture_manifest,
        dependencies,
        prepared,
        reader,
    )
