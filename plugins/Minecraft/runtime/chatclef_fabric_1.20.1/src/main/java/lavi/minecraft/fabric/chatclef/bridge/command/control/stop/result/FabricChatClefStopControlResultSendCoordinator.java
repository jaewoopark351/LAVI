package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result;

//20260905_kpopmodder: Preserve the result-send API as a thin facade over focused delivery stages.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.diagnostics.FabricChatClefStopControlTransitionEmitter;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.delivery.FabricChatClefStopControlResultCompletionDrainer;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.delivery.FabricChatClefStopControlResultCompletionStage;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.delivery.FabricChatClefStopControlResultDeliveryCommitStage;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.delivery.FabricChatClefStopControlResultDeliveryTransitionPublisher;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.delivery.FabricChatClefStopControlResultRetryStage;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.delivery.FabricChatClefStopControlResultSendCompletionQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.delivery.FabricChatClefStopControlResultStatusInspector;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.delivery.FabricChatClefStopControlResultSubmissionStage;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.delivery.FabricChatClefStopControlResultSubmissionDecision;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.delivery.FabricChatClefStopControlSentBarrierCommit;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlBaseRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;

import java.util.function.BooleanSupplier;

public final class FabricChatClefStopControlResultSendCoordinator {
    private final FabricChatClefStopControlResultDeliveryCommitStage commitStage;
    private final FabricChatClefStopControlResultSubmissionStage submissionStage;
    private final FabricChatClefStopControlResultCompletionStage completionStage;
    private final FabricChatClefStopControlResultCompletionDrainer completionDrainer;
    private final FabricChatClefStopControlResultRetryStage retryStage;
    private final FabricChatClefStopControlResultSendCompletionQueue completionQueue;

    public FabricChatClefStopControlResultSendCoordinator(
            FabricChatClefStopControlResultSender resultSender,
            FabricChatClefStopControlTransitionEmitter transitionEmitter,
            FabricChatClefStopControlResultDeliveryRegistry registry
    ) {
        this.completionQueue = new FabricChatClefStopControlResultSendCompletionQueue();
        FabricChatClefStopControlResultStatusInspector statusInspector =
                new FabricChatClefStopControlResultStatusInspector();
        FabricChatClefStopControlResultDeliveryTransitionPublisher transitions =
                new FabricChatClefStopControlResultDeliveryTransitionPublisher(
                        transitionEmitter,
                        statusInspector
                );
        this.commitStage = new FabricChatClefStopControlResultDeliveryCommitStage(registry, transitions);
        this.submissionStage = new FabricChatClefStopControlResultSubmissionStage(
                resultSender,
                completionQueue
        );
        this.completionStage = new FabricChatClefStopControlResultCompletionStage(
                registry,
                new FabricChatClefStopControlSentBarrierCommit(statusInspector),
                transitions
        );
        this.completionDrainer = new FabricChatClefStopControlResultCompletionDrainer(
                completionQueue,
                completionStage
        );
        this.retryStage = new FabricChatClefStopControlResultRetryStage(
                registry,
                submissionStage,
                completionStage
        );
    }

    public FabricChatClefStopControlResultDelivery commitAndSend(
            FabricChatClefStopControlBaseRequest request,
            FabricChatClefCommandResultPayload payload,
            boolean javaBarrierOwned,
            String invalidTargetFieldsMask,
            BooleanSupplier sentCommit
    ) {
        FabricChatClefStopControlResultDelivery delivery = commitStage.commit(
                request,
                payload,
                javaBarrierOwned,
                invalidTargetFieldsMask,
                sentCommit
        );
        long nowMs = System.currentTimeMillis();
        FabricChatClefStopControlResultSubmissionDecision decision =
                submissionStage.submitIfReady(delivery, nowMs);
        if (decision.immediateCompletion()) {
            completionStage.complete(delivery, decision.outcome(), nowMs);
        }
        return delivery;
    }

    public void onEndClientTick(long nowMs) {
        completionDrainer.drain(nowMs);
        retryStage.submitReady(nowMs);
    }

    public void resetCompletions() {
        completionQueue.clear();
    }
}
