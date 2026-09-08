#20260907_kpopmodder: Lock authoritative Java effect evidence requirements.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.crafting import (
    CraftingFeedbackEffectVerifier,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandTerminalEvidenceEvaluation,
)


class CraftingFeedbackEffectVerifierTests(unittest.TestCase):
    def test_accepts_only_consistent_authoritative_positive_delta(self):
        verifier = CraftingFeedbackEffectVerifier()

        self.assertTrue(verifier.verified(_result()))
        self.assertFalse(
            verifier.verified(
                _result(effect_observation_status="inventory_unavailable")
            )
        )
        self.assertFalse(verifier.verified(_result(target_count_delta=True)))
        self.assertFalse(verifier.verified(_result(target_count_delta=2)))
        self.assertFalse(verifier.verified(_result(result_reason="completed")))
        self.assertFalse(
            verifier.verified(
                _result(),
                context=SimpleNamespace(
                    target_item="diamond_pickaxe",
                    requested_count=1,
                    before_target_count=4,
                ),
            )
        )

    def test_evaluate_preserves_common_get_evaluation_and_verified_parity(self):
        verifier = CraftingFeedbackEffectVerifier()
        result = _result()

        evaluation = verifier.evaluate(result)

        self.assertIs(type(evaluation), CommandTerminalEvidenceEvaluation)
        self.assertIs(evaluation.verified, True)
        self.assertIsNone(evaluation.projection)
        self.assertIs(verifier.verified(result), evaluation.verified)

    def test_evaluate_forwards_projection_without_calling_a_legacy_api(self):
        projection = object()
        expected = CommandTerminalEvidenceEvaluation(True, projection)
        delegate = _EvaluateOnly(expected)
        verifier = CraftingFeedbackEffectVerifier(delegate)
        result = _result()
        context = object()

        actual = verifier.evaluate(result, context=context)

        self.assertIs(expected, actual)
        self.assertTrue(verifier.verified(result, context=context))
        self.assertEqual([(result, context), (result, context)], delegate.calls)


def _result(**overrides) -> CommandResultDTO:
    data = {
        "result_reason": "matching_task_finished",
        "result_fidelity": "callback_plus_matching_user_task_event",
        "effect_kind": "get_acquisition_delta",
        "target_item": "diamond_pickaxe",
        "requested_count": 1,
        "before_target_count": 0,
        "after_target_count": 1,
        "target_count_delta": 1,
        "effect_observation_status": "authoritative",
    }
    data.update(overrides)
    return CommandResultDTO(
        request_id="request-1",
        ok=True,
        status=CommandResultStatus.COMPLETED,
        data=data,
    )


class _EvaluateOnly:
    def __init__(self, evaluation: CommandTerminalEvidenceEvaluation) -> None:
        self._evaluation = evaluation
        self.calls: list[tuple[object, object]] = []

    def evaluate(self, result: object, *, context: object = None):
        self.calls.append((result, context))
        return self._evaluation


if __name__ == "__main__":
    unittest.main()
