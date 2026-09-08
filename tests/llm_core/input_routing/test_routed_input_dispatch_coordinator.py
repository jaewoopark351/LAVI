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
from llm_core.routed_response import (
    RoutedResponseEmission,
    RoutedResponsePresentationMetadata,
    RoutedResponseUiPresentationIdentity,
)


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
        self.assertFalse(publisher.emission_calls[0][1]["send_ui"])

    def test_typed_chat_presentation_renders_before_success_receipt(self):
        emission = _typed_emission(event_id="b" * 32)
        publisher = _RecordingPublisher(emission)
        coordinator = RoutedInputDispatchCoordinator(
            response_publisher_callback=lambda: publisher,
            log_callback=lambda _message: None,
        )
        timeline = []
        coordinator._components.chat_ui_yield_adapter._ui_adapter.render = (
            lambda *_args, **_kwargs: timeline.append("render") or "rendered"
        )

        def log_delivery(delivered_emission, *, delivered, reason):
            timeline.append("log")
            publisher.chat_ui_calls.append(
                (delivered_emission, delivered, reason)
            )
            return True

        publisher.log_chat_ui_delivery = log_delivery

        yielded = coordinator.prepare_response_for_yield(
            SimpleNamespace(source="lavi_chat_ui"),
            emission,
        )

        self.assertEqual("rendered", yielded)
        self.assertEqual(["render", "log"], timeline)
        logged, delivered, reason = publisher.chat_ui_calls[0]
        self.assertTrue(delivered)
        self.assertEqual("yielded", reason)
        self.assertTrue(logged.presentation_receipt.accepted)
        self.assertEqual("ui_presentation", logged.presentation_receipt.sink)
        self.assertEqual("yielded", logged.presentation_receipt.reason)
        self.assertIsNone(emission.presentation_receipt)

    def test_typed_chat_render_failure_logs_failed_receipt_and_keeps_text(self):
        emission = _typed_emission(event_id="c" * 32)
        publisher = _RecordingPublisher(emission)
        coordinator = RoutedInputDispatchCoordinator(
            response_publisher_callback=lambda: publisher,
            log_callback=lambda _message: None,
        )

        def fail_render(*_args, **_kwargs):
            raise RuntimeError("render failed")

        coordinator._components.chat_ui_yield_adapter._ui_adapter.render = (
            fail_render
        )

        yielded = coordinator.prepare_response_for_yield(
            SimpleNamespace(source="lavi_chat_ui"),
            emission,
        )

        self.assertEqual("response", yielded)
        logged, delivered, reason = publisher.chat_ui_calls[0]
        self.assertFalse(delivered)
        self.assertEqual("delivery_failed", reason)
        self.assertFalse(logged.presentation_receipt.accepted)
        self.assertEqual("delivery_failed", logged.presentation_receipt.reason)

    def test_coalesced_chat_yields_one_badged_terminal_message(self):
        emission = _typed_emission(
            event_id="e" * 32,
            response_kind="command_coalesced",
            text="감마를 바꿨어",
            detail_log=(
                '{"command_name":"gamma","form_kind":"explicit_value",'
                '"setting_value":"1.0"}'
            ),
        )
        publisher = _RecordingPublisher(emission)
        coordinator = RoutedInputDispatchCoordinator(
            response_publisher_callback=lambda: publisher,
            log_callback=lambda _message: None,
        )

        yielded = coordinator.prepare_response_for_yield(
            SimpleNamespace(source="lavi_chat_ui"),
            emission,
        )

        self.assertEqual("감마를 바꿨어", yielded.content)
        self.assertEqual("Minecraft", yielded.metadata["title"])
        self.assertEqual(
            RoutedResponseUiPresentationIdentity.from_response(
                event_id="e" * 32,
                route_kind="command_lifecycle",
                response_kind="command_coalesced",
                response_source="minecraft_chatclef",
                source_kind="minecraft",
                badge_label="Minecraft",
            ).token,
            yielded.metadata["id"],
        )
        self.assertIn('"command_name":"gamma"', yielded.metadata["log"])
        self.assertNotIn("gamma", yielded.content)
        self.assertEqual(1, len(publisher.chat_ui_calls))
        logged, delivered, reason = publisher.chat_ui_calls[0]
        self.assertTrue(delivered)
        self.assertEqual("yielded", reason)
        self.assertEqual("command_coalesced", logged.response_kind)
        self.assertTrue(logged.presentation_receipt.accepted)
        self.assertEqual("e" * 32, logged.presentation_receipt.event_id)

    def test_voice_current_input_uses_async_typed_ui_sink(self):
        emission = _typed_emission(event_id="d" * 32)
        publisher = _RecordingPublisher(emission)
        event = SimpleNamespace(source="voice_input_final", event_id="d" * 32)
        decision = SimpleNamespace(
            handled=True,
            suppress_response=False,
            response_text="response",
            publish_external_response=True,
            response_emission_capability=object(),
            response_source="minecraft_chatclef",
            route_kind="command_lifecycle",
            response_kind="command_start",
            presentation_detail_log=(
                '{"command_name":"get","form_kind":"target_count",'
                '"requested_count":1,"target":"diamond_pickaxe"}'
            ),
        )
        coordinator = RoutedInputDispatchCoordinator(
            response_publisher_callback=lambda: publisher,
            router=SimpleNamespace(route=lambda _event: decision),
            log_callback=lambda _message: None,
        )

        outcome = coordinator.dispatch(event)

        self.assertTrue(outcome.handled)
        metadata = publisher.emission_calls[0][1]
        self.assertTrue(metadata["send_ui"])
        self.assertEqual(
            "Minecraft",
            metadata["presentation_metadata"].badge_label,
        )
        self.assertIn(
            "diamond_pickaxe",
            metadata["presentation_metadata"].detail_log,
        )

    def test_ready_decision_is_resolved_once_after_wait_and_before_publish(self):
        timeline = []
        emission = _typed_emission(
            event_id="f" * 32,
            response_kind="command_coalesced",
            text="감마를 바꿨어",
        )
        publisher = _RecordingPublisher(emission)
        acknowledgement = _ReadyDecisionAcknowledgement(timeline)
        decision = SimpleNamespace(
            handled=True,
            suppress_response=False,
            response_text="감마를 바꿀게",
            publish_external_response=True,
            response_emission_capability=object(),
            response_source="minecraft_chatclef",
            route_kind="command_lifecycle",
            response_kind="command_start",
            response_publication_acknowledgement=acknowledgement,
        )
        coordinator = RoutedInputDispatchCoordinator(
            response_publisher_callback=lambda: publisher,
            router=SimpleNamespace(route=lambda _event: decision),
            log_callback=lambda _message: None,
        )

        outcome = coordinator.dispatch(
            SimpleNamespace(source="lavi_chat_ui", event_id="f" * 32)
        )

        self.assertTrue(outcome.handled)
        self.assertEqual(
            ["wait", "resolve", "ack:True"],
            timeline,
        )
        self.assertEqual(1, acknowledgement.resolve_count)
        published_text, metadata = publisher.emission_calls[0]
        self.assertEqual("감마를 바꿨어", published_text)
        self.assertEqual("command_coalesced", metadata["response_kind"])

    def test_ready_decision_resolver_is_not_called_when_wait_fails(self):
        timeline = []
        acknowledgement = _ReadyDecisionAcknowledgement(
            timeline,
            ready=False,
        )
        decision = SimpleNamespace(
            handled=True,
            response_text="start",
            publish_external_response=True,
            response_publication_acknowledgement=acknowledgement,
        )
        coordinator = RoutedInputDispatchCoordinator(
            response_publisher_callback=lambda: _RecordingPublisher(None),
            router=SimpleNamespace(route=lambda _event: decision),
            log_callback=lambda _message: None,
        )

        outcome = coordinator.dispatch("event")

        self.assertTrue(outcome.suppress_response)
        self.assertEqual(["wait", "ack:False"], timeline)
        self.assertEqual(0, acknowledgement.resolve_count)

    def test_ready_decision_resolver_exception_fails_closed(self):
        timeline = []
        acknowledgement = _ReadyDecisionAcknowledgement(
            timeline,
            fail_resolution=True,
        )
        publisher = _RecordingPublisher(None)
        decision = SimpleNamespace(
            handled=True,
            response_text="start",
            publish_external_response=True,
            response_publication_acknowledgement=acknowledgement,
        )
        coordinator = RoutedInputDispatchCoordinator(
            response_publisher_callback=lambda: publisher,
            router=SimpleNamespace(route=lambda _event: decision),
            log_callback=lambda _message: None,
        )

        outcome = coordinator.dispatch("event")

        self.assertTrue(outcome.suppress_response)
        self.assertEqual(["wait", "resolve", "ack:False"], timeline)
        self.assertEqual([], publisher.emission_calls)

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


class _ReadyDecisionAcknowledgement:
    def __init__(self, timeline, *, ready=True, fail_resolution=False):
        self._timeline = timeline
        self._ready = ready
        self._fail_resolution = fail_resolution
        self.resolve_count = 0

    def wait_until_ready(self, *, timeout_seconds):
        self._timeline.append("wait")
        return self._ready

    def resolve_ready_decision(self, decision):
        self.resolve_count += 1
        self._timeline.append("resolve")
        if self._fail_resolution:
            raise RuntimeError("resolution failed")
        return SimpleNamespace(
            **{
                **vars(decision),
                "response_text": "감마를 바꿨어",
                "response_kind": "command_coalesced",
            }
        )

    def acknowledge(self, *, published):
        self._timeline.append(f"ack:{published}")
        return True


def _typed_emission(
    *,
    event_id,
    response_kind="command_start",
    text="response",
    detail_log="",
):
    return RoutedResponseEmission(
        text=text,
        source="minecraft_chatclef",
        response_generation=3,
        output_delivered=True,
        full_output_delivered=False,
        history_remembered=False,
        event_id=event_id,
        route_kind="command_lifecycle",
        response_kind=response_kind,
        presentation_metadata=RoutedResponsePresentationMetadata.minecraft(
            detail_log=detail_log,
        ),
    )


if __name__ == "__main__":
    unittest.main()
