#20260818_kpopmodder: Verify guard clearing requires matching invocation evidence.
from __future__ import annotations

import unittest
from pathlib import Path

from .invocation_fingerprint import invocation_fingerprint
from .one_shot_run_reconciler import OneShotRunReconciler


class OneShotRunReconcilerTests(unittest.TestCase):
    def test_matching_records_are_deleted_marker_first(self):
        repository_root = Path(__file__).resolve().parents[4]
        deleted: list[str] = []
        expected = invocation_fingerprint("invocation-1")
        records = {
            "live-run-block.json": {"invocation_fingerprint": expected},
            "reconciliation-required.json": {
                "invocation_fingerprint": expected
            },
        }
        ledger = _InvocationLedger()
        reconciler = OneShotRunReconciler(
            str(repository_root),
            state_directory=(repository_root / "tests" / "tmp_reconciler_fixture"),
            record_exists=lambda path: path.name in records,
            record_reader=lambda path: records[path.name],
            record_deleter=lambda path: deleted.append(path.name),
            invocation_ledger=ledger,
        )

        result = reconciler.reconcile("invocation-1")

        self.assertTrue(result["ok"])
        self.assertEqual(["invocation-1"], ledger.consumed)
        self.assertEqual(
            ["reconciliation-required.json", "live-run-block.json"],
            deleted,
        )

    def test_invocation_mismatch_preserves_every_record(self):
        repository_root = Path(__file__).resolve().parents[4]
        deleted: list[str] = []
        records = {
            "live-run-block.json": {
                "invocation_fingerprint": invocation_fingerprint("other-invocation")
            }
        }
        reconciler = OneShotRunReconciler(
            str(repository_root),
            state_directory=(repository_root / "tests" / "tmp_reconciler_fixture"),
            record_exists=lambda path: path.name in records,
            record_reader=lambda path: records[path.name],
            record_deleter=lambda path: deleted.append(path.name),
            invocation_ledger=_InvocationLedger(),
        )

        result = reconciler.reconcile("invocation-1")

        self.assertFalse(result["ok"])
        self.assertIn("does not match", result["reason"])
        self.assertEqual([], deleted)


class _InvocationLedger:
    def __init__(self):
        self.consumed: list[str] = []

    def mark_consumed(self, invocation_id):
        self.consumed.append(invocation_id)
        return {"ok": True, "consumed": True, "reason": "recorded"}


if __name__ == "__main__":
    unittest.main()
