#20260914_kpopmodder: Bind FIND to the original trusted utterance after translation, under the existing route lock.
from __future__ import annotations

from typing import Mapping
from plugins.Minecraft.fabric.chatclef.intent.navigation.find import FindRequest, KoreanFindRuleParser
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import ChatClefIntentType
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import ChatClefIntentStatus
from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import ChatClefTranslationResultDTO
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import MinecraftChatClefInputRouteDecision


class FindTranslationBindingStage:
    def __init__(self, *, live_proof_validator, log_callback=None):
        self._proof = live_proof_validator
        self._log = log_callback

    def inspect(self, *, event, proof, translation):
        parsed = KoreanFindRuleParser().parse(getattr(event, "text", ""))
        intent = translation.get("intent", {}) if isinstance(translation, Mapping) else {}
        translated_find = isinstance(intent, Mapping) and intent.get("intent_type") == "find"
        command = translation.get("command") if isinstance(translation, Mapping) else None
        translated_find = translated_find or (isinstance(command, str) and command.lstrip("@").startswith("find "))
        if parsed is None and not translated_find:
            return None
        if isinstance(translation, Mapping) and translation.get("executable") is not True:
            return None  # The existing rejection stage owns a non-executable translation.
        reason = "original_find_matched"
        if (parsed is None or parsed.intent_type is not ChatClefIntentType.FIND
                or not translated_find or not isinstance(intent, Mapping) or intent.get("source") != "rule"
                or intent.get("slots") != parsed.slots
                or intent.get("original_text") != parsed.original_text
                or command != FindRequest.from_slots(parsed.slots).compile()):
            reason = "find_original_request_mismatch"
        elif getattr(event, "source", None) in {"lavi_chat_ui", "voice_input_final"} and self._proof(proof, event) is not True:
            reason = "find_live_proof_expired"
        if self._log is not None:
            try:
                self._log(f"[LAVI FIND Input] event_id={getattr(event, 'event_id', '')} reason={reason}")
            except Exception:
                pass
        if reason == "original_find_matched":
            return None
        message = "찾기 요청이 원래 입력과 일치하지 않거나 입력 확인이 만료돼서 실행하지 않았어."
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason=reason, response_text=message,
            result={"ok": False, "error": reason, "message": message, "details": {"reason_code": reason}},
            translation=ChatClefTranslationResultDTO.rejected(ChatClefIntentStatus.INVALID, reason, message).to_dict(),
            route_kind="minecraft_command",
        )
