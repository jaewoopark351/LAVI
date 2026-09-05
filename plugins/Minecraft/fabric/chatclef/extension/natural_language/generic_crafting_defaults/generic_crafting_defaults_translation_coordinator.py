#20260905_kpopmodder: Preserve scoped crafting translation as a thin facade.
from __future__ import annotations

from typing import Any

from .generic_crafting_defaults_activation_lifecycle import (
    GenericCraftingDefaultsActivationLifecycle,
)
from .generic_crafting_defaults_translation_result_factory import (
    GenericCraftingDefaultsTranslationResultFactory,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.generic_crafting_defaults.translation.generic_crafting_translation_component_graph import (
    GenericCraftingTranslationComponentGraph,
)


class GenericCraftingDefaultsTranslationCoordinator:
    def __init__(
        self,
        *,
        natural_language_service: object,
        activation_lifecycle: GenericCraftingDefaultsActivationLifecycle,
        result_factory: GenericCraftingDefaultsTranslationResultFactory | None = None,
        component_graph=None,
    ):
        self._natural_language_service = natural_language_service
        self._activation_lifecycle = activation_lifecycle
        self._result_factory = (
            result_factory or GenericCraftingDefaultsTranslationResultFactory()
        )
        self._component_graph = (
            component_graph
            or GenericCraftingTranslationComponentGraph(
                natural_language_service=natural_language_service,
                activation_lifecycle=activation_lifecycle,
                result_factory=self._result_factory,
            )
        )
        self._activation_guard = self._component_graph.activation_guard
        self._translation_invoker = self._component_graph.translation_invoker
        self._binding_commit = self._component_graph.binding_commit

    def translate(
        self,
        command: Any,
        *,
        input_event: object,
        item_resolution_profile: object,
        activation_receipt: object,
        korean_eligibility_proof: object,
    ) -> dict[str, Any]:
        rejection = self._activation_guard.rejection_if_invalid(
            activation_receipt,
            korean_eligibility_proof,
            input_event,
        )
        if rejection is not None:
            return rejection
        translated, rejection = self._translation_invoker.invoke(
            command,
            item_resolution_profile,
        )
        if rejection is not None:
            return rejection
        return self._binding_commit.commit(
            translated,
            activation_receipt=activation_receipt,
            korean_eligibility_proof=korean_eligibility_proof,
            input_event=input_event,
        )


__all__ = ("GenericCraftingDefaultsTranslationCoordinator",)
