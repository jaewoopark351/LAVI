package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.tick;

//20260905_kpopmodder: Synchronize lifecycle ownership with the ordinary command queue context.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.execution.FabricChatClefActiveExecutionStore;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.execution.FabricChatClefCommandLifecycleReset;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

import java.util.Optional;

public final class FabricChatClefActiveContextSynchronizer {
    private final FabricChatClefActiveExecutionStore executionStore;
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefCommandDiagnostics commandDiagnostics;
    private final FabricChatClefCommandLifecycleReset lifecycleReset;

    public FabricChatClefActiveContextSynchronizer(
            FabricChatClefActiveExecutionStore executionStore,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefCommandDiagnostics commandDiagnostics,
            FabricChatClefCommandLifecycleReset lifecycleReset
    ) {
        this.executionStore = executionStore;
        this.diagnostics = diagnostics;
        this.commandDiagnostics = commandDiagnostics;
        this.lifecycleReset = lifecycleReset;
    }

    public void synchronize(Optional<FabricChatClefCommandContext> activeContext) {
        FabricChatClefCommandExecution execution = executionStore.current();
        if (execution == null) {
            return;
        }
        if (activeContext.isPresent() && activeContext.get() == execution.context()) {
            return;
        }
        diagnostics.warn(
                "cleared lifecycle execution without active queue context request="
                        + execution.requestId()
        );
        commandDiagnostics.warn(
                "lifecycle_execution_without_active_queue_context",
                execution,
                FabricChatClefLifecycleDetailsPayload.queueContextMismatch(
                        activeContext.isPresent(),
                        activeContext.isPresent() ? activeContext.get().requestId() : ""
                )
        );
        executionStore.clearIfCurrent(execution);
        lifecycleReset.reset();
    }
}
