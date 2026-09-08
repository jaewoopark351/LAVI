#20260908_kpopmodder: Verify bounded STATUS decision construction shapes.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.routing.decision import (
    COMMAND_STATUS_EMERGENCY_DECISION,
    CommandStatusRouteDecisionFactory,
)


class CommandStatusRouteDecisionFactoryTests(unittest.TestCase):
    def setUp(self) -> None:
        self.factory = CommandStatusRouteDecisionFactory()

    def test_status_preserves_bounded_metadata_and_acknowledgement(self):
        acknowledgement = object()

        decision = self.factory.status(
            response_text="다이아 곡괭이 만드는 중이야",
            state="running",
            acknowledgement=acknowledgement,
            presentation_detail_log=(
                '{"command_name":"get","form_kind":"target_count",'
                '"requested_count":1,"target":"diamond_pickaxe"}'
            ),
        )

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_command_status_query", decision.reason)
        self.assertEqual("command_status_query", decision.route_kind)
        self.assertEqual("command_status", decision.response_kind)
        self.assertEqual("running", decision.result["status"])
        self.assertFalse(decision.result["command_submitted"])
        self.assertIs(
            acknowledgement,
            decision.response_publication_acknowledgement,
        )

    def test_cautious_fallthrough_and_emergency_are_distinct(self):
        cautious = self.factory.cautious()
        fallthrough = self.factory.conversational_fallthrough()

        self.assertTrue(cautious.handled)
        self.assertEqual(
            "지금 마인크래프트 작업 상태를 확인하지 못했어",
            cautious.response_text,
        )
        self.assertFalse(fallthrough.handled)
        self.assertEqual(
            "command_status_conversational_fallthrough",
            fallthrough.reason,
        )
        self.assertIs(
            COMMAND_STATUS_EMERGENCY_DECISION,
            self.factory.emergency(),
        )


if __name__ == "__main__":
    unittest.main()
