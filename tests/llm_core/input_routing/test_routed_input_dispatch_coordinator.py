# 20260905_kpopmodder: Verifies routed-input dispatch extraction and compatibility.
from __future__ import annotations

import unittest
from dataclasses import FrozenInstanceError
from types import SimpleNamespace

from llm_core.input_routing import (
    LlmPredictionDispatchCoordinator,
    RoutedInputDispatchCoordinator,
    RoutedInputDispatchOutcome,
)
from llm_core.llm_component import LLM, _ROUTED_WITHOUT_RESPONSE
from llm_core.routed_response import RoutedResponseEmission


class RoutedInputDispatchCoordinatorTests(unittest.TestCase):
    def test_outcome_is_immutable_and_enforces_dispatch_invariants(self):
        outcome = RoutedInputDispatchOutcome(
            handled=True,
            suppress_response=False,
            response="ready",
        )

        with self.assertRaises(FrozenInstanceError):
            outcome.response = "changed"
        with self.assertRaises(ValueError):
            RoutedInputDispatchOutcome(
                handled=False,
                suppress_response=False,
                response="impossible",
            )

    def test_trusted_route_is_preferred_only_when_evidence_is_present(self):
        calls = []
        router = SimpleNamespace(
            route=lambda event: (
                calls.append(("legacy", event))
                or SimpleNamespace(handled=True, response_text="legacy")
            ),
            route_trusted_user_input=lambda event, evidence: (
                calls.append(("trusted", event, evidence))
                or SimpleNamespace(handled=True, response_text="trusted")
            ),
        )
        coordinator = RoutedInputDispatchCoordinator(
            response_publisher_callback=lambda: None,
            router=router,
            log_callback=lambda _message: None,
        )

        trusted = coordinator.dispatch("event", trusted_ingress_evidence="proof")
        legacy = coordinator.dispatch("event")

        self.assertEqual("trusted", trusted.response)
        self.assertEqual("legacy", legacy.response)
        self.assertEqual(
            [("trusted", "event", "proof"), ("legacy", "event")],
            calls,
        )

    def test_capability_publication_owns_chat_ui_delivery(self):
        emission = RoutedResponseEmission(
            text="response",
            source="minecraft_chatclef",
            response_generation=3,
            output_delivered=True,
            full_output_delivered=False,
            history_remembered=False,
            event_id="a" * 32,
            route_kind="minecraft_command",
            response_kind="immediate",
        )
        publisher = _RecordingPublisher(emission)
        event = SimpleNamespace(source="lavi_chat_ui", event_id="a" * 32)
        decision = SimpleNamespace(
            handled=True,
            suppress_response=False,
            response_text="response",
            publish_external_response=True,
            response_emission_capability=object(),
            response_source="minecraft_chatclef",
            route_kind="minecraft_command",
            response_kind="immediate",
        )
        coordinator = RoutedInputDispatchCoordinator(
            response_publisher_callback=lambda: publisher,
            router=SimpleNamespace(route=lambda _event: decision),
            log_callback=lambda _message: None,
        )

        outcome = coordinator.dispatch(event)
        yielded = coordinator.prepare_response_for_yield(event, outcome.response)

        self.assertIs(emission, outcome.response)
        self.assertEqual("response", yielded)
        self.assertEqual(1, len(publisher.emission_calls))
        self.assertEqual(
            [(emission, True, "yielded")],
            publisher.chat_ui_calls,
        )

    def test_rejected_publication_and_explicit_suppression_are_handled_silently(self):
        rejected = RoutedInputDispatchCoordinator(
            response_publisher_callback=lambda: _RecordingPublisher(None),
            router=SimpleNamespace(
                route=lambda _event: SimpleNamespace(
                    handled=True,
                    response_text="response",
                    publish_external_response=True,
                )
            ),
            log_callback=lambda _message: None,
        ).dispatch(SimpleNamespace(source="voice_input_final"))
        suppressed = RoutedInputDispatchCoordinator(
            response_publisher_callback=lambda: None,
            router=SimpleNamespace(
                route=lambda _event: SimpleNamespace(
                    handled=True,
                    suppress_response=True,
                )
            ),
            log_callback=lambda _message: None,
        ).dispatch("event")

        self.assertTrue(rejected.handled)
        self.assertTrue(rejected.suppress_response)
        self.assertIsNone(rejected.response)
        self.assertEqual(rejected, suppressed)

    def test_dispatch_failure_log_excludes_message_and_exception_text(self):
        logs = []

        def fail(_event):
            raise RuntimeError("SECRET_ERROR")

        coordinator = RoutedInputDispatchCoordinator(
            response_publisher_callback=lambda: None,
            router=SimpleNamespace(route=fail),
            log_callback=logs.append,
        )

        outcome = coordinator.dispatch("SECRET_TRANSCRIPT")

        self.assertFalse(outcome.handled)
        self.assertEqual(1, len(logs))
        self.assertNotIn("SECRET_TRANSCRIPT", logs[0])
        self.assertNotIn("SECRET_ERROR", logs[0])

    def test_trusted_route_failure_is_suppressed_after_possible_side_effect(self):
        calls = []

        def fail_after_submission(_event, _evidence):
            calls.append("minecraft_submit")
            raise RuntimeError("post-submit response failure")

        coordinator = RoutedInputDispatchCoordinator(
            response_publisher_callback=lambda: None,
            router=SimpleNamespace(
                route_trusted_user_input=fail_after_submission,
                route=lambda _event: calls.append("legacy_route"),
            ),
            log_callback=lambda _message: None,
        )

        outcome = coordinator.dispatch(
            "trusted event",
            trusted_ingress_evidence=object(),
        )

        self.assertTrue(outcome.handled)
        self.assertTrue(outcome.suppress_response)
        self.assertIsNone(outcome.response)
        self.assertEqual(["minecraft_submit"], calls)

    def test_llm_compatibility_delegates_preserve_sentinel_and_direct_router_assignment(
        self,
    ):
        llm = LLM.__new__(LLM)
        llm.input_router = SimpleNamespace(
            route=lambda _event: SimpleNamespace(
                handled=True,
                suppress_response=True,
            )
        )

        routed = llm._try_route_external_input("event")

        self.assertIs(_ROUTED_WITHOUT_RESPONSE, routed)
        self.assertEqual([], list(llm.predict_wrapper("event", [], "system")))

        replacement = SimpleNamespace(
            route=lambda _event: SimpleNamespace(
                handled=True,
                response_text="replacement",
            )
        )
        llm.set_input_router(replacement)
        self.assertEqual("replacement", llm._try_route_external_input("event"))

    def test_llm_predict_wrapper_delegates_to_prediction_coordinator(self):
        llm = LLM.__new__(LLM)
        llm.input_router = SimpleNamespace(
            route=lambda _event: SimpleNamespace(
                handled=True,
                response_text="delegated",
            )
        )

        self.assertEqual(
            ["delegated"],
            list(llm.predict_wrapper("event", [], "system")),
        )
        self.assertIsInstance(
            llm.prediction_dispatch_coordinator,
            LlmPredictionDispatchCoordinator,
        )


class _RecordingPublisher:
    def __init__(self, emission):
        self._emission = emission
        self.emission_calls = []
        self.chat_ui_calls = []

    def emit_capability_response(self, text, **metadata):
        self.emission_calls.append((text, metadata))
        return self._emission

    def log_chat_ui_delivery(self, emission, *, delivered, reason):
        self.chat_ui_calls.append((emission, delivered, reason))
        return True


if __name__ == "__main__":
    unittest.main()
