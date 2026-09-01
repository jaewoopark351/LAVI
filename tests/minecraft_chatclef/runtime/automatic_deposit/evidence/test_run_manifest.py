#20260831_kpopmodder: Lock immutable run-to-row, artifact, process, and path identity.
from __future__ import annotations

import unittest
from dataclasses import FrozenInstanceError, replace
from pathlib import Path

from ...preflight.command_fingerprint import command_fingerprint
from ..scenario.matrix_catalog import automatic_deposit_matrix_catalog
from .artifact_identity import AutomaticDepositArtifactIdentity
from .latest_log_byte_cursor import AutomaticDepositLatestLogByteCursor
from .run_manifest_contract import verify_automatic_deposit_run_manifest
from .run_manifest_factory import build_automatic_deposit_run_manifest


class AutomaticDepositRunManifestTests(unittest.TestCase):
    def test_valid_manual_manifest_has_action_but_no_submission_identity(self):
        row = next(
            row
            for row in automatic_deposit_matrix_catalog()
            if row.row_id == "R4"
        )
        manifest = _valid_run_manifest(row)

        verified = verify_automatic_deposit_run_manifest(manifest, row)

        self.assertTrue(verified.ok)
        self.assertTrue(manifest.operator_action_fingerprint)
        self.assertEqual("", manifest.submission_invocation_fingerprint)

        with self.assertRaises(FrozenInstanceError):
            manifest.run_id = "changed"

    def test_p1_manual_manifest_never_relabels_invocation_as_submission(self):
        row = next(
            row for row in automatic_deposit_matrix_catalog() if row.row_id == "P1"
        )
        manifest = _valid_run_manifest(
            row,
            submission_invocation_id="future-supervised-p1-invocation",
        )

        verified = verify_automatic_deposit_run_manifest(manifest, row)

        self.assertTrue(verified.ok)
        self.assertEqual(
            command_fingerprint("@store_home"),
            manifest.operator_action_fingerprint,
        )
        self.assertEqual("", manifest.submission_invocation_fingerprint)

    def test_deployed_jar_outside_instance_mods_is_inconclusive(self):
        row = next(
            row
            for row in automatic_deposit_matrix_catalog()
            if row.row_id == "R3"
        )
        manifest = build_automatic_deposit_run_manifest(
            run_id="run-r3-001",
            created_at_utc="2026-08-31T12:00:00Z",
            repository_root=_repository_root(),
            git_commit="a" * 40,
            git_worktree_fingerprint="b" * 64,
            harness_fingerprint="c" * 64,
            row=row,
            operation_id="operation-r3-001",
            fixture_fingerprint="d" * 64,
            artifact_identity=AutomaticDepositArtifactIdentity(
                source_jar_path="C:/repo/LAVI/build/chatclef.jar",
                source_jar_sha256="1" * 64,
                deployed_jar_path="C:/wrong/chatclef.jar",
                deployed_jar_sha256="1" * 64,
                loaded_code_source="C:/wrong/chatclef.jar",
                discovered_chatclef_jars=("C:/wrong/chatclef.jar",),
            ),
            runtime_artifact_snapshot_fingerprint="7" * 64,
            loader_mod_list_fingerprint="9" * 64,
            mods_directory_fingerprint="8" * 64,
            instance_root="C:/minecraft/instance",
            mods_directory="C:/minecraft/instance/mods",
            latest_log_path="C:/minecraft/instance/logs/latest.log",
            latest_log_cursor=_latest_log_cursor(),
            crash_reports_directory="C:/minecraft/instance/crash-reports",
            lavi_process_identity_fingerprint="e" * 64,
            minecraft_process_identity_fingerprint="f" * 64,
            backend="fabric_chatclef",
            instance="automatic-deposit-test",
            world="automatic-deposit-fixture",
            diagnostics_mode="BOUNDARY",
        )

        verified = verify_automatic_deposit_run_manifest(manifest, row)

        self.assertFalse(verified.ok)
        self.assertIn("DEPLOYED_JAR_OUTSIDE_MODS_DIRECTORY", verified.errors)

    #20260901_kpopmodder: Keep malformed nested evidence inside an explicit verdict.
    def test_nested_artifact_identity_type_is_inconclusive(self):
        row = next(
            row for row in automatic_deposit_matrix_catalog() if row.row_id == "R4"
        )
        manifest = replace(_valid_run_manifest(row), artifact_identity=None)

        verified = verify_automatic_deposit_run_manifest(manifest, row)

        self.assertFalse(verified.ok)
        self.assertIn("RUN_ARTIFACT_IDENTITY_NOT_TYPED", verified.errors)

    def test_nested_latest_log_cursor_type_is_inconclusive(self):
        row = next(
            row for row in automatic_deposit_matrix_catalog() if row.row_id == "R4"
        )
        manifest = replace(_valid_run_manifest(row), latest_log_cursor=None)

        verified = verify_automatic_deposit_run_manifest(manifest, row)

        self.assertFalse(verified.ok)
        self.assertIn("LATEST_LOG_CURSOR_NOT_TYPED", verified.errors)

    def test_legacy_backend_alias_is_inconclusive(self):
        row = next(
            row for row in automatic_deposit_matrix_catalog() if row.row_id == "R4"
        )
        manifest = replace(
            _valid_run_manifest(row),
            backend="fabric_chatclef_1.20.1",
        )

        verified = verify_automatic_deposit_run_manifest(manifest, row)

        self.assertFalse(verified.ok)
        self.assertIn("RUN_BACKEND_NOT_CANONICAL", verified.errors)


def _artifact_identity():
    return AutomaticDepositArtifactIdentity(
        source_jar_path=str(Path(_repository_root()).joinpath("build", "chatclef.jar")),
        source_jar_sha256="1" * 64,
        deployed_jar_path="C:/minecraft/instance/mods/chatclef.jar",
        deployed_jar_sha256="1" * 64,
        loaded_code_source="C:/minecraft/instance/mods/chatclef.jar",
        discovered_chatclef_jars=(
            "C:/minecraft/instance/mods/chatclef.jar",
        ),
    )


def _valid_run_manifest(row, *, submission_invocation_id=""):
    return build_automatic_deposit_run_manifest(
        run_id=f"run-{row.row_id.lower()}-001",
        created_at_utc="2026-08-31T12:00:00Z",
        repository_root=_repository_root(),
        git_commit="a" * 40,
        git_worktree_fingerprint="b" * 64,
        harness_fingerprint="c" * 64,
        row=row,
        operation_id=f"operation-{row.row_id.lower()}-001",
        fixture_fingerprint="d" * 64,
        artifact_identity=_artifact_identity(),
        runtime_artifact_snapshot_fingerprint="7" * 64,
        loader_mod_list_fingerprint="9" * 64,
        mods_directory_fingerprint="8" * 64,
        instance_root="C:/minecraft/instance",
        mods_directory="C:/minecraft/instance/mods",
        latest_log_path="C:/minecraft/instance/logs/latest.log",
        latest_log_cursor=_latest_log_cursor(),
        crash_reports_directory="C:/minecraft/instance/crash-reports",
        lavi_process_identity_fingerprint="e" * 64,
        minecraft_process_identity_fingerprint="f" * 64,
        backend="fabric_chatclef",
        instance="automatic-deposit-test",
        world="automatic-deposit-fixture",
        diagnostics_mode="BOUNDARY",
        submission_invocation_id=submission_invocation_id,
    )


def _repository_root():
    return str(Path(__file__).resolve().parents[5])


def _latest_log_cursor():
    return AutomaticDepositLatestLogByteCursor(
        path="C:/minecraft/instance/logs/latest.log",
        device=1,
        inode=2,
        size=100,
        mtime_ns=200,
        prefix_sha256="2" * 64,
    )


if __name__ == "__main__":
    unittest.main()
