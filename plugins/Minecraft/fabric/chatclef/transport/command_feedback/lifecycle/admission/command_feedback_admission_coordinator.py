#20260907_kpopmodder: Issue feedback capabilities only for a live trusted descriptor proof.
from __future__ import annotations

from .command_feedback_admission_grant import CommandFeedbackAdmissionGrant


class CommandFeedbackAdmissionCoordinator:
    def __init__(self, *, live_proof_validator, descriptor_factory) -> None:
        self._live_proof_validator = live_proof_validator
        self._descriptor_factory = descriptor_factory

    def issue(
        self,
        *,
        event: object,
        korean_eligibility_proof: object,
        translation: object,
    ) -> CommandFeedbackAdmissionGrant | None:
        if self._live_proof_validator(korean_eligibility_proof, event) is not True:
            return None
        descriptor = self._descriptor_factory.from_trusted_translation(
            event=event,
            translation=translation,
        )
        return self.issue_descriptor(descriptor)

    def issue_descriptor(self, descriptor: object):
        validator = getattr(self._descriptor_factory, "accepts_descriptor", None)
        if not callable(validator) or validator(descriptor) is not True:
            return None
        try:
            return CommandFeedbackAdmissionGrant._issue(descriptor=descriptor)
        except (TypeError, ValueError):
            return None


__all__ = ("CommandFeedbackAdmissionCoordinator",)
