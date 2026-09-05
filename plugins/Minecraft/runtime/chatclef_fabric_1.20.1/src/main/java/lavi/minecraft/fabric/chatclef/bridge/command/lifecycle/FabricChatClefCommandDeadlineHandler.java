package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

//20260905_kpopmodder: Convert pending and active ordinary-command deadline expiry into terminal results.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResult;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandDeadlinePayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandResultOutbox;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.execution.FabricChatClefActiveExecutionStore;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.terminal.FabricChatClefCommandTerminalResultDispatcher;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;

public final class FabricChatClefCommandDeadlineHandler {
    private final FabricChatClefActiveExecutionStore executionStore;
    private final FabricChatClefCommandDiagnostics commandDiagnostics;
    private final FabricChatClefCommandResultOutbox resultOutbox;
    private final FabricChatClefCommandTerminalResultDispatcher terminalResultDispatcher;

    public FabricChatClefCommandDeadlineHandler(
            FabricChatClefActiveExecutionStore executionStore,
            FabricChatClefCommandDiagnostics commandDiagnostics,
            FabricChatClefCommandResultOutbox resultOutbox,
            FabricChatClefCommandTerminalResultDispatcher terminalResultDispatcher
    ) {
        this.executionStore = executionStore;
        this.commandDiagnostics = commandDiagnostics;
        this.resultOutbox = resultOutbox;
        this.terminalResultDispatcher = terminalResultDispatcher;
    }

    public void active(FabricChatClefCommandContext context) {
        if (!context.terminalSendReady(System.currentTimeMillis())) {
            return;
        }
        FabricChatClefCommandExecution execution = executionStore.current();
        if (execution != null && execution.context() == context) {
            commandDiagnostics.warn("active_deadline_exceeded", execution);
            terminalResultDispatcher.dispatch(
                    execution,
                    () -> execution.deadlineExceededResult(
                            "Fabric ChatClef command deadline expired while active."
                    )
            );
            return;
        }
        commandDiagnostics.contextWarn(
                "active_deadline_exceeded_without_lifecycle_execution",
                context,
                FabricChatClefLifecycleDetailsPayload.empty()
        );
        resultOutbox.sendTerminal(
                context,
                () -> FabricChatClefCommandResult.deadlineExceeded(
                        context.requestId(),
                        "Fabric ChatClef command deadline expired while active.",
                        deadlineData(context)
                )
        );
    }

    public void pending(FabricChatClefCommandContext context) {
        if (!context.terminalSendReady(System.currentTimeMillis())) {
            return;
        }
        commandDiagnostics.contextWarn(
                "pending_deadline_exceeded",
                context,
                FabricChatClefLifecycleDetailsPayload.empty()
        );
        resultOutbox.sendPendingTerminal(
                context,
                () -> FabricChatClefCommandResult.deadlineExceeded(
                        context.requestId(),
                        "Fabric ChatClef command deadline expired before dispatch."
                )
        );
    }

    private FabricChatClefCommandResultDataPayload deadlineData(FabricChatClefCommandContext context) {
        return FabricChatClefCommandDeadlinePayload.markTaskMayStillBeRunning(context.ownershipPayload());
    }
}
