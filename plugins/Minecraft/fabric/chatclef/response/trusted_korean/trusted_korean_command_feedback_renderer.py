#20260905_kpopmodder: Render Feature-A wording only after trusted Korean admission.
from __future__ import annotations


class TrustedKoreanCommandFeedbackRenderer:
    def render_disconnected(self) -> str:
        return "[Minecraft] 마인크래프트 연결이 끊겨 있어 명령을 보내지 못했어요."

    def render_busy(self) -> str:
        return (
            "[Minecraft] 다른 마인크래프트 명령을 실행 중이라 "
            "지금은 새 명령을 보낼 수 없어요."
        )

    def render_terminal_unknown(self) -> str:
        return (
            "[Minecraft] 실행 결과를 확인하지 못했어요. "
            "자동으로 다시 보내지 않아요."
        )

    def render_item_command_rejection(self, message: object) -> str:
        text = str(message or "").strip()
        if text:
            return f"[Minecraft] {text}"
        return "[Minecraft] 어떤 아이템을 준비할지 이해하지 못했어요."
