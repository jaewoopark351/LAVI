#20260905_kpopmodder: Close generic-crafting dispatch records only.


class GenericCraftingDispatchCloseLifecycle:
    def __init__(self, activation_registry):
        self._activation_registry = activation_registry

    def close_dispatch(self, korean_eligibility_proof: object) -> None:
        self._activation_registry.close_dispatch(korean_eligibility_proof)


__all__ = ("GenericCraftingDispatchCloseLifecycle",)
