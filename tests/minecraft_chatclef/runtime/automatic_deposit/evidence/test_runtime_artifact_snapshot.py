#20260831_kpopmodder: Lock strict runtime code-source and loader mod evidence parsing.
from __future__ import annotations

import unittest
from dataclasses import replace
from pathlib import Path

from .evidence_test_fixture import runtime_status
from .runtime_artifact_snapshot_collector import (
    collect_automatic_deposit_runtime_artifact_snapshot,
)


class AutomaticDepositRuntimeArtifactSnapshotTests(unittest.TestCase):
    def test_canonical_production_status_without_artifact_fails_at_missing_status(self):
        snapshot, reason = collect_automatic_deposit_runtime_artifact_snapshot(
            {
                "backend_id": "fabric_chatclef",
                "enabled": True,
                "connected": True,
                "lifecycle_state": "connected",
                "details": {
                    "sessions": {},
                    "commands": {"active_request_id": None},
                },
            }
        )

        self.assertIsNone(snapshot)
        self.assertEqual("RUNTIME_ARTIFACT_STATUS_MISSING", reason)

    def test_collects_sealed_snapshot_and_derives_loaded_code_source(self):
        chatclef = Path("C:/minecraft/instance/mods/chatclef.jar")

        snapshot, reason = collect_automatic_deposit_runtime_artifact_snapshot(
            runtime_status(chatclef)
        )

        self.assertEqual(reason, "RUNTIME_ARTIFACT_SNAPSHOT_COLLECTED")
        self.assertIsNotNone(snapshot)
        self.assertEqual(
            snapshot.loaded_code_source.casefold(),
            str(chatclef.resolve(strict=False)).casefold(),
        )
        self.assertEqual("altoclef", snapshot.loaded_mods[0].mod_id)
        with self.assertRaises(ValueError):
            replace(snapshot, loaded_code_source="C:/forged/chatclef.jar")

    def test_rejects_unknown_artifact_status_fields(self):
        snapshot, reason = collect_automatic_deposit_runtime_artifact_snapshot(
            runtime_status(
                Path("C:/minecraft/instance/mods/chatclef.jar"),
                artifact_extra={"caller_verified": True},
            )
        )

        self.assertIsNone(snapshot)
        self.assertEqual(reason, "RUNTIME_ARTIFACT_STATUS_SCHEMA_INVALID")

    def test_rejects_loaded_code_source_that_differs_from_loader_mod(self):
        payload = runtime_status(
            Path("C:/minecraft/instance/mods/chatclef.jar")
        )
        payload["details"]["runtime_artifact"]["loaded_code_source"] = (
            "C:/minecraft/instance/mods/other.jar"
        )

        snapshot, reason = collect_automatic_deposit_runtime_artifact_snapshot(
            payload
        )

        self.assertIsNone(snapshot)
        self.assertEqual(reason, "RUNTIME_CHATCLEF_CODE_SOURCE_MISMATCH")


if __name__ == "__main__":
    unittest.main()
