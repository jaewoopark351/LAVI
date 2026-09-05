#20260905_kpopmodder: Assemble natural-language collaborators outside its facade.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.command_registry.admission import (
    KoreanCommandSubmissionAdmission,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.generic_crafting_defaults import (
    GenericCraftingDefaultsActivationLifecycle,
    GenericCraftingDefaultsSubmissionCoordinator,
    GenericCraftingDefaultsTranslationCoordinator,
    GenericCraftingDefaultsTranslationResultFactory,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.generic_crafting_defaults.submission.generic_crafting_submission_component_graph import (
    GenericCraftingSubmissionComponentGraph,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.generic_crafting_defaults.translation.generic_crafting_translation_component_graph import (
    GenericCraftingTranslationComponentGraph,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.legacy_natural_language_command_coordinator import (
    LegacyNaturalLanguageCommandCoordinator,
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

from .natural_language_command_compatibility_installer import (
    NaturalLanguageCommandCompatibilityInstaller,
)


class NaturalLanguageCommandComponentGraph:
    def __init__(
        self,
        *,
        natural_language_service,
        registry_provider,
        command_submitter,
        result_recorder,
        admission=None,
        request_factory=None,
        result_factory=None,
        generic_crafting_defaults_activation_registry=None,
        translated_submission=None,
        generic_crafting_translation=None,
        generic_crafting_submission=None,
    ):
        self._compatibility_installer = (
            NaturalLanguageCommandCompatibilityInstaller()
        )
        self.natural_language_service = natural_language_service
        self.admission = admission or KoreanCommandSubmissionAdmission()
        self.request_factory = request_factory or TranslatedCommandRequestFactory()
        self.result_factory = (
            result_factory or NaturalLanguageCommandResultPayloadFactory()
        )
        self.translated_submission = (
            translated_submission
            or TranslatedCommandSubmissionCoordinator(
                registry_provider=registry_provider,
                command_submitter=command_submitter,
                result_recorder=result_recorder,
                admission=self.admission,
                request_factory=self.request_factory,
                result_factory=self.result_factory,
            )
        )
        self.activation_lifecycle = GenericCraftingDefaultsActivationLifecycle(
            generic_crafting_defaults_activation_registry
        )
        self.generic_crafting_translation_result_factory = (
            GenericCraftingDefaultsTranslationResultFactory()
        )
        self.generic_crafting_translation_component_graph = (
            GenericCraftingTranslationComponentGraph(
                natural_language_service=natural_language_service,
                activation_lifecycle=self.activation_lifecycle,
                result_factory=self.generic_crafting_translation_result_factory,
            )
        )
        self.generic_crafting_translation = (
            generic_crafting_translation
            or GenericCraftingDefaultsTranslationCoordinator(
                natural_language_service=natural_language_service,
                activation_lifecycle=self.activation_lifecycle,
                result_factory=(
                    self.generic_crafting_translation_result_factory
                ),
                component_graph=(
                    self.generic_crafting_translation_component_graph
                ),
            )
        )
        self.generic_crafting_submission_component_graph = (
            GenericCraftingSubmissionComponentGraph(
                translated_submission=self.translated_submission,
                activation_lifecycle=self.activation_lifecycle,
                result_factory=self.result_factory,
                result_recorder=result_recorder,
                action=GenericCraftingDefaultsSubmissionCoordinator.ACTION,
            )
        )
        self.generic_crafting_submission = (
            generic_crafting_submission
            or GenericCraftingDefaultsSubmissionCoordinator(
                translated_submission=self.translated_submission,
                activation_lifecycle=self.activation_lifecycle,
                result_factory=self.result_factory,
                result_recorder=result_recorder,
                component_graph=(
                    self.generic_crafting_submission_component_graph
                ),
            )
        )
        self.legacy_commands = LegacyNaturalLanguageCommandCoordinator(
            natural_language_service=natural_language_service,
            translated_submission=self.translated_submission,
        )

    def install_compatibility_seams(self, owner) -> None:
        self._compatibility_installer.install(owner, self)


__all__ = ("NaturalLanguageCommandComponentGraph",)
