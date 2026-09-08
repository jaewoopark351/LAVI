#20260908_kpopmodder: Verify exact shared STATUS custody and emergency decision contracts.
from __future__ import annotations

import unittest
from dataclasses import FrozenInstanceError, replace
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.publication import (
    CommandStatusPublicationCustodyPolicy,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.routing.decision.command_status_emergency_decision import (
    COMMAND_STATUS_EMERGENCY_DECISION,
    CommandStatusEmergencyDecision,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.publication import (
    CommandFeedbackPublicationAcknowledgement,
    CommandFeedbackPublicationPermit,
)


class CommandStatusPublicationCustodyTests(unittest.TestCase):
    def test_policy_accepts_both_routes_legacy_response_and_subclass(self):
        policy = CommandStatusPublicationCustodyPolicy()
        acknowledgement = _acknowledgement(CommandFeedbackPublicationPermit.STATUS)

        generalized = _decision(
            acknowledgement,
            route_kind="command_status_query",
            response_kind="command_status",
        )
        legacy = _decision(
            acknowledgement,
            route_kind="crafting_status_query",
            response_kind="immediate",
        )
        subclass = _StatusDecisionSubclass(**vars(generalized))

        self.assertTrue(policy.matches(generalized))
        self.assertTrue(policy.matches(legacy))
        self.assertTrue(policy.matches(subclass))

    def test_policy_rejects_start_lookalike_and_malformed_rows_without_raising(self):
        policy = CommandStatusPublicationCustodyPolicy()
        start = _decision(_acknowledgement(CommandFeedbackPublicationPermit.START))
        lookalike = _decision(
            SimpleNamespace(kind="status", acknowledge=lambda **_kwargs: True)
        )

        rows = (
            start,
            lookalike,
            replace(_decision(_acknowledgement("status")), handled=False),
            replace(_decision(_acknowledgement("status")), route_kind="other"),
            SimpleNamespace(handled=True),
            None,
        )

        self.assertTrue(all(policy.matches(row) is False for row in rows))

    def test_emergency_decision_is_slotted_deeply_immutable_and_replace_safe(self):
        decision = COMMAND_STATUS_EMERGENCY_DECISION

        self.assertIs(type(decision), CommandStatusEmergencyDecision)
        self.assertFalse(hasattr(decision, "__dict__"))
        self.assertEqual("command_status_publication_fail_closed", decision.reason)
        self.assertTrue(decision.handled)
        self.assertTrue(decision.suppress_response)
        self.assertFalse(decision.publish_external_response)
        self.assertIsNone(decision.response_publication_acknowledgement)
        self.assertEqual("command_status_query", decision.route_kind)
        self.assertEqual("command_status", decision.response_kind)
        with self.assertRaises(TypeError):
            decision.result["secret"] = True
        with self.assertRaises(TypeError):
            decision.translation["secret"] = True
        with self.assertRaises(FrozenInstanceError):
            decision.response_text = "changed"
        replaced = replace(decision, suppress_response=True)
        self.assertIs(type(replaced), CommandStatusEmergencyDecision)
        self.assertEqual(decision, replaced)


class _StatusDecisionSubclass(MinecraftChatClefInputRouteDecision):
    pass


def _acknowledgement(kind):
    return CommandFeedbackPublicationAcknowledgement(
        permit=CommandFeedbackPublicationPermit(
            lifecycle_token=object(),
            sequence=1,
            kind=kind,
        ),
        callback=lambda _permit, _published: True,
    )


def _decision(
    acknowledgement,
    *,
    route_kind="command_status_query",
    response_kind="command_status",
):
    return MinecraftChatClefInputRouteDecision.handled_result(
        reason="status",
        response_text="working",
        route_kind=route_kind,
        response_kind=response_kind,
        response_publication_acknowledgement=acknowledgement,
    )


if __name__ == "__main__":
    unittest.main()
