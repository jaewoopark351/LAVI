#20260905_kpopmodder: Build legacy H5 auto-deposit trust input-rejection decisions.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)


class AutoDepositTrustRouteDecisionFactory:
    def input_rejection(
        self,
        reason_code: str,
        message: str,
    ) -> MinecraftChatClefInputRouteDecision:
        reason = str(reason_code or "auto_deposit_trust_input_internal_error")
        text = str(message or reason).strip()
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason=reason,
            response_text=(
                "[Minecraft] 자동 보관 대상 등록 명령을 실행하지 않았어요: "
                f"{text}"
            ),
            result={
                "ok": False,
                "error": reason,
                "message": text,
                "details": {"reason_code": reason},
            },
        )
