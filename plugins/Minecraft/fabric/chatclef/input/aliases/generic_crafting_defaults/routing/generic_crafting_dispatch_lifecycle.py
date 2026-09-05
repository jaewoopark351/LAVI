#20260905_kpopmodder: Preserve Feature-B dispatch lifecycle APIs as a facade.
from __future__ import annotations

from .lifecycle import (
    GenericCraftingDispatchCloseLifecycle,
    GenericCraftingReceiptCleanup,
)


class GenericCraftingDispatchLifecycle:
    def __init__(
        self,
        activation_registry,
        *,
        receipt_cleanup=None,
        dispatch_close_lifecycle=None,
    ):
        self._activation_registry = activation_registry
        self._receipt_cleanup = (
            receipt_cleanup or GenericCraftingReceiptCleanup(activation_registry)
        )
        self._dispatch_close_lifecycle = (
            dispatch_close_lifecycle
            or GenericCraftingDispatchCloseLifecycle(activation_registry)
        )

    @property
    def activation_registry(self):
        return self._activation_registry

    def abandon_if_live(self, receipt: object) -> None:
        self._receipt_cleanup.abandon_if_live(receipt)

    def close_dispatch(self, korean_eligibility_proof: object) -> None:
        self._dispatch_close_lifecycle.close_dispatch(
            korean_eligibility_proof
        )


__all__ = ("GenericCraftingDispatchLifecycle",)
