package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch;

//20260905_kpopmodder: Verify ordinary-context retirement after the one-shot STOP mutation.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.diagnostics.FabricChatClefStopControlTransitionEmitter;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefOrdinaryCommandStopCapture;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

public final class FabricChatClefStopControlRetirementVerifier {
    public static final int MAX_LATER_TICKS =
            FabricChatClefStopControlVerificationDeadlinePolicy.MAX_LATER_TICKS;

    private final FabricChatClefStopControlTransitionEmitter transitionEmitter;
    private final FabricChatClefStopControlResultCommitter resultCommitter;
    private final FabricChatClefStopControlVerificationSessionFence sessionFence;
    private final FabricChatClefStopControlRetirementPredicate retirementPredicate;
    private final FabricChatClefStopControlVerificationDeadlinePolicy deadlinePolicy;
    private final FabricChatClefStopControlOriginalCancellationRetry cancellationRetry;

    public FabricChatClefStopControlRetirementVerifier(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefSessionGuard sessionGuard,
            FabricChatClefStopControlCommandLifecycle commandLifecycle,
            FabricChatClefStopControlTaskOwnershipReader taskOwnershipReader,
            FabricChatClefStopControlTransitionEmitter transitionEmitter,
            FabricChatClefStopControlResultCommitter resultCommitter
    ) {
        this.transitionEmitter = transitionEmitter;
        this.resultCommitter = resultCommitter;
        this.sessionFence = new FabricChatClefStopControlVerificationSessionFence(sessionGuard);
        this.retirementPredicate = new FabricChatClefStopControlRetirementPredicate(
                commandQueue,
                taskOwnershipReader
        );
        this.deadlinePolicy = new FabricChatClefStopControlVerificationDeadlinePolicy();
        this.cancellationRetry = new FabricChatClefStopControlOriginalCancellationRetry(
                commandLifecycle
        );
    }

    public void verify(
            FabricChatClefStopControlContext context,
            long nowMs,
            long clientTick
    ) {
        if (!sessionFence.matches(context)) {
            context.markQuarantined();
            transitionEmitter.contextBoundary(
                    context,
                    "connection_detached_after_execution",
                    "quarantined",
                    "blocked",
                    true,
                    "none"
            );
            return;
        }
        FabricChatClefOrdinaryCommandStopCapture capture = context.capture();
        FabricChatClefCommandContext ordinaryContext = capture == null ? null : capture.context();
        if (ordinaryContext == null) {
            resultCommitter.commitUnknown(context, "target_observation_failed", clientTick, false);
            return;
        }
        if (retirementPredicate.isRetired(context, capture, ordinaryContext)) {
            context.markVerified(clientTick);
            resultCommitter.commitCompleted(context, clientTick);
            return;
        }
        long verificationDeadlineTick = deadlinePolicy.deadlineTick(context);
        if (clientTick > verificationDeadlineTick) {
            context.markVerified(verificationDeadlineTick);
            resultCommitter.commitUnknown(context, "verification_timeout", verificationDeadlineTick, true);
            return;
        }
        String cancellationFailure = cancellationRetry.failureReason(ordinaryContext, nowMs);
        if ("original_cancel_send_failed".equals(cancellationFailure)) {
            context.markVerified(clientTick);
            resultCommitter.commitUnknown(context, "original_cancel_send_failed", clientTick, true);
            return;
        }
        if ("original_cancel_result_submission_failed".equals(cancellationFailure)) {
            resultCommitter.quarantineWithoutWire(context, cancellationFailure);
            return;
        }
        if (clientTick >= verificationDeadlineTick) {
            context.markVerified(verificationDeadlineTick);
            resultCommitter.commitUnknown(context, "verification_timeout", verificationDeadlineTick, true);
        }
    }
}
