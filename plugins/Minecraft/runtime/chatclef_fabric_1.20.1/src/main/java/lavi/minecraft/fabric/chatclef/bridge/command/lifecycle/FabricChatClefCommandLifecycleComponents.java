package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

//20260905_kpopmodder: Compose focused ordinary-command lifecycle owners behind the legacy facade.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.FabricChatClefOrdinaryCommandStopLifecycleAdapter;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.FabricChatClefStopControlCommandLifecycle;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandOutcomeClassifier;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandResultOutbox;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefPreexistingIdleRootStabilityGate;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassifier;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefUserTaskFinishedObserver;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.callback.FabricChatClefCommandDispatchReturnHandler;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.callback.FabricChatClefCommandExecutionStarter;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.callback.FabricChatClefCommandFailureHandler;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.callback.FabricChatClefCommandFinishCallbackHandler;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefNonterminalLifecycleEvidencePublisher;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefPreexistingIdleRootStabilityObserver;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.execution.FabricChatClefActiveExecutionStore;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.execution.FabricChatClefCommandLifecycleReset;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.observation.FabricChatClefTaskTerminationObservationDrainer;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.observation.FabricChatClefTaskTerminationObservationHandler;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.ownership.FabricChatClefBoundRootOwnershipView;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.ownership.FabricChatClefCommandBoundRootOwnershipReader;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.terminal.FabricChatClefCommandTerminalEvaluator;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.terminal.FabricChatClefCommandTerminalResultDispatcher;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.tick.FabricChatClefActiveContextSynchronizer;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.tick.FabricChatClefCommandLifecycleTickCoordinator;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.tick.FabricChatClefCommandResultCompletionTickStage;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

import java.util.concurrent.atomic.AtomicReference;

public final class FabricChatClefCommandLifecycleComponents {
    private final FabricChatClefCommandExecutionStarter executionStarter;
    private final FabricChatClefCommandDispatchReturnHandler dispatchReturnHandler;
    private final FabricChatClefCommandFinishCallbackHandler finishCallbackHandler;
    private final FabricChatClefCommandFailureHandler failureHandler;
    private final FabricChatClefCommandLifecycleTickCoordinator tickCoordinator;
    private final FabricChatClefCommandDeadlineHandler deadlineHandler;
    private final FabricChatClefDetachedExecutionRetirement detachedExecutionRetirement;
    private final FabricChatClefCommandBoundRootOwnershipReader boundRootOwnershipReader;
    private final FabricChatClefStopControlCommandLifecycle stopControlLifecycle;

    public FabricChatClefCommandLifecycleComponents(
            FabricChatClefUserTaskFinishedObserver taskFinishedObserver,
            FabricChatClefCommandOutcomeClassifier outcomeClassifier,
            FabricChatClefCommandResultOutbox resultOutbox,
            FabricChatClefCommandResultSender resultSender,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefTaskStateReader taskStateReader,
            AtomicReference<FabricChatClefCommandExecution> activeExecution,
            FabricChatClefPreexistingIdleRootStabilityGate preexistingIdleRootStabilityGate
    ) {
        FabricChatClefCommandDiagnostics commandDiagnostics =
                new FabricChatClefCommandDiagnostics(diagnostics);
        FabricChatClefActiveExecutionStore executionStore =
                new FabricChatClefActiveExecutionStore(activeExecution);
        FabricChatClefNonterminalLifecycleEvidencePublisher nonterminalEvidencePublisher =
                new FabricChatClefNonterminalLifecycleEvidencePublisher(
                        resultSender,
                        taskStateReader,
                        diagnostics
                );
        FabricChatClefPreexistingIdleRootStabilityObserver stabilityObserver =
                new FabricChatClefPreexistingIdleRootStabilityObserver(
                        preexistingIdleRootStabilityGate,
                        taskStateReader
                );
        FabricChatClefWaitingDecisionDiagnostics waitingDiagnostics =
                new FabricChatClefWaitingDecisionDiagnostics(commandDiagnostics, taskStateReader);
        FabricChatClefCommandLifecycleReset lifecycleReset = new FabricChatClefCommandLifecycleReset(
                waitingDiagnostics,
                nonterminalEvidencePublisher,
                stabilityObserver
        );
        FabricChatClefCommandTerminalResultDispatcher terminalResultDispatcher =
                new FabricChatClefCommandTerminalResultDispatcher(
                        resultOutbox,
                        diagnostics,
                        commandDiagnostics,
                        stabilityObserver
                );
        FabricChatClefCommandTerminalEvaluator terminalEvaluator =
                new FabricChatClefCommandTerminalEvaluator(
                        executionStore,
                        outcomeClassifier,
                        stabilityObserver,
                        waitingDiagnostics,
                        nonterminalEvidencePublisher,
                        terminalResultDispatcher
                );
        this.executionStarter = new FabricChatClefCommandExecutionStarter(
                executionStore,
                diagnostics,
                commandDiagnostics,
                lifecycleReset
        );
        this.dispatchReturnHandler = new FabricChatClefCommandDispatchReturnHandler(
                new FabricChatClefRootOwnershipClassifier(),
                commandDiagnostics,
                terminalEvaluator
        );
        this.finishCallbackHandler = new FabricChatClefCommandFinishCallbackHandler(
                commandDiagnostics,
                taskStateReader,
                terminalEvaluator
        );
        this.failureHandler = new FabricChatClefCommandFailureHandler(
                commandDiagnostics,
                terminalResultDispatcher
        );
        FabricChatClefTaskTerminationObservationHandler observationHandler =
                new FabricChatClefTaskTerminationObservationHandler(
                        executionStore,
                        diagnostics,
                        commandDiagnostics,
                        taskStateReader,
                        stabilityObserver,
                        terminalEvaluator
                );
        FabricChatClefTaskTerminationObservationDrainer observationDrainer =
                new FabricChatClefTaskTerminationObservationDrainer(
                        taskFinishedObserver,
                        observationHandler
                );
        this.tickCoordinator = new FabricChatClefCommandLifecycleTickCoordinator(
                new FabricChatClefCommandResultCompletionTickStage(
                        executionStore,
                        resultOutbox,
                        commandDiagnostics,
                        lifecycleReset
                ),
                new FabricChatClefActiveContextSynchronizer(
                        executionStore,
                        diagnostics,
                        commandDiagnostics,
                        lifecycleReset
                ),
                observationDrainer,
                executionStore,
                terminalEvaluator
        );
        this.deadlineHandler = new FabricChatClefCommandDeadlineHandler(
                executionStore,
                commandDiagnostics,
                resultOutbox,
                terminalResultDispatcher
        );
        this.detachedExecutionRetirement = new FabricChatClefDetachedExecutionRetirement(
                executionStore,
                commandDiagnostics,
                lifecycleReset
        );
        this.boundRootOwnershipReader = new FabricChatClefCommandBoundRootOwnershipReader(
                executionStore,
                new FabricChatClefBoundRootOwnershipView()
        );
        this.stopControlLifecycle = new FabricChatClefOrdinaryCommandStopLifecycleAdapter(
                executionStore::current,
                resultOutbox
        );
    }

    public FabricChatClefCommandExecutionStarter executionStarter() {
        return executionStarter;
    }

    public FabricChatClefCommandDispatchReturnHandler dispatchReturnHandler() {
        return dispatchReturnHandler;
    }

    public FabricChatClefCommandFinishCallbackHandler finishCallbackHandler() {
        return finishCallbackHandler;
    }

    public FabricChatClefCommandFailureHandler failureHandler() {
        return failureHandler;
    }

    public FabricChatClefCommandLifecycleTickCoordinator tickCoordinator() {
        return tickCoordinator;
    }

    public FabricChatClefCommandDeadlineHandler deadlineHandler() {
        return deadlineHandler;
    }

    public FabricChatClefDetachedExecutionRetirement detachedExecutionRetirement() {
        return detachedExecutionRetirement;
    }

    public FabricChatClefCommandBoundRootOwnershipReader boundRootOwnershipReader() {
        return boundRootOwnershipReader;
    }

    public FabricChatClefStopControlCommandLifecycle stopControlLifecycle() {
        return stopControlLifecycle;
    }
}
