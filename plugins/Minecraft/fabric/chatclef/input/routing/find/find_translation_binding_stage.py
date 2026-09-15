#20260914_kpopmodder: Bind FIND to the original trusted utterance after translation, under the existing route lock.
from __future__ import annotations

import json
from typing import Mapping
from plugins.Minecraft.fabric.chatclef.intent.chatclef_command_compiler import ChatClefCommandCompiler
from plugins.Minecraft.fabric.chatclef.intent.navigation.find import KoreanFindRuleParser
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import ChatClefIntentType
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import ChatClefIntentStatus
from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import ChatClefTranslationResultDTO
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import MinecraftChatClefInputRouteDecision


class FindTranslationBindingStage:
    def __init__(self, *, live_proof_validator, log_callback=None):
        self._proof = live_proof_validator
        self._log = log_callback
        self._compiler = ChatClefCommandCompiler()

    def inspect(self, *, event, proof, translation):
        parsed = KoreanFindRuleParser().parse(getattr(event, "text", ""))
        intent = translation.get("intent", {}) if isinstance(translation, Mapping) else {}
        translated_find = isinstance(intent, Mapping) and intent.get("intent_type") == "find"
        command = translation.get("command") if isinstance(translation, Mapping) else None
        translated_find = translated_find or (isinstance(command, str) and command.lstrip("@").startswith("find "))
        if parsed is None and not translated_find:
            return None
        if isinstance(translation, Mapping) and translation.get("executable") is not True:
            snapshot = translation.get("data", {}).get("find_resolution", {}) if isinstance(translation.get("data"), Mapping) else {}
            #20260915_kpopmodder: A parsed FIND with an unknown name is owned here, not generic item rejection/LLM fallthrough.
            if parsed is not None and parsed.intent_type is ChatClefIntentType.FIND:
                rejection = self._reject_unresolved(event, proof, parsed)
                evidence = rejection.translation.get("data", {}).get("find_resolution", {})
                self._emit(event, rejection.reason, {"requested_kind": parsed.slots["kind"],
                    "query": parsed.slots["query"], **evidence})
                return rejection
            self._emit(event, str(translation.get("reason_code", "find_rejected")), snapshot)
            return None  # Existing guards retain ownership of non-FIND/unsafe utterances.
        reason = "original_find_matched"
        expected_command = None
        snapshot = {}
        if parsed is not None and parsed.intent_type is ChatClefIntentType.FIND:
            try:
                resolved = self._compiler.resolve_find(parsed)
                expected_command = resolved.request.compile()
                snapshot = {"requested_kind": parsed.slots["kind"], "query": parsed.slots["query"],
                            "kind": resolved.request.kind, "registry_id": resolved.request.query,
                            "mode": resolved.request.mode, "source": resolved.source,
                            **dict(self._compiler.find_resolver.repository.diagnostics)}
            except (ValueError, TypeError, OSError, KeyError):
                reason = "find_original_request_mismatch"
        if (parsed is None or parsed.intent_type is not ChatClefIntentType.FIND
                or not translated_find or not isinstance(intent, Mapping) or intent.get("source") != "rule"
                or intent.get("slots") != parsed.slots
                or intent.get("original_text") != parsed.original_text
                or expected_command is None or command != expected_command):
            reason = "find_original_request_mismatch"
        elif getattr(event, "source", None) in {"lavi_chat_ui", "voice_input_final"} and self._proof(proof, event) is not True:
            reason = "find_live_proof_expired"
        self._emit(event, reason, snapshot)
        if reason == "original_find_matched":
            return None
        message = "찾기 요청이 원래 입력과 일치하지 않거나 입력 확인이 만료돼서 실행하지 않았어."
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason=reason, response_text=message,
            result={"ok": False, "error": reason, "message": message, "details": {"reason_code": reason}},
            translation=ChatClefTranslationResultDTO.rejected(ChatClefIntentStatus.INVALID, reason, message).to_dict(),
            route_kind="minecraft_command",
        )

    def _reject_unresolved(self, event, proof, parsed):
        # Recompute from the original request; do not echo an external rejection message or candidates.
        from plugins.Minecraft.fabric.chatclef.intent.natural_language.policy.chatclef_non_item_translation_stage import ChatClefNonItemTranslationStage
        reason = "find_original_request_mismatch"
        message = "찾기 요청이 원래 입력과 일치하지 않아서 실행하지 않았어."
        translation = None
        if getattr(event, "source", None) in {"lavi_chat_ui", "voice_input_final"} and self._proof(proof, event) is not True:
            reason = "find_live_proof_expired"
            message = "입력 확인이 만료돼서 찾기를 실행하지 않았어."
        else:
            try:
                recomputed = ChatClefNonItemTranslationStage().translate(parsed, self._compiler)
                if not recomputed.executable:
                    reason, message = recomputed.reason_code, recomputed.message
                    translation = recomputed.to_dict()
            except (ValueError, TypeError, OSError, KeyError):
                reason = "find_name_resources_unavailable"
                message = "찾기 이름 사전을 읽지 못했어. JSON 파일과 경로를 확인해 줘."
        if translation is None:
            translation = ChatClefTranslationResultDTO.rejected(ChatClefIntentStatus.INVALID, reason, message, parsed).to_dict()
        return MinecraftChatClefInputRouteDecision.handled_result(
            reason=reason, response_text=message,
            result={"ok": False, "error": reason, "message": message, "details": {"reason_code": reason}},
            translation=translation, route_kind="minecraft_command",
        )

    def _emit(self, event, reason, snapshot):
        # One bounded event per admitted/rejected input; the route logger owns output.
        if self._log is not None:
            try:
                allowed = ("requested_kind", "query", "kind", "registry_id", "mode", "source",
                           "asset_status", "asset_warnings", "name_count", "alias_count", "minecraft_version", "vocabulary_sha256")
                fields = {}
                for key in allowed:
                    value = snapshot.get(key) if isinstance(snapshot, Mapping) else None
                    if type(value) is str:
                        fields[key] = value[:128]
                    elif type(value) is int and 0 <= value <= 2_147_483_647:
                        fields[key] = value
                self._log("[LAVI FIND Input] " + json.dumps({"event_id": str(getattr(event, "event_id", ""))[:128],
                    "reason": str(reason)[:64], **fields}, ensure_ascii=False, separators=(",", ":")))
            except Exception:
                pass
