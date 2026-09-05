#20260905_kpopmodder: Preserve route activation as a thin facade.
from __future__ import annotations

from .activation import (
    GenericCraftingActivationAdmissionStage,
    GenericCraftingScopedCapabilityAvailabilityStage,
)


class GenericCraftingActivationStage:
    def __init__(
        self,
        *,
        extension,
        admission,
        translation_boundary,
        submission_boundary,
        decision_factory,
        capability_stage=None,
        admission_stage=None,
    ):
        self._extension = extension
        self._admission = admission
        self._translation_boundary = translation_boundary
        self._submission_boundary = submission_boundary
        self._decision_factory = decision_factory
        self._capability_stage = (
            capability_stage
            or GenericCraftingScopedCapabilityAvailabilityStage(
                extension=extension,
                translation_boundary=translation_boundary,
                submission_boundary=submission_boundary,
                decision_factory=decision_factory,
            )
        )
        self._admission_stage = (
            admission_stage
            or GenericCraftingActivationAdmissionStage(
                admission=admission,
                decision_factory=decision_factory,
            )
        )

    def activate(
        self,
        event: object,
        korean_eligibility_proof: object,
        candidate: object,
    ):
        rejection = self._capability_stage.rejection_if_unavailable()
        if rejection is not None:
            return rejection
        return self._admission_stage.admit(
            event,
            korean_eligibility_proof,
            candidate,
        )

    def scoped_seams_available(self) -> bool:
        return self._capability_stage.is_available()


__all__ = ("GenericCraftingActivationStage",)
