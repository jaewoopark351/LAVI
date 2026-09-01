#20260831_kpopmodder: Fail closed before live transport without full evidence.
from __future__ import annotations

from collections.abc import Callable, Mapping

from ..evidence.fixture_manifest import AutomaticDepositFixtureManifest
from ..evidence.run_manifest import AutomaticDepositRunManifest
from .live.application_admission import admit_automatic_deposit_live_application
from .live.p1_manual_application import run_p1_manual_live_application
from .live.p1_manual_observer_dependencies import P1ManualObserverDependencies


def run_automatic_deposit_live_matrix_application(
    environment: Mapping[str, object],
    *,
    row_id: object = "",
    run_manifest: AutomaticDepositRunManifest | None = None,
    fixture_manifest: AutomaticDepositFixtureManifest | None = None,
    guard_state_reader: Callable[[str], object] | None = None,
    p1_observer_dependencies: P1ManualObserverDependencies | None = None,
) -> dict[str, object]:
    admission = admit_automatic_deposit_live_application(
        environment,
        row_id=row_id,
        run_manifest=run_manifest,
        fixture_manifest=fixture_manifest,
    )
    if not admission.ok:
        return admission.result
    assert admission.row is not None
    assert isinstance(run_manifest, AutomaticDepositRunManifest)
    assert isinstance(fixture_manifest, AutomaticDepositFixtureManifest)
    if admission.row.row_id == "P1":
        return run_p1_manual_live_application(
            admission.result,
            run_manifest,
            fixture_manifest,
            guard_state_reader=guard_state_reader,
            observer_dependencies=p1_observer_dependencies,
        )
    return {
        **admission.result,
        "reason": "LIVE_TRANSPORT_AND_RUNTIME_OBSERVER_NOT_CONFIGURED",
    }
