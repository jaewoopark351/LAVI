#20260908_kpopmodder: Render only count-bearing wording from a frozen STORE_HOME evidence projection.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.result.store_home import (
    StoreHomeTerminalPayload,
)


class KoreanStoreHomeTerminalRenderer:
    def render(self, projection: object) -> str | None:
        if type(projection) is not StoreHomeTerminalPayload:
            return None
        stored_items = projection.stored_items
        if (
            projection.result != "COMPLETED"
            or projection.goal_satisfied is not True
            or type(projection.remaining_stacks) is not int
            or projection.remaining_stacks != 0
            or type(stored_items) is not int
            or stored_items < 0
        ):
            return None
        if stored_items == 0:
            return "집에 정리할 아이템이 없었어"
        if stored_items > 0:
            return f"아이템 {stored_items}개를 집에 정리했어"
        return None


__all__ = ("KoreanStoreHomeTerminalRenderer",)
