#20260905_kpopmodder: Keep trusted Korean route responsibilities split by module.
#20260909_kpopmodder: Verify only exact trusted typed pre-submit busy decisions.
from __future__ import annotations

from collections.abc import Mapping

from input_core.input_event.contracts.lavi_input_event import LaviInputEvent
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)

from .contextual_busy_response_evaluation import (
    ContextualBusyResponseEvaluation,
)


class ContextualBusyResponseEvaluator:
    _SOURCE_TUPLES = frozenset(
        {
            ("lavi_chat_ui", "lavi_chat_ui", "chat_submit", True),
            ("voice_input_final", "VoiceInput", "final_transcript", True),
        }
    )
    _ROUTE_KINDS = frozenset(
        {"minecraft_command", "generic_crafting_defaults", "minecraft_chatclef"}
    )

    def is_candidate(self, decision: object) -> bool:
        return self._is_candidate(decision)

    def evaluate(
        self,
        decision: object,
        *,
        event: object,
        proof_is_live: object,
    ) -> ContextualBusyResponseEvaluation:
        try:
            return self._evaluate(
                decision,
                event=event,
                proof_is_live=proof_is_live,
            )
        except Exception:
            return ContextualBusyResponseEvaluation(
                candidate=self._is_candidate(decision),
                verified=False,
            )

    def _evaluate(
        self,
        decision: object,
        *,
        event: object,
        proof_is_live: object,
    ) -> ContextualBusyResponseEvaluation:
        candidate = self._is_candidate(decision)
        if not candidate:
            return ContextualBusyResponseEvaluation(False, False)
        result = decision.result
        source_tuple = (
            event.source,
            event.provider_id,
            event.event_kind,
            event.final,
        ) if type(event) is LaviInputEvent else None
        verified = (
            decision.handled is True
            and decision.route_kind in self._ROUTE_KINDS
            and type(proof_is_live) is bool
            and proof_is_live
            and source_tuple in self._SOURCE_TUPLES
            and isinstance(result, Mapping)
            and result.get("ok") is False
            and result.get("error") == "active_command"
        )
        return ContextualBusyResponseEvaluation(True, verified)

    @staticmethod
    def _is_candidate(decision: object) -> bool:
        try:
            return (
                type(decision) is MinecraftChatClefInputRouteDecision
                and decision.reason == "minecraft_command_busy"
            )
        except Exception:
            return False


__all__ = ("ContextualBusyResponseEvaluator",)
