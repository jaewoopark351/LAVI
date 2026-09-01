#20260901_kpopmodder: Verify that recollected artifacts match the sealed run.
from __future__ import annotations

from ..artifact_identity_collection import AutomaticDepositArtifactIdentityCollection
from ..artifact_identity_contract import verify_automatic_deposit_artifact_identity
from ..run_manifest import AutomaticDepositRunManifest


def verify_artifact_recheck(
    run_manifest: AutomaticDepositRunManifest,
    artifact_collection: AutomaticDepositArtifactIdentityCollection,
) -> tuple[str, ...]:
    if not isinstance(artifact_collection, AutomaticDepositArtifactIdentityCollection):
        return ("ARTIFACT_RECHECK_NOT_TYPED",)
    if not artifact_collection.ok or artifact_collection.identity is None:
        return (f"ARTIFACT_RECHECK_FAILED:{artifact_collection.reason}",)
    if artifact_collection.identity != run_manifest.artifact_identity:
        return ("ARTIFACT_RECHECK_IDENTITY_MISMATCH",)
    if artifact_collection.runtime_snapshot_fingerprint != run_manifest.runtime_artifact_snapshot_fingerprint:
        return ("ARTIFACT_RUNTIME_SNAPSHOT_FINGERPRINT_MISMATCH",)
    if artifact_collection.mods_directory_fingerprint != run_manifest.mods_directory_fingerprint:
        return ("ARTIFACT_MODS_DIRECTORY_FINGERPRINT_MISMATCH",)
    return verify_automatic_deposit_artifact_identity(artifact_collection.identity).errors
