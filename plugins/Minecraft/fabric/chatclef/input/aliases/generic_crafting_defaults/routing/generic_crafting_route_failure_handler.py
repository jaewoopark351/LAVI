#20260905_kpopmodder: Project generic-crafting route failures only.


class GenericCraftingRouteFailureHandler:
    def __init__(self, decision_factory):
        self._decision_factory = decision_factory

    def handle(self, error: Exception):
        return self._decision_factory.operation_failure(
            "generic_crafting_defaults_failed",
            error,
        )


__all__ = ("GenericCraftingRouteFailureHandler",)
