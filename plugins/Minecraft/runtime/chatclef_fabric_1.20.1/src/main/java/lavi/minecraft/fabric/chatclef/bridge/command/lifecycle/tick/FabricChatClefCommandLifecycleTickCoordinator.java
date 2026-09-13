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
    //#if MC == 12001
    private final lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.RootLifetimeTickObserver rootLifetimeObserver;
    //#endif

    public FabricChatClefCommandLifecycleTickCoordinator(
            FabricChatClefCommandResultCompletionTickStage resultCompletionStage,
            FabricChatClefActiveContextSynchronizer contextSynchronizer,
            FabricChatClefTaskTerminationObservationDrainer observationDrainer,
            FabricChatClefActiveExecutionStore executionStore,
            FabricChatClefCommandTerminalEvaluator terminalEvaluator
            //#if MC == 12001
            , lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.RootLifetimeTickObserver rootLifetimeObserver
            //#endif
    ) {
        this.resultCompletionStage = resultCompletionStage;
        this.contextSynchronizer = contextSynchronizer;
        this.observationDrainer = observationDrainer;
        this.executionStore = executionStore;
        this.terminalEvaluator = terminalEvaluator;
        //#if MC == 12001
        this.rootLifetimeObserver = rootLifetimeObserver;
        //#endif
    }

    public void onEndClientTick(Optional<FabricChatClefCommandContext> activeContext) {
        resultCompletionStage.drain();
        contextSynchronizer.synchronize(activeContext);
        observationDrainer.drain();
        FabricChatClefCommandExecution execution = executionStore.current();
        if (execution != null) {
            //#if MC == 12001
            rootLifetimeObserver.observe(execution, activeContext.orElse(null));
            //#endif
            terminalEvaluator.evaluate(execution, true, activeContext.orElse(null));
        }
    }
}
