#20260629_kpopmodder: Verify memory SQLite writers are serialized across processes.
import multiprocessing
import os
import queue
import tempfile
import unittest

import pytest

from memory_core.sqlite_write_gate import (
    SQLiteWriteLockTimeout,
    connect_sqlite,
    sqlite_writer_lock,
    sqlite_writer_lock_path,
)


def _try_lock_writer(db_path, result_queue):
    try:
        with sqlite_writer_lock(
            db_path,
            timeout_sec=0.2,
            poll_interval_sec=0.01,
        ):
            result_queue.put("acquired")
    except SQLiteWriteLockTimeout:
        result_queue.put("timeout")
    except Exception as exc:
        result_queue.put(f"error:{type(exc).__name__}")


class SQLiteWriteGateTests(unittest.TestCase):
    def test_writer_lock_path_is_shared_by_memory_sqlite_files(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            raw_db = os.path.join(temp_dir, "raw_events.sqlite3")
            derived_db = os.path.join(temp_dir, "derived_memory.sqlite3")

            self.assertEqual(
                sqlite_writer_lock_path(raw_db),
                sqlite_writer_lock_path(derived_db),
            )

    def test_connect_sets_busy_timeout(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            db_path = os.path.join(temp_dir, "raw_events.sqlite3")
            connection = connect_sqlite(
                db_path,
                timeout_sec=0.25,
                busy_timeout_ms=250,
            )
            try:
                row = connection.execute("PRAGMA busy_timeout").fetchone()
                self.assertGreaterEqual(int(row[0]), 250)
            finally:
                connection.close()

    @pytest.mark.integration
    def test_writer_lock_times_out_while_other_process_holds_gate(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            db_path = os.path.join(temp_dir, "raw_events.sqlite3")
            context = multiprocessing.get_context("spawn")
            result_queue = context.Queue()
            process = context.Process(
                target=_try_lock_writer,
                args=(db_path, result_queue),
            )

            with sqlite_writer_lock(db_path, timeout_sec=1.0):
                process.start()
                try:
                    result = result_queue.get(timeout=3.0)
                except queue.Empty:
                    result = "missing"

            process.join(timeout=3.0)
            if process.is_alive():
                process.terminate()
                process.join(timeout=3.0)

            self.assertEqual("timeout", result)
            self.assertEqual(0, process.exitcode)


if __name__ == "__main__":
    unittest.main()
