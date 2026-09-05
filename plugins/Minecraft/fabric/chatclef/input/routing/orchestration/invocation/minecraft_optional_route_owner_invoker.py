#20260905_kpopmodder: Preserve optional route-owner invocation as a thin facade.
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)

from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.invocation.minecraft_optional_route_owner_component_graph import MinecraftOptionalRouteOwnerComponentGraph


class MinecraftOptionalRouteOwnerInvoker:
    def __init__(self, failure_handler):
        self._failure_handler = failure_handler
        self._component_graph = MinecraftOptionalRouteOwnerComponentGraph(
            failure_handler
        )
        self._shape_validator = self._component_graph.shape_validator
        self._call = self._component_graph.call
        self._result_validator = self._component_graph.result_validator
        self._failure_converter = self._component_graph.failure_converter

    def invoke(
        self,
        owner: object,
        event: object,
        korean_eligibility_proof: object,
        failure_reason: str,
    ) -> MinecraftChatClefInputRouteDecision | None:
        try:
            try_route = self._shape_validator.resolve(owner)
        except Exception as error:
            return self._failure_converter.convert(failure_reason, error)
        if try_route is None:
            return None
        try:
            raw_decision = self._call.invoke(
                try_route,
                event,
                korean_eligibility_proof,
            )
            return self._result_validator.validate(raw_decision)
        except Exception as error:
            return self._failure_converter.convert(failure_reason, error)


__all__ = ("MinecraftOptionalRouteOwnerInvoker",)
