#20260905_kpopmodder: Abandon a live generic-crafting receipt only.


class GenericCraftingReceiptCleanup:
    def __init__(self, activation_registry):
        self._activation_registry = activation_registry

    def abandon_if_live(self, receipt: object) -> None:
        if receipt is not None:
            self._activation_registry.abandon_if_live(receipt)


__all__ = ("GenericCraftingReceiptCleanup",)
