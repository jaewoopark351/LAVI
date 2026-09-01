# 20260901_kpopmodder: Verify the independently hashed JVM CodeSource against the sealed run artifact.
from __future__ import annotations

import re
from pathlib import Path

from ...artifact_identity import AutomaticDepositArtifactIdentity
from ...run_manifest import AutomaticDepositRunManifest
from ...runtime_observer.jvm_runtime_artifact_observation import (
    JvmRuntimeArtifactObservation,
)


_SHA256 = re.compile(r"[0-9a-fA-F]{64}\Z", re.ASCII)


def verify_p1_jvm_runtime_artifact_binding(
    observation: object,
    run_manifest: object,
) -> tuple[str, ...]:
    if not isinstance(observation, JvmRuntimeArtifactObservation):
        return ("P1_JVM_RUNTIME_ARTIFACT_OBSERVATION_NOT_TYPED",)
    if not isinstance(run_manifest, AutomaticDepositRunManifest):
        return ("P1_JVM_RUNTIME_RUN_MANIFEST_NOT_TYPED",)
    artifact = run_manifest.artifact_identity
    if not isinstance(artifact, AutomaticDepositArtifactIdentity):
        return ("P1_RUN_ARTIFACT_IDENTITY_NOT_TYPED",)
    errors: list[str] = []
    observed_path = _canonical(observation.runtime_code_source_path)
    deployed_path = _canonical(artifact.deployed_jar_path)
    loaded_path = _canonical(artifact.loaded_code_source)
    if observed_path is None:
        errors.append("P1_JVM_RUNTIME_CODESOURCE_PATH_INVALID")
    if deployed_path is None:
        errors.append("P1_RUN_ARTIFACT_DEPLOYED_PATH_INVALID")
    if loaded_path is None:
        errors.append("P1_RUN_ARTIFACT_LOADED_CODESOURCE_INVALID")
    if (
        observed_path is not None
        and deployed_path is not None
        and observed_path != deployed_path
    ):
        errors.append("P1_JVM_RUNTIME_CODESOURCE_PATH_NOT_DEPLOYED_JAR")
    if (
        observed_path is not None
        and loaded_path is not None
        and observed_path != loaded_path
    ):
        errors.append("P1_JVM_RUNTIME_CODESOURCE_PATH_NOT_RUN_LOADED_CODE_SOURCE")
    observed_sha256 = _sha256(observation.runtime_code_source_sha256)
    deployed_sha256 = _sha256(artifact.deployed_jar_sha256)
    source_sha256 = _sha256(artifact.source_jar_sha256)
    if observed_sha256 is None:
        errors.append("P1_JVM_RUNTIME_CODESOURCE_SHA256_INVALID")
    if deployed_sha256 is None:
        errors.append("P1_RUN_ARTIFACT_DEPLOYED_SHA256_INVALID")
    if source_sha256 is None:
        errors.append("P1_RUN_ARTIFACT_SOURCE_SHA256_INVALID")
    if (
        observed_sha256 is not None
        and deployed_sha256 is not None
        and observed_sha256 != deployed_sha256
    ):
        errors.append("P1_JVM_RUNTIME_CODESOURCE_SHA256_NOT_DEPLOYED_JAR")
    if (
        observed_sha256 is not None
        and source_sha256 is not None
        and observed_sha256 != source_sha256
    ):
        errors.append("P1_JVM_RUNTIME_CODESOURCE_SHA256_NOT_SOURCE_JAR")
    if observation.sha256_evidence_source != (
        "INDEPENDENT_STABLE_CODESOURCE_FILE_READ"
    ):
        errors.append("P1_JVM_RUNTIME_CODESOURCE_SHA256_SOURCE_INVALID")
    return tuple(errors)


def _canonical(value: object) -> str | None:
    if not isinstance(value, str) or not value or value != value.strip():
        return None
    try:
        path = Path(value)
        if not path.is_absolute():
            return None
        return str(path.resolve(strict=False)).casefold()
    except (OSError, ValueError):
        return None


def _sha256(value: object) -> str | None:
    if not isinstance(value, str) or _SHA256.fullmatch(value) is None:
        return None
    return value.casefold()
