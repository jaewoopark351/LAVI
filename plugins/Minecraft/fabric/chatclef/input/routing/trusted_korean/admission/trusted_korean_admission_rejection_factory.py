#20260905_kpopmodder: Build the fail-closed trusted admission rejection decision.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)


class TrustedKoreanAdmissionRejectionFactory:
    def create(self, reason: object) -> MinecraftChatClefInputRouteDecision:
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason=str(reason or "trusted_input_evidence_rejected"),
            response_text="",
            suppress_response=True,
        )


__all__ = ("TrustedKoreanAdmissionRejectionFactory",)
