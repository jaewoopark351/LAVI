package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

//20260905_kpopmodder: Retire the matching lifecycle execution after connection detach.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.execution.FabricChatClefActiveExecutionStore;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.execution.FabricChatClefCommandLifecycleReset;

public final class FabricChatClefDetachedExecutionRetirement {
    private final FabricChatClefActiveExecutionStore executionStore;
    private final FabricChatClefCommandDiagnostics commandDiagnostics;
    private final FabricChatClefCommandLifecycleReset lifecycleReset;

    public FabricChatClefDetachedExecutionRetirement(
            FabricChatClefActiveExecutionStore executionStore,
            FabricChatClefCommandDiagnostics commandDiagnostics,
            FabricChatClefCommandLifecycleReset lifecycleReset
    ) {
        this.executionStore = executionStore;
        this.commandDiagnostics = commandDiagnostics;
        this.lifecycleReset = lifecycleReset;
    }

    public boolean clear(FabricChatClefCommandContext context, String reason) {
        FabricChatClefCommandExecution execution = executionStore.current();
        if (execution == null || execution.context() != context) {
            commandDiagnostics.contextWarn(
                    "connection_detached_without_matching_lifecycle_execution",
                    context,
                    FabricChatClefLifecycleDetailsPayload.empty()
            );
            return false;
        }
        boolean cleared = executionStore.clearIfCurrent(execution);
        commandDiagnostics.warn(
                "connection_detached_lifecycle_cleared",
                execution,
                FabricChatClefLifecycleDetailsPayload.terminalDecision(reason)
        );
        if (cleared) {
            lifecycleReset.reset();
        }
        return cleared;
    }
}
