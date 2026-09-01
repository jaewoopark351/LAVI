# 20260901_kpopmodder: Rebind a sealed P1 runtime bundle at its consumption boundary.
from __future__ import annotations

from .p1_runtime_observation_bundle import P1RuntimeObservationBundle
from .p1_runtime_observation_bundle_contract import (
    verify_p1_runtime_observation_bundle_inputs,
)
from .p1_harness_run_binding_contract import verify_p1_harness_run_binding


def verify_p1_runtime_observation_bundle_binding(
    bundle: object,
    run_manifest: object,
) -> tuple[str, ...]:
    if not isinstance(bundle, P1RuntimeObservationBundle):
        return ("P1_RUNTIME_OBSERVATION_BUNDLE_NOT_TYPED",)
    errors = list(
        verify_p1_runtime_observation_bundle_inputs(
            bundle.production_status,
            bundle.jvm_log_prefix,
            run_manifest,
        )
    )
    errors.extend(
        verify_p1_harness_run_binding(
            bundle.harness_run_binding,
            run_manifest,
        )
    )
    return tuple(dict.fromkeys(errors))
