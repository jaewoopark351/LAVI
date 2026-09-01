#20260901_kpopmodder: Seal the verified pre-action inputs for manual P1 finalization.
from __future__ import annotations

from dataclasses import dataclass, field

from ....evidence.artifact_identity_collection import (
    AutomaticDepositArtifactIdentityCollection,
)
from ....evidence.assembly.store_home.runtime_observation.p1_runtime_observation_bundle import (
    P1RuntimeObservationBundle,
)
from ....evidence.carry_on_identity_collection import (
    AutomaticDepositCarryOnIdentityCollection,
)
from ....evidence.live_fixture.p1_live_fixture_observation import (
    P1LiveFixtureObservation,
)


_P1_MANUAL_PREPARATION_SEAL = object()


@dataclass(frozen=True, slots=True)
class P1ManualPreparedObservations:
    artifact_collection: AutomaticDepositArtifactIdentityCollection
    carry_on_collection: AutomaticDepositCarryOnIdentityCollection
    runtime_observation_bundle: P1RuntimeObservationBundle
    fixture_observation: P1LiveFixtureObservation
    _seal: object = field(default=None, repr=False, compare=False)

    def __post_init__(self) -> None:
        expected = (
            (self.artifact_collection, AutomaticDepositArtifactIdentityCollection),
            (self.carry_on_collection, AutomaticDepositCarryOnIdentityCollection),
            (self.runtime_observation_bundle, P1RuntimeObservationBundle),
            (self.fixture_observation, P1LiveFixtureObservation),
        )
        if self._seal is not _P1_MANUAL_PREPARATION_SEAL or any(
            not isinstance(value, expected_type)
            for value, expected_type in expected
        ):
            raise ValueError("P1 manual preparation must be pipeline-created")


def _create_p1_manual_prepared_observations(
    *,
    artifact_collection: AutomaticDepositArtifactIdentityCollection,
    carry_on_collection: AutomaticDepositCarryOnIdentityCollection,
    runtime_observation_bundle: P1RuntimeObservationBundle,
    fixture_observation: P1LiveFixtureObservation,
) -> P1ManualPreparedObservations:
    return P1ManualPreparedObservations(
        artifact_collection=artifact_collection,
        carry_on_collection=carry_on_collection,
        runtime_observation_bundle=runtime_observation_bundle,
        fixture_observation=fixture_observation,
        _seal=_P1_MANUAL_PREPARATION_SEAL,
    )
