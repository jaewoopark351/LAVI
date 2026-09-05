package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.tick;

//20260905_kpopmodder: Sequence focused ordinary-command lifecycle stages on the client tick.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.execution.FabricChatClefActiveExecutionStore;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.observation.FabricChatClefTaskTerminationObservationDrainer;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.terminal.FabricChatClefCommandTerminalEvaluator;

import java.util.Optional;

public final class FabricChatClefCommandLifecycleTickCoordinator {
    private final FabricChatClefCommandResultCompletionTickStage resultCompletionStage;
    private final FabricChatClefActiveContextSynchronizer contextSynchronizer;
    private final FabricChatClefTaskTerminationObservationDrainer observationDrainer;
    private final FabricChatClefActiveExecutionStore executionStore;
    private final FabricChatClefCommandTerminalEvaluator terminalEvaluator;

    public FabricChatClefCommandLifecycleTickCoordinator(
            FabricChatClefCommandResultCompletionTickStage resultCompletionStage,
            FabricChatClefActiveContextSynchronizer contextSynchronizer,
            FabricChatClefTaskTerminationObservationDrainer observationDrainer,
            FabricChatClefActiveExecutionStore executionStore,
            FabricChatClefCommandTerminalEvaluator terminalEvaluator
    ) {
        this.resultCompletionStage = resultCompletionStage;
        this.contextSynchronizer = contextSynchronizer;
        this.observationDrainer = observationDrainer;
        this.executionStore = executionStore;
        this.terminalEvaluator = terminalEvaluator;
    }

    public void onEndClientTick(Optional<FabricChatClefCommandContext> activeContext) {
        resultCompletionStage.drain();
        contextSynchronizer.synchronize(activeContext);
        observationDrainer.drain();
        FabricChatClefCommandExecution execution = executionStore.current();
        if (execution != null) {
            terminalEvaluator.evaluate(execution, true, activeContext.orElse(null));
        }
    }
}
