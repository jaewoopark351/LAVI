#20260901_kpopmodder: Provide supervised P1 execution and production-log fixtures shared by focused tests.
from __future__ import annotations

import hashlib

from minecraft_chatclef.runtime.automatic_deposit.evidence.gameplay_observation_collector import (
    collect_automatic_deposit_gameplay_observation,
)
from minecraft_chatclef.runtime.automatic_deposit.evidence.gameplay_observation_manifest import (
    AutomaticDepositGameplayObservationManifest,
)
from minecraft_chatclef.runtime.automatic_deposit.evidence.latest_log_delta_result import (
    _create_latest_log_delta_result,
)
from minecraft_chatclef.runtime.automatic_deposit.evidence.live_fixture.p1_live_fixture_observation import (
    P1LiveFixtureObservation,
    _create_p1_live_fixture_observation,
)


_WORLD_KEY = "world-1"
_DIMENSION = "OVERWORLD"
_POSITION = (12, 64, -9)
_DESTINATION_CANONICAL_KEY = "world-1|OVERWORLD|12|64|-9"
_DESTINATION_FINGERPRINT = hashlib.sha256(
    _DESTINATION_CANONICAL_KEY.encode("utf-8")
).hexdigest()
_DESTINATION_ID = f"td_{_DESTINATION_FINGERPRINT[:24]}"


def complete_p1_supervised_observation() -> dict[str, object]:
    return {
        "submission_outcome": "accepted",
        "gradio_submit_call_count": 1,
        "adapter_command_request_count": "unknown",
        "automatic_resubmit_count": 0,
        "automatic_rerun_count": 0,
        "submitted_request_id": "lavi-gui-request-17",
        "connection_state_verified": True,
        "connection_state_error": "",
        "terminal_lifecycle_observed": True,
        "terminal_request_id": "lavi-gui-request-17",
        "terminal_status": "completed",
        "active_request_clear": True,
        "active_clear_observation": "same_snapshot",
        "observer_timeout": False,
        "runtime_reported_completion": True,
        "gameplay_observation_complete": True,
        "gameplay_effect_observed": True,
        "expected_gameplay_effect_verified": True,
        "partial_gameplay_effect_observed": False,
        "unexpected_effect_observed": False,
        "prohibited_effect_absence_verified": True,
        "end_to_end_success": True,
        "reconciliation_required": True,
    }


def ready_p1_supervised_live_fixture(
    *,
    ready: bool = True,
) -> P1LiveFixtureObservation:
    return _create_p1_live_fixture_observation(
        schema_version="p1-live-fixture-observation/v1",
        verdict="PASS" if ready else "INCONCLUSIVE",
        reason=(
            "P1_LIVE_FIXTURE_READY"
            if ready
            else "RUNNING_JVM_SAVE_BARRIER_NOT_AVAILABLE"
        ),
        world_directory="C:/minecraft/instance/saves/world-1",
        world_key=_WORLD_KEY,
        dimension=_DIMENSION,
        trusted_position=_POSITION,
        trusted_destination_id=_DESTINATION_ID,
        trusted_destination_fingerprint=_DESTINATION_FINGERPRINT,
        container_type="minecraft:chest",
        container_capacity_slots=27,
        container_occupied_slots=1,
        container_empty_slots=26,
        container_has_capacity=True,
        container_inventory_fingerprint="c" * 64,
        player_inventory_counts=(("minecraft:cobblestone", 32),),
        player_inventory_fingerprint="b" * 64,
        level_dat_path="C:/minecraft/instance/saves/world-1/level.dat",
        level_dat_sha256="1" * 64,
        level_dat_size=128,
        level_dat_mtime_ns=10,
        player_data_path=(
            "C:/minecraft/instance/saves/world-1/playerdata/player.dat"
        ),
        player_data_sha256="2" * 64,
        player_data_size=256,
        player_data_mtime_ns=11,
        trusted_registry_path="C:/minecraft/instance/config/lavi/trusted.json",
        trusted_registry_sha256="3" * 64,
        trusted_registry_size=512,
        trusted_registry_mtime_ns=12,
        region_path="C:/minecraft/instance/saves/world-1/region/r.0.-1.mca",
        region_sha256="4" * 64,
        region_size=8192,
        region_mtime_ns=13,
        region_chunk_timestamp=1_777_777_777,
        world_snapshot_fingerprint="5" * 64,
        limitations=(
            () if ready else ("RUNNING_JVM_SAVE_BARRIER_NOT_AVAILABLE",)
        ),
    )


def complete_p1_supervised_gameplay_observation(
    *,
    run_id: str = "opaque-p1-run",
    operation_id: str = "17",
    artifact_sha256: str = "a" * 64,
    fixture: P1LiveFixtureObservation | None = None,
    fixture_fingerprint: str | None = None,
    before_world_snapshot_id: str | None = None,
    before_inventory_snapshot_id: str | None = None,
    overrides: dict[str, bool] | None = None,
) -> AutomaticDepositGameplayObservationManifest:
    bound_fixture = fixture or ready_p1_supervised_live_fixture()
    checkpoint = {
        "gameplay_observation_complete": True,
        "gameplay_effect_observed": True,
        "expected_gameplay_effect_verified": True,
        "partial_gameplay_effect_observed": False,
        "unexpected_effect_observed": False,
        "prohibited_effect_absence_verified": True,
    }
    checkpoint.update(overrides or {})
    observation, reason = collect_automatic_deposit_gameplay_observation(
        run_id=run_id,
        row_id="P1",
        operation_id=operation_id,
        artifact_sha256=artifact_sha256,
        fixture_fingerprint=(
            fixture_fingerprint or bound_fixture.observation_fingerprint
        ),
        before_world_snapshot_id=(
            before_world_snapshot_id or bound_fixture.world_snapshot_fingerprint
        ),
        after_world_snapshot_id="world-snapshot-after-1",
        before_inventory_snapshot_id=(
            before_inventory_snapshot_id
            or bound_fixture.player_inventory_fingerprint
        ),
        after_inventory_snapshot_id="inventory-snapshot-after-1",
        observation_source="AUTOMATED_WORLD_SNAPSHOT",
        observer_identity_fingerprint="d" * 64,
        operator_confirmed=True,
        checkpoint=checkpoint,
    )
    if observation is None:
        raise AssertionError(reason)
    return observation


def supervised_p1_log_delta(
    request_id: str,
    *,
    world_key: str = _WORLD_KEY,
    dimension: str = _DIMENSION,
    destination_canonical_key: str = _DESTINATION_CANONICAL_KEY,
    destination_id: str = _DESTINATION_ID,
    command_session_id: str = "fabric-chatclef-session-1",
):
    text = "\n".join(
        (
            supervised_store_home_line(
                1,
                "STORE_HOME_RUN_MANIFEST",
                "runId=opaque-p1-run "
                "runManifestIdSource=EXTERNAL_BUILD_DEPLOY_MANIFEST",
                request_id,
                command_session_id=command_session_id,
            ),
            supervised_store_home_line(
                2,
                "STORE_HOME_OPERATION_STARTED",
                "phase=START",
                request_id,
                command_session_id=command_session_id,
            ),
            supervised_store_home_line(
                3,
                "STORE_HOME_CANDIDATE_ACTIVATED",
                "candidateActivated=true "
                "activationResult=EXACT_SESSION_INSTALLED "
                f"worldKey={world_key} "
                f"dimension={dimension} "
                f"destinationCanonicalKey={destination_canonical_key} "
                f"destinationId={destination_id} "
                "candidatePosition=12%2C%2064%2C%20-9",
                request_id,
                command_session_id=command_session_id,
            ),
            supervised_store_home_line(
                4,
                "STORE_HOME_OPERATION_TERMINAL_SUMMARY",
                "terminalResult=COMPLETED operationResult=COMPLETED "
                "remainingStackCount=0 storedItems=64 touchedStackCount=2",
                request_id,
                command_session_id=command_session_id,
            ),
        )
    ) + "\n"
    return _create_latest_log_delta_result(
        True,
        "LATEST_LOG_DELTA_READ",
        None,
        100,
        100 + len(text.encode("utf-8")),
        text,
        "utf-8",
        "a" * 64,
    )


def supervised_store_home_line(
    sequence: int,
    event: str,
    extra: str,
    request_id: str,
    *,
    command_session_id: str = "fabric-chatclef-session-1",
) -> str:
    return (
        "[01:04:05] [Render thread/INFO]: [STDOUT]: ALTO CLEF: "
        "[LAVI ChatClefBoundary] traceId=trace-p1 clientTickId=812 "
        f"eventSequence={sequence} taskInstanceId=task-1 taskRunId=run-1 "
        "parentTaskRunId=unavailable threadName=Render%20thread level=BOUNDARY "
        f"event={event} reason=runtime_test taskClass=lavi.StoreHomeTask "
        "runManifestId=opaque-p1-run operationId=17 "
        f"commandContextAvailable=true commandRequestId={request_id} "
        "commandCorrelationId=message-17 "
        f"commandSessionId={command_session_id} "
        "commandConnectionGeneration=7 commandText=%40store_home "
        "commandSource=lavi_gui commandContextError=none "
        f"{extra} diagnosticCaptureStatus=complete"
    )
