#20260907_kpopmodder: Lock crafting response publication before correlated terminal delivery.
from __future__ import annotations

import threading
import unittest
from types import SimpleNamespace
from unittest.mock import patch

from llm_core.input_routing import RoutedInputDispatchCoordinator
from llm_core.input_routing.publication import (
    RoutedInputPublicationAcknowledger,
)
from llm_core.routed_response import RoutedResponseEmission
from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.response.crafting_lifecycle import (
    CraftingLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.crafting import (
    CraftingFeedbackAdmissionGrant,
    CraftingFeedbackEffectVerifier,
    CraftingFeedbackPublicationAcknowledgement,
    CraftingFeedbackResultCoordinator,
    CraftingFeedbackServerApi,
    CraftingFeedbackStatusSnapshot,
    CraftingFeedbackTerminalDelivery,
    CraftingFeedbackTerminalListener,
    CraftingFeedbackTracker,
)
from plugins.Minecraft.fabric.chatclef.transport.command_submission.fabric_chatclef_command_result_handler import (
    FabricChatClefCommandResultHandler,
)
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_connection_ownership import (
    FabricChatClefConnectionOwnership,
)


class CraftingFeedbackPublicationOrderingTests(unittest.TestCase):
    def test_concurrent_status_publications_follow_permit_fifo_before_terminal(self):
        runtime = _runtime()
        start_acknowledgement = runtime.api.claim_start(
            runtime.grant,
            _accepted_result(),
        )
        first = runtime.api.inspect_status("diamond_pickaxe")
        second = runtime.api.inspect_status("diamond_pickaxe")
        runtime.handler.handle(runtime.websocket, _envelope(_completed()))
        outcomes = []
        errors = []

        def publish(text, acknowledgement):
            try:
                outcomes.append(
                    _publish(
                        runtime,
                        text=text,
                        acknowledgement=acknowledgement,
                        delivered=True,
                        route_kind="crafting_status_query",
                    )
                )
            except BaseException as error:
                errors.append(error)

        second_thread = threading.Thread(
            target=publish,
            args=("status-two", second.publication_acknowledgement),
        )
        first_thread = threading.Thread(
            target=publish,
            args=("status-one", first.publication_acknowledgement),
        )
        second_thread.start()
        first_thread.start()

        _publish(
            runtime,
            text="start",
            acknowledgement=start_acknowledgement,
            delivered=True,
        )
        first_thread.join(timeout=2)
        second_thread.join(timeout=2)

        self.assertFalse(first_thread.is_alive())
        self.assertFalse(second_thread.is_alive())
        self.assertEqual([], errors)
        self.assertEqual(2, len(outcomes))
        self.assertEqual(
            [
                "start:start",
                "start:status-one",
                "start:status-two",
                "terminal:다이아 곡괭이 다 만들었어",
            ],
            runtime.events,
        )

    def test_terminal_before_start_is_flushed_once_after_start_sink_publication(self):
        runtime = _runtime()
        runtime.handler.handle(runtime.websocket, _envelope(_completed()))

        terminal_first_snapshot = runtime.api.inspect_status("diamond_pickaxe")
        acknowledgement = runtime.api.claim_start(
            runtime.grant,
            _accepted_result(),
        )

        self.assertEqual(
            CraftingFeedbackStatusSnapshot.UNAVAILABLE,
            terminal_first_snapshot.state,
        )
        self.assertIsNone(
            terminal_first_snapshot.publication_acknowledgement
        )
        self.assertIsInstance(
            acknowledgement,
            CraftingFeedbackPublicationAcknowledgement,
        )
        self.assertEqual([], runtime.events)

        outcome = _publish(
            runtime,
            text="다이아 곡괭이 만들어 줄게",
            acknowledgement=acknowledgement,
            delivered=True,
        )

        self.assertTrue(outcome.handled)
        self.assertEqual(
            ["start:다이아 곡괭이 만들어 줄게", "terminal:다이아 곡괭이 다 만들었어"],
            runtime.events,
        )
        self.assertFalse(acknowledgement.acknowledge(published=True))
        self.assertEqual(2, len(runtime.events))

    def test_status_snapshot_linearized_before_terminal_publishes_first(self):
        runtime = _runtime()
        start_acknowledgement = runtime.api.claim_start(
            runtime.grant,
            _accepted_result(),
        )
        _publish(
            runtime,
            text="다이아 곡괭이 만들어 줄게",
            acknowledgement=start_acknowledgement,
            delivered=True,
        )
        runtime.events.clear()
        runtime.handler.handle(runtime.websocket, _envelope(_running(1)))

        snapshot = runtime.api.inspect_status("diamond_pickaxe")
        self.assertEqual(CraftingFeedbackStatusSnapshot.RUNNING, snapshot.state)
        self.assertIsInstance(
            snapshot.publication_acknowledgement,
            CraftingFeedbackPublicationAcknowledgement,
        )

        runtime.handler.handle(runtime.websocket, _envelope(_completed()))
        self.assertEqual([], runtime.events)

        _publish(
            runtime,
            text="다이아 곡괭이 만드는 중이야",
            acknowledgement=snapshot.publication_acknowledgement,
            delivered=True,
            route_kind="crafting_status_query",
        )

        self.assertEqual(
            ["start:다이아 곡괭이 만드는 중이야", "terminal:다이아 곡괭이 다 만들었어"],
            runtime.events,
        )

    def test_failed_start_publication_drops_deferred_terminal_and_cleans_owner(self):
        runtime = _runtime()
        runtime.handler.handle(runtime.websocket, _envelope(_completed()))
        acknowledgement = runtime.api.claim_start(
            runtime.grant,
            _accepted_result(),
        )

        outcome = _publish(
            runtime,
            text="다이아 곡괭이 만들어 줄게",
            acknowledgement=acknowledgement,
            delivered=False,
        )

        self.assertTrue(outcome.handled)
        self.assertTrue(outcome.suppress_response)
        self.assertIsNone(outcome.response)
        self.assertEqual([], runtime.events)
        self.assertIsNone(runtime.tracker.context)
        self.assertTrue(runtime.tracker.reserve(_grant("f" * 32)))
        self.assertFalse(acknowledgement.acknowledge(published=False))

    def test_failed_status_publication_preserves_and_releases_terminal(self):
        runtime = _runtime()
        start_acknowledgement = runtime.api.claim_start(
            runtime.grant,
            _accepted_result(),
        )
        _publish(
            runtime,
            text="start",
            acknowledgement=start_acknowledgement,
            delivered=True,
        )
        runtime.events.clear()
        snapshot = runtime.api.inspect_status("diamond_pickaxe")
        runtime.handler.handle(runtime.websocket, _envelope(_completed()))

        outcome = _publish(
            runtime,
            text="status",
            acknowledgement=snapshot.publication_acknowledgement,
            delivered=False,
            route_kind="crafting_status_query",
        )

        self.assertTrue(outcome.suppress_response)
        self.assertEqual(
            ["terminal:다이아 곡괭이 다 만들었어"],
            runtime.events,
        )
        self.assertTrue(runtime.tracker.reserve(_grant("a" * 32)))

    def test_successful_out_of_order_status_ack_is_rejected_without_blocking_head(self):
        runtime = _runtime()
        start_acknowledgement = runtime.api.claim_start(
            runtime.grant,
            _accepted_result(),
        )
        _publish(
            runtime,
            text="start",
            acknowledgement=start_acknowledgement,
            delivered=True,
        )
        runtime.events.clear()
        first = runtime.api.inspect_status("diamond_pickaxe")
        second = runtime.api.inspect_status("diamond_pickaxe")

        self.assertFalse(
            second.publication_acknowledgement.acknowledge(published=True)
        )
        runtime.handler.handle(runtime.websocket, _envelope(_completed()))
        self.assertEqual([], runtime.events)

        _publish(
            runtime,
            text="status-one",
            acknowledgement=first.publication_acknowledgement,
            delivered=True,
            route_kind="crafting_status_query",
        )

        self.assertEqual(
            [
                "start:status-one",
                "terminal:다이아 곡괭이 다 만들었어",
            ],
            runtime.events,
        )

    def test_retiring_lifecycle_cancels_and_signals_waiting_status_turn(self):
        runtime = _runtime()
        runtime.api.claim_start(runtime.grant, _accepted_result())
        snapshot = runtime.api.inspect_status("diamond_pickaxe")
        entered = threading.Event()
        results = []

        def wait_for_status_turn():
            entered.set()
            results.append(
                snapshot.publication_acknowledgement.wait_until_ready(
                    timeout_seconds=5,
                )
            )

        waiter = threading.Thread(target=wait_for_status_turn)
        waiter.start()
        self.assertTrue(entered.wait(timeout=1))
        with runtime.lock:
            runtime.tracker.clear()
        waiter.join(timeout=1)

        self.assertFalse(waiter.is_alive())
        self.assertEqual([False], results)
        self.assertFalse(
            snapshot.publication_acknowledgement.acknowledge(
                published=False
            )
        )

    def test_waiting_status_turn_times_out_without_invoking_sink(self):
        runtime = _runtime()
        start_acknowledgement = runtime.api.claim_start(
            runtime.grant,
            _accepted_result(),
        )
        _publish(
            runtime,
            text="start",
            acknowledgement=start_acknowledgement,
            delivered=True,
        )
        runtime.events.clear()
        first = runtime.api.inspect_status("diamond_pickaxe")
        second = runtime.api.inspect_status("diamond_pickaxe")

        with patch.object(
            RoutedInputPublicationAcknowledger,
            "TURN_TIMEOUT_SECONDS",
            0.01,
        ):
            outcome = _publish(
                runtime,
                text="must-not-reach-sink",
                acknowledgement=second.publication_acknowledgement,
                delivered=True,
                route_kind="crafting_status_query",
            )

        self.assertTrue(outcome.suppress_response)
        self.assertEqual([], runtime.events)
        runtime.handler.handle(runtime.websocket, _envelope(_completed()))
        _publish(
            runtime,
            text="status-one",
            acknowledgement=first.publication_acknowledgement,
            delivered=True,
            route_kind="crafting_status_query",
        )
        self.assertEqual(
            [
                "start:status-one",
                "terminal:다이아 곡괭이 다 만들었어",
            ],
            runtime.events,
        )

    def test_replayed_out_of_order_or_malformed_evidence_cannot_roll_state_back(self):
        runtime = _runtime()
        tracker = runtime.tracker

        self.assertTrue(
            tracker.record_nonterminal(
                status="running",
                result_reason="dispatch_started",
                evidence_sequence=2,
            )
        )
        for sequence in (2, 1, None, True, 2.0, "3", 0, -1):
            with self.subTest(sequence=sequence):
                self.assertFalse(
                    tracker.record_nonterminal(
                        status="running",
                        result_reason="finish_callback_observed_nonterminal",
                        evidence_sequence=sequence,
                    )
                )
                self.assertEqual(
                    CraftingFeedbackStatusSnapshot.RUNNING,
                    tracker.inspect(
                        active_command=runtime.active,
                        connected=True,
                        quarantine_active=False,
                        target_item="diamond_pickaxe",
                    ).state,
                )

        self.assertTrue(
            tracker.record_nonterminal(
                status="running",
                result_reason="finish_callback_observed_nonterminal",
                evidence_sequence=3,
            )
        )
        self.assertEqual(
            CraftingFeedbackStatusSnapshot.UNAVAILABLE,
            tracker.inspect(
                active_command=runtime.active,
                connected=True,
                quarantine_active=False,
                target_item="diamond_pickaxe",
            ).state,
        )
        self.assertFalse(
            tracker.record_nonterminal(
                status="running",
                result_reason="dispatch_started",
                evidence_sequence=2,
            )
        )
        self.assertEqual(
            CraftingFeedbackStatusSnapshot.UNAVAILABLE,
            tracker.inspect(
                active_command=runtime.active,
                connected=True,
                quarantine_active=False,
                target_item="diamond_pickaxe",
            ).state,
        )


def _runtime():
    lock = _ObservedRLock()
    diagnostics = _Diagnostics()
    tracker = CraftingFeedbackTracker()
    ownership = FabricChatClefConnectionOwnership(
        crafting_feedback_tracker=tracker
    )
    websocket = object()
    if not ownership.try_activate(
        websocket=websocket,
        session_id="session-1",
    ).accepted:
        raise AssertionError("fixture connection failed")
    listener = CraftingFeedbackTerminalListener()
    events = []

    def terminal_callback(response):
        if lock.held:
            raise AssertionError("terminal callback ran under command_lock")
        events.append(f"terminal:{response.text}")

    listener.set_callback(terminal_callback)
    delivery = CraftingFeedbackTerminalDelivery(
        terminal_listener=listener,
        diagnostics=diagnostics,
    )
    api = CraftingFeedbackServerApi(
        connection_ownership=ownership,
        command_lock=lock,
        terminal_listener=listener,
        terminal_delivery=delivery,
    )
    grant = _grant("d" * 32)
    if not api.reserve(grant):
        raise AssertionError("fixture reservation failed")
    with lock:
        active = ownership.begin_command(
            request_id="request-1",
            command_message_id="message-1",
            command="get diamond_pickaxe 1",
            source="lavi_chat_ui",
            metadata=_metadata("d" * 32),
        )
    if active is None:
        raise AssertionError("fixture bind failed")
    coordinator = CraftingFeedbackResultCoordinator(
        tracker=tracker,
        effect_verifier=CraftingFeedbackEffectVerifier(),
        response_renderer=CraftingLifecycleResponseRenderer(),
    )
    handler = FabricChatClefCommandResultHandler(
        connection_ownership=ownership,
        command_lock=lock,
        diagnostics=diagnostics,
        crafting_feedback_result_coordinator=coordinator,
        crafting_feedback_terminal_delivery=delivery,
    )
    return SimpleNamespace(
        active=active,
        api=api,
        diagnostics=diagnostics,
        events=events,
        grant=grant,
        handler=handler,
        lock=lock,
        tracker=tracker,
        websocket=websocket,
    )


def _publish(
    runtime,
    *,
    text,
    acknowledgement,
    delivered,
    route_kind="minecraft_chatclef",
):
    decision = MinecraftChatClefInputRouteDecision.handled_result(
        reason="test",
        response_text=text,
        publish_external_response=True,
        route_kind=route_kind,
        response_publication_acknowledgement=acknowledgement,
    )
    publisher = _Publisher(runtime, delivered=delivered)
    coordinator = RoutedInputDispatchCoordinator(
        response_publisher_callback=lambda: publisher,
        router=SimpleNamespace(route=lambda _event: decision),
        log_callback=runtime.diagnostics.info,
    )
    return coordinator.dispatch(
        SimpleNamespace(source="lavi_chat_ui", event_id="d" * 32)
    )


class _Publisher:
    def __init__(self, runtime, *, delivered):
        self._runtime = runtime
        self._delivered = delivered

    def emit_capability_response(self, text, **_metadata):
        if self._runtime.lock.held:
            raise AssertionError("immediate sink ran under command_lock")
        if self._delivered:
            self._runtime.events.append(f"start:{text}")
        return RoutedResponseEmission(
            text=text,
            source="minecraft_chatclef",
            response_generation=1,
            output_delivered=self._delivered,
            full_output_delivered=False,
            history_remembered=False,
            event_id="d" * 32,
            route_kind="minecraft_chatclef",
            response_kind="immediate",
        )


class _ObservedRLock:
    def __init__(self):
        self._lock = threading.RLock()
        self._local = threading.local()

    @property
    def held(self):
        return getattr(self._local, "depth", 0) > 0

    def __enter__(self):
        self._lock.acquire()
        self._local.depth = getattr(self._local, "depth", 0) + 1
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        self._local.depth -= 1
        self._lock.release()


class _Diagnostics:
    def __init__(self):
        self.infos = []
        self.warnings = []

    def info(self, message):
        self.infos.append(message)

    def warning(self, message):
        self.warnings.append(message)


def _grant(event_id):
    return CraftingFeedbackAdmissionGrant._issue(
        acquisition_verb_class="craft",
        command="get diamond_pickaxe 1",
        command_source="lavi_chat_ui",
        event_id=event_id,
        event_kind="chat_submit",
        input_source="lavi_chat_ui",
        intent_kind="get_item",
        provider_id="lavi_chat_ui",
        requested_count=1,
        spoken_item_label="다이아 곡괭이",
        target_item="diamond_pickaxe",
    )


def _metadata(event_id):
    return {
        "input_event": {
            "source": "lavi_chat_ui",
            "provider_id": "lavi_chat_ui",
            "event_kind": "chat_submit",
            "final": True,
            "event_id": event_id,
        }
    }


def _accepted_result():
    return {
        "ok": True,
        "status": {
            "request_id": "request-1",
            "ok": True,
            "status": "accepted",
            "data": {
                "session_id": "session-1",
                "connection_generation": 1,
                "command_message_id": "message-1",
            },
        },
    }


def _running(sequence):
    return CommandResultDTO(
        request_id="request-1",
        ok=True,
        status=CommandResultStatus.RUNNING,
        data={
            "result_reason": "dispatch_started",
            "evidence_sequence": sequence,
        },
    )


def _completed():
    return CommandResultDTO(
        request_id="request-1",
        ok=True,
        status=CommandResultStatus.COMPLETED,
        data={
            "result_reason": "matching_task_finished",
            "result_fidelity": "callback_plus_matching_user_task_event",
            "effect_kind": "get_acquisition_delta",
            "target_item": "diamond_pickaxe",
            "requested_count": 1,
            "before_target_count": 2,
            "after_target_count": 3,
            "target_count_delta": 1,
            "effect_observation_status": "authoritative",
        },
    )


def _envelope(result):
    return BridgeEnvelopeDTO(
        protocol_version=1,
        message_type=BridgeMessageType.COMMAND_RESULT,
        message_id="result-message-1",
        correlation_id="message-1",
        session_id="session-1",
        timestamp_ms=1,
        payload=result.to_dict(),
    )


if __name__ == "__main__":
    unittest.main()
