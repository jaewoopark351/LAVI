#20260905_kpopmodder: Isolate STOP, crafting, H5, and ordinary route ordering.
from __future__ import annotations

from dataclasses import replace

from plugins.Minecraft.fabric.chatclef.input.gating import (
    MinecraftChatClefInputRouteKind,
)
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)


class MinecraftInputRouteSequence:
    def __init__(
        self,
        *,
        input_event_normalizer,
        stop_control_route_owner,
        crafting_status_route_owner,
        generic_crafting_defaults_route_owner,
        auto_deposit_trust_route_coordinator,
        ordinary_command_route_coordinator,
    ) -> None:
        self._input_event_normalizer = input_event_normalizer
        self._stop_control_route_owner = stop_control_route_owner
        self._crafting_status_route_owner = crafting_status_route_owner
        self._generic_crafting_defaults_route_owner = (
            generic_crafting_defaults_route_owner
        )
        self._auto_deposit_trust_route_coordinator = (
            auto_deposit_trust_route_coordinator
        )
        self._ordinary_command_route_coordinator = (
            ordinary_command_route_coordinator
        )
    def route(
        self,
        value: object,
        *,
        korean_eligibility_proof: object = None,
        optional_route_callback,
        gate_inspection_callback,
    ) -> MinecraftChatClefInputRouteDecision:
        event = self._input_event_normalizer.normalize(value)
        command_text = event.text
        if not command_text:
            return MinecraftChatClefInputRouteDecision.not_handled("empty_input")
        if korean_eligibility_proof is not None:
            stop_decision = optional_route_callback(
                self._stop_control_route_owner,
                event,
                korean_eligibility_proof,
                "minecraft_stop_route_failed",
            )
            if stop_decision is not None:
                return replace(stop_decision, route_kind="stop_control")
            #20260907_kpopmodder: Route status after STOP and before crafting/busy handling.
            status_decision = optional_route_callback(
                self._crafting_status_route_owner,
                event,
                korean_eligibility_proof,
                "command_status_query_failed",
            )
            if status_decision is not None:
                # The owner selects either the generalized command-status
                # identity or the intentionally retained exact-craft identity.
                return status_decision
            crafting_decision = optional_route_callback(
                self._generic_crafting_defaults_route_owner,
                event,
                korean_eligibility_proof,
                "generic_crafting_defaults_failed",
            )
            if crafting_decision is not None:
                if crafting_decision.route_kind == "minecraft_chatclef":
                    return replace(
                        crafting_decision,
                        route_kind="generic_crafting_defaults",
                    )
                return crafting_decision
        gate_decision = gate_inspection_callback(command_text)
        if not gate_decision.consider:
            return MinecraftChatClefInputRouteDecision.not_handled(
                "no_minecraft_trigger"
            )
        if (
            gate_decision.route_kind
            is MinecraftChatClefInputRouteKind.H5_AUTO_DEPOSIT_TRUST
        ):
            return self._auto_deposit_trust_route_coordinator.route(event)

        decision = self._ordinary_command_route_coordinator.route(
            event,
            command_text.strip(),
            korean_eligibility_proof=korean_eligibility_proof,
        )
        if korean_eligibility_proof is not None:
            if decision.route_kind == "minecraft_chatclef":
                return replace(decision, route_kind="minecraft_command")
            return decision
        return decision

__all__ = ("MinecraftInputRouteSequence",)
