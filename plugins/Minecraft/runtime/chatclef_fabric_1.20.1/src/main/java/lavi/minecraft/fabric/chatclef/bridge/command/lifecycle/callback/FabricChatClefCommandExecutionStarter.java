package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.callback;

//20260905_kpopmodder: Install and diagnose the start of one ordinary-command execution.

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.execution.FabricChatClefActiveExecutionStore;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.execution.FabricChatClefCommandLifecycleReset;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

public final class FabricChatClefCommandExecutionStarter {
    private final FabricChatClefActiveExecutionStore executionStore;
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefCommandDiagnostics commandDiagnostics;
    private final FabricChatClefCommandLifecycleReset lifecycleReset;

    public FabricChatClefCommandExecutionStarter(
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

    public void begin(FabricChatClefCommandExecution execution) {
        if (!executionStore.installIfAbsent(execution)) {
            FabricChatClefCommandExecution active = executionStore.current();
            diagnostics.warn(
                    "replaced stale lifecycle execution active="
                            + (active == null ? "<none>" : active.requestId())
                            + " next="
                            + execution.requestId()
            );
            commandDiagnostics.warn(
                    "begin_execution_replaced_stale_active",
                    execution,
                    FabricChatClefLifecycleDetailsPayload.replacedActive(
                            active == null ? "" : active.requestId()
                    )
            );
            executionStore.replace(execution);
        }
        lifecycleReset.reset();
        commandDiagnostics.info("begin_execution", execution);
    }
}
