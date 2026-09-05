package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.delivery;

//20260905_kpopmodder: Resubmit live STOP deliveries whose bounded backoff has elapsed.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultDelivery;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultDeliveryRegistry;

public final class FabricChatClefStopControlResultRetryStage {
    private final FabricChatClefStopControlResultDeliveryRegistry registry;
    private final FabricChatClefStopControlResultSubmissionStage submissionStage;
    private final FabricChatClefStopControlResultCompletionStage completionStage;

    public FabricChatClefStopControlResultRetryStage(
            FabricChatClefStopControlResultDeliveryRegistry registry,
            FabricChatClefStopControlResultSubmissionStage submissionStage,
            FabricChatClefStopControlResultCompletionStage completionStage
    ) {
        this.registry = registry;
        this.submissionStage = submissionStage;
        this.completionStage = completionStage;
    }

    public void submitReady(long nowMs) {
        for (FabricChatClefStopControlResultDelivery delivery : registry.snapshot()) {
            FabricChatClefStopControlResultSubmissionDecision decision =
                    submissionStage.submitIfReady(delivery, nowMs);
            if (decision.immediateCompletion()) {
                completionStage.complete(delivery, decision.outcome(), nowMs);
            }
        }
    }
}
