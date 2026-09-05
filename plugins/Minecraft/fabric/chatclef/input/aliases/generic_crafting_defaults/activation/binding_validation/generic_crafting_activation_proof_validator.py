#20260905_kpopmodder: Own the bound generic-crafting eligibility-proof authority check.
from __future__ import annotations

from typing import Callable


class GenericCraftingActivationProofValidator:
    def __init__(self):
        self._validator: Callable[[object, object], bool] | None = None

    def bind(
        self,
        validator: Callable[[object, object], bool],
    ) -> None:
        if not callable(validator):
            raise TypeError("activation proof validator is required")
        self._validator = validator

    def is_valid(self, proof: object, event: object) -> bool:
        validator = self._validator
        if validator is None:
            return False
        try:
            return validator(proof, event) is True
        except Exception:
            return False

    @property
    def bound_validator(self):
        return self._validator


__all__ = ("GenericCraftingActivationProofValidator",)
