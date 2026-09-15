#20260915_kpopmodder: Korean confirmation wording is independent of pending-state ownership.
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)


class KoreanConfirmationResponseFactory:
    def pending(self, command_text, translation, ttl_seconds):
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason="minecraft_confirmation_required",
            response_text=f"‘{command_text.strip()}’ 요청을 실행하려면 {int(ttl_seconds)}초 안에 같은 입력 창이나 마이크에서 ‘확인’이라고 해 줘. 취소하려면 ‘취소’라고 해 줘.",
            translation=translation,
            route_kind="minecraft_command",
            publish_external_response=True,
        )

    def outcome(self, reason):
        messages = {
            "confirmation_cancelled": "확인 대기 중인 요청을 취소했어. 실행하지 않았어.",
            "confirmation_missing": "확인 대기 중인 요청이 없어. 실행할 명령을 다시 말해 줘.",
            "confirmation_expired": "확인 시간이 지났어. 실행할 명령을 다시 말해 줘.",
            "minecraft_command_busy": "다른 작업을 실행 중이라 확인한 요청은 실행하지 않았어.",
        }
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason=reason,
            response_text=messages.get(
                reason,
                "요청의 입력 출처나 연결 상태를 다시 확인할 수 없어 실행하지 않았어. 명령을 다시 말해 줘.",
            ),
            route_kind="minecraft_command",
            publish_external_response=True,
        )
