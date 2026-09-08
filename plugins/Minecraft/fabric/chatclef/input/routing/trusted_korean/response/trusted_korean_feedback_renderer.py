#20260905_kpopmodder: Own rendering feedback into a handled route decision.
from __future__ import annotations

from dataclasses import replace


class TrustedKoreanFeedbackRenderer:
    def __init__(self, feedback_facade):
        self._feedback_facade = feedback_facade

    def render(self, decision):
        if not decision.handled:
            return decision
        response_text = self._feedback_facade.render(decision)
        if response_text == decision.response_text:
            return decision
        return replace(
            decision,
            response_text=response_text,
        )


__all__ = ("TrustedKoreanFeedbackRenderer",)
