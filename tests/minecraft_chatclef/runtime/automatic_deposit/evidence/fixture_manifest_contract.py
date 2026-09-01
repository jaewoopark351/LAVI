#20260831_kpopmodder: Validate a fixture only against its exact catalog row.
from __future__ import annotations

import re

from ..scenario.matrix_row import AutomaticDepositMatrixRow
from .carry_on_identity import AutomaticDepositCarryOnIdentity
from .carry_on_identity_contract import (
    verify_automatic_deposit_carry_on_identity,
)
from .fixture_manifest import AutomaticDepositFixtureManifest
from .fixture_manifest_fingerprint import automatic_deposit_fixture_fingerprint
from .fixture_manifest_verification import (
    AutomaticDepositFixtureManifestVerification,
)


def verify_automatic_deposit_fixture_manifest(
    manifest: AutomaticDepositFixtureManifest,
    row: AutomaticDepositMatrixRow,
) -> AutomaticDepositFixtureManifestVerification:
    errors: list[str] = []
    if manifest.schema_version != "automatic-deposit-fixture/v1":
        errors.append("FIXTURE_SCHEMA_VERSION_INVALID")
    if manifest.row_id != row.row_id:
        errors.append("FIXTURE_ROW_ID_MISMATCH")
    if not manifest.fixture_id.strip():
        errors.append("FIXTURE_ID_MISSING")
    if not _is_sha256(manifest.fixture_fingerprint):
        errors.append("FIXTURE_FINGERPRINT_INVALID")
    elif isinstance(
        manifest.carry_on_identity,
        AutomaticDepositCarryOnIdentity,
    ) and manifest.fixture_fingerprint.lower() != automatic_deposit_fixture_fingerprint(
        manifest
    ):
        errors.append("FIXTURE_FINGERPRINT_CONTENT_MISMATCH")
    if manifest.operator_confirmed is not True:
        errors.append("FIXTURE_NOT_OPERATOR_CONFIRMED")
    attribute_keys = tuple(key for key, _value in manifest.attributes)
    if len(manifest.attributes) > 64:
        errors.append("FIXTURE_ATTRIBUTE_COUNT_EXCEEDS_BOUND")
    if any(not key.strip() for key in attribute_keys):
        errors.append("FIXTURE_ATTRIBUTE_KEY_EMPTY")
    if any(len(key) > 64 or len(value) > 512 for key, value in manifest.attributes):
        errors.append("FIXTURE_ATTRIBUTE_EXCEEDS_BOUND")
    if len(set(attribute_keys)) != len(attribute_keys):
        errors.append("FIXTURE_ATTRIBUTE_KEY_DUPLICATE")
    standard_keys = {
        "fixture_fingerprint",
        "world_snapshot_id",
        "inventory_snapshot_id",
        "operator_confirmed",
        "carry_on_state",
        "container_type",
        "trusted_destination_fingerprint",
        "diagnostics_mode",
    }
    if any(key in standard_keys for key in attribute_keys):
        errors.append("FIXTURE_ATTRIBUTE_SHADOWS_STANDARD_FIELD")
    carry_on = verify_automatic_deposit_carry_on_identity(
        manifest.carry_on_identity
    )
    errors.extend(carry_on.errors)
    if not isinstance(
        manifest.carry_on_identity,
        AutomaticDepositCarryOnIdentity,
    ):
        return AutomaticDepositFixtureManifestVerification(
            ok=False,
            reason="FIXTURE_MANIFEST_INCONCLUSIVE",
            errors=tuple(errors),
        )
    evidence = manifest.as_evidence_mapping()
    errors.extend(
        f"FIXTURE_FIELD_MISSING:{field}"
        for field in row.required_fixture_fields
        if not _present(evidence.get(field))
    )
    errors.extend(
        f"FIXTURE_VALUE_MISMATCH:{key}"
        for key, expected in row.expected_fixture_values
        if str(evidence.get(key) or "").strip() != expected
    )
    return AutomaticDepositFixtureManifestVerification(
        ok=not errors,
        reason=(
            "FIXTURE_MANIFEST_VERIFIED"
            if not errors
            else "FIXTURE_MANIFEST_INCONCLUSIVE"
        ),
        errors=tuple(errors),
    )


def _is_sha256(value: object) -> bool:
    return bool(re.fullmatch(r"[0-9a-fA-F]{64}", str(value or "")))


def _present(value: object) -> bool:
    if value is None or value is False:
        return False
    if isinstance(value, str):
        return bool(value.strip())
    return True
