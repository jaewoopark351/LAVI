package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.outbox;

//20260905_kpopmodder: Render terminal outbox diagnostics without owning send or queue decisions.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandContextUnbindDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.queue.FabricChatClefCommandQueueCompletion;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

public final class FabricChatClefCommandResultOutboxDiagnostics {
    private final FabricChatClefBridgeDiagnostics diagnostics;

    public FabricChatClefCommandResultOutboxDiagnostics(FabricChatClefBridgeDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    public void staleSubmission(String requestId, Object duplicateData, boolean pending) {
        diagnostics.warn(
                "ignored stale "
                        + (pending ? "pending " : "")
                        + "terminal result request="
                        + requestId
                        + " data="
                        + duplicateData
        );
    }

    public void submissionFailed(
            FabricChatClefCommandContext context,
            FabricChatClefCommandResultSendOutcome outcome
    ) {
        diagnostics.warn(
                "terminal result send submit failed request="
                        + context.requestId()
                        + " attempt="
                        + context.terminalSendAttemptCount()
                        + " state="
                        + context.terminalSendState()
                        + " outcome="
                        + outcome.diagnosticMessage()
        );
    }

    public void submissionStarted(FabricChatClefCommandContext context) {
        diagnostics.info(
                "terminal result send started request="
                        + context.requestId()
                        + " attempt="
                        + context.terminalSendAttemptCount()
        );
    }

    public void malformedCompletion() {
        diagnostics.warn("ignored malformed terminal result send completion");
    }

    public void asyncSendFailed(
            FabricChatClefCommandContext context,
            FabricChatClefCommandResultSendOutcome outcome
    ) {
        diagnostics.warn(
                "terminal result async send failed request="
                        + context.requestId()
                        + " attempt="
                        + context.terminalSendAttemptCount()
                        + " state="
                        + context.terminalSendState()
                        + " next_retry_at_ms="
                        + context.nextTerminalSendAttemptAtMs()
                        + " outcome="
                        + outcome.diagnosticMessage()
        );
    }

    public void pendingRetired(FabricChatClefCommandContext context, boolean removed) {
        diagnostics.info(
                "pending terminal result sent request="
                        + context.requestId()
                        + " removed_pending="
                        + removed
        );
    }

    public void staleCompletion(FabricChatClefCommandContext context, long completedAtMs) {
        diagnostics.warn(
                "ignored terminal result send completion for stale command request="
                        + context.requestId()
                        + " completed_at_ms="
                        + completedAtMs
        );
    }

    public void completionBoundary(
            FabricChatClefCommandQueueCompletion completion,
            FabricChatClefTaskOwnershipSnapshot ownershipBefore
    ) {
        FabricChatClefCommandContextUnbindDiagnostics.logBoundary(
                completion.reason(),
                completion.context(),
                completion.activeBefore(),
                completion.activeAfter(),
                completion.mutationApplied(),
                ownershipBefore,
                FabricChatClefCommandContextUnbindDiagnostics.captureOwnershipSnapshot()
        );
    }
}
