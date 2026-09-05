package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.stage;

//20260905_kpopmodder: Validate queued STOP session generations and deadline before mutation.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlRequest;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

public final class FabricChatClefStopControlEligibilityValidator {
    private final FabricChatClefSessionGuard sessionGuard;

    public FabricChatClefStopControlEligibilityValidator(FabricChatClefSessionGuard sessionGuard) {
        this.sessionGuard = sessionGuard;
    }

    public String rejectionReason(FabricChatClefStopControlContext context, long nowMs) {
        FabricChatClefStopControlRequest request = context.request();
        if (!sessionGuard.handshakeAccepted()
                || !sessionGuard.isActiveSession(request.identity().sessionId())
                || sessionGuard.acceptedIdentity()
                .map(identity -> identity.javaSocketGeneration() != request.javaSocketGeneration())
                .orElse(true)) {
            return "session_mismatch";
        }
        if (sessionGuard.acceptedIdentity()
                .map(identity -> identity.serverConnectionGeneration()
                        != request.identity().serverConnectionGeneration())
                .orElse(true)) {
            return "server_generation_mismatch";
        }
        return request.deadlineExceeded(nowMs) ? "deadline_exceeded" : null;
    }
}
