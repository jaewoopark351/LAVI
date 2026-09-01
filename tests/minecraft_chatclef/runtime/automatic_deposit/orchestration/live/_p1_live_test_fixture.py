#20260901_kpopmodder: Build one fully sealed P1 live-pipeline test context.
from __future__ import annotations

from dataclasses import dataclass, replace
from urllib.parse import quote

from ...evidence.artifact_identity_collection import (
    AutomaticDepositArtifactIdentityCollection,
)
from ...evidence.assembly.store_home.runtime_observation._test_fixture import (
    sealed_inputs,
)
from ...evidence.carry_on_identity_collection import (
    AutomaticDepositCarryOnIdentityCollection,
)
from ...evidence.fixture_manifest import AutomaticDepositFixtureManifest
from ...evidence.fixture_manifest_fingerprint import (
    automatic_deposit_fixture_fingerprint,
)
from ...evidence.gameplay_observation_manifest import (
    AutomaticDepositGameplayObservationManifest,
)
from ...evidence.latest_log_byte_cursor import (
    automatic_deposit_latest_log_cursor_fingerprint,
)
from ...evidence.latest_log_delta_result import (
    AutomaticDepositLatestLogDeltaResult,
    _create_latest_log_delta_result,
)
from ...evidence.live_fixture.p1_live_fixture_observation import (
    P1LiveFixtureObservation,
    _create_p1_live_fixture_observation,
)
from ...evidence.operator_action.manual_operator_action_collector import (
    collect_manual_operator_action_observation,
)
from ...evidence.operator_action.manual_operator_action_observation import (
    ManualOperatorActionObservation,
)
from ...evidence.run_manifest import AutomaticDepositRunManifest
from ...evidence.runtime_observer.log_prefix.jvm_runtime_artifact_log_prefix_observation import (
    JvmRuntimeArtifactLogPrefixObservation,
)
from ...evidence.runtime_observer.production_status_observation import (
    ProductionFabricStatusObservation,
)
from ...scenario.matrix_catalog import automatic_deposit_matrix_catalog
from ...scenario.matrix_row import AutomaticDepositMatrixRow
from ...testing.hermetic_evidence import (
    hermetic_artifact_collection,
    hermetic_carry_on_collection,
    hermetic_fixture,
    hermetic_gameplay_observation,
    hermetic_run_manifest,
)


@dataclass(frozen=True, slots=True)
class P1LiveTestContext:
    row: AutomaticDepositMatrixRow
    fixture: AutomaticDepositFixtureManifest
    run: AutomaticDepositRunManifest
    status: ProductionFabricStatusObservation
    artifact_collection: AutomaticDepositArtifactIdentityCollection
    carry_on_collection: AutomaticDepositCarryOnIdentityCollection
    log_prefix: JvmRuntimeArtifactLogPrefixObservation
    fixture_observation: P1LiveFixtureObservation
    operator_action: ManualOperatorActionObservation
    log_delta: AutomaticDepositLatestLogDeltaResult
    gameplay_observation: AutomaticDepositGameplayObservationManifest


def p1_live_test_context(
    *,
    fixture_ready: bool,
    fixture_observation: P1LiveFixtureObservation | None = None,
) -> P1LiveTestContext:
    row = next(row for row in automatic_deposit_matrix_catalog() if row.row_id == "P1")
    fixture_observation = fixture_observation or _fixture_observation(
        ready=fixture_ready
    )
    fixture, run = _matching_manifests(row, fixture_observation)
    status, log_prefix, _unused_run = sealed_inputs(
        command_context_available=False,
    )
    operator_action, reason = collect_manual_operator_action_observation(
        run_id=run.run_id,
        row_id=row.row_id,
        harness_operation_id=run.operation_id,
        action_text="@store_home",
        observation_source="OPERATOR_ACTION_OBSERVER",
        observer_identity_fingerprint="d" * 64,
        operator_confirmed=True,
    )
    if operator_action is None:
        raise AssertionError(reason)
    return P1LiveTestContext(
        row=row,
        fixture=fixture,
        run=run,
        status=status,
        artifact_collection=hermetic_artifact_collection(row),
        carry_on_collection=hermetic_carry_on_collection(row),
        log_prefix=log_prefix,
        fixture_observation=fixture_observation,
        operator_action=operator_action,
        log_delta=_production_delta(run, fixture, log_prefix),
        gameplay_observation=hermetic_gameplay_observation(run, fixture),
    )


def _matching_manifests(
    row: AutomaticDepositMatrixRow,
    observation: P1LiveFixtureObservation,
) -> tuple[AutomaticDepositFixtureManifest, AutomaticDepositRunManifest]:
    base = hermetic_fixture(row)
    observed = observation.as_fixture_evidence_mapping()
    attributes = tuple(
        (key, str(observed.get(key, value))) for key, value in base.attributes
    )
    provisional = replace(
        base,
        fixture_fingerprint="0" * 64,
        world_snapshot_id=observation.world_snapshot_fingerprint,
        inventory_snapshot_id=observation.player_inventory_fingerprint,
        container_type=str(observed["container_type"]),
        trusted_destination_fingerprint=(
            observation.trusted_destination_fingerprint
        ),
        attributes=attributes,
    )
    fixture = replace(
        provisional,
        fixture_fingerprint=automatic_deposit_fixture_fingerprint(provisional),
    )
    run = replace(
        hermetic_run_manifest(row, fixture),
        world=observation.world_key,
    )
    return fixture, run


def _fixture_observation(*, ready: bool) -> P1LiveFixtureObservation:
    limitations = () if ready else ("RUNNING_JVM_SAVE_BARRIER_NOT_AVAILABLE",)
    return _create_p1_live_fixture_observation(
        schema_version="p1-live-fixture-observation/v1",
        verdict="PASS" if ready else "INCONCLUSIVE",
        reason=(
            "P1_LIVE_FIXTURE_READY"
            if ready
            else "RUNNING_JVM_SAVE_BARRIER_NOT_AVAILABLE"
        ),
        world_directory="C:/minecraft/instance/saves/P1 fixture",
        world_key="singleplayer:P1 fixture",
        dimension="OVERWORLD",
        trusted_position=(12, 64, -9),
        trusted_destination_id="td_ready",
        trusted_destination_fingerprint="e" * 64,
        container_type="minecraft:chest",
        container_capacity_slots=27,
        container_occupied_slots=1,
        container_empty_slots=26,
        container_has_capacity=True,
        container_inventory_fingerprint="c" * 64,
        player_inventory_counts=(("minecraft:cobblestone", 32),),
        player_inventory_fingerprint="b" * 64,
        level_dat_path="C:/minecraft/instance/saves/P1 fixture/level.dat",
        level_dat_sha256="1" * 64,
        level_dat_size=128,
        level_dat_mtime_ns=10,
        player_data_path=(
            "C:/minecraft/instance/saves/P1 fixture/playerdata/player.dat"
        ),
        player_data_sha256="2" * 64,
        player_data_size=256,
        player_data_mtime_ns=11,
        trusted_registry_path="C:/minecraft/instance/config/lavi/trusted.json",
        trusted_registry_sha256="3" * 64,
        trusted_registry_size=512,
        trusted_registry_mtime_ns=12,
        region_path=(
            "C:/minecraft/instance/saves/P1 fixture/region/r.0.-1.mca"
        ),
        region_sha256="4" * 64,
        region_size=8192,
        region_mtime_ns=13,
        region_chunk_timestamp=1_777_777_777,
        world_snapshot_fingerprint="a" * 64,
        limitations=limitations,
    )


def _production_delta(
    run: AutomaticDepositRunManifest,
    fixture: AutomaticDepositFixtureManifest,
    log_prefix: JvmRuntimeArtifactLogPrefixObservation,
) -> AutomaticDepositLatestLogDeltaResult:
    manifest_id = log_prefix.runtime_artifact_observation.run_manifest_id
    position = fixture.as_evidence_mapping()["trusted_container_position"]
    text = (
        "\n".join(
            (
                _store_home_line(
                    10,
                    "STORE_HOME_OPERATION_STARTED",
                    "phase=START",
                    manifest_id,
                ),
                _store_home_line(
                    11,
                    "STORE_HOME_CANDIDATE_ACTIVATED",
                    "candidateActivated=true "
                    "activationResult=EXACT_SESSION_INSTALLED "
                    "destinationId=trusted-home-1 "
                    f"candidatePosition={quote(str(position), safe='-._~')}",
                    manifest_id,
                ),
                _store_home_line(
                    12,
                    "STORE_HOME_OPERATION_TERMINAL_SUMMARY",
                    "terminalResult=COMPLETED operationResult=COMPLETED "
                    "remainingStackCount=0 storedItems=64 touchedStackCount=2",
                    manifest_id,
                ),
            )
        )
        + "\n"
    )
    start = run.latest_log_cursor.size
    return _create_latest_log_delta_result(
        True,
        "LATEST_LOG_DELTA_READ",
        None,
        start,
        start + len(text.encode("utf-8")),
        text,
        "utf-8",
        automatic_deposit_latest_log_cursor_fingerprint(run.latest_log_cursor),
    )


def _store_home_line(
    sequence: int,
    event: str,
    extra: str,
    run_manifest_id: str,
) -> str:
    return (
        "[01:04:05] [Render thread/INFO]: [STDOUT]: ALTO CLEF: "
        "[LAVI ChatClefBoundary] traceId=trace-p1 clientTickId=812 "
        f"eventSequence={sequence} taskInstanceId=task-1 taskRunId=run-1 "
        "parentTaskRunId=unavailable threadName=Render%20thread level=BOUNDARY "
        f"event={event} reason=runtime_test taskClass=lavi.StoreHomeTask "
        f"runManifestId={quote(run_manifest_id, safe='-._~')} operationId=17 "
        "commandContextAvailable=false commandRequestId=~EMPTY~ "
        "commandCorrelationId=~EMPTY~ commandSessionId=~EMPTY~ "
        "commandConnectionGeneration=unavailable commandText=~EMPTY~ "
        "commandSource=~EMPTY~ commandContextError=no_active_command "
        f"{extra} diagnosticCaptureStatus=complete"
    )
