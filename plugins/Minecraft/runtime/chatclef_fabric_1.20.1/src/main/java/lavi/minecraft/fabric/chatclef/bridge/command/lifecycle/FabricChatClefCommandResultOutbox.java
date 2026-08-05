package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
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
        if (!execution.markTerminalSent()) {
            diagnostics.warn(
                    "ignored duplicate terminal result request="
                            + execution.requestId()
                            + " data="
                            + execution.duplicateTerminalData("duplicate_terminal_result")
            );
            return false;
        }
        FabricChatClefCommandResultPayload result = resultFactory.get();
        if (!commandQueue.complete(execution.context())) {
            diagnostics.warn(
                    "ignored stale terminal result request="
                            + execution.requestId()
                            + " data="
                            + execution.duplicateTerminalData("stale_terminal_result")
            );
            return true;
        }
        resultSender.sendCommandResult(execution.context(), result);
        return true;
    }

    public boolean sendTerminal(
            FabricChatClefCommandContext context,
            Supplier<FabricChatClefCommandResultPayload> resultFactory
    ) {
        if (!context.markTerminalSent()) {
            diagnostics.warn(
                    "ignored duplicate terminal result request="
                            + context.requestId()
                            + " data="
                            + context.ownershipPayload().toMap()
            );
            return false;
        }
        FabricChatClefCommandResultPayload result = resultFactory.get();
        if (!commandQueue.complete(context)) {
            diagnostics.warn(
                    "ignored stale terminal result request="
                            + context.requestId()
                            + " data="
                            + context.ownershipPayload().toMap()
            );
            return true;
        }
        resultSender.sendCommandResult(context, result);
        return true;
    }

    public boolean sendPendingTerminal(
            FabricChatClefCommandContext context,
            Supplier<FabricChatClefCommandResultPayload> resultFactory
    ) {
        if (!context.markTerminalSent()) {
            diagnostics.warn(
                    "ignored duplicate pending result request="
                            + context.requestId()
                            + " data="
                            + context.ownershipPayload().toMap()
            );
            return false;
        }
        FabricChatClefCommandResultPayload result = resultFactory.get();
        resultSender.sendCommandResult(context, result);
        return true;
    }
}
