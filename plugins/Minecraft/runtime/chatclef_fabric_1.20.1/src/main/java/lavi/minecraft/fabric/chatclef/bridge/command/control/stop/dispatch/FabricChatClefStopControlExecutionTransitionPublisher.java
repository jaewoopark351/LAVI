package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch;

//20260905_kpopmodder: Publish canonical transition boundaries for queued STOP execution.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.diagnostics.FabricChatClefStopControlTransitionEmitter;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;

public final class FabricChatClefStopControlExecutionTransitionPublisher {
    private final FabricChatClefStopControlTransitionEmitter transitionEmitter;

    public FabricChatClefStopControlExecutionTransitionPublisher(
            FabricChatClefStopControlTransitionEmitter transitionEmitter
    ) {
        this.transitionEmitter = transitionEmitter;
    }

    public void targetCaptured(FabricChatClefStopControlContext context) {
        transitionEmitter.contextBoundary(
                context,
                "target_captured",
                "closed",
                "blocked",
                false,
                "captured_context"
        );
    }

    public void invocationCommitted(FabricChatClefStopControlContext context) {
        transitionEmitter.contextBoundary(
                context,
                "registered_stop_invocation_committed",
                "closed",
                "blocked",
                false,
                "none"
        );
    }

    public void verificationStarted(FabricChatClefStopControlContext context) {
        transitionEmitter.contextBoundary(
                context,
                "retirement_verification_started",
                "closed",
                "blocked",
                false,
                "original_result_pending"
        );
    }
}
