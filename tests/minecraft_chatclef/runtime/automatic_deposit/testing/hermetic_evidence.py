#20260831_kpopmodder: Build correlated in-memory evidence without live services.
from __future__ import annotations

import hashlib
import xml.etree.ElementTree as ET
from dataclasses import replace
from pathlib import Path
from types import SimpleNamespace
from urllib.parse import quote

from ..evidence.artifact_identity import AutomaticDepositArtifactIdentity
from ..evidence.artifact_identity_collection import AutomaticDepositArtifactIdentityCollection
from ..evidence.artifact_identity_collector import (
    collect_automatic_deposit_artifact_identity,
)
from ..evidence.carry_on_configuration_snapshot_collector import (
    collect_automatic_deposit_carry_on_configuration_snapshot,
)
from ..evidence.carry_on_identity_collection import (
    AutomaticDepositCarryOnIdentityCollection,
)
from ..evidence.carry_on_identity_collector import (
    collect_automatic_deposit_carry_on_identity,
)
from ..evidence.evidence_test_fixture import (
    HermeticEvidenceFileSystem,
    fabric_mod_jar,
    runtime_status,
)
from ..evidence.fixture_manifest import AutomaticDepositFixtureManifest
from ..evidence.fixture_manifest_fingerprint import (
    automatic_deposit_fixture_fingerprint,
)
from ..evidence.gameplay_observation_collector import (
    collect_automatic_deposit_gameplay_observation,
)
from ..evidence.gameplay_observation_manifest import (
    AutomaticDepositGameplayObservationManifest,
)
from ..evidence.java_contract_manifest import AutomaticDepositJavaContractManifest
from ..evidence.java_contract_report_collector import (
    automatic_deposit_java_contract_test_cases,
    collect_automatic_deposit_java_contract_report,
)
from ..evidence.latest_log_byte_cursor import (
    AutomaticDepositLatestLogByteCursor,
    automatic_deposit_latest_log_cursor_fingerprint,
)
from ..evidence.latest_log_delta_result import (
    AutomaticDepositLatestLogDeltaResult,
    _create_latest_log_delta_result,
)
from ..evidence.mods_directory_snapshot_collector import (
    collect_automatic_deposit_mods_directory_snapshot,
)
from ..evidence.runtime_artifact_snapshot_collector import (
    collect_automatic_deposit_runtime_artifact_snapshot,
)
from ..evidence.run_manifest import AutomaticDepositRunManifest
from ..evidence.run_manifest_factory import build_automatic_deposit_run_manifest
from ..scenario.matrix_row import AutomaticDepositMatrixRow

_HERMETIC_JAR_BYTES = fabric_mod_jar("altoclef", "1.20.1-0.18.23")
_REPOSITORY_ROOT = Path(__file__).resolve().parents[5]


def hermetic_fixture(
    row: AutomaticDepositMatrixRow,
) -> AutomaticDepositFixtureManifest:
    expected = dict(row.expected_fixture_values)
    carry_on = hermetic_carry_on_collection(row).identity
    standard = {
        "fixture_fingerprint",
        "world_snapshot_id",
        "inventory_snapshot_id",
        "operator_confirmed",
        "carry_on_state",
        "container_type",
        "trusted_destination_fingerprint",
        "diagnostics_mode",
    }
    manifest = AutomaticDepositFixtureManifest(
        schema_version="automatic-deposit-fixture/v1",
        row_id=row.row_id,
        fixture_id=f"fixture-{row.row_id}",
        fixture_fingerprint="0" * 64,
        world_snapshot_id="world-snapshot-1",
        inventory_snapshot_id="inventory-snapshot-1",
        operator_confirmed=True,
        carry_on_identity=carry_on,
        container_type=expected.get("container_type", "CHEST"),
        trusted_destination_fingerprint="e" * 64,
        diagnostics_mode=expected.get("diagnostics_mode", "BOUNDARY"),
        attributes=tuple(
            (field, expected.get(field, "present"))
            for field in row.required_fixture_fields
            if field not in standard
        ),
    )
    return replace(
        manifest,
        fixture_fingerprint=automatic_deposit_fixture_fingerprint(manifest),
    )


def hermetic_artifact_identity(
    row: AutomaticDepositMatrixRow,
) -> AutomaticDepositArtifactIdentity:
    return hermetic_artifact_collection(row).identity


def hermetic_artifact_collection(
    row: AutomaticDepositMatrixRow,
) -> AutomaticDepositArtifactIdentityCollection:
    artifact, _carry_on = _hermetic_collections(row)
    return artifact


def hermetic_carry_on_collection(
    row: AutomaticDepositMatrixRow,
) -> AutomaticDepositCarryOnIdentityCollection:
    _artifact, carry_on = _hermetic_collections(row)
    return carry_on


def _hermetic_collections(
    row: AutomaticDepositMatrixRow,
) -> tuple[
    AutomaticDepositArtifactIdentityCollection,
    AutomaticDepositCarryOnIdentityCollection,
]:
    instance = Path("C:/minecraft/instance")
    mods = instance.joinpath("mods")
    source = _REPOSITORY_ROOT.joinpath("build", "chatclef.jar")
    deployed = mods.joinpath("chatclef.jar")
    files = HermeticEvidenceFileSystem(instance, mods)
    files.add_file(source, _HERMETIC_JAR_BYTES)
    files.add_file(deployed, _HERMETIC_JAR_BYTES, in_mods=True)

    installed = (
        dict(row.expected_fixture_values).get("carry_on_state") == "INSTALLED"
    )
    carry_on_path = mods.joinpath("carryon.jar") if installed else None
    configuration = None
    if carry_on_path is not None:
        files.add_file(
            carry_on_path,
            fabric_mod_jar("carryon", "2.1.2.7"),
            in_mods=True,
        )
        #20260901_kpopmodder: Mirror the exact known Carry On 2.1.2.7 profile files and binding key.
        options = instance.joinpath("options.txt")
        client_config = instance.joinpath("config", "carryon-client.json")
        common_config = instance.joinpath("config", "carryon-common.json")
        files.add_file(options, b"key_key.carry.desc:key.keyboard.unknown\n")
        files.add_file(client_config, b'{"renderArms":true}\n')
        files.add_file(common_config, b'{"settings":{"maxDistance":2.5}}\n')
        configuration, reason = (
            collect_automatic_deposit_carry_on_configuration_snapshot(
                instance,
                (options, client_config, common_config),
                bytes_reader=files.read_bytes,
                stat_reader=files.read_stat,
            )
        )
        if configuration is None:
            raise AssertionError(reason)

    mods_snapshot, reason = collect_automatic_deposit_mods_directory_snapshot(
        instance,
        mods,
        directory_reader=files.read_directory,
        bytes_reader=files.read_bytes,
        stat_reader=files.read_stat,
    )
    if mods_snapshot is None:
        raise AssertionError(reason)
    runtime_snapshot, reason = collect_automatic_deposit_runtime_artifact_snapshot(
        runtime_status(deployed, carry_on_path=carry_on_path)
    )
    if runtime_snapshot is None:
        raise AssertionError(reason)
    artifact = collect_automatic_deposit_artifact_identity(
        source,
        mods_snapshot,
        runtime_snapshot,
        bytes_reader=files.read_bytes,
        stat_reader=files.read_stat,
    )
    if not artifact.ok:
        raise AssertionError(artifact.reason)
    carry_on = collect_automatic_deposit_carry_on_identity(
        mods_snapshot,
        runtime_snapshot,
        configuration_snapshot=configuration,
    )
    if not carry_on.ok:
        raise AssertionError(carry_on.reason)
    return artifact, carry_on


def hermetic_run_manifest(
    row: AutomaticDepositMatrixRow,
    fixture: AutomaticDepositFixtureManifest,
    *,
    invocation_id: str = "",
) -> AutomaticDepositRunManifest:
    artifact_collection = hermetic_artifact_collection(row)
    carry_on_collection = hermetic_carry_on_collection(row)
    return build_automatic_deposit_run_manifest(
        run_id=f"run-{row.row_id}",
        created_at_utc="2026-08-31T12:00:00Z",
        repository_root=str(_REPOSITORY_ROOT),
        git_commit="2" * 40,
        git_worktree_fingerprint="3" * 64,
        harness_fingerprint="4" * 64,
        row=row,
        operation_id=f"operation-{row.row_id}",
        fixture_fingerprint=fixture.fixture_fingerprint,
        artifact_identity=artifact_collection.identity,
        runtime_artifact_snapshot_fingerprint=(
            artifact_collection.runtime_snapshot_fingerprint
        ),
        loader_mod_list_fingerprint=(
            carry_on_collection.identity.loader_mod_list_fingerprint
        ),
        mods_directory_fingerprint=(
            artifact_collection.mods_directory_fingerprint
        ),
        instance_root="C:/minecraft/instance",
        mods_directory="C:/minecraft/instance/mods",
        latest_log_path="C:/minecraft/instance/logs/latest.log",
        latest_log_cursor=AutomaticDepositLatestLogByteCursor(
            path="C:/minecraft/instance/logs/latest.log",
            device=1,
            inode=2,
            size=0,
            mtime_ns=1,
            prefix_sha256=hashlib.sha256(b"").hexdigest(),
        ),
        crash_reports_directory="C:/minecraft/instance/crash-reports",
        lavi_process_identity_fingerprint="5" * 64,
        minecraft_process_identity_fingerprint="6" * 64,
        backend="fabric_chatclef",
        instance="automatic-deposit-test",
        world="automatic-deposit-fixture",
        diagnostics_mode=fixture.diagnostics_mode or "BOUNDARY",
        submission_invocation_id=invocation_id,
    )


def hermetic_java_contract(
    row: AutomaticDepositMatrixRow,
    run_manifest: AutomaticDepositRunManifest,
    *,
    overrides: dict[str, bool] | None = None,
) -> AutomaticDepositJavaContractManifest | None:
    replacements = overrides or {}
    results = {
        requirement.key: replacements.get(
            requirement.key,
            requirement.expected_value == "true",
        )
        for requirement in row.evidence_requirements
        if requirement.owner == "JAVA_DETERMINISTIC"
    }
    if not results:
        return None
    suite = ET.Element(
        "testsuite",
        {
            "name": "automatic-deposit-hermetic-contract",
            "tests": str(
                len(automatic_deposit_java_contract_test_cases(tuple(results)))
            ),
            "failures": str(sum(not value for value in results.values())),
            "errors": "0",
            "skipped": "0",
        },
    )
    properties = ET.SubElement(suite, "properties")
    for name, value in (
        ("lavi.provenanceSchema", "automatic-deposit-junit/v1"),
        ("lavi.gitCommit", run_manifest.git_commit),
        (
            "lavi.sourceJarSha256",
            run_manifest.artifact_identity.source_jar_sha256,
        ),
        ("lavi.gradleInvocation", "clean build --rerun-tasks"),
        ("lavi.testTask", "test"),
        ("lavi.testRunId", f"hermetic-{run_manifest.run_id}"),
    ):
        ET.SubElement(properties, "property", {"name": name, "value": value})
    failed_cases = {
        automatic_deposit_java_contract_test_cases((key,))[0]
        for key, passed in results.items()
        if not passed
    }
    for classname, name in automatic_deposit_java_contract_test_cases(
        tuple(results)
    ):
        testcase = ET.SubElement(
            suite,
            "testcase",
            {"classname": classname, "name": f"{name}()"},
        )
        if (classname, name) in failed_cases:
            ET.SubElement(testcase, "failure", {"message": "hermetic failure"})
    raw = ET.tostring(suite, encoding="utf-8", xml_declaration=True)
    report_directory = _REPOSITORY_ROOT.joinpath(
        "plugins",
        "Minecraft",
        "runtime",
        "chatclef_fabric_1.20.1",
        "build",
        "test-results",
        "test",
    )
    report_path = report_directory.joinpath("TEST-hermetic.xml")
    stat = SimpleNamespace(
        st_dev=1,
        st_ino=2,
        st_size=len(raw),
        st_mtime_ns=3,
    )
    manifest, reason = collect_automatic_deposit_java_contract_report(
        report_directory,
        tuple(results),
        report_paths_reader=lambda _directory: (report_path,),
        bytes_reader=lambda _path: raw,
        stat_reader=lambda _path: stat,
    )
    if manifest is None:
        raise AssertionError(reason)
    return manifest


def hermetic_gameplay_observation(
    run_manifest: AutomaticDepositRunManifest,
    fixture: AutomaticDepositFixtureManifest,
    *,
    overrides: dict[str, bool] | None = None,
) -> AutomaticDepositGameplayObservationManifest:
    values = {
        "gameplay_observation_complete": True,
        "gameplay_effect_observed": True,
        "expected_gameplay_effect_verified": True,
        "partial_gameplay_effect_observed": False,
        "unexpected_effect_observed": False,
        "prohibited_effect_absence_verified": True,
    }
    values.update(overrides or {})
    manifest, reason = collect_automatic_deposit_gameplay_observation(
        run_id=run_manifest.run_id,
        row_id=run_manifest.row_id,
        operation_id=run_manifest.operation_id,
        artifact_sha256=(
            run_manifest.artifact_identity.deployed_jar_sha256
        ),
        fixture_fingerprint=fixture.fixture_fingerprint,
        before_world_snapshot_id=fixture.world_snapshot_id,
        after_world_snapshot_id="world-snapshot-after-1",
        before_inventory_snapshot_id=fixture.inventory_snapshot_id,
        after_inventory_snapshot_id="inventory-snapshot-after-1",
        observation_source="OPERATOR_OBSERVATION",
        observer_identity_fingerprint="d" * 64,
        operator_confirmed=True,
        checkpoint=values,
    )
    if manifest is None:
        raise AssertionError(reason)
    return manifest


def hermetic_log_delta(
    row: AutomaticDepositMatrixRow,
    run_manifest: AutomaticDepositRunManifest,
    fixture: AutomaticDepositFixtureManifest,
    *,
    overrides: dict[str, str] | None = None,
    omit_keys: frozenset[str] = frozenset(),
) -> AutomaticDepositLatestLogDeltaResult:
    replacements = overrides or {}
    lines: list[str] = []
    sequence = 0
    for requirement in row.evidence_requirements:
        if requirement.owner != "RUNTIME_LOG" or requirement.key == "runtime_log_complete":
            continue
        if requirement.key in omit_keys:
            continue
        sequence += 1
        value = replacements.get(requirement.key, requirement.expected_value)
        fields = (
            ("schemaVersion", "automatic-deposit-evidence/v1"),
            ("event", "automaticDepositEvidence"),
            ("eventSequence", str(sequence)),
            ("runId", run_manifest.run_id),
            ("rowId", row.row_id),
            ("operationId", run_manifest.operation_id),
            (
                "artifactSha256",
                run_manifest.artifact_identity.deployed_jar_sha256,
            ),
            ("fixtureFingerprint", fixture.fixture_fingerprint),
            ("evidenceOwner", "RUNTIME_LOG"),
            ("evidenceKey", requirement.key),
            ("evidenceValue", value),
            ("diagnosticCaptureStatus", "complete"),
        )
        payload = " ".join(f"{key}={_encode(value)}" for key, value in fields)
        lines.append(f"[LAVI ChatClefBoundary] {payload}")
    text = "\n".join(lines) + "\n"
    byte_count = len(text.encode("utf-8"))
    return _create_latest_log_delta_result(
        True,
        "LATEST_LOG_DELTA_READ",
        None,
        run_manifest.latest_log_cursor.size,
        run_manifest.latest_log_cursor.size + byte_count,
        text,
        "utf-8",
        automatic_deposit_latest_log_cursor_fingerprint(
            run_manifest.latest_log_cursor
        ),
    )


def _encode(value: str) -> str:
    return quote(str(value), safe="-._~")
