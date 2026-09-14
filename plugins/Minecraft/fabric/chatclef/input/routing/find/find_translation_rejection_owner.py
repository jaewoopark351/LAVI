#20260914_kpopmodder: Keep invalid FIND candidates owned before item rejection or conversation fallback.
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import MinecraftChatClefInputRouteDecision
from plugins.Minecraft.fabric.chatclef.intent.find.find_input_parser import FindInputParser
from plugins.Minecraft.fabric.chatclef.response.find import KoreanFindRejectionRenderer


class FindTranslationRejectionOwner:
    def __init__(self, *, live_proof_validator):
        self._live_proof_validator = live_proof_validator
        self._parser = FindInputParser()

    def inspect(self, *, event, translation, proof):
        if not self._parser.is_candidate(getattr(event, "text", None)) or translation.get("executable") is True:
            return None
        data = translation.get("data") or {}
        if data.get("find_rejection") is not True:
            return None
        if getattr(event, "source", None) in {"lavi_chat_ui", "voice_input_final"} and self._live_proof_validator(proof, event) is not True:
            return MinecraftChatClefInputRouteDecision(handled=True, reason="find_live_proof_expired", suppress_response=True)
        reason = translation.get("reason_code")
        return MinecraftChatClefInputRouteDecision.handled_result(reason="minecraft_find_translation_rejected",
            response_text=KoreanFindRejectionRenderer.render(reason),
            result={"ok": False, "error": reason, "details": {"find_owned_invalid": True}},
            translation=dict(translation))
