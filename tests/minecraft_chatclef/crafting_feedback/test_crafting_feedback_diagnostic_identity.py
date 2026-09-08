#20260907_kpopmodder: Preserve Feature-A admission diagnostics for craft starts.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.input.diagnostics.projection import (
    MinecraftKoreanFeatureIdentityClassifier,
)
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.response.crafting_lifecycle import (
    CraftingLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.response.crafting_lifecycle.crafting_feedback_start_decision_decorator import (
    CraftingFeedbackStartDecisionDecorator,
)


class CraftingFeedbackDiagnosticIdentityTests(unittest.TestCase):
    def test_start_keeps_existing_command_route_and_feature_a_identity(self):
        for route_kind in ("minecraft_chatclef", "minecraft_command"):
            with self.subTest(route_kind=route_kind):
                ordinary = MinecraftChatClefInputRouteDecision.handled_result(
                    reason="minecraft_command_routed",
                    response_text="old",
                    route_kind=route_kind,
                )

                decorated = CraftingFeedbackStartDecisionDecorator(
                    CraftingLifecycleResponseRenderer()
                ).decorate(ordinary, start_claimed=True)
                identity = MinecraftKoreanFeatureIdentityClassifier().classify(
                    _Event(),
                    proof_issued=True,
                    route_kind=decorated.route_kind,
                    handled=decorated.handled,
                )

                self.assertEqual(route_kind, decorated.route_kind)
                self.assertEqual(
                    ("A", "minecraft_command_feedback_v1", "none"),
                    identity,
                )

    def test_status_query_is_intentionally_outside_existing_a_b_c_scopes(self):
        identity = MinecraftKoreanFeatureIdentityClassifier().classify(
            _Event(),
            proof_issued=True,
            route_kind="crafting_status_query",
            handled=True,
        )

        self.assertEqual(("none", "none", "none"), identity)


class _Event:
    text = "다이아 곡괭이 만들어줘"


if __name__ == "__main__":
    unittest.main()
