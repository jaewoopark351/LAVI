#20260905_kpopmodder: Lock the public route-decision factory as a thin facade.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.input.routing import (
    MinecraftChatClefRouteDecisionFactory,
)
from plugins.Minecraft.fabric.chatclef.input.routing.decisions import (
    AutoDepositTrustRouteDecisionFactory,
    GenericCraftingDefaultsRouteDecisionFactory,
    ItemCommandRouteDecisionFactory,
    MinecraftSubmissionResultStatusClassifier,
    MinecraftSubmissionRouteDecisionFactory,
    MinecraftTranslationRouteDecisionFactory,
)
from plugins.Minecraft.fabric.chatclef.response import ChatClefCommandResponseRenderer


class MinecraftChatClefRouteDecisionFactoryDelegationTests(unittest.TestCase):
    def test_component_graph_installs_focused_decision_builders(self):
        renderer = ChatClefCommandResponseRenderer()

        factory = MinecraftChatClefRouteDecisionFactory(renderer)

        self.assertIs(renderer, factory._response_renderer)
        self.assertIsInstance(
            factory._status_classifier,
            MinecraftSubmissionResultStatusClassifier,
        )
        self.assertIsInstance(
            factory._submission_factory,
            MinecraftSubmissionRouteDecisionFactory,
        )
        self.assertIsInstance(
            factory._translation_factory,
            MinecraftTranslationRouteDecisionFactory,
        )
        self.assertIsInstance(
            factory._item_command_factory,
            ItemCommandRouteDecisionFactory,
        )
        self.assertIsInstance(
            factory._auto_deposit_trust_factory,
            AutoDepositTrustRouteDecisionFactory,
        )
        self.assertIsInstance(
            factory._generic_crafting_defaults_factory,
            GenericCraftingDefaultsRouteDecisionFactory,
        )
        self.assertIs(
            renderer,
            factory._submission_factory._response_renderer,
        )
        self.assertIs(
            renderer,
            factory._translation_factory._response_renderer,
        )

    def test_compatibility_helpers_delegate_to_submission_collaborators(self):
        factory = MinecraftChatClefRouteDecisionFactory()
        translation = {"command": "get redstone 5"}
        result = {"ok": True, "status": {"status": "RUNNING"}}

        self.assertEqual("running", factory.result_status(result))
        self.assertEqual(
            factory._response_renderer.render_submitted(translation, result),
            factory._response_text(translation, result),
        )

    def test_feature_specific_rejections_keep_shared_response_text_empty(self):
        factory = MinecraftChatClefRouteDecisionFactory()

        item = factory.item_command_translation_rejection(
            {"status": "rejected"},
            "unknown_item",
            "어떤 아이템을 준비할지 이해하지 못했어요.",
        )
        crafting = factory.generic_crafting_defaults_rejection(
            "invalid_quantity",
            "invalid",
        )

        self.assertEqual("", item.response_text)
        self.assertEqual("", crafting.response_text)
        self.assertTrue(item.result["details"]["item_command_owned_invalid"])
        self.assertTrue(
            crafting.result["details"]["generic_crafting_defaults"]
        )


if __name__ == "__main__":
    unittest.main()
