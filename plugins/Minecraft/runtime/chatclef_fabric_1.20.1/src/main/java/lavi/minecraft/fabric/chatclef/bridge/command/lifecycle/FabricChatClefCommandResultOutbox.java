package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandContextUnbindDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.queue.FabricChatClefCommandQueueCompletion;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendCompletion;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

import java.util.function.Supplier;

//20260803_kpopmodder: Send terminal command results exactly once after lifecycle classification.
public final class FabricChatClefCommandResultOutbox {
    private static final int MAX_SEND_COMPLETIONS_PER_TICK = 32;

    private final FabricChatClefCommandQueue commandQueue;
    private final FabricChatClefCommandResultSender resultSender;
    private final FabricChatClefBridgeDiagnostics diagnostics;

    public FabricChatClefCommandResultOutbox(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefCommandResultSender resultSender,
            FabricChatClefBridgeDiagnostics diagnostics
    ) {
        this.commandQueue = commandQueue;
        this.resultSender = resultSender;
        this.diagnostics = diagnostics;
    }

    public boolean sendTerminal(
            FabricChatClefCommandExecution execution,
            Supplier<FabricChatClefCommandResultPayload> resultFactory
    ) {
        return submitTerminal(
                execution.context(),
                execution.requestId(),
                execution.duplicateTerminalPayload("duplicate_terminal_result").toMap(),
                resultFactory,
                true
        );
    }

    public boolean sendTerminal(
            FabricChatClefCommandContext context,
            Supplier<FabricChatClefCommandResultPayload> resultFactory
    ) {
        return submitTerminal(
                context,
                context.requestId(),
                context.ownershipPayload().toMap(),
                resultFactory,
                true
        );
    }

    public boolean sendPendingTerminal(
            FabricChatClefCommandContext context,
            Supplier<FabricChatClefCommandResultPayload> resultFactory
    ) {
        return submitTerminal(
                context,
                context.requestId(),
                context.ownershipPayload().toMap(),
                resultFactory,
                false
        );
    }

    public boolean drainSendCompletions(FabricChatClefCommandExecution activeExecution) {
        boolean activeTerminalCompleted = false;
        for (int index = 0; index < MAX_SEND_COMPLETIONS_PER_TICK; index++) {
            FabricChatClefCommandResultSendCompletion completion =
                    commandQueue.pollCommandResultSendCompletion();
            if (completion == null) {
                return activeTerminalCompleted;
            }
            activeTerminalCompleted |= handleSendCompletion(activeExecution, completion);
        }
        return activeTerminalCompleted;
    }

    private boolean submitTerminal(
            FabricChatClefCommandContext context,
            String requestId,
            Object duplicateData,
            Supplier<FabricChatClefCommandResultPayload> resultFactory,
            boolean requireActive
    ) {
        if (!beginTerminalSend(context, requestId, duplicateData)) {
            return false;
        }
        FabricChatClefCommandResultPayload result;
        try {
            result = resultFactory.get();
        } catch (RuntimeException error) {
            context.cancelTerminalSendAttempt("terminal_result_factory_failed");
            throw error;
        }
        if (requireActive && !commandQueue.isActive(context)) {
            context.cancelTerminalSendAttempt("stale_terminal_result");
            diagnostics.warn(
                    "ignored stale terminal result request="
                            + requestId
                            + " data="
                            + duplicateData
            );
            return false;
        }
        if (!requireActive && !commandQueue.isPending(context)) {
            context.cancelTerminalSendAttempt("stale_pending_terminal_result");
            diagnostics.warn(
                    "ignored stale pending terminal result request="
                            + requestId
                            + " data="
                            + duplicateData
            );
            return false;
        }
        FabricChatClefCommandResultSendSubmission submission = resultSender.sendTerminalCommandResult(context, result);
        if (!submission.acceptedForAsyncSend()) {
            FabricChatClefCommandResultSendOutcome outcome = submission.immediateOutcome();
            context.completeTerminalSend(outcome);
            diagnostics.warn(
                    "terminal result send submit failed request="
                            + requestId
                            + " attempt="
                            + context.terminalSendAttemptCount()
                            + " state="
                            + context.terminalSendState()
                            + " outcome="
                            + outcome.diagnosticMessage()
            );
            return false;
        }
        diagnostics.info(
                "terminal result send started request="
                        + requestId
                        + " attempt="
                        + context.terminalSendAttemptCount()
        );
        return true;
    }

    private boolean handleSendCompletion(
            FabricChatClefCommandExecution activeExecution,
            FabricChatClefCommandResultSendCompletion sendCompletion
    ) {
        FabricChatClefCommandContext context = sendCompletion.context();
        FabricChatClefCommandResultSendOutcome outcome = sendCompletion.outcome();
        if (context == null || outcome == null) {
            diagnostics.warn("ignored malformed terminal result send completion");
            return false;
        }
        boolean terminalMarked = context.completeTerminalSend(outcome);
        if (!outcome.succeeded()) {
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
            return false;
        }
        if (commandQueue.isActive(context)) {
            return completeActiveContextAfterSend(activeExecution, context, terminalMarked);
        }
        if (commandQueue.isPending(context)) {
            boolean removed = commandQueue.removePending(context);
            diagnostics.info(
                    "pending terminal result sent request="
                            + context.requestId()
                            + " removed_pending="
                            + removed
            );
            return false;
        }
        diagnostics.warn(
                "ignored terminal result send completion for stale command request="
                        + context.requestId()
                        + " completed_at_ms="
                        + sendCompletion.completedAtMs()
        );
        return false;
    }

    private boolean completeActiveContextAfterSend(
            FabricChatClefCommandExecution activeExecution,
            FabricChatClefCommandContext context,
            boolean terminalMarked
    ) {
        FabricChatClefTaskOwnershipSnapshot ownershipBefore =
                FabricChatClefCommandContextUnbindDiagnostics.captureOwnershipSnapshot();
        FabricChatClefCommandQueueCompletion completion = commandQueue.complete(context, "terminal_result");
        logCompletionBoundary(completion, ownershipBefore);
        return activeExecution != null
                && activeExecution.context() == context
                && terminalMarked
                && completion.mutationApplied();
    }

    private boolean beginTerminalSend(
            FabricChatClefCommandContext context,
            String requestId,
            Object data
    ) {
        if (context.beginTerminalSend(System.currentTimeMillis())) {
            return true;
        }
        return false;
    }

    private void logCompletionBoundary(
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
