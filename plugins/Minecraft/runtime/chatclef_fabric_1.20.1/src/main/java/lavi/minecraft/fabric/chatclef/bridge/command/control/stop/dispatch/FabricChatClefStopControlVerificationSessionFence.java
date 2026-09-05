package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch;

//20260905_kpopmodder: Verify only that STOP retirement remains bound to its accepted session.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlRequest;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

public final class FabricChatClefStopControlVerificationSessionFence {
    private final FabricChatClefSessionGuard sessionGuard;

    public FabricChatClefStopControlVerificationSessionFence(
            FabricChatClefSessionGuard sessionGuard
    ) {
        this.sessionGuard = sessionGuard;
    }

    public boolean matches(FabricChatClefStopControlContext context) {
        FabricChatClefStopControlRequest request = context.request();
        return sessionGuard.matches(
                request.identity().sessionId(),
                request.identity().serverConnectionGeneration(),
                request.javaSocketGeneration()
        );
    }
}
