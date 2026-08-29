#20260827_kpopmodder: Render STORE_HOME only from validated operation-specific outcomes.
#20260828_kpopmodder: Require completed outer lifecycle evidence before rendering STORE_HOME success.
from __future__ import annotations

from typing import Any, Mapping

from plugins.Minecraft.fabric.chatclef.result.store_home import (
    StoreHomeTerminalPayload,
)


class StoreHomeCommandResponseRenderer:
    def matches_translation(self, translation: Mapping[str, Any]) -> bool:
        intent = translation.get("intent")
        if isinstance(intent, Mapping) and intent.get("intent_type") == "store_home":
            return True
        return str(translation.get("command") or "").strip() == "store_home"

    def matches_rejection(self, translation: Mapping[str, Any]) -> bool:
        return str(translation.get("reason_code") or "").startswith("store_home_")

    def render_submitted(
        self,
        result: Mapping[str, Any],
    ) -> str:
        result_status = self._result_status(result)
        if result_status in {"accepted", "running"}:
            return "[Minecraft] 집 보관 명령을 제출했어요."
        if self._public_enable_blocked(result):
            return "[Minecraft] 집 보관 자연어 명령은 아직 공개 검증 전이라 실행하지 않았어요."

        terminal = StoreHomeTerminalPayload.from_command_result(result)
        if terminal is None:
            return (
                "[Minecraft] 집 보관 작업의 종료 응답은 받았지만 "
                "실제 저장 결과를 확인하지 못했어요."
            )
        if terminal.result == "COMPLETED":
            if result_status != "completed":
                return (
                    "[Minecraft] 집 보관 작업의 종료 응답은 받았지만 "
                    "실제 저장 결과를 확인하지 못했어요."
                )
            if terminal.stored_items == 0:
                return "[Minecraft] 집 보관 완료: 저장할 아이템이 없었어요."
            return (
                "[Minecraft] 집 보관 완료: "
                f"아이템 {terminal.stored_items}개를 저장했어요."
            )
        if terminal.result == "PARTIAL_TRUSTED_CAPACITY_EXHAUSTED":
            return self._partial_message(
                terminal,
                "등록된 trusted storage에 더 이상 빈 공간이 없어요.",
            )
        if terminal.result == "PARTIAL_TRUSTED_DESTINATIONS_UNAVAILABLE":
            return self._partial_message(
                terminal,
                "남은 trusted 상자를 사용할 수 없어요.",
            )
        messages = {
            "NO_USABLE_TRUSTED_DESTINATION": (
                "[Minecraft] 사용할 수 있는 trusted 상자가 없어 저장하지 않았어요."
            ),
            "NO_TRUSTED_CAPACITY": (
                "[Minecraft] 등록된 trusted 상자에 빈 공간이 없어 저장하지 않았어요."
            ),
            "CURSOR_NOT_EMPTY": (
                "[Minecraft] cursor에 아이템이 있어 안전을 위해 집 보관을 시작하지 않았어요."
            ),
            "MANIFEST_STALE": (
                "[Minecraft] 인벤토리 변경을 감지해 집 보관을 안전하게 중단했어요."
            ),
            "CONTEXT_CHANGED": (
                "[Minecraft] world 또는 dimension 변경을 감지해 집 보관을 중단했어요."
            ),
            "TRANSFER_UNCONFIRMED": (
                "[Minecraft] trusted 상자로의 전송을 확인하지 못해 집 보관을 중단했어요."
            ),
            "INTERRUPTED": "[Minecraft] 다른 작업이 시작되어 집 보관이 중단됐어요.",
        }
        return messages[terminal.result]

    def render_rejection(self, translation: Mapping[str, Any]) -> str:
        message = str(translation.get("message") or "").strip()
        if message:
            return f"[Minecraft] {message}"
        return "[Minecraft] 집 보관 요청인지 확실하지 않아 저장하지 않았어요."

    def _partial_message(
        self,
        terminal: StoreHomeTerminalPayload,
        detail: str,
    ) -> str:
        if terminal.remaining_stacks == 0:
            remaining = (
                "최종 보고에서는 남은 저장 대상을 찾지 못했지만, "
                "이 값만으로 완료를 확인할 수는 없습니다."
            )
        else:
            remaining = (
                f"저장 대상 stack {terminal.remaining_stacks}개가 남았습니다."
            )
        return (
            "[Minecraft] 집 정리 부분 완료: "
            f"아이템 {terminal.stored_items}개를 저장했고 "
            f"{remaining} {detail}"
        )

    def _public_enable_blocked(self, result: Mapping[str, Any]) -> bool:
        details = result.get("details")
        return isinstance(details, Mapping) and (
            details.get("public_korean_enabled") is False
            and details.get("blocked_command") == "store_home"
        )

    def _result_status(self, result: Mapping[str, Any]) -> str:
        status = result.get("status")
        if isinstance(status, Mapping):
            return str(status.get("status") or "").strip().lower()
        return str(status or "").strip().lower()
