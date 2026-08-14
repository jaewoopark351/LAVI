package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandContextUnbindDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.queue.FabricChatClefCommandQueueCompletion;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

import java.util.function.Supplier;

//20260803_kpopmodder: Send terminal command results exactly once after lifecycle classification.
public final class FabricChatClefCommandResultOutbox {
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
        FabricChatClefCommandContext context = execution.context();
        if (!beginTerminalSend(context, execution.requestId(), execution.duplicateTerminalPayload("duplicate_terminal_result").toMap())) {
            return false;
        }
        FabricChatClefCommandResultPayload result;
        try {
            result = resultFactory.get();
        } catch (RuntimeException error) {
            context.completeTerminalSend(false);
            throw error;
        }
        if (!commandQueue.isActive(context)) {
            context.completeTerminalSend(false);
            diagnostics.warn(
                    "ignored stale terminal result request="
                            + execution.requestId()
                            + " data="
                            + execution.duplicateTerminalPayload("stale_terminal_result").toMap()
            );
            return false;
        }
        FabricChatClefTaskOwnershipSnapshot ownershipBefore =
                FabricChatClefCommandContextUnbindDiagnostics.captureOwnershipSnapshot();
        FabricChatClefCommandResultSendOutcome sendOutcome = resultSender.sendCommandResult(context, result);
        if (!sendOutcome.succeeded()) {
            context.completeTerminalSend(false);
            diagnostics.warn(
                    "terminal result send failed request="
                            + execution.requestId()
                            + " outcome="
                            + sendOutcome.diagnosticMessage()
            );
            return false;
        }
        boolean terminalMarked = context.completeTerminalSend(true);
        FabricChatClefCommandQueueCompletion completion = commandQueue.complete(context, "terminal_result");
        logCompletionBoundary(completion, ownershipBefore);
        return terminalMarked && completion.mutationApplied();
    }

    public boolean sendTerminal(
            FabricChatClefCommandContext context,
            Supplier<FabricChatClefCommandResultPayload> resultFactory
    ) {
        if (!beginTerminalSend(context, context.requestId(), context.ownershipPayload().toMap())) {
            return false;
        }
        FabricChatClefCommandResultPayload result;
        try {
            result = resultFactory.get();
        } catch (RuntimeException error) {
            context.completeTerminalSend(false);
            throw error;
        }
        if (!commandQueue.isActive(context)) {
            context.completeTerminalSend(false);
            diagnostics.warn(
                    "ignored stale terminal result request="
                            + context.requestId()
                            + " data="
                            + context.ownershipPayload().toMap()
            );
            return false;
        }
        FabricChatClefTaskOwnershipSnapshot ownershipBefore =
                FabricChatClefCommandContextUnbindDiagnostics.captureOwnershipSnapshot();
        FabricChatClefCommandResultSendOutcome sendOutcome = resultSender.sendCommandResult(context, result);
        if (!sendOutcome.succeeded()) {
            context.completeTerminalSend(false);
            diagnostics.warn(
                    "terminal result send failed request="
                            + context.requestId()
                            + " outcome="
                            + sendOutcome.diagnosticMessage()
            );
            return false;
        }
        boolean terminalMarked = context.completeTerminalSend(true);
        FabricChatClefCommandQueueCompletion completion = commandQueue.complete(context, "terminal_result");
        logCompletionBoundary(completion, ownershipBefore);
        return terminalMarked && completion.mutationApplied();
    }

    public boolean sendPendingTerminal(
            FabricChatClefCommandContext context,
            Supplier<FabricChatClefCommandResultPayload> resultFactory
    ) {
        if (!context.beginTerminalSend()) {
            diagnostics.warn(
                    "ignored duplicate pending result request="
                            + context.requestId()
                            + " data="
                            + context.ownershipPayload().toMap()
            );
            return false;
        }
        FabricChatClefCommandResultPayload result;
        try {
            result = resultFactory.get();
        } catch (RuntimeException error) {
            context.completeTerminalSend(false);
            throw error;
        }
        FabricChatClefCommandResultSendOutcome sendOutcome = resultSender.sendCommandResult(context, result);
        boolean terminalMarked = context.completeTerminalSend(sendOutcome.succeeded());
        if (!sendOutcome.succeeded()) {
            diagnostics.warn(
                    "pending terminal result send failed request="
                            + context.requestId()
                            + " outcome="
                            + sendOutcome.diagnosticMessage()
            );
        }
        return terminalMarked && sendOutcome.succeeded();
    }

    private boolean beginTerminalSend(
            FabricChatClefCommandContext context,
            String requestId,
            Object data
    ) {
        if (context.beginTerminalSend()) {
            return true;
        }
        diagnostics.warn(
                "ignored duplicate or in-flight terminal result request="
                        + requestId
                        + " data="
                        + data
        );
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
