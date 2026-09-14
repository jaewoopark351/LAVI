#20260914_kpopmodder: Own deterministic FIND translation, rejection, and safe decision logs.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.diagnostics import FabricChatClefDiagnostics
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import ChatClefIntentDTO
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_status import ChatClefIntentStatus
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import ChatClefIntentType

from .find_command_compiler import FindCommandCompiler
from .find_input_parser import FindInputParser
from .find_target_resolver import FindTargetResolver


class FindTranslationService:
    def __init__(self, *, catalog_provider=None, diagnostics=None):
        self._catalog_provider = catalog_provider or (lambda: None)
        self._diagnostics = diagnostics or FabricChatClefDiagnostics()
        self._parser = FindInputParser()
        self._resolver = FindTargetResolver()
        self._compiler = FindCommandCompiler()

    def translate_if_candidate(self, text: object):
        from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import ChatClefTranslationResultDTO
        request = self._parser.parse(text)
        if not request.candidate:
            return None
        if request.reason:
            return self._reject(request.reason)
        if request.target_kind not in {"", "entity", "block", "item", "player"} or request.mode not in {"report", "approach"}:
            return self._reject("invalid_find_kind_or_mode")
        snapshot = None if request.target_kind == "player" else self._catalog_provider()
        reason, candidates = self._resolver.resolve(request, snapshot)
        if reason != "validated":
            return self._reject(reason, candidates)
        kind, target, label = candidates[0]
        if kind == "item" and target == "minecraft:air":
            return self._reject("invalid_find_target")
        if kind == "item" and request.mode == "approach":
            return self._reject("item_find_approach_unsupported")
        intent = ChatClefIntentDTO(intent_type=ChatClefIntentType.FIND, original_text=str(text),
            source="rule", slots={"target_kind": kind, "target_phrase": request.target_phrase, "mode": request.mode})
        data = {"find_display_label": label}
        if kind != "player":
            data.update(find_catalog_digest=snapshot.catalog_digest, find_resource_generation=snapshot.resource_generation,
                        find_session_id=snapshot.session_id, find_connection_generation=snapshot.connection_generation)
        command = self._compiler.compile(intent, target)
        self._log("resolved", kind=kind, mode=request.mode, target="player_literal" if kind == "player" else target,
                  generation=data.get("find_connection_generation", "unbound"))
        return ChatClefTranslationResultDTO.validated(command, intent, resolved_target=target, data=data)

    def _reject(self, reason, candidates=()):
        from plugins.Minecraft.fabric.chatclef.intent.chatclef_translation_result_dto import ChatClefTranslationResultDTO
        from plugins.Minecraft.fabric.chatclef.response.find import KoreanFindRejectionRenderer
        status = ChatClefIntentStatus.AMBIGUOUS if reason == "find_target_ambiguous" else ChatClefIntentStatus.UNSUPPORTED
        self._log("rejected", reason=reason, candidate_count=len(candidates))
        return ChatClefTranslationResultDTO.rejected(status, reason, KoreanFindRejectionRenderer.render(reason),
            data={"find_rejection": True, "candidates": [{"target_kind": kind, "canonical_target_id": target, "label": label}
                  for kind, target, label in candidates]})

    def _log(self, event, **fields):
        try:
            values = " ".join(f"{key}={value}" for key, value in sorted(fields.items()))
            self._diagnostics.info(f"FIND_INPUT event={event} {values}")
        except Exception:
            pass
