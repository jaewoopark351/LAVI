# 20260901_kpopmodder: Fail closed unless sealed status and typed run evidence name the exact production backend.
from __future__ import annotations

from ..run_manifest import AutomaticDepositRunManifest
from .production_backend_identity import PRODUCTION_FABRIC_BACKEND_ID
from .production_status_observation import ProductionFabricStatusObservation


def verify_production_status_run_binding(
    status: object,
    run_manifest: object,
) -> tuple[str, ...]:
    errors: list[str] = []
    if not isinstance(status, ProductionFabricStatusObservation):
        errors.append("PRODUCTION_STATUS_OBSERVATION_NOT_TYPED")
    if not isinstance(run_manifest, AutomaticDepositRunManifest):
        errors.append("PRODUCTION_STATUS_RUN_MANIFEST_NOT_TYPED")
    if errors:
        return tuple(errors)

    if status.backend_id != PRODUCTION_FABRIC_BACKEND_ID:
        errors.append("PRODUCTION_STATUS_BACKEND_NOT_CANONICAL")
    if run_manifest.backend != PRODUCTION_FABRIC_BACKEND_ID:
        errors.append("RUN_MANIFEST_BACKEND_NOT_CANONICAL")
    if status.backend_id != run_manifest.backend:
        errors.append("PRODUCTION_STATUS_RUN_BACKEND_MISMATCH")
    return tuple(errors)
