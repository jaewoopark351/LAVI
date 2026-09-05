#20260905_kpopmodder: Assemble scoped crafting submission collaborators only.
from plugins.Minecraft.fabric.chatclef.extension.natural_language.generic_crafting_defaults.submission.generic_crafting_extension_submission_pipeline import (
    GenericCraftingExtensionSubmissionPipeline,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.generic_crafting_defaults.submission.generic_crafting_submission_activation_spend_guard import (
    GenericCraftingSubmissionActivationSpendGuard,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.generic_crafting_defaults.submission.generic_crafting_submission_cleanup import (
    GenericCraftingSubmissionCleanup,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.generic_crafting_defaults.submission.generic_crafting_submission_execution_lifecycle import (
    GenericCraftingSubmissionExecutionLifecycle,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.generic_crafting_defaults.submission.generic_crafting_submission_input_decoder import (
    GenericCraftingSubmissionInputDecoder,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.generic_crafting_defaults.submission.generic_crafting_submission_result_recorder import (
    GenericCraftingSubmissionResultRecorder,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.generic_crafting_defaults.submission.generic_crafting_submission_translation_parser import (
    GenericCraftingSubmissionTranslationParser,
)


class GenericCraftingSubmissionComponentGraph:
    def __init__(
        self,
        *,
        translated_submission,
        activation_lifecycle,
        result_factory,
        result_recorder,
        action: str,
    ):
        self.input_decoder = GenericCraftingSubmissionInputDecoder(
            result_factory
        )
        self.translation_parser = GenericCraftingSubmissionTranslationParser(
            result_factory
        )
        self.spend_guard = GenericCraftingSubmissionActivationSpendGuard(
            activation_lifecycle=activation_lifecycle,
            result_factory=result_factory,
        )
        self.early_result_recorder = GenericCraftingSubmissionResultRecorder(
            result_recorder,
            action=action,
        )
        self.cleanup = GenericCraftingSubmissionCleanup(activation_lifecycle)
        self.execution_lifecycle = GenericCraftingSubmissionExecutionLifecycle(
            self.cleanup
        )
        self.pipeline = GenericCraftingExtensionSubmissionPipeline(
            translated_submission=translated_submission,
            input_decoder=self.input_decoder,
            translation_parser=self.translation_parser,
            spend_guard=self.spend_guard,
            early_result_recorder=self.early_result_recorder,
            action=action,
        )


__all__ = ("GenericCraftingSubmissionComponentGraph",)
