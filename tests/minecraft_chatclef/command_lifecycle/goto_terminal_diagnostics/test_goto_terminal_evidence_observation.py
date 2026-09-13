#20260913_kpopmodder: Preserve evaluator decisions, exceptions, and terminal claim count.
from types import SimpleNamespace
import unittest
from unittest.mock import Mock

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.fabric.chatclef.diagnostics.goto_terminal import GotoTerminalDiagnosticObserver
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackResultCoordinator, CommandTerminalEvidenceEvaluation, CommandTerminalEvidenceEvaluator,
)


class GotoTerminalEvidenceObservationTests(unittest.TestCase):
    def test_legacy_goto_remains_unverified_for_off_on_and_throwing_observer(self):
        logs = []
        throwing = SimpleNamespace(evidence_decided=Mock(side_effect=RuntimeError("observer method")))
        for observer in (None, GotoTerminalDiagnosticObserver(logs.append), throwing):
            evaluation = CommandTerminalEvidenceEvaluator(diagnostic_observer=observer).evaluate(_result(), context=_context())
            self.assertFalse(evaluation.verified)
            self.assertIsNone(evaluation.projection)
            self.assertIsNone(evaluation.failure_projection)
        self.assertEqual(1, len(logs))
        self.assertIn("boundary=evidence_decided", logs[0])
        self.assertIn("reason=goto_result_identity_invalid", logs[0])
        self.assertIn("profile_id=goto_terminal_evidence_v1 rollout_state=verified", logs[0])
        throwing.evidence_decided.assert_called_once()

    def test_each_existing_rejection_reports_only_the_branch_actually_taken(self):
        cases = (
            ("invalid_result_type", {"result": object()}),
            ("invalid_result_data", {"bad_data": True}),
            ("profile_unavailable", {"profile_error": KeyError("missing")}),
            ("profile_not_verified", {"rollout": "cautious"}),
            ("descriptor_not_verified", {"descriptor_rollout": "cautious"}),
            ("unsupported_detail", {"detail": "command_name_only"}),
            ("evaluator_unavailable", {"evaluator_id": "missing"}),
            ("invalid_evaluation_type", {"evaluation": object()}),
        )
        for reason, changes in cases:
            with self.subTest(reason=reason):
                observer = Mock()
                result = changes.get("result", _result())
                if changes.get("bad_data"):
                    object.__setattr__(result, "data", None)
                registry = Mock()
                registry.profile.side_effect = changes.get("profile_error")
                registry.profile.return_value = _profile(
                    rollout_state=changes.get("rollout", "verified"),
                    success_evaluator_id=changes.get("evaluator_id", "get_acquisition"),
                )
                delegate = Mock(evaluate=Mock(return_value=changes.get("evaluation", CommandTerminalEvidenceEvaluation(True))))
                evaluator = CommandTerminalEvidenceEvaluator(registry, get_evaluator=delegate, diagnostic_observer=observer)
                context = _context(rollout=changes.get("descriptor_rollout", "verified"), detail=changes.get("detail", "typed"))
                actual = evaluator.evaluate(result, context=context)
                self.assertFalse(actual.verified)
                observer.evidence_decided.assert_called_once()
                self.assertEqual(reason, observer.evidence_decided.call_args.kwargs["reason"])
                self.assertIs(actual, observer.evidence_decided.call_args.kwargs["evaluation"])
                self.assertEqual(int(reason == "invalid_evaluation_type"), delegate.evaluate.call_count)

    def test_success_object_identity_and_delegated_exception_are_preserved(self):
        evaluation = CommandTerminalEvidenceEvaluation(True, object())
        delegate = Mock(evaluate=Mock(return_value=evaluation))
        observer = SimpleNamespace(evidence_decided=Mock(side_effect=RuntimeError("observer method")))
        evaluator = CommandTerminalEvidenceEvaluator(Mock(profile=Mock(return_value=_profile())), get_evaluator=delegate, diagnostic_observer=observer)
        self.assertIs(evaluation, evaluator.evaluate(_result(), context=_context()))
        delegate.evaluate.assert_called_once()
        delegate.evaluate.side_effect = LookupError("original domain failure")
        with self.assertRaisesRegex(LookupError, "original domain failure"):
            evaluator.evaluate(_result(), context=_context())
        self.assertEqual(1, observer.evidence_decided.call_count)

    def test_duplicate_terminal_claim_does_not_repeat_evaluation_or_diagnostic(self):
        logs = []
        context = _context()
        context.event_id = context.descriptor.event_id
        tracker = Mock(context=context)
        tracker.dispatch_started_observed.return_value = True
        tracker.claim_terminal.side_effect = (context, None)
        tracker.stage_terminal.side_effect = lambda _owner, fact: fact
        evaluator = CommandTerminalEvidenceEvaluator(diagnostic_observer=GotoTerminalDiagnosticObserver(logs.append))
        coordinator = CommandFeedbackResultCoordinator(
            tracker=tracker, evidence_evaluator=evaluator,
            correlator=SimpleNamespace(correlated=lambda **_: True),
        )
        values = dict(websocket=object(), envelope=object(), result=_result(), outcome=SimpleNamespace(accepted=True, reason="accepted"), expected_active=object())
        fact = coordinator.accept_fact(**values)
        self.assertFalse(fact.verified)
        self.assertIsNone(coordinator.accept_fact(**values))
        self.assertEqual(1, len(logs))
        tracker.stage_terminal.assert_called_once()


def _context(*, rollout="verified", detail="typed"):
    return SimpleNamespace(
        descriptor=SimpleNamespace(command_name="goto", event_id="a" * 32, rollout_state=rollout, detail_level=detail),
        request_id="r", command_message_id="c", session_id="s", generation=1,
    )


def _profile(**changes):
    return SimpleNamespace(**{
        "profile_id": "goto_terminal_evidence_v1", "rollout_state": "verified",
        "success_evaluator_id": "get_acquisition", **changes,
    })


def _result():
    return CommandResultDTO(request_id="r", status="completed", ok=True, data={"result_reason": "matching_task_finished"})
