#20260907_kpopmodder: Render FOOD and MEAT values as hunger units, never item counts.
from __future__ import annotations


class KoreanCommandHungerGoalRenderer:
    def render(self, descriptor: object, family: str) -> str:
        units = getattr(descriptor, "requested_count", None)
        amount = f" {units}만큼" if type(units) is int and units >= 1 else ""
        noun = "음식을" if family == "food_acquisition" else "고기를"
        return f"허기를{amount} 채울 {noun}"


__all__ = ("KoreanCommandHungerGoalRenderer",)
