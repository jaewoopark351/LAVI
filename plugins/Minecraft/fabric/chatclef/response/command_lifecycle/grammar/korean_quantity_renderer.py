#20260907_kpopmodder: Render bounded Korean item quantities without guessing absent slots.
from __future__ import annotations


class KoreanQuantityRenderer:
    def render(self, label: object, quantity: object, *, omit_one: bool = True) -> str:
        target = str(label or "").strip() or "요청한 아이템"
        if type(quantity) is not int or quantity < 1 or (omit_one and quantity == 1):
            return target
        return f"{target} {quantity}개"


__all__ = ("KoreanQuantityRenderer",)
