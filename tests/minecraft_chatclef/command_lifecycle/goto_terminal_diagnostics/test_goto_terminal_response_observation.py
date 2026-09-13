#20260913_kpopmodder: Observe rendered terminal statuses without changing existing sentences.
from types import SimpleNamespace
import unittest
from unittest.mock import Mock

from plugins.Minecraft.fabric.chatclef.diagnostics.goto_terminal import GotoTerminalDiagnosticObserver
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import CommandLifecycleResponseRenderer


class GotoTerminalResponseObservationTests(unittest.TestCase):
    def test_every_terminal_status_keeps_exact_text_with_disabled_enabled_or_throwing_observer(self):
        for status in ("completed", "failed", "cancelled", "rejected", "deadline_exceeded", "unknown"):
            with self.subTest(status=status):
                fact = _fact(status)
                baseline = CommandLifecycleResponseRenderer().render_terminal(fact)
                logs = []
                throwing = SimpleNamespace(response_rendered=Mock(side_effect=RuntimeError("observer method")))
                for observer in (GotoTerminalDiagnosticObserver(), GotoTerminalDiagnosticObserver(logs.append), throwing):
                    self.assertEqual(baseline, CommandLifecycleResponseRenderer(diagnostic_observer=observer).render_terminal(fact))
                self.assertEqual(1, len(logs))
                self.assertIn("boundary=response_rendered", logs[0])
                self.assertIn(f"status={status}", logs[0])
                self.assertNotIn(baseline, logs[0])
                throwing.response_rendered.assert_called_once()
        self.assertIn("도착했는지는 확인하지 못했어", CommandLifecycleResponseRenderer().render_terminal(_fact("completed")))
        self.assertIn("가다가 실패했어", CommandLifecycleResponseRenderer().render_terminal(_fact("failed")))

    def test_observer_receives_actual_output_and_render_exception_still_propagates(self):
        original_text = "same exact rendered object"
        phrase_renderer = Mock(terminal=Mock(return_value=original_text))
        observer = Mock()
        renderer = CommandLifecycleResponseRenderer(phrase_renderer=phrase_renderer, diagnostic_observer=observer)
        fact = _fact("failed")
        self.assertIs(original_text, renderer.render_terminal(fact))
        phrase_renderer.terminal.assert_called_once()
        self.assertIs(original_text, observer.response_rendered.call_args.kwargs["text"])
        phrase_renderer.terminal.side_effect = ValueError("original render failure")
        with self.assertRaisesRegex(ValueError, "original render failure"):
            renderer.render_terminal(fact)
        observer.response_rendered.assert_called_once()

    def test_start_and_status_do_not_emit_terminal_records(self):
        observer = Mock()
        renderer = CommandLifecycleResponseRenderer(diagnostic_observer=observer)
        renderer.render_start(_fact("completed").descriptor)
        renderer.render_status(SimpleNamespace(state="idle", owner_present=False, availability_reason="no_tracked_owner"))
        observer.response_rendered.assert_not_called()


def _fact(status):
    return SimpleNamespace(
        descriptor=SimpleNamespace(command_name="goto", event_id="a" * 32, coordinates=(500, 90, -928), rollout_state="cautious"),
        status=status, verified=False, dispatch_started=True, result_reason="matching_task_finished",
        evidence_projection=None, owner_token=SimpleNamespace(request_id="r", command_message_id="c", session_id="s", generation=1),
    )
