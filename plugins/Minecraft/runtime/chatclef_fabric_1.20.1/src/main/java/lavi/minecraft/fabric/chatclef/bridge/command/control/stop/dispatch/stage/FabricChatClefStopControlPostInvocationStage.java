package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.stage;

//20260905_kpopmodder: Advance a successful STOP invocation into completion or retirement verification.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.FabricChatClefStopControlCommandLifecycle;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.FabricChatClefStopControlResultCommitter;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.FabricChatClefStopControlExecutionTransitionPublisher;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefOrdinaryCommandStopCapture;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;

public final class FabricChatClefStopControlPostInvocationStage {
    private final FabricChatClefStopControlCommandLifecycle commandLifecycle;
    private final FabricChatClefStopControlResultCommitter resultCommitter;
    private final FabricChatClefStopControlExecutionTransitionPublisher transitions;

    public FabricChatClefStopControlPostInvocationStage(
            FabricChatClefStopControlCommandLifecycle commandLifecycle,
            FabricChatClefStopControlResultCommitter resultCommitter,
            FabricChatClefStopControlExecutionTransitionPublisher transitions
    ) {
        this.commandLifecycle = commandLifecycle;
        this.resultCommitter = resultCommitter;
        this.transitions = transitions;
    }

    public void advance(
            FabricChatClefStopControlContext context,
            FabricChatClefOrdinaryCommandStopCapture capture,
            FabricChatClefStopControlMarkerBindingResult binding,
            long clientTick
    ) {
        if (capture.active() && !binding.markerBound()) {
            resultCommitter.commitUnknown(context, "user_stop_marker_bind_failed", clientTick, true);
            return;
        }
        if (capture.absent()) {
            context.markVerified(clientTick);
            resultCommitter.commitCompleted(context, clientTick);
            return;
        }
        try {
            commandLifecycle.sendUserStopCancellation(capture.context());
        } catch (Throwable error) {
            resultCommitter.quarantineWithoutWire(context, "original_cancel_result_submission_failed");
            return;
        }
        beginVerification(context);
    }

    private void beginVerification(FabricChatClefStopControlContext context) {
        context.markVerifying();
        transitions.verificationStarted(context);
    }
}
