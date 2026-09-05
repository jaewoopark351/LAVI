#20260905_kpopmodder: Preserve generic-crafting submission as a thin facade.
from __future__ import annotations

from typing import Any, Callable

from plugins.Minecraft.fabric.chatclef.extension.natural_language.natural_language_command_result_payload_factory import (
    NaturalLanguageCommandResultPayloadFactory,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.translated_command_submission_coordinator import (
    TranslatedCommandSubmissionCoordinator,
)

from .generic_crafting_defaults_activation_lifecycle import (
    GenericCraftingDefaultsActivationLifecycle,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.generic_crafting_defaults.submission.generic_crafting_submission_component_graph import (
    GenericCraftingSubmissionComponentGraph,
)


class GenericCraftingDefaultsSubmissionCoordinator:
    ACTION = "submit_translated_generic_crafting_defaults"

    def __init__(
        self,
        *,
        translated_submission: TranslatedCommandSubmissionCoordinator,
        activation_lifecycle: GenericCraftingDefaultsActivationLifecycle,
        result_factory: NaturalLanguageCommandResultPayloadFactory,
        result_recorder: Callable[[dict[str, Any], str], None],
        component_graph=None,
    ):
        self._translated_submission = translated_submission
        self._activation_lifecycle = activation_lifecycle
        self._result_factory = result_factory
        self._result_recorder = result_recorder
        self._component_graph = (
            component_graph
            or GenericCraftingSubmissionComponentGraph(
                translated_submission=translated_submission,
                activation_lifecycle=activation_lifecycle,
                result_factory=result_factory,
                result_recorder=result_recorder,
                action=self.ACTION,
            )
        )
        self._input_decoder = self._component_graph.input_decoder
        self._translation_parser = self._component_graph.translation_parser
        self._spend_guard = self._component_graph.spend_guard
        self._early_result_recorder = self._component_graph.early_result_recorder
        self._cleanup = self._component_graph.cleanup
        self._pipeline = self._component_graph.pipeline
        self._execution_lifecycle = self._component_graph.execution_lifecycle

    def submit(
        self,
        command: Any,
        translation: Any,
        *,
        activation_receipt: object,
        korean_eligibility_proof: object,
    ) -> dict[str, Any]:
        return self._execution_lifecycle.execute(
            activation_receipt,
            lambda: self._pipeline.submit(
                command,
                translation,
                activation_receipt=activation_receipt,
                korean_eligibility_proof=korean_eligibility_proof,
            ),
        )


__all__ = ("GenericCraftingDefaultsSubmissionCoordinator",)
