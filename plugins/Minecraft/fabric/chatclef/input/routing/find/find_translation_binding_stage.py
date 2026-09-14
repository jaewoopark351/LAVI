#20260914_kpopmodder: Prevent FIND fallback, expired proof, or stale catalog before submission.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import MinecraftChatClefInputRouteDecision
from plugins.Minecraft.fabric.chatclef.intent.find.find_input_parser import FindInputParser
from plugins.Minecraft.fabric.chatclef.intent.find.find_target_resolver import FindTargetResolver
from plugins.Minecraft.fabric.chatclef.result.find import FindCommandBinding
from plugins.Minecraft.fabric.chatclef.transport.find_catalog import FindCatalogSnapshot


class FindTranslationBindingStage:
    def __init__(self, *, extension, live_proof_validator, log_callback):
        self._extension = extension
        self._live_proof_validator = live_proof_validator
        self._log_callback = log_callback
        self._parser = FindInputParser()
        self._resolver = FindTargetResolver()

    def inspect(self, *, event, translation, proof):
        request = self._parser.parse(getattr(event, "text", None))
        intent = translation.get("intent") or {}
        is_find = intent.get("intent_type") == "find"
        if not request.candidate and not is_find:
            return None
        if translation.get("executable") is not True:
            return None
        reason = None
        binding = FindCommandBinding.from_translation(dict(translation))
        if not request.candidate or request.reason or not is_find or binding is None:
            reason = "find_original_request_mismatch"
        elif getattr(event, "source", None) in {"lavi_chat_ui", "voice_input_final"} and self._live_proof_validator(proof, event) is not True:
            reason = "find_live_proof_expired"
        else:
            getter = getattr(self._extension, "get_find_catalog_snapshot", None)
            snapshot = getter() if binding.target_kind != "player" and callable(getter) else None
            if binding.target_kind != "player" and (type(snapshot) is not FindCatalogSnapshot
                    or (snapshot.catalog_digest, snapshot.resource_generation, snapshot.session_id, snapshot.connection_generation)
                    != (binding.catalog_digest, binding.resource_generation, binding.session_id, binding.connection_generation)):
                reason = "find_catalog_stale"
            else:
                resolved, candidates = self._resolver.resolve(request, snapshot)
                if (resolved != "validated" or len(candidates) != 1
                        or (candidates[0][0], candidates[0][1], request.mode) != (binding.target_kind, binding.target, binding.mode)):
                    reason = "find_original_request_mismatch"
        try:
            event_id = str(getattr(event, "event_id", "unbound"))[:32]
            self._log_callback(f"FIND_INPUT boundary=submission_binding event_id={event_id} reason={reason or 'binding_matched'}")
        except Exception:
            pass
        if reason is None:
            return None
        return MinecraftChatClefInputRouteDecision.handled_result(reason=reason,
            response_text="대상 목록이나 요청 상태가 바뀌어서 찾기 명령은 보내지 않았어. 다시 요청해 줘.",
            result={"ok": False, "error": reason}, translation=dict(translation))
