#20260905_kpopmodder: Assemble route ordering helpers outside the public facade.
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.minecraft_generic_crafting_dispatch_cleanup import MinecraftGenericCraftingDispatchCleanup
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.minecraft_input_gate_inspector import MinecraftInputGateInspector
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.invocation import MinecraftOptionalRouteOwnerInvoker
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.minecraft_input_route_sequence import MinecraftInputRouteSequence


class MinecraftInputRouteOrderingComponentGraph:
    def __init__(
        self,
        *,
        input_event_normalizer,
        intent_gate,
        stop_control_route_owner,
        generic_crafting_defaults_route_owner,
        auto_deposit_trust_route_coordinator,
        ordinary_command_route_coordinator,
        failure_handler,
    ) -> None:
        self.input_gate_inspector = MinecraftInputGateInspector(intent_gate)
        self.optional_route_owner_invoker = MinecraftOptionalRouteOwnerInvoker(
            failure_handler
        )
        self.crafting_dispatch_cleanup = MinecraftGenericCraftingDispatchCleanup(
            generic_crafting_defaults_route_owner
        )
        self.route_sequence = MinecraftInputRouteSequence(
            input_event_normalizer=input_event_normalizer,
            stop_control_route_owner=stop_control_route_owner,
            generic_crafting_defaults_route_owner=(
                generic_crafting_defaults_route_owner
            ),
            auto_deposit_trust_route_coordinator=(
                auto_deposit_trust_route_coordinator
            ),
            ordinary_command_route_coordinator=(
                ordinary_command_route_coordinator
            ),
        )


__all__ = ("MinecraftInputRouteOrderingComponentGraph",)
