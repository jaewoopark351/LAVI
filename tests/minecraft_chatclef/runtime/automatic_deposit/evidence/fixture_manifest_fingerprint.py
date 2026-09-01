#20260831_kpopmodder: Derive fixture identity from canonical immutable contents.
from __future__ import annotations

import hashlib
import json

from .fixture_manifest import AutomaticDepositFixtureManifest


def automatic_deposit_fixture_fingerprint(
    manifest: AutomaticDepositFixtureManifest,
) -> str:
    carry_on = manifest.carry_on_identity
    payload = {
        "schema_version": manifest.schema_version,
        "row_id": manifest.row_id,
        "fixture_id": manifest.fixture_id,
        "world_snapshot_id": manifest.world_snapshot_id,
        "inventory_snapshot_id": manifest.inventory_snapshot_id,
        "operator_confirmed": manifest.operator_confirmed,
        "carry_on": {
            "presence": carry_on.presence.value,
            "loaded": carry_on.loaded,
            "jar_path": carry_on.jar_path,
            "jar_sha256": carry_on.jar_sha256,
            "version": carry_on.version,
            "config_fingerprint": carry_on.config_fingerprint,
            "loader_mod_list_fingerprint": carry_on.loader_mod_list_fingerprint,
            "mods_directory_fingerprint": carry_on.mods_directory_fingerprint,
            "observation_reason": carry_on.observation_reason,
        },
        "container_type": manifest.container_type,
        "trusted_destination_fingerprint": manifest.trusted_destination_fingerprint,
        "diagnostics_mode": manifest.diagnostics_mode,
        "attributes": list(manifest.attributes),
    }
    canonical = json.dumps(
        payload,
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")
    return hashlib.sha256(canonical).hexdigest()
