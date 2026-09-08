#20260908_kpopmodder: Reuse admission descriptor truth before a contextual STATUS claim.
from __future__ import annotations

from ....admission.command_feedback_admission_grant import (
    CommandFeedbackAdmissionGrant,
)
from ....descriptor.command_feedback_descriptor_factory import (
    CommandFeedbackDescriptorFactory,
)


class CommandStatusDescriptorEligibilityValidator:
    def __init__(
        self,
        *,
        descriptor_factory=None,
        evidence_profiles=None,
    ) -> None:
        self._descriptor_factory = (
            descriptor_factory
            if descriptor_factory is not None
            else CommandFeedbackDescriptorFactory(
                evidence_profiles=evidence_profiles,
            )
        )

    def accepts(
        self,
        descriptor: object,
        *,
        admission_grant: object = None,
    ) -> bool:
        try:
            validator = getattr(
                self._descriptor_factory,
                "accepts_descriptor",
                None,
            )
            if not callable(validator) or validator(descriptor) is not True:
                return False
            if admission_grant is None:
                return True
            return bool(
                type(admission_grant) is CommandFeedbackAdmissionGrant
                and admission_grant.descriptor is descriptor
            )
        except Exception:
            return False


__all__ = ("CommandStatusDescriptorEligibilityValidator",)
