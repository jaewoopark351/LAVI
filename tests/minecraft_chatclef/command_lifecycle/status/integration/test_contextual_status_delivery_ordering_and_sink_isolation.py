#20260908_kpopmodder: Prove STATUS ordering and sink isolation with production lifecycle acknowledgements.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from llm_core.event_dispatcher import LLMEventDispatcher
from llm_core.input_routing import RoutedInputDispatchCoordinator
from llm_core.routed_response import RoutedExternalResponsePublisher
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean import (
    TrustedKoreanProofValidator,
)
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.response import (
    TrustedKoreanResponseAuthorizer,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle import (
    CommandStatusQuery,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.publication import (
    CommandStatusPublicationCustodyPolicy,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.diagnostics import (
    CommandStatusPublicationFailureAdapter,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackLifecycleSnapshot,
    CommandFeedbackPublicationAcknowledgement,
)
from tts_core.delivery.lifecycle_response import (
    TtsLifecycleResponseDeliveryAdapter,
)

from .contextual_status_route_harness import ContextualStatusRouteHarness
from .test_fabric_server_graph_status_diagnostic_custody import (
    _accepted_submission_result,
    _bind_ordinary_command,
    _envelope,
    _failed_result,
    _graph,
    _running_result,
    _Diagnostics,
)
from .trusted_status_input_fixture import issue_trusted_status_input


class ContextualStatusDeliveryOrderingAndSinkIsolationTests(unittest.TestCase):
    def test_real_acknowledgement_releases_staged_terminal_at_dispatch_failures(self):
        scenarios = (
            "initial_resolution",
            "external_publication",
        )
        for scenario in scenarios:
            with self.subTest(scenario=scenario):
                runtime = _status_runtime()
                terminal_responses = []
                runtime.graph.command_feedback_api.set_terminal_callback(
                    terminal_responses.append
                )
                runtime.graph.command_result_handler.handle(
                    runtime.websocket,
                    _envelope(
                        runtime.active,
                        _failed_result(runtime.active),
                    ),
                )
                self.assertEqual([], terminal_responses)

                publisher = _RecordingPublisher(
                    error=(
                        LookupError("SECRET")
                        if scenario == "external_publication"
                        else None
                    )
                )
                coordinator = _dispatch_coordinator(
                    runtime.decision,
                    publisher=publisher,
                )
                if scenario == "initial_resolution":
                    coordinator._components.decision_resolver = (
                        _FailingDecisionResolver()
                    )

                outcome = coordinator.dispatch(runtime.event)

                self.assertTrue(outcome.suppress_response)
                self.assertEqual(1, len(terminal_responses))
                self.assertEqual(
                    runtime.descriptor.event_id,
                    terminal_responses[0].event_id,
                )
                self.assertFalse(
                    runtime.acknowledgement.acknowledge(published=False)
                )
                after = runtime.graph.command_feedback_api.inspect_status(
                    CommandStatusQuery("any", True)
                )
                self.assertEqual(CommandFeedbackLifecycleSnapshot.IDLE, after.state)
                self.assertIsNone(after.publication_acknowledgement)

    def test_repeated_status_precedes_one_terminal_without_replaying_start(self):
        runtime = _status_runtime()
        timeline = ["start"]
        runtime.graph.command_feedback_api.set_terminal_callback(
            lambda _response: timeline.append("terminal")
        )
        publisher = _RecordingPublisher(
            on_publish=lambda: timeline.append("status")
        )

        first = _dispatch_coordinator(
            runtime.decision,
            publisher=publisher,
        ).dispatch(runtime.event)
        self.assertTrue(first.handled)

        second_decision = _status_decision(
            runtime,
            event_id="b" * 32,
        )
        second = _dispatch_coordinator(
            second_decision,
            publisher=publisher,
        ).dispatch(runtime.event)
        self.assertTrue(second.handled)

        runtime.graph.command_result_handler.handle(
            runtime.websocket,
            _envelope(runtime.active, _failed_result(runtime.active)),
        )
        runtime.graph.command_result_handler.handle(
            runtime.websocket,
            _envelope(runtime.active, _failed_result(runtime.active)),
        )

        self.assertEqual(["start", "status", "status", "terminal"], timeline)
        self.assertEqual(2, len(publisher.calls))
        after = runtime.graph.command_feedback_api.inspect_status(
            CommandStatusQuery("any", True)
        )
        self.assertEqual(CommandFeedbackLifecycleSnapshot.IDLE, after.state)
        self.assertIsNone(after.publication_acknowledgement)

    def test_terminal_before_status_never_routes_a_late_running_answer(self):
        runtime = _status_runtime(create_status=False)
        terminal_responses = []
        runtime.graph.command_feedback_api.set_terminal_callback(
            terminal_responses.append
        )
        runtime.graph.command_result_handler.handle(
            runtime.websocket,
            _envelope(runtime.active, _failed_result(runtime.active)),
        )

        decision = _status_decision(runtime, event_id="c" * 32)

        self.assertEqual(1, len(terminal_responses))
        self.assertFalse(decision.handled)
        self.assertEqual(
            "command_status_conversational_fallthrough",
            decision.reason,
        )
        self.assertNotEqual("command_status_query", decision.route_kind)
        after = runtime.graph.command_feedback_api.inspect_status(
            CommandStatusQuery("any", True)
        )
        self.assertEqual(CommandFeedbackLifecycleSnapshot.IDLE, after.state)
        self.assertIsNone(after.publication_acknowledgement)

    def test_chat_render_failure_follows_true_output_commit_without_replay(self):
        runtime = _status_runtime()
        output_payloads = []
        event_dispatcher = LLMEventDispatcher()
        event_dispatcher.add_output_event_listener(output_payloads.append)
        publisher = _real_publisher(
            runtime,
            send_output=event_dispatcher.send_output,
        )
        coordinator = _dispatch_coordinator(
            runtime.decision,
            publisher=publisher,
        )
        render_attempts = []

        def fail_render(*_args, **_kwargs):
            render_attempts.append(1)
            raise RuntimeError("SECRET")

        coordinator._components.chat_ui_yield_adapter._ui_adapter.render = (
            fail_render
        )

        first = coordinator.dispatch(runtime.event)
        yielded = coordinator.prepare_response_for_yield(
            runtime.event,
            first.response,
        )
        replay = coordinator.dispatch(runtime.event)

        self.assertTrue(first.handled)
        self.assertEqual(runtime.decision.response_text, yielded)
        self.assertTrue(replay.suppress_response)
        self.assertEqual(1, len(output_payloads))
        self.assertEqual([1], render_attempts)
        self.assertFalse(runtime.acknowledgement.acknowledge(published=True))

    def test_tts_enqueue_and_playback_failures_do_not_redefine_output_commit(self):
        scenarios = ("enqueue", "playback")
        for scenario in scenarios:
            with self.subTest(scenario=scenario):
                runtime = _status_runtime()
                output_payloads = []
                enqueue_receipts = []
                playback_receipts = []
                enqueue_calls = []
                adapter = TtsLifecycleResponseDeliveryAdapter()
                event_dispatcher = LLMEventDispatcher()
                event_dispatcher.add_output_event_listener(output_payloads.append)

                def deliver_to_tts(payload):
                    def enqueue(items, **identity):
                        enqueue_calls.append((items, identity))
                        if scenario == "enqueue":
                            raise RuntimeError("SECRET")
                        return len(items)

                    receipt = adapter.enqueue(
                        event_id=payload["event_id"],
                        route_kind=payload["route_kind"],
                        response_kind=payload["response_kind"],
                        response_generation=payload["response_generation"],
                        delivery_mode=payload["delivery_mode"],
                        items=(payload["text"],),
                        enqueue_callback=enqueue,
                    )
                    enqueue_receipts.append(receipt)
                    if scenario == "playback":
                        identity = enqueue_calls[0][1]
                        playback_receipts.append(
                            adapter.observe_playback(
                                event_id=payload["event_id"],
                                route_kind=payload["route_kind"],
                                response_kind=payload["response_kind"],
                                delivery_token=identity["delivery_token"],
                                item_index=0,
                                played=False,
                                reason="playback_failed",
                            )
                        )

                event_dispatcher.add_output_event_listener(deliver_to_tts)
                publisher = _real_publisher(
                    runtime,
                    send_output=event_dispatcher.send_output,
                )
                coordinator = _dispatch_coordinator(
                    runtime.decision,
                    publisher=publisher,
                )

                first = coordinator.dispatch(runtime.event)
                replay = coordinator.dispatch(runtime.event)

                self.assertTrue(first.handled)
                self.assertTrue(first.response.output_delivered)
                self.assertTrue(replay.suppress_response)
                self.assertEqual(1, len(output_payloads))
                self.assertEqual(1, len(enqueue_calls))
                self.assertEqual(1, len(enqueue_receipts))
                if scenario == "enqueue":
                    self.assertFalse(enqueue_receipts[0].accepted)
                    self.assertEqual("delivery_failed", enqueue_receipts[0].reason)
                    self.assertEqual([], playback_receipts)
                else:
                    self.assertTrue(enqueue_receipts[0].accepted)
                    self.assertEqual(1, len(playback_receipts))
                    self.assertIsNotNone(playback_receipts[0])
                    self.assertFalse(playback_receipts[0].observed)
                    self.assertEqual("playback_failed", playback_receipts[0].reason)
                self.assertFalse(
                    runtime.acknowledgement.acknowledge(published=True)
                )


class _StatusRouter:
    def __init__(self, decision):
        self._decision = decision
        self._policy = CommandStatusPublicationCustodyPolicy()
        self._failure_adapter = CommandStatusPublicationFailureAdapter(
            custody_policy=self._policy,
        )

    def route(self, _event):
        return self._decision

    def claims_routed_input_publication_custody(self, decision):
        return self._policy.matches(decision)

    def observe_routed_input_publication_failure(
        self,
        decision,
        *,
        stage,
        exception_class,
    ):
        self._failure_adapter.observe(
            decision,
            stage=stage,
            exception_class=exception_class,
        )


class _RecordingPublisher:
    def __init__(self, *, error=None, on_publish=None):
        self._error = error
        self._on_publish = on_publish
        self.calls = []

    def emit_capability_response(self, text, **metadata):
        self.calls.append((text, metadata))
        if self._error is not None:
            raise self._error
        if self._on_publish is not None:
            self._on_publish()
        return SimpleNamespace(output_delivered=True)


class _FailingDecisionResolver:
    @staticmethod
    def resolve(_decision):
        raise RuntimeError("SECRET")


def _dispatch_coordinator(decision, *, publisher):
    return RoutedInputDispatchCoordinator(
        response_publisher_callback=lambda: publisher,
        router=_StatusRouter(decision),
        log_callback=lambda _message: None,
    )


def _real_publisher(runtime, *, send_output):
    generations = []
    return RoutedExternalResponsePublisher(
        begin_generation_callback=lambda: (
            generations.append(len(generations) + 1) or generations[-1]
        ),
        build_output_payload_callback=lambda text, generation: {
            "text": text,
            "response_generation": generation,
        },
        send_output_callback=send_output,
        send_full_output_callback=lambda _text: None,
        emission_capability_consumer=(
            runtime.registry.consume_routed_response_emission_capability
        ),
        log_callback=lambda _message: None,
    )


def _status_runtime(*, create_status=True):
    diagnostics = _Diagnostics()
    graph = _graph(diagnostics)
    descriptor, grant, websocket, active = _bind_ordinary_command(graph)
    start_acknowledgement = graph.command_feedback_api.claim_start(
        grant,
        _accepted_submission_result(active),
    )
    if start_acknowledgement is None or not start_acknowledgement.acknowledge(
        published=True
    ):
        raise AssertionError("production START acknowledgement was not accepted")
    graph.command_result_handler.handle(
        websocket,
        _envelope(active, _running_result(active)),
    )
    runtime = SimpleNamespace(
        graph=graph,
        descriptor=descriptor,
        websocket=websocket,
        active=active,
        diagnostics=diagnostics,
        decision=None,
        acknowledgement=None,
        event=None,
        registry=None,
    )
    if create_status:
        runtime.decision = _status_decision(runtime, event_id="a" * 32)
        runtime.acknowledgement = (
            runtime.decision.response_publication_acknowledgement
        )
        if type(runtime.acknowledgement) is not (
            CommandFeedbackPublicationAcknowledgement
        ):
            raise AssertionError("production STATUS acknowledgement was not issued")
    return runtime


def _status_decision(runtime, *, event_id):
    proof_owner = object()
    registry, event, proof = issue_trusted_status_input(
        voice=False,
        event_id=event_id,
        proof_owner=proof_owner,
    )
    harness = ContextualStatusRouteHarness(
        proof_validator=TrustedKoreanProofValidator(owner=proof_owner).is_live,
        status_inspector=runtime.graph.command_feedback_api.inspect_status,
    )
    try:
        decision = harness.route(
            event=event,
            proof=proof,
            descriptor=runtime.descriptor,
        )
        if decision.handled:
            decision = TrustedKoreanResponseAuthorizer(
                owner=proof_owner
            ).authorize(
                decision=decision,
                event=event,
                proof=proof,
            )
    finally:
        proof.close()
    runtime.event = event
    runtime.registry = registry
    return decision


if __name__ == "__main__":
    unittest.main()
