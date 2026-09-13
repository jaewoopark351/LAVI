#20260913_kpopmodder: Revalidate GOTO input binding after translation inside the existing lock.
from __future__ import annotations

from typing import Any, Callable, Mapping

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)

from .goto_input_binding import GotoInputBinding
from .goto_input_decision_factory import GotoInputDecisionFactory
from .goto_input_diagnostics import GotoInputDiagnostics
from .goto_translation_binding_validator import GotoTranslationBindingValidator


class GotoTranslationBindingStage:
    def __init__(
        self,
        *,
        live_proof_validator: Callable[[object, object], bool],
        diagnostics: GotoInputDiagnostics,
    ):
        self._live_proof_validator = live_proof_validator
        self._diagnostics = diagnostics
        self._validator = GotoTranslationBindingValidator()
        self._decisions = GotoInputDecisionFactory()

    def inspect(
        self,
        *,
        event: object,
        proof: object,
        binding: GotoInputBinding | None,
        translation: Mapping[str, Any],
    ) -> MinecraftChatClefInputRouteDecision | None:
        automatic_input = GotoInputBinding.applies_to(event)
        if not automatic_input:
            return None
        parsed = None if binding is None else binding.parse_result
        if binding is None:
            reason = "goto_input_binding_missing"
        elif not binding.automatic_input:
            reason = "goto_input_scope_mismatch"
        else:
            reason = self._validator.validate(binding, translation)
            if not parsed.executable and reason is None:
                return None
            if reason is None and self._live_proof_validator(proof, event) is not True:
                reason = "goto_live_proof_expired"
        self._diagnostics.observe(
            event,
            boundary="translation_binding",
            parsed=parsed,
            reason=reason or "original_xyz_matched",
        )
        return None if reason is None else self._decisions.reject(reason)
