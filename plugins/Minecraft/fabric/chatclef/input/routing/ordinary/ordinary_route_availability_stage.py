#20260905_kpopmodder: Isolate ordinary-route extension and boundary availability.
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)


class OrdinaryRouteAvailabilityStage:
    def __init__(
        self,
        *,
        extension,
        translation_boundary,
        submission_boundary,
        router_logger,
    ):
        self._extension = extension
        self._translation_boundary = translation_boundary
        self._submission_boundary = submission_boundary
        self._router_logger = router_logger

    def rejection(self) -> MinecraftChatClefInputRouteDecision | None:
        if self._extension is None:
            self._router_logger.log("route skipped: extension unavailable")
            return MinecraftChatClefInputRouteDecision.not_handled(
                "extension_unavailable"
            )
        if not self._translation_boundary.is_available(
            self._extension
        ) or not self._submission_boundary.is_available(self._extension):
            self._router_logger.log(
                "route skipped: single-pass submission boundary unavailable"
            )
            return MinecraftChatClefInputRouteDecision.not_handled(
                "handler_unavailable"
            )
        return None


__all__ = ("OrdinaryRouteAvailabilityStage",)
