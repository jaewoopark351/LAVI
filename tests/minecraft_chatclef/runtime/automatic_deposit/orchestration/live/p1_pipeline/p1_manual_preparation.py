#20260901_kpopmodder: Orchestrate the ordered read-only P1 pre-action evidence gates.
from __future__ import annotations

from ....evidence.artifact_identity_collection import (
    AutomaticDepositArtifactIdentityCollection,
)
from ....evidence.assembly.store_home.runtime_observation.p1_runtime_observation_bundle import (
    P1RuntimeObservationBundle,
)
from ....evidence.assembly.store_home.runtime_observation.p1_runtime_observation_bundle_collector import (
    collect_p1_runtime_observation_bundle,
)
from ....evidence.carry_on_identity_collection import (
    AutomaticDepositCarryOnIdentityCollection,
)
from ....evidence.fixture_manifest import AutomaticDepositFixtureManifest
from ....evidence.live_fixture.p1_live_fixture_binding_contract import (
    verify_p1_live_fixture_binding,
)
from ....evidence.live_fixture.p1_live_fixture_observation import (
    P1LiveFixtureObservation,
)
from ....evidence.preflight.artifact_recheck import verify_artifact_recheck
from ....evidence.preflight.carry_on_recheck import verify_carry_on_recheck
from ....evidence.run_manifest import AutomaticDepositRunManifest
from ....evidence.runtime_observer.log_prefix.jvm_runtime_artifact_log_prefix_observation import (
    JvmRuntimeArtifactLogPrefixObservation,
)
from ....evidence.runtime_observer.production_status_observation import (
    ProductionFabricStatusObservation,
)
from ..result.p1_live_result_builder import (
    p1_contract_failure_result,
    p1_fixture_failure_result,
    p1_observer_failure_result,
)
from ..p1_manual_observer_dependencies import P1ManualObserverDependencies
from .p1_manual_prepared_observations import (
    P1ManualPreparedObservations,
    _create_p1_manual_prepared_observations,
)
from ..p1_typed_observer_reader import read_typed_p1_observation


def prepare_p1_manual_observations(
    base: dict[str, object],
    run_manifest: AutomaticDepositRunManifest,
    fixture_manifest: AutomaticDepositFixtureManifest,
    dependencies: P1ManualObserverDependencies,
) -> tuple[P1ManualPreparedObservations | None, dict[str, object]]:
    status_result = read_typed_p1_observation(
        "status_reader",
        dependencies.status_reader,
        ProductionFabricStatusObservation,
    )
    if not status_result.ok:
        return None, p1_observer_failure_result(base, status_result)
    status = status_result.value
    assert isinstance(status, ProductionFabricStatusObservation)

    artifact_result = read_typed_p1_observation(
        "artifact_recheck_reader",
        dependencies.artifact_recheck_reader,
        AutomaticDepositArtifactIdentityCollection,
    )
    if not artifact_result.ok:
        return None, p1_observer_failure_result(base, artifact_result)
    artifact_collection = artifact_result.value
    assert isinstance(
        artifact_collection,
        AutomaticDepositArtifactIdentityCollection,
    )
    artifact_errors = verify_artifact_recheck(run_manifest, artifact_collection)
    if artifact_errors:
        return None, p1_contract_failure_result(
            base,
            "P1_ARTIFACT_RECHECK_NOT_VERIFIED",
            artifact_errors,
        )

    carry_on_result = read_typed_p1_observation(
        "carry_on_recheck_reader",
        dependencies.carry_on_recheck_reader,
        AutomaticDepositCarryOnIdentityCollection,
    )
    if not carry_on_result.ok:
        return None, p1_observer_failure_result(base, carry_on_result)
    carry_on_collection = carry_on_result.value
    assert isinstance(
        carry_on_collection,
        AutomaticDepositCarryOnIdentityCollection,
    )
    carry_on_errors = verify_carry_on_recheck(
        run_manifest,
        fixture_manifest,
        carry_on_collection,
    )
    if carry_on_errors:
        return None, p1_contract_failure_result(
            base,
            "P1_CARRY_ON_RECHECK_NOT_VERIFIED",
            carry_on_errors,
        )

    log_prefix_result = read_typed_p1_observation(
        "runtime_artifact_reader",
        dependencies.runtime_artifact_reader,
        JvmRuntimeArtifactLogPrefixObservation,
        run_manifest.latest_log_cursor,
    )
    if not log_prefix_result.ok:
        return None, p1_observer_failure_result(base, log_prefix_result)
    log_prefix = log_prefix_result.value
    assert isinstance(log_prefix, JvmRuntimeArtifactLogPrefixObservation)

    bundle_result = read_typed_p1_observation(
        "runtime_observation_bundle",
        collect_p1_runtime_observation_bundle,
        P1RuntimeObservationBundle,
        status,
        log_prefix,
        run_manifest,
    )
    if not bundle_result.ok:
        return None, p1_observer_failure_result(base, bundle_result)
    runtime_bundle = bundle_result.value
    assert isinstance(runtime_bundle, P1RuntimeObservationBundle)

    fixture_result = read_typed_p1_observation(
        "trusted_fixture_reader",
        dependencies.trusted_fixture_reader,
        P1LiveFixtureObservation,
    )
    if not fixture_result.ok:
        return None, p1_observer_failure_result(base, fixture_result)
    fixture_observation = fixture_result.value
    assert isinstance(fixture_observation, P1LiveFixtureObservation)
    fixture_verification = verify_p1_live_fixture_binding(
        fixture_observation,
        run_manifest,
        fixture_manifest,
    )
    if not fixture_verification.ok:
        return None, p1_fixture_failure_result(base, fixture_verification)

    return (
        _create_p1_manual_prepared_observations(
            artifact_collection=artifact_collection,
            carry_on_collection=carry_on_collection,
            runtime_observation_bundle=runtime_bundle,
            fixture_observation=fixture_observation,
        ),
        {},
    )
