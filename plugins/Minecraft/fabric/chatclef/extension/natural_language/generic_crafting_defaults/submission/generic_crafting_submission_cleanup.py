#20260905_kpopmodder: Finalize generic-crafting activation cleanup only.


class GenericCraftingSubmissionCleanup:
    def __init__(self, activation_lifecycle):
        self._activation_lifecycle = activation_lifecycle

    def finalize(self, activation_receipt: object) -> None:
        self._activation_lifecycle.abandon_if_live(activation_receipt)


__all__ = ("GenericCraftingSubmissionCleanup",)
