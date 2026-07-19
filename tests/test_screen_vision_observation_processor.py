import unittest

from plugins.ScreenVision.screen_vision_core.observation_processor import (
    ScreenObservationProcessor,
)
from plugins.ScreenVision.screen_vision_core.observation_policy import (
    ObservationPolicy,
)
from plugins.ScreenVision.screen_vision_core.observation_dispatch import (
    ScreenObservationDispatchHelper,
)
from plugins.ScreenVision.screen_vision_core.observation_decision_reporter import (
    ScreenObservationDecisionReporter,
)
from plugins.ScreenVision.screen_vision_core.observation_prompt_builder import (
    ScreenObservationPromptBuilder,
)
from plugins.ScreenVision.screen_vision_core.observation_text_cleaner import (
    ScreenObservationTextCleaner,
)
from plugins.ScreenVision.screen_vision_core.observation_result_flow import (
    ScreenObservationResultFlow,
)
from plugins.ScreenVision.screen_vision_core.observation_analysis import (
    ScreenObservationAnalysisHelper,
)
from plugins.ScreenVision.screen_vision_core.observation_memory_dispatch import (
    ScreenObservationMemoryDispatch,
)


class FakeObservationPolicy:
    def normalize(self, observation):
        return " ".join(str(observation or "").split())

    def is_broken(self, observation):
        return observation == "broken"

    def describe_broken(self, observation):
        return f"broken detail: {observation}"

    def is_no_important_change(self, observation):
        return observation == "no change"

    def describe_no_important_change(self, observation):
        return f"no-change detail: {observation}"

    def is_duplicate(self, previous_observation, current_observation):
        return previous_observation == current_observation

    def describe_duplicate(self, previous_observation, current_observation):
        return f"duplicate detail: {previous_observation} == {current_observation}"


class ScreenObservationProcessorTests(unittest.TestCase):
    def setUp(self):
        self.processor = ScreenObservationProcessor(
            FakeObservationPolicy(),
        )

    def test_accepts_normalized_observation(self):
        decision = self.processor.evaluate("  normal   observation ")

        self.assertTrue(decision.accepted)
        self.assertEqual("normal observation", decision.observation)
        self.assertEqual("", decision.reason)
        self.assertEqual("passed checks: broken/noise", decision.detail)

    def test_rejects_in_existing_filter_order(self):
        decision = self.processor.evaluate(
            "broken",
            previous_observation="broken",
            reject_no_important_change=True,
            check_duplicate=True,
            is_ai_speaking_callback=lambda: True,
            can_send_callback=lambda: False,
        )

        self.assertFalse(decision.accepted)
        self.assertEqual("broken/noise", decision.reason)
        self.assertEqual("broken detail: broken", decision.detail)

    def test_optional_no_change_and_duplicate_checks(self):
        no_change = self.processor.evaluate(
            "no change",
            reject_no_important_change=True,
        )
        duplicate = self.processor.evaluate(
            "same",
            previous_observation="same",
            check_duplicate=True,
        )

        self.assertEqual("no_important_change", no_change.reason)
        self.assertEqual("no-change detail: no change", no_change.detail)
        self.assertEqual("duplicate/similar", duplicate.reason)
        self.assertEqual("duplicate detail: same == same", duplicate.detail)

    def test_callbacks_are_lazy_and_keep_order(self):
        calls = []

        decision = self.processor.evaluate(
            "normal",
            is_ai_speaking_callback=lambda: calls.append("ai") or False,
            can_send_callback=lambda: calls.append("cooldown") or False,
        )

        self.assertFalse(decision.accepted)
        self.assertEqual("cooldown", decision.reason)
        self.assertEqual("can_send_callback returned False", decision.detail)
        self.assertEqual(["ai", "cooldown"], calls)

    def test_accepts_and_summarizes_long_structured_observation(self):
        processor = ScreenObservationProcessor(ObservationPolicy())
        observation = " ".join(
            f"Visible item {index}. YouTube URL https://example.com/{index}. "
            f"Time 02:{index:02d}. Normal screen detail appears."
            for index in range(30)
        )

        decision = processor.evaluate(observation)

        self.assertTrue(decision.accepted)
        self.assertLess(
            len(decision.observation),
            len(ObservationPolicy().normalize(observation)),
        )
        self.assertIn("summarized=", decision.detail)


class ScreenObservationDispatchHelperTests(unittest.TestCase):
    def test_builds_screen_observation_payload(self):
        helper = ScreenObservationDispatchHelper()

        payload = helper.build_output_payload(
            observation="A browser window is visible.",
            question="What is on screen?",
            source="Manual",
        )

        self.assertEqual("screen_observation", payload["kind"])
        self.assertEqual("Manual", payload["source"])
        self.assertEqual("A browser window is visible.", payload["observation"])
        self.assertEqual("[Manual] A browser window is visible.", payload["display_text"])
        self.assertFalse(payload["remember_history"])
        self.assertEqual("normal", payload["metadata"]["screen_memory_quality"])
        self.assertIn("[Manual]", payload["text"])
        self.assertIn("What is on screen?", payload["text"])

    def test_saves_screen_observation_and_raw_event(self):
        class MemoryStore:
            def __init__(self):
                self.observations = []
                self.raw_events = []

            def add_screen_observation(self, **kwargs):
                self.observations.append(kwargs)

            def add_raw_event(self, **kwargs):
                self.raw_events.append(kwargs)

        memory_store = MemoryStore()
        helper = ScreenObservationDispatchHelper(memory_store)

        helper.save_observation(
            observation="A game lobby is visible.",
            question="latest",
            source="Auto",
            event_type="screen_observation_silent",
            error_log_message="[Memory] silent screen observation save failed",
            silent=True,
        )

        self.assertEqual(
            [{
                "observation": "A game lobby is visible.",
                "source": "Auto",
                "confidence": 0.95,
            }],
            memory_store.observations,
        )
        self.assertEqual("screen_observation_silent", memory_store.raw_events[0]["event_type"])
        self.assertEqual("A game lobby is visible.", memory_store.raw_events[0]["value"])
        self.assertTrue(memory_store.raw_events[0]["metadata"]["silent"])
        self.assertFalse(memory_store.raw_events[0]["metadata"]["remember_history"])
        self.assertEqual(
            "normal",
            memory_store.raw_events[0]["metadata"]["screen_memory_quality"],
        )

    def test_marks_self_ui_observation_as_low_confidence(self):#20260720_kpopmodder
        class MemoryStore:
            def __init__(self):
                self.observations = []
                self.raw_events = []

            def add_screen_observation(self, **kwargs):
                self.observations.append(kwargs)

            def add_raw_event(self, **kwargs):
                self.raw_events.append(kwargs)

        memory_store = MemoryStore()
        helper = ScreenObservationDispatchHelper(memory_store)

        helper.save_observation(
            observation="OBS Studio shows a Codex chat window.",
            question="latest",
            source="Auto",
            event_type="screen_observation_silent",
            error_log_message="[Memory] silent screen observation save failed",
            silent=True,
        )

        self.assertEqual([], memory_store.observations)
        self.assertEqual(
            "ui_noise",
            memory_store.raw_events[0]["metadata"]["screen_memory_quality"],
        )
        self.assertIn(
            "codex",
            memory_store.raw_events[0]["metadata"]["screen_memory_noise_terms"],
        )


class ScreenObservationMemoryDispatchTests(unittest.TestCase):
    def test_remembers_latest_screen_observation_and_saves_silent_event(self):
        class MemoryStore:
            def __init__(self):
                self.observations = []
                self.raw_events = []

            def add_screen_observation(self, **kwargs):
                self.observations.append(kwargs)

            def add_raw_event(self, **kwargs):
                self.raw_events.append(kwargs)

        class Owner:
            def __init__(self):
                self.memory_store = MemoryStore()
                self.dispatcher = ScreenObservationDispatchHelper(self.memory_store)
                self.last_screen_observation = ""
                self.last_screen_observation_source = ""
                self.last_screen_observation_time = 0.0

            def _get_observation_dispatcher(self):
                return self.dispatcher

        owner = Owner()
        helper = ScreenObservationMemoryDispatch(owner)

        helper.remember_screen_observation(
            observation="A game lobby is visible.",
            question="latest",
            source="Auto",
        )

        self.assertEqual("A game lobby is visible.", owner.last_screen_observation)
        self.assertEqual("Auto", owner.last_screen_observation_source)
        self.assertGreater(owner.last_screen_observation_time, 0.0)
        self.assertEqual(
            [{
                "observation": "A game lobby is visible.",
                "source": "Auto",
                "confidence": 0.95,
            }],
            owner.memory_store.observations,
        )
        self.assertEqual(
            "screen_observation_silent",
            owner.memory_store.raw_events[0]["event_type"],
        )
        self.assertTrue(owner.memory_store.raw_events[0]["metadata"]["silent"])

    def test_send_observation_to_llm_saves_and_dispatches_payload(self):
        class MemoryStore:
            def __init__(self):
                self.observations = []
                self.raw_events = []

            def add_screen_observation(self, **kwargs):
                self.observations.append(kwargs)

            def add_raw_event(self, **kwargs):
                self.raw_events.append(kwargs)

        class Owner:
            def __init__(self):
                self.memory_store = MemoryStore()
                self.dispatcher = ScreenObservationDispatchHelper(self.memory_store)
                self.outputs = []

            def _get_observation_dispatcher(self):
                return self.dispatcher

            def send_output(self, output):
                self.outputs.append(output)

        owner = Owner()
        helper = ScreenObservationMemoryDispatch(owner)

        helper.send_observation_to_llm(
            observation="A browser is visible.",
            question="What is on screen?",
            source="Manual",
        )

        self.assertEqual(
            [{
                "observation": "A browser is visible.",
                "source": "Manual",
                "confidence": 0.95,
            }],
            owner.memory_store.observations,
        )
        self.assertEqual("screen_observation", owner.memory_store.raw_events[0]["event_type"])
        self.assertEqual(1, len(owner.outputs))
        self.assertEqual("screen_observation", owner.outputs[0]["kind"])
        self.assertEqual("Manual", owner.outputs[0]["source"])
        self.assertFalse(owner.outputs[0]["remember_history"])


class ScreenObservationDecisionReporterTests(unittest.TestCase):
    def test_records_decision_with_duplicate_context(self):
        class Decision:
            accepted = False
            observation = "same screen"
            reason = "duplicate/similar"
            detail = "same as previous"

        events = []

        reporter = ScreenObservationDecisionReporter(
            record_raw_event_callback=lambda **kwargs: events.append(kwargs),
            live_textbox=None,
        )

        reporter.record_decision(
            decision=Decision(),
            raw_observation="raw same screen",
            event_source="auto_watch",
            question="question",
            last_auto_observation="same screen",
            metadata={"stage": "test"},
        )

        self.assertEqual("screen_observation_decision", events[0]["event_type"])
        self.assertEqual("raw same screen", events[0]["value"])
        self.assertEqual("auto_watch", events[0]["source"])
        self.assertFalse(events[0]["metadata"]["accepted"])
        self.assertEqual("same screen", events[0]["metadata"]["normalized_observation"])
        self.assertEqual("same as previous", events[0]["metadata"]["detail"])
        self.assertEqual("same screen", events[0]["metadata"]["last_auto_observation"])
        self.assertEqual("test", events[0]["metadata"]["stage"])

    def test_reports_ignored_ai_speaking_to_live_textbox(self):
        class Decision:
            observation = "screen"
            reason = "ai_speaking"
            detail = "callback returned True"

        class LiveTextbox:
            def __init__(self):
                self.messages = []

            def print(self, message):
                self.messages.append(message)

        live_textbox = LiveTextbox()
        reporter = ScreenObservationDecisionReporter(
            record_raw_event_callback=lambda **kwargs: None,
            live_textbox=live_textbox,
        )

        reporter.report_ignored(Decision(), "Auto")

        self.assertEqual(
            [
                "[ScreenVision] Auto observation skipped: AI is speaking. "
                "(callback returned True)"
            ],
            live_textbox.messages,
        )


class ScreenObservationPromptBuilderTests(unittest.TestCase):
    def test_builds_screen_question_with_trimmed_hint(self):
        builder = ScreenObservationPromptBuilder()

        question = builder.build_screen_question("  custom hint  ")

        self.assertIn("custom hint ", question)
        self.assertNotIn("  custom hint  ", question)
        self.assertIn("당신은 PC 화면 관찰 기록 도우미입니다.", question)
        self.assertNotRegex(question, r"\?[가-힣]")

    def test_builds_screen_question_without_hint_gap(self):
        builder = ScreenObservationPromptBuilder()

        question = builder.build_screen_question("")

        self.assertNotIn("None", question)
        self.assertIn("화면에 실제로 보이는 내용만", question)


class ScreenObservationTextCleanerTests(unittest.TestCase):
    def test_delegates_normalize_to_policy(self):
        class Policy:
            def normalize(self, observation):
                return f"normalized:{observation}"

            def normalize_for_decision(self, observation):
                return f"decision:{observation}"

        cleaner = ScreenObservationTextCleaner(Policy())

        self.assertEqual("normalized:screen", cleaner.normalize("screen"))
        self.assertEqual(
            "decision:screen",
            cleaner.normalize_for_decision("screen"),
        )

    def test_strips_existing_screen_prefixes(self):
        cleaner = ScreenObservationTextCleaner(ObservationPolicy())

        self.assertEqual(
            "YouTube is visible.",
            cleaner.strip_screen_prefix(
                "현재 화면에는 PC 화면에는 YouTube is visible.",
            ),
        )


class ScreenObservationResultFlowTests(unittest.TestCase):
    def test_process_records_and_reports_accepted_observation(self):
        class Decision:
            accepted = True
            observation = "normalized screen"
            reason = ""
            detail = "ok"

        class LiveTextbox:
            def __init__(self):
                self.messages = []

            def print(self, message):
                self.messages.append(message)

        calls = []
        live_textbox = LiveTextbox()
        flow = ScreenObservationResultFlow(
            normalize_callback=lambda raw: "normalized screen",
            evaluate_callback=lambda observation, **kwargs: calls.append(
                ("evaluate", observation, kwargs)
            ) or Decision(),
            record_raw_event_callback=lambda **kwargs: calls.append(
                ("raw", kwargs)
            ),
            record_decision_callback=lambda **kwargs: calls.append(
                ("decision", kwargs)
            ),
            record_ignored_callback=lambda **kwargs: calls.append(
                ("ignored", kwargs)
            ),
            report_ignored_callback=lambda decision, label: calls.append(
                ("report_ignored", label)
            ),
            report_accepted_callback=lambda decision, label: calls.append(
                ("report_accepted", label)
            ),
            live_textbox=live_textbox,
        )

        result = flow.process(
            raw_observation=" raw screen ",
            event_source="auto_watch",
            question="question",
            label="Auto",
            decision_kwargs={"check_duplicate": True},
            raw_metadata={"difference": 4},
            decision_metadata={"manual_force": False},
            print_observation_prefix="[ScreenVision] Auto observation",
        )

        self.assertTrue(result.accepted)
        self.assertEqual("normalized screen", result.observation)
        self.assertEqual(
            ["[ScreenVision] Auto observation: normalized screen"],
            live_textbox.messages,
        )
        self.assertEqual("raw", calls[0][0])
        self.assertEqual("screen_observation_raw", calls[0][1]["event_type"])
        self.assertEqual("auto_watch", calls[0][1]["source"])
        self.assertEqual(
            {
                "normalized_observation": "normalized screen",
                "stage": "captured",
                "question": "question",
                "difference": 4,
            },
            calls[0][1]["metadata"],
        )
        self.assertEqual(
            ("evaluate", "normalized screen", {"check_duplicate": True}),
            calls[1],
        )
        self.assertEqual("decision", calls[2][0])
        self.assertEqual({"manual_force": False}, calls[2][1]["metadata"])
        self.assertEqual(("report_accepted", "Auto"), calls[3])
        self.assertNotIn("ignored", [call[0] for call in calls])

    def test_process_records_and_reports_ignored_observation(self):
        class Decision:
            accepted = False
            observation = "broken"
            reason = "broken/noise"
            detail = "broken detail"

        calls = []
        flow = ScreenObservationResultFlow(
            normalize_callback=lambda raw: "broken",
            evaluate_callback=lambda observation, **kwargs: Decision(),
            record_raw_event_callback=lambda **kwargs: calls.append(
                ("raw", kwargs)
            ),
            record_decision_callback=lambda **kwargs: calls.append(
                ("decision", kwargs)
            ),
            record_ignored_callback=lambda **kwargs: calls.append(
                ("ignored", kwargs)
            ),
            report_ignored_callback=lambda decision, label: calls.append(
                ("report_ignored", label)
            ),
            report_accepted_callback=lambda decision, label: calls.append(
                ("report_accepted", label)
            ),
            live_textbox=None,
        )

        result = flow.process(
            raw_observation="raw broken",
            event_source="latest_screen",
            question="latest",
            label="Latest",
            decision_metadata={"manual_force": True},
        )

        self.assertFalse(result.accepted)
        self.assertEqual("ignored", calls[2][0])
        self.assertEqual({"manual_force": True}, calls[2][1]["metadata"])
        self.assertEqual(("report_ignored", "Latest"), calls[3])
        self.assertNotIn("report_accepted", [call[0] for call in calls])


class ScreenObservationAnalysisHelperTests(unittest.TestCase):
    def test_analyze_and_process_passes_analyzer_result_to_flow(self):
        calls = []

        class Flow:
            def process(self, **kwargs):
                calls.append(("process", kwargs))
                return "flow-result"

        helper = ScreenObservationAnalysisHelper(
            analyze_callback=lambda **kwargs: calls.append(
                ("analyze", kwargs)
            ) or "raw screen",
            result_flow_provider=lambda: Flow(),
        )

        result = helper.analyze_and_process(
            image="image",
            question="question",
            event_source="manual_look",
            label="Manual",
            decision_kwargs={"check_duplicate": True},
            raw_metadata={"difference": 3},
            decision_metadata={"manual_force": False},
            print_observation_prefix="[ScreenVision] Manual observation",
            analyze_kwargs={"max_new_tokens": 512},
        )

        self.assertEqual("flow-result", result)
        self.assertEqual(
            (
                "analyze",
                {
                    "image": "image",
                    "question": "question",
                    "max_new_tokens": 512,
                },
            ),
            calls[0],
        )
        self.assertEqual("process", calls[1][0])
        self.assertEqual("raw screen", calls[1][1]["raw_observation"])
        self.assertEqual("manual_look", calls[1][1]["event_source"])
        self.assertEqual({"check_duplicate": True}, calls[1][1]["decision_kwargs"])
        self.assertEqual({"difference": 3}, calls[1][1]["raw_metadata"])
        self.assertEqual({"manual_force": False}, calls[1][1]["decision_metadata"])

    def test_analyze_and_process_returns_none_when_analysis_is_interrupted(self):
        calls = []
        helper = ScreenObservationAnalysisHelper(
            analyze_callback=lambda **kwargs: None,
            result_flow_provider=lambda: calls.append("flow"),
        )

        result = helper.analyze_and_process(
            image="image",
            question="question",
            event_source="auto_watch",
            label="Auto",
        )

        self.assertIsNone(result)
        self.assertEqual([], calls)


if __name__ == "__main__":
    unittest.main()
