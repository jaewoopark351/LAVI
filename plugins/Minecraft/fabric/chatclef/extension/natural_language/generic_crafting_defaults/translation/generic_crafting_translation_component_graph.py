#20260905_kpopmodder: Assemble scoped crafting translation collaborators only.
from plugins.Minecraft.fabric.chatclef.extension.natural_language.generic_crafting_defaults.translation.generic_crafting_scoped_translation_invoker import (
    GenericCraftingScopedTranslationInvoker,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.generic_crafting_defaults.translation.generic_crafting_translation_activation_guard import (
    GenericCraftingTranslationActivationGuard,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.generic_crafting_defaults.translation.generic_crafting_translation_binding_commit import (
    GenericCraftingTranslationBindingCommit,
)


class GenericCraftingTranslationComponentGraph:
    def __init__(self, *, natural_language_service, activation_lifecycle, result_factory):
        self.activation_guard = GenericCraftingTranslationActivationGuard(
            activation_lifecycle=activation_lifecycle,
            result_factory=result_factory,
        )
        self.translation_invoker = GenericCraftingScopedTranslationInvoker(
            natural_language_service=natural_language_service,
            result_factory=result_factory,
        )
        self.binding_commit = GenericCraftingTranslationBindingCommit(
            activation_lifecycle=activation_lifecycle,
            result_factory=result_factory,
        )


__all__ = ("GenericCraftingTranslationComponentGraph",)
