#20260907_kpopmodder: Represent one deterministic read-only crafting status query.
from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class CraftingStatusQuery:
    target_item: str | None


__all__ = ("CraftingStatusQuery",)
