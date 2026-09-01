# 20260901_kpopmodder: Compose the three existing runtime-to-run contracts without weakening them.
from __future__ import annotations

from ....run_manifest import AutomaticDepositRunManifest
from ....runtime_observer.log_prefix.jvm_runtime_artifact_log_prefix_binding_contract import (
    verify_jvm_runtime_artifact_log_prefix_binding,
)
from ....runtime_observer.log_prefix.jvm_runtime_artifact_log_prefix_observation import (
    JvmRuntimeArtifactLogPrefixObservation,
)
from ....runtime_observer.production_status_run_binding_contract import (
    verify_production_status_run_binding,
)
from ..runtime_artifact_binding_contract import (
    verify_p1_jvm_runtime_artifact_binding,
)
from .static_manifest_context_contract import (
    verify_p1_static_manifest_context,
)
from .p1_harness_run_binding_contract import verify_p1_harness_run_input


def verify_p1_runtime_observation_bundle_inputs(
    status: object,
    log_prefix: object,
    run_manifest: object,
) -> tuple[str, ...]:
    errors = list(verify_p1_harness_run_input(run_manifest))
    errors.extend(verify_production_status_run_binding(status, run_manifest))
    errors.extend(
        verify_jvm_runtime_artifact_log_prefix_binding(log_prefix, run_manifest)
    )
    if isinstance(
        log_prefix,
        JvmRuntimeArtifactLogPrefixObservation,
    ) and isinstance(run_manifest, AutomaticDepositRunManifest):
        errors.extend(
            verify_p1_static_manifest_context(
                log_prefix.runtime_artifact_observation
            )
        )
        errors.extend(
            verify_p1_jvm_runtime_artifact_binding(
                log_prefix.runtime_artifact_observation,
                run_manifest,
            )
        )
    return tuple(dict.fromkeys(errors))
