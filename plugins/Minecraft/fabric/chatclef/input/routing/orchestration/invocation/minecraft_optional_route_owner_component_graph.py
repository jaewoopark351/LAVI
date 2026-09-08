#20260905_kpopmodder: Assemble optional route-owner invocation components.
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.invocation.minecraft_optional_route_owner_call import MinecraftOptionalRouteOwnerCall
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.invocation.minecraft_optional_route_owner_failure_converter import MinecraftOptionalRouteOwnerFailureConverter
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.invocation.validation import (
    MinecraftOptionalRouteOwnerResultValidator,
    MinecraftOptionalRouteOwnerShapeValidator,
)
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.invocation.publication import (
    AcknowledgedOptionalRouteResultGuard,
)


class MinecraftOptionalRouteOwnerComponentGraph:
    def __init__(
        self,
        failure_handler,
        *,
        publication_custody_policy=None,
        emergency_decision=None,
    ) -> None:
        self.shape_validator = MinecraftOptionalRouteOwnerShapeValidator()
        self.call = MinecraftOptionalRouteOwnerCall()
        self.result_validator = MinecraftOptionalRouteOwnerResultValidator()
        self.result_guard = AcknowledgedOptionalRouteResultGuard(
            custody_policy=publication_custody_policy,
            emergency_decision=emergency_decision,
        )
        self.failure_converter = MinecraftOptionalRouteOwnerFailureConverter(
            failure_handler
        )


__all__ = ("MinecraftOptionalRouteOwnerComponentGraph",)
