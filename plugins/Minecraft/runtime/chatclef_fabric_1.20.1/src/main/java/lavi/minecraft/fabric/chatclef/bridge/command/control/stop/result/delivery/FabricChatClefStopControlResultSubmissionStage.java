package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.delivery;

//20260905_kpopmodder: Submit one ready STOP result and normalize immediate transport failures.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlBaseRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultDelivery;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendStatus;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;

public final class FabricChatClefStopControlResultSubmissionStage {
    private final FabricChatClefStopControlResultSender resultSender;
    private final FabricChatClefStopControlResultSendCompletionQueue completionQueue;

    public FabricChatClefStopControlResultSubmissionStage(
            FabricChatClefStopControlResultSender resultSender,
            FabricChatClefStopControlResultSendCompletionQueue completionQueue
    ) {
        this.resultSender = resultSender;
        this.completionQueue = completionQueue;
    }

    public FabricChatClefStopControlResultSubmissionDecision submitIfReady(
            FabricChatClefStopControlResultDelivery delivery,
            long nowMs
    ) {
        if (!delivery.beginSend(nowMs)) {
            return FabricChatClefStopControlResultSubmissionDecision.pending();
        }
        FabricChatClefStopControlBaseRequest request = delivery.request();
        FabricChatClefCommandResultSendSubmission submission;
        try {
            submission = resultSender.sendStopControlResult(
                    request.identity().messageId(),
                    request.identity().sessionId(),
                    request.javaSocketGeneration(),
                    delivery.payload(),
                    outcome -> completionQueue.enqueue(delivery, outcome)
            );
        } catch (Throwable ignored) {
            return FabricChatClefStopControlResultSubmissionDecision.immediate(
                    failed("stop_control_result_submission_failed")
            );
        }
        if (submission == null) {
            return FabricChatClefStopControlResultSubmissionDecision.immediate(
                    failed("missing_stop_control_result_submission")
            );
        }
        if (!submission.acceptedForAsyncSend()) {
            return FabricChatClefStopControlResultSubmissionDecision.immediate(
                    submission.immediateOutcome()
            );
        }
        return FabricChatClefStopControlResultSubmissionDecision.pending();
    }

    private FabricChatClefCommandResultSendOutcome failed(String reason) {
        return FabricChatClefCommandResultSendOutcome.failed(
                FabricChatClefCommandResultSendStatus.SEND_FAILED,
                reason
        );
    }
}
