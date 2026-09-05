#20260905_kpopmodder: Render Feature-B submission and rejection wording in isolation.
from __future__ import annotations

from typing import Any, Mapping


class GenericCraftingDefaultsResponseRenderer:
    _QUANTITY_REASONS = frozenset(
        {
            "generic_crafting_multiple_quantity_tokens",
            "generic_crafting_non_ascii_decimal_quantity",
            "generic_crafting_invalid_quantity",
        }
    )

    def render_submitted(
        self,
        translation: Mapping[str, Any],
        result: Mapping[str, Any],
    ) -> str | None:
        if result.get("ok") is not True:
            return None
        status = self._result_status(result)
        if status not in {"accepted", "running"}:
            return None
        return f"[Minecraft] {self._item_label(translation)}를 준비하도록 명령했어요."

    def render_rejection(self, reason: object, message: object) -> str:
        reason_text = str(reason or "").strip()
        message_text = str(message or reason_text).strip()
        if reason_text in self._QUANTITY_REASONS:
            return (
                "[Minecraft] 제작 수량을 이해하지 못해 명령을 보내지 않았어요: "
                f"{message_text}"
            )
        return (
            "[Minecraft] 제작 요청을 안전하게 확인하지 못해 명령을 보내지 않았어요: "
            f"{message_text}"
        )

    def _item_label(self, translation: Mapping[str, Any]) -> str:
        intent = translation.get("intent")
        if not isinstance(intent, Mapping):
            return "아이템"
        item_phrase = str(intent.get("item_phrase") or "").strip() or "아이템"
        quantity = intent.get("quantity")
        if type(quantity) is int and quantity > 0:
            return f"{item_phrase} {quantity}개"
        return item_phrase

    def _result_status(self, result: Mapping[str, Any]) -> str:
        status = result.get("status")
        if isinstance(status, Mapping):
            return str(status.get("status") or "").strip().lower()
        return str(status or "").strip().lower()
