package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.outbox;

//20260905_kpopmodder: Validate active or pending queue ownership before terminal transport.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;

public final class FabricChatClefTerminalResultOwnershipValidator {
    private final FabricChatClefCommandQueue commandQueue;
    private final FabricChatClefCommandResultOutboxDiagnostics diagnostics;

    public FabricChatClefTerminalResultOwnershipValidator(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefCommandResultOutboxDiagnostics diagnostics
    ) {
        this.commandQueue = commandQueue;
        this.diagnostics = diagnostics;
    }

    public boolean validateActive(
            FabricChatClefCommandContext context,
            String requestId,
            Object duplicateData
    ) {
        if (commandQueue.isActive(context)) {
            return true;
        }
        context.cancelTerminalSendAttempt("stale_terminal_result");
        diagnostics.staleSubmission(requestId, duplicateData, false);
        return false;
    }

    public boolean validatePending(
            FabricChatClefCommandContext context,
            String requestId,
            Object duplicateData
    ) {
        if (commandQueue.isPending(context)) {
            return true;
        }
        context.cancelTerminalSendAttempt("stale_pending_terminal_result");
        diagnostics.staleSubmission(requestId, duplicateData, true);
        return false;
    }
}
