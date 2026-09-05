#20260905_kpopmodder: Assemble activation collaborators outside the registry facade.
from ..generic_crafting_activation_binding_validator import (
    GenericCraftingActivationBindingValidator,
)
from ..generic_crafting_activation_context_factory import (
    GenericCraftingActivationContextFactory,
)
from ..generic_crafting_activation_state_store import (
    GenericCraftingActivationStateStore,
)
from ..state import (
    GenericCraftingActivationRecordStore,
    GenericCraftingActivationTransitionLifecycle,
)
from ..context import (
    GenericCraftingActivationContextProjector,
    GenericCraftingActivationEventCandidateValidator,
)
from .generic_crafting_activation_compatibility_installer import (
    GenericCraftingActivationCompatibilityInstaller,
)


class GenericCraftingActivationComponentGraph:
    def __init__(self, *, capacity: int, normalizer=None):
        if type(capacity) is not int or capacity < 0:
            raise ValueError("capacity must be a non-negative exact int")
        self._compatibility_installer = (
            GenericCraftingActivationCompatibilityInstaller()
        )
        self.context_input_validator = (
            GenericCraftingActivationEventCandidateValidator()
        )
        self.context_projector = GenericCraftingActivationContextProjector(
            normalizer
        )
        self.context_factory = GenericCraftingActivationContextFactory(
            normalizer,
            input_validator=self.context_input_validator,
            context_projector=self.context_projector,
        )
        self.binding_validator = GenericCraftingActivationBindingValidator()
        self.record_store = GenericCraftingActivationRecordStore(
            capacity=capacity
        )
        self.transition_lifecycle = (
            GenericCraftingActivationTransitionLifecycle(
                record_store=self.record_store,
                context_factory=self.context_factory,
                binding_validator=self.binding_validator,
            )
        )
        self.state_store = GenericCraftingActivationStateStore(
            capacity=capacity,
            context_factory=self.context_factory,
            binding_validator=self.binding_validator,
            record_store=self.record_store,
            transition_lifecycle=self.transition_lifecycle,
        )

    def install_compatibility_seams(self, owner) -> None:
        self._compatibility_installer.install(owner, self)


__all__ = ("GenericCraftingActivationComponentGraph",)
