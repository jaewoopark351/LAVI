package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.tick;

//20260905_kpopmodder: Retire active lifecycle ownership after terminal result send completion.

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandResultOutbox;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.execution.FabricChatClefActiveExecutionStore;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.execution.FabricChatClefCommandLifecycleReset;

public final class FabricChatClefCommandResultCompletionTickStage {
    private final FabricChatClefActiveExecutionStore executionStore;
    private final FabricChatClefCommandResultOutbox resultOutbox;
    private final FabricChatClefCommandDiagnostics commandDiagnostics;
    private final FabricChatClefCommandLifecycleReset lifecycleReset;

    public FabricChatClefCommandResultCompletionTickStage(
            FabricChatClefActiveExecutionStore executionStore,
            FabricChatClefCommandResultOutbox resultOutbox,
            FabricChatClefCommandDiagnostics commandDiagnostics,
            FabricChatClefCommandLifecycleReset lifecycleReset
    ) {
        this.executionStore = executionStore;
        this.resultOutbox = resultOutbox;
        this.commandDiagnostics = commandDiagnostics;
        this.lifecycleReset = lifecycleReset;
    }

    public void drain() {
        FabricChatClefCommandExecution execution = executionStore.current();
        if (resultOutbox.drainSendCompletions(execution)
                && executionStore.clearIfCurrent(execution)) {
            commandDiagnostics.info(
                    "terminal_result_sent",
                    execution,
                    FabricChatClefLifecycleDetailsPayload.terminalResult(true, true)
            );
            lifecycleReset.reset();
        }
    }
}
