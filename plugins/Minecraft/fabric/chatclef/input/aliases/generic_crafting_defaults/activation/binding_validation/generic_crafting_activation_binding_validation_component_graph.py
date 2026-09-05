#20260905_kpopmodder: Assemble activation binding validators outside the legacy facade.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.activation.binding_validation.generic_crafting_activation_context_binding_validator import (
    GenericCraftingActivationContextBindingValidator,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.activation.binding_validation.generic_crafting_activation_event_receipt_validator import (
    GenericCraftingActivationEventReceiptValidator,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.activation.binding_validation.generic_crafting_activation_projection_validator import (
    GenericCraftingActivationProjectionValidator,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.activation.binding_validation.generic_crafting_activation_proof_validator import (
    GenericCraftingActivationProofValidator,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.activation.binding_validation.generic_crafting_activation_registry_record_validator import (
    GenericCraftingActivationRegistryRecordValidator,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.activation.binding_validation.generic_crafting_activation_request_validator import (
    GenericCraftingActivationRequestValidator,
)


class GenericCraftingActivationBindingValidationComponentGraph:
    def __init__(self):
        self.proof_validator = GenericCraftingActivationProofValidator()
        self.event_validator = (
            GenericCraftingActivationEventReceiptValidator()
        )
        self.context_validator = (
            GenericCraftingActivationContextBindingValidator(
                proof_validator=self.proof_validator,
                event_validator=self.event_validator,
            )
        )
        self.projection_validator = (
            GenericCraftingActivationProjectionValidator()
        )
        self.request_validator = GenericCraftingActivationRequestValidator()
        self.registry_record_validator = (
            GenericCraftingActivationRegistryRecordValidator()
        )


__all__ = (
    "GenericCraftingActivationBindingValidationComponentGraph",
)
