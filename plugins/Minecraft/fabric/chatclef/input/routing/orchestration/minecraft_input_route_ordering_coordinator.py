#20260905_kpopmodder: Preserve route-ordering APIs as a thin facade.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)

from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.minecraft_input_route_ordering_component_graph import MinecraftInputRouteOrderingComponentGraph


class MinecraftInputRouteOrderingCoordinator:
    def __init__(
        self,
        *,
        input_event_normalizer,
        intent_gate,
        stop_control_route_owner,
        crafting_status_route_owner,
        generic_crafting_defaults_route_owner,
        auto_deposit_trust_route_coordinator,
        ordinary_command_route_coordinator,
        failure_handler,
    ):
        self._input_event_normalizer = input_event_normalizer
        self._intent_gate = intent_gate
        self._stop_control_route_owner = stop_control_route_owner
        #20260907_kpopmodder: Keep read-only crafting status distinct from later busy routing.
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
        self._failure_handler = failure_handler
        self._component_graph = MinecraftInputRouteOrderingComponentGraph(
            input_event_normalizer=input_event_normalizer,
            intent_gate=intent_gate,
            stop_control_route_owner=stop_control_route_owner,
            crafting_status_route_owner=crafting_status_route_owner,
            generic_crafting_defaults_route_owner=(
                generic_crafting_defaults_route_owner
            ),
            auto_deposit_trust_route_coordinator=(
                auto_deposit_trust_route_coordinator
            ),
            ordinary_command_route_coordinator=(
                ordinary_command_route_coordinator
            ),
            failure_handler=failure_handler,
        )
        self._input_gate_inspector = self._component_graph.input_gate_inspector
        self._optional_route_owner_invoker = (
            self._component_graph.optional_route_owner_invoker
        )
        self._crafting_dispatch_cleanup = (
            self._component_graph.crafting_dispatch_cleanup
        )
        self._route_sequence = self._component_graph.route_sequence

    def route(
        self,
        value: object,
        *,
        korean_eligibility_proof: object = None,
    ) -> MinecraftChatClefInputRouteDecision:
        return self._route_sequence.route(
            value,
            korean_eligibility_proof=korean_eligibility_proof,
            optional_route_callback=self.try_optional_route_owner,
            gate_inspection_callback=self.inspect_gate,
        )

    def close_generic_crafting_defaults_dispatch(
        self,
        korean_eligibility_proof: object,
    ) -> None:
        self._crafting_dispatch_cleanup.close(korean_eligibility_proof)

    def try_optional_route_owner(
        self,
        owner: object,
        event: object,
        korean_eligibility_proof: object,
        failure_reason: str,
    ) -> MinecraftChatClefInputRouteDecision | None:
        return self._optional_route_owner_invoker.invoke(
            owner,
            event,
            korean_eligibility_proof,
            failure_reason,
        )

    def inspect_gate(self, text: str):
        return self._input_gate_inspector.inspect(text)


__all__ = ("MinecraftInputRouteOrderingCoordinator",)
