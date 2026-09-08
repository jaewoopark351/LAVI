#20260908_kpopmodder: Lock one-shot terminal evidence evaluation after correlation and claim.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.result.store_home import (
    StoreHomeTerminalPayload,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackResultCoordinator,
    CommandTerminalEvidenceEvaluation,
    CommandTerminalEvidenceFailureReporter,
)


class CommandFeedbackResultEvaluationTests(unittest.TestCase):
    def test_completed_prefers_evaluate_once_and_freezes_projection(self):
        projection = _store_projection()
        evaluator = _CountingEvaluator(
            CommandTerminalEvidenceEvaluation(True, projection)
        )
        tracker = _ClaimOnceTracker()
        coordinator = _coordinator(tracker, evaluator)

        fact = coordinator.accept_fact(**_accept_values(_result("completed")))
        duplicate = coordinator.accept_fact(**_accept_values(_result("completed")))

        self.assertIsNotNone(fact)
        self.assertTrue(fact.verified)
        self.assertIs(projection, fact.evidence_projection)
        self.assertIsNone(duplicate)
        self.assertEqual(1, evaluator.evaluate_calls)
        self.assertEqual(0, evaluator.verified_calls)
        self.assertEqual([fact], tracker.staged)

    def test_non_completed_terminal_never_calls_evidence_evaluator(self):
        evaluator = _CountingEvaluator(RuntimeError("must not run"), raises=True)
        tracker = _ClaimOnceTracker()

        fact = _coordinator(tracker, evaluator).accept_fact(
            **_accept_values(_result("failed"))
        )

        self.assertIsNotNone(fact)
        self.assertFalse(fact.verified)
        self.assertIsNone(fact.evidence_projection)
        self.assertEqual(0, evaluator.evaluate_calls)
        self.assertEqual(0, evaluator.verified_calls)

    def test_legacy_verified_only_accepts_exact_bool_without_projection(self):
        for decision, expected in (
            (True, True),
            (False, False),
            (1, False),
            (object(), False),
        ):
            with self.subTest(decision=decision):
                evaluator = _LegacyEvaluator(decision)
                fact = _coordinator(
                    _ClaimOnceTracker(),
                    evaluator,
                ).accept_fact(**_accept_values(_result("completed")))

                self.assertIsNotNone(fact)
                self.assertIs(expected, fact.verified)
                self.assertIsNone(fact.evidence_projection)
                self.assertEqual(1, evaluator.calls)

    def test_evaluator_exception_stages_one_cautious_fact_and_bounded_diagnostic(self):
        diagnostics = _Diagnostics()
        evaluator = _CountingEvaluator(
            _PrivateEvidenceError("secret request and result payload"),
            raises=True,
        )
        tracker = _ClaimOnceTracker()
        coordinator = _coordinator(
            tracker,
            evaluator,
            evidence_failure_reporter=CommandTerminalEvidenceFailureReporter(
                diagnostics
            ),
        )

        fact = coordinator.accept_fact(**_accept_values(_result("completed")))
        duplicate = coordinator.accept_fact(**_accept_values(_result("completed")))

        self.assertIsNotNone(fact)
        self.assertFalse(fact.verified)
        self.assertIsNone(fact.evidence_projection)
        self.assertIsNone(duplicate)
        self.assertEqual(1, evaluator.evaluate_calls)
        self.assertEqual(
            [
                "event=command_terminal_evidence_evaluation_failed "
                "command_name=store_home status=completed "
                "exception_type=_PrivateEvidenceError "
                "fallback=unverified_cautious"
            ],
            diagnostics.warnings,
        )
        self.assertNotIn("secret", diagnostics.warnings[0])

    def test_reporter_failure_cannot_lose_the_claimed_cautious_fact(self):
        evaluator = _CountingEvaluator(RuntimeError("private"), raises=True)
        tracker = _ClaimOnceTracker()
        reporter = SimpleNamespace(
            report=lambda **_values: (_ for _ in ()).throw(
                RuntimeError("reporter failed")
            )
        )

        fact = _coordinator(
            tracker,
            evaluator,
            evidence_failure_reporter=reporter,
        ).accept_fact(**_accept_values(_result("completed")))

        self.assertIsNotNone(fact)
        self.assertFalse(fact.verified)
        self.assertEqual([fact], tracker.staged)


def _coordinator(tracker, evaluator, *, evidence_failure_reporter=None):
    return CommandFeedbackResultCoordinator(
        tracker=tracker,
        evidence_evaluator=evaluator,
        correlator=SimpleNamespace(correlated=lambda **_values: True),
        evidence_failure_reporter=evidence_failure_reporter,
    )


def _accept_values(result):
    return {
        "websocket": object(),
        "envelope": object(),
        "result": result,
        "outcome": SimpleNamespace(accepted=True, reason="accepted"),
        "expected_active": _ClaimOnceTracker.OWNER,
    }


def _result(status: str) -> CommandResultDTO:
    result_status = CommandResultStatus(status)
    return CommandResultDTO(
        request_id="request-store-home",
        ok=result_status.ok,
        status=result_status,
        data={"result_reason": "matching_task_finished"},
    )


def _store_projection() -> StoreHomeTerminalPayload:
    projection = StoreHomeTerminalPayload.from_data(
        {
            "operation": "store_home",
            "store_home_result": "COMPLETED",
            "stored_items": 909,
            "remaining_stacks": 0,
            "reason": "matching_task_finished",
            "goal_satisfied": True,
        }
    )
    if projection is None:
        raise AssertionError("fixture projection failed")
    return projection


class _ClaimOnceTracker:
    OWNER = object()

    def __init__(self) -> None:
        self.context = SimpleNamespace(
            descriptor=SimpleNamespace(command_name="store_home"),
            event_id="a" * 32,
        )
        self.claimed = False
        self.staged = []

    def dispatch_started_observed(self, owner_token):
        return False

    def claim_terminal(self, owner_token):
        if self.claimed or owner_token is not self.OWNER:
            return None
        self.claimed = True
        return self.context

    def stage_terminal(self, owner_token, fact):
        if owner_token is not self.OWNER or not self.claimed:
            return None
        self.staged.append(fact)
        return fact


class _CountingEvaluator:
    def __init__(self, value, *, raises=False) -> None:
        self.value = value
        self.raises = raises
        self.evaluate_calls = 0
        self.verified_calls = 0

    def evaluate(self, _result, *, context=None):
        self.evaluate_calls += 1
        if self.raises:
            raise self.value
        return self.value

    def verified(self, _result, *, context=None):
        self.verified_calls += 1
        raise AssertionError("verified compatibility path must not run")


class _LegacyEvaluator:
    def __init__(self, decision) -> None:
        self.decision = decision
        self.calls = 0

    def verified(self, _result, *, context=None):
        self.calls += 1
        return self.decision


class _Diagnostics:
    def __init__(self) -> None:
        self.warnings = []

    def warning(self, message):
        self.warnings.append(message)


class _PrivateEvidenceError(Exception):
    pass


if __name__ == "__main__":
    unittest.main()
