#20260907_kpopmodder: Coordinate generalized grant preparation and submission cleanup.
from __future__ import annotations


class CommandFeedbackSubmissionObserver:
    def __init__(self, *, extension, admission_coordinator) -> None:
        self._extension = extension
        self._admission = admission_coordinator

    def prepare(
        self,
        *,
        event: object,
        korean_eligibility_proof: object,
        translation: object,
    ):
        try:
            grant = self._admission.issue(
                event=event,
                korean_eligibility_proof=korean_eligibility_proof,
                translation=translation,
            )
        except Exception:
            return None
        return self.prepare_grant(grant)

    def prepare_descriptor(self, descriptor: object):
        return self.prepare_grant(self._admission.issue_descriptor(descriptor))

    def prepare_grant(self, grant: object):
        if grant is None:
            return None
        reserve = getattr(self._extension, "reserve_command_feedback", None)
        if not callable(reserve) and self._legacy_exact_craft(grant):
            reserve = getattr(self._extension, "reserve_crafting_feedback", None)
        if not callable(reserve):
            return None
        try:
            return grant if reserve(grant) is True else None
        except Exception:
            self.abandon(grant)
            return None

    def claim_start(self, grant: object, result: object):
        if grant is None:
            return False
        claim = getattr(self._extension, "claim_command_feedback_start", None)
        if not callable(claim) and self._legacy_exact_craft(grant):
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
        abandon = getattr(self._extension, "abandon_command_feedback", None)
        if not callable(abandon) and self._legacy_exact_craft(grant):
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

    @staticmethod
    def _legacy_exact_craft(grant: object) -> bool:
        descriptor = getattr(grant, "descriptor", None)
        return bool(
            getattr(descriptor, "command", None) == "get diamond_pickaxe 1"
            and getattr(descriptor, "acquisition_verb_class", None) == "craft"
        )


__all__ = ("CommandFeedbackSubmissionObserver",)
