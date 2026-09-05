package lavi.minecraft.fabric.chatclef.bridge.command.control.stop;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefOrdinaryCommandStopCapture;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlIdentity;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlBaseRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlTargetScope;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultFactory;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260905_kpopmodder: Lock the STOP result factory to the phase-specific closed wire profiles.
final class FabricChatClefStopControlResultFactoryTest {
    private final FabricChatClefStopControlResultFactory factory = new FabricChatClefStopControlResultFactory();

    @Test
    void noContextSuccessRequiresVerifiedTickEqualToInvocationTick() {
        FabricChatClefStopControlContext context = context(globalRequest("control-global", "control-message"));
        context.capture(FabricChatClefOrdinaryCommandStopCapture.none(), "none");
        context.markExecuted(100L, false, null);

        FabricChatClefCommandResultPayload payload = factory.completed(context, 100L);

        assertTopLevel(payload, "completed", true, null);
        assertProfile(
                payload,
                "stopped",
                "global_stop_executed_no_lavi_context",
                "current_global_automation",
                "none",
                "none",
                "none",
                "not_applicable",
                true,
                100L,
                100L
        );
        assertThrows(IllegalStateException.class, () -> factory.completed(context, 101L));
    }

    @Test
    void rejectionProfilesKeepPreQueueNullsSeparateFromTickK() {
        FabricChatClefStopControlBaseRequest base = base("control-a", "message-a");
        FabricChatClefCommandContext target = ordinary("ordinary-a", "ordinary-message-a");
        FabricChatClefStopControlRequest tracked = trackedRequest(base, target);

        FabricChatClefCommandResultPayload invalidProfile =
                factory.rejected(base, "invalid_control_profile", null, null, null);
        assertNoMutation(invalidProfile, "rejected", "invalid_request", "invalid_control_profile", null, null);

        FabricChatClefCommandResultPayload invalidFields = factory.rejected(
                base,
                "invalid_target_fields",
                FabricChatClefStopControlTargetScope.TRACKED_COMMAND,
                null,
                null
        );
        assertNoMutation(
                invalidFields,
                "rejected",
                "invalid_request",
                "invalid_target_fields",
                "tracked_command",
                null
        );
        assertRequestedTargetNull(invalidFields);

        FabricChatClefCommandResultPayload inFlight = factory.rejected(
                base,
                "stop_control_in_flight",
                FabricChatClefStopControlTargetScope.TRACKED_COMMAND,
                tracked,
                null
        );
        assertNoMutation(
                inFlight,
                "rejected",
                "invalid_request",
                "stop_control_in_flight",
                "tracked_command",
                null
        );
        assertRequestedTarget(inFlight, target);

        FabricChatClefCommandResultPayload tickMismatch = factory.rejected(
                base,
                "session_mismatch",
                FabricChatClefStopControlTargetScope.TRACKED_COMMAND,
                tracked,
                44L
        );
        assertNoMutation(
                tickMismatch,
                "rejected",
                "invalid_request",
                "session_mismatch",
                "tracked_command",
                44L
        );
        assertRequestedTarget(tickMismatch, target);

        FabricChatClefCommandResultPayload deadline =
                factory.rejected(base, "deadline_exceeded", null, null, null);
        assertNoMutation(
                deadline,
                "deadline_exceeded",
                "deadline_exceeded",
                "deadline_exceeded",
                null,
                null
        );
    }

    @Test
    void uncertaintyProfilesEmitOnlyTheirClosedStatusErrorStateDeliveryAndTicks() {
        FabricChatClefCommandContext activeTarget = ordinary("ordinary-active", "ordinary-active-message");
        FabricChatClefStopControlContext observation = context(globalRequest("observation", "observation-message"));
        FabricChatClefCommandResultPayload observationPayload =
                factory.unknown(observation, "target_observation_failed", 200L);
        assertTopLevel(observationPayload, "unknown", false, "internal_error");
        assertProfile(
                observationPayload,
                "unknown",
                "target_observation_failed",
                "current_global_automation",
                "unknown",
                "unknown",
                "unknown",
                "not_applicable",
                false,
                null,
                200L
        );

        FabricChatClefStopControlContext markerFailure =
                context(trackedRequest(base("marker", "marker-message"), activeTarget));
        markerFailure.capture(FabricChatClefOrdinaryCommandStopCapture.active(activeTarget), "exact");
        markerFailure.markExecuted(300L, false, null);
        FabricChatClefCommandResultPayload markerPayload =
                factory.unknown(markerFailure, "user_stop_marker_bind_failed", 300L);
        assertTopLevel(markerPayload, "unknown", false, null);
        assertProfile(
                markerPayload,
                "unknown",
                "user_stop_marker_bind_failed",
                "tracked_command",
                "exact",
                "active",
                "unknown",
                "not_applicable",
                true,
                300L,
                300L
        );

        FabricChatClefStopControlContext exception = context(globalRequest("exception", "exception-message"));
        exception.capture(FabricChatClefOrdinaryCommandStopCapture.none(), "none");
        exception.markExecuted(400L, false, null);
        FabricChatClefCommandResultPayload exceptionPayload =
                factory.unknown(exception, "stop_command_exception", 400L);
        assertTopLevel(exceptionPayload, "unknown", false, "internal_error");
        assertProfile(
                exceptionPayload,
                "unknown",
                "stop_command_exception",
                "current_global_automation",
                "none",
                "none",
                "unknown",
                "not_applicable",
                true,
                400L,
                400L
        );

        FabricChatClefCommandContext failedTarget = ordinary("ordinary-failed", "ordinary-failed-message");
        exhaustTerminalDelivery(failedTarget);
        FabricChatClefStopControlContext sendFailure =
                context(trackedRequest(base("send-failure", "send-failure-message"), failedTarget));
        sendFailure.capture(FabricChatClefOrdinaryCommandStopCapture.pending(failedTarget), "exact");
        sendFailure.markExecuted(500L, false, null);
        FabricChatClefCommandResultPayload failurePayload =
                factory.unknown(sendFailure, "original_cancel_send_failed", 501L);
        assertTopLevel(failurePayload, "unknown", false, null);
        assertProfile(
                failurePayload,
                "unknown",
                "original_cancel_send_failed",
                "tracked_command",
                "exact",
                "pending",
                "unknown",
                "failed",
                true,
                500L,
                501L
        );

        FabricChatClefCommandContext sentButUnretired = ordinary("ordinary-timeout", "ordinary-timeout-message");
        markTerminalSent(sentButUnretired);
        FabricChatClefStopControlContext timeout =
                context(globalRequest("timeout", "timeout-message"));
        timeout.capture(FabricChatClefOrdinaryCommandStopCapture.active(sentButUnretired), "captured_current");
        timeout.markExecuted(600L, true, null);
        FabricChatClefCommandResultPayload timeoutPayload =
                factory.unknown(timeout, "verification_timeout", 620L);
        assertTopLevel(timeoutPayload, "unknown", false, null);
        assertProfile(
                timeoutPayload,
                "unknown",
                "verification_timeout",
                "current_global_automation",
                "captured_current",
                "active",
                "unknown",
                "sent",
                true,
                600L,
                620L
        );
        assertThrows(
                IllegalStateException.class,
                () -> factory.unknown(timeout, "verification_timeout", 619L)
        );
    }

    @Test
    void trackedCapturedCurrentCannotAliasTheExactRequestedTarget() {
        FabricChatClefCommandContext target = ordinary("ordinary-exact", "ordinary-exact-message");
        markTerminalSent(target);
        FabricChatClefStopControlContext context =
                context(trackedRequest(base("control-exact", "control-exact-message"), target));
        context.capture(FabricChatClefOrdinaryCommandStopCapture.pending(target), "captured_current");
        context.markExecuted(700L, false, null);

        assertThrows(IllegalStateException.class, () -> factory.completed(context, 701L));
    }

    private void assertNoMutation(
            FabricChatClefCommandResultPayload payload,
            String status,
            String errorCode,
            String reason,
            String scope,
            Long verifiedTick
    ) {
        assertTopLevel(payload, status, false, errorCode);
        assertProfile(
                payload,
                "rejected",
                reason,
                scope,
                "not_evaluated",
                "not_evaluated",
                "not_evaluated",
                "not_applicable",
                false,
                null,
                verifiedTick
        );
    }

    private void assertTopLevel(
            FabricChatClefCommandResultPayload payload,
            String status,
            boolean ok,
            String errorCode
    ) {
        assertEquals(status, payload.toMap().get("status"));
        assertEquals(ok, payload.toMap().get("ok"));
        assertEquals(errorCode, payload.toMap().get("error_code"));
    }

    private void assertProfile(
            FabricChatClefCommandResultPayload payload,
            String outcome,
            String reason,
            String scope,
            String resolution,
            String before,
            String after,
            String delivery,
            boolean invoked,
            Long executedTick,
            Long verifiedTick
    ) {
        Map<String, Object> data = data(payload);
        assertEquals("stop_control_v1", data.get("request_kind"));
        assertEquals("stop_ai", data.get("operation"));
        assertEquals(outcome, data.get("control_outcome"));
        assertEquals(reason, data.get("control_reason"));
        assertEquals(scope, data.get("target_scope"));
        assertEquals(resolution, data.get("target_resolution"));
        assertEquals(before, data.get("target_state_before"));
        assertEquals(after, data.get("target_state_after"));
        assertEquals(delivery, data.get("original_result_delivery"));
        assertEquals(invoked, data.get("stop_command_invoked"));
        assertEquals(executedTick, data.get("executed_client_tick"));
        assertEquals(verifiedTick, data.get("verified_client_tick"));
        assertEquals(41L, data.get("connection_generation"));
        assertEquals(7L, data.get("java_socket_generation"));
    }

    private void assertRequestedTargetNull(FabricChatClefCommandResultPayload payload) {
        Map<String, Object> data = data(payload);
        assertNull(data.get("requested_target_request_id"));
        assertNull(data.get("requested_target_command_message_id"));
        assertNull(data.get("requested_target_session_id"));
        assertNull(data.get("requested_target_server_connection_generation"));
    }

    private void assertRequestedTarget(
            FabricChatClefCommandResultPayload payload,
            FabricChatClefCommandContext target
    ) {
        Map<String, Object> data = data(payload);
        assertEquals(target.requestId(), data.get("requested_target_request_id"));
        assertEquals(target.correlationId(), data.get("requested_target_command_message_id"));
        assertEquals(target.sessionId(), data.get("requested_target_session_id"));
        assertEquals(target.serverConnectionGeneration(), data.get("requested_target_server_connection_generation"));
    }

    private FabricChatClefStopControlContext context(FabricChatClefStopControlRequest request) {
        return new FabricChatClefStopControlContext(request, null);
    }

    private FabricChatClefStopControlBaseRequest base(String requestId, String messageId) {
        return new FabricChatClefStopControlBaseRequest(
                new FabricChatClefStopControlIdentity("session-a", 41L, requestId, messageId),
                7L,
                "fingerprint"
        );
    }

    private FabricChatClefStopControlRequest globalRequest(String requestId, String messageId) {
        return new FabricChatClefStopControlRequest(
                base(requestId, messageId),
                Long.MAX_VALUE,
                FabricChatClefStopControlTargetScope.CURRENT_GLOBAL_AUTOMATION,
                null,
                null,
                null,
                null
        );
    }

    private FabricChatClefStopControlRequest trackedRequest(
            FabricChatClefStopControlBaseRequest base,
            FabricChatClefCommandContext target
    ) {
        return new FabricChatClefStopControlRequest(
                base,
                Long.MAX_VALUE,
                FabricChatClefStopControlTargetScope.TRACKED_COMMAND,
                target.requestId(),
                target.correlationId(),
                target.sessionId(),
                target.serverConnectionGeneration()
        );
    }

    private FabricChatClefCommandContext ordinary(String requestId, String messageId) {
        FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
        request.requestId = requestId;
        request.command = "get oak_log 1";
        request.source = "lavi_chat";
        request.deadlineMs = Long.MAX_VALUE;
        return new FabricChatClefCommandContext(request, messageId, "session-a", 41L, 7L);
    }

    private void markTerminalSent(FabricChatClefCommandContext context) {
        assertTrue(context.beginTerminalSend(0L));
        assertTrue(context.completeTerminalSend(FabricChatClefCommandResultSendOutcome.sent()));
    }

    private void exhaustTerminalDelivery(FabricChatClefCommandContext context) {
        assertTrue(context.beginTerminalSend(0L));
        assertFalse(context.completeTerminalSend(
                FabricChatClefCommandResultSendOutcome.failed(
                        FabricChatClefCommandResultSendStatus.ENCODE_FAILED,
                        "definite test failure"
                )
        ));
        assertTrue(context.terminalSendRetryExhausted());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(FabricChatClefCommandResultPayload payload) {
        return (Map<String, Object>) payload.toMap().get("data");
    }
}
