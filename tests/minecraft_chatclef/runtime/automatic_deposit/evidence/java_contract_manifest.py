#20260831_kpopmodder: Seal deterministic Java assertions collected from report bytes.
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field

_JAVA_CONTRACT_SEAL = object()


@dataclass(frozen=True, slots=True)
class AutomaticDepositJavaContractManifest:
    schema_version: str
    git_commit: str
    source_jar_sha256: str
    test_report_paths: tuple[str, ...]
    test_report_sha256: str
    gradle_invocation: str
    test_task: str
    test_run_id: str
    results: tuple[tuple[str, str], ...]
    _seal: object = field(repr=False, compare=False)
    _integrity: str = field(repr=False, compare=False)

    def __post_init__(self) -> None:
        if self._seal is not _JAVA_CONTRACT_SEAL:
            raise ValueError("Java contract manifest must be collected from report bytes")
        if self._integrity != _manifest_integrity(
            self.schema_version,
            self.git_commit,
            self.source_jar_sha256,
            self.test_report_paths,
            self.test_report_sha256,
            self.gradle_invocation,
            self.test_task,
            self.test_run_id,
            self.results,
        ):
            raise ValueError("Java contract manifest integrity mismatch")


def _create_java_contract_manifest(
    *,
    schema_version: str,
    git_commit: str,
    source_jar_sha256: str,
    test_report_paths: tuple[str, ...],
    test_report_sha256: str,
    gradle_invocation: str,
    test_task: str,
    test_run_id: str,
    results: tuple[tuple[str, str], ...],
) -> AutomaticDepositJavaContractManifest:
    normalized_results = tuple(sorted(results))
    return AutomaticDepositJavaContractManifest(
        schema_version,
        git_commit,
        source_jar_sha256,
        test_report_paths,
        test_report_sha256,
        gradle_invocation,
        test_task,
        test_run_id,
        normalized_results,
        _JAVA_CONTRACT_SEAL,
        _manifest_integrity(
            schema_version,
            git_commit,
            source_jar_sha256,
            test_report_paths,
            test_report_sha256,
            gradle_invocation,
            test_task,
            test_run_id,
            normalized_results,
        ),
    )


def _manifest_integrity(
    schema_version: str,
    git_commit: str,
    source_jar_sha256: str,
    test_report_paths: tuple[str, ...],
    test_report_sha256: str,
    gradle_invocation: str,
    test_task: str,
    test_run_id: str,
    results: tuple[tuple[str, str], ...],
) -> str:
    raw = json.dumps(
        (
            schema_version,
            git_commit,
            source_jar_sha256,
            test_report_paths,
            test_report_sha256,
            gradle_invocation,
            test_task,
            test_run_id,
            results,
        ),
        ensure_ascii=False,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(raw).hexdigest()
