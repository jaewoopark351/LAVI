#20260818_kpopmodder: Verify completed invocation fingerprints cannot be reused.
from __future__ import annotations

import unittest
from pathlib import Path

from .one_shot_invocation_ledger import OneShotInvocationLedger


class OneShotInvocationLedgerTests(unittest.TestCase):
    def test_marked_invocation_becomes_consumed_without_raw_id_storage(self):
        repository_root = Path(__file__).resolve().parents[4]
        records: dict[str, dict[str, object]] = {}

        def exists(path):
            return path.name in records

        def read(path):
            return records[path.name]

        def write(path, payload):
            if path.name in records:
                return False
            records[path.name] = dict(payload)
            return True

        ledger = OneShotInvocationLedger(
            str(repository_root),
            state_directory=repository_root / "tests" / "tmp_ledger_fixture",
            record_exists=exists,
            record_reader=read,
            record_writer=write,
        )

        before = ledger.status("batch-invocation-1")
        marked = ledger.mark_consumed("batch-invocation-1")
        after = ledger.status("batch-invocation-1")

        self.assertIs(False, before["consumed"])
        self.assertIs(True, marked["consumed"])
        self.assertIs(True, after["consumed"])
        self.assertNotIn("batch-invocation-1", str(records))

    def test_invalid_existing_record_fails_closed(self):
        repository_root = Path(__file__).resolve().parents[4]
        ledger = OneShotInvocationLedger(
            str(repository_root),
            state_directory=repository_root / "tests" / "tmp_ledger_fixture",
            record_exists=lambda _path: True,
            record_reader=lambda _path: {"invocation_fingerprint": "wrong"},
        )

        result = ledger.status("batch-invocation-1")

        self.assertIs(False, result["ok"])
        self.assertIs(True, result["consumed"])


if __name__ == "__main__":
    unittest.main()
