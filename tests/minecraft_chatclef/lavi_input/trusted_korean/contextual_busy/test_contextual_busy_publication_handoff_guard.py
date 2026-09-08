#20260909_kpopmodder: Verify exact contextual-busy STATUS custody transfer.
from __future__ import annotations

import unittest

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.publication.status import (
    CommandStatusPublicationCustody,
)
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.response.contextual_busy import (
    CONTEXTUAL_BUSY_SUPPRESSED_DECISION,
    ContextualBusyResponsePreparation,
)
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.response.contextual_busy.publication import (
    ContextualBusyPublicationHandoffGuard,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackPublicationAcknowledgement,
    CommandFeedbackPublicationPermit,
)


class ContextualBusyPublicationHandoffGuardTests(unittest.TestCase):
    def test_transfers_only_the_exact_decision_and_acknowledgement(self):
        acknowledgement, acknowledgements, records = _acknowledgement()
        decision = _decision(acknowledgement)
        preparation = ContextualBusyResponsePreparation(
            decision,
            local_handoff_token=acknowledgement,
        )
        expected_custody = _custody(decision, acknowledgement)

        resolved, custody = ContextualBusyPublicationHandoffGuard().claim_and_transfer(
            preparation,
            outer_claim=lambda value: (
                expected_custody
                if value is decision
                else self.fail("outer claim received a copied decision")
            ),
        )

        self.assertIs(decision, resolved)
        self.assertIs(expected_custody, custody)
        self.assertEqual([], acknowledgements)
        self.assertEqual([], records)

    def test_preparation_without_local_token_uses_the_existing_claim_unchanged(self):
        decision = MinecraftChatClefInputRouteDecision.not_handled("unrelated")
        expected_custody = object()
        claimed = []

        resolved, custody = ContextualBusyPublicationHandoffGuard().claim_and_transfer(
            ContextualBusyResponsePreparation(decision),
            outer_claim=lambda value: (
                claimed.append(value) or expected_custody
            ),
        )

        self.assertIs(decision, resolved)
        self.assertIs(expected_custody, custody)
        self.assertEqual([decision], claimed)

    def test_every_failed_transfer_suppresses_and_resolves_local_ack_once(self):
        cases = ("claim_exception", "no_custody", "ack_replaced", "route_changed")
        for case in cases:
            with self.subTest(case=case):
                acknowledgement, acknowledgements, records = _acknowledgement()
                decision = _decision(acknowledgement)
                preparation = ContextualBusyResponsePreparation(
                    decision,
                    local_handoff_token=acknowledgement,
                )
                outer_claim = _failing_claim(case, decision)
                guard = ContextualBusyPublicationHandoffGuard()

                resolved, custody = guard.claim_and_transfer(
                    preparation,
                    outer_claim=outer_claim,
                )
                repeated, repeated_custody = guard.claim_and_transfer(
                    preparation,
                    outer_claim=outer_claim,
                )

                self.assertIs(CONTEXTUAL_BUSY_SUPPRESSED_DECISION, resolved)
                self.assertIs(CONTEXTUAL_BUSY_SUPPRESSED_DECISION, repeated)
                self.assertIsNone(custody)
                self.assertIsNone(repeated_custody)
                self.assertEqual([False], acknowledgements)
                self.assertEqual(1, len(records))
                self.assertEqual("contextual_busy_custody_handoff", records[0][0])
                expected_exception = "RuntimeError" if case == "claim_exception" else "none"
                self.assertEqual(expected_exception, records[0][1])

    def test_rejects_non_exact_preparation_before_claiming(self):
        with self.assertRaises(TypeError):
            ContextualBusyPublicationHandoffGuard().claim_and_transfer(
                object(),
                outer_claim=lambda _decision: self.fail("must not claim"),
            )


def _decision(acknowledgement):
    return MinecraftChatClefInputRouteDecision.handled_result(
        reason="minecraft_command_busy",
        response_text="다이아 곡괭이 만드는 중이야",
        result={"ok": False, "error": "active_command"},
        route_kind="command_busy_current_work",
        response_kind="command_status",
        response_publication_acknowledgement=acknowledgement,
    )


def _custody(decision, acknowledgement, *, route_kind=None):
    return CommandStatusPublicationCustody(
        decision=decision,
        acknowledgement=acknowledgement,
        diagnostic_custody=(
            acknowledgement.publication_failure_diagnostic_custody
        ),
        decision_type=type(decision),
        route_kind=route_kind or decision.route_kind,
        response_kind=decision.response_kind,
    )


def _failing_claim(case, decision):
    if case == "claim_exception":
        return lambda _decision: _raise(RuntimeError("private"))
    if case == "no_custody":
        return lambda _decision: None
    if case == "ack_replaced":
        replacement, _acknowledgements, _records = _acknowledgement()
        return lambda _decision: _custody(decision, replacement)
    if case == "route_changed":
        acknowledgement = decision.response_publication_acknowledgement
        return lambda _decision: _custody(
            decision,
            acknowledgement,
            route_kind="command_status_query",
        )
    raise AssertionError(case)


def _acknowledgement():
    acknowledgements = []
    records = []
    recorded = [False]

    def record_once(stage, exception_class):
        if recorded[0]:
            return False
        recorded[0] = True
        records.append((stage, exception_class))
        return True

    acknowledgement = CommandFeedbackPublicationAcknowledgement(
        permit=CommandFeedbackPublicationPermit(
            lifecycle_token=object(),
            sequence=1,
            kind=CommandFeedbackPublicationPermit.STATUS,
        ),
        callback=lambda _permit, published: (
            acknowledgements.append(published) or True
        ),
        publication_failure_diagnostic_custody=type(
            "DiagnosticCustody",
            (),
            {"record_once": staticmethod(record_once)},
        )(),
    )
    return acknowledgement, acknowledgements, records


def _raise(error):
    raise error


if __name__ == "__main__":
    unittest.main()
