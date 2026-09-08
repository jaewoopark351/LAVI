#20260908_kpopmodder: Verify safe dispatcher-to-STATUS diagnostic adaptation.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.diagnostics import (
    CommandStatusPublicationFailureAdapter,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.publication import (
    CommandStatusPublicationCustodyPolicy,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.publication import (
    CommandFeedbackPublicationAcknowledgement,
    CommandFeedbackPublicationPermit,
)


class CommandStatusPublicationFailureAdapterTests(unittest.TestCase):
    def test_adapter_reapplies_policy_and_records_bounded_dispatch_failure(self):
        records = []
        decision = _decision(
            SimpleNamespace(
                record_once=lambda stage, exception_class: records.append(
                    (stage, exception_class)
                )
            )
        )
        adapter = CommandStatusPublicationFailureAdapter(
            custody_policy=CommandStatusPublicationCustodyPolicy()
        )

        adapter.observe(
            decision,
            stage="dispatcher_initial_decision_resolution",
            exception_class="RuntimeError",
        )

        self.assertEqual(
            [("dispatcher_initial_decision_resolution", "RuntimeError")],
            records,
        )

    def test_policy_negative_and_throwing_sink_are_isolated(self):
        adapter = CommandStatusPublicationFailureAdapter(
            custody_policy=CommandStatusPublicationCustodyPolicy()
        )
        ordinary = MinecraftChatClefInputRouteDecision.handled_result(
            reason="ordinary",
            response_text="ordinary",
        )

        adapter.observe(
            ordinary,
            stage="dispatcher_initial_decision_resolution",
            exception_class="RuntimeError",
        )
        adapter.observe(
            _decision(
                SimpleNamespace(
                    record_once=lambda *_args, **_kwargs: _raise(
                        LookupError("sink")
                    )
                )
            ),
            stage="dispatcher_ready_decision_resolution",
            exception_class="RuntimeError",
        )


def _decision(custody):
    acknowledgement = CommandFeedbackPublicationAcknowledgement(
        permit=CommandFeedbackPublicationPermit(
            lifecycle_token=object(),
            sequence=1,
            kind=CommandFeedbackPublicationPermit.STATUS,
        ),
        callback=lambda _permit, _published: True,
        publication_failure_diagnostic_custody=custody,
    )
    return MinecraftChatClefInputRouteDecision.handled_result(
        reason="status",
        response_text="working",
        route_kind="command_status_query",
        response_kind="command_status",
        response_publication_acknowledgement=acknowledgement,
    )


def _raise(error):
    raise error


if __name__ == "__main__":
    unittest.main()
