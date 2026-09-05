#20260905_kpopmodder: Assemble focused submission route decision factories.
from plugins.Minecraft.fabric.chatclef.input.routing.decisions.submission.minecraft_precheck_route_decision_factory import MinecraftPrecheckRouteDecisionFactory
from plugins.Minecraft.fabric.chatclef.input.routing.decisions.submission.minecraft_reconciled_route_decision_factory import MinecraftReconciledRouteDecisionFactory
from plugins.Minecraft.fabric.chatclef.input.routing.decisions.submission.minecraft_submitted_route_decision_factory import MinecraftSubmittedRouteDecisionFactory


class MinecraftSubmissionRouteDecisionComponentGraph:
    def __init__(self, response_renderer, status_classifier) -> None:
        self.reconciliation_factory = MinecraftReconciledRouteDecisionFactory(
            response_renderer
        )
        self.precheck_factory = MinecraftPrecheckRouteDecisionFactory(
            response_renderer
        )
        self.submitted_factory = MinecraftSubmittedRouteDecisionFactory(
            response_renderer,
            status_classifier,
        )


__all__ = ("MinecraftSubmissionRouteDecisionComponentGraph",)
