#20260905_kpopmodder: Own validated non-item command compilation.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import (
    ChatClefTranslationResultDTO,
)


class ChatClefNonItemTranslationStage:
    def translate(self, intent: object, compiler: object) -> ChatClefTranslationResultDTO:
        #20260915_kpopmodder: Name resolution is a Python boundary, not a Java search or LLM fallback.
        from ...chatclef_intent_type import ChatClefIntentType
        if intent.intent_type is ChatClefIntentType.FIND:
            return self._translate_find(intent, compiler)
        command = compiler.compile(intent)
        return ChatClefTranslationResultDTO.validated(command=command, intent=intent)

    @staticmethod
    def _translate_find(intent, compiler):
        from ...chatclef_intent_status import ChatClefIntentStatus
        from ...navigation.find.find_target_resolver import FindTargetResolutionError
        try:
            resolved = compiler.resolve_find(intent)
        except FindTargetResolutionError as error:
            status = {"find_unknown_name": ChatClefIntentStatus.UNKNOWN,
                      "find_ambiguous_name": ChatClefIntentStatus.AMBIGUOUS}.get(error.code, ChatClefIntentStatus.INVALID)
            message = "대상 이름을 해석하지 못했어. 정확한 이름이나 종류와 레지스트리 ID를 지정해 줘."
            if error.code == "find_ambiguous_name":
                message = "같은 이름의 대상이 여러 종류야. 종류와 ID를 지정해 줘. 후보: " + ", ".join(error.suggestions)
            elif error.code == "find_invalid_registry_id":
                message = "레지스트리 ID 형식이 잘못됐어. 예: minecraft:iron_golem."
            return ChatClefTranslationResultDTO.rejected(status, error.code, message, intent,
                {"find_resolution": {"code": error.code, "suggestions": list(error.suggestions),
                    **dict(compiler.find_resolver.repository.diagnostics)}})
        return ChatClefTranslationResultDTO.validated(
            command=resolved.request.compile(), intent=intent,
            data={"find_resolution": {"requested_kind": intent.slots["kind"],
                  "query": intent.slots["query"], "kind": resolved.request.kind,
                  "registry_id": resolved.request.query, "mode": resolved.request.mode,
                  "label": resolved.label, "source": resolved.source,
                  **dict(compiler.find_resolver.repository.diagnostics)}},
        )


__all__ = ("ChatClefNonItemTranslationStage",)
