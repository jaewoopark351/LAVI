#20260901_kpopmodder: Lock one transport-neutral sealed latest.log delta contract.
from __future__ import annotations

import unittest
from pathlib import Path

from ..latest_log_byte_cursor import (
    AutomaticDepositLatestLogByteCursor,
    automatic_deposit_latest_log_cursor_fingerprint,
)
from ..latest_log_delta_result import _create_latest_log_delta_result
from .log_delta_contract import verify_automatic_deposit_runtime_log_delta


class AutomaticDepositRuntimeLogDeltaContractTests(unittest.TestCase):
    def setUp(self) -> None:
        self.cursor = AutomaticDepositLatestLogByteCursor(
            path=str(Path("C:/minecraft/instance/logs/latest.log")),
            device=1,
            inode=2,
            size=100,
            mtime_ns=200,
            prefix_sha256="a" * 64,
        )

    def test_accepts_complete_delta_bound_to_exact_cursor(self):
        errors = verify_automatic_deposit_runtime_log_delta(
            self.cursor,
            self._delta("complete line\n"),
        )

        self.assertEqual((), errors)

    def test_rejects_cursor_offset_and_partial_record_mismatch(self):
        wrong_cursor = self._delta("complete line\n", cursor_fingerprint="b" * 64)
        wrong_offset = self._delta("complete line\n", start_offset=101)
        partial = self._delta("partial line")

        self.assertEqual(
            ("LATEST_LOG_DELTA_CURSOR_FINGERPRINT_MISMATCH",),
            verify_automatic_deposit_runtime_log_delta(self.cursor, wrong_cursor),
        )
        self.assertEqual(
            ("LATEST_LOG_DELTA_START_OFFSET_MISMATCH",),
            verify_automatic_deposit_runtime_log_delta(self.cursor, wrong_offset),
        )
        self.assertEqual(
            ("LATEST_LOG_DELTA_PARTIAL_RECORD",),
            verify_automatic_deposit_runtime_log_delta(self.cursor, partial),
        )

    def test_rejects_untyped_cursor_or_delta(self):
        self.assertEqual(
            ("LATEST_LOG_CURSOR_NOT_TYPED",),
            verify_automatic_deposit_runtime_log_delta({}, self._delta("line\n")),
        )
        self.assertEqual(
            ("LATEST_LOG_DELTA_NOT_TYPED",),
            verify_automatic_deposit_runtime_log_delta(self.cursor, {}),
        )

    def _delta(
        self,
        text: str,
        *,
        cursor_fingerprint: str | None = None,
        start_offset: int | None = None,
    ):
        start = self.cursor.size if start_offset is None else start_offset
        return _create_latest_log_delta_result(
            True,
            "LATEST_LOG_DELTA_READ",
            None,
            start,
            start + len(text.encode("utf-8")),
            text,
            "utf-8",
            cursor_fingerprint
            or automatic_deposit_latest_log_cursor_fingerprint(self.cursor),
        )


if __name__ == "__main__":
    unittest.main()
