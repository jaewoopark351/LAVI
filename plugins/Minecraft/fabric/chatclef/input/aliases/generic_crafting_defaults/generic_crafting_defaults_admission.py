#20260905_kpopmodder: Preserve generic-crafting admission as a thin facade.
from __future__ import annotations

from typing import Callable

from .admission import GenericCraftingAdmissionComponentGraph
from .generic_crafting_defaults_activation_registry import (
    GenericCraftingDefaultsActivationRegistry,
)
from .generic_crafting_defaults_admission_decision import (
    GenericCraftingDefaultsAdmissionDecision,
)
from .generic_crafting_defaults_candidate import GenericCraftingDefaultsCandidate


class GenericCraftingDefaultsAdmission:
    def __init__(
        self,
        activation_registry: GenericCraftingDefaultsActivationRegistry,
        eligibility_proof_validator: Callable[[object, object], bool],
    ):
        self._component_graph = GenericCraftingAdmissionComponentGraph(
            admission_owner=self,
            activation_registry=activation_registry,
            eligibility_proof_validator=eligibility_proof_validator,
        )
        self._registry_binding = self._component_graph.registry_binding
        self._rejection_decision_factory = (
            self._component_graph.rejection_decision_factory
        )
        self._evaluator = self._component_graph.evaluator
        self._activation_registry = self._registry_binding.activation_registry

    @property
    def activation_registry(self) -> GenericCraftingDefaultsActivationRegistry:
        return self._activation_registry

    def admit(
        self,
        event: object,
        eligibility_proof: object,
        candidate: GenericCraftingDefaultsCandidate,
    ) -> GenericCraftingDefaultsAdmissionDecision:
        return self._evaluator.admit(event, eligibility_proof, candidate)


__all__ = ("GenericCraftingDefaultsAdmission",)
