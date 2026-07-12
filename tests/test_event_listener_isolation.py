#20260703_kpopmodder: Listener failures should not break later runtime subscribers.
import sys
import unittest
from pathlib import Path
from unittest import mock


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


class EventListenerIsolationTests(unittest.TestCase):
    def test_llm_dispatcher_continues_after_output_listener_failure(self):
        from llm_core.event_dispatcher import LLMEventDispatcher

        dispatcher = LLMEventDispatcher()
        calls = []

        def broken_listener(output):
            raise RuntimeError("listener boom")

        def stable_listener(output):
            calls.append(output)

        dispatcher.add_output_event_listener(broken_listener)
        dispatcher.add_output_event_listener(stable_listener)

        with mock.patch("llm_core.event_dispatcher.log_print") as log_mock:
            dispatcher.send_output("hello")

        self.assertEqual(["hello"], calls)
        self.assertIn("listener boom", str(log_mock.mock_calls))

    def test_llm_dispatcher_continues_after_full_output_listener_failure(self):
        from llm_core.event_dispatcher import LLMEventDispatcher

        dispatcher = LLMEventDispatcher()
        calls = []

        def broken_listener(output):
            raise RuntimeError("full listener boom")

        def stable_listener(output):
            calls.append(output)

        dispatcher.add_output_event_listener(broken_listener, full_response=True)
        dispatcher.add_output_event_listener(stable_listener, full_response=True)

        with mock.patch("llm_core.event_dispatcher.log_print") as log_mock:
            dispatcher.send_full_output("complete")

        self.assertEqual(["complete"], calls)
        self.assertIn("full listener boom", str(log_mock.mock_calls))

    def test_screen_vision_send_output_continues_after_listener_failure(self):
        from plugins.ScreenVision.screenVision import ScreenVision

        screen_vision = ScreenVision.__new__(ScreenVision)
        calls = []

        def broken_listener(output):
            raise RuntimeError("screen listener boom")

        def stable_listener(output):
            calls.append(output)

        screen_vision.output_event_listeners = [
            broken_listener,
            stable_listener,
        ]

        with mock.patch("plugins.ScreenVision.screenVision.log_print") as log_mock:
            screen_vision.send_output({"text": "screen"})

        self.assertEqual([{"text": "screen"}], calls)
        self.assertIn("screen listener boom", str(log_mock.mock_calls))


if __name__ == "__main__":
    unittest.main()
