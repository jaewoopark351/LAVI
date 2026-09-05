#20260905_kpopmodder: Own one-time activation-registry and proof binding only.
from __future__ import annotations

from typing import Callable

from ..generic_crafting_defaults_activation_registry import (
    GenericCraftingDefaultsActivationRegistry,
)


class GenericCraftingAdmissionRegistryBinding:
    def __init__(
        self,
        activation_registry: GenericCraftingDefaultsActivationRegistry,
        eligibility_proof_validator: Callable[[object, object], bool],
    ):
        if not isinstance(
            activation_registry,
            GenericCraftingDefaultsActivationRegistry,
        ):
            raise TypeError("Feature-B activation registry is required")
        if not callable(eligibility_proof_validator):
            raise TypeError("Feature-B eligibility proof validator is required")
        self.activation_registry = activation_registry
        self._eligibility_proof_validator = eligibility_proof_validator

    def bind(self, admission_owner: object) -> None:
        self.activation_registry._bind_admission_owner(
            admission_owner,
            self._eligibility_proof_validator,
        )


__all__ = ("GenericCraftingAdmissionRegistryBinding",)
