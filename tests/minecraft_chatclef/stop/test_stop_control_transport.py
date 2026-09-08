# 20260905_kpopmodder: Verify STOP admission, barrier, wire profile, and terminal demux.
import asyncio
import threading
import unittest
from types import SimpleNamespace

from input_core.input_event.contracts.lavi_input_event import LaviInputEvent
from plugins.Minecraft.fabric.chatclef.input.eligibility import (
    KoreanChatMicrophoneEligibilityProof,
)
from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO
from plugins.Minecraft.common.protocol.bridge_message_type import BridgeMessageType
from plugins.Minecraft.fabric.chatclef.input.stop import StopControlClaimRegistry
from plugins.Minecraft.fabric.chatclef.transport.control.stop import (
    StopControlAdmissionBarrier,
    StopControlResultDemultiplexer,
    StopControlSubmitter,
    StopControlTerminalListener,
    StopControlTransitionLogger,
    StopControlTrackerRegistry,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.result_handling.stop_control_result_handling_component_graph import (
    StopControlResultHandlingComponentGraph,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.stop_control_submission_component_graph import (
    StopControlSubmissionComponentGraph,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.send_outcome import (
    StopControlSendDiagnosticStage,
    StopControlSendResultStage,
    StopControlSendStateTransitionStage,
    StopControlTransportDeliveryStage,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.submission.send_outcome.stop_control_send_outcome_component_graph import (
    StopControlSendOutcomeComponentGraph,
)
from plugins.Minecraft.fabric.chatclef.transport.fabric_chatclef_connection_ownership import (
    FabricChatClefConnectionOwnership,
)


class _ConsumedEvidence:
    def __init__(self, event, owner):
        self.event = event
        self.owner = owner
        self.open = True

    def is_live_for(self, event, owner):
        return self.open and event is self.event and owner is self.owner

    def close(self):
        self.open = False


class _Connection:
    def __init__(self):
        self.active_websocket = object()
        self.active_session_id = "session-a"
        self.active_generation = 7
        self.cleared = []
        self.active_command_owner = SimpleNamespace(
            request_id="ordinary-a",
            command_message_id="message-a",
            session_id="session-a",
            generation=7,
        )

    def is_connected(self):
        return True

    def is_active_websocket(self, websocket):
        return self.active_websocket is websocket

    def local_admission_snapshot(self):
        return {
            "active_request_id": "ordinary-a",
            "active_command_message_id": "message-a",
            "active_session_id": "session-a",
            "active_generation": 7,
        }

    def clear_command_if_identity(self, **identity):
        self.cleared.append(identity)
        return identity.get("owner_token") is self.active_command_owner


class _PostCommitTransportSetupFailure:
    def __init__(self):
        self.envelopes = []

    def send(self, _websocket, _envelope):
        raise RuntimeError("post-commit transport setup failed")


class _PrecommitWebSocketFailureConnection(_Connection):
    def __getattribute__(self, name):
        if name == "active_websocket":
            raise RuntimeError("precommit websocket lookup failed")
        return super().__getattribute__(name)


class _RegisterThenRaiseTrackerRegistry(StopControlTrackerRegistry):
    def register(self, tracker):
        super().register(tracker)
        raise RuntimeError("tracker registration outcome unknown")


class _Loop:
    def is_running(self):
        return True


class _Future:
    def result(self, timeout=None):
        return None


class _Transport:
    def __init__(self):
        self.envelopes = []

    async def send(self, websocket, envelope):
        self.envelopes.append((websocket, envelope))


class _Diagnostics:
    def __init__(self):
        self.infos = []
        self.warnings = []

    def info(self, message):
        self.infos.append(message)

    def warning(self, message):
        self.warnings.append(message)


class _ReconnectAfterCommitDiagnostics(_Diagnostics):
    def __init__(self, connection):
        super().__init__()
        self._connection = connection

    def info(self, message):
        super().info(message)
        if "diagnostic_disposition=python_admission_committed" in message:
            self._connection.active_websocket = object()
            self._connection.active_session_id = "session-b"
            self._connection.active_generation = 8


class StopControlTransportTests(unittest.TestCase):
    def setUp(self):
        self.event = LaviInputEvent(
            text="멈춰",
            source="lavi_chat_ui",
            event_id="a" * 32,
            event_kind="chat_submit",
            final=True,
            provider_id="lavi_chat_ui",
            fallback_payload="멈춰",
        )
        self.claim_owner = object()
        self.proof = _proof(self.event, self.claim_owner)
        self.claims = _claim_registry(self.claim_owner)
        self.receipt, _ = self.claims.issue(
            event=self.event,
            eligibility_proof=self.proof,
            normalized_phrase="멈춰",
        )
        self.barrier = StopControlAdmissionBarrier()
        self.trackers = StopControlTrackerRegistry()
        self.connection = _Connection()
        self.transport = _Transport()
        self.listener = StopControlTerminalListener()
        self.published = []
        self.listener.set_callback(self.published.append)
        self.diagnostics = _Diagnostics()
        self.lock = __import__("threading").RLock()
        session = SimpleNamespace(
            session_id="session-a",
            capabilities={"chatclef_stop_control_v1": True},
        )
        self.sessions = SimpleNamespace(active_session=lambda: session)

    def test_legacy_facades_delegate_to_exact_component_graphs(self):
        submitter = self._submitter()
        demultiplexer = self._demux()

        self.assertIsInstance(
            submitter._component_graph,
            StopControlSubmissionComponentGraph,
        )
        self.assertIs(
            submitter._coordinator,
            submitter._component_graph.coordinator,
        )
        self.assertIsInstance(
            demultiplexer._component_graph,
            StopControlResultHandlingComponentGraph,
        )
        self.assertIs(
            demultiplexer._coordinator,
            demultiplexer._component_graph.coordinator,
        )

        send_coordinator = submitter._coordinator._send_outcome_coordinator
        send_graph = send_coordinator._component_graph
        self.assertIsInstance(send_graph, StopControlSendOutcomeComponentGraph)
        self.assertIsInstance(
            send_graph.transport_delivery,
            StopControlTransportDeliveryStage,
        )
        self.assertIsInstance(
            send_graph.state_transitions,
            StopControlSendStateTransitionStage,
        )
        self.assertIsInstance(
            send_graph.diagnostics,
            StopControlSendDiagnosticStage,
        )
        self.assertIsInstance(send_graph.results, StopControlSendResultStage)

    def test_submit_closes_barrier_and_builds_exact_profile(self):
        submitter = self._submitter()
        outcome = submitter.submit(
            event=self.event,
            eligibility_proof=self.proof,
            receipt=self.receipt,
        )
        self.assertTrue(outcome.accepted)
        self.assertTrue(self.barrier.closed)
        envelope = self.transport.envelopes[0][1]
        metadata = envelope.payload["metadata"]
        self.assertEqual(metadata["request_kind"], "stop_control_v1")
        self.assertEqual(metadata["operation"], "stop_ai")
        self.assertEqual(metadata["target_scope"], "tracked_command")
        self.assertEqual(metadata["input_event"]["event_id"], "a" * 32)
        fields = _transition_fields(self.diagnostics.infos[-1])
        self.assertEqual(
            set(fields),
            set(StopControlTransitionLogger.CANONICAL_FIELDS),
        )
        self.assertEqual(
            fields["diagnostic_disposition"],
            "control_send_accepted",
        )
        self.assertEqual(fields["python_stop_barrier_state"], "closed")
        self.assertEqual(fields["quarantine_active"], "false")

    def test_matching_completed_result_releases_barrier_and_publishes_once(self):
        self._submitter().submit(
            event=self.event,
            eligibility_proof=self.proof,
            receipt=self.receipt,
        )
        tracker = self.trackers.current()
        result_data = {
            "request_kind": "stop_control_v1",
            "operation": "stop_ai",
            "connection_generation": 7,
            "java_socket_generation": 3,
            "control_outcome": "stopped",
            "control_reason": "tracked_active_stopped",
            "target_scope": "tracked_command",
            "target_resolution": "exact",
            "target_state_before": "active",
            "target_state_after": "retired",
            "original_result_delivery": "sent",
            "stop_command_invoked": True,
            "executed_client_tick": 10,
            "verified_client_tick": 11,
            "requested_target_request_id": "ordinary-a",
            "requested_target_command_message_id": "message-a",
            "requested_target_session_id": "session-a",
            "requested_target_server_connection_generation": 7,
            "resolved_target_request_id": "ordinary-a",
            "resolved_target_command_message_id": "message-a",
            "resolved_target_session_id": "session-a",
            "resolved_target_server_connection_generation": 7,
        }
        envelope = BridgeEnvelopeDTO(
            protocol_version=1,
            message_type=BridgeMessageType.COMMAND_RESULT,
            message_id="java-result",
            correlation_id=tracker.identity.message_id,
            session_id="session-a",
            timestamp_ms=1,
            payload={
                "request_id": tracker.identity.request_id,
                "ok": True,
                "status": "completed",
                "error_code": None,
                "message": "stopped",
                "data": result_data,
            },
        )
        demux = StopControlResultDemultiplexer(
            command_lock=self.lock,
            connection_ownership=self.connection,
            tracker_registry=self.trackers,
            admission_barrier=self.barrier,
            terminal_listener=self.listener,
            diagnostics=self.diagnostics,
        )
        self.assertTrue(demux.handle(self.connection.active_websocket, envelope))
        self.assertFalse(self.barrier.closed)
        self.assertIsNone(self.trackers.current())
        self.assertEqual(
            [response.text for response in self.published],
            ["멈췄어"],
        )
        self.assertEqual(self.published[0].event_id, "a" * 32)
        self.assertEqual(self.published[0].route_kind, "stop_control")
        self.assertEqual(self.published[0].response_kind, "stop_terminal")
        self.assertTrue(demux.handle(self.connection.active_websocket, envelope))
        self.assertEqual(len(self.published), 1)
        fields = _transition_fields(self.diagnostics.infos[-1])
        self.assertEqual(fields["control_status"], "completed")
        self.assertEqual(fields["control_outcome"], "stopped")
        self.assertEqual(fields["control_reason"], "tracked_active_stopped")
        self.assertEqual(fields["target_resolution"], "exact")
        self.assertEqual(fields["requested_target_request_id"], "ordinary-a")
        self.assertEqual(fields["resolved_target_request_id"], "ordinary-a")
        self.assertEqual(fields["executed_client_tick"], "10")
        self.assertEqual(fields["verified_client_tick"], "11")
        self.assertEqual(fields["python_stop_barrier_state"], "released")
        self.assertEqual(fields["ordinary_owner_gate_state"], "open")
        self.assertEqual(fields["quarantine_active"], "false")
        self.assertEqual(
            fields["retirement_evidence_kind"],
            "verified_stopped",
        )
        self.assertEqual(fields["frozen_python_owner_exact_match"], "true")

    def test_send_unknown_quarantine_absorbs_late_completed_result(self):
        submitter = self._submitter(
            future_scheduler=lambda coroutine, loop: _UnknownFuture(coroutine),
        )
        outcome = submitter.submit(
            event=self.event,
            eligibility_proof=self.proof,
            receipt=self.receipt,
        )
        self.assertEqual(outcome.reason, "control_send_unknown")
        tracker = self.trackers.current()
        self.assertTrue(tracker.quarantined)
        envelope = _completed_envelope(tracker)
        demux = self._demux()
        self.assertTrue(demux.handle(self.connection.active_websocket, envelope))
        self.assertIs(self.trackers.current(), tracker)
        self.assertTrue(self.barrier.closed)
        self.assertEqual(self.connection.cleared, [])
        self.assertEqual(self.published, [])
        fields = _transition_fields(self.diagnostics.infos[-1])
        self.assertEqual(fields["control_result_delivery"], "unknown")
        self.assertEqual(fields["python_stop_barrier_state"], "closed")
        self.assertEqual(fields["quarantine_active"], "true")
        self.assertEqual(
            fields["retirement_evidence_kind"],
            "unknown_quarantine",
        )

    def test_terminal_release_wins_timeout_observation_race(self):
        released_trackers = []

        def schedule(coroutine, _loop):
            coroutine.close()

            def publish_terminal_result():
                tracker = self.trackers.current()
                released_trackers.append(tracker)
                self.assertTrue(
                    self._demux().handle(
                        self.connection.active_websocket,
                        _completed_envelope(tracker),
                    )
                )

            return _CallbackTimeoutFuture(publish_terminal_result)

        outcome = self._submitter(future_scheduler=schedule).submit(
            event=self.event,
            eligibility_proof=self.proof,
            receipt=self.receipt,
        )

        self.assertTrue(outcome.accepted)
        self.assertEqual(outcome.reason, "accepted")
        self.assertEqual(len(released_trackers), 1)
        self.assertTrue(released_trackers[0].released)
        self.assertFalse(released_trackers[0].quarantined)
        self.assertIsNone(self.trackers.current())
        self.assertFalse(self.barrier.closed)
        self.assertEqual(
            [response.text for response in self.published],
            ["멈췄어"],
        )

    def test_terminal_release_wins_not_scheduled_retirement_race(self):
        def publish_after_first_send_status_observation():
            tracker = self.trackers.current()
            self.assertIsNotNone(tracker)
            self.assertTrue(
                self._demux().handle(
                    self.connection.active_websocket,
                    _completed_envelope(tracker),
                )
            )

        self.lock = _AfterNthExitLock(
            publish_after_first_send_status_observation,
            exit_number=3,
        )

        def reject_schedule(_coroutine, _loop):
            raise RuntimeError("not scheduled")

        outcome = self._submitter(
            future_scheduler=reject_schedule,
        ).submit(
            event=self.event,
            eligibility_proof=self.proof,
            receipt=self.receipt,
        )

        self.assertTrue(outcome.accepted)
        self.assertEqual("accepted", outcome.reason)
        self.assertIsNone(self.trackers.current())
        self.assertFalse(self.barrier.closed)
        self.assertEqual(
            [response.text for response in self.published],
            ["멈췄어"],
        )

    def test_wire_unknown_quarantine_absorbs_late_completed_result(self):
        self._submitter().submit(
            event=self.event,
            eligibility_proof=self.proof,
            receipt=self.receipt,
        )
        tracker = self.trackers.current()
        unknown = _completed_envelope(tracker)
        unknown.payload.update(
            {
                "ok": False,
                "status": "unknown",
                "error_code": "internal_error",
            }
        )
        unknown.payload["data"].update(
            {
                "control_outcome": "unknown",
                "control_reason": "stop_command_exception",
                "target_state_after": "unknown",
                "original_result_delivery": "not_applicable",
                "verified_client_tick": 10,
            }
        )
        demux = self._demux()
        self.assertTrue(demux.handle(self.connection.active_websocket, unknown))
        self.assertTrue(tracker.quarantined)
        self.assertTrue(
            demux.handle(
                self.connection.active_websocket,
                _completed_envelope(tracker),
            )
        )
        self.assertIs(self.trackers.current(), tracker)
        self.assertTrue(self.barrier.closed)
        self.assertEqual(self.connection.cleared, [])
        self.assertEqual(
            [response.text for response in self.published],
            ["중지 결과를 확인하지 못했어요. 자동으로 다시 보내지 않아요."],
        )

    def test_submitter_rejects_receipt_from_non_authoritative_registry(self):
        foreign = _claim_registry(self.claim_owner)
        foreign_receipt, _ = foreign.issue(
            event=self.event,
            eligibility_proof=self.proof,
            normalized_phrase="멈춰",
        )
        outcome = self._submitter().submit(
            event=self.event,
            eligibility_proof=self.proof,
            receipt=foreign_receipt,
        )
        self.assertFalse(outcome.accepted)
        self.assertEqual(outcome.reason, "foreign_stop_claim_receipt")
        self.assertFalse(self.barrier.closed)
        self.assertIsNone(self.trackers.current())
        self.assertEqual(self.transport.envelopes, [])

    def test_frozen_owner_identity_cannot_clear_reused_newer_owner(self):
        ownership = FabricChatClefConnectionOwnership()
        websocket = object()
        ownership.try_activate(websocket=websocket, session_id="session-a")
        original = ownership.begin_command(
            request_id="ordinary-a",
            command_message_id="message-a",
        )
        self.assertTrue(ownership.clear_command_if_current(original))
        replacement = ownership.begin_command(
            request_id="ordinary-a",
            command_message_id="message-a",
        )
        self.assertIsNot(replacement, original)
        self.assertFalse(
            ownership.clear_command_if_identity(
                request_id="ordinary-a",
                command_message_id="message-a",
                session_id="session-a",
                server_connection_generation=1,
                owner_token=original,
            )
        )
        self.assertIs(ownership.active_command_owner, replacement)

    def test_post_commit_transport_setup_failure_is_unknown_and_quarantined(self):
        self.transport = _PostCommitTransportSetupFailure()
        outcome = self._submitter().submit(
            event=self.event,
            eligibility_proof=self.proof,
            receipt=self.receipt,
        )
        tracker = self.trackers.current()
        self.assertFalse(outcome.accepted)
        self.assertEqual(outcome.reason, "control_send_unknown")
        self.assertIsNotNone(tracker)
        self.assertTrue(tracker.quarantined)
        self.assertTrue(self.barrier.closed)
        self.assertEqual(self.transport.envelopes, [])

    def test_precommit_transport_snapshot_failure_is_definite_rejection(self):
        self.connection = _PrecommitWebSocketFailureConnection()
        outcome = self._submitter().submit(
            event=self.event,
            eligibility_proof=self.proof,
            receipt=self.receipt,
        )
        self.assertFalse(outcome.accepted)
        self.assertEqual(outcome.reason, "control_send_rejected")
        self.assertIsNone(self.trackers.current())
        self.assertFalse(self.barrier.closed)
        self.assertEqual(self.transport.envelopes, [])

    def test_rejected_before_write_retires_tracker_and_releases_barrier(self):
        def reject_schedule(_coroutine, _loop):
            raise RuntimeError("not scheduled")

        outcome = self._submitter(
            future_scheduler=reject_schedule,
        ).submit(
            event=self.event,
            eligibility_proof=self.proof,
            receipt=self.receipt,
        )
        self.assertFalse(outcome.accepted)
        self.assertEqual(outcome.reason, "control_send_rejected")
        self.assertIsNone(self.trackers.current())
        self.assertFalse(self.barrier.closed)
        fields = _transition_fields(self.diagnostics.infos[-1])
        self.assertEqual(
            fields["diagnostic_disposition"],
            "control_send_rejected_before_write",
        )
        self.assertEqual(fields["python_stop_barrier_state"], "released")
        self.assertEqual(
            fields["retirement_evidence_kind"],
            "rejected_before_write",
        )

    def test_register_then_raise_is_unknown_and_keeps_barrier_quarantined(self):
        self.trackers = _RegisterThenRaiseTrackerRegistry()
        outcome = self._submitter().submit(
            event=self.event,
            eligibility_proof=self.proof,
            receipt=self.receipt,
        )
        tracker = self.trackers.current()
        self.assertFalse(outcome.accepted)
        self.assertEqual(outcome.reason, "control_send_unknown")
        self.assertIsNotNone(tracker)
        self.assertTrue(tracker.quarantined)
        self.assertTrue(self.barrier.closed)
        self.assertEqual(self.transport.envelopes, [])
        fields = _transition_fields(self.diagnostics.infos[-1])
        self.assertEqual(
            fields["diagnostic_disposition"],
            "tracker_registration_outcome_unknown",
        )

    def test_reconnect_before_write_rejects_and_releases_exact_tracker(self):
        self.diagnostics = _ReconnectAfterCommitDiagnostics(self.connection)
        outcome = self._submitter().submit(
            event=self.event,
            eligibility_proof=self.proof,
            receipt=self.receipt,
        )
        self.assertFalse(outcome.accepted)
        self.assertEqual(outcome.reason, "control_send_rejected")
        self.assertIsNone(self.trackers.current())
        self.assertFalse(self.barrier.closed)
        self.assertEqual(self.transport.envelopes, [])
        fields = _transition_fields(self.diagnostics.infos[-1])
        self.assertEqual(
            fields["diagnostic_disposition"],
            "connection_changed_before_write",
        )
        self.assertEqual(fields["python_stop_barrier_state"], "released")

    def test_send_uses_frozen_socket_if_connection_changes_after_prewrite_check(self):
        frozen_websocket = self.connection.active_websocket
        replacement_websocket = object()

        def reconnect_then_run(coroutine, _loop):
            self.connection.active_websocket = replacement_websocket
            self.connection.active_session_id = "session-b"
            self.connection.active_generation = 8
            return self._run(coroutine)

        outcome = self._submitter(
            future_scheduler=reconnect_then_run,
        ).submit(
            event=self.event,
            eligibility_proof=self.proof,
            receipt=self.receipt,
        )
        self.assertTrue(outcome.accepted)
        self.assertIs(self.transport.envelopes[0][0], frozen_websocket)
        self.assertIsNot(self.transport.envelopes[0][0], replacement_websocket)

    def test_stale_websocket_cannot_release_live_tracker_or_owner(self):
        self._submitter().submit(
            event=self.event,
            eligibility_proof=self.proof,
            receipt=self.receipt,
        )
        tracker = self.trackers.current()
        envelope = _completed_envelope(tracker)
        envelope.payload["data"].pop("request_kind")
        envelope.payload["data"].pop("operation")
        self.assertTrue(self._demux().handle(object(), envelope))
        self.assertIs(self.trackers.current(), tracker)
        self.assertTrue(self.barrier.closed)
        self.assertEqual(self.connection.cleared, [])
        self.assertEqual(self.published, [])

    def test_matching_top_level_identity_owns_partial_unmarked_result(self):
        self._submitter().submit(
            event=self.event,
            eligibility_proof=self.proof,
            receipt=self.receipt,
        )
        tracker = self.trackers.current()
        envelope = _completed_envelope(tracker)
        envelope.payload["data"].pop("request_kind")
        envelope.payload["data"].pop("operation")
        envelope.payload["data"].pop("connection_generation")

        self.assertTrue(
            self._demux().handle(self.connection.active_websocket, envelope)
        )
        self.assertIs(self.trackers.current(), tracker)
        self.assertTrue(tracker.quarantined)
        self.assertTrue(self.barrier.closed)
        self.assertEqual(self.connection.cleared, [])

    def test_unmarked_top_level_mismatch_remains_for_ordinary_demux(self):
        self._submitter().submit(
            event=self.event,
            eligibility_proof=self.proof,
            receipt=self.receipt,
        )
        tracker = self.trackers.current()
        envelope = _completed_envelope(tracker)
        envelope.payload["request_id"] = "ordinary-request"
        envelope.payload["data"].pop("request_kind")
        envelope.payload["data"].pop("operation")

        self.assertFalse(
            self._demux().handle(self.connection.active_websocket, envelope)
        )
        self.assertIs(self.trackers.current(), tracker)
        self.assertFalse(tracker.quarantined)
        self.assertTrue(self.barrier.closed)

    def test_live_session_and_generation_are_result_acceptance_fences(self):
        for field, replacement in (
            ("active_session_id", "session-b"),
            ("active_generation", 8),
        ):
            with self.subTest(field=field):
                self.setUp()
                self._submitter().submit(
                    event=self.event,
                    eligibility_proof=self.proof,
                    receipt=self.receipt,
                )
                tracker = self.trackers.current()
                setattr(self.connection, field, replacement)
                self.assertTrue(
                    self._demux().handle(
                        self.connection.active_websocket,
                        _completed_envelope(tracker),
                    )
                )
                self.assertIs(self.trackers.current(), tracker)
                self.assertTrue(self.barrier.closed)
                self.assertEqual(self.connection.cleared, [])
                self.assertEqual(self.published, [])

    def _submitter(self, future_scheduler=None):
        return StopControlSubmitter(
            connection_ownership=self.connection,
            command_lock=self.lock,
            session_registry=self.sessions,
            claim_registry=self.claims,
            admission_barrier=self.barrier,
            tracker_registry=self.trackers,
            diagnostics=self.diagnostics,
            loop_provider=lambda: _Loop(),
            envelope_transport=self.transport,
            send_timeout_sec=1,
            future_scheduler=(
                future_scheduler or (lambda coroutine, loop: self._run(coroutine))
            ),
            message_id_factory=lambda: "m" * 32,
            request_id_factory=lambda: "r" * 32,
            now_ms=lambda: 1000,
        )

    def _demux(self):
        return StopControlResultDemultiplexer(
            command_lock=self.lock,
            connection_ownership=self.connection,
            tracker_registry=self.trackers,
            admission_barrier=self.barrier,
            terminal_listener=self.listener,
            diagnostics=self.diagnostics,
        )

    def _run(self, coroutine):
        asyncio.run(coroutine)
        return _Future()


class _UnknownFuture:
    def __init__(self, coroutine):
        coroutine.close()

    def result(self, timeout=None):
        raise TimeoutError("delivery outcome unknown")


class _CallbackTimeoutFuture:
    def __init__(self, callback):
        self._callback = callback

    def result(self, timeout=None):
        self._callback()
        raise TimeoutError("terminal raced send observation")


class _AfterNthExitLock:
    def __init__(self, callback, *, exit_number):
        self._lock = threading.RLock()
        self._callback = callback
        self._exit_number = exit_number
        self._exit_count = 0

    def __enter__(self):
        self._lock.acquire()
        return self

    def __exit__(self, _error_type, _error, _traceback):
        self._lock.release()
        self._exit_count += 1
        if self._exit_count == self._exit_number:
            self._callback()


def _proof(event, owner):
    return KoreanChatMicrophoneEligibilityProof._issue(
        event=event,
        consumed_evidence=_ConsumedEvidence(event, owner),
        owner=owner,
    )


def _claim_registry(owner):
    registry = StopControlClaimRegistry()
    registry._bind_claim_owner(owner)
    return registry


def _completed_envelope(tracker):
    target = tracker.target
    return BridgeEnvelopeDTO(
        protocol_version=1,
        message_type=BridgeMessageType.COMMAND_RESULT,
        message_id="java-result",
        correlation_id=tracker.identity.message_id,
        session_id=tracker.identity.session_id,
        timestamp_ms=1,
        payload={
            "request_id": tracker.identity.request_id,
            "ok": True,
            "status": "completed",
            "error_code": None,
            "message": "stopped",
            "data": {
                "request_kind": "stop_control_v1",
                "operation": "stop_ai",
                "connection_generation": tracker.identity.server_connection_generation,
                "java_socket_generation": 3,
                "control_outcome": "stopped",
                "control_reason": "tracked_active_stopped",
                "target_scope": "tracked_command",
                "target_resolution": "exact",
                "target_state_before": "active",
                "target_state_after": "retired",
                "original_result_delivery": "sent",
                "stop_command_invoked": True,
                "executed_client_tick": 10,
                "verified_client_tick": 11,
                "requested_target_request_id": target.request_id,
                "requested_target_command_message_id": target.command_message_id,
                "requested_target_session_id": target.session_id,
                "requested_target_server_connection_generation": target.server_connection_generation,
                "resolved_target_request_id": target.request_id,
                "resolved_target_command_message_id": target.command_message_id,
                "resolved_target_session_id": target.session_id,
                "resolved_target_server_connection_generation": target.server_connection_generation,
            },
        },
    )


def _transition_fields(message):
    parts = message.split()
    if not parts or parts[0] != "event=stop_control_transition":
        raise AssertionError(f"not a STOP transition log: {message!r}")
    return dict(part.split("=", 1) for part in parts[1:])


if __name__ == "__main__":
    unittest.main()
