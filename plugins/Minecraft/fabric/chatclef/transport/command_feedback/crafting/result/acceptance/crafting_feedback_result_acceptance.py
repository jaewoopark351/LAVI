#20260907_kpopmodder: Accept correlated crafting results without rendering responses.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackResultCoordinator,
)


class CraftingFeedbackResultAcceptance:
    def __init__(
        self,
        *,
        tracker,
        effect_verifier,
        stop_terminal_arbitrator=None,
        evidence_failure_reporter=None,
    ) -> None:
        self._coordinator = CommandFeedbackResultCoordinator(
            tracker=tracker,
            evidence_evaluator=effect_verifier,
            stop_terminal_arbitrator=stop_terminal_arbitrator,
            evidence_failure_reporter=evidence_failure_reporter,
        )

    def accept_fact(self, **values):
        return self._coordinator.accept_fact(**values)


__all__ = ("CraftingFeedbackResultAcceptance",)
