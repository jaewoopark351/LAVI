#20260905_kpopmodder: Verify focused trusted Korean route sequencing and fail-closed cleanup.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from llm_core.input_routing import RoutedInputDispatchCoordinator
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean import (
    TrustedKoreanInputRouteCoordinator,
)
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.trusted_korean_proof_lifecycle_closer import (
    TrustedKoreanProofLifecycleCloser,
)


class TrustedKoreanInputRouteCoordinatorTests(unittest.TestCase):
    def test_rejected_evidence_is_consumed_without_route_or_feedback(self):
        trace = []
        coordinator = _coordinator(
            trace,
            proof=None,
            reason="consumed_ingress_evidence_invalid",
            route_callback=lambda *_args, **_kwargs: self.fail(
                "rejected ingress must not route"
            ),
        )

        decision = coordinator.route("event", object())

        self.assertTrue(decision.handled)
        self.assertTrue(decision.suppress_response)
        self.assertFalse(decision.publish_external_response)
        self.assertIsNone(decision.response_emission_capability)
        self.assertEqual("", decision.response_text)
        self.assertEqual(
            ["normalize", "admit", "diagnostic", "log"],
            trace,
        )

    def test_scope_out_reason_falls_through_and_is_observed(self):
        trace = []
        fallthrough = MinecraftChatClefInputRouteDecision.not_handled(
            "ordinary_fallthrough"
        )
        coordinator = _coordinator(
            trace,
            proof=None,
            reason="input_language_not_korean",
            route_callback=lambda event: (
                trace.append(("fallthrough", event)) or fallthrough
            ),
        )

        decision = coordinator.route("event", object())

        self.assertIs(fallthrough, decision)
        self.assertEqual(
            [
                "normalize",
                "admit",
                ("fallthrough", "normalized-event"),
                "diagnostic",
                "log",
            ],
            trace,
        )

    def test_handled_route_renders_authorizes_observes_and_closes_in_order(self):
        trace = []
        capability = object()
        proof = _Proof(trace, capability=capability)
        routed = MinecraftChatClefInputRouteDecision.handled_result(
            reason="minecraft_command_routed",
            response_text="pre-render",
        )

        def route(event, *, korean_eligibility_proof):
            trace.append(("route", event, korean_eligibility_proof))
            return routed

        coordinator = _coordinator(
            trace,
            proof=proof,
            reason="eligible",
            route_callback=route,
            rendered_text="준비 명령을 보냈어요.",
        )

        decision = coordinator.route("event", object())

        self.assertEqual("준비 명령을 보냈어요.", decision.response_text)
        self.assertTrue(decision.publish_external_response)
        self.assertFalse(decision.suppress_response)
        self.assertEqual("minecraft_chatclef", decision.response_source)
        self.assertIs(capability, decision.response_emission_capability)
        self.assertEqual(
            [
                "normalize",
                "admit",
                ("route", "normalized-event", proof),
                "render",
                (
                    "authorize",
                    "normalized-event",
                    "준비 명령을 보냈어요.",
                    "minecraft_chatclef",
                ),
                "diagnostic",
                "log",
                ("close-feature", proof),
                "close-proof",
            ],
            trace,
        )

    def test_missing_response_capability_clears_text_and_suppresses(self):
        trace = []
        proof = _Proof(trace, capability=None)
        coordinator = _coordinator(
            trace,
            proof=proof,
            reason="eligible",
            route_callback=lambda _event, **_kwargs: (
                MinecraftChatClefInputRouteDecision.handled_result(
                    reason="minecraft_command_routed",
                    response_text="pre-render",
                )
            ),
            rendered_text="응답",
        )

        decision = coordinator.route("event", object())

        self.assertEqual("", decision.response_text)
        self.assertFalse(decision.publish_external_response)
        self.assertTrue(decision.suppress_response)
        self.assertIsNone(decision.response_emission_capability)
        self.assertTrue(proof.closed)

    def test_empty_handled_response_suppresses_without_issuing_capability(self):
        trace = []
        proof = _Proof(trace, capability=object())
        coordinator = _coordinator(
            trace,
            proof=proof,
            reason="eligible",
            route_callback=lambda _event, **_kwargs: (
                MinecraftChatClefInputRouteDecision.handled_result(
                    reason="stop_control_accepted",
                    response_text="",
                    route_kind="stop_control",
                    response_kind="command_start",
                )
            ),
            rendered_text="",
        )

        decision = coordinator.route("event", object())

        self.assertEqual("", decision.response_text)
        self.assertFalse(decision.publish_external_response)
        self.assertTrue(decision.suppress_response)
        self.assertIsNone(decision.response_emission_capability)
        self.assertFalse(
            any(
                isinstance(entry, tuple) and entry[0] == "authorize"
                for entry in trace
            )
        )
        self.assertTrue(proof.closed)

        publisher_requests = []
        outcome = RoutedInputDispatchCoordinator(
            response_publisher_callback=lambda: publisher_requests.append(
                "publisher_requested"
            ),
            router=SimpleNamespace(route=lambda _event: decision),
            log_callback=lambda _message: None,
        ).dispatch("event")

        self.assertTrue(outcome.handled)
        self.assertTrue(outcome.suppress_response)
        self.assertIsNone(outcome.response)
        self.assertEqual([], publisher_requests)

    def test_trusted_route_exception_closes_proof_and_outer_dispatch_fails_closed(self):
        trace = []
        proof = _Proof(trace, capability=object())

        def fail_after_side_effect(_event, **_kwargs):
            trace.append("minecraft-side-effect")
            raise RuntimeError("private failure detail")

        coordinator = _coordinator(
            trace,
            proof=proof,
            reason="eligible",
            route_callback=fail_after_side_effect,
        )
        legacy_calls = []
        logs = []
        outer = RoutedInputDispatchCoordinator(
            response_publisher_callback=lambda: None,
            router=SimpleNamespace(
                route_trusted_user_input=coordinator.route,
                route=lambda _event: legacy_calls.append("legacy"),
            ),
            log_callback=logs.append,
        )

        outcome = outer.dispatch(
            "event",
            trusted_ingress_evidence=object(),
        )

        self.assertTrue(outcome.handled)
        self.assertTrue(outcome.suppress_response)
        self.assertIsNone(outcome.response)
        self.assertEqual([], legacy_calls)
        self.assertTrue(proof.closed)
        self.assertEqual(1, trace.count("close-proof"))
        self.assertEqual(1, len(logs))
        self.assertNotIn("private failure detail", logs[0])

    def test_feature_cleanup_failure_still_closes_proof_once(self):
        trace = []
        proof = _Proof(trace, capability=None)

        def fail_feature_cleanup(_proof):
            trace.append("close-feature-failed")
            raise RuntimeError("cleanup failed")

        closer = TrustedKoreanProofLifecycleCloser(fail_feature_cleanup)

        with self.assertRaisesRegex(RuntimeError, "cleanup failed"):
            closer.close(proof)

        self.assertEqual(
            ["close-feature-failed", "close-proof"],
            trace,
        )
        self.assertTrue(proof.closed)


def _coordinator(
    trace,
    *,
    proof,
    reason,
    route_callback,
    rendered_text="rendered",
):
    normalizer = SimpleNamespace(
        normalize=lambda _value: trace.append("normalize") or "normalized-event"
    )

    def issue(**_kwargs):
        trace.append("admit")
        return proof, reason

    admission = SimpleNamespace(issue=issue)
    feedback = SimpleNamespace(
        render=lambda _decision: trace.append("render") or rendered_text
    )

    def project(**_kwargs):
        trace.append("diagnostic")
        return object()

    logger = SimpleNamespace(log=lambda _record: trace.append("log"))
    return TrustedKoreanInputRouteCoordinator(
        owner="owner",
        input_event_normalizer=normalizer,
        eligibility_admission=admission,
        feedback_facade=feedback,
        feature_admission_logger=logger,
        feature_admission_projector=SimpleNamespace(project=project),
        route_callback=route_callback,
        close_feature_dispatch_callback=lambda value: trace.append(
            ("close-feature", value)
        ),
    )


class _Proof:
    def __init__(self, trace, *, capability):
        self._trace = trace
        self._capability = capability
        self.closed = False

    def issue_response_emission_capability(
        self,
        event,
        _owner,
        *,
        text,
        source,
        response_kind="immediate",
    ):
        self._trace.append(("authorize", event, text, source))
        return self._capability

    def close(self):
        self._trace.append("close-proof")
        self.closed = True


if __name__ == "__main__":
    unittest.main()
