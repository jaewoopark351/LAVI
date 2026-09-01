#20260901_kpopmodder: Verify that Carry On identity shares the sealed runtime snapshot.
from __future__ import annotations

from ..carry_on_identity_collection import AutomaticDepositCarryOnIdentityCollection
from ..fixture_manifest import AutomaticDepositFixtureManifest
from ..run_manifest import AutomaticDepositRunManifest


def verify_carry_on_recheck(
    run_manifest: AutomaticDepositRunManifest,
    fixture_manifest: AutomaticDepositFixtureManifest,
    collection: AutomaticDepositCarryOnIdentityCollection,
) -> tuple[str, ...]:
    if not collection.ok or collection.identity is None:
        return (f"CARRY_ON_IDENTITY_RECHECK_FAILED:{collection.reason}",)
    if collection.identity != fixture_manifest.carry_on_identity:
        return ("CARRY_ON_IDENTITY_RECHECK_MISMATCH",)
    if collection.runtime_snapshot_fingerprint != run_manifest.runtime_artifact_snapshot_fingerprint:
        return ("CARRY_ON_RUNTIME_SNAPSHOT_FINGERPRINT_MISMATCH",)
    if collection.identity.loader_mod_list_fingerprint != run_manifest.loader_mod_list_fingerprint:
        return ("CARRY_ON_RUNTIME_SNAPSHOT_FINGERPRINT_MISMATCH",)
    if collection.identity.mods_directory_fingerprint != run_manifest.mods_directory_fingerprint:
        return ("CARRY_ON_MODS_DIRECTORY_FINGERPRINT_MISMATCH",)
    return ()
