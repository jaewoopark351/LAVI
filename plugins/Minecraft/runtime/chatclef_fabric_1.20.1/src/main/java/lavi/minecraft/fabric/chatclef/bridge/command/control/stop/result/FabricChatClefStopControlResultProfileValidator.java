package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result;

//20260905_kpopmodder: Enforce closed STOP result evidence profiles independently from payload rendering.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefOrdinaryCommandStopCapture;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlBaseRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlTargetScope;

public final class FabricChatClefStopControlResultProfileValidator {
    public void validateRejection(
            FabricChatClefStopControlBaseRequest base,
            String reason,
            FabricChatClefStopControlTargetScope scope,
            FabricChatClefStopControlRequest request,
            Long verifiedClientTick
    ) {
        requireBase(base);
        if (!isNoMutationReason(reason)) {
            throw new IllegalArgumentException("unsupported STOP rejection reason=" + reason);
        }
        if (verifiedClientTick == null) {
            if ("invalid_target_fields".equals(reason)) {
                if (scope == null || request != null) {
                    throw new IllegalStateException("invalid_target_fields requires declared scope and null request");
                }
                return;
            }
            if ("stop_control_in_flight".equals(reason)) {
                requireRequest(request);
                requireSameScope(scope, request.targetScope());
                return;
            }
            if (scope != null || request != null) {
                throw new IllegalStateException("pre-profile rejection requires null scope and request");
            }
            return;
        }
        if (verifiedClientTick < 0L
                || !("session_mismatch".equals(reason)
                || "server_generation_mismatch".equals(reason)
                || "deadline_exceeded".equals(reason))) {
            throw new IllegalStateException("invalid client-tick rejection profile");
        }
        requireRequest(request);
        requireSameScope(scope, request.targetScope());
    }

    public FabricChatClefOrdinaryCommandStopCapture validateCompleted(
            FabricChatClefStopControlContext context,
            long verifiedClientTick
    ) {
        if (context == null) {
            throw new IllegalStateException("STOP success requires a context");
        }
        FabricChatClefOrdinaryCommandStopCapture capture = context.capture();
        if (capture == null || !context.stopCommandInvoked()) {
            throw new IllegalStateException("STOP success requires capture and invocation evidence");
        }
        requireRequest(context.request());
        long executedClientTick = context.executedClientTick();
        if (!context.stopCommandInvoked()
                || executedClientTick < 0L
                || verifiedClientTick < executedClientTick
                || verifiedClientTick > executedClientTick + 20L) {
            throw new IllegalStateException("STOP success has invalid invocation/tick evidence");
        }
        String resolution = context.targetResolution();
        if (capture.absent()) {
            if (!"none".equals(capture.state())
                    || !"none".equals(resolution)
                    || verifiedClientTick != executedClientTick) {
                throw new IllegalStateException("no-context STOP success must verify synchronously at T");
            }
            return capture;
        }
        requireCapturedContext(capture, resolution, context.request());
        if (!capture.context().terminalSent()) {
            throw new IllegalStateException("captured STOP success requires original cancellation SENT");
        }
        if (capture.active() && !context.userStopMarkerBound()) {
            throw new IllegalStateException("active STOP success requires a pre-bound user-stop marker");
        }
        return capture;
    }

    public FabricChatClefOrdinaryCommandStopCapture validateUnknown(
            FabricChatClefStopControlContext context,
            String reason,
            long verifiedClientTick
    ) {
        if (!isUnknownReason(reason)) {
            throw new IllegalArgumentException("unsupported STOP uncertainty reason=" + reason);
        }
        requireRequest(context == null ? null : context.request());
        FabricChatClefOrdinaryCommandStopCapture capture = context.capture();
        if (verifiedClientTick < 0L) {
            throw new IllegalStateException("STOP uncertainty requires a non-negative verified tick");
        }
        if ("target_observation_failed".equals(reason)) {
            if (context.stopCommandInvoked() || context.executedClientTick() >= 0L) {
                throw new IllegalStateException("target observation failure cannot include STOP invocation");
            }
            if (capture != null && capture.pending()) {
                requireCapturedContext(capture, context.targetResolution(), context.request());
            }
            return capture;
        }
        long executedClientTick = context.executedClientTick();
        if (!context.stopCommandInvoked()
                || executedClientTick < 0L
                || verifiedClientTick < executedClientTick
                || verifiedClientTick > executedClientTick + 20L) {
            throw new IllegalStateException("post-mutation STOP uncertainty has invalid invocation/ticks");
        }
        if ("user_stop_marker_bind_failed".equals(reason)) {
            if (capture == null || !capture.active()
                    || context.userStopMarkerBound()
                    || verifiedClientTick != executedClientTick) {
                throw new IllegalStateException("invalid user-stop marker failure profile");
            }
            requireCapturedContext(capture, context.targetResolution(), context.request());
            return capture;
        }
        if ("stop_command_exception".equals(reason)) {
            if (capture == null || verifiedClientTick != executedClientTick) {
                throw new IllegalStateException("invalid StopCommand exception profile");
            }
            if (!capture.absent()) {
                requireCapturedContext(capture, context.targetResolution(), context.request());
            } else if (!"none".equals(context.targetResolution())) {
                throw new IllegalStateException("no-context StopCommand exception requires none resolution");
            }
            return capture;
        }
        if (capture == null || capture.absent()) {
            throw new IllegalStateException(reason + " requires a captured ordinary context");
        }
        requireCapturedContext(capture, context.targetResolution(), context.request());
        if ("original_cancel_send_failed".equals(reason)) {
            if (!capture.context().terminalSendRetryExhausted()) {
                throw new IllegalStateException("original cancellation failure requires exhausted delivery");
            }
            return capture;
        }
        if (verifiedClientTick != executedClientTick + 20L) {
            throw new IllegalStateException("verification timeout must be exactly T+20");
        }
        return capture;
    }

    private void requireCapturedContext(
            FabricChatClefOrdinaryCommandStopCapture capture,
            String resolution,
            FabricChatClefStopControlRequest request
    ) {
        FabricChatClefCommandContext captured = capture.context();
        if (captured == null
                || !(capture.pending() || capture.active())
                || !("exact".equals(resolution) || "captured_current".equals(resolution))
                || captured.requestId().isBlank()
                || captured.correlationId().isBlank()
                || captured.sessionId().isBlank()
                || captured.serverConnectionGeneration() <= 0L) {
            throw new IllegalStateException("STOP captured target identity is incomplete");
        }
        if ("exact".equals(resolution) && !request.matchesTarget(captured)) {
            throw new IllegalStateException("exact STOP target does not match requested quartet");
        }
        if ("captured_current".equals(resolution)
                && request.targetScope() == FabricChatClefStopControlTargetScope.TRACKED_COMMAND
                && request.matchesTarget(captured)) {
            throw new IllegalStateException("captured-current STOP target must differ from the tracked request");
        }
    }

    private void requireBase(FabricChatClefStopControlBaseRequest base) {
        if (base == null
                || base.identity() == null
                || base.identity().sessionId() == null
                || base.identity().sessionId().isBlank()
                || base.identity().requestId() == null
                || base.identity().requestId().isBlank()
                || base.identity().messageId() == null
                || base.identity().messageId().isBlank()
                || base.identity().serverConnectionGeneration() <= 0L
                || base.javaSocketGeneration() <= 0L) {
            throw new IllegalStateException("STOP result base identity is incomplete");
        }
    }

    private void requireRequest(FabricChatClefStopControlRequest request) {
        if (request == null || request.targetScope() == null) {
            throw new IllegalStateException("STOP result requires a validated request");
        }
        requireBase(request.base());
        if (request.targetScope() == FabricChatClefStopControlTargetScope.TRACKED_COMMAND) {
            if (request.targetRequestId() == null || request.targetRequestId().isBlank()
                    || request.targetCommandMessageId() == null || request.targetCommandMessageId().isBlank()
                    || request.targetSessionId() == null || request.targetSessionId().isBlank()
                    || request.targetServerConnectionGeneration() == null
                    || request.targetServerConnectionGeneration() <= 0L) {
                throw new IllegalStateException("tracked STOP result requires a complete requested quartet");
            }
            return;
        }
        if (request.targetRequestId() != null
                || request.targetCommandMessageId() != null
                || request.targetSessionId() != null
                || request.targetServerConnectionGeneration() != null) {
            throw new IllegalStateException("global STOP result forbids requested target fields");
        }
    }

    private void requireSameScope(
            FabricChatClefStopControlTargetScope actual,
            FabricChatClefStopControlTargetScope expected
    ) {
        if (actual == null || actual != expected) {
            throw new IllegalStateException("STOP result scope does not match validated request");
        }
    }

    private boolean isNoMutationReason(String reason) {
        return "invalid_control_profile".equals(reason)
                || "invalid_target_scope".equals(reason)
                || "invalid_target_fields".equals(reason)
                || "session_mismatch".equals(reason)
                || "server_generation_mismatch".equals(reason)
                || "stop_control_in_flight".equals(reason)
                || "deadline_exceeded".equals(reason);
    }

    private boolean isUnknownReason(String reason) {
        return "target_observation_failed".equals(reason)
                || "user_stop_marker_bind_failed".equals(reason)
                || "stop_command_exception".equals(reason)
                || "original_cancel_send_failed".equals(reason)
                || "verification_timeout".equals(reason);
    }
}
