#20260913_kpopmodder: Guard Korean GOTO before ordinary translation, never submit here.
from __future__ import annotations

from typing import Callable

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.intent.navigation.goto import (
    GotoParseDecision,
    KoreanGotoCoordinateParser,
)

from .goto_input_binding import GotoInputBinding
from .goto_input_decision_factory import GotoInputDecisionFactory
from .goto_input_diagnostics import GotoInputDiagnostics


class GotoInputRouteGuard:
    def __init__(
        self,
        *,
        live_proof_validator: Callable[[object, object], bool] | None = None,
        diagnostics: GotoInputDiagnostics | None = None,
    ):
        self._parser = KoreanGotoCoordinateParser()
        self._live_proof_validator = live_proof_validator
        self._decisions = GotoInputDecisionFactory()
        self._diagnostics = diagnostics or GotoInputDiagnostics()

    def inspect(
        self,
        event: object,
        proof: object,
    ) -> tuple[GotoInputBinding, MinecraftChatClefInputRouteDecision | None]:
        # The event's original text must reach the parser before strip/normalization.
        parsed = self._parser.parse(getattr(event, "text", None))
        binding = GotoInputBinding(GotoInputBinding.applies_to(event), parsed)
        if not parsed.candidate:
            return binding, None

        decision = None
        reason = parsed.reason_code or parsed.decision.value
        if parsed.decision is GotoParseDecision.NONCOMMAND:
            decision = MinecraftChatClefInputRouteDecision.not_handled(reason)
        elif binding.automatic_input and (
            self._live_proof_validator is None
            or self._live_proof_validator(proof, event) is not True
        ):
            reason = "goto_live_proof_required"
            decision = self._decisions.reject(
                reason,
                "입력 확인이 끝난 Chat 또는 최종 마이크 문장으로 다시 말해줘.",
            )
        elif parsed.decision is GotoParseDecision.CLARIFY:
            decision = self._decisions.reject(reason, parsed.message)

        self._diagnostics.observe(
            event, boundary="input_guard", parsed=parsed, reason=reason
        )
        return binding, decision
