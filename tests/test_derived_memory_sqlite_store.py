import os
import tempfile
import unittest

from memory_core.derived_memory_sqlite_store import DerivedMemorySQLiteStore


#20260626_kpopmodder: Cover derived-memory SQLite upsert/search behavior without heavy dependencies.
class DerivedMemorySQLiteStoreTests(unittest.TestCase):
    def test_upsert_merges_exact_duplicate_key(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            store = DerivedMemorySQLiteStore(
                os.path.join(temp_dir, "derived_memory.sqlite3")
            )
            item = self._memory_item(
                search_text="YouTube Ghost Hunter video is playing",
                normalized_key="youtubeghosthuntervideoisplaying",
            )

            first = store.upsert_memory(item)
            second = store.upsert_memory(item)
            rows = store.get_recent(limit=10)

            self.assertEqual("inserted", first["action"])
            self.assertEqual("updated", second["action"])
            self.assertEqual(1, len(rows))
            self.assertEqual(2, rows[0]["source_event_count"])
            self.assertEqual(1, rows[0]["duplicate_count"])

    def test_upsert_counts_incoming_source_events_as_duplicates(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            store = DerivedMemorySQLiteStore(
                os.path.join(temp_dir, "derived_memory.sqlite3")
            )
            first_item = self._memory_item(
                search_text="ScreenVision saw a Python editor",
                normalized_key="screenvisionsawapythoneditor",
            )
            merged_item = self._memory_item(
                search_text="ScreenVision saw a Python editor again",
                normalized_key="screenvisionsawapythoneditor",
            )
            merged_item["source_event_count"] = 3
            merged_item["duplicate_count"] = 2
            merged_item["last_created_ts"] = 2.0

            store.upsert_memory(first_item)
            update = store.upsert_memory(merged_item)
            rows = store.get_recent(limit=10)

            self.assertEqual("updated", update["action"])
            self.assertEqual(1, len(rows))
            self.assertEqual(4, rows[0]["source_event_count"])
            self.assertEqual(3, rows[0]["duplicate_count"])
            self.assertEqual(4, update["source_event_count"])
            self.assertEqual(3, update["duplicate_count"])

    def test_search_and_clear(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            store = DerivedMemorySQLiteStore(
                os.path.join(temp_dir, "derived_memory.sqlite3")
            )
            store.upsert_memory(
                self._memory_item(
                    search_text="The project uses raw_events.jsonl as append only log",
                    normalized_key="projectusesraweventsjsonlasappendonlylog",
                    topic_key="memory",
                )
            )

            results = store.search("append only raw events", limit=4)
            self.assertEqual(1, len(results))
            self.assertIn("raw_events", results[0]["search_text"])

            store.clear()
            self.assertEqual([], store.get_recent(limit=4))

    def test_search_prefilters_before_candidate_limit(self):#20260627_kpopmodder
        with tempfile.TemporaryDirectory() as temp_dir:
            store = DerivedMemorySQLiteStore(
                os.path.join(temp_dir, "derived_memory.sqlite3")
            )
            old_match = self._memory_item(
                search_text="The forgotten nebula clue is blue comet",
                normalized_key="theforgottennebulaclueisbluecomet",
                topic_key="memory",
            )
            old_match["first_created_ts"] = 1.0
            old_match["last_created_ts"] = 1.0
            store.upsert_memory(old_match)

            for index in range(160):
                filler = self._memory_item(
                    search_text=f"recent unrelated filler note {index}",
                    normalized_key=f"recentunrelatedfillernote{index}",
                    topic_key="filler",
                )
                filler["first_created_ts"] = float(index + 10)
                filler["last_created_ts"] = float(index + 10)
                store.upsert_memory(filler)

            results = store.search("forgotten nebula clue", limit=1)

            self.assertEqual(1, len(results))
            self.assertIn("blue comet", results[0]["search_text"])

    def test_stats_report_row_count_and_stale_state(self):#20260627_kpopmodder
        with tempfile.TemporaryDirectory() as temp_dir:
            store = DerivedMemorySQLiteStore(
                os.path.join(temp_dir, "derived_memory.sqlite3")
            )

            empty_stats = store.get_stats(raw_latest_created_ts=10.0)
            self.assertEqual(0, empty_stats["row_count"])
            self.assertTrue(empty_stats["stale"])

            store.upsert_memory(
                self._memory_item(
                    search_text="ScreenVision saw YouTube",
                    normalized_key="screenvisionsawyoutube",
                    topic_key="youtube",
                )
            )
            fresh_stats = store.get_stats(raw_latest_created_ts=1.0)
            stale_stats = store.get_stats(raw_latest_created_ts=2.0)

            self.assertEqual(1, fresh_stats["row_count"])
            self.assertFalse(fresh_stats["stale"])
            self.assertTrue(stale_stats["stale"])

    def _memory_item(
        self,
        search_text,
        normalized_key,
        topic_key="youtube",
    ):
        return {
            "kind": "screen_observation",
            "title": search_text,
            "summary": search_text,
            "search_text": search_text,
            "normalized_key": normalized_key,
            "topic_key": topic_key,
            "source_event_count": 1,
            "duplicate_count": 0,
            "first_created_at": "2026-06-26 10:00:00",
            "last_created_at": "2026-06-26 10:00:00",
            "first_created_ts": 1.0,
            "last_created_ts": 1.0,
            "confidence": 0.75,
            "metadata": {},
            "created_ts": 1.0,
            "updated_ts": 1.0,
        }


if __name__ == "__main__":
    unittest.main()
