import ast
import json
import os
import tempfile
import unittest
from pathlib import Path

from memory_core.memory_consolidator import MemoryConsolidator
from memory_core.memory_context_builder import MemoryContextBuilder
from memory_core.memory_retriever import MemoryRetriever
from memory_core.memory_store import MemoryStore


class MemoryRetrievalTests(unittest.TestCase):
    def test_consolidates_user_and_assistant_events_into_episode(self):
        consolidator = MemoryConsolidator()
        episodes = consolidator.consolidate([
            {
                "event_type": "user_message",
                "value": "내 고양이 이름은 나비야.",
                "created_at": "2026-06-01 10:00:00",
                "created_ts": 1.0,
            },
            {
                "event_type": "assistant_message",
                "value": "나비라는 이름이 예쁘네요.",
                "created_at": "2026-06-01 10:00:01",
                "created_ts": 2.0,
            },
        ])

        self.assertEqual(1, len(episodes))
        self.assertEqual("conversation", episodes[0]["kind"])
        self.assertIn("내 고양이 이름은 나비야.", episodes[0]["text"])
        self.assertIn("나비라는 이름이 예쁘네요.", episodes[0]["text"])

    def test_retrieves_related_old_conversation_and_ignores_current_query(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            self._write_events(store.raw_events_path, [
                self._event(
                    "user_message",
                    "내 고양이 이름은 나비야.",
                    "2026-06-01 10:00:00",
                    1.0,
                ),
                self._event(
                    "assistant_message",
                    "나비라는 이름을 기억할게.",
                    "2026-06-01 10:00:01",
                    2.0,
                ),
                self._event(
                    "user_message",
                    "오늘 날씨가 덥다.",
                    "2026-06-02 10:00:00",
                    3.0,
                ),
                self._event(
                    "assistant_message",
                    "물을 자주 마셔.",
                    "2026-06-02 10:00:01",
                    4.0,
                ),
                self._event(
                    "user_message",
                    "예전에 말한 내 고양이 이름이 뭐였지?",
                    "2026-06-03 10:00:00",
                    5.0,
                ),
            ])

            retriever = MemoryRetriever(
                store,
                max_results=3,
            )
            results = retriever.retrieve(
                "예전에 말한 내 고양이 이름이 뭐였지?"
            )

            self.assertTrue(results)
            self.assertIn("나비", results[0]["text"])
            self.assertNotIn(
                "뭐였지?",
                "\n".join(item["text"] for item in results),
            )
            self.assertNotIn(
                "오늘 날씨",
                "\n".join(item["text"] for item in results),
            )

    def test_retriever_excludes_current_user_input_even_with_router_query(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            self._write_events(store.raw_events_path, [
                self._event(
                    "user_message",
                    "Neuro가 부른 노래 제목은 Memories Are You",
                    "2026-06-01 10:00:00",
                    1.0,
                ),
                self._event(
                    "user_message",
                    "Neuro 기억함?",
                    "2026-06-01 10:01:00",
                    2.0,
                ),
                self._event(
                    "assistant_message",
                    "Neuro가 Bohemian Rhapsody를 불렀던 기억이 있습니다.",
                    "2026-06-01 10:01:01",
                    3.0,
                ),
                self._event(
                    "user_message",
                    "Neuro 기억함?",
                    "2026-06-01 10:02:00",
                    4.0,
                ),
            ])

            retriever = MemoryRetriever(store, max_results=3)

            results = retriever.retrieve(
                "neuro",
                exclude_texts=["Neuro 기억함?"],
            )

            self.assertTrue(results)
            joined = "\n".join(item["text"] for item in results)
            self.assertIn("Memories Are You", joined)
            self.assertNotIn("Bohemian Rhapsody", joined)
            self.assertNotIn("Neuro 기억함?", joined)

    def test_retriever_excludes_assistant_answers_to_memory_questions(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            self._write_events(store.raw_events_path, [
                self._event(
                    "user_message",
                    "Neuro song title is Memories Are You",
                    "2026-06-01 10:00:00",
                    1.0,
                ),
                self._event(
                    "assistant_message",
                    "I will remember that Neuro song title is Memories Are You.",
                    "2026-06-01 10:00:01",
                    2.0,
                ),
                self._event(
                    "user_message",
                    "Do you remember Neuro song?",
                    "2026-06-01 10:01:00",
                    3.0,
                ),
                self._event(
                    "assistant_message",
                    "Neuro sang Bohemian Rhapsody.",
                    "2026-06-01 10:01:01",
                    4.0,
                ),
            ])

            retriever = MemoryRetriever(store, max_results=3)

            results = retriever.retrieve("Neuro song")

            self.assertTrue(results)
            joined = "\n".join(item["text"] for item in results)
            self.assertIn("Memories Are You", joined)
            self.assertNotIn("Bohemian Rhapsody", joined)
            self.assertNotIn("Do you remember Neuro song?", joined)

    def test_retriever_keeps_user_stated_song_title_fact(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            self._write_events(store.raw_events_path, [
                self._event(
                    "user_message",
                    "Neuro\uac00 \ubd80\ub978 \ub178\ub798 \uc81c\ubaa9\uc740 Memories Are You",
                    "2026-06-01 10:00:00",
                    1.0,
                ),
                self._event(
                    "assistant_message",
                    "\uae30\uc5b5\ud560\uac8c\uc694.",
                    "2026-06-01 10:00:01",
                    2.0,
                ),
                self._event(
                    "user_message",
                    "Neuro \uae30\uc5b5\ud568?",
                    "2026-06-01 10:01:00",
                    3.0,
                ),
                self._event(
                    "assistant_message",
                    "Neuro\uac00 Bohemian Rhapsody\ub97c \ubd88\ub800\ub358 \uae30\uc5b5\uc774 \uc788\uc2b5\ub2c8\ub2e4.",
                    "2026-06-01 10:01:01",
                    4.0,
                ),
            ])

            retriever = MemoryRetriever(store, max_results=3)

            results = retriever.retrieve("Neuro \ub178\ub798")

            self.assertTrue(results)
            joined = "\n".join(item["text"] for item in results)
            self.assertIn("Memories Are You", joined)
            self.assertNotIn("Bohemian Rhapsody", joined)

    def test_context_builder_injects_only_query_relevant_recall(self):
        class FakeRetriever:
            def __init__(self):
                self.queries = []

            def retrieve(self, query):
                self.queries.append(query)
                return [{
                    "text": "사용자: 좋아하는 색은 파란색이야.\nAI: 기억할게.",
                    "created_at": "2026-06-01 10:00:00",
                }]

        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            retriever = FakeRetriever()
            builder = MemoryContextBuilder(
                store,
                memory_retriever=retriever,
            )

            context = builder.build_context_text(
                query="내가 좋아하는 색이 뭐였지?"
            )

            self.assertEqual(
                ["내가 좋아하는 색이 뭐였지?"],
                retriever.queries,
            )
            self.assertIn("[Relevant recalled memory]", context)
            self.assertIn("파란색", context)
            self.assertIn("[LAVI memory context]", context)
            self.assertIn("Use this memory only as supplemental context", context)
            self.assertNotIn("[AI ", context)

    def test_generic_recall_returns_recent_meaningful_conversations(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            self._write_events(store.raw_events_path, [
                self._event(
                    "user_message",
                    "고양이 나비가 오늘 창가에서 잤어.",
                    "2026-06-01 10:00:00",
                    1.0,
                ),
                self._event(
                    "assistant_message",
                    "햇볕이 따뜻했나 봐.",
                    "2026-06-01 10:00:01",
                    2.0,
                ),
                self._event(
                    "user_message",
                    "예전 기억해줘",
                    "2026-06-02 10:00:00",
                    3.0,
                ),
                self._event(
                    "assistant_message",
                    "장기기억에 저장했습니다: 예전",
                    "2026-06-02 10:00:01",
                    4.0,
                    source="memory_command",
                ),
                self._event(
                    "user_message",
                    "TTS 끊김 문제를 테스트했어.",
                    "2026-06-03 10:00:00",
                    5.0,
                ),
                self._event(
                    "assistant_message",
                    "큐 인터럽트 테스트가 통과했어.",
                    "2026-06-03 10:00:01",
                    6.0,
                ),
                self._event(
                    "screen_observation_silent",
                    "YouTube에서 GHOST HUNTER 영상을 보고 있습니다.",
                    "2026-06-03 10:30:00",
                    6.05,
                    source="ScreenVision",
                ),
                self._event(
                    "screen_observation_silent",
                    "Visual Studio Code에서 Python 코드를 작성하고 있습니다.",
                    "2026-06-03 10:40:00",
                    6.07,
                    source="ScreenVision",
                ),
                self._event(
                    "screen_observation",
                    "YouTube에서 GHOST HUNTER 영상과 한글 자막이 보입니다.",
                    "2026-06-03 10:30:10",
                    6.06,
                    source="ScreenVision",
                ),
                self._event(
                    "user_message",
                    "옛날일 기억나?",
                    "2026-06-03 11:00:00",
                    6.1,
                ),
                self._event(
                    "assistant_message",
                    "전에 TTS 테스트를 했던 일이 기억나.",
                    "2026-06-03 11:00:01",
                    6.2,
                ),
                self._event(
                    "user_message",
                    "옜날일 기억나?",
                    "2026-06-04 10:00:00",
                    7.0,
                ),
            ])

            retriever = MemoryRetriever(store, max_results=3)
            results = retriever.retrieve("옜날일 기억나?")
            recalled_text = "\n".join(item["text"] for item in results)

            self.assertEqual(2, len(results))
            self.assertNotIn("YouTube", recalled_text)
            self.assertIn("TTS 끊김", recalled_text)
            self.assertIn("고양이 나비", recalled_text)
            self.assertEqual(
                0,
                sum(
                    item["kind"] == "screen_observation"
                    for item in results
                ),
            )
            self.assertNotIn("장기기억에 저장했습니다", recalled_text)
            self.assertNotIn("전에 TTS 테스트를 했던 일이 기억나", recalled_text)
            self.assertTrue(all(
                item.get("recall_mode") == "recent_events"
                for item in results
            ))

    def test_generic_recall_honors_max_results_override(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            events = []
            for index in range(6):
                events.append(self._event(
                    "user_message",
                    f"archive note {index}",
                    f"2026-06-0{index + 1} 10:00:00",
                    float(index * 2 + 1),
                ))
                events.append(self._event(
                    "assistant_message",
                    f"assistant response {index}",
                    f"2026-06-0{index + 1} 10:00:01",
                    float(index * 2 + 2),
                ))
            self._write_events(store.raw_events_path, events)

            retriever = MemoryRetriever(store, max_results=2)
            results = retriever.retrieve(
                "remember anything from before",
                max_results_override=5,
            )
            recalled_text = "\n".join(item["text"] for item in results)

            self.assertEqual(5, len(results))
            self.assertTrue(all(
                item.get("recall_mode") == "recent_events"
                for item in results
            ))
            self.assertIn("archive note 5", recalled_text)
            self.assertNotIn("archive note 0", recalled_text)

    def test_generic_recall_context_tells_model_to_answer_from_recent_events(self):
        class GenericRecallRetriever:
            def retrieve(self, query):
                return [{
                    "text": "사용자: TTS 테스트를 했어.\nAI: 테스트가 통과했어.",
                    "created_at": "2026-06-03 10:00:00",
                    "recall_mode": "recent_events",
                }]

        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            builder = MemoryContextBuilder(
                store,
                memory_retriever=GenericRecallRetriever(),
            )

            context = builder.build_context_text(query="옛날일 기억나?")

            self.assertIn("[Recent recalled events]", context)
            self.assertIn("Use only facts present in the bullets", context)
            self.assertIn("do not infer unseen wins", context)
            self.assertIn("TTS 테스트", context)

    def test_deep_recall_context_promotes_concrete_song_titles(self):#20260627_kpopmodder
        class DeepRecallRetriever:
            def retrieve(self, query, exclude_texts=None):
                return [
                    {
                        "text": (
                            'AI: "\uc624\ub355\uc2a4"\ub294 Neuro\uac00 '
                            '\ubd80\ub978 \uace1\uc785\ub2c8\ub2e4.'
                        ),
                        "created_at": "2026-06-26 23:32:31",
                        "recall_mode": "raw_long_search",
                    },
                    {
                        "text": (
                            'AI: Neuro\uac00 \ubd80\ub978 \ub178\ub798 '
                            '\uc911 \ud558\ub098\ub294 '
                            '"Shooting Stars - Bag Raiders"\uc785\ub2c8\ub2e4.'
                        ),
                        "created_at": "2026-06-26 23:31:44",
                        "recall_mode": "raw_long_search",
                    },
                ]

        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            builder = MemoryContextBuilder(
                store,
                memory_retriever=DeepRecallRetriever(),
            )

            context = builder.build_context_text(
                query="Neuro all memories",
            )

            self.assertIn("Deep recall request:", context)
            self.assertIn(
                "Deep recall concrete titles/phrases found:",
                context,
            )
            self.assertIn("Shooting Stars - Bag Raiders", context)
            self.assertLess(
                context.index("Deep recall concrete titles/phrases found:"),
                context.index("- [2026-06-26 23:32:31]"),
            )

    def test_store_skips_malformed_raw_event_lines(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            with open(store.raw_events_path, "w", encoding="utf-8") as f:
                f.write("{broken json}\n")
                f.write(json.dumps(
                    self._event(
                        "user_message",
                        "정상 기록",
                        "2026-06-01 10:00:00",
                        1.0,
                    ),
                    ensure_ascii=False,
                ))
                f.write("\n")

            events = store.get_raw_events(limit=10)

            self.assertEqual(1, len(events))
            self.assertEqual("정상 기록", events[0]["value"])

    def test_store_reads_raw_events_from_sqlite_when_jsonl_is_missing(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)

            store.add_raw_event(
                "user_message",
                "SQLite raw event",
                source="test",
            )
            os.remove(store.raw_events_path)

            events = store.get_raw_events(limit=10)

            self.assertEqual(1, len(events))
            self.assertEqual("user_message", events[0]["event_type"])
            self.assertEqual("SQLite raw event", events[0]["value"])
            self.assertTrue(os.path.exists(store.raw_events_db_path))
            self.assertIsInstance(events[0].get("raw_event_id"), int)
            self.assertEqual(64, len(events[0].get("raw_line_hash", "")))

    #20260626_kpopmodder: Long recall needs limit=None to scan all source-of-truth raw events.
    def test_store_reads_all_raw_events_when_limit_is_none(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            self._write_events(store.raw_events_path, [
                self._event(
                    "assistant_message",
                    "first raw event",
                    "2026-06-01 10:00:00",
                    1.0,
                ),
                self._event(
                    "assistant_message",
                    "second raw event",
                    "2026-06-01 10:00:01",
                    2.0,
                ),
                self._event(
                    "assistant_message",
                    "third raw event",
                    "2026-06-01 10:00:02",
                    3.0,
                ),
            ])

            events = store.get_raw_events(limit=None)

            self.assertEqual(
                [
                    "first raw event",
                    "second raw event",
                    "third raw event",
                ],
                [event["value"] for event in events],
            )

    def test_store_iter_raw_events_filters_event_types(self):#20260703_kpopmodder
        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            self._write_events(store.raw_events_path, [
                self._event(
                    "starcraft116_game_event",
                    "ignored game event",
                    "2026-06-01 10:00:00",
                    1.0,
                ),
                self._event(
                    "user_message",
                    "kept user event",
                    "2026-06-01 10:00:01",
                    2.0,
                ),
            ])

            events = list(store.iter_raw_events(
                limit=None,
                event_types=("user_message",),
            ))

            self.assertEqual(["kept user event"], [
                event["value"] for event in events
            ])

    def test_retriever_long_raw_uses_iterator_budget_and_event_filter(self):#20260703_kpopmodder
        class IterStore:
            def __init__(self):
                self.calls = []

            def iter_raw_events(self, **kwargs):
                self.calls.append(kwargs)
                yield {
                    "event_type": "user_message",
                    "value": "The protected archive clue is silver fern",
                    "created_at": "2026-06-01 10:00:00",
                    "created_ts": 1.0,
                }
                yield {
                    "event_type": "assistant_message",
                    "value": "Stored silver fern.",
                    "created_at": "2026-06-01 10:00:01",
                    "created_ts": 2.0,
                }

        store = IterStore()
        retriever = MemoryRetriever(
            store,
            accuracy_first_raw_search=True,
            max_long_raw_events=123,
            raw_search_time_budget_sec=0.5,
            raw_search_batch_size=17,
            minimum_score=0.1,
        )

        results = retriever.retrieve("protected archive clue silver fern")

        self.assertTrue(results)
        self.assertEqual(1, len(store.calls))
        self.assertEqual(123, store.calls[0]["max_events"])
        self.assertEqual(0.5, store.calls[0]["time_budget_sec"])
        self.assertEqual(17, store.calls[0]["batch_size"])
        self.assertIn("user_message", store.calls[0]["event_types"])

    def test_retriever_searches_long_raw_when_recent_window_misses_memory(self):#20260626_kpopmodder
        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            events = [
                self._event(
                    "user_message",
                    "The secret launch codename is Nebula Orchid",
                    "2026-06-01 10:00:00",
                    1.0,
                ),
                self._event(
                    "assistant_message",
                    "I will remember that the launch codename is Nebula Orchid.",
                    "2026-06-01 10:00:01",
                    2.0,
                ),
            ]
            for index in range(3, 130):
                events.append(
                    self._event(
                        "user_message",
                        f"recent filler note {index}",
                        f"2026-06-01 10:00:{index:02d}",
                        float(index),
                    )
                )
            self._write_events(store.raw_events_path, events)
            retriever = MemoryRetriever(
                store,
                max_raw_events=100,
                max_results=3,
            )

            results = retriever.retrieve(
                "What was the secret launch codename?"
            )
            recalled_text = "\n".join(item["text"] for item in results)

            self.assertIn("Nebula Orchid", recalled_text)
            self.assertEqual("raw_long_search", results[0]["recall_mode"])

    def test_retriever_returns_recent_raw_match_without_full_scan(self):#20260626_kpopmodder
        class RecentMatchStore:
            def __init__(self, events):
                self.events = events
                self.calls = []

            def get_raw_events(self, limit=2000):
                self.calls.append(limit)
                if limit is None:
                    raise AssertionError("full raw scan should not run")
                return self.events

        events = [
            self._event(
                "user_message",
                "The archive clue is blue comet",
                "2026-06-01 10:00:00",
                1.0,
            ),
            self._event(
                "assistant_message",
                "I will remember that the archive clue is blue comet.",
                "2026-06-01 10:00:01",
                2.0,
            ),
        ]
        store = RecentMatchStore(events)
        retriever = MemoryRetriever(store, max_results=2)

        results = retriever.retrieve("What was the archive clue?")

        self.assertTrue(results)
        self.assertIn("blue comet", results[0]["text"])
        self.assertEqual([2000], store.calls)
        self.assertEqual("raw_recent", results[0]["recall_mode"])

    def test_accuracy_first_raw_search_scans_all_raw_before_recent_hit(self):#20260627_kpopmodder
        class AccuracyFirstStore:
            def __init__(self, all_events):
                self.all_events = all_events
                self.calls = []

            def get_raw_events(self, limit=2000):
                self.calls.append(limit)
                if limit is None:
                    return self.all_events
                raise AssertionError("recent window should not run first")

        all_events = [
            self._event(
                "user_message",
                "Neuro song memory: Shooting Stars - Bag Raiders",
                "2026-06-01 10:00:00",
                1.0,
            ),
            self._event(
                "assistant_message",
                "I will remember Neuro sang Shooting Stars by Bag Raiders.",
                "2026-06-01 10:00:01",
                2.0,
            ),
        ]
        store = AccuracyFirstStore(all_events)
        retriever = MemoryRetriever(
            store,
            max_results=2,
            accuracy_first_raw_search=True,
        )

        results = retriever.retrieve("Neuro song")
        recalled_text = "\n".join(item["text"] for item in results)

        self.assertTrue(results)
        self.assertEqual([None], store.calls)
        self.assertEqual("raw_long_search", results[0]["recall_mode"])
        self.assertIn("Shooting Stars - Bag Raiders", recalled_text)

    def test_retriever_skips_full_scan_when_full_recent_window_matches(self):#20260626_kpopmodder
        class RecentWindowStore:
            def __init__(self, events):
                self.events = events
                self.calls = []

            def get_raw_events(self, limit=2000):
                self.calls.append(limit)
                if limit is None:
                    raise AssertionError("full raw scan should not run")
                return self.events

        events = []
        for index in range(98):
            events.append(
                self._event(
                    "user_message",
                    f"recent filler note {index}",
                    f"2026-06-01 10:00:{index:02d}",
                    float(index),
                )
            )
        events.extend([
            self._event(
                "user_message",
                "The studio password hint is silver maple",
                "2026-06-01 10:02:00",
                200.0,
            ),
            self._event(
                "assistant_message",
                "I will remember the studio hint is silver maple.",
                "2026-06-01 10:02:01",
                201.0,
            ),
        ])
        store = RecentWindowStore(events)
        retriever = MemoryRetriever(
            store,
            max_raw_events=100,
            max_results=2,
        )

        results = retriever.retrieve("What was the studio password hint?")

        self.assertTrue(results)
        self.assertIn("silver maple", results[0]["text"])
        self.assertEqual([100], store.calls)
        self.assertEqual("raw_recent", results[0]["recall_mode"])

    def test_retriever_expands_full_scan_for_explicit_old_memory_request(self):#20260626_kpopmodder
        class ExpandableRawStore:
            def __init__(self, recent_events, all_events):
                self.recent_events = recent_events
                self.all_events = all_events
                self.calls = []

            def get_raw_events(self, limit=2000):
                self.calls.append(limit)
                if limit is None:
                    return self.all_events
                return self.recent_events

        old_events = [
            self._event(
                "user_message",
                "Starfall old concert memory was saved",
                "2026-06-01 10:00:00",
                1.0,
            ),
            self._event(
                "assistant_message",
                "I remember the old Starfall concert memory.",
                "2026-06-01 10:00:01",
                2.0,
            ),
        ]
        recent_events = [
            self._event(
                "user_message",
                "Starfall recent folder memory was saved",
                "2026-06-26 10:00:00",
                1000.0,
            ),
            self._event(
                "assistant_message",
                "I remember the recent Starfall folder memory.",
                "2026-06-26 10:00:01",
                1001.0,
            ),
        ]
        store = ExpandableRawStore(
            recent_events=recent_events,
            all_events=old_events + recent_events,
        )
        retriever = MemoryRetriever(
            store,
            max_raw_events=100,
            max_results=3,
        )

        results = retriever.retrieve("starfall old memory all")
        recalled_text = "\n".join(item["text"] for item in results)

        self.assertTrue(results)
        self.assertEqual([100, None], store.calls)
        self.assertEqual("raw_long_search", results[0]["recall_mode"])
        self.assertIn("old Starfall concert", recalled_text)
        self.assertIn("recent Starfall folder", recalled_text)

    def test_retriever_expands_full_scan_for_watched_youtube_request(self):#20260627_kpopmodder
        class ExpandableRawStore:
            def __init__(self, recent_events, all_events):
                self.recent_events = recent_events
                self.all_events = all_events
                self.calls = []

            def get_raw_events(self, limit=2000):
                self.calls.append(limit)
                if limit is None:
                    return self.all_events
                return self.recent_events

        old_events = [
            self._event(
                "screen_observation",
                "YouTube에서 OLD ARCHIVE CLIP 영상을 보고 있습니다.",
                "2026-06-01 10:00:00",
                1.0,
                source="ScreenVision",
            ),
        ]
        recent_events = [
            self._event(
                "screen_observation",
                "YouTube에서 RECENT STREAM CLIP 영상이 재생 중입니다.",
                "2026-06-26 10:00:00",
                1000.0,
                source="ScreenVision",
            ),
        ]
        store = ExpandableRawStore(
            recent_events=recent_events,
            all_events=old_events + recent_events,
        )
        retriever = MemoryRetriever(
            store,
            max_raw_events=100,
            max_results=3,
        )

        results = retriever.retrieve("Youtube 봤던 거 알려줘")
        recalled_text = "\n".join(item["text"] for item in results)

        self.assertTrue(results)
        self.assertEqual([100, None], store.calls)
        self.assertEqual("raw_long_search", results[0]["recall_mode"])
        self.assertIn("OLD ARCHIVE CLIP", recalled_text)
        self.assertIn("RECENT STREAM CLIP", recalled_text)

    def test_deep_recall_skips_prior_youtube_denial_answer(self):#20260629_kpopmodder
        class ExpandableRawStore:
            def __init__(self, recent_events, all_events):
                self.recent_events = recent_events
                self.all_events = all_events
                self.calls = []

            def get_raw_events(self, limit=2000):
                self.calls.append(limit)
                if limit is None:
                    return self.all_events
                return self.recent_events

        useful_screen_events = [
            self._event(
                "screen_observation",
                "YouTube에서 Cyber Angel: ZERO Exception 영상이 재생 중입니다.",
                "2026-06-25 22:11:20",
                1.0,
                source="ScreenVision",
            ),
        ]
        prior_denial_events = [
            self._event(
                "user_message",
                "Youtube 봤던 거 거억하는 거 전부 알려줘",
                "2026-06-27 22:03:35",
                1000.0,
            ),
            self._event(
                "assistant_message",
                (
                    "제가 기억하고 있는 정보에는 사용자가 본 YouTube "
                    "동영상에 대한 내용이 포함되어 있지 않습니다. "
                    "이전 대화나 활동에 대한 세부 사항을 제공할 수 없습니다."
                ),
                "2026-06-27 22:03:36",
                1001.0,
            ),
        ]
        store = ExpandableRawStore(
            recent_events=prior_denial_events,
            all_events=useful_screen_events + prior_denial_events,
        )
        retriever = MemoryRetriever(
            store,
            max_raw_events=100,
            max_results=3,
        )

        results = retriever.retrieve("Youtube 기억하는 거 전부 기억해줘")
        recalled_text = "\n".join(item["text"] for item in results)

        self.assertTrue(results)
        self.assertEqual([100, None], store.calls)
        self.assertEqual("raw_long_search", results[0]["recall_mode"])
        self.assertEqual("screen_observation", results[0]["kind"])
        self.assertIn("Cyber Angel: ZERO Exception", recalled_text)
        self.assertNotIn("포함되어 있지 않습니다", recalled_text)

    def test_deep_recall_skips_recent_deep_recall_echo(self):#20260626_kpopmodder
        class ExpandableRawStore:
            def __init__(self, recent_events, all_events):
                self.recent_events = recent_events
                self.all_events = all_events

            def get_raw_events(self, limit=2000):
                if limit is None:
                    return self.all_events
                return self.recent_events

        old_events = [
            self._event(
                "user_message",
                "Starfall first concert memory was saved",
                "2026-06-01 10:00:00",
                1.0,
            ),
            self._event(
                "assistant_message",
                "I remember the first Starfall concert memory.",
                "2026-06-01 10:00:01",
                2.0,
            ),
        ]
        recent_echo_events = [
            self._event(
                "user_message",
                "starfall old memory all",
                "2026-06-26 10:00:00",
                1000.0,
            ),
            self._event(
                "assistant_message",
                "I found the recent Starfall summary again.",
                "2026-06-26 10:00:01",
                1001.0,
            ),
        ]
        store = ExpandableRawStore(
            recent_events=recent_echo_events,
            all_events=old_events + recent_echo_events,
        )
        retriever = MemoryRetriever(
            store,
            max_raw_events=100,
            max_results=3,
        )

        results = retriever.retrieve("starfall old memory all")
        recalled_text = "\n".join(item["text"] for item in results)

        self.assertTrue(results)
        self.assertEqual("raw_long_search", results[0]["recall_mode"])
        self.assertIn("first Starfall concert", recalled_text)
        self.assertNotIn("recent Starfall summary again", recalled_text)

    def test_deep_recall_prefers_explicit_focus_token(self):#20260626_kpopmodder
        class ExpandableRawStore:
            def __init__(self, recent_events, all_events):
                self.recent_events = recent_events
                self.all_events = all_events

            def get_raw_events(self, limit=2000):
                if limit is None:
                    return self.all_events
                return self.recent_events

        faust_recall_events = [
            self._event(
                "user_message",
                "Faust old memory all",
                "2026-06-26 18:49:49",
                1000.0,
            ),
            self._event(
                "assistant_message",
                "I found an old Faust memory summary.",
                "2026-06-26 18:49:50",
                1001.0,
            ),
        ]
        neuro_events = [
            self._event(
                "user_message",
                "Neuro-sama was singing Shooting Stars by Bag Raiders.",
                "2026-06-01 10:00:00",
                1.0,
            ),
            self._event(
                "assistant_message",
                "I remember Neuro-sama singing Shooting Stars.",
                "2026-06-01 10:00:01",
                2.0,
            ),
        ]
        store = ExpandableRawStore(
            recent_events=faust_recall_events,
            all_events=neuro_events + faust_recall_events,
        )
        retriever = MemoryRetriever(
            store,
            max_raw_events=100,
            max_results=3,
        )

        results = retriever.retrieve("Neuro old memory all")
        recalled_text = "\n".join(item["text"] for item in results)

        self.assertTrue(results)
        self.assertEqual("raw_long_search", results[0]["recall_mode"])
        self.assertIn("Neuro-sama", recalled_text)
        self.assertNotIn("Faust", recalled_text)

    def test_topic_recall_filters_sidebar_only_screen_hits_and_returns_all_hits(self):#20260627_kpopmodder
        class ExpandableRawStore:
            def __init__(self, recent_events, all_events):
                self.recent_events = recent_events
                self.all_events = all_events

            def get_raw_events(self, limit=2000):
                if limit is None:
                    return self.all_events
                return self.recent_events

        sidebar_noise = (
            "YouTube Shorts page shows a search for "
            "\ud55c\uad6d \ucd95\uad6c\uacc4\uc758 \uc758\ubb38\uc810. "
            + ("soccer clip details " * 50)
            + "\ud654\uba74 \uc624\ub978\ucabd \uad6c\ub3c5 \ubaa9\ub85d\uc5d0 "
            "\ubd09\uad343rd channel text is visible."
        )
        honkai_screen_events = [
            self._event(
                "screen_observation",
                (
                    "YouTube\uc5d0\uc11c \ubd09\uad343rd Cyber Angel: "
                    "ZERO Exception Ver. \uc601\uc0c1\uc774 \uc7ac\uc0dd "
                    "\uc911\uc785\ub2c8\ub2e4."
                ),
                "2026-06-25 22:11:20",
                1.0,
            ),
            self._event(
                "screen_observation",
                (
                    "YouTube\uc5d0\uc11c \ubd09\uad34 3rd starfall "
                    "\uc601\uc0c1\uc774 \uc7ac\uc0dd \uc911\uc785\ub2c8\ub2e4."
                ),
                "2026-06-25 22:12:20",
                2.0,
            ),
        ]
        store = ExpandableRawStore(
            recent_events=[],
            all_events=honkai_screen_events + [
                self._event(
                    "screen_observation",
                    sidebar_noise,
                    "2026-06-27 18:16:33",
                    1000.0,
                ),
            ],
        )
        retriever = MemoryRetriever(
            store,
            max_raw_events=100,
            max_results=1,
            accuracy_first_raw_search=True,
        )

        results = retriever.retrieve(
            "\uc720\ud29c\ube0c\uc5d0\uc11c \ubd24\ub358 \ubd95\uad343rd \uc601\uc0c1 \uae30\uc5b5\ub098\ub294 \uac70 \uc54c\ub824\uc918",
            max_results_override=10,
        )
        recalled_text = "\n".join(item["text"] for item in results)

        self.assertEqual(2, len(results))
        self.assertTrue(all(
            item["recall_mode"] == "raw_long_search"
            for item in results
        ))
        self.assertIn("Cyber Angel", recalled_text)
        self.assertIn("starfall", recalled_text)
        self.assertNotIn("\ud55c\uad6d \ucd95\uad6c", recalled_text)

    def test_deep_recall_filters_ai_answer_summaries_and_failure_answers(self):#20260626_kpopmodder
        class ExpandableRawStore:
            def __init__(self, recent_events, all_events):
                self.recent_events = recent_events
                self.all_events = all_events
                self.calls = []

            def get_raw_events(self, limit=2000):
                self.calls.append(limit)
                if limit is None:
                    return self.all_events
                return self.recent_events

        useful_events = [
            self._event(
                "user_message",
                "Neuro song memory: Memories Are You",
                "2026-06-01 10:00:00",
                1.0,
            ),
            self._event(
                "assistant_message",
                "I will remember Neuro sang Memories Are You.",
                "2026-06-01 10:00:01",
                2.0,
            ),
        ]
        polluted_recent_events = [
            self._event(
                "user_message",
                "No specific memory is visible. What information do you need?",
                "2026-06-26 10:00:00",
                1000.0,
            ),
            self._event(
                "assistant_message",
                "Please tell me what to search for.",
                "2026-06-26 10:00:01",
                1001.0,
            ),
            self._event(
                "screen_observation",
                (
                    "Gradio Chatbot shows ChatGPT_OpenAI answered that "
                    "Neuro sang Fake Song."
                ),
                "2026-06-26 10:01:00",
                1002.0,
            ),
        ]
        store = ExpandableRawStore(
            recent_events=polluted_recent_events,
            all_events=useful_events + polluted_recent_events,
        )
        retriever = MemoryRetriever(
            store,
            max_raw_events=100,
            max_results=5,
        )

        results = retriever.retrieve("Neuro song all memories")
        recalled_text = "\n".join(item["text"] for item in results)

        self.assertTrue(results)
        self.assertEqual([100, None], store.calls)
        self.assertEqual("raw_long_search", results[0]["recall_mode"])
        self.assertIn("Memories Are You", recalled_text)
        self.assertNotIn("Fake Song", recalled_text)
        self.assertNotIn("No specific memory", recalled_text)

    def test_builder_excludes_screenvision_for_regular_topic_recall(self):#20260720_kpopmodder
        class EmptyShortTermSelector:
            def select(self, query=None, active_history=None):
                return []

        class RawStore:
            def __init__(self, events):
                self.events = list(events)

            def get_raw_events(self, limit=2000):
                if limit is None:
                    return list(self.events)
                return self.events[-int(limit):]

            def get_working_memory(self):
                return []

            def get_session_memory(self):
                return {}

            def get_long_term_memory(self):
                return {}

        events = [
            self._event(
                "screen_observation",
                (
                    "ScreenVision StarCraft replay scoreboard shows "
                    "Fake Screen Build."
                ),
                "2026-06-01 10:00:00",
                1.0,
                source="ScreenVision",
            ),
            self._event(
                "user_message",
                "StarCraft Monster bot connected through BWAPI client.",
                "2026-06-01 10:00:01",
                2.0,
            ),
            self._event(
                "assistant_message",
                "Monster is running with Client Connection.",
                "2026-06-01 10:00:02",
                3.0,
            ),
        ]
        store = RawStore(events)
        retriever = MemoryRetriever(
            store,
            max_raw_events=100,
            max_results=3,
            accuracy_first_raw_search=True,
            minimum_score=0.1,
        )
        builder = MemoryContextBuilder(
            store,
            memory_retriever=retriever,
            short_term_memory_selector=EmptyShortTermSelector(),
        )

        context = builder.build_context_text(query="StarCraft remember?")

        self.assertIn("Monster bot connected", context)
        self.assertNotIn("Fake Screen Build", context)

    def test_builder_allows_screenvision_for_video_recall(self):#20260720_kpopmodder
        class EmptyShortTermSelector:
            def select(self, query=None, active_history=None):
                return []

        class RawStore:
            def __init__(self, events):
                self.events = list(events)

            def get_raw_events(self, limit=2000):
                if limit is None:
                    return list(self.events)
                return self.events[-int(limit):]

            def get_working_memory(self):
                return []

            def get_session_memory(self):
                return {}

            def get_long_term_memory(self):
                return {}

        events = [
            self._event(
                "screen_observation",
                (
                    "ScreenVision YouTube page shows Cyber Angel ZERO "
                    "Exception video playback."
                ),
                "2026-06-01 10:00:00",
                1.0,
                source="ScreenVision",
            ),
        ]
        store = RawStore(events)
        retriever = MemoryRetriever(
            store,
            max_raw_events=100,
            max_results=3,
            accuracy_first_raw_search=True,
            minimum_score=0.1,
        )
        builder = MemoryContextBuilder(
            store,
            memory_retriever=retriever,
            short_term_memory_selector=EmptyShortTermSelector(),
        )

        context = builder.build_context_text(
            query="Youtube watched video remember?",
        )

        self.assertIn("Cyber Angel ZERO Exception", context)

    def test_retriever_does_not_call_derived_store_when_fallback_disabled(self):#20260626_kpopmodder
        class EmptyRawStore:
            def get_raw_events(self, limit=2000):
                return []

        class TrackingDerivedStore:
            def __init__(self):
                self.search_called = False
                self.get_recent_called = False

            def search(self, query, limit=4):
                self.search_called = True
                raise AssertionError("derived search should not run")

            def get_recent(self, limit=4):
                self.get_recent_called = True
                raise AssertionError("derived recent should not run")

        derived_store = TrackingDerivedStore()
        retriever = MemoryRetriever(
            EmptyRawStore(),
            derived_store=derived_store,
            use_derived_fallback=False,
        )

        results = retriever.retrieve("What was the missing memory?")

        self.assertEqual([], results)
        self.assertFalse(derived_store.search_called)
        self.assertFalse(derived_store.get_recent_called)

    def test_retriever_logs_raw_event_lookup_failure_fail_closed(self):#20260627_kpopmodder
        class BrokenRawStore:
            def get_raw_events(self, limit=2000):
                raise RuntimeError("raw sqlite unavailable")

        retriever = MemoryRetriever(BrokenRawStore(), max_results=2)

        with self.assertLogs("LAV.memory_core", level="WARNING") as captured:
            results = retriever.retrieve("What was the missing clue?")

        self.assertEqual([], results)
        logs = "\n".join(captured.output)
        self.assertIn("[MemoryRetrieverRawEventsLookupFailed]", logs)
        self.assertIn("error_type=RuntimeError", logs)

    def test_retriever_logs_derived_fallback_failure_fail_closed(self):#20260627_kpopmodder
        class EmptyRawStore:
            def get_raw_events(self, limit=2000):
                return []

        class BrokenDerivedStore:
            def search(self, query, limit=4):
                raise RuntimeError("derived sqlite unavailable")

            def get_recent(self, limit=4):
                raise RuntimeError("derived sqlite unavailable")

        retriever = MemoryRetriever(
            EmptyRawStore(),
            derived_store=BrokenDerivedStore(),
            use_derived_fallback=True,
            max_results=2,
        )

        with self.assertLogs("LAV.memory_core", level="WARNING") as captured:
            results = retriever.retrieve("What was the missing clue?")

        self.assertEqual([], results)
        logs = "\n".join(captured.output)
        self.assertIn("[MemoryRetrieverDerivedMemoryFallbackFailed]", logs)
        self.assertIn("error_type=RuntimeError", logs)

    def test_refresh_derived_memory_rebuilds_stale_index(self):#20260720_kpopmodder
        from app_core.memory_bootstrap import refresh_derived_memory_if_stale
        from memory_core.derived_memory_sqlite_store import (
            DerivedMemorySQLiteStore,
        )

        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            self._write_events(store.raw_events_path, [
                self._event(
                    "user_message",
                    "LAVI project memory anchor",
                    "2026-07-20 01:00:00",
                    1.0,
                ),
                self._event(
                    "assistant_message",
                    "The project memory anchor was saved.",
                    "2026-07-20 01:00:01",
                    2.0,
                ),
                self._event(
                    "screen_observation_decision",
                    "Non-indexed decision event should not keep derived stale.",
                    "2026-07-20 01:00:02",
                    3.0,
                ),
            ])
            store.initialize_raw_event_sqlite()
            derived_store = DerivedMemorySQLiteStore(
                os.path.join(temp_dir, "derived_memory.sqlite3")
            )
            derived_store.initialize()

            refreshed = refresh_derived_memory_if_stale(
                derived_store,
                store,
                {"stale": True, "row_count": 0},
                auto_rebuild=True,
            )

            self.assertGreater(refreshed["row_count"], 0)
            self.assertFalse(refreshed["stale"])

    def test_memory_bootstrap_wires_derived_fallback_disabled_by_default(self):#20260627_kpopmodder
        bootstrap_path = (
            Path(__file__).resolve().parents[1]
            / "app_core"
            / "memory_bootstrap.py"
        )
        bootstrap_text = bootstrap_path.read_text(encoding="utf-8")
        module = ast.parse(bootstrap_text)
        config_text = (
            Path(__file__).resolve().parents[1] / "config.ini.example"
        ).read_text(encoding="utf-8")

        def find_assignment(name):
            for node in ast.walk(module):
                if not isinstance(node, ast.Assign):
                    continue
                for target in node.targets:
                    if getattr(target, "id", "") == name:
                        return node.value
            return None

        def constant_values(node):
            if not isinstance(node, ast.Call):
                return []
            return [
                arg.value
                for arg in node.args
                if isinstance(arg, ast.Constant)
            ]

        memory_retriever_calls = [
            node
            for node in ast.walk(module)
            if isinstance(node, ast.Call)
            and getattr(node.func, "id", "") == "MemoryRetriever"
        ]
        memory_context_builder_calls = [
            node
            for node in ast.walk(module)
            if isinstance(node, ast.Call)
            and getattr(node.func, "id", "") == "MemoryContextBuilder"
        ]

        self.assertTrue(memory_retriever_calls)
        self.assertTrue(memory_context_builder_calls)
        use_derived_fallback_values = [
            keyword.value.value
            for call in memory_retriever_calls
            for keyword in call.keywords
            if keyword.arg == "use_derived_fallback"
            and isinstance(keyword.value, ast.Constant)
        ]

        self.assertIn(False, use_derived_fallback_values)
        allow_single_values = [
            getattr(keyword.value, "id", "")
            for call in memory_retriever_calls
            for keyword in call.keywords
            if keyword.arg == "allow_single_screen_observation_fallback"
        ]
        self.assertIn(
            "memory_allow_single_screen_observation_fallback",
            allow_single_values,
        )
        allow_single_assignment = find_assignment(
            "memory_allow_single_screen_observation_fallback"
        )
        self.assertIn("allow_single_screen_observation_fallback", constant_values(
            allow_single_assignment,
        ))
        self.assertIn("false", constant_values(allow_single_assignment))
        self.assertIn(
            "allow_single_screen_observation_fallback = false",
            config_text,
        )
        accuracy_values = [
            getattr(keyword.value, "id", "")
            for call in memory_retriever_calls
            for keyword in call.keywords
            if keyword.arg == "accuracy_first_raw_search"
        ]
        self.assertIn("memory_accuracy_first_raw_search", accuracy_values)
        accuracy_assignment = find_assignment(
            "memory_accuracy_first_raw_search"
        )
        self.assertIn("accuracy_first_raw_search", constant_values(
            accuracy_assignment,
        ))
        self.assertIn("true", constant_values(accuracy_assignment))
        self.assertIn("accuracy_first_raw_search = true", config_text)

        retriever_max_values = [
            getattr(keyword.value, "id", "")
            for call in memory_retriever_calls
            for keyword in call.keywords
            if keyword.arg == "max_results"
        ]
        self.assertIn("memory_retriever_max_results", retriever_max_values)
        retriever_max_assignment = find_assignment("memory_retriever_max_results")
        self.assertIn("max_results", constant_values(retriever_max_assignment))
        self.assertIn("memory_retriever_max_results", constant_values(
            retriever_max_assignment,
        ))
        self.assertIn(12, constant_values(retriever_max_assignment))
        self.assertIn("max_results = 12", config_text)

        deep_recall_limits = [
            getattr(keyword.value, "id", "")
            for call in memory_context_builder_calls
            for keyword in call.keywords
            if keyword.arg == "max_deep_recalled_items"
        ]
        self.assertIn("memory_max_deep_recalled_items", deep_recall_limits)
        deep_recall_assignment = find_assignment("memory_max_deep_recalled_items")
        self.assertIn("max_deep_recalled_items", constant_values(
            deep_recall_assignment,
        ))
        self.assertIn("memory_context_max_deep_recalled_items", constant_values(
            deep_recall_assignment,
        ))
        self.assertIn(12, constant_values(deep_recall_assignment))
        self.assertIn("max_deep_recalled_items = 12", config_text)

        prefer_values = [
            getattr(keyword.value, "id", "")
            for call in memory_context_builder_calls
            for keyword in call.keywords
            if keyword.arg == "prefer_derived_first"
        ]
        self.assertIn("memory_prefer_derived_first", prefer_values)
        prefer_assignment = find_assignment("memory_prefer_derived_first")
        self.assertIn("prefer_derived_first", constant_values(prefer_assignment))
        self.assertIn("true", constant_values(prefer_assignment))
        self.assertIn("prefer_derived_first = true", config_text)
        auto_rebuild_assignment = find_assignment(
            "memory_auto_rebuild_derived_when_stale"
        )
        self.assertIn("auto_rebuild_derived_when_stale", constant_values(
            auto_rebuild_assignment,
        ))
        self.assertIn("true", constant_values(auto_rebuild_assignment))
        self.assertIn("auto_rebuild_derived_when_stale = true", config_text)
        self.assertIn('derived_memory_stats.get("stale")', bootstrap_text)
        self.assertIn("refresh_derived_memory_if_stale", bootstrap_text)
        self.assertIn("prefer_derived_first is disabled", bootstrap_text)

    def test_memory_bootstrap_router_provider_defaults_to_rule_and_openai_is_opt_in(self):#20260627_kpopmodder
        project_root = Path(__file__).resolve().parents[1]
        bootstrap_path = project_root / "app_core" / "memory_bootstrap.py"
        module = ast.parse(bootstrap_path.read_text(encoding="utf-8"))
        config_text = (project_root / "config.ini.example").read_text(
            encoding="utf-8",
        )

        def find_assignment(name):
            for node in ast.walk(module):
                if not isinstance(node, ast.Assign):
                    continue
                for target in node.targets:
                    if getattr(target, "id", "") == name:
                        return node.value
            return None

        def constant_values(node):
            if not isinstance(node, ast.Call):
                return []
            return [
                arg.value
                for arg in node.args
                if isinstance(arg, ast.Constant)
            ]

        provider_assignment = find_assignment("memory_router_provider")
        self.assertIn("provider", constant_values(provider_assignment))
        self.assertIn("memory_router_provider", constant_values(provider_assignment))
        self.assertIn("rule", constant_values(provider_assignment))

        openai_default = find_assignment("openai_memory_router_provider")
        self.assertIsInstance(openai_default, ast.Constant)
        self.assertIsNone(openai_default.value)

        memory_router_calls = [
            node
            for node in ast.walk(module)
            if isinstance(node, ast.Call)
            and getattr(node.func, "id", "") == "MemoryRouter"
        ]
        self.assertTrue(memory_router_calls)
        provider_keywords = [
            getattr(keyword.value, "id", "")
            for call in memory_router_calls
            for keyword in call.keywords
            if keyword.arg == "provider"
        ]
        callback_keywords = [
            getattr(keyword.value, "id", "")
            for call in memory_router_calls
            for keyword in call.keywords
            if keyword.arg == "ai_response_callback"
        ]
        self.assertIn("memory_router_provider", provider_keywords)
        self.assertIn("openai_memory_router_provider", callback_keywords)
        self.assertIn("provider = rule", config_text)
        self.assertIn("provider = openai", config_text)
        self.assertIn("part of the user input may be sent", config_text)

    def test_runtime_lifecycle_update_globals_periodic_guards_shutdown_timer(self):#20260627_kpopmodder
        lifecycle_path = (
            Path(__file__).resolve().parents[1]
            / "app_core"
            / "runtime_lifecycle.py"
        )
        module = ast.parse(lifecycle_path.read_text(encoding="utf-8"))

        function = None
        for node in ast.walk(module):
            if isinstance(node, ast.FunctionDef) and node.name == "update_globals_periodic":
                function = node
                break

        self.assertIsNotNone(function)

        def is_shutdown_guard(statement):
            test = getattr(statement, "test", None)
            return (
                isinstance(statement, ast.If)
                and isinstance(test, ast.Attribute)
                and getattr(test.value, "id", "") == "self"
                and test.attr == "app_shutdown_done"
            )

        def creates_timer(statement):
            for child in ast.walk(statement):
                if not isinstance(child, ast.Call):
                    continue
                func = child.func
                if (
                    isinstance(func, ast.Attribute)
                    and func.attr == "timer_factory"
                    and getattr(func.value, "id", "") == "self"
                ):
                    return True
            return False

        timer_index = next(
            index
            for index, statement in enumerate(function.body)
            if creates_timer(statement)
        )
        guard_indexes = [
            index
            for index, statement in enumerate(function.body)
            if is_shutdown_guard(statement)
        ]

        self.assertGreaterEqual(len(guard_indexes), 2)
        self.assertTrue(all(index < timer_index for index in guard_indexes))

    def test_retriever_logs_recall_evidence_without_returning_trace_fields(self):#20260626_kpopmodder
        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            store.add_raw_event(
                "user_message",
                "The archive password clue is blue comet",
                source="test",
            )
            store.add_raw_event(
                "assistant_message",
                "I will remember the archive clue is blue comet.",
                source="test",
            )
            retriever = MemoryRetriever(store, max_results=2)

            with self.assertLogs("LAV.memory_core", level="INFO") as captured:
                results = retriever.retrieve("What was the archive clue?")

            self.assertTrue(results)
            self.assertIn("blue comet", results[0]["text"])
            logs = "\n".join(captured.output)
            self.assertIn("[MemoryRecall]", logs)
            self.assertIn("raw_event_ids=[", logs)
            self.assertIn("[MemoryRecallScore]", logs)
            self.assertTrue(all(
                record.name == "LAV.memory_core"
                for record in captured.records
            ))
            self.assertNotIn("raw_event_ids", results[0])
            self.assertNotIn("raw_line_hashes", results[0])
            self.assertNotIn("_score_breakdown", results[0])

    def test_store_falls_back_to_jsonl_when_sqlite_fails(self):
        class BrokenSQLiteStore:
            def import_jsonl(self, jsonl_path):
                raise RuntimeError("sqlite unavailable")

            def get_recent_events(self, limit):
                raise RuntimeError("sqlite unavailable")

            def get_all_events(self):
                raise RuntimeError("sqlite unavailable")

        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            store.raw_event_sqlite_store = BrokenSQLiteStore()
            self._write_events(store.raw_events_path, [
                self._event(
                    "assistant_message",
                    "JSONL fallback",
                    "2026-06-01 10:00:00",
                    1.0,
                ),
            ])

            events = store.get_raw_events(limit=10)

            self.assertEqual(1, len(events))
            self.assertEqual("JSONL fallback", events[0]["value"])

    def test_store_imports_jsonl_to_sqlite_only_once_for_regular_reads(self):#20260626_kpopmodder
        class CountingSQLiteStore:
            def __init__(self):
                self.initialize_count = 0
                self.import_count = 0
                self.recent_count = 0

            def initialize(self):
                self.initialize_count += 1

            def import_jsonl(self, jsonl_path):
                self.import_count += 1

            def get_recent_events(self, limit):
                self.recent_count += 1
                return []

            def get_all_events(self):
                return []

        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            sqlite_store = CountingSQLiteStore()
            store.raw_event_sqlite_store = sqlite_store

            store.get_raw_events(limit=10)
            store.get_raw_events(limit=10)

            self.assertEqual(1, sqlite_store.import_count)
            self.assertEqual(2, sqlite_store.recent_count)

    def test_store_reads_all_jsonl_events_when_sqlite_fails_and_limit_is_none(self):#20260626_kpopmodder
        class BrokenSQLiteStore:
            def import_jsonl(self, jsonl_path):
                raise RuntimeError("sqlite unavailable")

            def get_recent_events(self, limit):
                raise RuntimeError("sqlite unavailable")

            def get_all_events(self):
                raise RuntimeError("sqlite unavailable")

        with tempfile.TemporaryDirectory() as temp_dir:
            store = MemoryStore(memory_dir=temp_dir)
            store.raw_event_sqlite_store = BrokenSQLiteStore()
            self._write_events(store.raw_events_path, [
                self._event(
                    "assistant_message",
                    "first JSONL fallback",
                    "2026-06-01 10:00:00",
                    1.0,
                ),
                self._event(
                    "assistant_message",
                    "second JSONL fallback",
                    "2026-06-01 10:00:01",
                    2.0,
                ),
            ])

            events = store.get_raw_events(limit=None)

            self.assertEqual(
                ["first JSONL fallback", "second JSONL fallback"],
                [event["value"] for event in events],
            )

    def _write_events(self, path, events):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            for event in events:
                f.write(json.dumps(event, ensure_ascii=False))
                f.write("\n")

    def _event(
        self,
        event_type,
        value,
        created_at,
        created_ts,
        source="test",
    ):
        return {
            "event_type": event_type,
            "value": value,
            "source": source,
            "metadata": {},
            "created_at": created_at,
            "created_ts": created_ts,
        }


if __name__ == "__main__":
    unittest.main()
