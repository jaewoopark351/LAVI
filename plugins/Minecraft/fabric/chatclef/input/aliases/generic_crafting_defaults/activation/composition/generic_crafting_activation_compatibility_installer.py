#20260905_kpopmodder: Install activation-registry compatibility seams separately.


class GenericCraftingActivationCompatibilityInstaller:
    def install(self, owner, graph) -> None:
        owner._context_input_validator = graph.context_input_validator
        owner._context_projector = graph.context_projector
        owner._context_factory = graph.context_factory
        owner._binding_validator = graph.binding_validator
        owner._record_store = graph.record_store
        owner._transition_lifecycle = graph.transition_lifecycle
        owner._state_store = graph.state_store
        owner._capacity = graph.state_store.capacity
        owner._normalizer = graph.context_factory.normalizer
        owner._lock = graph.state_store.lock
        owner._registry_token = graph.state_store.registry_token
        owner._records = graph.state_store.records


__all__ = ("GenericCraftingActivationCompatibilityInstaller",)
