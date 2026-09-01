# 20260901_kpopmodder: Collect a P1 runtime bundle only after every sealed binding contract passes.
from __future__ import annotations

from ....run_manifest import AutomaticDepositRunManifest
from ....runtime_observer.log_prefix.jvm_runtime_artifact_log_prefix_observation import (
    JvmRuntimeArtifactLogPrefixObservation,
)
from ....runtime_observer.production_status_observation import (
    ProductionFabricStatusObservation,
)
from .p1_runtime_observation_bundle import (
    P1RuntimeObservationBundle,
    _create_p1_runtime_observation_bundle,
)
from .p1_runtime_observation_bundle_contract import (
    verify_p1_runtime_observation_bundle_inputs,
)
from .static_manifest_binding_classifier import (
    classify_p1_static_manifest_binding,
)
from .p1_harness_run_binding import _create_p1_harness_run_binding


def collect_p1_runtime_observation_bundle(
    status: object,
    log_prefix: object,
    run_manifest: object,
) -> tuple[P1RuntimeObservationBundle | None, str]:
    errors = verify_p1_runtime_observation_bundle_inputs(
        status,
        log_prefix,
        run_manifest,
    )
    if errors:
        return None, f"P1_RUNTIME_OBSERVATION_INPUTS_INVALID:{errors[0]}"
    if not isinstance(status, ProductionFabricStatusObservation):
        return None, "P1_RUNTIME_OBSERVATION_STATUS_NOT_TYPED"
    if not isinstance(log_prefix, JvmRuntimeArtifactLogPrefixObservation):
        return None, "P1_RUNTIME_OBSERVATION_LOG_PREFIX_NOT_TYPED"
    if not isinstance(run_manifest, AutomaticDepositRunManifest):
        return None, "P1_RUNTIME_OBSERVATION_RUN_MANIFEST_NOT_TYPED"

    static_binding, binding_reason = classify_p1_static_manifest_binding(
        status,
        log_prefix.runtime_artifact_observation,
    )
    if static_binding is None:
        return None, binding_reason
    return (
        _create_p1_runtime_observation_bundle(
            production_status=status,
            jvm_log_prefix=log_prefix,
            static_manifest_binding=static_binding,
            harness_run_binding=_create_p1_harness_run_binding(run_manifest),
        ),
        "P1_RUNTIME_OBSERVATION_BUNDLE_COLLECTED",
    )
