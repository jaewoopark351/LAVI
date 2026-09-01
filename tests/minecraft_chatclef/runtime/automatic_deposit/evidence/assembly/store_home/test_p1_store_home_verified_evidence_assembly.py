# 20260901_kpopmodder: Bind manual P1 production evidence to one run without synthetic substitution.
from __future__ import annotations

import unittest
from dataclasses import replace
from pathlib import Path
from urllib.parse import quote

from ....scenario.matrix_catalog import automatic_deposit_matrix_catalog
from ....testing.hermetic_evidence import (
    hermetic_artifact_collection,
    hermetic_carry_on_collection,
    hermetic_fixture,
    hermetic_gameplay_observation,
    hermetic_log_delta,
    hermetic_run_manifest,
)
from ...fixture_manifest_fingerprint import automatic_deposit_fixture_fingerprint
from ...latest_log_byte_cursor import (
    automatic_deposit_latest_log_cursor_fingerprint,
)
from ...latest_log_delta_result import _create_latest_log_delta_result
from ...live_fixture.p1_live_fixture_observation import (
    _create_p1_live_fixture_observation,
)
from ...operator_action.manual_operator_action_collector import (
    collect_manual_operator_action_observation,
)
from ...runtime_observer.jvm_runtime_artifact_observation import (
    _create_jvm_runtime_artifact_observation,
)
from ...runtime_observer.log_prefix.jvm_runtime_artifact_log_prefix_observation import (
    _create_jvm_runtime_artifact_log_prefix_observation,
)
from ...runtime_observer.production_status_observer import (
    observe_production_fabric_status,
)
from ...runtime_observer.test_production_status_observer import (
    _production_status,
)
from .p1_store_home_verified_evidence_assembly import (
    build_p1_store_home_verified_evidence,
)
from .runtime_observation.p1_runtime_observation_bundle_collector import (
    collect_p1_runtime_observation_bundle,
)


class P1StoreHomeVerifiedEvidenceAssemblyTests(unittest.TestCase):
    def test_assembles_production_p1_evidence_and_preserves_both_operation_ids(self):
        context = _context()

        result = _assemble(context)

        self.assertTrue(result.ok, result.errors)
        self.assertIsNotNone(result.evidence)
        self.assertEqual(
            context.run_manifest.operation_id,
            result.evidence.operation_id,
        )
        values = result.evidence.as_mapping()
        self.assertEqual("17", values["java_store_home_operation_id"])
        self.assertEqual(
            context.jvm_observation.run_manifest_id,
            values["jvm_run_manifest_id"],
        )
        self.assertEqual(
            context.runtime_bundle.bundle_fingerprint,
            values["p1_runtime_observation_bundle_fingerprint"],
        )
        self.assertEqual(
            "COMMAND_CONTEXT_UNAVAILABLE",
            values["p1_static_manifest_binding_classification"],
        )
        self.assertFalse(values["p1_operation_correlation_claimed"])
        self.assertEqual(
            context.fixture_observation.observation_fingerprint,
            values["p1_live_fixture_observation_fingerprint"],
        )
        self.assertTrue(values["operator_action_observed"])
        self.assertTrue(values["runtime_reported_completion"])
        self.assertTrue(values["gameplay_observation_complete"])

    def test_rejects_synthetic_runtime_evidence_instead_of_substituting_it(self):
        context = _context()

        result = _assemble(
            context,
            log_delta=hermetic_log_delta(
                context.row,
                context.run_manifest,
                context.fixture,
            ),
        )

        self.assertFalse(result.ok)
        self.assertEqual("P1_STORE_HOME_RUNTIME_LOG_NOT_VERIFIED", result.reason)
        self.assertIn("P1_STORE_HOME_START_MISSING", result.errors)

    def test_rejects_log_delta_not_bound_to_the_run_cursor_before_projection(self):
        context = _context()
        delta = _production_delta(context)
        wrong_cursor_delta = _create_latest_log_delta_result(
            True,
            delta.reason,
            delta.verdict,
            delta.start_offset,
            delta.end_offset,
            delta.text,
            delta.encoding,
            "f" * 64,
        )

        result = _assemble(context, log_delta=wrong_cursor_delta)

        self.assertFalse(result.ok)
        self.assertEqual("P1_STORE_HOME_LOG_DELTA_FAILED", result.reason)
        self.assertIn(
            "LATEST_LOG_DELTA_CURSOR_FINGERPRINT_MISMATCH",
            result.errors,
        )

    def test_requires_the_same_ready_live_fixture_observation_at_assembly(self):
        context = _context()
        inconclusive = _live_fixture_observation(
            context,
            verdict="INCONCLUSIVE",
            reason="RUNNING_JVM_SAVE_BARRIER_NOT_AVAILABLE",
            limitations=("RUNNING_JVM_SAVE_BARRIER_NOT_AVAILABLE",),
        )
        cases = (
            (None, "P1_LIVE_FIXTURE_OBSERVATION_NOT_TYPED"),
            (inconclusive, "P1_LIVE_FIXTURE_VERDICT_INCONCLUSIVE"),
        )

        for observation, expected_error in cases:
            with self.subTest(expected_error=expected_error):
                result = _assemble(
                    context,
                    fixture_observation=observation,
                )

                self.assertFalse(result.ok)
                self.assertEqual(
                    "P1_STORE_HOME_LIVE_FIXTURE_BINDING_FAILED",
                    result.reason,
                )
                self.assertIn(expected_error, result.errors)

    def test_rejects_untyped_or_bundle_not_bound_to_the_consuming_run(self):
        context = _context()
        changed_cursor = replace(
            context.run_manifest.latest_log_cursor,
            inode=context.run_manifest.latest_log_cursor.inode + 1,
        )
        changed_run = replace(
            context.run_manifest,
            latest_log_cursor=changed_cursor,
        )
        changed_bundle = _runtime_bundle(changed_run)
        mismatches = (
            (
                None,
                "P1_RUNTIME_OBSERVATION_BUNDLE_NOT_TYPED",
            ),
            (changed_bundle, "JVM_LOG_PREFIX_CURSOR_INODE_MISMATCH"),
        )
        for bundle, expected_error in mismatches:
            with self.subTest(expected_error=expected_error):
                result = _assemble(context, runtime_bundle=bundle)

                self.assertFalse(result.ok)
                self.assertEqual(
                    "P1_STORE_HOME_RUNTIME_OBSERVATION_BUNDLE_FAILED",
                    result.reason,
                )
                self.assertIn(expected_error, result.errors)

    def test_uses_direct_jvm_run_manifest_id_and_rejects_log_mismatch(self):
        context = _context()
        log_delta = _production_delta(context, run_manifest_id="other-manifest")

        result = _assemble(context, log_delta=log_delta)

        self.assertFalse(result.ok)
        self.assertEqual("P1_STORE_HOME_RUNTIME_LOG_NOT_VERIFIED", result.reason)
        self.assertIn(
            "P1_STORE_HOME_RUN_MANIFEST_ID_NOT_EXPECTED",
            result.errors,
        )

    def test_requires_typed_exact_manual_operator_action(self):
        context = _context()
        wrong_action, reason = collect_manual_operator_action_observation(
            run_id=context.run_manifest.run_id,
            row_id=context.row.row_id,
            harness_operation_id=context.run_manifest.operation_id,
            action_text="@stop",
            observation_source="OPERATOR_ACTION_OBSERVER",
            observer_identity_fingerprint="a" * 64,
            operator_confirmed=True,
        )
        self.assertEqual("MANUAL_OPERATOR_ACTION_OBSERVED", reason)

        cases = (
            (None, "P1_MANUAL_OPERATOR_ACTION_NOT_TYPED"),
            (wrong_action, "P1_MANUAL_OPERATOR_ACTION_TEXT_MISMATCH"),
        )
        for observation, expected_error in cases:
            with self.subTest(expected_error=expected_error):
                result = _assemble(context, operator_action=observation)

                self.assertFalse(result.ok)
                self.assertEqual("P1_STORE_HOME_OPERATOR_ACTION_FAILED", result.reason)
                self.assertIn(expected_error, result.errors)

    def test_does_not_treat_observation_fingerprint_as_run_manifest_id(self):
        context = _context()
        observation = _jvm_observation(
            context.run_manifest,
            run_manifest_id="manifest-direct-raw-value",
        )
        runtime_bundle = _runtime_bundle(
            context.run_manifest,
            jvm_observation=observation,
        )
        delta = _production_delta(
            context,
            run_manifest_id=observation.observation_fingerprint,
        )

        result = _assemble(
            context,
            runtime_bundle=runtime_bundle,
            log_delta=delta,
        )

        self.assertFalse(result.ok)
        self.assertIn(
            "P1_STORE_HOME_RUN_MANIFEST_ID_NOT_EXPECTED",
            result.errors,
        )


class _Context:
    def __init__(self) -> None:
        self.row = next(
            row for row in automatic_deposit_matrix_catalog() if row.row_id == "P1"
        )
        base_fixture = hermetic_fixture(self.row)
        attributes = tuple(
            (
                key,
                "12, 64, -9" if key == "trusted_container_position" else value,
            )
            for key, value in base_fixture.attributes
        )
        provisional = replace(
            base_fixture,
            attributes=attributes,
            fixture_fingerprint="0" * 64,
        )
        self.fixture = replace(
            provisional,
            fixture_fingerprint=automatic_deposit_fixture_fingerprint(provisional),
        )
        self.run_manifest = hermetic_run_manifest(self.row, self.fixture)
        self.fixture_observation = _live_fixture_observation(self)
        self.artifact_collection = hermetic_artifact_collection(self.row)
        self.carry_on_collection = hermetic_carry_on_collection(self.row)
        self.gameplay_observation = hermetic_gameplay_observation(
            self.run_manifest,
            self.fixture,
        )
        self.jvm_observation = _jvm_observation(self.run_manifest)
        self.runtime_bundle = _runtime_bundle(
            self.run_manifest,
            jvm_observation=self.jvm_observation,
        )
        self.operator_action, reason = collect_manual_operator_action_observation(
            run_id=self.run_manifest.run_id,
            row_id=self.row.row_id,
            harness_operation_id=self.run_manifest.operation_id,
            action_text="@store_home",
            observation_source="OPERATOR_ACTION_OBSERVER",
            observer_identity_fingerprint="a" * 64,
            operator_confirmed=True,
        )
        if self.operator_action is None:
            raise AssertionError(reason)
        self.log_delta = _production_delta(self)


def _context() -> _Context:
    return _Context()


def _assemble(
    context: _Context,
    *,
    log_delta=None,
    runtime_bundle=...,
    fixture_observation=...,
    operator_action=...,
):
    return build_p1_store_home_verified_evidence(
        context.row,
        context.run_manifest,
        context.fixture,
        context.artifact_collection,
        context.log_delta if log_delta is None else log_delta,
        context.gameplay_observation,
        carry_on_collection=context.carry_on_collection,
        p1_runtime_observation_bundle=(
            context.runtime_bundle if runtime_bundle is ... else runtime_bundle
        ),
        p1_live_fixture_observation=(
            context.fixture_observation
            if fixture_observation is ...
            else fixture_observation
        ),
        operator_action_observation=(
            context.operator_action if operator_action is ... else operator_action
        ),
    )


def _runtime_bundle(run_manifest, *, jvm_observation=None):
    status, status_reason = observe_production_fabric_status(_production_status())
    if status is None:
        raise AssertionError(status_reason)
    raw = jvm_observation or _jvm_observation(run_manifest)
    cursor = run_manifest.latest_log_cursor
    log_prefix = _create_jvm_runtime_artifact_log_prefix_observation(
        runtime_artifact_observation=raw,
        source_log_path=str(Path(cursor.path).resolve(strict=False)),
        source_cursor_fingerprint=(
            automatic_deposit_latest_log_cursor_fingerprint(cursor)
        ),
        source_prefix_sha256=cursor.prefix_sha256.casefold(),
        source_prefix_size=cursor.size,
        source_device=cursor.device,
        source_inode=cursor.inode,
    )
    bundle, reason = collect_p1_runtime_observation_bundle(
        status,
        log_prefix,
        run_manifest,
    )
    if bundle is None:
        raise AssertionError(reason)
    return bundle


def _live_fixture_observation(
    context,
    *,
    verdict="PASS",
    reason="P1_LIVE_FIXTURE_READY",
    limitations=(),
):
    return _create_p1_live_fixture_observation(
        schema_version="p1-live-fixture-observation/v1",
        verdict=verdict,
        reason=reason,
        world_directory="C:/minecraft/instance/saves/automatic-deposit-fixture",
        world_key=context.run_manifest.world,
        dimension="OVERWORLD",
        trusted_position=(12, 64, -9),
        trusted_destination_id="trusted-home-1",
        trusted_destination_fingerprint=(
            context.fixture.trusted_destination_fingerprint
        ),
        container_type=f"minecraft:{context.fixture.container_type.casefold()}",
        container_capacity_slots=27,
        container_occupied_slots=1,
        container_empty_slots=26,
        container_has_capacity=True,
        container_inventory_fingerprint="c" * 64,
        player_inventory_counts=(("minecraft:cobblestone", 64),),
        player_inventory_fingerprint=context.fixture.inventory_snapshot_id,
        level_dat_path=(
            "C:/minecraft/instance/saves/automatic-deposit-fixture/level.dat"
        ),
        level_dat_sha256="1" * 64,
        level_dat_size=128,
        level_dat_mtime_ns=10,
        player_data_path=(
            "C:/minecraft/instance/saves/automatic-deposit-fixture/"
            "playerdata/player.dat"
        ),
        player_data_sha256="2" * 64,
        player_data_size=256,
        player_data_mtime_ns=11,
        trusted_registry_path="C:/minecraft/instance/config/lavi/trusted.json",
        trusted_registry_sha256="3" * 64,
        trusted_registry_size=512,
        trusted_registry_mtime_ns=12,
        region_path=(
            "C:/minecraft/instance/saves/automatic-deposit-fixture/"
            "region/r.0.-1.mca"
        ),
        region_sha256="4" * 64,
        region_size=8192,
        region_mtime_ns=13,
        region_chunk_timestamp=1_777_777_777,
        world_snapshot_fingerprint=context.fixture.world_snapshot_id,
        limitations=limitations,
    )


def _jvm_observation(
    run_manifest,
    *,
    run_manifest_id: str = "manifest-p1-direct",
    runtime_code_source_path: str | None = None,
    runtime_code_source_sha256: str | None = None,
):
    artifact = run_manifest.artifact_identity
    path = runtime_code_source_path or artifact.deployed_jar_path
    sha256 = runtime_code_source_sha256 or artifact.deployed_jar_sha256
    return _create_jvm_runtime_artifact_observation(
        run_manifest_id=run_manifest_id,
        run_manifest_id_source="RUNTIME_GENERATED",
        manifest_event_sequence=9,
        runtime_code_source_uri=Path(path).resolve(strict=False).as_uri(),
        runtime_code_source_path=str(Path(path).resolve(strict=False)),
        runtime_code_source_sha256=sha256,
        runtime_code_source_size=1024,
        runtime_code_source_mtime_ns=123,
        reported_runtime_jar_sha256="UNVERIFIED",
        minecraft_version="1.20.1",
        fabric_loader_version="0.19.3",
        chatclef_version="1.20.1-0.18.23",
        command_context_available=False,
        command_session_id="",
        command_connection_generation=None,
    )


def _production_delta(
    context: _Context,
    *,
    run_manifest_id: str | None = None,
):
    manifest_id = run_manifest_id or context.jvm_observation.run_manifest_id
    position = context.fixture.as_evidence_mapping()["trusted_container_position"]
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
                    "candidateActivated=true activationResult=EXACT_SESSION_INSTALLED "
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
    start = context.run_manifest.latest_log_cursor.size
    return _create_latest_log_delta_result(
        True,
        "LATEST_LOG_DELTA_READ",
        None,
        start,
        start + len(text.encode("utf-8")),
        text,
        "utf-8",
        automatic_deposit_latest_log_cursor_fingerprint(
            context.run_manifest.latest_log_cursor
        ),
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


if __name__ == "__main__":
    unittest.main()
