#20260905_kpopmodder: Guarantee extension submission cleanup only.
from __future__ import annotations

from typing import Callable


class GenericCraftingSubmissionExecutionLifecycle:
    def __init__(self, cleanup):
        self._cleanup = cleanup

    def execute(self, activation_receipt: object, operation: Callable[[], object]):
        try:
            return operation()
        finally:
            self._cleanup.finalize(activation_receipt)


__all__ = ("GenericCraftingSubmissionExecutionLifecycle",)
