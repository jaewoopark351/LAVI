#20260908_kpopmodder: Keep legacy crafting STATUS fallback from claiming a target it cannot verify.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.extension.minecraft_fabric_chatclef_command_feedback_facade import (
    MinecraftFabricChatClefCommandFeedbackFacade,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle import (
    CommandStatusQuery,
    CommandStatusQueryClassifier,
    CommandStatusRouteOwner,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleResponseRenderer,
)


class CommandStatusLegacyInspectorCompatibilityTests(unittest.TestCase):
    def test_facade_uses_legacy_inspector_only_without_a_target_constraint(self):
        calls = []
        marker = object()
        facade = MinecraftFabricChatClefCommandFeedbackFacade(
            SimpleNamespace(
                inspect_crafting_feedback_status=(
                    lambda target: calls.append(target) or marker
                )
            )
        )

        family_result = facade.inspect_status(
            CommandStatusQuery(
                requested_family="item_get",
                addressed=True,
            )
        )
        target_result = facade.inspect_status(
            CommandStatusQuery(
                requested_family="item_get",
                addressed=False,
                target_text="철",
            )
        )

        self.assertIs(marker, family_result)
        self.assertIsNone(target_result)
        self.assertEqual([None], calls)

    def test_route_owner_bypasses_prefixless_target_when_only_legacy_inspector_exists(self):
        calls = []
        owner = CommandStatusRouteOwner(
            extension=SimpleNamespace(
                inspect_crafting_feedback_status=(
                    lambda target: calls.append(target) or self.fail(
                        "an unverifiable target must not reach the legacy inspector"
                    )
                )
            ),
            live_proof_validator=lambda _proof, _event: True,
            classifier=CommandStatusQueryClassifier(),
            response_renderer=CommandLifecycleResponseRenderer(),
        )

        decision = owner.try_route(
            SimpleNamespace(text="철 만드는 중이야?"),
            object(),
        )

        self.assertIsNotNone(decision)
        self.assertFalse(decision.handled)
        self.assertEqual(
            "command_status_conversational_fallthrough",
            decision.reason,
        )
        self.assertEqual([], calls)


if __name__ == "__main__":
    unittest.main()
