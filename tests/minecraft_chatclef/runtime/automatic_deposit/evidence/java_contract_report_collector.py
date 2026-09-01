#20260831_kpopmodder: Derive Java contract evidence from stable Gradle JUnit XML reports.
from __future__ import annotations

import hashlib
import json
import re
import xml.etree.ElementTree as ET
from collections.abc import Callable, Sequence
from pathlib import Path

from ...preflight.shared_file_reader import read_shared_bytes
from .java_contract_manifest import (
    AutomaticDepositJavaContractManifest,
    _create_java_contract_manifest,
)


_PROVENANCE_PROPERTIES = (
    "lavi.provenanceSchema",
    "lavi.gitCommit",
    "lavi.sourceJarSha256",
    "lavi.gradleInvocation",
    "lavi.testTask",
    "lavi.testRunId",
)
_JAVA_CONTRACT_TEST_CASES: dict[str, tuple[tuple[str, str], ...]] = {
    "generation_boundary_verified": (
        (
            "lavi.minecraft.task.container.deposit.handoff.candidate."
            "DepositAllContainerCandidateHandoffLifecycleTest",
            "completedChestPlacementReevaluatesBarrelOnTheNextGeneration",
        ),
        (
            "lavi.minecraft.task.container.deposit.handoff.candidate."
            "DepositAllContainerCandidateHandoffLifecycleTest",
            "completedBarrelPlacementReevaluatesChestOnTheNextGeneration",
        ),
        (
            "lavi.minecraft.task.container.deposit.handoff.candidate."
            "DepositAllContainerCandidateHandoffLifecycleTest",
            "changedCandidateKeepsTheActualPlacementThroughTheBarrierAndReevaluatesTheNextGeneration",
        ),
        (
            "lavi.minecraft.task.container.deposit.handoff.candidate."
            "DepositAllContainerCandidateHandoffLifecycleTest",
            "completedChestPlacementAllowsFreshChestIdentityInALaterGeneration",
        ),
        (
            "lavi.minecraft.task.container.deposit.handoff.candidate."
            "AutoDepositContainerCandidateOperationIsolationTest",
            "dirtyPreviousOperationDoesNotLeakStateIntoTheNextOperation",
        ),
        (
            "lavi.minecraft.task.container.deposit.handoff.candidate."
            "AutoDepositContainerCandidateOperationIsolationTest",
            "dirtyStoreGenerationDoesNotLeakIntoTheNextOperation",
        ),
    ),
    "one_tick_barrier_verified": (
        (
            "lavi.minecraft.task.container.deposit.handoff."
            "DepositAllPostPlaceHandoffLifecycleTest",
            "stopsTheActualPlacementChildBeforeStartingTheOpenChildOnTheNextTick",
        ),
        (
            "lavi.minecraft.task.container.deposit.handoff.candidate."
            "DepositAllTaskPostPlaceHandoffBoundaryTest",
            "productionBoundaryClearsTheFinishedPlacementAndDefersOnlyOnce",
        ),
    ),
    "diagnostics_internal_mutation_verified": (
        (
            "lavi.minecraft.diagnostics.session.runtime."
            "DiagnosticSessionRuntimeTest",
            "offDispatchDoesNotMutateAdmissionOrSuppressionAccounting",
        ),
        (
            "lavi.minecraft.diagnostics.session.runtime."
            "DiagnosticSessionRuntimeTest",
            "enabledToOffWaitsForTheAdmittedEmissionAndLeavesNoPendingLease",
        ),
        (
            "lavi.minecraft.diagnostics.session.runtime."
            "DiagnosticSessionRuntimeTest",
            "offCleanTeardownIsEventlessAndMutationFree",
        ),
        (
            "lavi.minecraft.diagnostics.toolselect."
            "ToolSelectionStrictOffContractTest",
            "offWaitsForTheEligibilityLeaseThenClearsEveryToolSelectionState",
        ),
    ),
}


def automatic_deposit_java_contract_test_cases(
    required_keys: Sequence[str],
) -> tuple[tuple[str, str], ...]:
    cases: list[tuple[str, str]] = []
    for key in required_keys:
        cases.extend(_JAVA_CONTRACT_TEST_CASES.get(key, ()))
    return tuple(dict.fromkeys(cases))


def collect_automatic_deposit_java_contract_report(
    report_directory: object,
    required_keys: Sequence[str],
    *,
    report_paths_reader: Callable[[Path], Sequence[Path]] | None = None,
    bytes_reader: Callable[[Path], bytes] | None = None,
    stat_reader: Callable[[Path], object] | None = None,
    max_report_bytes: int = 4 * 1024 * 1024,
    max_report_count: int = 512,
) -> tuple[AutomaticDepositJavaContractManifest | None, str]:
    keys = tuple(required_keys)
    if (
        not keys
        or len(keys) != len(set(keys))
        or any(key not in _JAVA_CONTRACT_TEST_CASES for key in keys)
    ):
        return None, "JAVA_CONTRACT_REQUIRED_KEYS_INVALID"
    expected_directory = _expected_report_directory()
    path = Path(str(report_directory or "").strip())
    if not path.is_absolute() or _canonical(path) != _canonical(expected_directory):
        return None, "JAVA_CONTRACT_REPORT_DIRECTORY_NOT_CANONICAL"
    if max_report_bytes < 1 or max_report_count < 1:
        return None, "JAVA_CONTRACT_REPORT_BOUND_INVALID"
    try:
        candidates = tuple(
            (report_paths_reader or _default_report_paths)(expected_directory)
        )
    except Exception as error:
        return None, f"JAVA_CONTRACT_REPORT_SCAN_FAILED:{type(error).__name__}"
    if not candidates or len(candidates) > max_report_count:
        return None, "JAVA_CONTRACT_REPORT_COUNT_INVALID"

    required_cases = {
        case for key in keys for case in _JAVA_CONTRACT_TEST_CASES[key]
    }
    observed: dict[tuple[str, str], bool] = {}
    skipped: set[tuple[str, str]] = set()
    provenance: dict[str, str] | None = None
    used_reports: list[tuple[str, str]] = []
    for candidate in sorted(candidates, key=lambda value: str(value).casefold()):
        report_path = Path(candidate).resolve(strict=False)
        if not _valid_report_path(report_path, expected_directory):
            return None, "JAVA_CONTRACT_REPORT_PATH_INVALID"
        raw, reason = _read_stable_report(
            report_path,
            bytes_reader=bytes_reader or read_shared_bytes,
            stat_reader=stat_reader,
            max_report_bytes=max_report_bytes,
        )
        if raw is None:
            return None, reason
        suite, reason = _parse_suite(raw)
        if suite is None:
            return None, reason
        matched = tuple(
            testcase
            for testcase in suite.findall("testcase")
            if _testcase_identity(testcase) in required_cases
        )
        if not matched:
            continue
        suite_provenance, reason = _suite_provenance(suite)
        if suite_provenance is None:
            return None, reason
        if provenance is None:
            provenance = suite_provenance
        elif provenance != suite_provenance:
            return None, "JAVA_CONTRACT_PROVENANCE_INCONSISTENT"
        used_reports.append((str(report_path), hashlib.sha256(raw).hexdigest()))
        for testcase in matched:
            identity = _testcase_identity(testcase)
            if identity in observed or identity in skipped:
                return None, "JAVA_CONTRACT_TESTCASE_DUPLICATE"
            child_tags = tuple(child.tag for child in testcase)
            if "skipped" in child_tags:
                skipped.add(identity)
            else:
                observed[identity] = not any(
                    tag in ("failure", "error") for tag in child_tags
                )

    if skipped:
        return None, "JAVA_CONTRACT_REQUIRED_TESTCASE_SKIPPED"
    missing = required_cases - set(observed)
    if missing:
        return None, "JAVA_CONTRACT_REQUIRED_TESTCASE_MISSING"
    if provenance is None or not used_reports:
        return None, "JAVA_CONTRACT_PROVENANCE_MISSING"
    results = tuple(
        (
            key,
            "true"
            if all(observed[case] for case in _JAVA_CONTRACT_TEST_CASES[key])
            else "false",
        )
        for key in keys
    )
    aggregate = json.dumps(
        sorted(used_reports),
        ensure_ascii=False,
        separators=(",", ":"),
    ).encode("utf-8")
    manifest = _create_java_contract_manifest(
        schema_version="automatic-deposit-java-contract/junit-v1",
        git_commit=provenance["lavi.gitCommit"],
        source_jar_sha256=provenance["lavi.sourceJarSha256"],
        test_report_paths=tuple(path for path, _digest in sorted(used_reports)),
        test_report_sha256=hashlib.sha256(aggregate).hexdigest(),
        gradle_invocation=provenance["lavi.gradleInvocation"],
        test_task=provenance["lavi.testTask"],
        test_run_id=provenance["lavi.testRunId"],
        results=results,
    )
    return manifest, "JAVA_CONTRACT_JUNIT_REPORTS_COLLECTED"


def _expected_report_directory() -> Path:
    return Path(__file__).resolve().parents[5].joinpath(
        "plugins",
        "Minecraft",
        "runtime",
        "chatclef_fabric_1.20.1",
        "build",
        "test-results",
        "test",
    )


def _default_report_paths(directory: Path) -> Sequence[Path]:
    return tuple(directory.glob("TEST-*.xml"))


def _valid_report_path(path: Path, directory: Path) -> bool:
    return (
        path.parent == directory.resolve(strict=False)
        and path.name.startswith("TEST-")
        and path.suffix.casefold() == ".xml"
    )


def _read_stable_report(
    path: Path,
    *,
    bytes_reader: Callable[[Path], bytes],
    stat_reader: Callable[[Path], object] | None,
    max_report_bytes: int,
) -> tuple[bytes | None, str]:
    read_stat = stat_reader or (lambda candidate: candidate.stat())
    try:
        before = _stat_identity(read_stat(path))
        if before is None or before[2] < 1 or before[2] > max_report_bytes:
            return None, "JAVA_CONTRACT_REPORT_SIZE_INVALID"
        raw = bytes_reader(path)
        after = _stat_identity(read_stat(path))
    except Exception as error:
        return None, f"JAVA_CONTRACT_REPORT_READ_FAILED:{type(error).__name__}"
    if not isinstance(raw, bytes):
        return None, "JAVA_CONTRACT_REPORT_READER_DID_NOT_RETURN_BYTES"
    if before != after or len(raw) != before[2]:
        return None, "JAVA_CONTRACT_REPORT_CHANGED_DURING_READ"
    return raw, "JAVA_CONTRACT_REPORT_STABLE"


def _parse_suite(raw: bytes) -> tuple[ET.Element | None, str]:
    upper = raw.upper()
    if b"<!DOCTYPE" in upper or b"<!ENTITY" in upper:
        return None, "JAVA_CONTRACT_REPORT_XML_DECLARATION_FORBIDDEN"
    try:
        root = ET.fromstring(raw.decode("utf-8", errors="strict"))
    except (UnicodeDecodeError, ET.ParseError):
        return None, "JAVA_CONTRACT_REPORT_XML_INVALID"
    if root.tag != "testsuite":
        return None, "JAVA_CONTRACT_REPORT_ROOT_INVALID"
    return root, "JAVA_CONTRACT_REPORT_XML_PARSED"


def _suite_provenance(
    suite: ET.Element,
) -> tuple[dict[str, str] | None, str]:
    properties_element = suite.find("properties")
    if properties_element is None:
        return None, "JAVA_CONTRACT_PROVENANCE_MISSING"
    properties: dict[str, str] = {}
    for element in properties_element.findall("property"):
        name = element.get("name", "")
        if name in properties:
            return None, "JAVA_CONTRACT_PROVENANCE_DUPLICATE"
        properties[name] = element.get("value", "")
    if any(not properties.get(name) for name in _PROVENANCE_PROPERTIES):
        return None, "JAVA_CONTRACT_PROVENANCE_MISSING"
    if properties["lavi.provenanceSchema"] != "automatic-deposit-junit/v1":
        return None, "JAVA_CONTRACT_PROVENANCE_SCHEMA_INVALID"
    if not re.fullmatch(r"[0-9a-fA-F]{40}", properties["lavi.gitCommit"]):
        return None, "JAVA_CONTRACT_PROVENANCE_GIT_INVALID"
    if not re.fullmatch(
        r"[0-9a-fA-F]{64}", properties["lavi.sourceJarSha256"]
    ):
        return None, "JAVA_CONTRACT_PROVENANCE_ARTIFACT_INVALID"
    if properties["lavi.gradleInvocation"] != "clean build --rerun-tasks":
        return None, "JAVA_CONTRACT_GRADLE_INVOCATION_INVALID"
    if properties["lavi.testTask"] != "test":
        return None, "JAVA_CONTRACT_TEST_TASK_INVALID"
    if not re.fullmatch(
        r"[A-Za-z0-9][A-Za-z0-9._-]{0,127}",
        properties["lavi.testRunId"],
    ):
        return None, "JAVA_CONTRACT_TEST_RUN_ID_INVALID"
    return {name: properties[name] for name in _PROVENANCE_PROPERTIES}, ""


def _testcase_identity(testcase: ET.Element) -> tuple[str, str]:
    name = testcase.get("name", "")
    if name.endswith("()"):
        name = name[:-2]
    return testcase.get("classname", ""), name


def _stat_identity(value: object) -> tuple[int, int, int, int] | None:
    try:
        result = (
            int(getattr(value, "st_dev")),
            int(getattr(value, "st_ino")),
            int(getattr(value, "st_size")),
            int(getattr(value, "st_mtime_ns")),
        )
    except (AttributeError, TypeError, ValueError):
        return None
    return result if min(result) >= 0 else None


def _canonical(path: Path) -> str:
    return str(path.resolve(strict=False)).casefold()
