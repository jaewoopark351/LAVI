# 20260901_kpopmodder: Seal status, log-prefix, raw artifact, and static binding fingerprints together.
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field

from ....runtime_observer.log_prefix.jvm_runtime_artifact_log_prefix_observation import (
    JvmRuntimeArtifactLogPrefixObservation,
)
from ....runtime_observer.production_status_observation import (
    ProductionFabricStatusObservation,
)
from .static_manifest_binding import P1StaticManifestBinding
from .p1_harness_run_binding import P1HarnessRunBinding


_P1_RUNTIME_OBSERVATION_BUNDLE_SEAL = object()


@dataclass(frozen=True, slots=True)
class P1RuntimeObservationBundle:
    production_status: ProductionFabricStatusObservation
    jvm_log_prefix: JvmRuntimeArtifactLogPrefixObservation
    static_manifest_binding: P1StaticManifestBinding
    harness_run_binding: P1HarnessRunBinding
    status_fingerprint: str
    log_prefix_fingerprint: str
    raw_artifact_fingerprint: str
    binding_fingerprint: str
    harness_run_binding_fingerprint: str
    static_binding_classification: str
    static_binding_reason: str
    raw_run_manifest_id: str
    p1_operation_correlation_claimed: bool
    bundle_fingerprint: str
    _seal: object = field(default=None, repr=False, compare=False)
    _integrity: str = field(default="", repr=False, compare=False)

    def __post_init__(self) -> None:
        if not isinstance(self.production_status, ProductionFabricStatusObservation):
            raise ValueError("bundle production status must be sealed")
        if not isinstance(
            self.jvm_log_prefix,
            JvmRuntimeArtifactLogPrefixObservation,
        ):
            raise ValueError("bundle JVM log prefix must be sealed")
        if not isinstance(self.static_manifest_binding, P1StaticManifestBinding):
            raise ValueError("bundle static manifest binding must be sealed")
        if not isinstance(self.harness_run_binding, P1HarnessRunBinding):
            raise ValueError("bundle harness run binding must be sealed")
        raw = self.jvm_log_prefix.runtime_artifact_observation
        expected_values = (
            (self.status_fingerprint, self.production_status.observation_fingerprint),
            (self.log_prefix_fingerprint, self.jvm_log_prefix.observation_fingerprint),
            (self.raw_artifact_fingerprint, raw.observation_fingerprint),
            (
                self.binding_fingerprint,
                self.static_manifest_binding.binding_fingerprint,
            ),
            (
                self.harness_run_binding_fingerprint,
                self.harness_run_binding.binding_fingerprint,
            ),
            (
                self.static_binding_classification,
                self.static_manifest_binding.classification,
            ),
            (self.static_binding_reason, self.static_manifest_binding.reason),
            (self.raw_run_manifest_id, raw.run_manifest_id),
        )
        if any(actual != expected for actual, expected in expected_values):
            raise ValueError("P1 runtime observation bundle member mismatch")
        if self.p1_operation_correlation_claimed is not False:
            raise ValueError("static JVM manifest cannot claim P1 operation correlation")
        expected = _fingerprint(self)
        if self._seal is not _P1_RUNTIME_OBSERVATION_BUNDLE_SEAL:
            raise ValueError("P1 runtime observation bundle must be created by its collector")
        if self.bundle_fingerprint != expected or self._integrity != expected:
            raise ValueError("P1 runtime observation bundle integrity mismatch")


def _create_p1_runtime_observation_bundle(
    *,
    production_status: ProductionFabricStatusObservation,
    jvm_log_prefix: JvmRuntimeArtifactLogPrefixObservation,
    static_manifest_binding: P1StaticManifestBinding,
    harness_run_binding: P1HarnessRunBinding,
) -> P1RuntimeObservationBundle:
    raw = jvm_log_prefix.runtime_artifact_observation
    provisional = P1RuntimeObservationBundle.__new__(P1RuntimeObservationBundle)
    values = {
        "production_status": production_status,
        "jvm_log_prefix": jvm_log_prefix,
        "static_manifest_binding": static_manifest_binding,
        "harness_run_binding": harness_run_binding,
        "status_fingerprint": production_status.observation_fingerprint,
        "log_prefix_fingerprint": jvm_log_prefix.observation_fingerprint,
        "raw_artifact_fingerprint": raw.observation_fingerprint,
        "binding_fingerprint": static_manifest_binding.binding_fingerprint,
        "harness_run_binding_fingerprint": (
            harness_run_binding.binding_fingerprint
        ),
        "static_binding_classification": static_manifest_binding.classification,
        "static_binding_reason": static_manifest_binding.reason,
        "raw_run_manifest_id": raw.run_manifest_id,
        "p1_operation_correlation_claimed": False,
    }
    for name, value in values.items():
        object.__setattr__(provisional, name, value)
    fingerprint = _fingerprint(provisional)
    return P1RuntimeObservationBundle(
        **values,
        bundle_fingerprint=fingerprint,
        _seal=_P1_RUNTIME_OBSERVATION_BUNDLE_SEAL,
        _integrity=fingerprint,
    )


def _fingerprint(bundle: P1RuntimeObservationBundle) -> str:
    payload = json.dumps(
        {
            "binding_fingerprint": bundle.binding_fingerprint,
            "harness_run_binding_fingerprint": (
                bundle.harness_run_binding_fingerprint
            ),
            "log_prefix_fingerprint": bundle.log_prefix_fingerprint,
            "p1_operation_correlation_claimed": (
                bundle.p1_operation_correlation_claimed
            ),
            "raw_artifact_fingerprint": bundle.raw_artifact_fingerprint,
            "raw_run_manifest_id": bundle.raw_run_manifest_id,
            "static_binding_classification": (
                bundle.static_binding_classification
            ),
            "static_binding_reason": bundle.static_binding_reason,
            "status_fingerprint": bundle.status_fingerprint,
        },
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()
