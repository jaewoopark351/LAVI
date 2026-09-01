#20260901_kpopmodder: Read only the already-recorded P1 action, log, and gameplay evidence.
from __future__ import annotations

from ....evidence.gameplay_observation_manifest import (
    AutomaticDepositGameplayObservationManifest,
)
from ....evidence.latest_log_delta_result import AutomaticDepositLatestLogDeltaResult
from ....evidence.operator_action.manual_operator_action_observation import (
    ManualOperatorActionObservation,
)
from ..result.p1_live_result_builder import p1_observer_failure_result
from .p1_manual_final_observations import (
    P1ManualFinalObservations,
    _create_p1_manual_final_observations,
)
from ..p1_manual_observer_dependencies import P1ManualObserverDependencies
from ..p1_typed_observer_reader import read_typed_p1_observation


def observe_p1_post_action_evidence(
    base: dict[str, object],
    dependencies: P1ManualObserverDependencies,
) -> tuple[P1ManualFinalObservations | None, dict[str, object]]:
    operator_result = read_typed_p1_observation(
        "operator_action_reader",
        dependencies.operator_action_reader,
        ManualOperatorActionObservation,
    )
    if not operator_result.ok:
        return None, p1_observer_failure_result(base, operator_result)
    operator_action = operator_result.value
    assert isinstance(operator_action, ManualOperatorActionObservation)

    log_result = read_typed_p1_observation(
        "log_delta_reader",
        dependencies.log_delta_reader,
        AutomaticDepositLatestLogDeltaResult,
    )
    if not log_result.ok:
        return None, p1_observer_failure_result(base, log_result)
    log_delta = log_result.value
    assert isinstance(log_delta, AutomaticDepositLatestLogDeltaResult)

    gameplay_result = read_typed_p1_observation(
        "gameplay_reader",
        dependencies.gameplay_reader,
        AutomaticDepositGameplayObservationManifest,
    )
    if not gameplay_result.ok:
        return None, p1_observer_failure_result(base, gameplay_result)
    gameplay_observation = gameplay_result.value
    assert isinstance(
        gameplay_observation,
        AutomaticDepositGameplayObservationManifest,
    )

    return (
        _create_p1_manual_final_observations(
            operator_action=operator_action,
            log_delta=log_delta,
            gameplay_observation=gameplay_observation,
        ),
        {},
    )
