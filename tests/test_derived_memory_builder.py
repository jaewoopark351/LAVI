import json
import os
import tempfile
import unittest

from memory_core.derived_memory_builder import DerivedMemoryBuilder
from memory_core.derived_memory_sqlite_store import DerivedMemorySQLiteStore
from memory_core.memory_retriever import MemoryRetriever
from memory_core.memory_store import MemoryStore


#20260626_kpopmodder: Derived memory must stay raw-adjacent while preserving raw-event fallback.
class DerivedMemoryBuilderTests(unittest.TestCase):
    def test_same_screen_observation_is_preserved_with_suffix_keys(self):
        builder = DerivedMemoryBuilder()
        events = [
            self._event(
                "screen_observation",
                "YouTube Ghost Hunter video is playing",
                1.0,
                source="ScreenVision",
            ),
            self._event(
                "screen_observation",
                "YouTube Ghost Hunter video is playing",
                2.0,
                source="ScreenVision",
            ),
            self._event(
                "screen_observation",
                "YouTube Ghost Hunter video is playing",
                3.0,
                source="ScreenVision",
            ),
        ]

        items, stats = builder.build_items(events)

        self.assertEqual(3, len(items))
        self.assertEqual(0, stats["merged_duplicate_count"])
        self.assertEqual(2, stats["preserved_duplicate_row_count"])
        self.assertEqual(
            len({item["normalized_key"] for item in items}),
            len(items),
        )
        self.assertTrue(items[1]["normalized_key"].endswith("__2"))
        self.assertTrue(items[2]["normalized_key"].endswith("__3"))
        self.assertTrue(all(item["source_event_count"] == 3 for item in items))
        self.assertTrue(all(item["duplicate_count"] == 2 for item in items))
        self.assertTrue(all(
            item["metadata"]["base_normalized_key_count"] == 3
            for item in items
        ))

    def test_similar_screen_observation_is_preserved_separately(self):
        builder = DerivedMemoryBuilder()
        events = [
            self._event(
                "screen_observation",
                "YouTube Ghost Hunter video is playing",
                1.0,
                source="ScreenVision",
            ),
            self._event(
                "screen_observation",
                "YouTube Ghost Hunter video is playing now",
                2.0,
                source="ScreenVision",
            ),
        ]

        items, stats = builder.build_items(events)

        self.assertEqual(2, len(items))
        self.assertEqual(0, stats["merged_duplicate_count"])
        self.assertEqual(0, stats["preserved_duplicate_row_count"])

    def test_different_conversations_do_not_merge(self):
        builder = DerivedMemoryBuilder()
        events = [
            self._event("user_message", "The favorite color is blue", 1.0),
            self._event("assistant_message", "I will remember blue", 2.0),
            self._event("user_message", "The CUDA GPU is device one", 3.0),
            self._event("assistant_message", "I will keep CUDA on device one", 4.0),
        ]

        items, _stats = builder.build_items(events)
        conversation_items = [
            item for item in items if item["kind"] == "conversation"
        ]

        self.assertEqual(2, len(conversation_items))

    def test_memory_command_source_is_excluded(self):
        builder = DerivedMemoryBuilder()
        events = [
            self._event(
                "user_message",
                "remember this temporary command",
                1.0,
                source="memory_command",
            ),
            self._event(
                "assistant_message",
                "saved memory",
                2.0,
                source="memory_command",
            ),
        ]

        items, stats = builder.build_items(events)

        self.assertEqual([], items)
        self.assertEqual(1, stats["skipped_noise_count"])

    def test_retriever_falls_back_to_raw_when_derived_store_fails(self):
        class BrokenDerivedStore:
            def search(self, query, limit=4):
                raise RuntimeError("derived unavailable")

            def get_recent(self, limit=4):
                raise RuntimeError("derived unavailable")

        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            self._write_events(store.raw_events_path, [
                self._event(
                    "user_message",
                    "The project nickname is LAV",
                    1.0,
                ),
                self._event(
                    "assistant_message",
                    "I will remember that the project nickname is LAV",
                    2.0,
                ),
            ])
            retriever = MemoryRetriever(
                store,
                max_results=3,
                derived_store=BrokenDerivedStore(),
            )

            results = retriever.retrieve("What was the project nickname?")

            self.assertTrue(results)
            self.assertIn("LAV", results[0]["text"])

    def test_retriever_does_not_let_single_derived_row_replace_raw(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            self._write_events(store.raw_events_path, [
                self._event(
                    "user_message",
                    "The project nickname is LAV",
                    1.0,
                ),
                self._event(
                    "assistant_message",
                    "I will remember that the project nickname is LAV",
                    2.0,
                ),
            ])
            derived_store = DerivedMemorySQLiteStore(
                os.path.join(temp_dir, "derived_memory.sqlite3")
            )
            derived_store.upsert_memory({
                "kind": "conversation",
                "title": "The project nickname is WRONG",
                "summary": "The project nickname is WRONG",
                "search_text": "The project nickname is WRONG",
                "normalized_key": "theprojectnicknameiswrong",
                "topic_key": "project",
                "source_event_count": 1,
                "duplicate_count": 0,
                "first_created_at": "2026-06-26 10:00:03",
                "last_created_at": "2026-06-26 10:00:03",
                "first_created_ts": 3.0,
                "last_created_ts": 3.0,
                "confidence": 0.8,
                "metadata": {},
                "created_ts": 3.0,
                "updated_ts": 3.0,
            })
            retriever = MemoryRetriever(
                store,
                max_results=3,
                derived_store=derived_store,
            )

            results = retriever.retrieve("What was the project nickname?")
            recalled_text = "\n".join(item["text"] for item in results)

            self.assertIn("LAV", recalled_text)
            self.assertNotIn("WRONG", recalled_text)

    def test_retriever_does_not_let_repeated_conversation_replace_raw(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            self._write_events(store.raw_events_path, [
                self._event(
                    "user_message",
                    "The project nickname is LAV",
                    1.0,
                ),
                self._event(
                    "assistant_message",
                    "I will remember that the project nickname is LAV",
                    2.0,
                ),
            ])
            derived_store = DerivedMemorySQLiteStore(
                os.path.join(temp_dir, "derived_memory.sqlite3")
            )
            derived_store.upsert_memory({
                "kind": "conversation",
                "title": "The project nickname is WRONG",
                "summary": "The project nickname is WRONG",
                "search_text": "The project nickname is WRONG",
                "normalized_key": "theprojectnicknameiswrong",
                "topic_key": "project",
                "source_event_count": 2,
                "duplicate_count": 1,
                "first_created_at": "2026-06-26 10:00:03",
                "last_created_at": "2026-06-26 10:00:04",
                "first_created_ts": 3.0,
                "last_created_ts": 4.0,
                "confidence": 0.8,
                "metadata": {},
                "created_ts": 3.0,
                "updated_ts": 4.0,
            })
            retriever = MemoryRetriever(
                store,
                max_results=3,
                derived_store=derived_store,
            )

            results = retriever.retrieve("What was the project nickname?")
            recalled_text = "\n".join(item["text"] for item in results)

            self.assertIn("LAV", recalled_text)
            self.assertNotIn("WRONG", recalled_text)

    def test_rebuild_does_not_modify_raw_events_jsonl(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            events = [
                self._event(
                    "screen_observation",
                    "YouTube Ghost Hunter video is playing",
                    1.0,
                    source="ScreenVision",
                ),
            ]
            self._write_events(store.raw_events_path, events)
            before = self._read_bytes(store.raw_events_path)

            derived_store = DerivedMemorySQLiteStore(
                os.path.join(temp_dir, "derived_memory.sqlite3")
            )
            builder = DerivedMemoryBuilder()
            stats = builder.rebuild(
                store.get_raw_events(limit=10),
                derived_store=derived_store,
                clear=True,
            )
            after = self._read_bytes(store.raw_events_path)

            self.assertEqual(before, after)
            self.assertEqual(1, stats["inserted_count"])
            self.assertEqual(1, len(derived_store.get_recent(limit=10)))

    def test_rebuild_preserves_same_screen_observations_in_sqlite(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            events = [
                self._event(
                    "screen_observation",
                    "YouTube Ghost Hunter video is playing",
                    1.0,
                    source="ScreenVision",
                ),
                self._event(
                    "screen_observation",
                    "YouTube Ghost Hunter video is playing",
                    2.0,
                    source="ScreenVision",
                ),
                self._event(
                    "screen_observation",
                    "YouTube Ghost Hunter video is playing",
                    3.0,
                    source="ScreenVision",
                ),
            ]
            self._write_events(store.raw_events_path, events)

            derived_store = DerivedMemorySQLiteStore(
                os.path.join(temp_dir, "derived_memory.sqlite3")
            )
            builder = DerivedMemoryBuilder()
            stats = builder.rebuild(
                store.get_raw_events(limit=10),
                derived_store=derived_store,
                clear=True,
            )
            rows = derived_store.get_recent(limit=10)

            self.assertEqual(3, stats["inserted_count"])
            self.assertEqual(0, stats["merged_duplicate_count"])
            self.assertEqual(2, stats["preserved_duplicate_row_count"])
            self.assertEqual(3, len(rows))
            self.assertEqual(
                len({row["normalized_key"] for row in rows}),
                len(rows),
            )
            self.assertTrue(all(row["source_event_count"] == 3 for row in rows))
            self.assertTrue(all(row["duplicate_count"] == 2 for row in rows))
            self.assertTrue(all(
                row["metadata"]["base_normalized_key_count"] == 3
                for row in rows
            ))

    def test_rebuilt_screen_observation_is_recalled_with_explicit_single_fallback(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            raw_events = [
                self._event(
                    "screen_observation",
                    "YouTube Ghost Hunter video is playing",
                    1.0,
                    source="ScreenVision",
                ),
                self._event(
                    "screen_observation",
                    "YouTube Ghost Hunter video is playing",
                    2.0,
                    source="ScreenVision",
                ),
            ]
            derived_store = DerivedMemorySQLiteStore(
                os.path.join(temp_dir, "derived_memory.sqlite3")
            )
            builder = DerivedMemoryBuilder()
            builder.rebuild(
                raw_events,
                derived_store=derived_store,
                clear=True,
            )
            empty_store = MemoryStore(memory_dir=os.path.join(temp_dir, "empty"))
            retriever = MemoryRetriever(
                empty_store,
                max_results=3,
                derived_store=derived_store,
                use_derived_fallback=True,
                allow_single_screen_observation_fallback=True,
            )

            results = retriever.retrieve("Ghost Hunter video")

            self.assertTrue(results)
            self.assertEqual("derived_memory", results[0]["recall_mode"])
            self.assertIn("Ghost Hunter", results[0]["text"])
            self.assertEqual(2, results[0]["source_event_count"])
            self.assertEqual(1, results[0]["duplicate_count"])

    def test_rebuilt_repeated_screen_observation_recalled_by_default_fallback(self):#20260627_kpopmodder
        for fallback_mode in (True, "prefer"):
            with self.subTest(fallback_mode=fallback_mode):
                with tempfile.TemporaryDirectory() as temp_dir:
                    raw_events = [
                        self._event(
                            "screen_observation",
                            "YouTube Ghost Hunter video is playing",
                            1.0,
                            source="ScreenVision",
                        ),
                        self._event(
                            "screen_observation",
                            "YouTube Ghost Hunter video is playing",
                            2.0,
                            source="ScreenVision",
                        ),
                    ]
                    derived_store = DerivedMemorySQLiteStore(
                        os.path.join(temp_dir, "derived_memory.sqlite3")
                    )
                    builder = DerivedMemoryBuilder()
                    builder.rebuild(
                        raw_events,
                        derived_store=derived_store,
                        clear=True,
                    )
                    empty_store = MemoryStore(
                        memory_dir=os.path.join(temp_dir, "empty")
                    )
                    retriever = MemoryRetriever(
                        empty_store,
                        max_results=3,
                        derived_store=derived_store,
                        use_derived_fallback=fallback_mode,
                        allow_single_screen_observation_fallback=False,
                    )

                    results = retriever.retrieve("Ghost Hunter video")

                    self.assertTrue(results)
                    self.assertEqual(
                        "derived_memory",
                        results[0]["recall_mode"],
                    )
                    self.assertIn("Ghost Hunter", results[0]["text"])
                    self.assertEqual(2, results[0]["source_event_count"])
                    self.assertEqual(1, results[0]["duplicate_count"])

    def test_retriever_skips_single_screen_derived_fallback_by_default(self):#20260627_kpopmodder
        with tempfile.TemporaryDirectory() as temp_dir:
            empty_store = MemoryStore(memory_dir=os.path.join(temp_dir, "empty"))
            derived_store = DerivedMemorySQLiteStore(
                os.path.join(temp_dir, "derived_memory.sqlite3")
            )
            derived_store.upsert_memory(self._screen_memory_item(
                "Neuro-sama sings Shooting Stars on YouTube",
                "neurosamasingShootingStars",
                source_event_count=1,
                duplicate_count=0,
            ))
            retriever = MemoryRetriever(
                empty_store,
                max_results=3,
                derived_store=derived_store,
                use_derived_fallback=True,
            )

            results = retriever.retrieve("Neuro Shooting Stars")

            self.assertEqual([], results)

    def test_retriever_allows_repeated_screen_derived_fallback(self):#20260627_kpopmodder
        cases = (
            ("source_count", 2, 0),
            ("duplicate_count", 1, 1),
        )
        for case_name, source_event_count, duplicate_count in cases:
            with self.subTest(case_name=case_name):
                with tempfile.TemporaryDirectory() as temp_dir:
                    empty_store = MemoryStore(
                        memory_dir=os.path.join(temp_dir, "empty")
                    )
                    derived_store = DerivedMemorySQLiteStore(
                        os.path.join(temp_dir, "derived_memory.sqlite3")
                    )
                    derived_store.upsert_memory(self._screen_memory_item(
                        "Neuro-sama sings Shooting Stars on YouTube",
                        f"neurosamasingShootingStars{case_name}",
                        source_event_count=source_event_count,
                        duplicate_count=duplicate_count,
                    ))
                    retriever = MemoryRetriever(
                        empty_store,
                        max_results=3,
                        derived_store=derived_store,
                        use_derived_fallback=True,
                    )

                    results = retriever.retrieve("YouTube video Neuro Shooting Stars")

                    self.assertTrue(results)
                    self.assertEqual("derived_memory", results[0]["recall_mode"])
                    self.assertIn("Shooting Stars", results[0]["text"])

    def test_retriever_uses_single_screen_derived_fallback_when_explicitly_allowed(self):#20260627_kpopmodder
        with tempfile.TemporaryDirectory() as temp_dir:
            empty_store = MemoryStore(memory_dir=os.path.join(temp_dir, "empty"))
            derived_store = DerivedMemorySQLiteStore(
                os.path.join(temp_dir, "derived_memory.sqlite3")
            )
            derived_store.upsert_memory(self._screen_memory_item(
                "Neuro-sama sings Shooting Stars on YouTube",
                "neurosamasingShootingStars",
                source_event_count=1,
                duplicate_count=0,
            ))
            retriever = MemoryRetriever(
                empty_store,
                max_results=3,
                derived_store=derived_store,
                use_derived_fallback=True,
                allow_single_screen_observation_fallback=True,
            )

            with self.assertLogs("LAV", level="INFO") as captured:
                results = retriever.retrieve("YouTube video Neuro Shooting Stars")

            self.assertTrue(results)
            self.assertEqual("derived_memory", results[0]["recall_mode"])
            self.assertEqual(1, results[0]["source_event_count"])
            self.assertEqual(0, results[0]["duplicate_count"])
            self.assertIn("Shooting Stars", results[0]["text"])
            logs = "\n".join(captured.output)
            self.assertIn("source_event_count=1", logs)
            self.assertIn("duplicate_count=0", logs)

    def test_retriever_prefers_derived_screen_row_when_prefer_enabled(self):#20260627_kpopmodder
        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=os.path.join(temp_dir, "raw"))
            self._write_events(store.raw_events_path, [
                self._event(
                    "screen_observation",
                    "Ghost Hunter raw screen observation",
                    1.0,
                    source="ScreenVision",
                ),
            ])
            derived_store = DerivedMemorySQLiteStore(
                os.path.join(temp_dir, "derived_memory.sqlite3")
            )
            derived_store.upsert_memory({
                "kind": "screen_observation",
                "title": "Ghost Hunter derived screen observation",
                "summary": "Ghost Hunter derived screen observation",
                "search_text": "Ghost Hunter derived screen observation",
                "normalized_key": "ghosthunterderivedscreenobservation",
                "topic_key": "game",
                "source_event_count": 2,
                "duplicate_count": 1,
                "first_created_at": "2026-06-27 00:00:00",
                "last_created_at": "2026-06-27 00:00:00",
                "first_created_ts": 2.0,
                "last_created_ts": 2.0,
                "confidence": 0.8,
                "metadata": {},
                "created_ts": 2.0,
                "updated_ts": 2.0,
            })
            retriever = MemoryRetriever(
                store,
                max_results=3,
                derived_store=derived_store,
                use_derived_fallback="prefer",
            )

            results = retriever.retrieve("screen Ghost Hunter")

            self.assertTrue(results)
            self.assertEqual("derived_memory", results[0]["recall_mode"])
            self.assertIn("derived", results[0]["text"])

    def _screen_memory_item(
        self,
        text,
        normalized_key,
        source_event_count=1,
        duplicate_count=0,
    ):
        return {
            "kind": "screen_observation",
            "title": text,
            "summary": text,
            "search_text": text,
            "normalized_key": normalized_key,
            "topic_key": "neuro",
            "source_event_count": source_event_count,
            "duplicate_count": duplicate_count,
            "first_created_at": "2026-06-27 00:00:00",
            "last_created_at": "2026-06-27 00:00:00",
            "first_created_ts": 1.0,
            "last_created_ts": 1.0,
            "confidence": 0.8,
            "metadata": {},
            "created_ts": 1.0,
            "updated_ts": 1.0,
        }

    def _event(
        self,
        event_type,
        value,
        created_ts,
        source="test",
    ):
        return {
            "event_type": event_type,
            "value": value,
            "source": source,
            "metadata": {},
            "created_at": f"2026-06-26 10:00:0{int(created_ts)}",
            "created_ts": created_ts,
        }

    def _write_events(self, path, events):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            for event in events:
                f.write(json.dumps(event, ensure_ascii=False))
                f.write("\n")

    def _read_bytes(self, path):
        with open(path, "rb") as f:
            return f.read()


if __name__ == "__main__":
    unittest.main()
