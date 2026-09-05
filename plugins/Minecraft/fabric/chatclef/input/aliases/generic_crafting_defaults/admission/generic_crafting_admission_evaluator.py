#20260905_kpopmodder: Evaluate one generic-crafting admission request only.
from __future__ import annotations

from ..generic_crafting_defaults_admission_decision import (
    GenericCraftingDefaultsAdmissionDecision,
)
from ..generic_crafting_defaults_candidate import GenericCraftingDefaultsCandidate


class GenericCraftingAdmissionEvaluator:
    def __init__(
        self,
        *,
        activation_registry,
        admission_owner: object,
        rejection_decision_factory,
    ):
        self._activation_registry = activation_registry
        self._admission_owner = admission_owner
        self._rejection_decision_factory = rejection_decision_factory

    def admit(
        self,
        event: object,
        eligibility_proof: object,
        candidate: GenericCraftingDefaultsCandidate,
    ) -> GenericCraftingDefaultsAdmissionDecision:
        receipt, reason = self._activation_registry._issue(
            event,
            eligibility_proof,
            candidate,
            admission_owner=self._admission_owner,
        )
        if receipt is not None:
            return GenericCraftingDefaultsAdmissionDecision(
                admitted=True,
                feature_owned=True,
                receipt=receipt,
            )
        return self._rejection_decision_factory.create(reason)


__all__ = ("GenericCraftingAdmissionEvaluator",)
