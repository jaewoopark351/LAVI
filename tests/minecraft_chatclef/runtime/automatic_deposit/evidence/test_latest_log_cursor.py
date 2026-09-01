#20260831_kpopmodder: Lock latest.log byte-cursor identity handling.
from __future__ import annotations

import hashlib
import unittest
from dataclasses import replace
from types import SimpleNamespace

from ..oracle.matrix_verdict import AutomaticDepositVerdict
from .latest_log_byte_cursor import AutomaticDepositLatestLogByteCursor
from .latest_log_cursor_reader import (
    capture_automatic_deposit_latest_log_cursor,
    read_automatic_deposit_latest_log_delta,
)


class AutomaticDepositLatestLogCursorTests(unittest.TestCase):
    def test_complete_append_is_read_from_exact_byte_offset(self):
        initial = b"before\n"
        appended = b"[LAVI ChatClefDiag] event=terminal\n"
        capture = capture_automatic_deposit_latest_log_cursor(
            "C:/instance/logs/latest.log",
            stat_reader=_stable_stat(len(initial), inode=7, mtime_ns=10),
            bytes_reader=lambda _path: initial,
        )

        delta = read_automatic_deposit_latest_log_delta(
            capture.cursor,
            stat_reader=_stable_stat(
                len(initial + appended),
                inode=7,
                mtime_ns=11,
            ),
            bytes_reader=lambda _path: initial + appended,
        )

        self.assertTrue(delta.ok)
        self.assertEqual(appended.decode("utf-8"), delta.text)

    def test_collected_delta_cannot_be_replaced(self):
        initial = b"before\n"
        cursor = _cursor(initial, inode=7)
        delta = read_automatic_deposit_latest_log_delta(
            cursor,
            stat_reader=_stable_stat(len(initial), inode=7, mtime_ns=10),
            bytes_reader=lambda _path: initial,
        )

        with self.assertRaisesRegex(ValueError, "integrity mismatch"):
            replace(delta, text="fabricated\n")

    def test_inode_change_is_inconclusive_rotation_or_replacement(self):
        initial = b"before\n"
        cursor = _cursor(initial, inode=7)

        delta = read_automatic_deposit_latest_log_delta(
            cursor,
            stat_reader=_stable_stat(len(initial), inode=8, mtime_ns=11),
            bytes_reader=lambda _path: initial,
        )

        self.assertFalse(delta.ok)
        self.assertIs(AutomaticDepositVerdict.INCONCLUSIVE, delta.verdict)
        self.assertEqual("LATEST_LOG_ROTATED_OR_REPLACED", delta.reason)

    def test_truncation_is_inconclusive(self):
        initial = b"before\n"
        cursor = _cursor(initial, inode=7)

        delta = read_automatic_deposit_latest_log_delta(
            cursor,
            stat_reader=_stable_stat(2, inode=7, mtime_ns=11),
            bytes_reader=lambda _path: b"x\n",
        )

        self.assertFalse(delta.ok)
        self.assertEqual("LATEST_LOG_TRUNCATED", delta.reason)

    def test_same_size_changed_prefix_is_inconclusive_replacement(self):
        initial = b"before\n"
        replacement = b"changed\n"
        cursor = _cursor(initial, inode=7)

        delta = read_automatic_deposit_latest_log_delta(
            cursor,
            stat_reader=_stable_stat(len(replacement), inode=7, mtime_ns=11),
            bytes_reader=lambda _path: replacement,
        )

        self.assertFalse(delta.ok)
        self.assertEqual("LATEST_LOG_REPLACED", delta.reason)

    def test_partial_appended_record_is_inconclusive(self):
        initial = b"before\n"
        partial = b"event=ter"
        cursor = _cursor(initial, inode=7)

        delta = read_automatic_deposit_latest_log_delta(
            cursor,
            stat_reader=_stable_stat(
                len(initial + partial),
                inode=7,
                mtime_ns=11,
            ),
            bytes_reader=lambda _path: initial + partial,
        )

        self.assertFalse(delta.ok)
        self.assertEqual("LATEST_LOG_DELTA_HAS_PARTIAL_RECORD", delta.reason)


def _stable_stat(size: int, *, inode: int, mtime_ns: int):
    def read_stat(_path):
        return SimpleNamespace(
            st_dev=1,
            st_ino=inode,
            st_size=size,
            st_mtime_ns=mtime_ns,
        )

    return read_stat


def _cursor(raw: bytes, *, inode: int):
    return AutomaticDepositLatestLogByteCursor(
        path="C:/instance/logs/latest.log",
        device=1,
        inode=inode,
        size=len(raw),
        mtime_ns=10,
        prefix_sha256=hashlib.sha256(raw).hexdigest(),
    )


if __name__ == "__main__":
    unittest.main()
