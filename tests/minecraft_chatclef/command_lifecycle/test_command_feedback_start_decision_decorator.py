#20260907_kpopmodder: Verify accepted START uses lifecycle presentation identity.
from __future__ import annotations

import json
import unittest

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandFeedbackStartDecisionDecorator,
    CommandLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackDescriptor,
)


class CommandFeedbackStartDecisionDecoratorTests(unittest.TestCase):
    def test_claimed_start_replaces_technical_identity_and_keeps_acknowledgement(self):
        acknowledgement = object()
        decision = MinecraftChatClefInputRouteDecision.handled_result(
            reason="submitted",
            response_text="명령을 제출했어요",
            route_kind="minecraft_command",
            response_kind="immediate",
        )
        descriptor = CommandFeedbackDescriptor(
            command_name="get",
            command="get diamond_pickaxe 1",
            command_source="lavi_chat_ui",
            lifecycle_kind="task",
            phrase_profile_id="get_phrase_v1",
            evidence_profile_id="get_effect_v1",
            rollout_state="verified",
            event_id="a" * 32,
            input_source="lavi_chat_ui",
            provider_id="lavi_chat_ui",
            event_kind="chat_submit",
            requested_family="item_get",
            target_item="diamond_pickaxe",
            requested_count=1,
            quantity_semantics=CommandFeedbackDescriptor.ACQUIRE_DELTA,
            acquisition_verb_class="craft",
            spoken_target_label="다이아 곡괭이",
        )

        decorated = CommandFeedbackStartDecisionDecorator(
            CommandLifecycleResponseRenderer()
        ).decorate(
            decision,
            descriptor=descriptor,
            start_claimed=True,
            publication_acknowledgement=acknowledgement,
        )

        self.assertEqual("다이아 곡괭이 만들어 줄게", decorated.response_text)
        self.assertEqual("command_lifecycle", decorated.route_kind)
        self.assertEqual("command_start", decorated.response_kind)
        self.assertIs(
            acknowledgement,
            decorated.response_publication_acknowledgement,
        )
        self.assertEqual(
            "diamond_pickaxe",
            json.loads(decorated.presentation_detail_log)["target"],
        )
        self.assertNotIn("diamond_pickaxe", decorated.response_text)


if __name__ == "__main__":
    unittest.main()
