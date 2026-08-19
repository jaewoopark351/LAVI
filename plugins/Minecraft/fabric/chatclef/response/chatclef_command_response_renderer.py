#20260819_kpopmodder: Render Korean ChatClef route replies from verified Python evidence only.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.intent.chatclef_korean_display_name_repository import (
    ChatClefKoreanDisplayNameRepository,
)


class ChatClefCommandResponseRenderer:
    def __init__(
        self,
        display_names: ChatClefKoreanDisplayNameRepository | None = None,
    ):
        self._display_names = display_names or ChatClefKoreanDisplayNameRepository()

    def render_submitted(
        self,
        translation: Mapping[str, Any],
        result: Mapping[str, Any],
    ) -> str:
        result_status = self._result_status(result)
        message = self._text(result.get("message"))
        command_label = self._command_label(translation)

        if result.get("ok") is True:
            if result_status in {"accepted", "running"}:
                return f"[Minecraft] {command_label} 명령을 제출했어요."
            if result_status == "completed":
                if self._expected_effect_verified(result):
                    return f"[Minecraft] {command_label} 결과를 확인했어요."
                return (
                    f"[Minecraft] {command_label} 명령의 완료 응답을 받았어요. "
                    "아이템 증가는 별도로 확인해야 해요."
                )
            return f"[Minecraft] {command_label} 명령을 전달했어요."

        if result_status == "unknown":
            if message:
                return f"[Minecraft] 명령 결과를 확정하지 못했어요: {message}"
            return "[Minecraft] 명령 결과를 확정하지 못했어요. 자동으로 다시 보내지 않아요."
        if message:
            return f"[Minecraft] 명령을 제출하지 않았어요: {message}"
        return "[Minecraft] 명령을 제출하지 않았어요."

    def render_translation_rejection(
        self,
        translation: Mapping[str, Any],
    ) -> str:
        reason = self._text(
            translation.get("reason_code")
            or translation.get("status")
            or "translation_rejected"
        )
        message = self._text(translation.get("message")) or reason
        return f"[Minecraft] 명령을 이해하지 못했어요: {message}"

    def render_precheck_rejection(self, reason: str, message: str) -> str:
        reason_text = self._text(reason)
        message_text = self._text(message)
        if reason_text == "minecraft_bridge_disconnected":
            return "[Minecraft] 마인크래프트 연결이 끊겨 있어요."
        if reason_text == "minecraft_command_busy":
            return "[Minecraft] 지금 다른 마인크래프트 작업을 하고 있어요."
        if message_text:
            return f"[Minecraft] 명령을 제출할 수 없어요: {message_text}"
        return "[Minecraft] 명령을 제출할 수 없어요."

    def render_operation_failure(self, reason: str, message: str) -> str:
        message_text = self._text(message)
        if message_text:
            return f"[Minecraft] 명령 처리 중 오류가 났어요: {message_text}"
        return f"[Minecraft] 명령 처리 중 오류가 났어요: {self._text(reason)}"

    def render_reconciled_without_submission(self) -> str:
        return (
            "[Minecraft] 이전 마인크래프트 명령의 종료 응답을 확인했어요. "
            "지금 입력한 명령은 보내지 않았으니, 계속 원하면 다시 보내 주세요."
        )

    def _command_label(self, translation: Mapping[str, Any]) -> str:
        intent = translation.get("intent")
        if isinstance(intent, Mapping) and intent.get("intent_type") in {
            "get_item",
            "equip_item",
            "deposit_item",
            "give_item",
        }:
            intent_type = self._text(intent.get("intent_type"))
            target = self._text(translation.get("resolved_target"))
            item_phrase = self._text(intent.get("item_phrase"))
            display = self._display_names.display_name(target, item_phrase or target)
            quantity = intent.get("quantity")
            if intent_type == "equip_item":
                return f"{display} 장착"
            if intent_type == "deposit_item":
                if type(quantity) is int and quantity > 0:
                    return f"{display} {quantity}개 보관"
                return f"{display} 보관"
            if intent_type == "give_item":
                player_name = self._text(intent.get("player_name"))
                recipient = f"{player_name}에게 " if player_name else ""
                if type(quantity) is int and quantity > 0:
                    return f"{recipient}{display} {quantity}개 전달"
                return f"{recipient}{display} 전달"
            if type(quantity) is int and quantity > 0:
                return f"{display} {quantity}개 수집"
            return f"{display} 수집"

        command = self._text(translation.get("command"))
        if command:
            return f"마인크래프트 {command}"
        return "마인크래프트"

    def _expected_effect_verified(self, result: Mapping[str, Any]) -> bool:
        details = result.get("details")
        if isinstance(details, Mapping):
            return details.get("expected_gameplay_effect_verified") is True
        status = result.get("status")
        if isinstance(status, Mapping):
            data = status.get("data")
            if isinstance(data, Mapping):
                return data.get("expected_gameplay_effect_verified") is True
        return False

    def _result_status(self, result: Mapping[str, Any]) -> str:
        status = result.get("status")
        if isinstance(status, Mapping):
            return self._text(status.get("status")).lower()
        return self._text(status).lower()

    def _text(self, value: object) -> str:
        return str(value or "").strip()
