#20260908_kpopmodder: Verify STATUS-only optional result validation custody.
from __future__ import annotations

import unittest
from dataclasses import replace
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.invocation.publication import (
    AcknowledgedOptionalRouteResultGuard,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.publication import (
    CommandStatusPublicationCustodyPolicy,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.routing.decision.command_status_emergency_decision import (
    COMMAND_STATUS_EMERGENCY_DECISION,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.publication import (
    CommandFeedbackPublicationAcknowledgement,
    CommandFeedbackPublicationPermit,
)


class AcknowledgedOptionalRouteResultGuardTests(unittest.TestCase):
    def test_validation_exception_records_and_acknowledges_false_once(self):
        for route_kind, response_kind in _STATUS_ROUTE_IDENTITIES:
            with self.subTest(route_kind=route_kind):
                decision, acknowledgements, records = _protected_decision(
                    route_kind=route_kind,
                    response_kind=response_kind,
                )
                guard = _guard()

                result = guard.validate(
                    decision,
                    SimpleNamespace(
                        validate=lambda _value: _raise(ValueError("SECRET"))
                    ),
                )

                self.assertIs(COMMAND_STATUS_EMERGENCY_DECISION, result)
                self.assertEqual([False], acknowledgements)
                self.assertEqual(
                    [("optional_route_result_validation", "ValueError")],
                    records,
                )

    def test_equal_replacement_fails_acknowledgement_identity_closed(self):
        for route_kind, response_kind in _STATUS_ROUTE_IDENTITIES:
            with self.subTest(route_kind=route_kind):
                decision, acknowledgements, records = _protected_decision(
                    route_kind=route_kind,
                    response_kind=response_kind,
                )

                result = _guard().validate(
                    decision,
                    SimpleNamespace(validate=lambda value: replace(value)),
                )

                self.assertIs(COMMAND_STATUS_EMERGENCY_DECISION, result)
                self.assertEqual([False], acknowledgements)
                self.assertEqual(
                    [("optional_route_result_acknowledgement_identity", "none")],
                    records,
                )

    def test_emergency_identity_bypasses_existing_validator(self):
        result = _guard().validate(
            COMMAND_STATUS_EMERGENCY_DECISION,
            SimpleNamespace(validate=lambda _value: _raise(AssertionError())),
        )

        self.assertIs(COMMAND_STATUS_EMERGENCY_DECISION, result)

    def test_policy_negative_validation_keeps_existing_exception_behavior(self):
        decision = MinecraftChatClefInputRouteDecision.handled_result(
            reason="ordinary",
            response_text="ordinary",
        )

        with self.assertRaisesRegex(RuntimeError, "existing"):
            _guard().validate(
                decision,
                SimpleNamespace(
                    validate=lambda _value: _raise(RuntimeError("existing"))
                ),
            )


def _guard():
    return AcknowledgedOptionalRouteResultGuard(
        custody_policy=CommandStatusPublicationCustodyPolicy(),
        emergency_decision=COMMAND_STATUS_EMERGENCY_DECISION,
    )


_STATUS_ROUTE_IDENTITIES = (
    ("command_status_query", "command_status"),
    ("command_busy_current_work", "command_status"),
    ("crafting_status_query", "immediate"),
)


def _protected_decision(
    *,
    route_kind="command_status_query",
    response_kind="command_status",
):
    acknowledgements = []
    records = []
    custody = SimpleNamespace(
        record_once=lambda stage, exception_class: records.append(
            (stage, exception_class)
        )
    )
    acknowledgement = CommandFeedbackPublicationAcknowledgement(
        permit=CommandFeedbackPublicationPermit(
            lifecycle_token=object(),
            sequence=1,
            kind=CommandFeedbackPublicationPermit.STATUS,
        ),
        callback=lambda _permit, published: acknowledgements.append(published) or True,
        publication_failure_diagnostic_custody=custody,
    )
    return (
        MinecraftChatClefInputRouteDecision.handled_result(
            reason="status",
            response_text="working",
            route_kind=route_kind,
            response_kind=response_kind,
            response_publication_acknowledgement=acknowledgement,
        ),
        acknowledgements,
        records,
    )


def _raise(error):
    raise error


if __name__ == "__main__":
    unittest.main()
