#20260905_kpopmodder: Assemble route-decision collaborators without owning behavior.
from plugins.Minecraft.fabric.chatclef.response import ChatClefCommandResponseRenderer

from plugins.Minecraft.fabric.chatclef.input.routing.decisions.auto_deposit_trust_route_decision_factory import (
    AutoDepositTrustRouteDecisionFactory,
)
from plugins.Minecraft.fabric.chatclef.input.routing.decisions.generic_crafting_defaults_route_decision_factory import (
    GenericCraftingDefaultsRouteDecisionFactory,
)
from plugins.Minecraft.fabric.chatclef.input.routing.decisions.item_command_route_decision_factory import ItemCommandRouteDecisionFactory
from plugins.Minecraft.fabric.chatclef.input.routing.decisions.minecraft_submission_result_status_classifier import (
    MinecraftSubmissionResultStatusClassifier,
)
from plugins.Minecraft.fabric.chatclef.input.routing.decisions.minecraft_submission_route_decision_factory import (
    MinecraftSubmissionRouteDecisionFactory,
)
from plugins.Minecraft.fabric.chatclef.input.routing.decisions.minecraft_translation_route_decision_factory import (
    MinecraftTranslationRouteDecisionFactory,
)


class MinecraftRouteDecisionComponentGraph:
    def __init__(
        self,
        response_renderer: ChatClefCommandResponseRenderer | None = None,
    ):
        self.response_renderer = response_renderer or ChatClefCommandResponseRenderer()
        self.status_classifier = MinecraftSubmissionResultStatusClassifier()
        self.submission_factory = MinecraftSubmissionRouteDecisionFactory(
            self.response_renderer,
            self.status_classifier,
        )
        self.translation_factory = MinecraftTranslationRouteDecisionFactory(
            self.response_renderer
        )
        self.item_command_factory = ItemCommandRouteDecisionFactory()
        self.auto_deposit_trust_factory = AutoDepositTrustRouteDecisionFactory()
        self.generic_crafting_defaults_factory = (
            GenericCraftingDefaultsRouteDecisionFactory()
        )
