#20260827_kpopmodder: Added this module to keep one project class per Python file.
#20260905_kpopmodder: Preserve natural-language APIs as a thin collaborator facade.
from __future__ import annotations

from typing import Any, Callable

from plugins.Minecraft.fabric.chatclef.command_registry.admission import (
    KoreanCommandSubmissionAdmission,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.composition import (
    NaturalLanguageCommandComponentGraph,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.generic_crafting_defaults import (
    GenericCraftingDefaultsSubmissionCoordinator,
    GenericCraftingDefaultsTranslationCoordinator,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.natural_language_command_result_payload_factory import (
    NaturalLanguageCommandResultPayloadFactory,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.translated_command_request_factory import (
    TranslatedCommandRequestFactory,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.translated_command_submission_coordinator import (
    TranslatedCommandSubmissionCoordinator,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.generic_crafting_defaults_activation_registry import (
    GenericCraftingDefaultsActivationRegistry,
)


class NaturalLanguageCommandCoordinator:
    def __init__(
        self,
        *,
        natural_language_service: object,
        registry_provider: Callable[[], object],
        command_submitter: Callable[[Any], dict[str, Any]],
        result_recorder: Callable[[dict[str, Any], str], None],
        admission: KoreanCommandSubmissionAdmission | None = None,
        request_factory: TranslatedCommandRequestFactory | None = None,
        result_factory: NaturalLanguageCommandResultPayloadFactory | None = None,
        generic_crafting_defaults_activation_registry: (
            GenericCraftingDefaultsActivationRegistry | None
        ) = None,
        translated_submission: TranslatedCommandSubmissionCoordinator | None = None,
        generic_crafting_translation: (
            GenericCraftingDefaultsTranslationCoordinator | None
        ) = None,
        generic_crafting_submission: (
            GenericCraftingDefaultsSubmissionCoordinator | None
        ) = None,
    ):
        self._component_graph = NaturalLanguageCommandComponentGraph(
            natural_language_service=natural_language_service,
            registry_provider=registry_provider,
            command_submitter=command_submitter,
            result_recorder=result_recorder,
            admission=admission,
            request_factory=request_factory,
            result_factory=result_factory,
            generic_crafting_defaults_activation_registry=(
                generic_crafting_defaults_activation_registry
            ),
            translated_submission=translated_submission,
            generic_crafting_translation=generic_crafting_translation,
            generic_crafting_submission=generic_crafting_submission,
        )
        self._component_graph.install_compatibility_seams(self)

    @property
    def _request_factory(self) -> object:
        return self._translated_submission.request_factory

    @_request_factory.setter
    def _request_factory(self, request_factory: object) -> None:
        self._translated_submission.replace_request_factory(request_factory)

    def translate(self, command: Any) -> dict[str, Any]:
        return self._legacy_commands.translate(command)

    def handle(self, command: Any) -> dict[str, Any]:
        return self._legacy_commands.handle(command)

    def translate_generic_crafting_defaults(
        self,
        command: Any,
        *,
        input_event: object,
        item_resolution_profile: object,
        activation_receipt: object,
        korean_eligibility_proof: object,
    ) -> dict[str, Any]:
        return self._generic_crafting_translation.translate(
            command,
            input_event=input_event,
            item_resolution_profile=item_resolution_profile,
            activation_receipt=activation_receipt,
            korean_eligibility_proof=korean_eligibility_proof,
        )

    def submit_translated(
        self,
        command: Any,
        translation: Any,
        *,
        route_claim: object = None,
    ) -> dict[str, Any]:
        return self._translated_submission.submit_translated(
            command,
            translation,
            route_claim=route_claim,
        )

    def submit_translated_generic_crafting_defaults(
        self,
        command: Any,
        translation: Any,
        *,
        activation_receipt: object,
        korean_eligibility_proof: object,
    ) -> dict[str, Any]:
        return self._generic_crafting_submission.submit(
            command,
            translation,
            activation_receipt=activation_receipt,
            korean_eligibility_proof=korean_eligibility_proof,
        )
