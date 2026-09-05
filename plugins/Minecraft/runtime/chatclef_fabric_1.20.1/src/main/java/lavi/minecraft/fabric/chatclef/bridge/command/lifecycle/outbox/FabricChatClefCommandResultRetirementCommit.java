package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.outbox;

//20260905_kpopmodder: Commit queue retirement only after a successful terminal-send completion.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandContextUnbindDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.queue.FabricChatClefCommandQueueCompletion;

public final class FabricChatClefCommandResultRetirementCommit {
    private final FabricChatClefCommandQueue commandQueue;
    private final FabricChatClefCommandResultOutboxDiagnostics diagnostics;

    public FabricChatClefCommandResultRetirementCommit(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefCommandResultOutboxDiagnostics diagnostics
    ) {
        this.commandQueue = commandQueue;
        this.diagnostics = diagnostics;
    }

    public boolean retire(
            FabricChatClefCommandExecution activeExecution,
            FabricChatClefCommandContext context,
            boolean terminalMarked,
            long completedAtMs
    ) {
        if (commandQueue.isActive(context)) {
            return retireActive(activeExecution, context, terminalMarked);
        }
        if (commandQueue.isPending(context)) {
            boolean removed = commandQueue.removePending(context);
            diagnostics.pendingRetired(context, removed);
            return false;
        }
        diagnostics.staleCompletion(context, completedAtMs);
        return false;
    }

    private boolean retireActive(
            FabricChatClefCommandExecution activeExecution,
            FabricChatClefCommandContext context,
            boolean terminalMarked
    ) {
        FabricChatClefTaskOwnershipSnapshot ownershipBefore =
                FabricChatClefCommandContextUnbindDiagnostics.captureOwnershipSnapshot();
        FabricChatClefCommandQueueCompletion completion =
                commandQueue.complete(context, "terminal_result");
        diagnostics.completionBoundary(completion, ownershipBefore);
        return activeExecution != null
                && activeExecution.context() == context
                && terminalMarked
                && completion.mutationApplied();
    }
}
