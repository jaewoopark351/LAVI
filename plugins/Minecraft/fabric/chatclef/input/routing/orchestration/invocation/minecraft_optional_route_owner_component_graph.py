#20260905_kpopmodder: Assemble optional route-owner invocation components.
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.invocation.minecraft_optional_route_owner_call import MinecraftOptionalRouteOwnerCall
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.invocation.minecraft_optional_route_owner_failure_converter import MinecraftOptionalRouteOwnerFailureConverter
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.invocation.validation import (
    MinecraftOptionalRouteOwnerResultValidator,
    MinecraftOptionalRouteOwnerShapeValidator,
)


class MinecraftOptionalRouteOwnerComponentGraph:
    def __init__(self, failure_handler) -> None:
        self.shape_validator = MinecraftOptionalRouteOwnerShapeValidator()
        self.call = MinecraftOptionalRouteOwnerCall()
        self.result_validator = MinecraftOptionalRouteOwnerResultValidator()
        self.failure_converter = MinecraftOptionalRouteOwnerFailureConverter(
            failure_handler
        )


__all__ = ("MinecraftOptionalRouteOwnerComponentGraph",)
