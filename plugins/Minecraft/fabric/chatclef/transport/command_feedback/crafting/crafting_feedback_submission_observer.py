#20260907_kpopmodder: Coordinate reservation, cleanup, and start-CAS delegation.
from __future__ import annotations


class CraftingFeedbackSubmissionObserver:
    def __init__(self, *, extension, authorizer) -> None:
        self._extension = extension
        self._authorizer = authorizer

    def prepare(
        self,
        *,
        event: object,
        korean_eligibility_proof: object,
        translation: object,
    ):
        try:
            grant = self._authorizer.issue(
                event=event,
                korean_eligibility_proof=korean_eligibility_proof,
                translation=translation,
            )
        except Exception:
            return None
        reserve = getattr(self._extension, "reserve_crafting_feedback", None)
        if grant is None or not callable(reserve):
            return None
        try:
            return grant if reserve(grant) is True else None
        except Exception:
            self.abandon(grant)
            return None

    def claim_start(self, grant: object, result: object):
        if grant is None:
            return False
        claim = getattr(self._extension, "claim_crafting_feedback_start", None)
        if not callable(claim):
            return False
        try:
            return claim(grant, result)
        except Exception:
            return None

    def abandon(self, grant: object) -> None:
        if grant is None:
            return
        abandon = getattr(self._extension, "abandon_crafting_feedback", None)
        if callable(abandon):
            try:
                abandon(grant)
                return
            except Exception:
                pass
        try:
            grant.abandon_if_reserved()
        except Exception:
            pass


__all__ = ("CraftingFeedbackSubmissionObserver",)
