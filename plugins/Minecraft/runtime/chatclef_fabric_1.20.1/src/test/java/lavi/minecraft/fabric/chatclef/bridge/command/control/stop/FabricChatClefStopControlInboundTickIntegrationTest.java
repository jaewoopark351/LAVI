package lavi.minecraft.fabric.chatclef.bridge.command.control.stop;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.FabricChatClefStopCommandExecutor;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.FabricChatClefStopControlCommandLifecycle;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.FabricChatClefStopControlTickDispatcher;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlAdmissionBarrier;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlDedupeRegistry;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlIdentity;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefOriginalCancellationResultFactory;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultOutbox;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendStatus;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.FabricChatClefInboundMessageHandler;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.FabricChatClefStopControlInboundHandler;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260905_kpopmodder: Exercise raw callback validation through queue, client tick, retirement, and SENT release.
class FabricChatClefStopControlInboundTickIntegrationTest {
    @Test
    void noContextInvokesStopOnceAndWaitsForControlResultSentBeforeRelease() {
        Fixture fixture = new Fixture();
        fixture.submitGlobal("stop-global", "message-global");

        assertTrue(fixture.tick(100L));
        assertEquals(1, fixture.stopExecutor.invocations.get());
        assertEquals(1, fixture.stopSender.payloads.size());
        assertControlResult(
                fixture.stopSender.payloads.get(0),
                "completed",
                "global_stop_executed_no_lavi_context",
                "none",
                "none",
                "none"
        );
        assertEquals(41L, data(fixture.stopSender.payloads.get(0)).get("connection_generation"));
        assertEquals(7L, fixture.stopSender.javaSocketGenerations.get(0));
        assertEquals(100L, data(fixture.stopSender.payloads.get(0)).get("executed_client_tick"));
        assertEquals(100L, data(fixture.stopSender.payloads.get(0)).get("verified_client_tick"));
        assertTrue(fixture.barrier.blocksOrdinary());
        assertTrue(fixture.tick(101L));
        assertEquals(1, fixture.stopExecutor.invocations.get());

        fixture.stopSender.completeLatest(FabricChatClefCommandResultSendOutcome.sent());

        assertFalse(fixture.tick(102L));
        assertFalse(fixture.barrier.blocksOrdinary());
        assertTrue(fixture.stopQueue.active().isEmpty());
    }

    @Test
    void pendingContextIsNeverDispatchedAndRetiresAsCancelledBeforeCompletedStopResult() {
        Fixture fixture = new Fixture();
        FabricChatClefCommandContext pending = fixture.ordinary("ordinary-pending", "ordinary-message");
        assertTrue(fixture.ordinaryQueue.offer(pending));
        fixture.submitTracked("stop-pending", "stop-message", pending);

        assertTrue(fixture.tick(200L));
        assertEquals(1, fixture.stopExecutor.invocations.get());
        assertTrue(fixture.ordinaryQueue.peekPending().isEmpty());
        assertFalse(fixture.ordinaryQueue.hasActive());
        assertEquals(1, fixture.lifecycle.originalResults.size());
        assertOriginalCancellation(fixture.lifecycle.originalResults.get(0));
        assertEquals(0, fixture.stopSender.payloads.size());

        assertTrue(fixture.tick(201L));
        assertEquals(1, fixture.stopSender.payloads.size());
        assertControlResult(
                fixture.stopSender.payloads.get(0),
                "completed",
                "tracked_pending_stopped",
                "exact",
                "pending",
                "retired"
        );
        assertTrue(fixture.barrier.blocksOrdinary());

        fixture.stopSender.completeLatest(FabricChatClefCommandResultSendOutcome.sent());
        assertFalse(fixture.tick(202L));
        assertEquals(1, fixture.stopExecutor.invocations.get());
    }

    @Test
    void activeContextBindsMarkerBeforeOneStopAndRequiresRootRetirement() {
        Fixture fixture = new Fixture();
        FabricChatClefCommandContext active = fixture.ordinary("ordinary-active", "ordinary-active-message");
        assertTrue(fixture.ordinaryQueue.offer(active));
        assertSame(active, fixture.ordinaryQueue.pollForDispatch().orElseThrow());
        fixture.currentRoot.set(new DummyTask("owned-root"));
        fixture.submitGlobal("stop-active", "stop-active-message");

        assertTrue(fixture.tick(300L));
        assertTrue(fixture.lifecycle.markerBound);
        assertEquals(1, fixture.stopExecutor.invocations.get());
        assertFalse(fixture.ordinaryQueue.hasActive());
        assertOriginalCancellation(fixture.lifecycle.originalResults.get(0));

        assertTrue(fixture.tick(301L));
        assertControlResult(
                fixture.stopSender.payloads.get(0),
                "completed",
                "global_active_stopped",
                "captured_current",
                "active",
                "retired"
        );
        fixture.stopSender.completeLatest(FabricChatClefCommandResultSendOutcome.sent());
        assertFalse(fixture.tick(302L));
        assertEquals(1, fixture.stopExecutor.invocations.get());
    }

    @Test
    void activeMarkerFailureIsTruthfulUnknownAndSentDoesNotReleaseBarrier() {
        Fixture fixture = new Fixture();
        FabricChatClefCommandContext active = fixture.ordinary("ordinary-active", "ordinary-active-message");
        fixture.ordinaryQueue.offer(active);
        fixture.ordinaryQueue.pollForDispatch();
        fixture.currentRoot.set(new DummyTask("owned-root"));
        fixture.lifecycle.allowMarkerBind = false;
        fixture.submitTracked("stop-marker-fail", "stop-marker-message", active);

        assertTrue(fixture.tick(400L));
        assertEquals(1, fixture.stopExecutor.invocations.get());
        assertEquals(0, fixture.lifecycle.originalResults.size());
        assertControlResult(
                fixture.stopSender.payloads.get(0),
                "unknown",
                "user_stop_marker_bind_failed",
                "exact",
                "active",
                "unknown"
        );

        fixture.stopSender.completeLatest(FabricChatClefCommandResultSendOutcome.sent());

        assertTrue(fixture.tick(401L));
        assertTrue(fixture.barrier.blocksOrdinary());
        assertTrue(fixture.ordinaryQueue.hasActive());
        assertEquals(1, fixture.stopExecutor.invocations.get());
    }

    @Test
    void targetObservationFailureDoesNotInvokeStopAndQuarantinesAfterUnknownDelivery() {
        Fixture fixture = new Fixture();
        FabricChatClefCommandContext active = fixture.ordinary("ordinary-active", "ordinary-active-message");
        fixture.ordinaryQueue.offer(active);
        fixture.ordinaryQueue.pollForDispatch();
        fixture.ownershipThrows = true;
        fixture.submitGlobal("stop-observation", "stop-observation-message");

        assertTrue(fixture.tick(500L));
        assertEquals(0, fixture.stopExecutor.invocations.get());
        assertControlResult(
                fixture.stopSender.payloads.get(0),
                "unknown",
                "target_observation_failed",
                "unknown",
                "unknown",
                "unknown"
        );
        fixture.stopSender.completeLatest(FabricChatClefCommandResultSendOutcome.sent());
        assertTrue(fixture.tick(501L));
        assertTrue(fixture.barrier.blocksOrdinary());
    }

    @Test
    void markerBindExceptionIsPreMutationObservationFailure() {
        Fixture fixture = new Fixture();
        FabricChatClefCommandContext active = fixture.ordinary("ordinary-bind-error", "ordinary-bind-message");
        fixture.ordinaryQueue.offer(active);
        fixture.ordinaryQueue.pollForDispatch();
        fixture.currentRoot.set(new DummyTask("active-root"));
        fixture.lifecycle.bindThrows = true;
        fixture.submitGlobal("stop-bind-error", "stop-bind-message");

        assertTrue(fixture.tick(550L));
        assertEquals(0, fixture.stopExecutor.invocations.get());
        assertControlResult(
                fixture.stopSender.payloads.get(0),
                "unknown",
                "target_observation_failed",
                "unknown",
                "unknown",
                "unknown"
        );
    }

    @Test
    void preQueueInvalidProfileUsesNullScopeAndTicksWithoutCreatingBarrier() {
        Fixture fixture = new Fixture();

        fixture.submitInvalidControlProfile("stop-invalid", "stop-invalid-message");

        assertTrue(fixture.stopQueue.active().isEmpty());
        assertFalse(fixture.barrier.blocksOrdinary());
        assertEquals(0, fixture.stopExecutor.invocations.get());
        FabricChatClefCommandResultPayload payload = fixture.stopSender.payloads.get(0);
        assertControlResult(
                payload,
                "rejected",
                "invalid_control_profile",
                "not_evaluated",
                "not_evaluated",
                "not_evaluated"
        );
        assertNull(data(payload).get("target_scope"));
        assertNull(data(payload).get("executed_client_tick"));
        assertNull(data(payload).get("verified_client_tick"));
    }

    @Test
    void tickSessionRevalidationKeepsValidatedScopeAndUsesObservationTickK() {
        Fixture fixture = new Fixture();
        fixture.submitGlobal("stop-session", "stop-session-message");
        fixture.sessionGuard.markConnectionDetached(7L);

        assertTrue(fixture.tick(600L));

        FabricChatClefCommandResultPayload payload = fixture.stopSender.payloads.get(0);
        assertControlResult(
                payload,
                "rejected",
                "session_mismatch",
                "not_evaluated",
                "not_evaluated",
                "not_evaluated"
        );
        assertEquals("current_global_automation", data(payload).get("target_scope"));
        assertNull(data(payload).get("executed_client_tick"));
        assertEquals(600L, data(payload).get("verified_client_tick"));
        assertEquals(0, fixture.stopExecutor.invocations.get());
    }

    @Test
    void stopCommandExceptionUsesSameTickUnknownAndNeverReexecutes() {
        Fixture fixture = new Fixture();
        fixture.stopExecutor.throwAfterIncrement = true;
        fixture.submitGlobal("stop-exception", "stop-exception-message");

        assertTrue(fixture.tick(700L));

        FabricChatClefCommandResultPayload payload = fixture.stopSender.payloads.get(0);
        assertControlResult(
                payload,
                "unknown",
                "stop_command_exception",
                "none",
                "none",
                "unknown"
        );
        assertEquals("internal_error", payload.toMap().get("error_code"));
        assertEquals(700L, data(payload).get("executed_client_tick"));
        assertEquals(700L, data(payload).get("verified_client_tick"));
        fixture.stopSender.completeLatest(FabricChatClefCommandResultSendOutcome.sent());
        assertTrue(fixture.tick(701L));
        assertEquals(1, fixture.stopExecutor.invocations.get());
    }

    @Test
    void exhaustedOriginalCancellationUsesFailedUnknownProfile() {
        Fixture fixture = new Fixture();
        FabricChatClefCommandContext pending = fixture.ordinary("ordinary-failed", "ordinary-failed-message");
        fixture.ordinaryQueue.offer(pending);
        fixture.lifecycle.originalMode = OriginalMode.FAILED;
        fixture.submitGlobal("stop-original-failed", "stop-original-failed-message");

        assertTrue(fixture.tick(800L));
        assertTrue(fixture.tick(801L));

        FabricChatClefCommandResultPayload payload = fixture.stopSender.payloads.get(0);
        assertControlResult(
                payload,
                "unknown",
                "original_cancel_send_failed",
                "captured_current",
                "pending",
                "unknown"
        );
        assertEquals("failed", data(payload).get("original_result_delivery"));
        assertEquals(1, fixture.stopExecutor.invocations.get());
    }

    @Test
    void unresolvedRetirementTimesOutAtExactlyTPlusTwentyEvenAfterTickJump() {
        Fixture fixture = new Fixture();
        FabricChatClefCommandContext pending = fixture.ordinary("ordinary-timeout", "ordinary-timeout-message");
        fixture.ordinaryQueue.offer(pending);
        fixture.lifecycle.originalMode = OriginalMode.IN_FLIGHT;
        fixture.submitGlobal("stop-timeout", "stop-timeout-message");

        assertTrue(fixture.tick(900L));
        assertTrue(fixture.tick(925L));

        FabricChatClefCommandResultPayload payload = fixture.stopSender.payloads.get(0);
        assertControlResult(
                payload,
                "unknown",
                "verification_timeout",
                "captured_current",
                "pending",
                "unknown"
        );
        assertEquals(900L, data(payload).get("executed_client_tick"));
        assertEquals(920L, data(payload).get("verified_client_tick"));
        assertEquals("unknown", data(payload).get("original_result_delivery"));
        assertEquals(1, fixture.stopExecutor.invocations.get());
    }

    @Test
    void tickJumpPastVerificationBoundaryCannotCreateOutOfRangeSendFailureProfile() {
        Fixture fixture = new Fixture();
        FabricChatClefCommandContext pending = fixture.ordinary("ordinary-jump", "ordinary-jump-message");
        fixture.ordinaryQueue.offer(pending);
        fixture.lifecycle.originalMode = OriginalMode.FAILED;
        fixture.submitGlobal("stop-jump", "stop-jump-message");

        assertTrue(fixture.tick(1_000L));
        assertTrue(fixture.tick(1_025L));

        FabricChatClefCommandResultPayload payload = fixture.stopSender.payloads.get(0);
        assertControlResult(
                payload,
                "unknown",
                "verification_timeout",
                "captured_current",
                "pending",
                "unknown"
        );
        assertEquals(1_000L, data(payload).get("executed_client_tick"));
        assertEquals(1_020L, data(payload).get("verified_client_tick"));
    }

    private static void assertOriginalCancellation(FabricChatClefCommandResultPayload payload) {
        assertEquals("cancelled", payload.toMap().get("status"));
        assertEquals(false, payload.toMap().get("ok"));
        assertEquals("user_stop_requested", data(payload).get("result_reason"));
        assertEquals(41L, data(payload).get("connection_generation"));
        assertEquals(7L, data(payload).get("java_socket_generation"));
    }

    private static void assertControlResult(
            FabricChatClefCommandResultPayload payload,
            String status,
            String reason,
            String resolution,
            String before,
            String after
    ) {
        assertEquals(status, payload.toMap().get("status"));
        assertEquals(reason, data(payload).get("control_reason"));
        assertEquals(resolution, data(payload).get("target_resolution"));
        assertEquals(before, data(payload).get("target_state_before"));
        assertEquals(after, data(payload).get("target_state_after"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> data(FabricChatClefCommandResultPayload payload) {
        return (Map<String, Object>) payload.toMap().get("data");
    }

    private static final class Fixture {
        private final FabricChatClefStopControlAdmissionBarrier barrier =
                new FabricChatClefStopControlAdmissionBarrier();
        private final FabricChatClefCommandQueue ordinaryQueue = new FabricChatClefCommandQueue(barrier);
        private final FabricChatClefStopControlQueue stopQueue = new FabricChatClefStopControlQueue(barrier);
        private final FabricChatClefStopControlDedupeRegistry dedupe =
                new FabricChatClefStopControlDedupeRegistry();
        private final FabricChatClefBridgeDiagnostics diagnostics = new FabricChatClefBridgeDiagnostics();
        private final FabricChatClefSessionGuard sessionGuard = new FabricChatClefSessionGuard(
                new FabricChatClefBridgeState(),
                diagnostics
        );
        private final CapturingStopResultSender stopSender = new CapturingStopResultSender();
        private final FabricChatClefStopControlResultOutbox stopResultOutbox =
                new FabricChatClefStopControlResultOutbox(stopSender, diagnostics);
        private final AtomicReference<Task> currentRoot = new AtomicReference<>();
        private final FakeLifecycle lifecycle = new FakeLifecycle(ordinaryQueue, currentRoot);
        private final CountingStopExecutor stopExecutor = new CountingStopExecutor();
        private final FabricChatClefInboundMessageHandler inbound;
        private final FabricChatClefStopControlTickDispatcher dispatcher;
        private volatile boolean ownershipThrows;

        private Fixture() {
            sessionGuard.beginHandshake("handshake-a", 7L);
            if (!sessionGuard.acceptHandshake(ack(), 7L)) {
                throw new AssertionError("test handshake was not accepted");
            }
            FabricChatClefStopControlInboundHandler stopInbound =
                    new FabricChatClefStopControlInboundHandler(
                            dedupe,
                            stopQueue,
                            stopResultOutbox,
                            sessionGuard,
                            diagnostics
                    );
            inbound = new FabricChatClefInboundMessageHandler(
                    diagnostics,
                    new FabricChatClefBridgeJson(),
                    sessionGuard,
                    null,
                    stopInbound
            );
            dispatcher = new FabricChatClefStopControlTickDispatcher(
                    stopQueue,
                    ordinaryQueue,
                    sessionGuard,
                    lifecycle,
                    stopExecutor,
                    stopResultOutbox,
                    dedupe,
                    () -> {
                        if (ownershipThrows) {
                            throw new IllegalStateException("ownership unavailable");
                        }
                        return ownership(currentRoot.get());
                    },
                    diagnostics
            );
        }

        private void submitGlobal(String requestId, String messageId) {
            inbound.handle(7L, stopJson(
                    requestId,
                    messageId,
                    "current_global_automation",
                    ""
            ));
            assertTrue(stopQueue.active().isPresent());
        }

        private void submitInvalidControlProfile(String requestId, String messageId) {
            String invalid = stopJson(
                    requestId,
                    messageId,
                    "current_global_automation",
                    ""
            ).replace("\"operation\":\"stop_ai\"", "\"operation\":\"wrong_operation\"");
            inbound.handle(7L, invalid);
        }

        private void submitTracked(
                String requestId,
                String messageId,
                FabricChatClefCommandContext target
        ) {
            String fields = """
                    ,
                    "target_request_id":"%s",
                    "target_command_message_id":"%s",
                    "target_session_id":"%s",
                    "target_server_connection_generation":%d
                    """.formatted(
                    target.requestId(),
                    target.correlationId(),
                    target.sessionId(),
                    target.serverConnectionGeneration()
            );
            inbound.handle(7L, stopJson(requestId, messageId, "tracked_command", fields));
            assertTrue(stopQueue.active().isPresent());
        }

        private boolean tick(long clientTick) {
            return dispatcher.onEndClientTick(System.currentTimeMillis(), clientTick);
        }

        private FabricChatClefCommandContext ordinary(String requestId, String messageId) {
            FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
            request.requestId = requestId;
            request.command = "get oak_log 1";
            request.source = "lavi_chat";
            request.deadlineMs = Long.MAX_VALUE;
            return new FabricChatClefCommandContext(request, messageId, "session-a", 41L, 7L);
        }

        private String stopJson(
                String requestId,
                String messageId,
                String targetScope,
                String targetFields
        ) {
            long deadline = System.currentTimeMillis() + 100_000L;
            return """
                    {
                      "protocol_version":1,
                      "message_type":"command_request",
                      "message_id":"%s",
                      "session_id":"session-a",
                      "timestamp_ms":1,
                      "payload":{
                        "request_id":"%s",
                        "command":"stop",
                        "source":"lavi_chat",
                        "deadline_ms":%d,
                        "metadata":{
                          "request_kind":"stop_control_v1",
                          "operation":"stop_ai",
                          "input_event":{
                            "source":"lavi_chat",
                            "provider_id":"local_chat",
                            "event_kind":"final_text",
                            "final":true,
                            "event_id":"0123456789abcdef0123456789abcdef"
                          },
                          "server_connection_generation":41,
                          "target_scope":"%s"%s
                        }
                      }
                    }
                    """.formatted(messageId, requestId, deadline, targetScope, targetFields);
        }

        private FabricChatClefBridgeEnvelope ack() {
            FabricChatClefBridgeEnvelope envelope = new FabricChatClefBridgeEnvelope();
            envelope.messageType = "handshake_ack";
            envelope.correlationId = "handshake-a";
            envelope.sessionId = "session-a";
            envelope.payload = new HashMap<>();
            envelope.payload.put("accepted", true);
            envelope.payload.put("session_id", "session-a");
            envelope.payload.put("connection_generation", 41L);
            return envelope;
        }
    }

    private static final class CapturingStopResultSender implements FabricChatClefStopControlResultSender {
        private final List<FabricChatClefCommandResultPayload> payloads = new ArrayList<>();
        private final List<Long> javaSocketGenerations = new ArrayList<>();
        private final List<Consumer<FabricChatClefCommandResultSendOutcome>> completions = new ArrayList<>();

        @Override
        public FabricChatClefCommandResultSendSubmission sendStopControlResult(
                String correlationId,
                String sessionId,
                long javaSocketGeneration,
                FabricChatClefCommandResultPayload payload,
                Consumer<FabricChatClefCommandResultSendOutcome> completion
        ) {
            payloads.add(payload);
            javaSocketGenerations.add(javaSocketGeneration);
            completions.add(completion);
            return FabricChatClefCommandResultSendSubmission.accepted();
        }

        private void completeLatest(FabricChatClefCommandResultSendOutcome outcome) {
            completions.get(completions.size() - 1).accept(outcome);
        }
    }

    private static final class CountingStopExecutor implements FabricChatClefStopCommandExecutor {
        private final AtomicInteger invocations = new AtomicInteger();
        private boolean throwAfterIncrement;

        @Override
        public void executeRegisteredStop() throws Exception {
            invocations.incrementAndGet();
            if (throwAfterIncrement) {
                throw new Exception("registered stop failed");
            }
        }
    }

    private enum OriginalMode {
        SENT,
        IN_FLIGHT,
        FAILED
    }

    private static final class FakeLifecycle implements FabricChatClefStopControlCommandLifecycle {
        private final FabricChatClefCommandQueue ordinaryQueue;
        private final AtomicReference<Task> currentRoot;
        private final List<FabricChatClefCommandResultPayload> originalResults = new ArrayList<>();
        private boolean allowMarkerBind = true;
        private boolean bindThrows;
        private boolean markerBound;
        private FabricChatClefStopControlIdentity boundIdentity;
        private OriginalMode originalMode = OriginalMode.SENT;

        private FakeLifecycle(
                FabricChatClefCommandQueue ordinaryQueue,
                AtomicReference<Task> currentRoot
        ) {
            this.ordinaryQueue = ordinaryQueue;
            this.currentRoot = currentRoot;
        }

        @Override
        public boolean bindUserStop(
                FabricChatClefCommandContext context,
                FabricChatClefStopControlIdentity identity,
                Task currentTask
        ) {
            if (bindThrows) {
                throw new IllegalStateException("test marker bind failure");
            }
            markerBound = allowMarkerBind
                    && context != null
                    && currentTask != null
                    && currentTask == currentRoot.get();
            if (markerBound) {
                boundIdentity = identity;
            }
            return markerBound;
        }

        @Override
        public boolean clearUserStop(
                FabricChatClefCommandContext context,
                FabricChatClefStopControlIdentity identity
        ) {
            if (!markerBound || !boundIdentity.equals(identity)) {
                return false;
            }
            markerBound = false;
            boundIdentity = null;
            return true;
        }

        @Override
        public boolean sendUserStopCancellation(FabricChatClefCommandContext context) {
            if (!context.beginTerminalSend(System.currentTimeMillis())) {
                return false;
            }
            FabricChatClefCommandResultPayload payload = context.commitTerminalPayload(
                    () -> new FabricChatClefOriginalCancellationResultFactory().create(context)
            );
            originalResults.add(payload);
            if (originalMode == OriginalMode.IN_FLIGHT) {
                return true;
            }
            if (originalMode == OriginalMode.FAILED) {
                context.completeTerminalSend(
                        FabricChatClefCommandResultSendOutcome.failed(
                                FabricChatClefCommandResultSendStatus.ENCODE_FAILED,
                                "definite test failure"
                        )
                );
                return false;
            }
            context.completeTerminalSend(FabricChatClefCommandResultSendOutcome.sent());
            if (ordinaryQueue.isPending(context)) {
                ordinaryQueue.removePending(context);
            } else if (ordinaryQueue.isActive(context)) {
                ordinaryQueue.complete(context, "test_original_cancel_sent");
            }
            currentRoot.set(null);
            return true;
        }

        @Override
        public boolean matchesBoundRootTask(FabricChatClefCommandContext context, Task candidateTask) {
            return markerBound && candidateTask != null && candidateTask == currentRoot.get();
        }
    }

    private static FabricChatClefTaskOwnershipEvidence ownership(Task root) {
        long nowMs = System.currentTimeMillis();
        FabricChatClefTaskSnapshot snapshot = FabricChatClefTaskSnapshot.capture(root);
        FabricChatClefTaskOwnershipSnapshot ownership = FabricChatClefTaskOwnershipSnapshot.of(
                nowMs,
                0L,
                Thread.currentThread().getName(),
                snapshot,
                root == null ? "" : root.getClass().getName(),
                root == null ? "none" : Integer.toHexString(System.identityHashCode(root)),
                root == null ? "none" : "assignment-a",
                root == null ? 0L : 1L,
                false,
                false,
                root != null,
                root == null ? "" : "test-chain",
                root == null ? "" : "test-chain-id",
                root != null,
                ""
        );
        return FabricChatClefTaskOwnershipEvidence.of(
                root,
                snapshot,
                ownership,
                nowMs,
                System.nanoTime(),
                0L,
                Thread.currentThread().getName()
        );
    }

    private static final class DummyTask extends Task {
        private final String name;

        private DummyTask(String name) {
            this.name = name;
        }

        @Override
        protected void onStart() {
        }

        @Override
        protected Task onTick() {
            return null;
        }

        @Override
        protected void onStop(Task interruptTask) {
        }

        @Override
        protected boolean isEqual(Task other) {
            return other instanceof DummyTask task && name.equals(task.name);
        }

        @Override
        protected String toDebugString() {
            return name;
        }
    }
}
