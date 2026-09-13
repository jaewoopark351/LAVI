#20260913_kpopmodder: Carry validated failure details without claiming arrival or duplicating terminal output.
from types import SimpleNamespace
import unittest

from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.evidence.command_terminal_evidence_evaluation import CommandTerminalEvidenceEvaluation
from ..test_command_feedback_result_evaluation import (
    _ClaimOnceTracker, _CountingEvaluator, _accept_values, _coordinator, _result,
)


class GotoFailedFactProjectionTests(unittest.TestCase):
    def test_failed_goto_evaluated_once_and_projected_separately(self):
        failure = object()
        tracker = _ClaimOnceTracker()
        tracker.context.descriptor = SimpleNamespace(command_name="goto")
        evaluator = _CountingEvaluator(CommandTerminalEvidenceEvaluation(False, failure_projection=failure))
        coordinator = _coordinator(tracker, evaluator)
        fact = coordinator.accept_fact(**_accept_values(_result("failed")))
        self.assertEqual("failed", fact.status)
        self.assertFalse(fact.verified)
        self.assertIsNone(fact.evidence_projection)
        self.assertIs(failure, fact.failure_projection)
        self.assertIsNone(coordinator.accept_fact(**_accept_values(_result("failed"))))
        self.assertEqual(1, evaluator.evaluate_calls)

    def test_inconsistent_success_evaluation_cannot_promote_failed_fact(self):
        tracker = _ClaimOnceTracker()
        tracker.context.descriptor = SimpleNamespace(command_name="goto")
        fact = _coordinator(tracker, _CountingEvaluator(CommandTerminalEvidenceEvaluation(True))).accept_fact(
            **_accept_values(_result("failed")),
        )
        self.assertFalse(fact.verified)
        self.assertIsNone(fact.failure_projection)
