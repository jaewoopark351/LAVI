#20260913_kpopmodder: Verify immutable, bounded diagnostic projection and sink isolation.
from dataclasses import FrozenInstanceError, replace
from types import SimpleNamespace
import unittest
from unittest.mock import Mock

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.fabric.chatclef.diagnostics.goto_terminal import GotoTerminalDiagnosticObserver
from plugins.Minecraft.fabric.chatclef.diagnostics.goto_terminal.goto_terminal_diagnostic_formatter import GotoTerminalDiagnosticFormatter
from plugins.Minecraft.fabric.chatclef.diagnostics.goto_terminal.goto_terminal_diagnostic_projector import GotoTerminalDiagnosticProjector


class GotoTerminalDiagnosticProjectionTests(unittest.TestCase):
    def test_projection_is_immutable_bounded_and_has_no_raw_text_or_payload(self):
        projector = GotoTerminalDiagnosticProjector()
        text = "좌표 500, 90, -928로 가는 작업은 끝났는데, 도착했는지는 확인하지 못했어"
        descriptor = SimpleNamespace(command_name="goto", event_id="a" * 32, rollout_state="cautious")
        fact = SimpleNamespace(
            descriptor=descriptor, status="completed", verified=False,
            owner_token=SimpleNamespace(request_id="r", command_message_id="c", session_id="s", generation=1),
            result_reason="matching_task_finished", evidence_projection=None,
        )
        record = projector.response(
            role="late_terminal", fact=fact,
            profile=SimpleNamespace(profile_id="goto_phrase_v1", family="movement_goto"), text=text,
        )
        with self.assertRaises(FrozenInstanceError):
            record.verified = "true"
        with self.assertRaises(ValueError):
            replace(record, request_id="injected\nfield=true")
        message = GotoTerminalDiagnosticFormatter().format(record)
        self.assertNotIn(text, message)
        self.assertNotIn("500", message)
        self.assertEqual(str(len(text)), record.output_chars)
        self.assertEqual(64, len(record.output_sha256))
        self.assertEqual("false", record.verified)
        self.assertLessEqual(len(message), GotoTerminalDiagnosticFormatter.MAX_RECORD_CHARS)
        fact.verified = True
        self.assertEqual("false", record.verified)

    def test_allowlists_reject_injected_or_unbounded_identifiers_and_semantics(self):
        poisoned = Mock()
        poisoned.__str__ = Mock(side_effect=AssertionError("must not stringify arbitrary objects"))
        values = dict(
            role="invalid\nrole", reason="private-payload",
            context=SimpleNamespace(
                descriptor=SimpleNamespace(command_name="goto", event_id="x" * 1000),
                request_id=poisoned, command_message_id="private\nsecret", session_id="s" * 97, generation=True,
            ),
            result=CommandResultDTO(status="completed", ok=True, data={
                "result_reason": "PRIVATE_TEXT", "result_fidelity": "PRIVATE_TEXT", "raw": "PRIVATE_TEXT",
            }),
            profile=SimpleNamespace(profile_id="PRIVATE_TEXT", rollout_state="PRIVATE_TEXT", success_evaluator_id="PRIVATE_TEXT"),
            evaluation=SimpleNamespace(verified=False, projection=None),
        )
        record = GotoTerminalDiagnosticProjector().evidence(**values)
        self.assertEqual("result_evaluation", record.role)
        for field in ("reason", "event_id", "request_id", "correlation_id", "session_id", "generation", "profile_id", "rollout_state", "evaluator_id", "result_reason", "result_fidelity"):
            self.assertEqual("unknown", getattr(record, field), field)
        poisoned.__str__.assert_not_called()
        self.assertNotIn("PRIVATE", GotoTerminalDiagnosticFormatter().format(record))

    def test_disabled_or_failing_projection_formatting_and_sink_do_not_escape(self):
        values = dict(result=None, context=None, profile=None, evaluation=None, reason="invalid_result_type")
        projector = Mock()
        GotoTerminalDiagnosticObserver(None, projector=projector).evidence_decided(**values)
        projector.evidence.assert_not_called()
        for observer in (
            GotoTerminalDiagnosticObserver(Mock(), projector=SimpleNamespace(evidence=Mock(side_effect=RuntimeError("projection")))),
            GotoTerminalDiagnosticObserver(Mock(), projector=SimpleNamespace()),
            GotoTerminalDiagnosticObserver(Mock(), projector=SimpleNamespace(evidence=Mock(return_value=object())), formatter=Mock(format=Mock(side_effect=RuntimeError("format")))),
            GotoTerminalDiagnosticObserver(Mock(side_effect=RuntimeError("sink")), projector=SimpleNamespace(evidence=Mock(return_value=object())), formatter=SimpleNamespace(format=lambda _: "record")),
        ):
            observer.evidence_decided(**values)

    def test_other_commands_emit_nothing_and_large_rendered_text_is_not_hashed(self):
        logs = []
        observer = GotoTerminalDiagnosticObserver(logs.append)
        fact = SimpleNamespace(descriptor=SimpleNamespace(command_name="get"))
        observer.response_rendered(fact=fact, profile=None, text="private")
        self.assertEqual([], logs)
        fact.descriptor = SimpleNamespace(command_name="goto", event_id="b" * 32)
        observer.response_rendered(fact=fact, profile=None, text="x" * 5000)
        self.assertEqual(1, len(logs))
        self.assertIn("output_chars=5000 output_sha256=too_long", logs[0])
