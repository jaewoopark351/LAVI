#20260901_kpopmodder: Seal the three already-collected post-action P1 observations.
from __future__ import annotations

from dataclasses import dataclass, field

from ....evidence.gameplay_observation_manifest import (
    AutomaticDepositGameplayObservationManifest,
)
from ....evidence.latest_log_delta_result import AutomaticDepositLatestLogDeltaResult
from ....evidence.operator_action.manual_operator_action_observation import (
    ManualOperatorActionObservation,
)


_P1_MANUAL_FINAL_OBSERVATIONS_SEAL = object()


@dataclass(frozen=True, slots=True)
class P1ManualFinalObservations:
    operator_action: ManualOperatorActionObservation
    log_delta: AutomaticDepositLatestLogDeltaResult
    gameplay_observation: AutomaticDepositGameplayObservationManifest
    _seal: object = field(default=None, repr=False, compare=False)

    def __post_init__(self) -> None:
        expected = (
            (self.operator_action, ManualOperatorActionObservation),
            (self.log_delta, AutomaticDepositLatestLogDeltaResult),
            (
                self.gameplay_observation,
                AutomaticDepositGameplayObservationManifest,
            ),
        )
        if self._seal is not _P1_MANUAL_FINAL_OBSERVATIONS_SEAL or any(
            not isinstance(value, expected_type)
            for value, expected_type in expected
        ):
            raise ValueError("P1 final observations must be pipeline-created")


def _create_p1_manual_final_observations(
    *,
    operator_action: ManualOperatorActionObservation,
    log_delta: AutomaticDepositLatestLogDeltaResult,
    gameplay_observation: AutomaticDepositGameplayObservationManifest,
) -> P1ManualFinalObservations:
    return P1ManualFinalObservations(
        operator_action=operator_action,
        log_delta=log_delta,
        gameplay_observation=gameplay_observation,
        _seal=_P1_MANUAL_FINAL_OBSERVATIONS_SEAL,
    )
