#20260905_kpopmodder: Verify focused trusted Korean route sequencing and fail-closed cleanup.
#20260908_kpopmodder: Verify acknowledged STATUS and emergency sentinel custody.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean import (
    TrustedKoreanInputRouteCoordinator,
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


class TrustedCommandStatusPublicationCustodyTests(unittest.TestCase):
    def test_feedback_exception_fails_original_ack_and_returns_sentinel(self):
        for route_kind, response_kind in _STATUS_ROUTE_IDENTITIES:
            with self.subTest(route_kind=route_kind):
                decision, acknowledgements, records = _protected_decision(
                    route_kind=route_kind,
                    response_kind=response_kind,
                )
                proof = _Proof()
                coordinator = _coordinator(
                    decision,
                    proof,
                    render=lambda _decision: _raise(RuntimeError("SECRET")),
                )

                result = coordinator.route("event", object())

                self.assertIs(COMMAND_STATUS_EMERGENCY_DECISION, result)
                self.assertEqual([False], acknowledgements)
                self.assertEqual(
                    [("trusted_feedback_rendering", "RuntimeError")],
                    records,
                )
                self.assertEqual(1, proof.close_count)

    def test_authorizer_exception_protects_both_status_route_identities(self):
        for route_kind, response_kind in _STATUS_ROUTE_IDENTITIES:
            with self.subTest(route_kind=route_kind):
                decision, acknowledgements, records = _protected_decision(
                    route_kind=route_kind,
                    response_kind=response_kind,
                )
                proof = _Proof(issue_error=LookupError("SECRET"))
                coordinator = _coordinator(decision, proof)

                result = coordinator.route("event", object())

                self.assertIs(COMMAND_STATUS_EMERGENCY_DECISION, result)
                self.assertEqual([False], acknowledgements)
                self.assertEqual(
                    [("response_capability_authorization", "LookupError")],
                    records,
                )
                self.assertEqual(1, proof.close_count)
                self.assertEqual(1, proof.capability_count)

    def test_finalization_and_close_failures_keep_first_stage_and_close_once(self):
        decision, acknowledgements, records = _protected_decision()
        proof = _Proof()
        coordinator = _coordinator(
            decision,
            proof,
            close_feature=lambda _proof: _raise(LookupError("SECOND")),
        )
        coordinator.log_feature_admission = lambda *_args, **_kwargs: _raise(
            RuntimeError("FIRST")
        )

        result = coordinator.route("event", object())

        self.assertIs(COMMAND_STATUS_EMERGENCY_DECISION, result)
        self.assertEqual([False], acknowledgements)
        self.assertEqual(
            [("feature_admission_finalization", "RuntimeError")],
            records,
        )
        self.assertEqual(1, proof.close_count)

    def test_emergency_sentinel_survives_finalization_and_close_exceptions(self):
        proof = _Proof()
        coordinator = _coordinator(
            COMMAND_STATUS_EMERGENCY_DECISION,
            proof,
            close_feature=lambda _proof: _raise(LookupError("SECOND")),
        )
        coordinator.log_feature_admission = lambda *_args, **_kwargs: _raise(
            RuntimeError("FIRST")
        )

        result = coordinator.route("event", object())

        self.assertIs(COMMAND_STATUS_EMERGENCY_DECISION, result)
        self.assertEqual(1, proof.close_count)
        self.assertEqual(0, proof.capability_count)


class _Proof:
    def __init__(self, *, issue_error=None):
        self.close_count = 0
        self.capability_count = 0
        self.issue_error = issue_error

    def issue_response_emission_capability(self, *_args, **_kwargs):
        self.capability_count += 1
        if self.issue_error is not None:
            raise self.issue_error
        return object()

    def close(self):
        self.close_count += 1


def _coordinator(decision, proof, *, render=None, close_feature=None):
    return TrustedKoreanInputRouteCoordinator(
        owner=object(),
        input_event_normalizer=SimpleNamespace(normalize=lambda value: value),
        eligibility_admission=SimpleNamespace(
            issue=lambda **_kwargs: (proof, "eligible")
        ),
        feedback_facade=SimpleNamespace(
            render=render or (lambda value: value.response_text)
        ),
        feature_admission_logger=SimpleNamespace(log=lambda _record: None),
        feature_admission_projector=SimpleNamespace(
            project=lambda **_kwargs: object()
        ),
        route_callback=lambda _event, **_kwargs: decision,
        close_feature_dispatch_callback=close_feature or (lambda _proof: None),
        status_publication_custody_policy=(
            CommandStatusPublicationCustodyPolicy()
        ),
        status_publication_emergency_decision=(
            COMMAND_STATUS_EMERGENCY_DECISION
        ),
    )


_STATUS_ROUTE_IDENTITIES = (
    ("command_status_query", "command_status"),
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
    decision = MinecraftChatClefInputRouteDecision.handled_result(
        reason="status",
        response_text="working",
        route_kind=route_kind,
        response_kind=response_kind,
        response_publication_acknowledgement=acknowledgement,
    )
    return decision, acknowledgements, records


def _raise(error):
    raise error


if __name__ == "__main__":
    unittest.main()
