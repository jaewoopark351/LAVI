#20260905_kpopmodder: Assemble generic-crafting admission collaborators only.
from __future__ import annotations

from .generic_crafting_admission_evaluator import (
    GenericCraftingAdmissionEvaluator,
)
from .generic_crafting_admission_registry_binding import (
    GenericCraftingAdmissionRegistryBinding,
)
from .generic_crafting_admission_rejection_decision_factory import (
    GenericCraftingAdmissionRejectionDecisionFactory,
)


class GenericCraftingAdmissionComponentGraph:
    def __init__(
        self,
        *,
        admission_owner: object,
        activation_registry,
        eligibility_proof_validator,
    ):
        self.registry_binding = GenericCraftingAdmissionRegistryBinding(
            activation_registry,
            eligibility_proof_validator,
        )
        self.registry_binding.bind(admission_owner)
        self.rejection_decision_factory = (
            GenericCraftingAdmissionRejectionDecisionFactory()
        )
        self.evaluator = GenericCraftingAdmissionEvaluator(
            activation_registry=self.registry_binding.activation_registry,
            admission_owner=admission_owner,
            rejection_decision_factory=self.rejection_decision_factory,
        )


__all__ = ("GenericCraftingAdmissionComponentGraph",)
