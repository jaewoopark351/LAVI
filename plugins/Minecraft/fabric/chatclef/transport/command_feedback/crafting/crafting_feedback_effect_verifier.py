#20260907_kpopmodder: Preserve the crafting verifier over generalized GET evidence.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandTerminalEvidenceEvaluator,
)


class CraftingFeedbackEffectVerifier:
    def __init__(self, evaluator=None) -> None:
        self._evaluator = evaluator or CommandTerminalEvidenceEvaluator()

    def evaluate(self, result: object, *, context: object = None):
        return self._evaluator.evaluate(result, context=context)

    def verified(self, result: object, *, context: object = None) -> bool:
        return self.evaluate(result, context=context).verified


__all__ = ("CraftingFeedbackEffectVerifier",)
