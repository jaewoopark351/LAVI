package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch;

//20260905_kpopmodder: Sequence focused queued STOP stages without absorbing their validation or mutation ownership.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.diagnostics.FabricChatClefStopControlTransitionEmitter;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.stage.FabricChatClefStopControlEligibilityValidator;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.stage.FabricChatClefStopControlInvocationOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.stage.FabricChatClefStopControlInvocationStage;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.stage.FabricChatClefStopControlMarkerBindingResult;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.stage.FabricChatClefStopControlMarkerBindingStage;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.stage.FabricChatClefStopControlPostInvocationStage;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.stage.FabricChatClefStopControlTargetCaptureStage;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefOrdinaryCommandStopCapture;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlAdmissionBarrier;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

public final class FabricChatClefStopControlQueuedExecutor {
    private final FabricChatClefStopControlEligibilityValidator eligibilityValidator;
    private final FabricChatClefStopControlTargetCaptureStage captureStage;
    private final FabricChatClefStopControlMarkerBindingStage markerBindingStage;
    private final FabricChatClefStopControlInvocationStage invocationStage;
    private final FabricChatClefStopControlPostInvocationStage postInvocationStage;
    private final FabricChatClefStopControlExecutionTransitionPublisher transitions;
    private final FabricChatClefStopControlResultCommitter resultCommitter;

    public FabricChatClefStopControlQueuedExecutor(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefStopControlAdmissionBarrier admissionBarrier,
            FabricChatClefSessionGuard sessionGuard,
            FabricChatClefStopControlCommandLifecycle commandLifecycle,
            FabricChatClefStopCommandExecutor stopCommandExecutor,
            FabricChatClefStopControlTaskOwnershipReader taskOwnershipReader,
            FabricChatClefStopControlTargetResolver targetResolver,
            FabricChatClefStopControlTransitionEmitter transitionEmitter,
            FabricChatClefStopControlResultCommitter resultCommitter
    ) {
        this.resultCommitter = resultCommitter;
        this.eligibilityValidator = new FabricChatClefStopControlEligibilityValidator(sessionGuard);
        this.captureStage = new FabricChatClefStopControlTargetCaptureStage(
                commandQueue,
                admissionBarrier,
                targetResolver
        );
        this.markerBindingStage = new FabricChatClefStopControlMarkerBindingStage(
                commandLifecycle,
                taskOwnershipReader
        );
        this.invocationStage = new FabricChatClefStopControlInvocationStage(stopCommandExecutor);
        this.transitions = new FabricChatClefStopControlExecutionTransitionPublisher(transitionEmitter);
        this.postInvocationStage = new FabricChatClefStopControlPostInvocationStage(
                commandLifecycle,
                resultCommitter,
                transitions
        );
    }

    public void execute(
            FabricChatClefStopControlContext context,
            long nowMs,
            long clientTick
    ) {
        String rejectionReason = eligibilityValidator.rejectionReason(context, nowMs);
        if (rejectionReason != null) {
            resultCommitter.commitNoMutation(context, rejectionReason, clientTick);
            return;
        }

        FabricChatClefOrdinaryCommandStopCapture capture = captureStage.capture(context);
        if (capture == null) {
            resultCommitter.commitUnknown(context, "target_observation_failed", clientTick, false);
            return;
        }
        transitions.targetCaptured(context);

        FabricChatClefStopControlMarkerBindingResult binding = markerBindingStage.bind(context, capture);
        if (!binding.succeeded()) {
            resultCommitter.commitUnknown(context, binding.failureReason(), clientTick, false);
            return;
        }
        invocationStage.commit(context, binding, clientTick);
        transitions.invocationCommitted(context);
        FabricChatClefStopControlInvocationOutcome invocation = invocationStage.invokeRegisteredStop();
        if (!invocation.succeeded()) {
            markerBindingStage.clearAfterInvocationFailure(context, capture, binding);
            resultCommitter.commitUnknown(context, invocation.failureReason(), clientTick, true);
            return;
        }
        postInvocationStage.advance(context, capture, binding, clientTick);
    }
}
