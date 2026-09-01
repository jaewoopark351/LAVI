#20260831_kpopmodder: Lock installed and absent Carry On fixture identity semantics.
from __future__ import annotations

import unittest

from .carry_on_identity import AutomaticDepositCarryOnIdentity
from .carry_on_identity_contract import (
    verify_automatic_deposit_carry_on_identity,
)
from .carry_on_presence import AutomaticDepositCarryOnPresence


class AutomaticDepositCarryOnIdentityTests(unittest.TestCase):
    def test_installed_identity_requires_version_jar_and_config_hashes(self):
        identity = AutomaticDepositCarryOnIdentity(
            AutomaticDepositCarryOnPresence.INSTALLED,
            True,
            jar_path="C:/instance/mods/carryon.jar",
            jar_sha256="a" * 64,
            version="2.1.2.7",
            config_fingerprint="b" * 64,
            loader_mod_list_fingerprint="c" * 64,
            mods_directory_fingerprint="d" * 64,
        )

        self.assertTrue(
            verify_automatic_deposit_carry_on_identity(identity).ok
        )

    def test_absent_identity_rejects_residual_jar_fields(self):
        identity = AutomaticDepositCarryOnIdentity(
            AutomaticDepositCarryOnPresence.ABSENT,
            False,
            jar_path="C:/instance/mods/carryon.jar",
            loader_mod_list_fingerprint="c" * 64,
            mods_directory_fingerprint="d" * 64,
            observation_reason="loader and mods snapshots show absence",
        )

        verified = verify_automatic_deposit_carry_on_identity(identity)

        self.assertFalse(verified.ok)
        self.assertIn("CARRY_ON_ABSENCE_HAS_ARTIFACT_FIELDS", verified.errors)


if __name__ == "__main__":
    unittest.main()
