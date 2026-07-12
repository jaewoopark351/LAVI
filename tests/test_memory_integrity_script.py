import json
import os
import tempfile
import unittest

from memory_core.raw_event_sqlite_store import RawEventSQLiteStore
from scripts.check_memory_integrity import build_report


#20260626_kpopmodder: Verify memory integrity checks without touching real memory files.
class MemoryIntegrityScriptTests(unittest.TestCase):
    def test_report_detects_missing_raw_sqlite_index_rows(self):#20260626_kpopmodder
        with tempfile.TemporaryDirectory() as temp_dir:
            jsonl_path = os.path.join(temp_dir, "raw_events.jsonl")
            db_path = os.path.join(temp_dir, "raw_events.sqlite3")
            derived_path = os.path.join(temp_dir, "derived_memory.sqlite3")
            first = self._event("first indexed raw event", 1.0)
            second = self._event("second missing raw event", 2.0)
            first_line = json.dumps(first, ensure_ascii=False)
            second_line = json.dumps(second, ensure_ascii=False)

            with open(jsonl_path, "w", encoding="utf-8") as f:
                f.write(first_line)
                f.write("\n")
                f.write(second_line)
                f.write("\n")

            RawEventSQLiteStore(db_path).add_event(
                first,
                line_text=first_line,
            )

            report = build_report(
                raw_events_jsonl=jsonl_path,
                raw_events_db=db_path,
                derived_db=derived_path,
            )

            self.assertEqual("WARN", report["status"])
            self.assertEqual(2, report["raw_events_jsonl_valid_event_count"])
            self.assertEqual(1, report["raw_events_sqlite_count"])
            self.assertEqual(1, report["raw_sqlite_missing_count"])
            self.assertIn(
                "raw_sqlite_missing_jsonl_events",
                report["warnings"],
            )

    def test_report_syncs_raw_sqlite_index_when_requested(self):#20260626_kpopmodder
        with tempfile.TemporaryDirectory() as temp_dir:
            jsonl_path = os.path.join(temp_dir, "raw_events.jsonl")
            db_path = os.path.join(temp_dir, "raw_events.sqlite3")
            derived_path = os.path.join(temp_dir, "derived_memory.sqlite3")
            event = self._event("sync me into sqlite", 1.0)

            with open(jsonl_path, "w", encoding="utf-8") as f:
                f.write(json.dumps(event, ensure_ascii=False))
                f.write("\n")

            report = build_report(
                raw_events_jsonl=jsonl_path,
                raw_events_db=db_path,
                derived_db=derived_path,
                sync=True,
            )

            self.assertEqual("OK", report["status"])
            self.assertEqual(1, report["raw_events_sqlite_count"])
            self.assertEqual(0, report["raw_sqlite_missing_count"])

    def test_raw_sqlite_initialize_accepts_filename_only_path(self):#20260627_kpopmodder
        with tempfile.TemporaryDirectory() as temp_dir:
            previous_cwd = os.getcwd()
            try:
                os.chdir(temp_dir)
                store = RawEventSQLiteStore("raw_events.sqlite3")

                store.initialize()

                self.assertTrue(os.path.exists("raw_events.sqlite3"))
            finally:
                os.chdir(previous_cwd)

    def _event(self, value, created_ts):
        return {
            "event_type": "user_message",
            "value": value,
            "source": "test",
            "metadata": {},
            "created_at": "2026-06-26 10:00:00",
            "created_ts": created_ts,
        }


if __name__ == "__main__":
    unittest.main()
