#20260913_kpopmodder: Build GOTO input rejections without item or H5 ownership.
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import (
    ChatClefIntentStatus,
)
from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import (
    ChatClefTranslationResultDTO,
)


class GotoInputDecisionFactory:
    def reject(
        self,
        reason_code: str,
        message: str = "좌표 이동 명령을 실행하지 않았어. X, Y, Z 좌표 세 개를 다시 알려줘.",
    ) -> MinecraftChatClefInputRouteDecision:
        translation = ChatClefTranslationResultDTO.rejected(
            ChatClefIntentStatus.INVALID,
            reason_code,
            message,
            data={"goto_input_rejection": True},
        ).to_dict()
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason=reason_code,
            response_text=message,
            result={
                "ok": False,
                "error": reason_code,
                "message": message,
                "details": {"reason_code": reason_code},
            },
            translation=translation,
            route_kind="minecraft_command",
        )
