#20260905_kpopmodder: Preserve submission APIs as a thin one-shot sequence facade.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.input.routing.submission.minecraft_chatclef_submission_boundary_component_graph import (
    MinecraftChatClefSubmissionBoundaryComponentGraph,
)


class MinecraftChatClefSubmissionBoundary:
    def __init__(self):
        self._component_graph = MinecraftChatClefSubmissionBoundaryComponentGraph()
        self._request_builder = self._component_graph.request_builder
        self._transport_invoker = self._component_graph.transport_invoker
        self._outcome_validator = self._component_graph.outcome_validator

    @property
    def _request_factory(self):
        return self._request_builder.request_factory

    @_request_factory.setter
    def _request_factory(self, request_factory: object) -> None:
        self._request_builder.replace_request_factory(request_factory)

    @property
    def _result_normalizer(self):
        return self._outcome_validator.result_normalizer

    @_result_normalizer.setter
    def _result_normalizer(self, result_normalizer: object) -> None:
        self._outcome_validator.replace_result_normalizer(result_normalizer)

    def is_available(self, extension: Any) -> bool:
        return self._transport_invoker.is_available(extension)

    def is_generic_crafting_defaults_available(self, extension: Any) -> bool:
        return self._transport_invoker.is_generic_crafting_defaults_available(
            extension
        )

    def submit_once(
        self,
        extension: Any,
        event: Any,
        translation: Mapping[str, Any],
        *,
        route_claim: object = None,
        original_text: str | None = None,
        translation_input_text: str | None = None,
    ) -> dict[str, Any]:
        request = self._request_builder.build(
            event,
            original_text=original_text,
            translation_input_text=translation_input_text,
        )
        request_id = str(request["request_id"])
        try:
            payload = self._transport_invoker.invoke(
                extension,
                request,
                translation,
                route_claim=route_claim,
            )
        except Exception as error:
            return self._outcome_validator.transport_unknown(
                request_id,
                error,
                scoped=False,
            )
        try:
            return self._outcome_validator.validate(payload, request_id=request_id)
        except Exception as error:
            return self._outcome_validator.validation_unknown(
                request_id,
                error,
                scoped=False,
            )

    def submit_generic_crafting_defaults_once(
        self,
        extension: Any,
        event: Any,
        translation: Mapping[str, Any],
        *,
        activation_receipt: object,
        korean_eligibility_proof: object,
    ) -> dict[str, Any]:
        request = self._request_builder.build_generic_crafting_defaults(event)
        request_id = str(request["request_id"])
        try:
            payload = self._transport_invoker.invoke_generic_crafting_defaults(
                extension,
                request,
                translation,
                activation_receipt=activation_receipt,
                korean_eligibility_proof=korean_eligibility_proof,
            )
        except Exception as error:
            return self._outcome_validator.transport_unknown(
                request_id,
                error,
                scoped=True,
            )
        try:
            return self._outcome_validator.validate(payload, request_id=request_id)
        except Exception as error:
            return self._outcome_validator.validation_unknown(
                request_id,
                error,
                scoped=True,
            )


__all__ = ("MinecraftChatClefSubmissionBoundary",)
