#20260905_kpopmodder: Preserve translation APIs as a thin focused-boundary facade.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.input.routing.translation.minecraft_chatclef_translation_boundary_component_graph import (
    MinecraftChatClefTranslationBoundaryComponentGraph,
)


class MinecraftChatClefTranslationBoundary:
    def __init__(self) -> None:
        self._component_graph = MinecraftChatClefTranslationBoundaryComponentGraph()
        self._capability_inspector = self._component_graph.capability_inspector
        self._invoker = self._component_graph.invoker
        self._result_validator = self._component_graph.result_validator

    def is_available(self, extension: Any) -> bool:
        return self._capability_inspector.is_available(extension)

    def translate_once(self, extension: Any, text: str) -> Any:
        return self._invoker.invoke(extension, text)

    def is_generic_crafting_defaults_available(self, extension: Any) -> bool:
        return self._capability_inspector.is_generic_crafting_defaults_available(
            extension
        )

    def translate_generic_crafting_defaults_once(
        self,
        extension: Any,
        text: str,
        *,
        input_event: object,
        item_resolution_profile: object,
        activation_receipt: object,
        korean_eligibility_proof: object,
    ) -> Any:
        return self._invoker.invoke_generic_crafting_defaults(
            extension,
            text,
            input_event=input_event,
            item_resolution_profile=item_resolution_profile,
            activation_receipt=activation_receipt,
            korean_eligibility_proof=korean_eligibility_proof,
        )

    def validate(self, payload: Any) -> dict[str, Any]:
        return self._result_validator.validate(payload)

    def status(self, translation: Mapping[str, Any]) -> str:
        return self._result_validator.status(translation)

    def _mapping_payload(self, payload: Any) -> dict[str, Any]:
        return self._result_validator.mapping_payload(payload)


__all__ = ("MinecraftChatClefTranslationBoundary",)
