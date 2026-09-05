#20260905_kpopmodder: Own route failure containment and receipt-finalization order.
from __future__ import annotations

from typing import Callable


class GenericCraftingRouteExecutionLifecycle:
    def __init__(self, *, failure_handler, receipt_cleanup):
        self._failure_handler = failure_handler
        self._receipt_cleanup = receipt_cleanup

    def execute(self, operation: Callable[[Callable[[object], object]], object]):
        receipt_holder = [None]

        def capture_receipt(receipt: object) -> object:
            receipt_holder[0] = receipt
            return receipt

        try:
            return operation(capture_receipt)
        except Exception as error:
            return self._failure_handler.handle(error)
        finally:
            self._receipt_cleanup.abandon_if_live(receipt_holder[0])


__all__ = ("GenericCraftingRouteExecutionLifecycle",)
