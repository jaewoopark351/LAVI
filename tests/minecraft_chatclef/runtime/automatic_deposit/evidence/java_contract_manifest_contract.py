#20260831_kpopmodder: Verify deterministic Java evidence without accepting loose booleans.
from __future__ import annotations

import re
from pathlib import Path

from ..scenario.matrix_row import AutomaticDepositMatrixRow
from .artifact_identity import AutomaticDepositArtifactIdentity
from .java_contract_manifest import AutomaticDepositJavaContractManifest
from .run_manifest import AutomaticDepositRunManifest


def verify_automatic_deposit_java_contract_manifest(
    manifest: AutomaticDepositJavaContractManifest | None,
    run_manifest: AutomaticDepositRunManifest,
    row: AutomaticDepositMatrixRow,
) -> tuple[str, ...]:
    required_keys = tuple(
        requirement.key
        for requirement in row.evidence_requirements
        if requirement.owner == "JAVA_DETERMINISTIC"
    )
    if not required_keys:
        return () if manifest is None else ("UNEXPECTED_JAVA_CONTRACT_MANIFEST",)
    if manifest is None:
        return ("JAVA_CONTRACT_MANIFEST_MISSING",)

    errors: list[str] = []
    if manifest.schema_version != "automatic-deposit-java-contract/junit-v1":
        errors.append("JAVA_CONTRACT_SCHEMA_VERSION_INVALID")
    if manifest.git_commit != run_manifest.git_commit:
        errors.append("JAVA_CONTRACT_GIT_COMMIT_MISMATCH")
    #20260901_kpopmodder: Keep malformed nested run identity inside the verification result.
    if not isinstance(
        run_manifest.artifact_identity,
        AutomaticDepositArtifactIdentity,
    ):
        errors.append("JAVA_CONTRACT_RUN_ARTIFACT_IDENTITY_NOT_TYPED")
    elif (
        manifest.source_jar_sha256.casefold()
        != run_manifest.artifact_identity.source_jar_sha256.casefold()
    ):
        errors.append("JAVA_CONTRACT_ARTIFACT_SHA256_MISMATCH")
    if not _is_sha256(manifest.test_report_sha256):
        errors.append("JAVA_CONTRACT_TEST_REPORT_SHA256_INVALID")
    expected_report_directory = Path(__file__).resolve().parents[5].joinpath(
        "plugins",
        "Minecraft",
        "runtime",
        "chatclef_fabric_1.20.1",
        "build",
        "test-results",
        "test",
    ).resolve(strict=False)
    if not manifest.test_report_paths or len(manifest.test_report_paths) > 512:
        errors.append("JAVA_CONTRACT_TEST_REPORT_PATHS_INVALID")
    for value in manifest.test_report_paths:
        report_path = Path(value)
        if (
            not report_path.is_absolute()
            or report_path.resolve(strict=False).parent
            != expected_report_directory
            or not report_path.name.startswith("TEST-")
            or report_path.suffix.casefold() != ".xml"
        ):
            errors.append("JAVA_CONTRACT_TEST_REPORT_PATH_INVALID")
    if manifest.gradle_invocation != "clean build --rerun-tasks":
        errors.append("JAVA_CONTRACT_GRADLE_INVOCATION_INVALID")
    if manifest.test_task != "test":
        errors.append("JAVA_CONTRACT_TEST_TASK_INVALID")
    if not re.fullmatch(
        r"[A-Za-z0-9][A-Za-z0-9._-]{0,127}", manifest.test_run_id
    ):
        errors.append("JAVA_CONTRACT_TEST_RUN_ID_INVALID")
    if len(manifest.results) > 64:
        errors.append("JAVA_CONTRACT_RESULT_COUNT_EXCEEDS_BOUND")
    keys = tuple(key for key, _value in manifest.results)
    if len(set(keys)) != len(keys):
        errors.append("JAVA_CONTRACT_RESULT_KEY_DUPLICATE")
    if set(keys) != set(required_keys):
        errors.append("JAVA_CONTRACT_RESULT_KEYS_MISMATCH")
    for key, value in manifest.results:
        if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_.-]{0,95}", key):
            errors.append("JAVA_CONTRACT_RESULT_KEY_INVALID")
        if value not in ("true", "false"):
            errors.append(f"JAVA_CONTRACT_RESULT_VALUE_INVALID:{key}")
    return tuple(errors)


def java_contract_evidence_mapping(
    manifest: AutomaticDepositJavaContractManifest | None,
) -> dict[str, object]:
    if manifest is None:
        return {}
    return {key: value == "true" for key, value in manifest.results}


def _is_sha256(value: object) -> bool:
    return bool(re.fullmatch(r"[0-9a-fA-F]{64}", str(value or "")))
