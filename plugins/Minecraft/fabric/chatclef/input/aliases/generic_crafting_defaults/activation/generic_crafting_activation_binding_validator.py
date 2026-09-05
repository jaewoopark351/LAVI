#20260905_kpopmodder: Preserve the activation binding validator as a thin compatibility facade.
from __future__ import annotations

from typing import Any, Callable

from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.activation.binding_validation.generic_crafting_activation_binding_validation_component_graph import (
    GenericCraftingActivationBindingValidationComponentGraph,
)
from ..generic_crafting_defaults_activation_receipt import (
    GenericCraftingDefaultsActivationReceipt,
)
from ..generic_crafting_defaults_translation_projection import (
    GenericCraftingDefaultsTranslationProjection,
)


class GenericCraftingActivationBindingValidator:
    def __init__(self):
        self._component_graph = (
            GenericCraftingActivationBindingValidationComponentGraph()
        )

    def bind_proof_validator(
        self,
        proof_validator: Callable[[object, object], bool],
    ) -> None:
        self._component_graph.proof_validator.bind(proof_validator)

    def proof_is_valid(self, proof: object, event: object) -> bool:
        return self._component_graph.proof_validator.is_valid(proof, event)

    def matches_context(
        self,
        record: dict[str, object],
        eligibility_proof: object,
        event: object,
    ) -> bool:
        return self._component_graph.context_validator.matches(
            record,
            eligibility_proof,
            event,
        )

    def event_matches_receipt(
        self,
        event: object,
        receipt: GenericCraftingDefaultsActivationReceipt,
    ) -> bool:
        return self._component_graph.event_validator.matches(
            event,
            receipt,
        )

    def projection_matches_receipt(
        self,
        projection: GenericCraftingDefaultsTranslationProjection,
        receipt: GenericCraftingDefaultsActivationReceipt,
    ) -> bool:
        return self._component_graph.projection_validator.matches(
            projection,
            receipt,
        )

    def request_matches(
        self,
        receipt: GenericCraftingDefaultsActivationReceipt,
        request: object,
        translation: Any,
        projection: GenericCraftingDefaultsTranslationProjection,
    ) -> bool:
        return self._component_graph.request_validator.matches(
            receipt,
            request,
            translation,
            projection,
        )

    def receipt_belongs_to_registry(
        self,
        receipt: object,
        registry_token: object,
    ) -> bool:
        return self._component_graph.registry_record_validator.receipt_belongs_to_registry(
            receipt,
            registry_token,
        )

    def record_matches_receipt(
        self,
        record: dict[str, object],
        receipt: GenericCraftingDefaultsActivationReceipt,
    ) -> bool:
        return self._component_graph.registry_record_validator.record_matches_receipt(
            record,
            receipt,
        )

    def request_value(self, request: object, name: str) -> object:
        return self._component_graph.request_validator.value(request, name)

    @property
    def proof_validator(self):
        return self._component_graph.proof_validator.bound_validator


__all__ = ("GenericCraftingActivationBindingValidator",)
